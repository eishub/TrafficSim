package microModel.core.vehicle;

import microModel.core.driver.jDriver;
import microModel.core.observation.jObserver;
import microModel.core.vehicle.device.AbstractOBU;
import microModel.core.road.device.AbstractRSU;
import microModel.core.road.LatDirection;
import microModel.core.road.jLane;
import microModel.jModel;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;

/** Default wrapper for a vehicle. It contains a driver and possibly an OBU. */
public class Vehicle extends AbstractVehicle {
    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(Vehicle.class);

    /** OBU within the vehicle, may be <tt>null</tt>. */
    public AbstractOBU OBU;

    /** Driver of the vehicle. */
    public jDriver driver;

    /** Current lateral speed in 'amount of lane per time step' [0...1]. */
    public double dy;

    /** Total progress of a lane change in 'amount of lane' [0...1]. */
    public double laneChangeProgress;

    /** Lane change direction. */
    public LatDirection lcDirection;

    /** Maximum vehicle speed [km/h]. */
    public double vMax;

    /** Vehicle class ID. */
    public int classID;

    @Override
    public boolean isChangingLane() {
        return laneChangeProgress > 0 ;
    }

    @Override
    public double getLaneChangeProgress() {
        return laneChangeProgress;
    }

    @Override
    public LatDirection getLaneChangeDirection() {
        if (isChangingLane())
            return lcDirection;
        return null;
    }

    @Override
    public void move(double dt) {
        /**
         * TODO: The correct calculation is the following:
         *       given:
         *       heading = (hx, hy)
         *       c = hy/hx
         *       dm = a * dt ^ 2 + 0.5 * v * dt
         *       then
         *       dx = dm / sqrt( c^2 + 1 )
         *       dy = c * dm / sqrt( C^2 + 1 )
         * NOTE: But this messes the whole way the simulator works.
         *       So I have to fix this later on to
         *       Also give vehicles control over the steering.
         *       This will be a big change in the simulator.
         */

        if (!crashed) {
            if (jSettings.getInstance().get(BuiltInSettings.DEBUG_MODEL)) {
                accelerations.put(jModel.getInstance().getT(), a);
            }
            // lateral
            setJustExceededLane(false);
            laneChangeProgress = laneChangeProgress + dy;
            // longitudinal
            double dx = dt * v + .5 * a * dt * dt;
            dx = dx >= 0 ? dx : 0;
            v = v + dt * a;
            v = v >= 0 ? v : 0;
            //Notifies the lane that this vehicle is moving.
            for (jObserver observer: observers) {
                observer.see(this);
            }
            translate(dx, dy);
            setXY();
            if ((dy != 0) && (laneChangeProgress >=1)) {
                endLaneChange();
            }
        }
    }

    /**
     * Function to translate a distance, moving onto downstream lanes as needed.
     * If a destination is reached the vehicle is deleted.
     *
     * @param dx Distance [m] to translate.
     * @param dy this is ignored for now.
     */
    private void translate(double dx, double dy) {

        jModel model = jModel.getInstance();
        jSettings settings = jSettings.getInstance();

        // Move movable downstream
        setX(getX() + dx);
        setJustExceededLane(false);
        if (getX() > getLane().getL()) {
            setJustExceededLane(true);
            if (getLane().getDestination() == this.driver.getRoute().destinations()[0]) {
                logger.debug("Vehicle reached destination " + getLane().getDestination());
                delete();
            } else if (getLane().getDown() == null && getLane().getDestination() == 0) {
                logger.debug("Vehicle deleted as lane " + getLane().getId() + " is exceeded, dead end");
                delete();
            } else if (getLane().getDown() == null && getLane().getDestination() > 0) {
                // vehicle has reached (a) destination
                logger.debug("Vehicle reached the WRONG destination " + getLane().getDestination());
                delete();
            } else if (!this.getDriver().getRoute().canBeFollowedFrom(getLane().getDown())) {
                logger.debug("Vehicle deleted as lane " + getLane().getId() + " is exceeded, route unreachable");
                delete();
            } else {
                // update route
                if (getLane().getDestination() > 0) {
                    driver.setRoute(driver.getRoute().subRouteAfter(getLane().getDestination()));
                }
                // check whether adjacent neighbours need to be reset
                // these will be found automatically by updateNeighbour() in
                // the main model loop
                if (getLane().getLeft() != null && getLane().getLeft().getDown() != getLane().getDown().getLeft()) {
                    updateSurrounding(Enclosure.LEFT_UPSTREAM, null);
                    updateSurrounding(Enclosure.LEFT_DOWNSTREAM, null);
                }
                if (getLane().getRight() != null && getLane().getRight().getDown() != getLane().getDown().getRight()) {
                    updateSurrounding(Enclosure.RIGHT_UPSTREAM, null);
                    updateSurrounding(Enclosure.RIGHT_DOWNSTREAM, null);
                }
                // put on downstream lane
                double atX = getX() - getLane().getL();
                paste(getLane().getDown(), atX);
            }
        }
    }

