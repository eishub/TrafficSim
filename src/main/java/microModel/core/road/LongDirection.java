package microModel.core.road;

/** Enumeration of longitudinal directions. */
public enum LongDirection implements Direction {
    /** Upstream Direction.*/
    UP {
        @Override
        public Direction reverse() {
            return DOWN;
        }
    },
    /** Downstream direction.*/
    DOWN {
        @Override
        public Direction reverse() {
            return UP;
        }
    };

    @Override
    public abstract Direction reverse();

}
