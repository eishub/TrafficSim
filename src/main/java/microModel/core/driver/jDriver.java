package microModel.core.driver;

import microModel.core.Parameter;
import microModel.core.jRoute;
import microModel.core.vehicle.Vehicle;
import microModel.core.vehicle.jVehicle;

import java.util.HashMap;
import java.util.Map;

public interface jDriver {

    public int getID();
    public void setID(int id);

    public <T> T get(Parameter<T> parameter);
    public <T> void set(Parameter<T> parameter, T value);
    public <T> boolean hasParameter(Parameter<T> parameter);
    public void drive();
    public jRoute getRoute();
    public void setRoute(jRoute route);
    public void setVehicle(Vehicle vehicle);
    public jVehicle getVehicle();
    public double getSafeSpeed(double distance, double stoppingDistance, double safeTimeHeadway);


    /**
     * Utility class to help with making Builders for complex objects using the Builder Pattern.
     * This is to be used in conjunction with the {@link Builder} interface to construct builders
     * for drivers.
     *
     * This abstract class is a generic storage for the modelParameters with which the driver needs to
     * be instantiated.
     *
     * Example of usage:
     *   public class Builder extends jDriver.BuildHelper implements jDriver.Builder
     *
     */
    public static abstract class BuildHelper<T extends BuildHelper<T>> {
    /*
    * BuildHelper class for building drivers with all their modelParameters.
    *
    * The reason why this pattern has been used is to be able to reuse the
    * functionality of the jDriver.BuildHelper for building any type of driver
    * and yet at the same time have their build methods return the correct type
    * of driver.
    *
    * For example:
    *
    * new Driver.BuildHelper().setA(<some double>).build()     -->returns a Driver object
    * new AgentDriver.BuildHelper().setA(<some double>).build() -->returns a AgentDriver
    *
    * if this pattern was not used:
    * new AgentDriver.BuildHelper().setA(<some double>) would have returned a Driver.BuildHelper type
    * which meant that the next method call in the chain would return a Driver instead of a
    * AgentDriver.
    */
        protected Map<Parameter<?>, Object> parameters = new HashMap<Parameter<?>, Object>();

        protected final T self() {
            return (T) this;
        }

        public final <PARAMETER_TYPE> T set(Parameter<PARAMETER_TYPE> parameter, PARAMETER_TYPE value) {
            parameters.put(parameter, value);
            return self();
        }
    }

    public interface Builder {
        public jDriver build();
    }



}
