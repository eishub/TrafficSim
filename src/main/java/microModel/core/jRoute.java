package microModel.core;

import microModel.core.road.jLane;

/**
 * Simple representation of a route. The route is represented as an array of
 * destinations that should match <tt>jLane.destination</tt> attributes.
 */
public class jRoute {
    
    /** Array of destinations. */
    protected int[] destinations;
    
    /**
     * Default constructor.
     * @param destinations Ordered (intermediate) destinations of this route.
     */
    public jRoute(int[] destinations) {
        this.destinations = destinations;
    }
    
    /**
     * Returns the array of destinations.
     * @return Destination array.
     */
    public int[] destinations() {
        return destinations;
    }
    
    /**
     * Returns the sub-route after an intermediate destination has been passed.
     * If the intermediate destination is not part of the route, the full route
     * is returned.
     * @param destination Passed destination.
     * @return Sub-route after the passed destination.
     */
    public jRoute subRouteAfter(int destination) {
        int i = 0;
        for (int j=0; j<destinations.length; j++) {
            if (destinations[j]==destination) {
                i = j+1; // +1 for 'after'
            }
        }
        int[] newRoute = new int[destinations.length-i];
        for (int j=i; j<destinations.length; j++) {
            newRoute[j-i] = destinations[j];
        }
        return new jRoute(newRoute);
    }
    
    /**
     * Returns whether this route can be followed from the given lane.
     * @param lane LaneType of which needs to be known if the route can be followed.
     * @return Whether this route can be followed from the given lane.
     */
    public boolean canBeFollowedFrom(jLane lane) {
        if (lane==null) {
            return false;
        }
        for (int i=0; i<destinations.length; i++) {
            if (lane.leadsTo(destinations[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the number of lane changes that need to be performed to follow
     * this route from the given lane.
     * @param lane Considered lane.
     * @return Number of lane changes that needs to be performed for this route.
     */
    public int nLaneChanges(jLane lane) {
        for (int i=0; i<destinations.length; i++) {
            if (lane.leadsTo(destinations[i])) {
                return lane.nLaneChanges(destinations[i]);
            }
        }
        return 0;
    }
    
    /**
     * Returns the distance within which a number of lane changes has to be
     * performed to follow this route from the given lane.
     * @param lane Considered lane.
     * @return Distance [m] within which a number of lane changes has to be performed.
     */
    public double xLaneChanges(jLane lane) {
        for (int i=0; i<destinations.length; i++) {
            if (lane.leadsTo(destinations[i])) {
                return lane.xLaneChanges(destinations[i]);
            }
        }
        return 0;
    }
}