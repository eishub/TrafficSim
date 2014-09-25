package microModel.core.road;

import com.vividsolutions.jts.geom.Coordinate;
import microModel.core.traffic.AbstractQueuedTrafficGenerator;
import microModel.core.observation.jObservable;
import microModel.core.observation.jObserver;
import microModel.core.vehicle.*;
import microModel.core.road.device.AbstractRSU;
import microModel.map.CoordinateUtilities;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * A single lane stretch of road. Different lanes are connected in the
 * longitudinal or lateral direction. The <tt>jLane</tt> object also provides a
 * few network utilities to find vehicles and to get the longitudinal distance
 * between lanes.
 */
public class jLane implements jObserver, jObservable {
    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(jLane.class);

    /** List of observers subscribing state changes of this lane */
    private List<jObserver> observers = new ArrayList<jObserver>();

    /** The type of the lane. */
    private final LaneType type;

    /** Array of coordinates defining the lane curvature. */
    private List<Coordinate> coordinates;

    /** Array of x-coordinates defining the lane curvature. */
    private double[] x;

    /** Array of y-coordinates defining the lane curvature. */
    private double[] y;

    /** ID of lane for user recognition. */
    private final Integer id;

    /** Length of the lane [m]. */
    private Double l;

    /** Downstream lane that is a taper (if any). */
    private jLane taper;

    /** Upstream lane (if any). */
    private jLane up;

    /** Downstream lane (if any). */
    private jLane down;

    /** Left lane (if any). */
    private jLane left;

    /** Right lane (if any). */
    private jLane right;

    /** Whether one can change to the left lane. */
    private boolean goLeft;

    /** Whether one can change to the right lane. */
    private boolean goRight;

    /** Set of RSUs, ordered by position. */
    private List<AbstractRSU> RSUs = new ArrayList<AbstractRSU>();

    /** All movables on this lane, in sorted order of position. */
    private List<AbstractVehicle> vehicles = new ArrayList<AbstractVehicle>(0);

    /** Destination number, 0 if no destination. */
    private Integer destination = 0;

    /** Legal speed limit. */
    private double vLim = 120;

    /**
     * Number of lane changes to be performed from this lane towards a certain
     * destination number. This is automatichally filled with the model
     * initialization.
     */
    private Map<Integer, Integer> lanechanges = new HashMap<Integer, Integer>();

    /**
     * Distance [m] in which lane changes have to be performed towards a certain
     * destination number. This is automatichally filled with the model
     * initialization.
     */
    private Map<Integer, Double> endpoints = new HashMap<Integer, Double>();

    /** Vehicle generator, if any. */
    private AbstractQueuedTrafficGenerator generator;

    /**
     * Set of calculated x adjustments with longitudinally linked lanes. These
     * will be calculated and stored as needed.
     */
    private Map<Integer, Double> xAdjust = new HashMap<Integer, Double>();

    public static class Builder {
        private LaneType type;

        public Step1LaneBuilder withType(LaneType type) {
            return new Step1LaneBuilder(type);
        }

        public static class Step1LaneBuilder {
            private LaneType type;

            private Step1LaneBuilder(LaneType type) {
                this.type = type;
            }

            public Step2LaneBuilder withID(int id) {
                return new Step2LaneBuilder(type, id);
            }

            public static class Step2LaneBuilder {
                private LaneType type;
                private int id;

                private Step2LaneBuilder(LaneType type, int id) {
                    this.type = type;
                    this.id = id;
                }

                public Step3LaneBuilder withX(double[] x) {
                    return new Step3LaneBuilder(type, id, x);
                }

                public static class Step3LaneBuilder {
                    private LaneType type;
                    private int id;
                    private double[] x;

                    private Step3LaneBuilder(LaneType type, int id, double[] x) {
                        this.type = type;
                        this.id = id;
                        this.x = x;
                    }

                    public Step4LaneBuilder withY(double[] y) {
                        return new Step4LaneBuilder(type, id, x, y);
                    }

                    public static class Step4LaneBuilder {
                        private LaneType type;
                        private int id;
                        private double x[];
                        private double y[];

