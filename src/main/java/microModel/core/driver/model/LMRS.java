package microModel.core.driver.model;

import microModel.core.Parameter;

/**
* Created with IntelliJ IDEA.
* User: arman
* Date: 2/13/13
* Time: 7:38 PM
* To change this template use File | Settings | File Templates.
*/
public class LMRS {
    /** LMRS lane change desire to the left. */
    public static final Parameter<Double> D_LEFT = new Parameter<Double>("LMRS_Desire_To_Go_Left", 0.0);
    /** LMRS lane change desire to the right. */
    public static final Parameter<Double> D_RIGHT = new Parameter<Double>("LMRS_Desire_To_Go_Right", 0.0);
    /** LMRS free lane change threshold. */
    public static final Parameter<Double> D_FREE = new Parameter<Double>("LMRS_Free_Lane_Change_Threshold", 0.365);
    /** LMRS synchronized lane change threshold. */
    public static final Parameter<Double> D_SYNC = new Parameter<Double>("LMRS_Synchronized_Lane_Change_Threshold", 0.577);
    /** LMRS cooperative lane change threshold. */
    public static final Parameter<Double> D_COOP = new Parameter<Double>("LMRS_Cooperative_Lane_Change_Threshold", 0.788);
    /** LMRS mandatory lane change time [s]. */
    public static final Parameter<Double> T0 = new Parameter<Double>("LMRS_Mandatory_Lane_Change_Time", 43.0);
    /** LMRS mandatory lane change distance [m]. */
    public static final Parameter<Double> X0 = new Parameter<Double>("LMRS_Mandatory_Lane_Change_Distance", 295.0);
    /** LMRS speed gain [m/s] for full desire. */
    public static final Parameter<Double> V_GAIN = new Parameter<Double>("LMRS_Speed_Gain_Full_Desire", 69.6/3.6);
    /** LMRS critical speed [m/s] for a speed gain in the right lane. */
    public static final Parameter<Double> V_CONG = new Parameter<Double>("LMRS_Critical_Speed_Gain_Right_Lane", 60/3.6);
    /** LMRS deceleration for lane changes (default: equal to <tt>b</tt>). */
    public static final Parameter<Double> B_SAFE = new Parameter<Double>("LMRS_Deceleration_Lane_Change", 2.09);
    /** LMRS minimum time headway [s] for very desired lane change. */
    public static final Parameter<Double> T_MIN = new Parameter<Double>("LMRS_Min_TimeHeadway_for_Desired_Lane_Change", 0.56);
    /** LMRS relaxation time [s] for headway relaxation after lane change. */
    public static final Parameter<Double> TAU = new Parameter<Double>("LMRS_Relaxation_Time_After_Lane_Change", 25.0);
    /** Whether driver is synchronizing with the left lane. */
    public static final Parameter<Boolean> LEFT_SYNC = new Parameter<Boolean>("LMRS_Driver_Synchronizing_With_Left", false);
    /** Whether driver is synchronizing with the right lane. */
    public static final Parameter<Boolean> RIGHT_SYNC = new Parameter<Boolean>("LMRS_Driver_Synchronizing_With_Right", false);
    /** Whether driver is yielding for a vehicle in the right lane (for a left lane change). */
    public static final Parameter<Boolean> LEFT_YIELD = new Parameter<Boolean>("LMRS_Driver_Yielding_for_Vehicle_to_Left", false);
    /** Whether driver is yielding for a vehicle in the left lane (for a right lane change). */
    public static final Parameter<Boolean> RIGHT_YIELD = new Parameter<Boolean>("LMRS_Driver_Yielding_for_Vehicle_to_Right", false);
    /** Duration [s] of a lane change. */
    public static final Parameter<Double> DURATION = new Parameter<Double>("LMRS_Lane_Change_Duration", 3.0);
    /** Maximum deceleration [m/s^2] for a red light (otherwise jumped). */
    public static final Parameter<Double> B_RED = new Parameter<Double>("Max_Deceleration_for_Red_Light", 5.0);
}
