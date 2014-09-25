package microModel.core.driver.model;

import microModel.core.Parameter;
import microModel.core.driver.jDriver;

/**
 * Original Intelligent Driver Model as appearing in "Congested traffic states in empirical observations and microscopic simulations" by Treiber et al.
 */
public class IDM {

    /** Current desired velocity. Use <tt>updateDesiredVelocity()</tt> to get it. */
    public static final Parameter<Double> V0 = new Parameter<Double>("IDM_Desired_Speed", 0.0);
    /** Desired safe time Headway [s].*/
    public static final Parameter<Double> T = new Parameter<Double>("Driver_Safe_Time_Headway", 1.2);
    /** IDM acceleration [m/s^2]. */
    public static final Parameter<Double> A = new Parameter<Double>("IDM_Acceleration", 1.25);
    /** IDM deceleration [m/s^2]. */
    public static final Parameter<Double> B = new Parameter<Double>("IDM_Deceleration", 2.09);
    /** IDM stopping distance [m] (Minimum bumper-to-bumper distance to the front vehicle after coming to a full stop). */
    public static final Parameter<Double> S0 = new Parameter<Double>("IDM_Stopping_Distance", 3.0);
    /** Speed limit adherence factor. */
    public static final Parameter<Double> F_SPEED = new Parameter<Double>("Driver_Speed_Limit_Adherence_Factor", 1.0);
    /** Acceleration exponent used in calculating the acceleration in each round. */
    public static final Parameter<Integer> DELTA = new Parameter<Integer>("Acceleration_Exponent", 4);

    public static double desiredVelocity(double maxSpeed, double speedLimit, double speedLimitAdeherenceFactor) {
        return Math.min(maxSpeed, speedLimitAdeherenceFactor * speedLimit);
    }

    public static double desiredVelocity(jDriver driver) {
        return desiredVelocity(driver.getVehicle().getMaxSpeed(),
                               driver.getVehicle().getLane().getVLimInMetersPerSecond(),
                               driver.get(IDM.F_SPEED));
    }

    /**
     * This method updates the desired speed on the current lane and returns the current value.
     * It is used by the vehicle generator, and possibly the driver itself.
     * @return Desired velocity [m/s]
     */
    public static double updateDesiredVelocity(jDriver driver) {
        double vMax = driver.getVehicle().getMaxSpeed();
        double laneSpeedLimit = driver.getVehicle().getLane().getVLimInMetersPerSecond();
        double desiredVelocity = desiredVelocity(vMax, laneSpeedLimit, driver.get(IDMPlus.F_SPEED));
        driver.set(IDM.V0, desiredVelocity);
        return driver.get(IDM.V0);
    }

    /**
     * The desired headway method returns the desired <b>equilibrium</b> net
     * headway. It is used by the vehicle generator, and possibly the driver
     * itself.
     * @return Desired net headway [m]
     */
    public static double desiredEquilibriumHeadway(jDriver driver) {
        return desiredEquilibirumHeadway(driver.getVehicle().getSpeed(),
                driver.get(IDM.S0),
                driver.get(T));
    }

    public static double acceleration(jDriver driver, double distanceToBlockingObject, double blockingObjectSpeed) {
        // calculate acceleration
        return acceleration(driver.get(IDM.S0),
                            updateDesiredVelocity(driver),
                            driver.getVehicle().getSpeed(),
                            driver.get(IDM.DELTA),
                            driver.get(IDM.T),
                            driver.getVehicle().getSpeed() - blockingObjectSpeed,
                            distanceToBlockingObject,
                            driver.get(IDM.A),
                            driver.get(IDM.B));
    }

    public static double desiredEquilibirumHeadway(double v, double s0, double t) {
        return s0 + v * t;
    }

    public static double acceleration(double s0, double v0, double v, int delta, double t, double deltaV, double s, double a, double b) {
        return desiredAcceleration(a, v, v0, delta) - a * Math.pow(desiredDynamicDistance(s0, v, t, deltaV, a, b)/s,2);
    }

    //TODO: this is IDM+ not IDM
    public static double acceleration1(double s0, double v0, double v, int delta, double t, double deltaV, double s, double a, double b) {
        return a * Math.min(1-Math.pow(v/v0, delta),  1- Math.pow(desiredDynamicDistance(s0, v, t, deltaV, a, b)/s,2));
    }


    public static double desiredAcceleration(double a, double v, double v0, int delta) {
        return a * (1-Math.pow(v/v0, delta));
    }

    public static double desiredDynamicDistance(double s0, double v, double t, double deltaV, double a, double b) {
        return s0 + Math.max(0, v*t + (v * deltaV)/(2 * Math.sqrt(a*b)));
    }
}
