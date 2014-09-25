package microModel.core.road.device;

import microModel.core.device.AbstractController;
import microModel.core.driver.jDriver;
import microModel.core.observation.jObservable;
import microModel.core.observation.jObserver;
import microModel.core.road.jLane;

/**
 * Abstract shell for road-side units (RSUs). Besides controller functionality a
 * RSU can be passable, in which case passing vehicles will call the 
 * <tt>see(Vehicle)</tt> method. A RSU can also be noticable in which case the
 * driver responds when being near to the RSU by calling the 
 * <tt>isNoticed(Driver)</tt> method. It is not intended that driver behavior
 * is located in the <tt>isNoticed(Driver)</tt> method. Instead, forward the
 * notice to the driver by calling <tt>Driver.notice(this)</tt>. The driver
 * should have a method <tt>notice(myRSU)</tt> where <tt>myRSU</tt> extends
 * <tt>AbstractRSU</tt>. Note that if that method is missing, the drivers
 * <tt>notice(AbstractRSU)</tt> method is called, which will recognize that it is
 * being called in a loop and will report an error.
 */
public abstract class AbstractRSU extends AbstractController implements jObserver {

    /** LaneType where the RSU is located. */
    public jLane lane;
    
    /** Position [m] of the RSU on the lane. */
    private double x;
    
    /** Whether the RSU is passable (RSU reacts to vehicle). */
    public boolean passable;
    
    /** Whether the RSU is noticeable (driver reacts to RSU). */
    public boolean noticeable;
  
    /**
     * Constructor using the control every time step. The RSU is linked to the 
     * lane and vice versa.
     * @param lane LaneType where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractRSU(jLane lane, double x, boolean passable, boolean noticeable) {
        this(lane, x, 0, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt>. The RSU is linked to
     * the lane and vice versa.
     * @param lane LaneType where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param period Time [s] between control runs.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    public AbstractRSU(jLane lane, double x, double period, boolean passable, boolean noticeable) {
        this(lane, x, period, 0, passable, noticeable);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt> but no sooner than
     * <tt>start</tt>. The RSU is linked to the lane and vice versa.
     * @param lane LaneType where the RSU is located.
     * @param x Position [m] of the RSU on the lane.
     * @param period Time [s] between control runs.
     * @param start Time [s] of first control run.
     * @param passable Whether the RSU is passable.
     * @param noticeable Whether the RSU is noticeable.
     */
    // using this at the end is ok, RSU is fully initialized
    @SuppressWarnings(value = "LeakingThisInConstructor")
    public AbstractRSU(jLane lane, double x, double period, double start, boolean passable, boolean noticeable) {
        super(period, start);
        this.lane = lane;
        this.x = x;
        this.passable = passable;
        this.noticeable = noticeable;
        lane.addRSU(this);
    }
    
    /** 
     * Returns the location of this RSU.
     */
    public double x() {
        return x;
    }
    
    /** Performs the initialization.  */
    public abstract void init();
    
    /**
     * Vehicle see method to be defined by subclasses.
     * @param observable Vehicle that passes the RSU.
     */
    public abstract void see(jObservable observable);

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }
}