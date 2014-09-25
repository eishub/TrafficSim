package microModel.core.road;

import microModel.settings.BuiltInSettings;

/**
* Created with IntelliJ IDEA.
* User: arman
* Date: 8/29/12
* Time: 2:08 PM
* To change this template use File | Settings | File Templates.
*/
public enum LaneType {
    NORMAL(BuiltInSettings.NORMAL_LANE.value()) {
        @Override
        public boolean isTaper() {
            return false;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return true;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return true;
        }
    },
    ADDED(BuiltInSettings.ADDED_LANE.value()) {
        @Override
        public boolean isTaper() {
            return false;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return false;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return true;
        }

    },
    SUBTRACTED(BuiltInSettings.SUBTRACTED_LANE.value()) {
        @Override
        public boolean isTaper() {
            return false;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return true;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return false;
        }
    },
    EXTRA(BuiltInSettings.EXTRA_LANE.value()) {
        @Override
        public boolean isTaper() {
            return false;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return false;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return false;
        }
    },
    MERGE(BuiltInSettings.MERGE_LANE.value()) {
        @Override
        public boolean isTaper() {
            return true;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return true;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return false;
        }
    },
    DIVERGE(BuiltInSettings.DIVERGE_LANE.value()) {
        @Override
        public boolean isTaper() {
            return true;
        }

        @Override
        public boolean connectsToUpStreamLane() {
            return false;
        }

        @Override
        public boolean connectsToDownStreamLane() {
            return true;
        }
    };

    private String type;

    LaneType(String type) {
        this.type = type;
    }

    private boolean equals(String type) {
        return this.type.compareTo(type) == 0;
    }

    public abstract boolean isTaper();
    public abstract boolean connectsToUpStreamLane();
    public abstract boolean connectsToDownStreamLane();

    public static LaneType forType(String type) {
        for (LaneType l : LaneType.values()) {
            if (l.equals(type))
                return l;
        }
        return null;
    }
}
