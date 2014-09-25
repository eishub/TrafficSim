package microModel.core.device;

import microModel.jModel;

/**
 * <p>
 * Abstract class meant to provide general functionality for devives like
 * detectors, traffic lights, dynamic speed limiers. Other types of controllers
 * are vehicle generators, OBUs and RSUs.
 * </p>
 * <p>The simulation calls the {@link #run()} method during each round.</p>
 * <p>
 * The {@link #control()} and {@link #noControl()} methods should be defined in
 * sub classes and should respectively define the operation of the controller
 * while active and inactive.
 * </p>
 * Controllers that do not run with a fixed interval (i.e. always run), should
 * receive no <tt>duration</tt> as input (or alternatively a value of zero).
 * </p>
 */
public abstract class AbstractController implements jController {

    /** Time between control runs of this controller [s]. */
    protected double duration;
    
    /** Start time of the first control run of the controller [s]. */
    protected double start = 0;
    
    /** Time of last control run. */
    protected double t;

    /**
     * Controllers that always run.
     */
    public AbstractController() {
        this(0, 0);
    }

    /**
     * Controller that run every <tt>duration</tt> but no sooner
     * than <tt>start</tt>.
     * @param duration Time between control runs.
     * @param start Start time of first control run.
     */
    public AbstractController(double duration, double start) {
        this.duration = duration;
        this.start = start;
    }

    /**
     * <p>
     * Runs the controller by calling {@link #control()} within the active duration
     * of this controller and calling {@link #noControl()} otherwise.
     * </p>
     * <p>
     * Determining which function to call depends on the initial values provided for
     * at construction time. see {@link #AbstractController(double, double)},
     * {@link #AbstractController()}
     * </p>
     */
    @Override
    public final void run() {
        jModel model = jModel.getInstance();
        if (model.getT() >= t + duration && model.getT() >= start) {
            t = t + duration; // set time of latest control
            control();
        } else {
            noControl();
        }
    }

}