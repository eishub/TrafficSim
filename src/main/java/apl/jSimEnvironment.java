package apl;

import GUI.ImageBackdrop;
import GUI.vehicle.TimeToCollision;
import GUI.jModelGUI;
import apl.A16.DynamicDemandGenerator;
import apl.AccelerationComparison.BlockedVehicleGenerator;
import apl.CarFollowing.BlockedDensityGenerator;
import apl.CarFollowing.BlockedMixedDensityGenerator;
import apl.CarFollowing.DensityGenerator;
import apl.CarFollowing.MixedDensityGenerator;
import apl.Merging.TimeHeadwayGenerator;
import com.google.common.collect.Lists;
import eis.eis2java.environment.AbstractEnvironment;
import eis.exceptions.ManagementException;
import eis.iilang.*;
import microModel.core.jRoute;
import microModel.core.road.device.jDetector;
import microModel.core.road.jLane;
import microModel.jModel;
import microModel.map.KMLImporter;
import microModel.map.road.RoadSegment;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import microModel.util.DetectorDataUtilities;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

public class jSimEnvironment extends AbstractEnvironment {

    private enum Scenario {
        A16,
        MERGING,
        DENSITY_FLOW,
        DENSITY_FLOW_BLOCKED,
        MIXED_TRAFFIC,
        MIXED_TRAFFIC_BLOCKED,
        ACCELERATION,
        SCALABILITY;
    }

    public static int VEHICLE_COUNTER = 0;

    public static void main(String... args) {
        String sc = args[0];
        new jSimEnvironment().loadScenario(Scenario.valueOf(sc));
    }

