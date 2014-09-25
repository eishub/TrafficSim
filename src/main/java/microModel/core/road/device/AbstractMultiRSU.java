package microModel.core.road.device;

import microModel.core.driver.jDriver;
import microModel.core.observation.jObservable;
import microModel.core.road.LatDirection;
import microModel.core.road.jLane;

/**
 * Convenience class that makes one RSU valid on multiple lanes. It does so by
 * creating helper RSUs on all lanes at a cross section that forward everything
 * to the common RSU. The common RSU itself is located at one of the lanes, this
 * lane will therefore not have a helper RSU.
 */
public abstract class AbstractMultiRSU extends AbstractRSU {
    
    /**
     * Constructor using the control every time step. The RSU is linked to the 
     * lane, and all lanes in the same cross section, and vice versa.
     * @param lane LaneType in cross section where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane lane, double x, boolean passable, boolean noticeable) {
        this(lane, x, 0, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt>. The RSU is linked to
     * the lane, and all lanes in the same cross section, and vice versa.
     * @param lane LaneType in cross section where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param period Time [s] between control runs.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane lane, double x, double period, boolean passable, boolean noticeable) {
        this(lane, x, period, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt> but no sooner than
     * <tt>start</tt>. The RSU is linked to the lane, and all lanes in the same 
     * cross section, and vice versa.
     * @param lane LaneType in cross section where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param period Time [s] between control runs.
     * @param start Time [s] of first control run.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane lane, double x, double period, double start, boolean passable, boolean noticeable) {
        // Add this RSU to the first lane
        super(lane, x, period, start, passable, noticeable);
        addSingleLaneRSUs(null); // null means all in cross section
    }
    
    /**
     * Constructor using the control every time step. The RSU is linked to the 
     * lanes and vice versa.
     * @param lanes Lanes where the RSU is located.
     * @param x Position [m] of the RSU on the first lane.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane[] lanes, double x, boolean passable, boolean noticeable) {
        this(lanes, x, 0, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt>. The RSU is linked to
     * the lanes and vice versa.
     * @param lanes Lanes where the RSU is located.
     * @param x Position [m] of the RSU on the first lane.
     * @param period Time [s] between control runs.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane[] lanes, double x, double period, boolean passable, boolean noticeable) {
        this(lanes, x, period, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt> but no sooner than
     * <tt>start</tt>. The RSU is linked to the lanes and vice versa.
     * @param lanes Lanes where the RSU is located.
     * @param x Position [m] of the RSU on the first lane.
     * @param period Time [s] between control runs.
     * @param start Time [s] of first control run.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractMultiRSU(jLane[] lanes, double x, double period, double start, boolean passable, boolean noticeable) {
        // Add this RSU to the first lane
        super(lanes[0], x, period, start, passable, noticeable);
        addSingleLaneRSUs(lanes);
    }
    
    /**
     * Adds a RSU to be used as part of this multiRSU to all given lanes. The 
     * lane that is directly linked to the mutliRSU is skipped (as it has a RSU,
     * the mutliRSU) and all lanes need to be in the same cross section. If the
     * given lanes are <tt>null</tt>, all lanes in the cross section get a RSU.
     * @param lanes Lanes that this mutliRSU should be connected to.
     */
    private void addSingleLaneRSUs(jLane[] lanes) {
        // Add a SingleLaneRSU on every other lane
        // move to left lanes
        jLane curLane = lane.getLeft();
        double curX = lane.getAdjacentX(getX(), LatDirection.LEFT);
        while (curLane!=null) {
            if (lanes==null || java.util.Arrays.asList(lanes).contains(curLane)) {
                SingleLaneRSU tmp = new SingleLaneRSU(curLane, curX, passable, noticeable);
            }
            curX = curLane.getAdjacentX(curX, LatDirection.LEFT);
        }
        // move to the right lanes
        curLane = lane.getRight();
        curX = lane.getAdjacentX(getX(), LatDirection.RIGHT);
        while (curLane!=null) {
            if (lanes==null || java.util.Arrays.asList(lanes).contains(curLane)) {
                SingleLaneRSU tmp = new SingleLaneRSU(curLane, curX, passable, noticeable);
            }
            curX = curLane.getAdjacentX(curX, LatDirection.RIGHT);
        }
    }
    
    /**
     * Private inner class which adds a single-lane (regular) RSU to all but the 
     * first lanes in a multiRSU. This class has empty control and forwards 
     * verhicle passing and noticing by drivers to the outer multiRSU class.
     */
    private final class SingleLaneRSU extends AbstractRSU {
        
        /**
         * Constructor which links the RSU to the lane and vice versa.
         * @param lane LaneType where the RSU is located.
         * @param x Position [m] of the RSU on the lane.
         * @param passable Whether the RSU is passable.
         * @param noticeable Whether the RSU is noticeable.
         */
        public SingleLaneRSU(jLane lane, double x, boolean passable, boolean noticeable) {
            super(lane, x, passable, noticeable);
        }
        
        /** Empty, needs to be implemented.  */
        public void init() {}
        
        /** Empty, must be implemented.  */
        public void control() {}
        
        /** Empty, must be implemented.  */
        public void noControl() {}
        
        /**
         * Vehicle passing method which forwards the passing to the outer 
         * mutliRSU.
         * @param observable Passing vehicle.
         */
        public void see(jObservable observable) {
            AbstractMultiRSU.this.see(observable);
        }
    }
}