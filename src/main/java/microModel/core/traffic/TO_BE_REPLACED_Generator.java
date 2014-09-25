package microModel.core.traffic;

import microModel.core.driver.model.IDMPlus;
import microModel.core.jRoute;
import microModel.core.road.LongDirection;
import microModel.core.road.jLane;
import microModel.core.vehicle.Vehicle;
import microModel.core.vehicle.jClass;
import microModel.core.vehicle.jVehicle;
import microModel.jModel;
import microModel.random.ProbabilityDistribution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Standard vehicle generator with various types of vehicle generation.
 */
public class TO_BE_REPLACED_Generator extends AbstractQueuedTrafficGenerator {

    /** Current demand level [veh/h] */
    protected double demand = 0;

    /** Dynamic dymand as two columns with [t, demand]. May be <tt>null</tt>. */
    protected double[][] dynamicDemand;

    /** Interpolate or stepwise dynamic demand. */
    public boolean interpDemand = true;

    /** Distribution of headways. */
    protected Distribution dist;

    /** 
     * Static route probabilities. These may be changed dynamically by an 
     * external controller.
     */
    public double[] routeProb;

    /** Available routes from this generator. */
    public jRoute[] routes;

    /** Time when next vehicle will enter the link. */
    protected double tNext = 0;

    /** Next vehicle that will be placed on the lane. */
    protected Vehicle nextVehicle; // next vehicle

    /** 
     * Static class probabilities. These may be changed dynamically by an 
     * external controller. 
     */
    public Map<Integer, Double> classProbs = new HashMap<Integer, Double>(0);


    /** Total number of vehicles generated. */
    protected int generated = 0;

    /** Generation time of vehicles in case of predefined headway Distribution. */
    public double[] preTime;

    /** 
     * Class IDs of vehicles in case of predefined headway Distribution.
     */
    public int[] preClass;

    /** Speed of vehicles in case of predefined headway Distribution. */
    public double[] preVelocity; // velocities

    /** 
     * Route indeces (0, 1, 2, ...) of vehicles in case of predefined headway
     * Distribution.
     */
    public int[] preRoute; // routes
    
    /**
     * Array of probabilities of which the nth index will be the probability of
     * the nth created class in the containing jModel. If this array is not null
     * it will be used during initialization.
     */
    protected Double[] probabilities;

    public TO_BE_REPLACED_Generator(jLane lane) {
        super(lane);
        this.dist = Distribution.PREDEFINED;
    }

    /**
     * Constructor setting the lane and Distribution and linking the generator
     * with a lane and vice versa.
     * @param lane LaneType to generate vehicles on.
     * @param dist Headway Distribution.
     */
    // using this at the end is ok, generator is fully initialized
    public TO_BE_REPLACED_Generator(jLane lane, Distribution dist) {
        super(lane);
        this.dist = dist;
    }

