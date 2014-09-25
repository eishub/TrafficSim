package microModel.core.vehicle;

import microModel.core.driver.jDriver;
import microModel.core.road.LatDirection;
import microModel.core.road.jLane;

public interface jVehicle extends jMovable {
    /**
     * Returns a vehicle in the direct surrounding depending on which area is provided.
     * see {@link Enclosure} for possible areas.
     * @param area The area to look in
     * @return The neighboring vehicle in the provided area.
     */
    public jVehicle getVehicle(Enclosure area);

    /**
     * Sets a vehicle to be in the provided neighboring area (see {@link Enclosure}).
     * @param area The area in which the neighboring vehicle resides.
     * @param vehicle The vehicle.
     */
    public void updateSurrounding(Enclosure area, jVehicle vehicle);

    /**
     * @return The current lane.
     */
    public jLane getLane();

    /**
     * Returns the distance in [m] which the vehicle has travelled along the current lane.
     * @return Distance in [m]
     */
    public double getX();

    /**
     * Returns the distance in [m] on an adjacent lane of the the vehicle's current location.
     * This method takes care of lane length difference and curvatures.
     *
     * @param dir Direction of the adjacent lane.
     * @return Distance in [m] of location on adjacent lane.
     */
    public double getAdjacentX(LatDirection dir);

    /**
     * Returns the net headway in [m] to a given vehicle. This vehicle should be on the
     * same or an adjacent lane, or anywhere up- or downstream of those lanes.
     *
     * @param leader Leading vehicle, not necessarily the 'down' vehicle.
     * @return Net headway with leader in [m].
     */
    public double getGap(jVehicle leader);

    /**
     * Returns the current distance in [m] to the end of the current lane segment.
     * @return Distance in [m] to the end of the lane.
     */
    public double getDistanceToLaneEnd();

    /**
     * Returns the net headway in [s] to a given vehicle. This vehicle should be on the
     * same or an adjacent lane, or anywhere up- or downstream of those lanes.
     *
     * @param leader Leading vehicle, not necessarily the 'down' vehicle.
     * @return Net headway with leader in [m].
     */
    public double getTimeGap(jVehicle leader);

    /**
     * @return the driver of any jVehicle..
     */
    public jDriver getDriver();

    /**
     * Returns if the vehicle's left indicator is blinking (signaling a left turn or lane change)
     * @return True if left indicator is blinking, false otherwise.
     */
    public boolean isIndicatingLeft();

    /**
     * Toggles the left indicator light of the vehicle.
     */
    public void toggleLeftIndicator();

    /**
     * Returns if the vehicle's right indicator is blinking (signaling a right turn or lane change)
     * @return True if the right indicator is blinking, false otherwise.
     */
    public boolean isIndicatingRight();

    /**
     * Toggles the right indicator light of the vehicle.
     */
    public void toggleRightIndicator();

    /**
     * @return True is the vehicle is braking and false otherwise.
     */
    public boolean isBraking();

    /**
     * @return The maximum speed achievable by the vehicle in [m/s].
     */
    public double getMaxSpeed();

    /**
     * @return The maximum deceleration of the Vehicle in [m/s^2]
     */
    public double getMaxDeceleration();

    /**
     * @return The maximum acceleration of the vehicle in [m/s^2]
     */
    public double getMaxAcceleration();

    /**
     * @return true if vehicle is in the process of changing lane and false otherwise.
     */
    public boolean isChangingLane();

    /**
     * @return a number in the range [0 .. 1] indicating the progress of the lane change.
     */
    public double getLaneChangeProgress();

    /**
     * @return direction in which the vehicle is changing lane if vehicle changing lane. null otherwise.
     */
    public LatDirection getLaneChangeDirection();

    /**
     * Starts the lane changing process of a vehicle. This is an action that takes some time to complete.
     * The state of a lane change can be checked with the {@link #isChangingLane()} method.
     * @param direction The direction in which the lane change occurs. This can either be {@code LatDirection.LEFT}
     *                  or {@code LatDirection.RIGHT}
     * @param steeringAngle A value between (0..1] to indicate how fast the lane change has to occur.
     */
    public void changeLane(LatDirection direction, double steeringAngle);

    /**
     * Aborts a lane change instantaneously
     * (see {@link #changeLane(microModel.core.road.LatDirection, double)})
     */
    public void abortLaneChange();

}
