package microModel.map.road;

import microModel.core.road.LaneTransition;
import microModel.core.road.LaneType;

/**
 * Created with IntelliJ IDEA.
 * User: arman
 * Date: 9/3/12
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class LaneInfo {

    private final LaneType laneType;
    private final LaneTransition left;
    private final LaneTransition right;

    public LaneInfo(LaneTransition left, LaneType laneType, LaneTransition right) {
        this.laneType = laneType;
        this.left = left;
        this.right = right;
    }

    public LaneType getLaneType() {
        return laneType;
    }

    public LaneTransition getLeftTransition() {
        return left;
    }

    public LaneTransition getRightTransition() {
        return right;
    }
}
