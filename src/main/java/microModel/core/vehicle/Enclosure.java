package microModel.core.vehicle;

/**
 * <p>
 * Enum indicating the surrounding areas around a vehicle.
 * </p>
 * <pre>
 *           (Moving Direction) ===>>>
 * ----------------------------------------------
 *  Left Up.     |   Left          | Left Down.
 * ----------------------------------------------
 *  Upstream     |   Current Loc.  | Downstream
 * ----------------------------------------------
 *  Right Up.    |   Right         | Right Down.
 * ----------------------------------------------
 * </pre>
 */
public enum Enclosure {
    CURRENT_LOCATION {
        @Override
        public Enclosure reverse() {
            return this;
        }
    },
    UPSTREAM {
        @Override
        public Enclosure reverse() {
            return DOWNSTREAM;
        }
    },
    DOWNSTREAM {
        @Override
        public Enclosure reverse() {
            return UPSTREAM;
        }
    },
    LEFT {
        @Override
        public Enclosure reverse() {
            return RIGHT;
        }
    },
    RIGHT {
        @Override
        public Enclosure reverse() {
            return LEFT;
        }
    },
    LEFT_UPSTREAM {
        @Override
        public Enclosure reverse() {
            return RIGHT_DOWNSTREAM;
        }
    },
    LEFT_DOWNSTREAM {
        @Override
        public Enclosure reverse() {
            return RIGHT_UPSTREAM;
        }
    },
    RIGHT_UPSTREAM {
        @Override
        public Enclosure reverse() {
            return LEFT_DOWNSTREAM;
        }
    },
    RIGHT_DOWNSTREAM {
        @Override
        public Enclosure reverse() {
            return LEFT_UPSTREAM;
        }
    };

    public abstract Enclosure reverse();
}