                        private Step4LaneBuilder(LaneType type, int id, double[] x, double[] y) {
                            this.type = type;
                            this.id = id;
                            this.x = x;
                            this.y = y;
                        }

                        public jLane build() {
                            return new jLane(type, x, y, id);
                        }

                    }
                }
            }
        }
    }

    /**
     * Constructor that will calculate the lane length from the x and y
     * coordinates.
     * @param x X coordinates of curvature.
     * @param y Y coordinates of curvature.
     * @param id User recognizable lane id.
     */
    private jLane(LaneType type, double[] x, double[] y, int id) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.id = id;
        coordinates = CoordinateUtilities.combine(this.x, this.y);
        calculateLength();
    }
    
    /**
     * Sets the lane length based on the x and y coordinates. This method is 
     * called within the constructor and should only be used if coordinates are
     * changed afterwards (for instance to nicely connect lanes at the same 
     * point).
     */
    public void calculateLength() {
        // compute and set length
        double cumLength = 0;
        double dx;
        double dy;
        for (int i=1; i<=x.length-1; i++) {
            dx = this.x[i]-this.x[i-1];
            dy = this.y[i]-this.y[i-1];
            cumLength = cumLength + Math.sqrt(dx*dx + dy*dy);
        }
        l = cumLength;
    }

    /**
     * Initializes lane change info, taper presence, vehicle generation, RSUs.
     */
    public void init() {

        initLaneChangeInfo();
        if (taper==this) {
            jLane upLane = up;
            while (upLane!=null && upLane.left!=null) {
                upLane.taper = this;
                upLane = upLane.up;
            }
        }
        if (generator!=null) {
            generator.init();
        }
        for (int i=0; i<RSUs.size(); i++) {
            RSUs.get(i).init();
        }
    }
    
    /**
     * Initializes the lane change info throughout the network for a possible 
     * destination of this lane.
     */
    public void initLaneChangeInfo() {
        if (destination>0 && !leadsTo(destination)) {
            // find all lanes in cross section with same destination and set initial info
            ArrayList<jLane> curlanes = new ArrayList();
            curlanes.add(this);
            lanechanges.put(destination, 0);
            endpoints.put(destination, l);
            jLane lane = left;
            while (lane!=null && lane.destination==destination) {
                curlanes.add(lane);
                lane.lanechanges.put(destination, 0);
                lane.endpoints.put(destination, lane.l);
                lane = lane.left;
            }
            lane = right;
            while (lane!=null && lane.destination==destination) {
                curlanes.add(lane);
                lane.lanechanges.put(destination, 0);
                lane.endpoints.put(destination, lane.l);
                lane = lane.right;
            }
            // move through network and set lane change information
            while (!curlanes.isEmpty()) {
                // move left
                int n = curlanes.size();
                for (int i=0; i<n; i++) {
                    jLane curlane = curlanes.get(i);
                    Integer lcs = 0;
                    while (curlane.left!=null && curlane.left.goRight && 
                            !curlanes.contains(curlane.left) &&
                            !curlane.left.lanechanges.containsKey(destination)) {
                        // left lane is not in current set and has not been covered yet
                        lcs = lcs+1; // additional lane change required
                        curlanes.add(curlane.left); // add to current set
                        curlane.left.lanechanges.put(destination, lcs); // set # of lane changes
                        curlane.left.endpoints.put(destination, curlane.left.l);
                        curlane = curlane.left; // next left lane
                    }
                }
                // move right
                for (int i=0; i<n; i++) {
                    jLane curlane = curlanes.get(i);
                    Integer lcs = 0;
                    while (curlane.right!=null && curlane.right.goLeft && 
                            !curlanes.contains(curlane.right) &&
                            !curlane.right.lanechanges.containsKey(destination)) {
                        // right lane is not in current set and has not been covered yet
                        lcs = lcs+1; // additional lane change required
                        curlanes.add(curlane.right); // add to current set
                        curlane.right.lanechanges.put(destination, lcs); // set # of lane changes
                        curlane.right.endpoints.put(destination, curlane.right.l);
                        curlane = curlane.right; // next right lane
                    }
                }
                // move upstream
                ArrayList<jLane> uplanes = new ArrayList<jLane>();
                for (int i=0; i<curlanes.size(); i++) {
                    jLane curlane = curlanes.get(i);
                    if (curlane.up!=null && (!curlane.up.lanechanges.containsKey(destination) 
                            || curlane.up.lanechanges.get(destination)>curlane.lanechanges.get(destination)) ) {
                        // upstream lane is not covered yet or can be used with less lane changes
                        uplanes.add(curlane.up); // add to uplanes
                        // copy number of lane changes
                        curlane.up.lanechanges.put(destination, curlane.lanechanges.get(destination));
                        // increase with own length
                        curlane.up.endpoints.put(destination, curlane.endpoints.get(destination)+curlane.up.l);
                    }
                }
                // set curlanes for next loop
                curlanes = uplanes;
            }
        }
    }

    /**
     * Add RSU to lane. RSUs are ordered by position.
     * @param rsu
     */
    public void addRSU(AbstractRSU rsu) {
        RSUs.add(rsu);
        Collections.sort(RSUs, new Comparator<AbstractRSU>() {
            @Override
            public int compare(AbstractRSU o1, AbstractRSU o2) {
                if (o1.getX() < o2.getX())
                    return -1;
                else if (o1.getX() == o2.getX())
                    return 0;
                else
                    return 1;
            }
        });
    }

    /**
     * Removes RSU from this lane.
     * @param rsu RSU to remove.
     */
    public void removeRSU(AbstractRSU rsu) {
        RSUs.remove(rsu);
    }

    /**
     * Returns the number of RSUs at this lane.
     * @return Number of RSUs.
     */
    public int RSUcount() {
        return RSUs.size();
    }

    /**
     * Returns the RSU at the given index.
     * @param index Index of requested RSU.
     * @return RSU at index.
     */
    public AbstractRSU getRSU(int index) {
        return RSUs.get(index);
    }

    /**
     * Returns the ID of the lane.
     * @return ID of the lane.
     */
    public int id() {
        return id;
    }

    /**
     * Finds a movable beginning at some location and moving either up- or
     * downstream.
     * @param x Start location [m] for the search.
     * @param updown Whether to search up or downstream.
     * @return Found movable.
     */
    public jVehicle findVehicle(double x, LongDirection updown) {
        jVehicle veh = null;
        if (updown== LongDirection.UP) {
            // if there are vehicles on the lane, pick any vehicle
            if (!vehicles.isEmpty()) {
                veh = vehicles.get(0);
            }
            // search for upstream lane with vehicles
            else {
                jLane j = up;
                while (j!=null && j.vehicles.isEmpty()) {
                    j = j.up;
                }
                // pick any vehicle
                if (j!=null) {
                    veh = j.vehicles.get(0);
                }
            }
            // search up/downstream to match x
            if (veh != null) {
                while (veh.getVehicle(Enclosure.DOWNSTREAM) != null && veh.getVehicle(Enclosure.DOWNSTREAM).getX() + xAdj(veh.getVehicle(Enclosure.DOWNSTREAM).getLane()) <= x) {
                    veh = veh.getVehicle(Enclosure.DOWNSTREAM);
                }
                while (veh != null && veh.getX() + xAdj(veh.getLane()) > x) {
                    veh = veh.getVehicle(Enclosure.UPSTREAM);
                }
            }
        } else if (updown== LongDirection.DOWN) {
            // if there are vehicle on the lane, pick any vehicle
            if (!vehicles.isEmpty()) {
                veh = vehicles.get(0);
            }
            // search for downstream lane with vehicles
            else {
                jLane j = down;
                while (j!=null && j.vehicles.isEmpty()) {
                    j = j.down;
                }
                // pick any vehicle
                if (j!=null) {
                    veh = j.vehicles.get(0);
                }
            }
            // search up/downstream to match x
            if (veh != null) {
                while (veh.getVehicle(Enclosure.UPSTREAM) != null && veh.getVehicle(Enclosure.UPSTREAM).getX() + xAdj(veh.getVehicle(Enclosure.UPSTREAM).getLane()) >= x) {
                    veh = veh.getVehicle(Enclosure.UPSTREAM);
                }
                while (veh != null && veh.getX() + xAdj(veh.getLane()) < x) {
                    veh = veh.getVehicle(Enclosure.DOWNSTREAM);
                }
            }
        }
        return veh;
    }

    /**
     * Finds the first noticeable RSU downstream of a location within a certain
     * range.
     * @param x Start location of search [m].
     * @param range Range of search [m].
     * @return Next noticeable RSU.
     */
    public AbstractRSU findNoticeableRSU(double x, double range) {
        jLane atLane = this;
        double searchRange = 0;
        while (atLane!=null && searchRange<=range) {
            // Loop all RSUs on this lane
            for (int i=0; i<atLane.RSUcount(); i++) {
                if (atLane.getRSU(i).noticeable && xAdj(atLane)+ atLane.getRSU(i).getX() >x
                        && xAdj(atLane)+ atLane.getRSU(i).getX() -x<=range) {
                    return atLane.getRSU(i);
                }
                // Update search range and quit if possible
                searchRange = xAdj(atLane)+ atLane.getRSU(i).getX() -x;
                if (searchRange>range) {
                    return null;
                }
            }
            // If no noticable RSUs, move to next lane
            atLane = atLane.down;
            // Update searchrange at start of new lane
            searchRange = xAdj(atLane)-x;
        }
        return null;
    }

    /**
     * Finds the adjustment required to compare positions of two objects on
     * different but longitudinally connected lanes. A value is returned that 
     * can be added to the position of an object at <tt>otherLane</tt> to get
     * the appropriate position from the start of this lane. Note that the value
     * should always be added, no matter if <tt>otherLane</tt> is up- or 
     * downstream, as negative adjustments may be returned. If the two lanes
     * are not up- or downstream from one another, 0 is returned.
     * @param otherLane jLane from which the adjustment is required.
     * @return Distance [m] to other lane.
     */
    public double xAdj(jLane otherLane) {
        double dx = 0;
        if (otherLane!=this && otherLane!=null) {
            if (xAdjust.containsKey(otherLane.id)) {
                // Get dx from xAdjust
                dx = xAdjust.get(otherLane.id);
            } else {
                // Calculate dx and store in xAdjust
                boolean found = false;
                // search downstream
                jLane j = this;
                while (j != null && !found) {
                    // increase downstream distance
                    dx = dx + j.l;
                    if (j.down == otherLane) {
                        // lane found
                        found = true;
                    }
                    j = j.down;
                }
                // not found, search upstream
                if (!found) {
                    dx = 0;
                    j = this;
                    while (j != null && !found) {
                        // reduce upstream distance
                        if (j.up != null) {
                            dx = dx - j.up.l;
                        }
                        if (j.up == otherLane) {
                            found = true;
                        }
                        j = j.up;
                    }
                }
                if (!found) {
                    dx = 0;
                }
                xAdjust.put(otherLane.id, dx);
            }
        }
        return dx;
    }
    
    /**
     * Checks whether two <tt>jLane</tt>s are in the same physical lane, e.g.
     * <tt>true</tt> if the two <tt>jLane</tt>s are downstream or upstream of 
     * one another.
     * @param otherLane The other <tt>jLane</tt>.
     * @return <tt>true</tt> of the lanes are in the same physical lane.
     */
    public boolean isSameLane(jLane otherLane) {
        if (otherLane==this) {
            return true;
        } else if (otherLane==null) {
            return false;
        } else {
            return xAdj(otherLane)!=0;
        }
    }

    /**
     * Returns the speed limit as m/s.
     * @return Speed limit [m/s]
     */
    public double getVLimInMetersPerSecond() {
        return vLim/3.6;
    }

    /**
     * Returns the location of x on an adjacent lane keeping lane length 
     * difference and curvature in mind. If either lane change is possible the 
     * lanes are physically adjacent and it is assumed that curvature of both 
     * lanes is defined in adjacent straight sub-sections. If neither lane 
     * change is possible, the lanes may not be physically adjacent and only 
     * total length is considered.
     * @param x Location on this lane [m].
     * @param dir Left or right.
     * @return Adjacent location [m].
     */
    public double getAdjacentX(double x, LatDirection dir) {
        if (dir== LatDirection.LEFT && !goLeft && !left.goRight) {
            // maybe not physically adjacent, use total length only
            return x * left.l/l;
        } else if (dir== LatDirection.RIGHT && !goRight && !right.goLeft) {
            // maybe not physically adjacent, use total length only
            return x * right.l/l;
        } else {
            // get appropiate section, and fraction within section
            double xCumul = 0; // length at end of appropiate section
            int section = 0;
            double dx = 0;
            double dy = 0;
            if (x>l) {
                // last section
                section = this.x.length-2;
                dx = this.x[section+1]-this.x[section];
                dy = this.y[section+1]-this.y[section];
                xCumul = l;
            } else if (x<=0) {
                // first section
                dx = this.x[section+1]-this.x[section];
                dy = this.y[section+1]-this.y[section];
                xCumul = Math.sqrt(dx*dx + dy*dy);
            } else {
                // find section by looping
                while (xCumul<x) {
                    dx = this.x[section+1]-this.x[section];
                    dy = this.y[section+1]-this.y[section];
                    xCumul = xCumul + Math.sqrt(dx*dx + dy*dy);
                    section++;
                }
                section--;
            }
            double lSection = Math.sqrt(dx*dx + dy*dy); // length of appropiate section
            double fSection = 1-(xCumul-x)/lSection; // fraction within appropiate section
            // loop appropiate adjacent lane
            jLane lane = null;
            if (dir== LatDirection.LEFT) {
                lane = left;
            } else if (dir== LatDirection.RIGHT) {
                lane = right;
            }
            // loop preceding sections
            double xStart = 0;
            for (int i=0; i<section; i++) {
                dx = lane.x[i+1]-lane.x[i];
                dy = lane.y[i+1]-lane.y[i];
                xStart = xStart + Math.sqrt(dx*dx + dy*dy);
            }
            // add part of appropiate section
            dx = lane.x[section+1]-lane.x[section];
            dy = lane.y[section+1]-lane.y[section];
            return xStart + fSection*Math.sqrt(dx*dx + dy*dy);
        }
    }

    /**
     * Utility to connect this lane with right lane.
     * @param right The right lane.
     */
    public void connectLat(jLane right) {
        this.right = right;
        right.left = this;
    }

    /**
     * Utility to connect this lane with upstream lane.
     * @param up The upstream lane.
     */
    public void connectLong(jLane up) {
        up.down = this;
        this.up = up;
        if (this.getType().isTaper()) {
            this.up.setTaper(this);
        }
    }

    /**
     * Returns the global x and y at the lane centre.
     * @param pos Position [m] on the lane.
     * @return Point with x and y coordinates.
     */
    public Point2D.Double XY(double pos) {
        double cumlength[] = new double[x.length];
        cumlength[0] = 0;
        double dx; // section distance in x
        double dy; // section distance in y
        int section = -1; // current section of vehicle
        // calculate cumulative lengths untill x of vehicle is passed
        for (int i=1; i<x.length; i++) {
            dx = x[i] - x[i-1];
            dy = y[i] - y[i-1];
            cumlength[i] = cumlength[i-1] + java.lang.Math.sqrt(dx*dx + dy*dy);
            if (section==-1 && cumlength[i]>pos) {
                section = i;
                i = x.length; // stop loop
            }
        }
        if (section==-1) {
            // the vehicle is probably beyond the lane, extrapolate from last section
            section = x.length-1;
        }
        double x0 = x[section-1]; // start of current section
        double y0 = y[section-1];
        double x1 = x[section]; // end of current section
        double y1 = y[section];
        double res = pos-cumlength[section-1]; // distance within section
        double sec = cumlength[section] - cumlength[section-1]; // section length
        return new Point2D.Double(x0 + (x1-x0)*(res/sec), y0 + (y1-y0)*(res/sec));
    }
    
    /**
     * Returns the heading on the lane at the given position. The returned
     * <tt>Point2D.Double</tt> object is not actually a point. Instead, the x
     * and y values are the x and y headings.<br>
     * <pre><tt>
     *            x
     *       ----------
     *       |'-.
     *     y |   '-. 
     *       |      '-. heading, length = sqrt(x^2 + y^2) = 1</tt></pre> 
     * @param pos Position [m] on the lane.
     * @return Point where x and y are the x and y headings.
     */
    public Point2D.Double heading(double pos) {
        double cumlength[] = new double[x.length];
        cumlength[0] = 0;
        double dx; // section distance in x
        double dy; // section distance in y
        int section = -1; // current section of vehicle
        // calculate cumulative lengths untill x of vehicle is passed
        for (int i=1; i<x.length; i++) {
            dx = x[i] - x[i-1];
            dy = y[i] - y[i-1];
            cumlength[i] = cumlength[i-1] + java.lang.Math.sqrt(dx*dx + dy*dy);
            if (section==-1 && cumlength[i]>pos) {
                section = i;
                i = x.length; // stop loop
            }
        }
        if (section==-1) {
            // the vehicle is probably beyond the lane, extrapolate from last section
            section = x.length-1;
        }
        dx = x[section] - x[section-1];
        dy = y[section] - y[section-1];
        double f = 1/Math.sqrt(dx*dx + dy*dy);
        return new Point2D.Double(dx*f, dy*f);
    }
    
    /**
     * Returns whether the destination can be reached from this lane.
     * @param destination Destination of interest.
     * @return Whether this lane leads to the given destination.
     */
    public boolean leadsTo(int destination) {
        return lanechanges.containsKey(destination);
    }
    
    /**
     * Returns the number of lane changes required to go to the given destination.
     * @param destination Destination of interest.
     * @return The number of lane changes for the destination.
     */
    public int nLaneChanges(int destination) {
        return lanechanges.get(destination);
    }
    
    /**
     * Returns the number of lane changes that need to be performed to go to
     * the destination from this lane.
     * @param destination Destination of interest.
     * @return Number of lane changes that needs to be performed for this destination.
     */
    public double xLaneChanges(int destination) {
        return endpoints.get(destination);
    }

    public LaneType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public double[] getX() {
        return x;
    }

    public double[] getY() {
        return y;
    }

    public List<Coordinate> getCoordinates() {
        return this.coordinates;
    }

    public double getL() {
        return l;
    }

    public jLane getTaper() {
        return taper;
    }

    public jLane getUp() {
        return up;
    }

    public jLane getDown() {
        return down;
    }

    public jLane getLeft() {
        return left;
    }

    public jLane getRight() {
        return right;
    }

    public void setGoLeft(boolean allowed) {
        this.goLeft = allowed;
    }

    public boolean isGoLeft() {
        return goLeft;
    }

    public void setGoRight(boolean allowed) {
        this.goRight = allowed;
    }

    public boolean isGoRight() {
        return goRight;
    }

    public List<AbstractRSU> getRSUs() {
        return RSUs;
    }

    public List<AbstractVehicle> getVehicles() {
        return vehicles;
    }

    public List<AbstractVehicle> getVehicles(double startX, double endX) {
        List<AbstractVehicle> vehiclesInRange = new ArrayList<AbstractVehicle>();
        for (AbstractVehicle vehicle: vehicles) {
            if (vehicle.getX() >= startX && vehicle.getX() <= endX) {
                vehiclesInRange.add(vehicle);
            }
        }
        return vehiclesInRange;
    }


    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public double getvLim() {
        return vLim;
    }

    public void setvLim(double limit) {
        vLim = limit;
    }

    public Map<Integer, Integer> getLanechanges() {
        return lanechanges;
    }

    public Map<Integer, Double> getEndpoints() {
        return endpoints;
    }

    public AbstractQueuedTrafficGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(AbstractQueuedTrafficGenerator generator) {
        this.generator = generator;
    }

    public Map<Integer, Double> getxAdjust() {
        return xAdjust;
    }

    public void setUp(jLane up) {
        this.up = up;
    }

    public void setDown(jLane down) {
        this.down = down;
    }

    public void setTaper(jLane taper) {
        this.taper = taper;
    }

    /**
     * Adds a vehicle at the begining of this lane.
     * @param vehicle The vehicle to be added.
     */
    public void addVehicle(AbstractVehicle vehicle) {
        addVehicle(vehicle, 0, 0);
    }

    /**
     * <p>
     * Add a vehicle to the lane at position <tt>atX</tt>. The parameter, <tt>fromX</tt> is
     * the location of the vehicle before it landed on the <tt>atX</tt> position.
     * This will be used to automatically notify any Observers that existed in the range
     * [fromX, atX] that the vehicle has passed them. <tt>fromX</tt> should always be in
     * the coordinate system of the new lane.
     * </p>
     * <p>
     * For example, if during the last round a vehicle crossed in the downstream direction
     * from one lane to its downstream continuation, <tt>fromX</tt> will be a negative value
     * reflecting that the vehicle was previously on the upstream lane before ending up on
     * this lane.
     * </p>
     *
     * @param vehicle The vehicle to be added.
     * @param fromX The previous x coordinate of the vehicle in the coordinate system of this new lane.
     * @param atX The position at which the vehicle is to be added.
     */
    public void addVehicle(AbstractVehicle vehicle, double fromX, double atX) {
        addVehicleSorted(vehicle);
        vehicle.addObserver(this);

        for (jObserver observer: observers) {
            if (observer instanceof AbstractRSU) {
                AbstractRSU rsu = (AbstractRSU) observer;
                if (rsu.getX() >= fromX && rsu.getX() <= atX) {
                    rsu.see(vehicle);
                }
            }
            else {
                observer.see(vehicle);
            }
        }
    }

    private void addVehicleSorted(AbstractVehicle vehicle) {
        vehicles.add(vehicle);
        Collections.sort(vehicles, new Comparator<AbstractVehicle>() {
            @Override
            public int compare(AbstractVehicle o1, AbstractVehicle o2) {
                if (o1.getX() < o2.getX()) {
                    return -1;
                }
                else if (o1.getX() == o2.getX()) {
                    return 0;
                }
                else {
                    return 1;
                }
            }
        });
    }

    public void removeVehicle(AbstractVehicle vehicle) {
        vehicles.remove(vehicle);
        vehicle.detachObserver(this);
    }

    @Override
    public void addObserver(jObserver observer) {
        observers.add(observer);
    }

    @Override
    public void detachObserver(jObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void detachAllObservers() {
        observers.clear();
    }

    /**
     * Lanes subscribes to the movements of {@link jMovable}s and delegate
     * the information to observers of the lane (e.g. detectors) to
     * take appropriate actions.
     * @param subject The subject in which the state change has occured.
     */
    @Override
    public void see(jObservable subject) {
        if (! (subject instanceof jMovable)) {
            //This should never happen
            throw new IllegalArgumentException("Observed Non-Movable object!");
        }
        for (jObserver observer: observers) {
            observer.see(subject);
        }
    }

    public AbstractVehicle search(Enclosure area, double referenceX) {
        AbstractVehicle found = null;
        AbstractVehicle vehicle = new Vehicle();
        vehicle.setX(referenceX);

        int index = Collections.binarySearch(vehicles, vehicle, new Comparator<AbstractVehicle>() {
            @Override
            public int compare(AbstractVehicle o1, AbstractVehicle o2) {
                if (o1.getX() < o2.getX()) {
                    return -1;
                }
                else if (o1.getX() == o2.getX()) {
                    return 0;
                }
                else {
                    return 1;
                }
            }
        });
        int insertionIndex = index >=0 ? index : -(index + 1);

        switch (area) {
            case CURRENT_LOCATION:
                if (insertionIndex < vehicles.size() && vehicles.get(insertionIndex).getX() == referenceX) {
                    found = vehicles.get(index);
                }
                break;
            case DOWNSTREAM:
                if ( (index >=0) && (insertionIndex < vehicles.size() - 1 ) ) {
                    found = vehicles.get(insertionIndex + 1);
                }
                else if ( (index < 0 ) && (insertionIndex < vehicles.size())) {
                    found = vehicles.get(insertionIndex);
                }
                break;
            case UPSTREAM:
                if (insertionIndex > 0) {
                    found = vehicles.get(insertionIndex - 1);
                }
                break;
            case LEFT:
                if (this.getLeft() != null) {
                    found = this.getLeft().search(Enclosure.CURRENT_LOCATION, getAdjacentX(referenceX, LatDirection.LEFT));
                }
                break;
            case RIGHT:
                if (this.getRight() != null) {
                    found = this.getRight().search(Enclosure.CURRENT_LOCATION, getAdjacentX(referenceX, LatDirection.RIGHT));
                }
                break;
            case LEFT_DOWNSTREAM:
                if (this.getLeft() != null) {
                    found = this.getLeft().search(Enclosure.DOWNSTREAM, getAdjacentX(referenceX, LatDirection.LEFT));
                }
                break;
            case LEFT_UPSTREAM:
                if (this.getLeft() != null) {
                    found = this.getLeft().search(Enclosure.UPSTREAM, getAdjacentX(referenceX, LatDirection.LEFT));
                }
                break;
            case RIGHT_DOWNSTREAM:
                if (this.getRight() != null) {
                    found = this.getRight().search(Enclosure.DOWNSTREAM, getAdjacentX(referenceX, LatDirection.RIGHT));
                }
                break;
            case RIGHT_UPSTREAM:
                if (this.getRight() != null) {
                    found = this.getRight().search(Enclosure.UPSTREAM, getAdjacentX(referenceX, LatDirection.RIGHT));
                }
                break;
            default:
                break;
        }
        //This is the case where the downstream/upstream vehicles are not on the directly adjacent lanes but further
        // up or down the road.
        switch (area) {
            case DOWNSTREAM:
                if (found == null) {
                    if ( (getDown() != null) && (getDown().getVehicles().size() > 0) ) {
                        found = getDown().getVehicles().get(0);
                    }
                }
                break;
            case UPSTREAM:
                if (found == null) {
                    if ( ( getUp() != null ) && (getUp().getVehicles().size() > 0) ){
                        found = getUp().getVehicles().get(getUp().getVehicles().size()-1);
                    }
                }
                break;
            case LEFT_DOWNSTREAM:
                if (found == null) {
                    if ( (getLeft() != null) && (getLeft().getDown()!= null)  && (getLeft().getDown().getVehicles().size() > 0)) {
                        found  = getLeft().getUp().getVehicles().get(0);
                    }
                }
                break;
            case LEFT_UPSTREAM:
                if (found == null) {
                    if ( (getLeft() != null ) && ( getLeft().getUp() != null ) && ( getLeft().getUp().getVehicles().size()>0)) {
                        int size = getLeft().getUp().getVehicles().size();
                        found  =  getLeft().getUp().getVehicles().get(size - 1);
                    }
                }
                break;
            case RIGHT_DOWNSTREAM:
                if (found == null) {
                    if ( (getRight() != null) && (getRight().getDown()!= null)  && (getRight().getDown().getVehicles().size() > 0)) {
                        found  = getRight().getUp().getVehicles().get(0);
                    }
                }
                break;
            case RIGHT_UPSTREAM:
                if (found == null) {
                    if ( (getRight() != null ) && ( getRight().getUp() != null ) && ( getRight().getUp().getVehicles().size()>0)) {
                        int size = getRight().getUp().getVehicles().size();
                        found  =  getRight().getUp().getVehicles().get(size - 1);
                    }
                }
                break;
            default:
                break;
        }
        return found;
    }

    /**
     * Returns the headway in [s] available for a particular vehicle at the start of the lane based
     * on the speed with which the vehicle enters the lane.
     * @param vehicle The vehicle
     * @return
     */
    public double calculateTimeHeadway(jVehicle vehicle) {
        double freeSpace = calculateSpaceHeadway();
        return freeSpace / vehicle.getSpeed();
    }

    /**
     * Returns the headway in [m] available at the start of a lane.
     * @return
     */
    public double calculateSpaceHeadway() {
        return this.vehicles.size() > 0 ? vehicles.get(0).getX() : this.getL();
    }
}