    public jSimEnvironment() {
        // Setup log4j for debug logs.
        try {
            VEHICLE_COUNTER = 0;
            URL log4jProperties = getClass().getResource("/resources/log4j.properties");
            PropertyConfigurator.configure(log4jProperties.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadScenario(Scenario sc) {
        switch (sc) {
            case A16: setupA16(); break;
            case MERGING: setupMergingScenario(); break;
            case DENSITY_FLOW: setupFlowDensityScenario(); break;
            case DENSITY_FLOW_BLOCKED: setupBlockedFlowDensityScenario(); break;
            case MIXED_TRAFFIC: setupMixedTrafficScenario(); break;
            case MIXED_TRAFFIC_BLOCKED: setupBlockedMixedTrafficScenario(); break;
            case ACCELERATION: setupAccelerationComparison(); break;
            case SCALABILITY: setupScalabilityScenario(); break;
            default: break;
        }
    }


    @Override
    public void init(Map<String, Parameter> parameters) throws ManagementException {
        super.init(parameters);
        try {
            setState(EnvironmentState.PAUSED);
            Parameter p;
            // initialize settings of the simulation.
            for (microModel.core.Parameter setting: BuiltInSettings.PARAMETERS) {
                if (parameters.containsKey(setting.name())) {
                    p = parameters.get(setting.name());
                    if (p instanceof Identifier) {
                        Identifier i = (Identifier) p;
                        jSettings.getInstance().put(setting, i.getValue());
                    }
                    if (p instanceof Numeral) {
                        Numeral n = (Numeral) p;
                        jSettings.getInstance().put(setting, n.getValue());
                    }
                    if (p instanceof TruthValue) {
                        TruthValue t = (TruthValue) p;
                        jSettings.getInstance().put(setting, t.getValue());
                    }
                }
            }

            // run the scenario that was specified.
            p = parameters.get("scenario");
            if (p instanceof Identifier) {
                Identifier sc = (Identifier) p;
                loadScenario(Scenario.valueOf(sc.getValue()));
            }

        } catch (ManagementException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean isSupportedByEnvironment(Action action) {
        return true;
    }

    @Override
    protected boolean isSupportedByType(Action action, String s) {
        return true;
    }

    private void setupA16() {
        jSettings.getInstance().put(BuiltInSettings.DETECTOR_INFO_FILE_PATH, "/resources/detector_data/detectors.info");
        jSettings.getInstance().put(BuiltInSettings.DETECTOR_DATA_FILE_PATH, "/resources/detector_data/detectors.data");
        jSettings.getInstance().put(BuiltInSettings.IMPORT_DETECTOR_FROM_KML, Boolean.TRUE);
        jSettings.getInstance().put(BuiltInSettings.IMPORT_ORIGIN_FROM_KML, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/A16.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();

        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }
        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }

        Date startTime = Calendar.getInstance().getTime();
        try {
            startTime = DetectorDataUtilities.TIMESTAMP.parse("2009-05-13 15:00:00");
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(startTime.getTime()/1000);
        model.setEnvironment(this);

        for (RoadSegment rs: network.values()) {
            if (jSettings.getInstance().get(BuiltInSettings.IMPORT_DETECTOR_FROM_KML)) {
                rs.setupDetectors();
            }
            if (jSettings.getInstance().get(BuiltInSettings.IMPORT_ORIGIN_FROM_KML)) {
                List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{3})});
                AgentDriverGenerator agentDriverGenerator = new AgentDriverGenerator(routes);
                DynamicDemandGenerator.Builder builder = new DynamicDemandGenerator.Builder(agentDriverGenerator).withEnvironment(this);
                rs.setupGenerators(builder);
            }
        }

        model.init();

        jModelGUI gui = new jModelGUI();
        gui.addBackdrop(new ImageBackdrop("/resources/backdrop.png", -38, -963, 3769, 3769 / 5));
        gui.addPopupItem("Show backdrop", true);
        gui.addVehicleColor(new TimeToCollision());

    }

    private void setupMergingScenario() {
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/merge.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }


        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        model.init();

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        generateMergingScenarioHighwayFlow();
    }

    private void generateMergingScenarioHighwayFlow() {
        jModel model = jModel.getInstance();
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});
        ArrayList<Integer> laneIds = Lists.newArrayList(new Integer[]{0, 1, 2});
        for (jLane lane: model.getNetwork()) {
            if (laneIds.contains(lane.getId())) {
                if (lane.getId() == 0) {
                    new TimeHeadwayGenerator(this, lane, routes, 3, 30.55);
                }
                else if (lane.getId() == 1) {
                    new TimeHeadwayGenerator(this, lane, routes, 2, 30.55);
                }
                else {
                    new TimeHeadwayGenerator(this, lane, routes, 6, 20);
                }
            }
        }
    }


    private void setupFlowDensityScenario() {
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/16-Normal-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        oneGeneratorPerLane(20);
    }

    private void oneGeneratorPerLane(int count) {
        jModel model = jModel.getInstance();

        int laneCounter = 0;
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});
        for (jLane lane: model.getNetwork()) {
            DensityGenerator driverGenerator = new DensityGenerator(this, lane, routes, count);
//            driverGenerator.setGap(20 +  10 * laneCounter++);
            driverGenerator.setDensity(0.005 + 0.005 * laneCounter++);
        }
        //Put detectors at the end of all lanes
        laneCounter = 0;
        for (jLane lane: model.getNetwork()) {
            jDetector d = new jDetector(lane, lane.getL()/10 , jSettings.getInstance().get(BuiltInSettings.DETECTOR_PERIOD), laneCounter++);
            d.init();
            lane.addRSU(d);
            lane.addObserver(d);
        }
    }

    private void setupBlockedFlowDensityScenario() {
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/16-Normal-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        oneBlockingGeneratorPerLane(20);
    }

    private void oneBlockingGeneratorPerLane(int count) {
        jModel model = jModel.getInstance();

        int laneCounter = 0;
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});
        for (jLane lane: model.getNetwork()) {
            DensityGenerator driverGenerator = new BlockedDensityGenerator(this, lane, routes, count);
//            driverGenerator.setGap(20 +  10 * laneCounter++);
            driverGenerator.setDensity(0.005 + 0.005 * laneCounter++);
        }
        //Put detectors at the end of all lanes
        laneCounter = 0;
        for (jLane lane: model.getNetwork()) {
            jDetector d = new jDetector(lane, lane.getL()/10 , jSettings.getInstance().get(BuiltInSettings.DETECTOR_PERIOD), laneCounter++);
            d.init();
            lane.addRSU(d);
            lane.addObserver(d);
        }
    }

    private void setupMixedTrafficScenario() {
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/16-Normal-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        oneMixedGeneratorPerLane(20);

    }

    private void oneMixedGeneratorPerLane(int count) {
        jModel model = jModel.getInstance();

        int laneCounter = 0;
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});
        for (jLane lane: model.getNetwork()) {
            MixedDensityGenerator driverGenerator = new MixedDensityGenerator(this, lane, routes, count);
//            driverGenerator.setGap(20 +  10 * laneCounter++);
            driverGenerator.setDensity(0.005 + 0.005 * laneCounter++);
        }
        //Put detectors at the end of all lanes
        laneCounter = 0;
        for (jLane lane: model.getNetwork()) {
            jDetector d = new jDetector(lane, lane.getL()/10 , jSettings.getInstance().get(BuiltInSettings.DETECTOR_PERIOD), laneCounter++);
            d.init();
            lane.addRSU(d);
            lane.addObserver(d);
        }
    }

    private void setupBlockedMixedTrafficScenario() {
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/resources/16-Normal-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        oneBlockedMixedGeneratorPerLane(20);

    }

    private void oneBlockedMixedGeneratorPerLane(int count) {
        jModel model = jModel.getInstance();

        int laneCounter = 0;
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});
        for (jLane lane: model.getNetwork()) {
            BlockedMixedDensityGenerator driverGenerator = new BlockedMixedDensityGenerator(this, lane, routes, count);
//            driverGenerator.setGap(20 +  10 * laneCounter++);
            driverGenerator.setDensity(0.005 + 0.005 * laneCounter++);
        }
        //Put detectors at the end of all lanes
        laneCounter = 0;
        for (jLane lane: model.getNetwork()) {
            jDetector d = new jDetector(lane, lane.getL()/10 , jSettings.getInstance().get(BuiltInSettings.DETECTOR_PERIOD), laneCounter++);
            d.init();
            lane.addRSU(d);
            lane.addObserver(d);
        }
    }

    private void setupAccelerationComparison() {
        //This will cause the vehicle accelerations to be recorded.
        jSettings.getInstance().put(BuiltInSettings.DEBUG_MODEL, Boolean.TRUE);

        KMLImporter imp = new KMLImporter("/resources/2-Normal-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addVehicleColor(new TimeToCollision());

        generateBlockedVehicles();

    }

    private void generateBlockedVehicles() {
        jModel model = jModel.getInstance();

        int laneCounter = 0;
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{1})});

        new BlockedVehicleGenerator(this, model.getNetwork()[0], routes, AgentDriver.TYPE);

        new BlockedVehicleGenerator(this, model.getNetwork()[1], routes, "IDM");

    }

    private void setupScalabilityScenario() {
        KMLImporter imp = new KMLImporter("/resources/A16.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        jModel model = new jModel.Builder(lanes.toArray(new jLane[0])).build(0);
        model.setEnvironment(this);

        jModelGUI gui = new jModelGUI();
        gui.addBackdrop(new ImageBackdrop("/resources/backdrop.png", -38, -963, 3769, 3769 / 5));
        gui.addPopupItem("Show backdrop", true);
        gui.addVehicleColor(new TimeToCollision());

        generateMultipleVehicles(200, 0.1);

    }

    private void generateMultipleVehicles(int count, double densityFactor) {
        jModel model = jModel.getInstance();
        jLane lane1 = model.getNetwork()[0];
        jLane lane2 = model.getNetwork()[1];

        //Put detectors at the end of all lanes
        jLane tmpLane = lane1;
        int id = 0;
        while(tmpLane != null) {
            jDetector d = new jDetector(tmpLane, tmpLane.getL() - 5, 60, id++);
            d.init();
            tmpLane.addRSU(d);
            tmpLane.addObserver(d);
            tmpLane = tmpLane.getDown();
        }
        List<jRoute> routes = Arrays.asList(new jRoute[]{new jRoute(new int[]{3})});
        DensityGenerator driverGenerator1 = new DensityGenerator(this, lane1, routes, count);
        driverGenerator1.setGap(100.0);
        DensityGenerator driverGenerator2 = new DensityGenerator(this, lane2, routes, count);
        driverGenerator2.setGap(100.0);
    }

	@Override
	public void kill() throws ManagementException {
		// TODO Auto-generated method stub
		super.kill();
		jModel.getInstance().getGui().forceClose();
	}
    
    

}