    /**
     * Vehicle generation method. A new vehicle is genereted if appropiate.
     */
    public void control() {
        jModel model = jModel.getInstance();
        // Dynamic demand
        if (dynamicDemand!=null) {
            //double t = lane.model.t;
            int lastIndex = dynamicDemand.length-1;
            if (model.getT() >=dynamicDemand[lastIndex][0] && demand!=dynamicDemand[lastIndex][1]) {
                // set latest demand value
                setDemand(dynamicDemand[lastIndex][1]);
            } else if (model.getT() <dynamicDemand[lastIndex][0]) {
                // find index of latest demand
                int index = 0;
                while (model.getT() >=dynamicDemand[index][0]) {
                    index++;
                }
                index--; // subtract one as we found index of first upcoming demand
                if (interpDemand) {
                    // interpolate demand
                    double t1 = dynamicDemand[index][0];
                    double t2 = dynamicDemand[index+1][0];
                    double d1 = dynamicDemand[index][1];
                    double d2 = dynamicDemand[index+1][1];
                    double d;
                    if (d1!=d2) {
                        d = ((model.getT() -t1)*d2 + (t2- model.getT())*d1)/(t2-t1);
                    } else {
                        d = d1;
                    }
                    setDemand(d);
                } else if (demand!=dynamicDemand[index][1]) {
                    // update demand stepwise
                    setDemand(dynamicDemand[index][1]);
                }
            }
        }

        // Create vehicle(s)
        if (dist == Distribution.PREDEFINED) {
            // Generate vehicles from list
            while (tNext<= model.getT()) {
                // select class with right id
                jClass vehClass = model.getClass(preClass[generated]);
                // generate vehicle
                nextVehicle = vehClass.generateVehicle(0);
                nextVehicle.setSpeed(preVelocity[generated]);
                nextVehicle.getDriver().setRoute(routes[preRoute[generated]]);
                // put at lane
                nextVehicle.paste(getLane(), nextVehicle.getSpeed()*(model.getT() -tNext));
                model.addVehicle(nextVehicle);
                // see RSUs
                passRSUs(nextVehicle);
                // update status
                generated++;
                tNext = tNext+headway();
            }
        } else {
            // Regular vehicle generation
            // a vehicle is needed
            if (nextVehicle==null) {
                randomNextVehicle();
            }
            boolean success = true; // to have a first attempt
            while (queue.size() >0 && success) {
                // while there is a queue and the last vehicle could be
                // generated, attempt one more vehicle.
                success = addQueueVehicle();
            }
            while (tNext<= model.getT()) {
                // while tNext is in the past, generate new vehicle (or add to
                // queue).
                success = addFreeVehicle();
                tNext = tNext+headway(); // may be within the same time step
            }
        }
    }

    /** Empty, needs to be implemented.  */
    public void noControl() {}
    
    /**
     * Initializes vehicle generation.
     */
    public void init() {
        jModel model = jModel.getInstance();
        if (dist== Distribution.PREDEFINED && preTime!=null) {
            tNext = preTime[0];
        } else if (probabilities!=null) {
            for (int i=0; i<probabilities.length; i++) {
                classProbs.put(model.getClasses().get(i).getId(), probabilities[i]);
            }
        }
    }
    
    /**
     * Set the Distribution type.
     * @param dist Distribution type.
     */
    public void setDistibution(Distribution dist) {
        this.dist = dist;
    }

    /**
     * Private method to generate a free flowing vehicle. These are generated if
     * the acceleration >= 0. If the vehicle is not generated, it is added to
     * the queue.
     * @return Whether the vehicle could be generated.
     */
    protected boolean addFreeVehicle() {
        jModel model = jModel.getInstance();

        jLane genLane = getLane();
        jLane vehLane = genLane;
        boolean success = false;
        while (!success && genLane!=null) {
            // set speed of downstream vehicle, if any
            jVehicle down = genLane.findVehicle(0, LongDirection.DOWN);
            nextVehicle.paste(genLane, 0);
            double downX;
            if (down!=null) {
                nextVehicle.setSpeed(Math.min(down.getSpeed(), IDMPlus.updateDesiredVelocity(nextVehicle.getDriver())));
                downX = down.getX()+genLane.xAdj(down.getLane());
            } else {
                nextVehicle.setSpeed(IDMPlus.desiredEquilibriumHeadway(nextVehicle.driver));
                downX = Double.POSITIVE_INFINITY;
            }
            // paste at lane
            double x = nextVehicle.getSpeed()*(model.getT() -tNext);
            vehLane = genLane;
            while (x> vehLane.getL()) {
                // vehicle is generated beyond lane length
                x = x- vehLane.getL();
                vehLane = vehLane.getDown();
            }
            // make sure the vehicle is at the right location (was initialliy located at x=0)
            //nextVehicle.cut();
            //nextVehicle.paste(vehLane, x);
            // headway positive
            double s = 0;
            if (down!=null) {
                s = nextVehicle.getGap(down);
                // acceleration ok?
                if (s>=0) {
                    nextVehicle.driver.drive();
                }
            }
            // If there is no down, generate always, otherwise check acceleration
            if (down!=null && (s<0 || nextVehicle.getAcceleration()<0 || x>downX)) {
                genLane = null;
                nextVehicle.cut();
                nextVehicle.laneChangeProgress = 0;
                nextVehicle.setAcceleration(0);
            } else {
                success = true;
                model.addVehicle(nextVehicle);
                // see RSUs
                passRSUs(nextVehicle);
                randomNextVehicle(); // new next vehicle
            }
        }
        if (!success) {
            // try to add as queue vehicle
//            queue++;
            success = addQueueVehicle();
        }
        return success;
    }

