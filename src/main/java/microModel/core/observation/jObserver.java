package microModel.core.observation;

/**
 * <p>
 * Observer Pattern:
 * Any entity that observers the state changes of other entities needs to implement
 * this interface to be notified of those state changes.
 * </p>
 * <p>
 * For example Road Side Units {@link microModel.core.road.device.AbstractRSU} implement this interface
 * because entities like detectors need to be informed when vehicles pass over them.
 * </p>
 * <p>
 * This implementation assumes a pull updates method to get the state changes of observables.
 * It means that it should be able to extract the state information that it needs from the observable.
 * </p>
 */
public interface jObserver {
    /**
     * Notifies the observer of a state change.
     * @param subject The subject in which the state change has occured.
     */
    public void see(jObservable subject);
}
