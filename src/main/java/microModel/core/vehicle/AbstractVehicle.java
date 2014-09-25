package microModel.core.vehicle;

import eis.exceptions.EntityException;
import eis.exceptions.RelationException;
import microModel.core.driver.jDriver;
import microModel.core.road.LaneType;
import microModel.core.road.LatDirection;
import microModel.core.road.LongDirection;
import microModel.core.road.jLane;
import microModel.jModel;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class AbstractVehicle extends Movable implements jVehicle{
    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(AbstractVehicle.class);

    protected boolean crashed;

    /** LaneType where the movable is at. */
    private jLane lane;

    /** Position on the lane. */
    private double x;

    /** Used for keeping track of moving objects in the surroundings of this vehicle. */
    private Map<Enclosure, jVehicle> surroundings = new HashMap<Enclosure, jVehicle>();

    /** Left indicator on position. */
    private boolean leftIndicator;

    /** Right indicator on position. */
    private boolean rightIndicator;

    /** Boolean which is used for pointer book-keeping. */
    private boolean justExceededLane;

    /** Maximum vehicle deceleration (a value below 0) [m/s^2]. */
    protected double maxDeceleration;
    /** Maximum vehicle acceleration (a value above 0) [m/s^2]. */
    protected double maxAcceleration;

    public SortedMap<Double, Double> accelerations = new TreeMap<Double, Double>();

    protected AbstractVehicle() {
        surroundings.put(Enclosure.CURRENT_LOCATION, this);
    }

    @Override
    public synchronized jLane getLane() {
        return lane;
    }

    @Override
    public void sense() {
        for (Enclosure e: Enclosure.values()) {
            AbstractVehicle vehicle = this.getLane().search(e, getX());
            updateSurrounding(e, vehicle);
        }
        jVehicle downStreamVehicle = surroundings.get(Enclosure.DOWNSTREAM);
        if (downStreamVehicle != null && downStreamVehicle.getLane() == getLane() && downStreamVehicle.getX() <= this.getX() + getLength()) {
            setCrashed(true);
        }
    };


    protected synchronized void setLane(jLane lane){
        this.lane = lane;
    }

    /**
     * Places this vehicle on a new lane and informs the relevant lane observers of this change.
     * @param newLane The {@link jLane lane} on which to place this vehicle.
     * @param fromX The X position from which the vehicle entered the {@code newLane}.
     *              This has to be specified in the coordinate system of the {@code newLane}.
     *              For example if the vehicle entered from the beginning of the {@code newLane}
     *              as a result of reaching the end of an upstream lane this should be 0. On the
     *              other hand if this was due to a lane change maneuver this should be the X position
     *              from which the the vehicle started its lane change maneuver.
     * @param atX The X position on which the vehicle should be after this function call. This
     *            should be in the coordinate system of the {@code newLane}.
     */
    protected synchronized void updateLane(jLane newLane, double fromX, double atX) {
        if (lane != null) {
            lane.removeVehicle(this);
        }
        lane = newLane;
        lane.addVehicle(this, fromX, atX);
    }

    @Override
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    @Override
    public jVehicle getVehicle(Enclosure area) {
        jVehicle vehicle = surroundings.get(area);
        return (vehicle != null) ? vehicle : null;
    }

    @Override
    public void updateSurrounding(Enclosure area, jVehicle vehicle) {
        if (area != Enclosure.CURRENT_LOCATION) {
            surroundings.put(area, vehicle);
        }
    }

    @Override
    public boolean isIndicatingLeft() {
        return leftIndicator;
    }

    @Override
    public void toggleLeftIndicator() {
        leftIndicator = !leftIndicator;
    }

    @Override
    public boolean isIndicatingRight() {
        return rightIndicator;
    }

    @Override
    public void toggleRightIndicator() {
        rightIndicator = !rightIndicator;
    }

    @Override
    public boolean isBraking() {
        return a < 0;
    }

    public abstract jDriver getDriver();

    /**
     * Returns the projected vehicle movement using current speed,
     * acceleration and simulator time step size.
     * @return
     */
    public double getDx() {
        double dt = jModel.getInstance().getStepSize();
        double dx = dt * v + .5 * a * dt * dt;
        return dx;
    }

    /**
     * Returns the global x and y coordinate of the vehicle
     * NOTE: It assumes that the vehicle is positioned at the middle
     * of the lane in the Lateral direction (y coordinate).
     *
     * @return {@code Point2D.Double} with the global x, y coordinates.
     */
    public Point2D.Double atLaneXY() {
        return getLane().XY(getX());
    }

    /**
     * Returns the same x coordinate on an adjacent lane.
     * @param dir Direction of the adjacent lane. (see {@link LatDirection})
     * @return Adjacent X coordinate in the lane coordinate system.
     */
    public double getAdjacentX(LatDirection dir) {
        return getLane().getAdjacentX(getX(), dir);
    }

    @Override
    public double getTimeGap(jVehicle leader) {
        double spaceHeadway = getGap(leader);
        return getSpeed() == 0 ? Double.MAX_VALUE: spaceHeadway/getSpeed();
    }

    public double getSafeSpeed(double distance) {
        return getDriver().getSafeSpeed(distance, 2.0, 2.0);
    }

    @Override
    public double getGap(jVehicle leader) {
        /* Implementation note: using lane.isSame(otherLane) would give more
         * readable code here, however, since this method is so frequently used,
         * a lower-level approach is opted. The basic idea is that it is more
         * effecient to check whether the leader is on the same, left or right
         * lane (at the same section) before using xAdj() to check for up- or
         * downstream connectivity. This is especially true as by far most
         * requests will be to leaders on the same jLane.
         */
        double s = 0;
        if (leader == null) {
            if (getLane().getType() == LaneType.MERGE || getLane().getType() == LaneType.SUBTRACTED) {
                return getDistanceToLaneEnd();
            }
            return Double.MAX_VALUE;
        }
        if (getLane() == leader.getLane()) {
            // same lane
            s = leader.getX() - getX();
        } else if (getLane() == leader.getLane().getLeft()) {
            // leader is right
            s = leader.getAdjacentX(LatDirection.LEFT) - getX();
        } else if (getLane() == leader.getLane().getRight()) {
            // leader is left
            s = leader.getAdjacentX(LatDirection.RIGHT) - getX();
        } else if (getLane().xAdj(leader.getLane()) != 0) {
            // leader is up- or downstream
            s = leader.getX() + getLane().xAdj(leader.getLane()) - getX();
        } else if (getLane().xAdj(leader.getLane().getLeft()) != 0) {
            // leader is on right lane up- or downstream
            s = leader.getAdjacentX(LatDirection.LEFT) + getLane().xAdj(leader.getLane().getLeft()) - getX();
        } else if (getLane().getRight() != null && getLane().getRight().xAdj(leader.getLane()) != 0) {
            // leader is on right lane up- or downstream (no up/down lane)
            s = leader.getX() + getLane().getRight().xAdj(leader.getLane()) - getAdjacentX(LatDirection.RIGHT);
        } else if (getLane().xAdj(leader.getLane().getRight()) != 0) {
            // leader is on left lane up- or downstream
            s = leader.getAdjacentX(LatDirection.RIGHT) + getLane().xAdj(leader.getLane().getRight()) - getX();
        } else if (getLane().getLeft() != null && getLane().getLeft().xAdj(leader.getLane()) != 0) {
            // leader is on left lane up- or downstream (no up/down lane)
            s = leader.getX() + getLane().getLeft().xAdj(leader.getLane()) - getAdjacentX(LatDirection.LEFT);
        } else if (leader.getLane().getLeft() != null && leader.getLane().getLeft().getLeft() != null && getLane() ==leader.getLane().getLeft().getLeft()) {
            // leader is on the right+right lane of the vehicle
            double tmpX = leader.getLane().getAdjacentX(leader.getX(), LatDirection.LEFT);
            tmpX = leader.getLane().getLeft().getAdjacentX(tmpX, LatDirection.LEFT);
            s = tmpX - getX();
        } else if (leader.getLane().getRight() != null && leader.getLane().getRight().getRight() != null && getLane() == leader.getLane().getRight().getRight()) {
            // leader is on the left+left lane of the vehicle
            double tmpX = leader.getLane().getAdjacentX(leader.getX(), LatDirection.RIGHT);
            tmpX = leader.getLane().getRight().getAdjacentX(tmpX, LatDirection.RIGHT);
            s = tmpX - getX();
        }
        s = s - leader.getLength(); // gross -> net
        return s;
    }

    @Override
    public double getDistanceToLaneEnd() {
        return lane.getL() - getX() - getLength();
    }

    @Override
    public double getMaxDeceleration() {
        return maxDeceleration;
    }

    @Override
    public double getMaxAcceleration() {
        return maxAcceleration;
    }

    /**
     * Deletes a vehicle entirely while taking care of any neighbour reference
     * to the vehicle.
     */
    @Override
    public void delete() {
        /* TODO: When deleting a vehicle, all pointers to it need to be removed in
         * order for the garbage collector to remove the object from memory.
         * Vehicles are referenced from: model, lane, OBU, driver, trajectory,
         * lcVehicle<->vehicle and neighbouring vehicles.
         */
        final jModel model = jModel.getInstance();
        model.removeVehicle(this);
        getLane().getVehicles().remove(this);
        jModel.APL_UPDATE_THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    model.getEnvironment().deleteEntity("driver" + getDriver().getID());
                } catch (EntityException e) {
                    e.printStackTrace();
                } catch (RelationException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Places a vehicle on a lane, sets new neighbours and sets this vehicle as
     * neighbour of surrounding vehicles.
     *
     * @param atLane LaneType where the vehicle needs to be placed at.
     * @param atX    Location where the vehicle needs to be placed at.
     */
    @Override
    public void paste(jLane atLane, double atX) {
        // In case the lane is exceeded, change the lane to search on. This
        // could occur when searching for neighbours when ending a lane change
        // within the same time step a lane is exceeded.
        setJustExceededLane(true);
        if (atX > atLane.getL() && atLane.getDown() != null) {
            paste(atLane.getDown(), atX - atLane.getL());
            //also notify the lanes that have already been passed so they can notify their observers.
            updateLane(atLane, 0, atLane.getL());
            return;
        }
        // set properties
        setX(atX);
        updateLane(atLane, 0, atX);
    }

    /**
     * Cuts a vehicle from a lane. All pointers from the lane and neighbours
     * to this vehicle are updated or removed.
     */
    @Override
    public void cut() {
        jModel model = jModel.getInstance();
        // remove from lane vector
        getLane().getVehicles().remove(this);

        // delete own references
        updateSurrounding(Enclosure.UPSTREAM, null);
        updateSurrounding(Enclosure.DOWNSTREAM, null);
        updateSurrounding(Enclosure.LEFT_UPSTREAM, null);
        updateSurrounding(Enclosure.LEFT_DOWNSTREAM, null);
        updateSurrounding(Enclosure.RIGHT_UPSTREAM, null);
        updateSurrounding(Enclosure.RIGHT_DOWNSTREAM, null);
    }

    public boolean isJustExceededLane() {
        return justExceededLane;
    }

    protected void setJustExceededLane(boolean justExceededLane) {
        this.justExceededLane = justExceededLane;
    }

    public boolean isCrashed() {
        return this.crashed;
    }

    public void setCrashed(boolean crashed) {
        this.crashed = crashed;
    }
}