    /**
     * Private method to generate a vehicle from queue. These are created at
     * their desired headway.
     * @return Whether the vehicle could be generated.
     */
    protected boolean addQueueVehicle() {
        jModel model = jModel.getInstance();

        jLane genLane = getLane();
        jLane vehLane = genLane;
        boolean success = false;
        while (!success && genLane!=null) {
            jVehicle down = genLane.findVehicle(0, LongDirection.DOWN);
            nextVehicle.paste(genLane, 0);
            double downX=0;
            double downL=0;
            if (down!=null) {
                downX = down.getX()+genLane.xAdj(down.getLane());
                nextVehicle.setSpeed(Math.min(down.getSpeed(), IDMPlus.updateDesiredVelocity(nextVehicle.driver)));
                downL = down.getLength();
            } else {
                System.err.println("Trying to generate queue vehicle without downstream vehicle.");
//                queue--;
            }
            if (downX-downL > IDMPlus.desiredEquilibriumHeadway(nextVehicle.driver)) {
                double x = downX-downL- IDMPlus.desiredEquilibriumHeadway(nextVehicle.driver);
                // check that x<=v*t            
                vehLane = genLane;
                while (x> vehLane.getL()) {
                    // vehicle is generated beyond lane length
                    x = x- vehLane.getL();
                    vehLane = vehLane.getDown();
                }
                // make sure the vehicle is at the right location (was initialliy located at x=0)
                //nextVehicle.cut();
                //nextVehicle.paste(vehLane, x);
                success = true;
                model.addVehicle(nextVehicle);
                // see RSUs
                passRSUs(nextVehicle);
                randomNextVehicle(); // new next vehicle
//                queue--;
            } else {
                genLane = null;
                nextVehicle.cut();
            }
        }
        return success;
    }

    /**
     * Private method to set a random new vehicle.
     */
    protected void randomNextVehicle() {
        // select a random class
        nextVehicle = randomClass().generateVehicle(0);
        // give random destination
        double r = ProbabilityDistribution.RANDOM.nextDouble();
        double lowerLim = 0;
        int routeInd = 0;
        while (lowerLim+routeProb[routeInd] < r) {
            lowerLim = lowerLim+routeProb[routeInd];
            routeInd++;
        }
        nextVehicle.getDriver().setRoute(routes[routeInd]);
    }

