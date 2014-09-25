package microModel.core.driver.model;

import microModel.core.Parameter;
import microModel.core.driver.jDriver;
import microModel.core.vehicle.Vehicle;
import microModel.core.vehicle.jVehicle;

/**
 * IDM+ model as modified by Schakel et al. in "LMRS: An Integrated Lane Change Model with Relaxation and Synchronization"

 */
public final class IDMPlus {
    /** Current desired velocity. Use <tt>updateDesiredVelocity()</tt> to get it. */
    public static final Parameter<Double> V0 = new Parameter<Double>("IDM_Desired_Speed", 0.0);
    /** Desired safe time Headway [s].*/
    public static final Parameter<Double> T = new Parameter<Double>("Driver_Safe_Time_Headway", 1.2);
    /** IDMPlus maximum safe following headway [s]. */
    public static final Parameter<Double> T_MAX = new Parameter<Double>("IDM_MAX_Safe_Time_Headway", 1.2);
    /** IDMPlus acceleration [m/s^2]. */
    public static final Parameter<Double> A = new Parameter<Double>("IDM_Acceleration", 1.25);
    /** IDMPlus deceleration [m/s^2]. */
    public static final Parameter<Double> B = new Parameter<Double>("IDM_Deceleration", 2.09);
    /** IDMPlus stopping distance [m] (Minimum bumper-to-bumper distance to the front vehicle after coming to a full stop). */
    public static final Parameter<Double> S0 = new Parameter<Double>("IDM_Stopping_Distance", 3.0);
    /** Speed limit adherence factor. */
    public static final Parameter<Double> F_SPEED = new Parameter<Double>("Driver_Speed_Limit_Adherence_Factor", 1.0);
    /** Acceleration exponent used in calculating the acceleration in each round. */
    public static final Parameter<Integer> DELTA = new Parameter<Integer>("Acceleration_Exponent", 4);

    /**
     * Calculation of acceleration based on a specific leader. The default is
     * the IDMPlus+ car following model.
     * @param leader Acceleration is based on this vehicle.
     * @return Acceleration [m/s^2].
     */
    public static double acceleration(jDriver driver, jVehicle leader) {
        // Longitudinal model
        // get input
        double v = driver.getVehicle().getSpeed(); // own speed [m/s]
        double s; // net headway [m]
        double dv; // speed difference [m/s]
        double v0 = updateDesiredVelocity(driver);
        if (leader!=null) {
            s = driver.getVehicle().getGap(leader);
            dv = v-leader.getSpeed();
        } else {
            s = Double.POSITIVE_INFINITY;
            dv = 0;
        }
        double acc = 0;
        // calculate acceleration
        acc = acceleration(driver, v, dv, v0, s);
        // limit acceleration
        acc = Math.max(acc, driver.getVehicle().getMaxDeceleration());
        return acc;
    }

    /**
     * Same as <code>acceleration()</code> with single input. This
     * method additionally searches for the correct driver in the following
     * vehicle.
     * @param follower Vehicle of considered driver.
     * @param leader Acceleration is based on this vehicle.
     * @return Acceleration [ms/^2].
     */
    public static double acceleration(jVehicle follower, jVehicle leader) {
        return acceleration(follower.getDriver(), leader);
    }


    public static double acceleration(jVehicle follower, double gapWithLeader, double leaderSpeed) {
        return acceleration(follower.getDriver(), gapWithLeader, leaderSpeed);
    }

    public static double acceleration(jDriver driver, double distanceToBlockingObject, double blockingObjectSpeed) {
        double v = driver.getVehicle().getSpeed(); // own speed [m/s]
        double s = distanceToBlockingObject; // net headway [m]
        double dv = v - blockingObjectSpeed; // speed difference [m/s]
        double v0 = updateDesiredVelocity(driver);
        double acc = 0;
        // calculate acceleration
        acc = acceleration(driver, v, dv, v0, s);
        // limit acceleration
        acc = Math.max(acc, driver.getVehicle().getMaxDeceleration());
        return acc;
    }

