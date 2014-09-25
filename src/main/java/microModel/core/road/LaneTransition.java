package microModel.core.road;

import microModel.settings.BuiltInSettings;

/**
* Created with IntelliJ IDEA.
* User: arman
* Date: 8/29/12
* Time: 2:09 PM
* To change this template use File | Settings | File Templates.
*/
public enum LaneTransition {

    ALLOWED(BuiltInSettings.LANE_TRANSITION_ALLOWED.value()) {
        @Override
        public boolean allowed() {
            return true;
        }
    },
    NOT_ALLOWED(BuiltInSettings.LANE_TRANSITION_NOT_ALLOWED.value()) {
        @Override
        public boolean allowed() {
            return false;
        }
    };

    private String type;

    LaneTransition(String type) {
        this.type = type;
    }

    private boolean equals(String type) {
        return this.type.compareTo(type) == 0;
    }

    public abstract boolean allowed();

    public static LaneTransition forType(String type) {
        for (LaneTransition lt: LaneTransition.values()) {
            if (lt.equals(type))
                return lt;
        }
        return null;
    }
}