    /** Ends a lane change by deleting the <tt>lcVehicle</tt> and changing the lane. */
    private void endLaneChange() {
        // set vehicle at target lane and delete temporary vehicle
        jLane targetLane;
        double targetX = getAdjacentX(lcDirection);
        if (lcDirection == LatDirection.LEFT) {
            targetLane = getLane().getLeft();
            toggleLeftIndicator();
        } else {
            targetLane = getLane().getRight();
            toggleRightIndicator();
        }
        cut();
        paste(targetLane, targetX);
        laneChangeProgress = 0;
        dy = 0;
    }

    public void abortLaneChange() {
        // instantaneous abort of lane change
        laneChangeProgress = 0;
        dy = 0;
    }


    @Override
    public void changeLane(LatDirection direction, double steeringAngle) {
        lcDirection = direction;
        if (lcDirection == LatDirection.LEFT) {
            toggleLeftIndicator();
        } else {
            toggleRightIndicator();
        }
        dy = steeringAngle;
    }

    /**
     * Sets global x and y coordinates. This may be in between two lanes in case
     * of a lane change.
     */
    private void setXY() {
        Point2D.Double coord = atLaneXY();
        setCoordinates(new Point2D.Double(coord.x, coord.y));
        heading = getLane().heading(getX());
    }

    /**
     * Returns the maximum vehicle speed in m/s.
     *
     * @return Maximum vehicle speed [m/s].
     */
    @Override
    public double getMaxSpeed() {
        return vMax / 3.6;
    }

    /**
     * Returns the distance between a vehicle and an RSU.
     *
     * @param rsu RSU.
     * @return Distance [m] between vehicle and RSU.
     */
    public double getDistanceToRSU(AbstractRSU rsu) {
        return rsu.getX() + getLane().xAdj(rsu.lane) - getX();
    }

    /**
     * Returns whether the vehicle is equipped with an OBU.
     *
     * @return Whether the car is equiped.
     */
    public boolean isEquipped() {
        return OBU != null;
    }

    /**
     * Returns the driver of this vehicle.
     *
     * @return Driver of this vehicle.
     */
    public jDriver getDriver() {
        return driver;
    }

    public void setDriver(jDriver driver) {
        this.driver = driver;
        driver.setVehicle(this);
    }


    public static final class Builder {
        private double vMax = 160;
        private double maxDeceleraion = -10;
        private double maxAcceleration = 2;
        private double l = 5;
        private jLane lane;

        public Builder(jLane defaultLane) {
            this.lane = defaultLane;
        }

        public Builder withvMax(double vMax) {
            this.vMax = vMax;
            return this;
        }

        public Builder withMaxDeceleration(double maxDeceleration) {
            this.maxDeceleraion = - Math.abs(maxDeceleration);
            return this;
        }

        public Builder withMaxAcceleration(double maxAcceleration) {
            this.maxAcceleration = Math.abs(maxAcceleration);
            return this;
        }


        public Builder withL(double l) {
            this.l = l;
            return this;
        }

        public Builder onLane(jLane lane) {
            this.lane = lane;
            return this;
        }

        public Vehicle build() {
            Vehicle vehicle = new Vehicle();
            vehicle.vMax = vMax;
            vehicle.maxDeceleration = maxDeceleraion;
            vehicle.maxAcceleration = maxAcceleration;
            vehicle.l = l;
            vehicle.setLane(lane);
            vehicle.setXY();
            return vehicle;
        }

    }
}