    /**
     * Calculates the acceleration towards an object.
     * @param driver the driver of the vehicle.
     * @param v Own velocity.
     * @param dv Velocity difference with object.
     * @param v0 Desired velocity.
     * @param s Distance to object.
     * @return Acceleration towards object.
     */
    public static double acceleration(jDriver driver, double v, double dv, double v0, double s) {
        double ss = desiredEquilibriumHeadway(driver)+(v*dv)/(2*Math.sqrt(driver.get(IDMPlus.A)*driver.get(IDMPlus.B))); // dynamic desired gap
    /* Because of the power of 2, the IDMPlus inteprets a negative sign of
     * either s and ss as positive. In all cases this makes no sense.
     */
        ss = Math.max(ss, 0);
        s = Math.max(s, 1e-99); // no division by zero
        return driver.get(IDMPlus.A)*Math.min(1-Math.pow((v/v0),driver.get(IDMPlus.DELTA)), 1-Math.pow((ss/s),2));
    }

    /**
     * The desired headway method returns the desired <b>equilibrium</b> net
     * headway. It is used by the vehicle generator, and possibly the driver
     * itself.
     * @return Desired net headway [m]
     */
    public static double desiredEquilibriumHeadway(jDriver driver) {
        return desiredEquilibirumHeadway(driver.getVehicle().getSpeed(),
                                         driver.get(IDMPlus.S0),
                                         driver.get(T));
    }


    /**
     * This method updates the desired speed on the current lane and returns the current value.
     * It is used by the vehicle generator, and possibly the driver itself.
     * @return Desired velocity [m/s]
     */
    public static double updateDesiredVelocity(jDriver driver) {
        double desiredVelocity = desiredVelocity(driver);
        driver.set(IDMPlus.V0, desiredVelocity);
        return driver.get(IDMPlus.V0);
    }

    public static double desiredVelocity(jDriver driver) {
        double vMax = driver.getVehicle().getMaxSpeed();
        double laneSpeedLimit = driver.getVehicle().getLane().getVLimInMetersPerSecond();
        return desiredVelocity(vMax, laneSpeedLimit, driver.get(IDMPlus.F_SPEED));
    }
    public static double desiredVelocity(double maxSpeed, double speedLimit, double speedLimitAdeherenceFactor) {
        return Math.min(maxSpeed, speedLimitAdeherenceFactor * speedLimit);
    }

    public static double desiredEquilibirumHeadway(double v, double s0, double t) {
        return s0 + v * t;
    }

    public static double acceleration(double s0, double v0, double v, int delta, double t, double deltaV, double s, double a, double b) {
        return desiredAcceleration(a, v, v0, delta) - a * Math.pow(desiredDynamicDistance(s0, v, t, deltaV, a, b)/s,2);
    }

    public static double desiredAcceleration(double a, double v, double v0, int delta) {
        return a * (1-Math.pow(v/v0, delta));
    }

    public static double desiredDynamicDistance(double s0, double v, double t, double deltaV, double a, double b) {
        return s0 + Math.max(0, v*t + (v * deltaV)/(2 * Math.sqrt(a*b)));
    }


    public static double anticipatedAcceleration(jVehicle follower, jVehicle leader, double t) {
        double s0 = follower.getDriver().get(IDMPlus.S0);
        double v0 = desiredVelocity(follower.getDriver());
        double v = follower.getSpeed();
        Integer delta = follower.getDriver().get(IDMPlus.DELTA);
        double deltaV = follower.getSpeed() - leader.getSpeed();
        double s = follower.getGap(leader);
        Double a = follower.getDriver().get(IDMPlus.A);
        Double b = follower.getDriver().get(IDMPlus.B);
        return acceleration(s0, v0, v, delta, t, deltaV, s, a, b);
    }
}
