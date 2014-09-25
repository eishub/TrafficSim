package microModel;

import GUI.jModelGUI;
import apl.CarFollowing.DensityGenerator;

import com.google.common.collect.Lists;

import eis.eis2java.environment.AbstractEnvironment;
import microModel.core.driver.jDriver;
import microModel.core.road.device.AbstractRSU;
import microModel.core.device.jController;
import microModel.core.road.device.jDetector;
import microModel.core.road.jLane;
import microModel.core.vehicle.*;
import microModel.output.VehicleLogBuffer;
import microModel.output.jDetectorData;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Main model object. This functions as the main interface with the model. It
 * contains general settings, the network, all vehicles etc. Furthermore, it
 * represents the world.
 */
public class jModel {

    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(jModel.class);

    public static ExecutorService APL_UPDATE_THREAD_POOL = Executors.newFixedThreadPool(1);
    public static ExecutorService LOGGING_THREAD_POOL = Executors.newFixedThreadPool(1);

    /** Singelton instance. */
    private static jModel INSTANCE;

    /*******************************************************************************/
    /** Used to keep track of which drivers have performed an action in this round */
    public Set<jDriver> driverActionsPerCycle;

    /** Used a barrier to wait for all agents to perform an action before the simulator proceeds with executing the actions. */
    public CountDownLatch LATCH;

    public Map<Integer,Long> agentReactionWindow = new TreeMap<Integer, Long>();
    public Map<Integer,Integer> agentReactionCount = new TreeMap<Integer, Integer>();
    public Map<Integer,Long> simulationCycle = new TreeMap<Integer, Long>();
    public Map<Integer,Integer> simulationCycleCount = new TreeMap<Integer, Integer>();

    public synchronized void performedAction(jDriver driver) {
        if (!driverActionsPerCycle.contains(driver)) {
            driverActionsPerCycle.add(driver);
            LATCH.countDown();
        }
    }
    /*******************************************************************************/

    public static final class Builder {
        private final jLane[] network;

        public Builder(jLane[] network) {
            this.network = network;
        }

        /**
         * Builds a jModel instance.
         * @param startTime The start time in [s] of the simulation.
         * @return a jModel instance
         */
        public jModel build(double startTime) {
            INSTANCE = new jModel(this.network);
            INSTANCE.setStartTime(startTime);
            return INSTANCE;
        }
    }

    public static final jModel getInstance() {
        return INSTANCE;
    }

    /** The Environment Interface for APL. */
    private AbstractEnvironment environment;

    /** Time step number. Always starts as 0. */
    private int step;

    /** Current simulation time of the model [s]. Always starts at 0. */
    private double t;

    /** Time step size of the execution cycle in [s] */
    private double dt;

    /** Maximum simulation length [s]. */
    private double length;

    /** Absolute start time of simulation [s from epoch]. */
    private double startTime;

    /** Set of all vehicles in simulation. */
    private List<Vehicle> vehicles = new ArrayList<Vehicle>();

    /** Set of lanes that make up the network. */
    private jLane[] network = new jLane[0];

    /** Set of all vehicle-driver classes. */
    private List<jClass> classes = new ArrayList<jClass>();

    /** Set of controllers, both local and regional. */
    private List<jController> controllers = new ArrayList<jController>();
    
    private jModelGUI gui;

    private Map<jVehicle, VehicleLogBuffer> vehicleLogs = new HashMap<jVehicle, VehicleLogBuffer>();

    private jModel(jLane[] network) {
        this.network = network;
        jSettings settings = jSettings.getInstance();

        step = 0;
        t = 0;
        dt = settings.get(BuiltInSettings.SIMULATION_STEP_SIZE);
        length = settings.get(BuiltInSettings.SIMULATION_DURATION);

        vehicles = new ArrayList<Vehicle>();
    }

    /**
     * Initializes the model. This includes setting the lane change info per
     * lane and destination, initializing the vehicle generation and RSUs of the
     * lanes and initializing controllers. This method needs to be called before
     * running the model.
     */
    public void init() {
        // Initialize lanes
        for (jLane lane : network) {
            lane.init();
        }

        // Initialize controllers
        for (jController controller : controllers) {
            controller.init();
        }
    }


