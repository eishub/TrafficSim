package microModel.core.vehicle.device;

import microModel.core.device.AbstractController;
import microModel.core.vehicle.Vehicle;

/**
 * Abstract shell for an on-board unit (OBUs).
 */
public abstract class AbstractOBU extends AbstractController {

    /** Vehicle in which this OBU is. */
    public Vehicle vehicle;
    
    /**
     * Constructor using the control every time step. The OBU is linked to the 
     * vehicle and vice versa.
     * @param vehicle Vehicle.
     */
    public AbstractOBU(Vehicle vehicle) {
        this(vehicle, 0, 0);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt>. The OBU is linked to
     * the vehicle and vice versa.
     * @param vehicle Vehicle.
     * @param period Time [s] between control runs.
     */
    public AbstractOBU(Vehicle vehicle, double period) {
        this(vehicle, period, 0);
    }
    
    /**
     * Constructor using the control every <tt>duration</tt> but no sooner than
     * <tt>start</tt>. The OBU is linked to the vehicle and vice versa.
     * @param vehicle Vehicle.
     * @param period Time [s] between control runs.
     * @param start Time [s] of first control run.
     * 
     */
    // using this at the end is ok, OBU is fully initialized
    @SuppressWarnings("LeakingThisInConstructor")
    public AbstractOBU(Vehicle vehicle, double period, double start) {
        super(period, start);
        this.vehicle = vehicle;
        vehicle.OBU = this;
    }
    
    /** Performs the initialization.  */
    public abstract void init();

    /** The delete command should delete any pointers to this object. */
    public abstract void delete();
}