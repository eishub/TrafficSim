package microModel.core.device;

/**
 * This is an interface that provides the functionality of any type of entity that is
 * supposed to be controllable in the main loop of the micro simulation.
 */
public interface jController {
    /**
     * Initializes the controller.
     */
    public void init();

    /**
     * This is the method that is called in every round of the micro simulation main loop.
     */
    public void run();

    /**
     * This method defines the main operation of the controller (e.g. the thing that it does).
     * For example a measurement device can take its measurements here.
     */
    public void control();

    /**
     * <p>
     * This method defines the operation of a controller when it is not busy doing its main functionality.
     * The main purpose of this method is to include the influence of time.
     * </p>
     * <p>
     * For example a measurement device may be taking measurements in intervals. In between those
     * intervals it might do other things or not do anything at all. This function should be implemented here.
     * </p>
     */
    public void noControl();
}
