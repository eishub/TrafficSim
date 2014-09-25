package apl;

import eis.eis2java.annotation.AsAction;
import eis.eis2java.annotation.AsPercept;
import eis.eis2java.translation.Filter;
import microModel.core.Parameter;
import microModel.core.driver.AbstractDriver;
import microModel.core.driver.jDriver;
import microModel.core.driver.model.IDM;
import microModel.core.jRoute;
import microModel.core.road.LaneType;
import microModel.core.road.LatDirection;
import microModel.core.vehicle.Enclosure;
import microModel.core.vehicle.jVehicle;
import microModel.jModel;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;

import static microModel.core.road.LatDirection.*;

public class AgentDriver extends AbstractDriver {
    public static final String TYPE = "goaldriver";
    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(AgentDriver.class);
    private double acceleration;

    private AgentDriver() {
    }

    @AsPercept(name = Percepts.TIME, filter = Filter.Type.ALWAYS)
    public Double getTime() {
        return jModel.getInstance().getT();
    }

    @AsPercept(name = Percepts.TIME_STEP, filter = Filter.Type.ONCE)
    public Double getTimeStep() {
        return jModel.getInstance().getStepSize();
    }

    @AsPercept(name = Percepts.SPEED, filter = Filter.Type.ON_CHANGE)
    public Double getSpeed() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(getVehicle().getSpeed()));
    }

    @AsPercept(name = Percepts.ACCELERATION, filter = Filter.Type.ON_CHANGE)
    public Double getAccleration() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(getVehicle().getAcceleration()));
    }

    @AsPercept(name = Percepts.LANE, filter = Filter.Type.ON_CHANGE)
    public String getCurrentLane() {
        return String.valueOf(getVehicle().getLane().getId());
    }

    @AsPercept(name = Percepts.LANE_SPEED_LIMIT, filter = Filter.Type.ON_CHANGE)
    public Double getSpeedLimit() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(getVehicle().getLane().getVLimInMetersPerSecond()));
    }

    @AsPercept(name = Percepts.LEFT_LANE_CHANGE_ALLOWED, filter = Filter.Type.ON_CHANGE)
    public Boolean getLeftLaneChangeAllowed() {
        return getVehicle().getLane().isGoLeft();
    }

    @AsPercept(name = Percepts.RIGHT_LANE_CHANGE_ALLOWED, filter = Filter.Type.ON_CHANGE)
    public Boolean getRightLaneChangeAllowed() {
        return getVehicle().getLane().isGoRight();
    }

    @AsPercept(name = Percepts.LEFT_LANE, filter = Filter.Type.ON_CHANGE)
    public String getLeftLane() {
        return getVehicle().getLane().getLeft() == null ? null : String.valueOf(getVehicle().getLane().getLeft().getId());
    }

    @AsPercept(name = Percepts.RIGHT_LANE, filter = Filter.Type.ON_CHANGE)
    public String getRightLane() {
        return getVehicle().getLane().getRight() == null ? null : String.valueOf(getVehicle().getLane().getRight().getId());
    }

    @AsPercept(name = Percepts.DOWNSTREAM_LANE, filter = Filter.Type.ON_CHANGE)
    public String getDownstreamLane() {
        return getVehicle().getLane().getDown() == null ? null : String.valueOf(getVehicle().getLane().getDown().getId());
    }

    @AsPercept(name = Percepts.LANE_CHANGE_IN_PROGRESS, filter = Filter.Type.ON_CHANGE)
    public Boolean isLaneChangeInProgress() {
        return getVehicle().isChangingLane();
    }

    @AsPercept(name = Percepts.GAP, filter = Filter.Type.ON_CHANGE)
    public Double getGap() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        jVehicle downStreamVehicle = null;
        jVehicle downStreamVehicleLeft = null;
        jVehicle downStreamVehicleRight = null;
        double gap1, gap2, gap3, distanceToEndofLane;
        if ( !getVehicle().isChangingLane() ) {
            // Here the driver is not in the process of changing lane
            downStreamVehicle = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
            downStreamVehicleLeft = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
            downStreamVehicleRight = getVehicle().getVehicle(Enclosure.RIGHT_DOWNSTREAM);
        }
        else {
            // Here the driver is changing lane
            LatDirection lcDirection = getVehicle().getLaneChangeDirection();
            switch (lcDirection) {
                case LEFT:
                    downStreamVehicle = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
                    downStreamVehicleLeft = getVehicle().getLane().getLeft().search(Enclosure.LEFT_DOWNSTREAM, getVehicle().getAdjacentX(LEFT));
                    downStreamVehicleRight = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
                    break;
                case RIGHT:
                    downStreamVehicle = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
                    downStreamVehicleLeft = getVehicle().getLane().getRight().search(Enclosure.LEFT_DOWNSTREAM, getVehicle().getAdjacentX(LEFT));
                    downStreamVehicleRight = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
                    break;
            }
        }
        gap1 = getVehicle().getGap(downStreamVehicle);
        gap2 = getVehicle().getGap(downStreamVehicleLeft);
        gap3 = getVehicle().getGap(downStreamVehicleRight);
        double gap = gap1;
        if ( downStreamVehicleLeft != null && downStreamVehicleLeft.isIndicatingRight() && downStreamVehicleLeft.getLaneChangeProgress() >= 0.5) {
            gap = Math.min(gap, gap2);
        }
        if ( downStreamVehicleRight != null && downStreamVehicleRight.isIndicatingLeft() && downStreamVehicleRight.getLaneChangeProgress() >= 0.5) {
            gap = Math.min(gap,gap3);
        }

        boolean laneEnds = getVehicle().getLane().getType() == LaneType.MERGE || getVehicle().getLane().getType() == LaneType.SUBTRACTED;
        distanceToEndofLane = getVehicle().getDistanceToLaneEnd();
        if (laneEnds) {
            gap = Math.min(gap, distanceToEndofLane);
        }

        return Double.valueOf(twoDecimals.format(gap > 0 ? gap : 0.01));
    }

    @AsPercept(name = Percepts.TIME_GAP, filter = Filter.Type.ON_CHANGE)
    public Double getTimeGap() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        jVehicle downStreamVehicle = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
        if (downStreamVehicle != null) {
            return Double.valueOf(twoDecimals.format(getVehicle().getTimeGap(downStreamVehicle)));
        }

        //Return large number to indicate a lot of freespace
        return Double.valueOf(twoDecimals.format(1000));
    }

    @AsPercept(name = Percepts.MAX_ACCELERATION, filter = Filter.Type.ON_CHANGE)
    public Double getMaxAcceleration() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(getVehicle().getMaxAcceleration()));
    }

    @AsPercept(name = Percepts.MAX_DECELERATION, filter = Filter.Type.ON_CHANGE)
    public Double getMaxDeceleration() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(Math.abs(getVehicle().getMaxDeceleration())));
    }

    @AsPercept(name = Percepts.SPEED_DELTA, filter = Filter.Type.ON_CHANGE)
    public Double getSpeedDelta() {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        jVehicle downStreamVehicle = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
        if (downStreamVehicle != null) {
            return Double.valueOf(twoDecimals.format(getVehicle().getSpeed() - downStreamVehicle.getSpeed()));
        }
        return 0.00;
    }

    @AsPercept(name = Percepts.BLOCK, filter = Filter.Type.ON_CHANGE)
    public Integer getBlocking() {
        if (! getVehicle().isChangingLane()) {
            boolean blockedByVehicle = getVehicle().getVehicle(Enclosure.DOWNSTREAM) != null ? true : false;
            boolean laneEnds = getVehicle().getLane().getType() == LaneType.MERGE || getVehicle().getLane().getType() == LaneType.SUBTRACTED;
            return blockedByVehicle || laneEnds ? 1 : 0;
        }
        else {
            LatDirection lcDirection = getVehicle().getLaneChangeDirection();
            int blocked = 0;
            switch (lcDirection) {
                case LEFT:
                    blocked =getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM) != null ? 1: 0 ;
                    break;
                case RIGHT:
                    blocked = getVehicle().getVehicle(Enclosure.RIGHT_DOWNSTREAM) != null ? 1: 0 ;
                    break;
            }
            return blocked;
        }
    }

    @AsPercept(name = Percepts.ON_ROUTE, filter = Filter.Type.ALWAYS)
    public Boolean isOnRoute() {
        return getRoute().canBeFollowedFrom(getVehicle().getLane());
    }

    @AsPercept(name = Percepts.LANE_CHANGE_REQUIRED, filter = Filter.Type.ALWAYS)
    /** Returns the number of (left?) lane changes required to stay on route. If minus I guess the number of (right?) lcs.*/
    public Integer requireLaneChange(){
        return getRoute().nLaneChanges(getVehicle().getLane());
    }

    @AsPercept(name = Percepts.LEFT_LANE_LEAD_GAP, filter = Filter.Type.ALWAYS)
    public double leftLaneLeadGap() {
        jVehicle leadVehicle = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
        return getVehicle().getGap(leadVehicle);
    }

    @AsPercept(name = Percepts.LEFT_LANE_FOLLOW_GAP, filter = Filter.Type.ALWAYS)
    public double leftLaneFollowGap() {
        jVehicle followVehicle = getVehicle().getVehicle(Enclosure.LEFT_UPSTREAM);
        return getVehicle().getGap(followVehicle);
    }

    @AsPercept(name = Percepts.RIGHT_LANE_LEAD_GAP, filter = Filter.Type.ALWAYS)
    public double rightLaneLeadGap() {
        jVehicle leadVehicle = getVehicle().getVehicle(Enclosure.RIGHT_DOWNSTREAM);
        return getVehicle().getGap(leadVehicle);
    }

    @AsPercept(name = Percepts.RIGHT_LANE_FOLLOW_GAP, filter = Filter.Type.ALWAYS)
    public double rightLaneFollowGap() {
        jVehicle followVehicle = getVehicle().getVehicle(Enclosure.RIGHT_UPSTREAM);
        return getVehicle().getGap(followVehicle);
    }

    @AsPercept(name = Percepts.LEFT_LC_IMPOSED_ACC, filter = Filter.Type.ALWAYS)
    public double leftLaneChangeImposedAcceleration() {
        jVehicle followVehicle = getVehicle().getVehicle(Enclosure.LEFT_UPSTREAM);
        // The assumption here is that the following vehicle is using the IDM model
        if (followVehicle != null) {
            double gap = followVehicle.getGap(getVehicle());
            double speedDifference = followVehicle.getSpeed() - getVehicle().getSpeed();
            double imposedAcceleration = IDM.acceleration1(1.0, getVehicle().getSpeed(), followVehicle.getSpeed(), 5, 1.0, speedDifference, gap, followVehicle.getMaxAcceleration(), Math.abs(followVehicle.getMaxDeceleration()));
            return imposedAcceleration;
        }
        return 0;
    }

    @AsPercept(name = Percepts.LEFT_GAP_ACCEPTABLE, filter = Filter.Type.ALWAYS)
    public Boolean leftGapAcceptable_MOBIL() {
        //TODO: also consider deceleration required to stop
        double selfImposedAcceleration = getVehicle().getMaxDeceleration();
        jVehicle newLeader = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
        if (newLeader != null) {
            double gap = getVehicle().getGap(newLeader);
            double speedDifference = newLeader.getSpeed() - getVehicle().getSpeed();
            selfImposedAcceleration = IDM.acceleration1(1.0, newLeader.getSpeed(), getVehicle().getSpeed(), 5, 1.0, speedDifference, gap, newLeader.getMaxAcceleration(), Math.abs(newLeader.getMaxDeceleration()));
        }
        boolean safeForOther = leftLaneChangeImposedAcceleration() > -5.0;
        boolean safeForSelf = selfImposedAcceleration >= getVehicle().getMaxDeceleration();
        return safeForOther && safeForSelf;
    }

    @AsPercept(name = Percepts.LEFT_LC_BENEFICIAL, filter = Filter.Type.ALWAYS)
    public Boolean leftLaneChangeBeneficial_MOBIL() {

        double incentive = 0;
        double politeness = 0.7;

        jVehicle newFollower = getVehicle().getVehicle(Enclosure.LEFT_UPSTREAM);
        if (newFollower != null) {
            double gap = newFollower.getGap(getVehicle());
            double speedDifference = newFollower.getSpeed() - getVehicle().getSpeed();
            double imposedAcceleration = IDM.acceleration(3.0, newFollower.getLane().getVLimInMetersPerSecond(), newFollower.getSpeed(), 5, 2, speedDifference, gap, newFollower.getMaxAcceleration(), Math.abs(newFollower.getMaxDeceleration()));
            incentive += politeness * (imposedAcceleration - newFollower.getAcceleration());
        }
        jVehicle oldFollower = getVehicle().getVehicle(Enclosure.UPSTREAM);
        if (oldFollower != null) {
            jVehicle newDownstreamForOldFollower = getVehicle().getVehicle(Enclosure.DOWNSTREAM);
            double gap = oldFollower.getGap(newDownstreamForOldFollower);
            double speedDifference = 0;
            double imposedAcceleration;
            if (newDownstreamForOldFollower == null) {
                imposedAcceleration = IDM.acceleration(3.0, oldFollower.getLane().getVLimInMetersPerSecond(), oldFollower.getSpeed(), 5, 2, speedDifference, gap, oldFollower.getMaxAcceleration(), Math.abs(oldFollower.getMaxDeceleration()));
            }
            else {
                speedDifference = oldFollower.getSpeed() - newDownstreamForOldFollower.getSpeed();
                imposedAcceleration = IDM.acceleration(3.0, oldFollower.getLane().getVLimInMetersPerSecond(), oldFollower.getSpeed(), 5, 2, speedDifference, gap, oldFollower.getMaxAcceleration(), Math.abs(oldFollower.getMaxDeceleration()));
            }
            incentive += politeness * (imposedAcceleration - oldFollower.getAcceleration() );
        }
        jVehicle newLeader = getVehicle().getVehicle(Enclosure.LEFT_DOWNSTREAM);
        if (newLeader != null) {
            double gap = newLeader.getGap(getVehicle());
            double speedDifference = newLeader.getSpeed() - getVehicle().getSpeed();
            double imposedAcceleration = IDM.acceleration(3.0, newLeader.getLane().getVLimInMetersPerSecond(), newLeader.getSpeed(), 5, 2, speedDifference, gap, newLeader.getMaxAcceleration(), Math.abs(newLeader.getMaxDeceleration()));
            incentive += imposedAcceleration - newLeader.getAcceleration();
        }
        return incentive >= 0.1;
    }

    @AsAction(name = Actions.ACCELERATE)
    public void accelerate(double value) {
        if (value < 0) {
            decelerate(Math.abs(value));
            return;
        }
        acceleration = Math.min(Math.abs(value), getVehicle().getMaxAcceleration());
        jModel.getInstance().performedAction(this);
    }

    @AsAction(name = Actions.DECELERATE)
    public void decelerate(double value) {
        if (value < 0) {
            accelerate(Math.abs(value));
        }
        double deceleration = -Math.abs(value);
        acceleration = Math.max(deceleration, getVehicle().getMaxDeceleration());
        if (getSpeed() <= 0) {
            acceleration = 0;
        }
        jModel.getInstance().performedAction(this);
    }

    @AsAction(name = Actions.SKIP)
    public void doNothing() {
        jModel.getInstance().performedAction(this);
    }

    @AsAction(name = Actions.CHANGE_LANE)
    public void changeLane(String direction) {
        LatDirection dir = valueOf(direction.toUpperCase());
        //TODO: relate the lane change rate to the simulation step size.
        getVehicle().changeLane(dir,0.2);
        jModel.getInstance().performedAction(this);

    }

    @AsAction(name = Actions.ABORT_LANE_CHANGE)
    public void abortLaneChange() {
        getVehicle().abortLaneChange();
        jModel.getInstance().performedAction(this);
    }

    @Override
    public void drive() {
        getVehicle().setAcceleration(acceleration);
    }

    static final class Actions {
        final static String ACCELERATE = "accelerate";
        final static String DECELERATE = "decelerate";
        final static String SKIP = "skip";
        final static String CHANGE_LANE = "change_lane";
        final static String ABORT_LANE_CHANGE = "abort_lane_change";
    }

    static final class Percepts {
        /** Percepts from simulation */
        final static String TIME = "sim_time";
        final static String TIME_STEP = "sim_time_step";

        /** Car following percepts*/
        final static String SPEED = "speed";
        final static String GAP = "gap";
        final static String TIME_GAP = "time_gap";
        final static String ACCELERATION = "acceleration";
        final static String MAX_ACCELERATION = "max_acceleration";
        final static String MAX_DECELERATION = "max_deceleration";
        final static String SPEED_DELTA = "speed_delta";
        final static String BLOCK = "block";

        /** Percepts from lane */
        final static String LANE = "lane";
        final static String LANE_SPEED_LIMIT = "lane_speed_limit";
        final static String LEFT_LANE_CHANGE_ALLOWED = "left_lane_change_allowed";
        final static String RIGHT_LANE_CHANGE_ALLOWED = "right_lane_change_allowed";
        final static String LEFT_LANE = "left_lane";
        final static String RIGHT_LANE = "right_lane";
        final static String DOWNSTREAM_LANE = "downstream_lane";
        final static String LANE_CHANGE_IN_PROGRESS = "lane_change_in_progress";

        /** Route Information */
        final static String ON_ROUTE = "on_route";
        final static String LANE_CHANGE_REQUIRED = "require_lane_change";

        /** Lane Changing Information */
        final static String LEFT_LANE_LEAD_GAP = "left_lane_lead_gap";
        final static String LEFT_LANE_FOLLOW_GAP = "left_lane_follow_gap";
        //TODO: Remove the following 3 MOBIL specific functions and do such calculations in the GOAL agent.
        final static String LEFT_LC_IMPOSED_ACC = "left_lc_imposed_acc";
        final static String LEFT_GAP_ACCEPTABLE = "left_gap_acceptable";
        final static String LEFT_LC_BENEFICIAL = "left_lc_beneficial";

        final static String RIGHT_LANE_LEAD_GAP = "right_lane_lead_gap";
        final static String RIGHT_LANE_FOLLOW_GAP = "right_lane_follow_gap";

    }

    public static final class Builder extends BuildHelper implements jDriver.Builder {
        private jRoute route;

        public Builder(jRoute defaultRoute) {
            this.route = defaultRoute;
        }

        @Override
        public AgentDriver build() {
            AgentDriver driver = new AgentDriver();

            driver.setRoute(route);

            // Set any other modelParameters that might have been specified
            for (Object p : parameters.keySet()) {
                driver.set((Parameter<Object>) p, parameters.get(p));
            }

            return driver;
        }
    }
}
