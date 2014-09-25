package microModel.core.road.device;

import microModel.core.driver.jDriver;
import microModel.core.observation.jObservable;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Respresents a dual-loop induction detector that registers vehicles including
 * their speeds.
 */
public class jDetector extends AbstractRSU {
    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(jDetector.class);

    /** Vehicle count within the current duration. */
    protected int qCur;

    /** Average vehicle speed within the current duration. */
    protected double vCur;

    /** History of flow measurements. */
    public List<Integer> qHist = new ArrayList<Integer>();

    /** History of speed measuerments. */
    public List<Double> vHist = new ArrayList<Double>();

    /** History of measured duration starting times. */
    public List<Double> tHist = new ArrayList<Double>();

    /** ID of real-life detector / user recognizable number. */
    protected int id;

    /**
     * @param lane LaneType where detector is at.
     * @param x Position [m] of detector at the lane.
     * @param period Aggregation duration [s].
     * @param id User recognizable ID number.
     */
    public jDetector(jLane lane, double x, double period, int id) {
        super(lane, x, period, true, false);
        this.id = id;
    }

    public void init() {/** Empty*/}
    
    /**
     * Performs the detector task. At the end of each aggregation duration, a flow
     * count and average speed is added.
     */
    public void control() {

        jModel model = jModel.getInstance();
        jSettings settings = jSettings.getInstance();

        // data is aggregated this time step
        if (settings.get(BuiltInSettings.DEBUG_DETECTOR)) {
            qHist.add(qCur);
            vHist.add(vCur);
            tHist.add(model.getT());
        }
        // reset count
        qCur = 0;
        vCur = 0;
    }

    public void noControl() { /* empty */}

    /**
     * Updates the current measurement with an additional vehicle.
     * @param observable Passing vehicle.
     */
    public void see(jObservable observable) {
        jSettings settings = jSettings.getInstance();
        if (!settings.get(BuiltInSettings.DEBUG_DETECTOR)) {
            return;
        }
        if (!(observable instanceof AbstractVehicle)) {
            //This should never happen.
            throw new IllegalArgumentException("Non-Movable Object Detected!");
        }
        AbstractVehicle vehicle = (AbstractVehicle) observable;
        if ( passable &&
             (
             /** Vehicle passed the detector.*/
             (getX() >= vehicle.getX() && getX() <= vehicle.getX() + vehicle.getDx())
             ||
             /** Vehicle was on the upstream lane but the movement in previous round made it
              * land on next lane and during that it also passed this detector.
              */
             (getX() <= vehicle.getX() && vehicle.isJustExceededLane())
             )
           ) {
            //Vehicle just passed this detector.
            if (qCur == 0) {
                vCur = vehicle.getSpeed();
            } else {
                // add velocity to average
                vCur = ((vCur*qCur)+ vehicle.getSpeed())/(qCur+1);
            }
            qCur++;
        }
    }

    /**
     * Returns the ID of this detector.
     * @return ID of this detector.
     */
    public int id() {
        return id;
    }
}