package microModel.core.driver;

import microModel.core.Parameter;
import microModel.core.driver.model.IDMPlus;
import microModel.core.driver.model.LMRS;
import microModel.core.jRoute;
import microModel.core.road.LatDirection;
import microModel.core.road.LongDirection;
import microModel.core.road.device.jTrafficLight;
import microModel.core.road.jLane;
import microModel.core.vehicle.Enclosure;
import microModel.core.vehicle.jVehicle;
import microModel.jModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Original implementation of drivers in jModel slightly modified to use a Builder pattern for instantiation.
 * TODO: Remove this class because it is no longer needed! Only kept this class to see how the IDM and LMRS implementation origninally looked like.
 *
 */
public class IDMPlus_LMRS_Driver extends AbstractDriver {

        /** Last time when acceleration was lowered using <tt>lowerAcceleration</tt>. */
    protected double tAccLower = -1;

    /** Bookkeeping of influence on anticipated speed from a left lane. */
    protected Map<jLane, Double> antFromLeft = new HashMap<jLane, Double>();

    /** Bookkeeping of influence on anticipated speed from given lane. */
    protected Map<jLane, Double> antInLane = new HashMap<jLane, Double>();

    /** Bookkeeping of influence on anticipated speed from a right lane. */
    protected Map<jLane, Double> antFromRight = new HashMap<jLane, Double>();

    /** Current time of anticipated speed bookkeeping (for <tt>anticipatedSpeed</tt>). */
    protected double tAnt;

    public static class Builder extends jDriver.BuildHelper implements jDriver.Builder {

        private jRoute route;

        public Builder(jRoute defaultRoute) {
            this.route = defaultRoute;
        }

        @Override
        public IDMPlus_LMRS_Driver build() {
            IDMPlus_LMRS_Driver driver = new IDMPlus_LMRS_Driver();
            driver.set(IDMPlus.A, parameters.get(IDMPlus.A) != null ? (Double) parameters.get(IDMPlus.A) : IDMPlus.A.value());
            driver.set(IDMPlus.B, parameters.get(IDMPlus.B) != null ? (Double) parameters.get(IDMPlus.B) : IDMPlus.B.value());
            driver.set(IDMPlus.S0, parameters.get(IDMPlus.S0) != null ? (Double) parameters.get(IDMPlus.S0) : IDMPlus.S0.value());
            driver.set(IDMPlus.T_MAX, parameters.get(IDMPlus.T_MAX) != null ? (Double) parameters.get(IDMPlus.T_MAX) : IDMPlus.T_MAX.value());
            driver.set(IDMPlus.F_SPEED, parameters.get(IDMPlus.F_SPEED) != null ? (Double) parameters.get(IDMPlus.F_SPEED) : IDMPlus.F_SPEED.value());
            driver.set(IDMPlus.T, parameters.get(IDMPlus.T) != null ? (Double) parameters.get(IDMPlus.T) : IDMPlus.T_MAX.value());

            driver.set(LMRS.T_MIN, parameters.get(LMRS.T_MIN) != null ? (Double) parameters.get(LMRS.T_MIN) : LMRS.T_MIN.value());
            driver.set(LMRS.D_FREE, parameters.get(LMRS.D_FREE) != null ? (Double) parameters.get(LMRS.D_FREE) : LMRS.D_FREE.value());
            driver.set(LMRS.D_SYNC, parameters.get(LMRS.D_SYNC) != null ? (Double) parameters.get(LMRS.D_SYNC) : LMRS.D_SYNC.value());
            driver.set(LMRS.D_COOP, parameters.get(LMRS.D_COOP) != null ? (Double) parameters.get(LMRS.D_COOP) : LMRS.D_COOP.value());
            driver.set(LMRS.V_GAIN, parameters.get(LMRS.V_GAIN) != null ? (Double) parameters.get(LMRS.V_GAIN) : LMRS.V_GAIN.value());
            driver.set(LMRS.V_CONG, parameters.get(LMRS.V_CONG) != null ? (Double) parameters.get(LMRS.V_CONG) : LMRS.V_CONG.value());
            driver.set(LMRS.B_SAFE, parameters.get(LMRS.B_SAFE) != null ? (Double) parameters.get(LMRS.B_SAFE) : LMRS.B_SAFE.value());
            driver.set(LMRS.TAU, parameters.get(LMRS.TAU) != null ? (Double) parameters.get(LMRS.TAU) : LMRS.TAU.value());
            driver.set(LMRS.X0, parameters.get(LMRS.X0) != null ? (Double) parameters.get(LMRS.X0) : LMRS.X0.value());
            driver.set(LMRS.T0, parameters.get(LMRS.T0) != null ? (Double) parameters.get(LMRS.T0) : LMRS.T0.value());
            driver.set(LMRS.B_RED, parameters.get(LMRS.B_RED) != null ? (Double) parameters.get(LMRS.B_RED) : LMRS.B_RED.value());

            // Set any other modelParameters that might have been specified
            for(Object p : parameters.keySet()) {
                driver.set((Parameter<Object>) p, parameters.get(p));
            }

            driver.setRoute(route);
            return driver;
        }

    }

