package microModel.core.vehicle;

import microModel.core.observation.jObservable;
import microModel.core.road.jLane;

import java.awt.geom.Point2D;


/**
 * The general functionality of any object that can move in the simulator
 * is summarized in this interface.
 *
 * This interface also extends the {@link jObservable} interface because objects that
 * move in the simulation should be observable by for example detectors on lanes.
 */
public interface jMovable  extends jObservable {

    /**
     * Uses the movable object's acceleration, speed and heading to move the vehicle.
     * @param dt The time delta for calculating the movement of the object.
     */
    public void move(double dt);

    /**
     * Allows moving objects to sense other objects around them.
     */
    public void sense();

    /**
     * Returns the global coordinates of the object.
     *
     * @return A {@link Point2D.Double} object representing the global coordinates of the object.
     */
    public Point2D.Double getCoordinates();

    /**
     * Returns the a 2D Normalized vector indicating the heading of the object.
     *
     * @return A {@link Point2D.Double} giving the heading vector.
     */
    public Point2D.Double getHeading();

    /**
     * Sets the heading of object.
     *
     * @param heading New heading for the object. If the vector is not normalized it will be.
     */
    public void setHeading(final Point2D.Double heading);

    /**
     * Returns the current speed of the object.
     *
     * @return Speed in [m/s].
     */
    public double getSpeed();

    /**
     * Sets the current speed of the object.
     *
     * @param v speed in [m/s].
     */
    public void setSpeed(double v);

    /**
     * Returns the acceleration of the object.
     *
     * @return Acceleration in [m/s^2]
     */
    public double getAcceleration();

    /**
     * Sets the acceleration of the object.
     *
     * @param a Acceleration in [m/s^2].
     */
    public void setAcceleration(double a);

    /**
     * Returns the lenght of the object.
     *
     * @return Length in [m]
     */
    public double getLength();

    /** Deletes a movable object from the simulation */
    public void delete();

    /** TODO */
    public void cut();

    /**
     * TODO
     *
     * @param lane
     * @param x
     */
    public void paste(jLane lane, double x);
}
