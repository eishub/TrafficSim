package microModel.core.driver;

import microModel.core.Parameter;
import microModel.core.driver.model.IDMPlus;
import microModel.core.jRoute;
import microModel.core.vehicle.Vehicle;
import microModel.core.vehicle.jVehicle;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDriver implements jDriver {

    /** ID of the driver. */
    protected int id;
    /** Route of the  driver. */
    protected jRoute route;
    /** Vehicle of the driver. */
    protected Vehicle vehicle;
    /** Place holder for driver model parameters */
    protected Map<Parameter<?>, Object> modelParameters = new HashMap<Parameter<?>, Object>();


    @Override
    public jVehicle getVehicle() {
        return vehicle;
    }

    /**
     * Setter which links the driver with a vehicle and vice versa.
     * @param vehicle Vehicle of the driver.
     */
    @Override
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        vehicle.driver = this;
    }

    @Override
    public double getSafeSpeed(double distance, double stoppingDistance, double safeTimeHeadway) {
        double maxDeceleration = getVehicle().getMaxDeceleration();
        return (distance - 0.5 * maxDeceleration * Math.pow(safeTimeHeadway,2))/safeTimeHeadway;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }

    @Override
    public <T> T get(Parameter<T> parameter) {
        if (modelParameters.containsKey(parameter)) {
            return (T) modelParameters.get(parameter);
        }
        return parameter.value();
    }

    @Override
    public <T> void set(Parameter<T> parameter, T value) {
        modelParameters.put(parameter, value);
    }

    @Override
    public <T> boolean hasParameter(Parameter<T> parameter) {
        return modelParameters.containsKey(parameter);
    }

    @Override
    public jRoute getRoute() {
        return route;
    }

    @Override
    public void setRoute(jRoute route) {
        this.route = route;
    }
}
