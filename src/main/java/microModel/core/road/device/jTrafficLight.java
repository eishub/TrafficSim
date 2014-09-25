package microModel.core.road.device;

import microModel.core.driver.jDriver;
import microModel.core.observation.jObservable;
import microModel.core.road.jLane;

/**
 * Single traffic light.
 */
public class jTrafficLight extends AbstractRSU {

    /** Light color. */
    protected lightColor color = lightColor.GREEN;

    /**
     * Constructor that sets the traffic light as noticeable.
     * @param lane LaneType where the traffic light is at.
     * @param position Position on the lane.
     */
    public jTrafficLight(jLane lane, double position) {
        super(lane, position, false, true);
    }
    
    /** Empty, needs to be implemented.  */
    public void init() {}
    
    /** 
     * Empty, needs to be implemented.
     * @param observable Passing vehicle.
     */
    public void see(jObservable observable) {}

    /** Empty, needs to be implemented.  */
    public void control() {}
    
    /** 
     * Returns whether the traffic light is currently green.
     * @return Whether the traffic light is green.
     */
    public boolean isGreen() {
        return color==lightColor.GREEN;
    }
    
    /** 
     * Returns whether the traffic light is currently yellow. 
     * @return Whether the traffic light is yellow.
     */
    public boolean isYellow() {
        return color==lightColor.YELLOW;
    }
    
    /** 
     * Returns whether the traffic light is currently red. 
     * @return Whether the traffic light is red.
     */
    public boolean isRed() {
        return color==lightColor.RED;
    }
    
    /** Sets the traffic light to green. */
    public void setGreen() {
        color = lightColor.GREEN;
    }
    
    /** Sets the traffic light to yellow. */
    public void setYellow() {
        color = lightColor.YELLOW;
    }
    
    /** Sets the traffic light to red. */
    public void setRed() {
        color = lightColor.RED;
    }

    /** Empty, needs to be implemented.  */
    public void noControl() {}

    /** Enumeration for traffic light colors. */
    protected enum lightColor {
        /** Light is red. */
        RED,
        /** Light is yellow (or orange). */
        YELLOW,
        /** Light is green. */
        GREEN
    }
}