    /**
     * Updates the current demand value. The time of the next vehicle is adjusted.
     * @param dem New demand level [veh/h].
     */
    protected void setDemand(double dem) {
        /*
         * A certain time of the headway between tNext and t (now) remains.
         * Adjust this time with a fraction of demand/dem, i.e. larger demand is
         * smaller remaining time.
         */
        jModel model = jModel.getInstance();

        if (tNext>= model.getT()) {
            if (dem>0 && demand>0) {
                double f = demand/dem;
                demand = dem;
                tNext = model.getT() + (tNext- model.getT())*f;
            } else if (dem>0) {
                // demand was zero, start at random headway
                demand = dem;
                tNext = model.getT() + ProbabilityDistribution.RANDOM.nextDouble()*headway();
            } else {
                // demand will be zero
                demand = dem;
                tNext = Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
     * Returns the dynamic demand array.
     * @return Demand array.
     */
    public double[][] getDemand() {
        return dynamicDemand;
    }
    
//    /**
//     * Returns the queue size of the generator, which is the number of vehicles
//     * that could not be genererated, but will as soon as possible.
//     * @return Number of vehicles in queue.
//     */
//    public int getQueue() {
//        return queue;
//    }

    /**
     * Sets the dynamic demand. A value may be appended to cover the time after
     * the last given demand value.
     * @param dem Dynamic demand as two columns [t, demand].
     */
    public void setDemand(double[][] dem) {
        jModel model = jModel.getInstance();

        if (dem[dem.length-1][0] <= model.getEndTime()) {
            // log an infinite time value
            double[][] dem2 = new double[dem.length+1][2];
            // first copy data
            for (int i=0; i<dem.length; i++) {
                dem2[i][0] = dem[i][0];
                dem2[i][1] = dem[i][1];
            }
            // appended value (constant demand after last given time)
            dem2[dem.length][0] = model.getEndTime();
            dem2[dem.length][1] = dem[dem.length-1][1];
            dynamicDemand = dem2;
        } else {
            dynamicDemand = dem;
        }
    }

    /**
     * Determines a headway value based on generator settings.
     * @return Headway [s].
     */
    public double headway() {
        double headway = 0;
        if (dist== Distribution.PREDEFINED) {
            if (generated>=preTime.length) {
                // all vehicles were generated
                headway = Double.POSITIVE_INFINITY;
            } else if (generated==0) {
                // first vehicle
                headway = preTime[0];
            } else {
                // headway is time difference between 2 consecutive vehicles
                headway = preTime[generated] - preTime[generated-1];
            }
        } else {
            if (demand>0) {
                double dt = 3600/demand; // average headway
                if (dist== Distribution.UNIFORM) {
                    // always the average headway
                    headway = dt;
                } else if (dist== Distribution.EXPONENTIAL) {
                    // note: r = -log(uniform)/gamma & mean = 1/gamma
                    headway = -Math.log(ProbabilityDistribution.RANDOM.nextDouble()) * dt;
                }
            } else {
                // no demand
                headway = Double.POSITIVE_INFINITY;
            }
        }
        return headway;
    }

    /**
     * Randomly selects a class given generator specific class probabilities.
     * @return Randomly selected class.
     */
    public jClass randomClass() {
        jModel model = jModel.getInstance();

        double r = ProbabilityDistribution.RANDOM.nextDouble();
        double lowerLim = 0;
        Iterator<Integer> inter = classProbs.keySet().iterator();
        Integer id = inter.next();
        while (inter.hasNext() && lowerLim+classProbs.get(id) < r) {
            lowerLim = lowerLim+classProbs.get(id);
            id = inter.next();
        }
        return model.getClass(id);
    }

    /**
     * Passes RSUs that are upstream of the location where a vehicle was
     * generated.
     * @param veh Generated vehicle.
     */
    protected void passRSUs(Vehicle veh) {
        // upstream lanes (if any)
        jLane l = veh.getLane().getUp();
        while (l!=null) {
            for (int i=0; i<l.RSUcount(); i++) {
                if (l.getRSU(i).passable) {
                    l.getRSU(i).see(veh);
                }
            }
            l = l.getUp();
        }
        // lane itself
        for (int i=0; i< veh.getLane().RSUcount(); i++) {
            if (veh.getLane().getRSU(i).getX() <= veh.getX() && veh.getLane().getRSU(i).passable) {
                veh.getLane().getRSU(i).see(veh);
            }
        }
    }
    
    /**
     * Convenience method that sets the nth element of <tt>probabilities</tt> as
     * the probability of the nth class that was or will be constructed for the 
     * containing <tt>jModel</tt>. Note: the actual work happens during 
     * initialization. This method simply stores the array.
     * @param probabilities Class probabilities.
     */
    public void setClassProbabilities(Double[] probabilities) {
        this.probabilities = probabilities;
    }

    /** Enumeration of possible headway distributions. */
    public enum Distribution {
        /** Headways as given by pre-defined arrival time array. */
        PREDEFINED, 
        /** 
         * Exponential Distribution. Very short headways may result. The default
         * vehicle generation is able to deal with this by delaying vehicle 
         * generation and generating a queued vehicle (at following headway).
         */
        EXPONENTIAL, 
        /** Vehicles are uniformely spread over time (fixed headway). */
        UNIFORM;
    }
}