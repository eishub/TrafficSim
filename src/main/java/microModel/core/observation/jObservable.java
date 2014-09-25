package microModel.core.observation;

/**
 * <p>
 * Observer Pattern:
 * Entities that can change state and need to be observed have to implement this interface.
 * </p>
 * <p>
 * For example any object that moves ({@link microModel.core.vehicle.jMovable}) are observable by
 * detectors on the road.
 * </p>
 */
public interface jObservable {
    /**
     * Subscribes an observer to be notified of state changes.
     * @param observer The observer
     */
    public void addObserver(jObserver observer);

    /**
     * Unsubscribes an observer from state change notifications.
     * @param observer
     */
    public void detachObserver(jObserver observer);

    /**
     * Removes all observers subscribed to be notified of state changes.
     */
    public void detachAllObservers();
}