    /**
     * Performs the main model loop. This entails the RSUs, OBUs, controllers,
     * vehicle generators and drivers (in this order).
     *
     * @param n Number of loops to be run before returning.
     */
    public void run(int n) {

        // loop n times
        int nn = 0;
        while ( (nn < n) && (t < length) ) {
            synchronized (this) {
                driverActionsPerCycle = new HashSet<jDriver>();
                LATCH = new CountDownLatch(vehicles.size());
            }
            long simCycleStartTime = System.nanoTime();
            /** Step 1. sense surroundings ... */
            sensingCycle();
            try {
                long agentsStartReactionWindow = System.nanoTime();
//                LATCH.await(1500L, TimeUnit.MILLISECONDS);
                LATCH.await();
                long agentsEndReactionWindow = System.nanoTime();
                if (agentReactionWindow.containsKey(vehicles.size())) {
                    int count = agentReactionCount.get(vehicles.size());
                    agentReactionWindow.put(vehicles.size(), (agentReactionWindow.get(vehicles.size()) * count + agentsEndReactionWindow - agentsStartReactionWindow)/(count + 1));
                    agentReactionCount.put(vehicles.size(), count+1);
                }
                else {
                    agentReactionWindow.put(vehicles.size(), agentsEndReactionWindow - agentsStartReactionWindow);
                    agentReactionCount.put(vehicles.size(),1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            /** Step 2. Collect Data... */
            dataCollectCycle();

            /** Step 3. Execute new actions ... */
            executionCycle();
            long simCycleEndTime = System.nanoTime();
            if (simulationCycle.containsKey(vehicles.size())) {
                int count = simulationCycleCount.get(vehicles.size());
                simulationCycle.put(vehicles.size(), (simulationCycle.get(vehicles.size()) * count + simCycleEndTime - simCycleStartTime)/(count + 1));
                simulationCycleCount.put(vehicles.size(), count +1);
            }
            else {
                simulationCycle.put(vehicles.size(), simCycleEndTime - simCycleStartTime);
                simulationCycleCount.put(vehicles.size(), 1);
            }
            // Update time
            step = step + 1; // time step number
            t = step * dt; // time [s]
            nn++;
        }

    }

    private void sensingCycle() {
        for (AbstractVehicle vehicle: vehicles) {
            vehicle.sense();
        }
    }

    private void dataCollectCycle() {
        for (AbstractVehicle vehicle: vehicles) {
            if (!vehicleLogs.containsKey(vehicle)) {
                vehicleLogs.put(vehicle, new VehicleLogBuffer((Vehicle) vehicle));
            }
            vehicleLogs.get(vehicle).log();
        }
    }

    private void executionCycle() {
        // Run road-side units
        for (jLane lane : network) {
            for (int j = 0; j < lane.RSUcount(); j++) {
                lane.getRSU(j).run();
            }
        }
        // Run on-board units
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).isEquipped()) {
                vehicles.get(i).OBU.run();
            }
        }
        // Run controllers
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).run();
        }

        // Vehicle generation
        for (jLane lane : network) {
            if (lane.getGenerator() != null) {
                lane.getGenerator().run();
            }
        }

        // Drive (set acceleration and lane change decisions)
        for (int i = 0; i < vehicles.size(); i++) {
            vehicles.get(i).driver.drive(); // sets a and dy
        }
        //Move
        for (int i = 0; i < vehicles.size(); i++) {
            vehicles.get(i).move(getStepSize()); // performs a and dy
        }

        // Check for collisions
        jSettings settings = jSettings.getInstance();
        if (settings.get(BuiltInSettings.DEBUG)) {
            Iterator<Vehicle> vehicleIterator = getVehicles().iterator();
            while(vehicleIterator.hasNext()) {
                Vehicle vehicle = vehicleIterator.next();
                if (vehicle.isCrashed()) {
                    vehicle.delete();
                    System.err.println("Collision: " + vehicle.getX() + "@" + vehicle.getLane().getId());
                }
            }
        }
    }

    public void logToConsole() {
        logger.debug("<<--jSim -->> Density/Flow ");
        for (int i=0; i<1; i++) {
            for(jLane lane: network) {
                String debug = "";
                if (lane.getGenerator() != null) {
                    if (lane.getGenerator() instanceof DensityGenerator) {
                        DensityGenerator densityGenerator = (DensityGenerator) lane.getGenerator();
                        debug += densityGenerator.getDensity();
                        if (lane.getRSU(i) instanceof jDetector) {
                            jDetector detector = (jDetector) lane.getRSU(i);
                            for (Integer flow: detector.qHist) {
                                debug += " " + flow;
                            }
                        }
                        logger.debug(debug);
                    }
                }

            }
            logger.debug("-------------------------");
        }
        logger.debug("<<--jSim -->> Density/Speed ");
        for (int i=0; i<1; i++) {
            for(jLane lane: network) {
                String debug = "";
                if (lane.getGenerator() != null) {
                    if (lane.getGenerator() instanceof DensityGenerator) {
                        DensityGenerator densityGenerator = (DensityGenerator) lane.getGenerator();
                        debug += densityGenerator.getDensity();
                        if (lane.getRSU(i) instanceof jDetector) {
                            jDetector detector = (jDetector) lane.getRSU(i);
                            for (double flow: detector.vHist) {
                                debug += " " + flow;
                            }
                        }
                        logger.debug(debug);
                    }
                }
            }
            logger.debug("-------------------------");
        }
    }

    private void logSnapshotTimes() {
        jSettings settings = jSettings.getInstance();
        File outputPath = new File(settings.get(BuiltInSettings.OUTPUT_PATH));
        outputPath.mkdirs();
        File times = new File(outputPath, "snapshotTimes");
        try {
            FileWriter fw = new FileWriter(times);
            for (jVehicle vehicle: vehicleLogs.keySet()) {
                String s = "" + vehicle.getDriver().getID();
                VehicleLogBuffer log = vehicleLogs.get(vehicle);
                double[] t = log.t();
                for (Double d: t) {
                    s += "," + d;
                }
                s += "\n";
                fw.write(s);
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void logGaps() {
        jSettings settings = jSettings.getInstance();
        File outputPath = new File(settings.get(BuiltInSettings.OUTPUT_PATH));
        outputPath.mkdirs();
        File gapsFile = new File(outputPath, "gaps");
        try {
            FileWriter fw = new FileWriter(gapsFile);
            for (jVehicle vehicle: vehicleLogs.keySet()) {
                String s = "" + vehicle.getDriver().getID();
                VehicleLogBuffer log = vehicleLogs.get(vehicle);
                double[] gaps = log.gaps();
                for (Double d: gaps) {

                    s += "," + d;
                }
                s += "\n";
                fw.write(s);
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void logAgentActions() {
        jSettings settings = jSettings.getInstance();
        File outputPath = new File(settings.get(BuiltInSettings.OUTPUT_PATH));
        outputPath.mkdirs();

        File accelerationsOutputFile = new File(outputPath, "accelerations");
        try {
            FileWriter fw = new FileWriter(accelerationsOutputFile);
            for (AbstractVehicle vehicle: vehicles) {
                fw.write("Vehicle=========" + vehicle.toString()+"\n");
                for (Double time: vehicle.accelerations.keySet()) {
                    fw.write(time + " " + vehicle.accelerations.get(time) + "\n");
                }
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void logPerformance() {
        jSettings settings = jSettings.getInstance();
        File outputPath = new File(settings.get(BuiltInSettings.OUTPUT_PATH));
        outputPath.mkdirs();
        File performanceOutputFile = new File(outputPath, "performance");
        try {
            FileWriter fw = new FileWriter(performanceOutputFile);
            fw.write("======== Simulation cycle =========\n");
            for (Integer numberOfAgents: simulationCycle.keySet()) {
                fw.write(numberOfAgents + " " + simulationCycle.get(numberOfAgents) + "\n");
            }
            fw.write("======== Reaction Window =========\n");
            for (Integer numberOfAgents: agentReactionWindow.keySet()) {
                fw.write(numberOfAgents + " " + agentReactionWindow.get(numberOfAgents) + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Adds a class to the model.
     *
     * @param cls Class to add.
     */
    public void addClass(jClass cls) {
        classes.add(cls);
    }

    /**
     * Returns the class with given id.
     *
     * @param id Id of requested class.
     * @return Class with given id.
     */
    public jClass getClass(int id) {
        for (int i = 0; i < classes.size(); i++) {
            if (classes.get(i).getId() == id) {
                return classes.get(i);
            }
        }
        return null;
    }

    public List<jClass> getClasses() {
        return classes;
    }

    /**
     * Adds a vehicle in the simulation.
     *
     * @param vehicle Vehicle to add.
     */
    public void addVehicle(Movable vehicle) {
        if (vehicle instanceof Vehicle) {
            vehicles.add((Vehicle) vehicle);
        }
    }

    /**
     * Removes a vehicle from the simulation.
     *
     * @param vehicle Vehicle to remove.
     */
    public void removeVehicle(Movable vehicle) {
        if (vehicle instanceof Vehicle) {
            vehicles.remove((Vehicle) vehicle);
        }
    }

    /**
     * @return Returns a copy list of the current vehicles in simulation.
     */
    public List<Vehicle> getVehicles() {
//        return c.deepClone(vehicles);
        return Lists.newArrayList(vehicles);
    }


    /**
     * Adds a controller to the model.
     *
     * @param controller Controller to add.
     */
    public void addController(jController controller) {
        controllers.add(controller);
    }

    /**
     * Derives the current absolute time of the simulation as being <tt>t</tt>
     * seconds after the given <tt>startTime</tt>. If no <tt>startTime</tt> is
     * given, <tt>null</tt> is returned.
     *
     * @return Absolute current time.
     */
    public Date currentTime() {
        return new Date(getAbsoluteT().longValue() * 1000);
    }

    /**
     * Stores all requested data. This may include all trajectories of vehicles
     * still in simulation and in the buffer and all detectors. Typically, this
     * method is called after the simulation has finished.
     */
    public void saveLogsToDisk() {
        // Store remaining vehicles
        jModel.LOGGING_THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                logger.debug("Saving Logs before exiting");
                logPerformance();

                logAgentActions();
                logger.debug("<<--jSim -->> Accelerations written to file");

                logGaps();
                logger.debug("<<--jSim -->> Vehicle Gaps written to file");

                logSnapshotTimes();
                logger.debug("<<--jSim -->> snapshot times written to file");

                jSettings settings = jSettings.getInstance();
                if (settings.get(BuiltInSettings.DEBUG_TRAJECTORY) &&
                    settings.get(BuiltInSettings.DEBUG_TRAJECTORY_OUTPUT_SERIALIZED_OBJECTS)) {
                    for (VehicleLogBuffer logBuffer : vehicleLogs.values()) {
                        logBuffer.saveTrajectoryLogToDisk();
                    }
                }
                if (settings.get(BuiltInSettings.DEBUG_DETECTOR) &&
                    settings.get(BuiltInSettings.DEBUG_DETECTOR_OUTPUT_SERIALIZED_OBJETCS)) {
                    // Store detector data
                    logToConsole();
                    for (jLane lane : network) {
                        for (AbstractRSU rsu : lane.getRSUs()) {
                            if (rsu instanceof jDetector) {
                                saveDetectorData((jDetector) rsu);
                            }
                        }
                    }
                }

            }
        });
    }

    /**
     * Saves detector data to disk in a file with the detector id in the name.
     *
     * @param detector Detector.
     */
    public void saveDetectorData(jDetector detector) {
        jDetectorData dd = new jDetectorData(detector);
        // Save detector to disk.
        try {
            jSettings settings = jSettings.getInstance();
            File f = new File(settings.get(BuiltInSettings.OUTPUT_PATH), "detectors");
            f.mkdir();
            FileOutputStream fos = new FileOutputStream(
                    settings.get(BuiltInSettings.OUTPUT_PATH) +
                    File.separator +
                    "detectors" +
                    File.separator +
                    "detector" + detector.id() + ".dat");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(dd);
            oos.close();
        } catch (Exception e) {
            System.err.println("Unable to write to file: " + e.getMessage());
        }
    }

    /**
     * @return Current simulation time [s].
     */
    public Double getT() {
        return t;
    }

    /**
     * Returns simulation time as time from epoch in [s]
     * @return
     */
    public Double getAbsoluteT() {
        return startTime + t;
    }

    /**
     * Returns the starting point of the simulation as seconds from epoch.
     * @return
     */
    public Double getStartTime() {
        return this.startTime;
    }
    private void setStartTime(double t) {
        this.startTime = t;
    }

    public Double getEndTime() {
        return startTime + length;
    }

    public double getStepSize() {
        return dt;
    }

    /**
     * @return Returns a copy of the current array of lanes (The network)
     */
    public jLane[] getNetwork() {
        return network;
    }

    public List<jController> getControllers() {
        return controllers;
    }

    /**
     * Returns the maximum allowed simulation length.
     * @return maximum allowed simulation length in seconds
     */
    public double getSimulationLength() {
        return length;
    }

    public boolean isSimulationFinished() {
        return getT() >= getSimulationLength();
    }

    public AbstractEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(AbstractEnvironment environment) {
        this.environment = environment;
    }
    
    public void setGui(jModelGUI gui) {this.gui = gui;}
    
    public jModelGUI getGui() {return this.gui;}
}