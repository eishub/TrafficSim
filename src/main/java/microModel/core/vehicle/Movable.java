package microModel.core.vehicle;

import microModel.core.observation.jObserver;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This class has the common functionality of regular vehicles and temporary
 * lane change vehicles. This is the position on the network and relative to
 * neighbouring movables. Common methods are related to position, neighbours and
 * visualisation.
 */

public abstract class Movable implements jMovable {

    /** List of Observers subscribed to be notified of state changes of this object.
     * For example lanes subscribe to be notified of movement in order to delegate
     * information to detectors.
     */
    protected List<jObserver> observers = new ArrayList<jObserver>();
    /** Normalized heading of the vehicle. */
    public Point2D.Double heading = new Point2D.Double();
    /** Speed of the movable [m/s]. */
    protected double v;
    /** Acceleration of the movable [m/s^2]. */
    protected double a;
    /** Movable length [m]. */
    protected double l;
    /** Global x and y coordinates. */
    private Point2D.Double coordinates;

    @Override
    public Point2D.Double getCoordinates() {
        return this.coordinates;
    }

    /** Sets the global x and y positions, as implemented by a subclass. */
    protected void setCoordinates(Point2D.Double coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public Point2D.Double getHeading() {
        return this.heading;
    }

    @Override
    public void setHeading(final Point2D.Double heading) {
        double normalizationFactor = Math.sqrt(Math.pow(heading.x, 2) + Math.pow(heading.y, 2));
        this.heading = new Point2D.Double(heading.x / normalizationFactor, heading.y / normalizationFactor);
    }

    @Override
    public synchronized double getSpeed() {
        return this.v;
    }

    @Override
    public synchronized void setSpeed(double v) {
        this.v = v;
    }

    @Override
    public synchronized double getAcceleration() {
        return this.a;
    }

    @Override
    public synchronized void setAcceleration(double a) {
        this.a = a;
    }

    @Override
    public double getLength() {
        return this.l;
    }

    @Override
    public void addObserver(jObserver observer) {
        observers.add(observer);
    }

    @Override
    public void detachObserver(jObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void detachAllObservers() {
        observers.clear();
    }

    /**
     * Moves the moveable object according to acceleration, speed and heading.
     * @param dt The time delta for calculating the movement of the object.
     */
    public abstract void move(double dt);

}