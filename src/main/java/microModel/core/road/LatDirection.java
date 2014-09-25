package microModel.core.road;

/** Enumeration of lateral directions. */
public enum LatDirection implements Direction{
    /** Left direction */
    LEFT {
        @Override
        public Direction reverse() {
            return RIGHT;
        }
    },
    /** Right direction */
    RIGHT {
        @Override
        public Direction reverse() {
            return LEFT;
        }
    };

    @Override
    public abstract Direction reverse();
}