    protected IDMPlus_LMRS_Driver() {}


    /**
     * Driver behavior for acceleration and lane changes. This is the main
     * function that sets <code>a</code> (acceleration) and <code>dy</code>
     * (lateral change) of the vehicle. The latter is either set using
     * <tt>vehicle.changeLeft(dy)</tt> or <tt>vehicle.changeRight(dy)</tt>. It
     * combines the Intelligent Driver Model+ (IDMPlus+) and the Lane-change Model
     * with Relaxation and Synchronisation (LMRS).<br>
     * <br>
     * IDMPlus+<br>
     * This is the longitudinal car-following model that determines acceleration
     * based on the gap, velocity and relative velocity.<br>
     * <br>
     * LMRS<br>
     * This is the lateral lane change model that is based on lane change desire
     * based on incentives of: following a route, gaining speed, and following
     * traffic rules. Depending on the level of desire a vehicle may start to
     * synchronize with the target lane, or a gap may even be created.
     */
    @Override
    public void drive() {
        jModel model = jModel.getInstance();
        // Notice RSU's
//        noticeRSUs();

        vehicle.toggleLeftIndicator(); // for vehicle interaction
        vehicle.toggleRightIndicator();

        // Perform the lane change model only when not changing lane
        if (vehicle.laneChangeProgress == 0) {

            /* The headway is exponentially relaxed towards the normal value of
             * Tmax. The relaxation time is tau, which can never be smaller than
             * the time step. Smaller values of T can be set within the model.
             */
            set(IDMPlus.T, get(IDMPlus.T) + (get(IDMPlus.T_MAX)-get(IDMPlus.T))* model.getStepSize() /(get(LMRS.TAU)>= model.getStepSize() ?get(LMRS.TAU): model.getStepSize()));

            /* A lane change is not considered over the first 100m of lanes with
             * generators. Vehicles that are (virtually) upstream of the network
             * would influence this decision so the model is not valid here.
             */
            if (vehicle.getX() >100 || vehicle.getLane().getGenerator() ==null) {

                /* === ROUTE ===
                 * First, the lane change desire to leave a lane for the route
                 * on the left, current and right lane is determined based on
                 * remaining distance and remaining time.
                 */
                // Get remaining distance and number of lane changes from lane
                double xCur = route.xLaneChanges(vehicle.getLane()) - vehicle.getX();
                int nCur = route.nLaneChanges(vehicle.getLane());
                /* Desire to leave the current lane is eiter based on remaining
                 * distance or time. For every lane change required, a certain
                 * distance or time is desired.
                 */
                // Towards the left, always ignore taper on current lane
                double dCurRouteFL = Math.max(Math.max(1-(xCur/(nCur*get(LMRS.X0))),
                        1-((xCur/vehicle.getSpeed())/(nCur*get(LMRS.T0)))), 0);
                // Towards the right, no desire if on taper
                double dCurRouteFR = 0;
                if (!isOnTaper()) {
                    dCurRouteFR = dCurRouteFL;
                }
                /* Determine desire to leave the left lane if it exists and
                 * allows to follow the route.
                 */
                double dLeftRoute = 0;
                if (vehicle.getLane().getLeft() !=null && route.canBeFollowedFrom(vehicle.getLane())) {
                    // The first steps are similar as in the current lane
                    int nLeft = route.nLaneChanges(vehicle.getLane().getLeft());
                    double xLeft = route.xLaneChanges(vehicle.getLane().getLeft())
                            - vehicle.getAdjacentX(LatDirection.LEFT);
                    // We can always include a taper on the left lane if it's there
                    if (!isTaper(vehicle.getLane().getLeft())) {
                        dLeftRoute = Math.max(Math.max(1-(xLeft/(nLeft*get(LMRS.X0))),
                                1-((xLeft/vehicle.getSpeed())/(nLeft*get(LMRS.T0)))), 0);
                    }

                    /* We now have the desire to leave the current, and to leave
                     * the left lane. 'dLeftRoute' will now become the actual
                     * desire to change to the left lane by comparing the two.
                     * If desire to leave the left lane is lower, the desire to
                     * leave the current lane is used. If they are equal, the
                     * desire is zero. If the left lane is worse, the desire to
                     * go left is negative and equal to the desire to leave the
                     * left lane. In this way we have symmetry which prevents
                     * lane hopping.
                     */
                    if (dLeftRoute<dCurRouteFL) {
                        dLeftRoute = dCurRouteFL;
                    } else if (dLeftRoute>dCurRouteFL) {
                        dLeftRoute = -dLeftRoute;
                    } else {
                        dLeftRoute = 0;
                    }
                } else {
                    // Destination becomes unreachable after lane change
                    dLeftRoute = Double.NEGATIVE_INFINITY;
                }
                // Idem. for right lane
                double dRightRoute = 0;
                if (vehicle.getLane().getRight() !=null && route.canBeFollowedFrom(vehicle.getLane())) {
                    int nRight = route.nLaneChanges(vehicle.getLane().getRight());
                    double xRight = route.xLaneChanges(vehicle.getLane().getRight())
                            - vehicle.getAdjacentX(LatDirection.RIGHT);
                    // A taper on the right lane is never applicable
                    dRightRoute = Math.max(Math.max(1-(xRight/(nRight*get(LMRS.X0))),
                            1-((xRight/vehicle.getSpeed())/(nRight*get(LMRS.T0)))), 0);

                    if (dRightRoute<dCurRouteFR) {
                        dRightRoute = dCurRouteFR;
                    } else if (dRightRoute>dCurRouteFR) {
                        dRightRoute = -dRightRoute;
                    } else {
                        dRightRoute = 0;
                    }
                } else {
                    dRightRoute = Double.NEGATIVE_INFINITY;
                }

                /* === SPEED GAIN ===
                 * Drivers may change lane to gain speed. They assess an
                 * anticipated speed.
                 */
                // Get anticipated speeds in current and adjacent lanes
                double vAntLeft = anticipatedSpeed(vehicle.getLane().getLeft());
                double vAntCur = anticipatedSpeed(vehicle.getLane());
                double vAntRight = anticipatedSpeed(vehicle.getLane().getRight());
                /* An acceleration factor is determined. As drivers accelerate
                 * more, their lane change desire for speed reduces. This
                 * prevents slow accelerating vehicles from changing lane when
                 * being overtaken by fast accelerating vehicles. This would
                 * otherwise cause unreasonable lane changes.
                 */
                double aGain = 1-Math.max(IDMPlus.acceleration(vehicle, vehicle.getVehicle(Enclosure.DOWNSTREAM)),0)/get(IDMPlus.A);
                /* Desire to the left is related to a possible speed gain/loss.
                 * The parameter vGain determines for which speed gain the
                 * desire would be 1. Desire is 0 if there is no left lane or if
                 * it is prohibited or impossible to go there.
                 */
                double dLeftSpeed = 0;
                if (vehicle.getLane().isGoLeft()) {
                    dLeftSpeed = aGain*(vAntLeft-vAntCur)/get(LMRS.V_GAIN);
                }
                /* For the right lane, desire due to a speed gain is slightly
                 * different. As one is not allowed to overtake on the right, a
                 * speed gain is reduced to 0. A speed loss is still considered.
                 * For this, traffic should be free flow as one may overtake on
                 * the right in congestion.
                 */
                double dRightSpeed = 0;
                if (vehicle.getLane().isGoRight()) {
                    if (vAntCur>=get(LMRS.V_CONG)) {
                        dRightSpeed = aGain*Math.min(vAntRight-vAntCur, 0)/get(LMRS.V_GAIN);
                    } else {
                        dRightSpeed = aGain*(vAntRight-vAntCur)/get(LMRS.V_GAIN);
                    }
                }

                /* === LANE CHANGE BIAS ===
                 * Drivers have to keep right. It is assumed that this is only
                 * obeyed in free flow and when the anticipated speed on the
                 * right lane is equal to the desired speed. Or in other words,
                 * when there is no slower vehicle nearby in the right lane.
                 * The bias is equal to the free threshold, just triggering
                 * drivers to change in free conditions. Another condition is
                 * that there should be no route undesire towards the right
                 * whatsoever.
                 */
                double dLeftBias = 0;
                double dRightBias = 0;
                if (vAntRight== IDMPlus.updateDesiredVelocity(this) && dRightRoute>=0) {
                    dRightBias = get(LMRS.D_FREE);
                }

                /* === TOTAL DESIRE ===
                 * Depending on the level of desire from the route (mandatory),
                 * the speed and keep right (voluntary) incentives may be
                 * included partially or not at all. If the incentives are in
                 * the same direction, voluntary incentives are fully included.
                 * Otherwise, the voluntary incentives are less and less
                 * considered within the range dSync < |dRoute| < dCoop. The
                 * absolute value of dRouite is used as negative values may also
                 * dominate voluntary incentives.
                 */
                double thetaLeft = 0; // Assume not included
                if (dLeftRoute*(dLeftSpeed+dLeftBias)>=0 || Math.abs(dLeftRoute)<=get(LMRS.D_SYNC)) {
                    // Same direction or low mandatory desire
                    thetaLeft = 1;
                } else if (dLeftRoute*(dLeftSpeed+dLeftBias)<0 &&
                        get(LMRS.D_SYNC)<Math.abs(dLeftRoute) && Math.abs(dLeftRoute)<get(LMRS.D_COOP)) {
                    // Voluntary incentives paritally included
                    thetaLeft = (get(LMRS.D_COOP)-Math.abs(dLeftRoute)) / (get(LMRS.D_COOP)-get(LMRS.D_SYNC));
                }
                set(LMRS.D_LEFT, dLeftRoute + thetaLeft*(dLeftSpeed+dLeftBias));
                // Idem. for right
                double thetaRight = 0;
                if (dRightRoute*(dRightSpeed+dRightBias)>=0 || Math.abs(dRightRoute)<=get(LMRS.D_SYNC)) {
                    thetaRight = 1;
                } else if (dRightRoute*(dRightSpeed+dRightBias)<0 &&
                        get(LMRS.D_SYNC)<Math.abs(dRightRoute) && Math.abs(dRightRoute)<get(LMRS.D_COOP)) {
                    thetaRight = (get(LMRS.D_COOP)-Math.abs(dRightRoute)) / (get(LMRS.D_COOP)-get(LMRS.D_SYNC));
                }
                set(LMRS.D_RIGHT, dRightRoute + thetaRight*(dRightSpeed+dRightBias));

                /* === GAP ACCEPTANCE ===
                 * A gap is accepted or rejected based on the resulting
                 * acceleration of the driver itself and the potential follower.
                 */
                // Determine own acceleration
                double aSelf = 0; // assume ok
                if (vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM) !=null && vehicle.getGap(vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM))>0) {
                    // Use car-following model
                    Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_LEFT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                    aSelf = IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM), T);
                } else if (vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM) !=null) {
                    // Negative headway, reject gap
                    aSelf = Double.NEGATIVE_INFINITY;
                }
                // Determine follower anticipatedAcceleration
                double aFollow = 0; // assume ok
                if (vehicle.getVehicle(Enclosure.LEFT_UPSTREAM) != null) {
                    if (vehicle.getVehicle(Enclosure.LEFT_UPSTREAM).getGap(vehicle)>0) {
                        jVehicle anticipatedFollowingVehicle = vehicle.getVehicle(Enclosure.LEFT_UPSTREAM);
                        jDriver anticipatedFollowingDriver = anticipatedFollowingVehicle.getDriver();
                        Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_LEFT), anticipatedFollowingDriver.get(IDMPlus.T),anticipatedFollowingDriver.get(LMRS.T_MIN), anticipatedFollowingDriver.get(IDMPlus.T_MAX));
                        aFollow = IDMPlus.anticipatedAcceleration(anticipatedFollowingVehicle, vehicle, T);
                    } else {
                        aFollow = Double.NEGATIVE_INFINITY;
                    }
                }
                /* The gap is accepted if both accelerations are larger than a
                 * desire depedant threshold.
                 */
                boolean acceptLeft = false;
                if (aSelf >= -get(LMRS.B_SAFE)*get(LMRS.D_LEFT) && aFollow >= -get(LMRS.B_SAFE)*get(LMRS.D_LEFT) && vehicle.getLane().isGoLeft()) {
                    acceptLeft = true;
                }
                // Idem. for right gap
                aSelf = 0;
                if (vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM) !=null && vehicle.getGap(vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM))>0) {
                    Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_RIGHT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                    aSelf = IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM), T);
                } else if (vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM) !=null) {
                    aSelf = Double.NEGATIVE_INFINITY;
                }
                aFollow = 0;
                if (vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM) != null) {
                    if (vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM).getGap(vehicle)>0) {
                        jVehicle anticipatedFollowingVehicle = vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM);
                        jDriver anticipataedFollowingDriver = anticipatedFollowingVehicle.getDriver();
                        Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_RIGHT), anticipataedFollowingDriver.get(IDMPlus.T), anticipataedFollowingDriver.get(LMRS.T_MIN), anticipataedFollowingDriver.get(IDMPlus.T_MAX));
                        aFollow = IDMPlus.anticipatedAcceleration(anticipatedFollowingVehicle, vehicle, T);
                    } else {
                        aFollow = Double.NEGATIVE_INFINITY;
                    }
                }
                boolean acceptRight = false;
                if (aSelf >= -get(LMRS.B_SAFE)*get(LMRS.D_RIGHT) && aFollow >= -get(LMRS.B_SAFE)*get(LMRS.D_RIGHT) && vehicle.getLane().isGoRight()) {
                    acceptRight = true;
                }

                /* LANE CHANGE DECISION
                 * A lane change is initiated towards the largest desire if that
                 * gap is accepted. If the gap is rejected, the turn indicator
                 * may be turned on.
                 */
                if (get(LMRS.D_LEFT)>=get(LMRS.D_RIGHT) && get(LMRS.D_LEFT)>=get(LMRS.D_FREE) && acceptLeft) {
                    // Set dy to the left
                    double dur = Math.min((route.xLaneChanges(vehicle.getLane())- vehicle.getX())/(180/3.6), get(LMRS.DURATION));
                    vehicle.changeLane(LatDirection.LEFT, model.getStepSize() / dur);
                    // Set headway
                    setT(get(LMRS.D_LEFT));
                    // Set response headway of new follower
                    if (vehicle.getVehicle(Enclosure.LEFT_UPSTREAM) != null && vehicle.getVehicle(Enclosure.LEFT_UPSTREAM).getVehicle(Enclosure.RIGHT_DOWNSTREAM)==vehicle) {
                        jDriver newFollower = vehicle.getVehicle(Enclosure.LEFT_UPSTREAM).getDriver();
                        double updatedT = updatedTimeHeadwayForLaneChange(get(LMRS.D_LEFT), newFollower.get(IDMPlus.T), newFollower.get(LMRS.T_MIN), newFollower.get(IDMPlus.T_MAX));
                        newFollower.set(IDMPlus.T, updatedT);
                    }
                } else if (get(LMRS.D_RIGHT)>=get(LMRS.D_LEFT) && get(LMRS.D_RIGHT)>=get(LMRS.D_FREE) && acceptRight) {
                    // Set dy to the right
                    double dur = Math.min((route.xLaneChanges(vehicle.getLane())- vehicle.getX())/(180/3.6), get(LMRS.DURATION));
                    vehicle.changeLane(LatDirection.RIGHT, model.getStepSize() / dur);
                    // Set headway
                    setT(get(LMRS.D_RIGHT));
                    if (vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM) != null && vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM).getVehicle(Enclosure.LEFT_DOWNSTREAM)==vehicle) {
                        jDriver newFollower = vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM).getDriver();
                        newFollower.set(IDMPlus.T, updatedTimeHeadwayForLaneChange(get(LMRS.D_RIGHT), newFollower.get(IDMPlus.T), newFollower.get(LMRS.T_MIN), newFollower.get(IDMPlus.T_MAX)));
                    }
                } else if (get(LMRS.D_LEFT)>=get(LMRS.D_RIGHT) && get(LMRS.D_LEFT)>=get(LMRS.D_COOP)) {
                    // Indicate need to left
                    vehicle.toggleLeftIndicator();
                } else if (get(LMRS.D_RIGHT)>=get(LMRS.D_LEFT) && get(LMRS.D_RIGHT)>=get(LMRS.D_COOP)) {
                    // Indicate need to right
                    vehicle.toggleRightIndicator();
                }
            } // not on first 100m

            /* === LONGITUDINAL ===
             * Follow all applicable vehicles and use lowest acceleration.
             */
            // Follow leader (regular car following)
            lowerAcceleration(IDMPlus.acceleration(vehicle, vehicle.getVehicle(Enclosure.DOWNSTREAM)));
            // Synchronize to perform a lane change
            if (get(LMRS.D_LEFT)>=get(LMRS.D_SYNC) && get(LMRS.D_LEFT)>=get(LMRS.D_RIGHT) && vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM) !=null && vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getSpeed()-5/3.6<vehicle.getSpeed()) {
                // Apply shorter headway for synchronization
                Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_LEFT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                lowerAcceleration(safe(IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM),T)));
                set(LMRS.LEFT_SYNC, true);
            } else if (get(LMRS.D_RIGHT)>=get(LMRS.D_SYNC) && get(LMRS.D_RIGHT)>get(LMRS.D_LEFT) && vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM) !=null && vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getSpeed()-5/3.6<vehicle.getSpeed()) {
                // Apply shorter headway for synchronization
                setT(get(LMRS.D_RIGHT));
                Double T = updatedTimeHeadwayForLaneChange(get(LMRS.D_RIGHT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                lowerAcceleration(safe(IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM), T)));
                set(LMRS.RIGHT_SYNC, true);
            }
            // Synchronize to create a gap
            if (vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM) !=null && vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getVehicle(Enclosure.RIGHT_UPSTREAM)==vehicle &&
                    vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).isIndicatingRight()) {
                // Apply shorter headway for gap-creation
                Double T = updatedTimeHeadwayForLaneChange(vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getDriver().get(LMRS.D_RIGHT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                lowerAcceleration(safe(IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM), T)));
                set(LMRS.RIGHT_YIELD, true);
            }
            if (vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM) !=null && vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getVehicle(Enclosure.LEFT_UPSTREAM)==vehicle &&
                    vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).isIndicatingLeft()) {
                // Apply shorter headway for gap-creation
                Double T = updatedTimeHeadwayForLaneChange(vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getDriver().get(LMRS.D_LEFT), get(IDMPlus.T), get(LMRS.T_MIN), get(IDMPlus.T_MAX));
                lowerAcceleration(safe(IDMPlus.anticipatedAcceleration(vehicle, vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM), T)));
                set(LMRS.LEFT_YIELD, true);
            }

        } else {

            // Performing a lane change, simply follow both leaders
            lowerAcceleration(IDMPlus.acceleration(vehicle, vehicle.getVehicle(Enclosure.DOWNSTREAM)));
//            lowerAcceleration(IDMPlus.acceleration(vehicle, vehicle.lcVehicle.getVehicle(Enclosure.DOWNSTREAM)));
        }
    }

    /**
     * Sets the acceleration of the vehicle only if the given value is lower
     * than the current value or in case of a new time step.
     * @param a Vehicle acceleration [m/s^2].
     */
    public void lowerAcceleration(double a) {
        jModel model = jModel.getInstance();
        if (a<vehicle.getAcceleration() || tAccLower< model.getT()) {
            vehicle.setAcceleration(a);
            tAccLower = model.getT();
        }
    }

    /**
     * Returns a limited value of acceleration which remains comfortable and
     * safe according to a >= -bSafe. This limit can be used for synchronization
     * with an adjacent lane and not for car-following as 'safe' does not mean
     * collision free, but safe with regard to upsteram followers.
     * @param a Acceleration as calculated for an adjacent leader [m/s^2].
     * @return Limited safe acceleration [m/s^2].
     */
    public double safe(double a) {
        return a >= -get(LMRS.B_SAFE) ? a : -get(LMRS.B_SAFE);
    }

    /**
     * Sets T depending on the level of lane change desire. This method will
     * never increase the current T value. Lane change desire may be of another
     * driver. If the T value is only set for an evaluation that does not result
     * in an actual action, the value for T should be reset using
     * <code>resetT()</code>. If one calls this method consequentially without
     * calling the reset method in between, the regular car following headway
     * becomes lost.
     * @param d Desire for lane change.
     */
    public void setT(double d) {
        if (d>0 && d<1) {
            double Tint = d*get(LMRS.T_MIN) + (1-d)*get(IDMPlus.T_MAX);
            set(IDMPlus.T, Math.min(get(IDMPlus.T), Tint));
        } else if (d<=0) {
            set(IDMPlus.T, Math.min(get(IDMPlus.T), get(IDMPlus.T_MAX)));
        } else {
            set(IDMPlus.T, Math.min(get(IDMPlus.T), get(LMRS.T_MIN)));
        }
    }

    public static double updatedTimeHeadwayForLaneChange(double desire, double t, double tMin, double tMax) {
        if (desire>0 && desire<1) {
            double Tint = desire * tMin + (1-desire) * tMax;
            return Math.min(t, Tint);
        } else if (desire <= 0) {
            return Math.min(t, tMax);
        } else {
            return Math.min(t, tMin);
        }
    }

    /**
     * Returns an anticipated speed based on the speed limit, maximum vehicle
     * speed and the speed of leaders. Vehicles on adjacent lanes with
     * indicators turned on towards the lane are also considered. Internal
     * bookkeeping prevents multiple loops over leading vehicles on any
     * particular lane by storing the anticipation speed for adjacent lanes.
     *
     * @param lane LaneType at which an aticipated speed is required.
     * @return Anticipated speed on lanes [left, current, right].
     */
    protected double anticipatedSpeed(jLane lane) {
        jModel model = jModel.getInstance();

        if (lane==null) {
            return 0;
        }
        // Be sure we exclude the vehicle itself
        double x = vehicle.getX() +0.001;
        // In case of an adjacent lane, get the adjacent x
        if (lane!= vehicle.getLane()) {
            if (lane.getRight() !=null && lane.getRight() == vehicle.getLane()) {
                x = vehicle.getAdjacentX(LatDirection.LEFT); // lane = left lane of vehicle
            } else {
                x = vehicle.getAdjacentX(LatDirection.RIGHT); // lane = right lane of vehicle
            }
        }
        // Clear bookkeeping in case of new time step
        if (tAnt< model.getT()) {
            antFromLeft.clear();
            antInLane.clear();
            antFromRight.clear();
            tAnt = model.getT();
        }
        // Initialize as infinite
        double vLeft = Double.POSITIVE_INFINITY;
        double vCur = Double.POSITIVE_INFINITY;
        double vRight = Double.POSITIVE_INFINITY;
        // Calculate anticipation speed in current lane
        if (!antInLane.containsKey(lane)) {
            anticipatedSpeedFromLane(lane, x);
        }
        vCur = antInLane.get(lane); // all in lane
        // Calculate anticipation speed in left lane
        if (lane.getLeft() !=null) {
            if (!antFromLeft.containsKey(lane)) {
                double xleft = lane.getAdjacentX(x, LatDirection.LEFT);
                anticipatedSpeedFromLane(lane.getLeft(), xleft);
            }
            vLeft = antFromLeft.get(lane); // indicators only
        }
        // Calculate anticipation speed in right lane
        if (lane.getRight() !=null) {
            if (!antFromRight.containsKey(lane)) {
                double xright = lane.getAdjacentX(x, LatDirection.RIGHT);
                anticipatedSpeedFromLane(lane.getRight(), xright);
            }
            vRight = antFromRight.get(lane); // indicators only
        }
        // Return minimum of all
        return Math.min(vCur, Math.min(vLeft, vRight));
    }

    /**
     * Supporting method that loops all leaders in a lane and calculates their
     * influence on the given and adjacent lanes, where only vehicles with
     * indicators turned on are considered for the adjacent lanes. This method
     * stores the information for up to three lanes, preventing multiple loops
     * over the leaders in the requested lane. These speeds can be retreived
     * from the hashmaps <code>antFromLeft</code>, <code>antInLane</code> and
     * <code>antFromRight</code> by using their <code>.get(jLane)</code> method
     * where the lane is the lane for which the anticipation speed is required.
     * @param lane Requested lane of influence on this or an adjacent lane.
     * @param x Location on the lane.
     */
    protected void anticipatedSpeedFromLane(jLane lane, double x) {
        // Initialize as desired velocity (= no influence).
        Double vLeft = IDMPlus.updateDesiredVelocity(this);
        Double vCur = IDMPlus.updateDesiredVelocity(this);
        Double vRight = IDMPlus.updateDesiredVelocity(this);
        // Find first leader
        jVehicle down = lane.findVehicle(x, LongDirection.DOWN);
        double s = 0;
        double v = 0;
        // Loop leaders while within anticipation region
        while (down!=null && s<=get(LMRS.X0)) {
            // interpolate from "v(s=x0) = vDes" to "v(s=0) = down.getSpeed()" e.g.
            // with headway = 0 take vehicle fully into account, and with
            // headway > x0 ignore vehicle, in between interpolate linearly
            s = down.getX()+lane.xAdj(down.getLane()) - down.getLength() - x;
            // only consider if new headway is within consideration range and
            // speed is below the desired speed, otherwise there is no influence
            if (s<=get(LMRS.X0) && down.getSpeed()< IDMPlus.updateDesiredVelocity(this)) {
                // influence of a single vehicle
                v = anticipateSingle(s, down.getSpeed());
                // take minimum
                vCur = Math.min(vCur, v);
                /* Indicators from the current lane are not included for the
                 * anticipated speeds of the adjacent lanes. This is to prevent
                 * a slow queue adjacent to an empty lane, where the anticipated
                 * speeds are then consequently low.
                 */
                if (down.isIndicatingLeft() && lane!= vehicle.getLane()) {
                    vLeft = Math.min(vLeft, v);
                } else if (down.isIndicatingRight() && lane!= vehicle.getLane()) {
                    vRight = Math.min(vRight, v);
                }
                /* MODEL ERROR!
                 * lane!=vehicle.lane does not cover vehicles that are in any
                 * downstream lane. These should also be ignored! For now
                 * (17-08-2011) this will remain as paper with this model has
                 * been submitted. The if statements should include:
                 *  && vehicle.lane.xAdj(lane)==0
                 */
            }
            // go to next vehicle
            down = down.getVehicle(Enclosure.DOWNSTREAM);
        }
        // store anticipated speeds
        if (lane.getRight() !=null) {
            antFromLeft.put(lane.getRight(), vRight); // this lane is the left lane of lane.right
        }
        antInLane.put(lane, vCur);
        if (lane.getLeft() !=null) {
            antFromRight.put(lane.getLeft(), vLeft); // this lane is the right lane of lane.left
        }
    }

    /**
     * Supporting method to return the influence of a single vehicle on the
     * anticipated speed.
     * @param s Headway [m]
     * @param v Speed [m/s]
     * @return Anticipation speed of a single vehicle [m/s]
     */
    protected double anticipateSingle(double s, double v) {
        return (1-(s/get(LMRS.X0)))*v + (s/get(LMRS.X0))* IDMPlus.updateDesiredVelocity(this);
    }

    /**
     * Sets deceleration for a traffic light, if needed.
     * @param trafficLight Traffic light that was noticed.
     */
    public void notice(jTrafficLight trafficLight) {
        double v = vehicle.getSpeed();
        double dv = v; // stand still object
        double v0 = vehicle.getLane().getVLimInMetersPerSecond();
        double s = vehicle.getDistanceToRSU(trafficLight);
        if (trafficLight.isRed()) {
            double acc = IDMPlus.acceleration(this, v, dv, v0, s);
            if (acc>-get(LMRS.B_RED)) {
                lowerAcceleration(acc);
            }
        } else if (trafficLight.isYellow()) {
            double acc = IDMPlus.acceleration(this, v, dv, v0, s);
            if (acc>-get(IDMPlus.B)) { // yellow acceptance by -b [m/s2]
                lowerAcceleration(acc);
            }
        }
    }

    /**
     * Whether the driver is on a taper lane <i>that is applicable</i> to this
     * driver.
     * @return Whether the driver is on a taper lane.
     */
    public boolean isOnTaper() {
        return isTaper(vehicle.getLane());
    }

    /**
     * Same as isOnTaper(), but if the vehicle would be on the given lane.
     * @param lane LaneType for which it needs to be known whether it is an applicable taper.
     * @return Whether lane is an applicable taper to this driver.
     */
    public boolean isTaper(jLane lane) {
        return (lane.getTaper() !=null && route.canBeFollowedFrom(lane.getTaper()));
    }

    @Override
    public double getSafeSpeed(double distance, double stoppingDistance, double safeTimeHeadway) {
        double Tmin = get(IDMPlus.T);
        double maxDeceleration = getVehicle().getMaxDeceleration();
        return (distance - stoppingDistance - 0.5 * maxDeceleration * Math.pow(Tmin,2))/Tmin;
    }
}