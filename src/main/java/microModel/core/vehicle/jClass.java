package microModel.core.vehicle;

import microModel.core.driver.jDriver;
import microModel.core.driver.model.IDMPlus;
import microModel.core.road.jLane;
import microModel.jModel;
import microModel.random.ProbabilityDistribution;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * General shell describing a vehicle-driver class. <tt>jClass</tt> is not to be
 * confused with java classes. It takes two pre-configured Object Builders, one
 * for Generating vehicles and one for putting drivers in the vehicle.
 * The <tt>generateVehicle()</tt> method returns a vehicle loaded with a driver on
 * a given lane with a given route for the driver. If lane or route is not provided
 * the default values from the Builders will be used.
 * </p>
 * <p>
 * Additionally, stochastic modelParameters of the vehicle and the driver can be defined
 * by <tt>addStochasticVehicleParameter</tt> and <tt>addStochasticDriverParameter</tt>.
 * Stochastic modelParameters must be <tt>double</tt>.</br>
 * </p>
 * <p>
 * For more complex definitions of stochastic modelParameters, new java classes have
 * to be defined extending this class. In that class override the
 * <tt>generateVehicle()</tt> method and use <tt>super.generateVehicle()</tt>
 * to provide the default vehicle with default stochastic modelParameters, which can
 * then be adapted. Note that both the OBU and the driver also belong to the
 * vehicle, including all their attributes (modelParameters).
 * </p>
 */
public class jClass {

    private final Logger logger = Logger.getLogger(jClass.class);
    private static int classCounter =0 ;
    /** Identifier of the class which is for the user only. */
    private int id;

    private Vehicle.Builder vehicleBuilder;

    private jDriver.Builder driverBuilder;

    /** Set of distributions for vehicle modelParameters. */
    private Map<String, ProbabilityDistribution<Double>> stochasticVehicleParameters =
            new HashMap<String, ProbabilityDistribution<Double>>();

    /** Set of distributions for driver modelParameters. */
    private Map<String, ProbabilityDistribution<Double>> stochasticDriverParameters =
            new HashMap<String, ProbabilityDistribution<Double>>();

    public jClass(Vehicle.Builder vehicleBuilder, jDriver.Builder driverBuilder){
        this.id = classCounter++;
        this.vehicleBuilder = vehicleBuilder;
        this.driverBuilder = driverBuilder;
        jModel model = jModel.getInstance();
        model.addClass(this);
    }

    /**
     * <p>Generates a new vehicle on the given lane with the provided route. The driver and OBUs will also be initialized.</p>
     * <p>The vehicle and its driver are generated using the BuildHelper instances provided during the construction of this
     * jClass instance.</p>
     * <p>Any changes to this instance (e.g. the addition of stochastic vehicle or driver modelParameters) will apply to the builders
     * and therefore also effect later calls to the generateVehicle method.</p>
     *
     *
     * @param onLane The lane on which the vehicle should be placed.
     * @param withSpeed The speed at which the vehicle starts moving.
     * @return Generated vehicle with default values.
     */
    public Vehicle generateVehicle(jLane onLane, double withSpeed, int id) {
        // start of with the default vehicle
        Vehicle veh;
        jDriver d;
        if (onLane == null) {
            veh = vehicleBuilder.build();
        }
        else {
            veh = vehicleBuilder.onLane(onLane).build();
        }
//        logger.debug("Generated Vehicle " + veh);

        d = driverBuilder.build();
        d.setVehicle(veh);
        veh.getDriver().setID(id);

//        logger.debug("Generated Driver " + veh.getDriver()+" for Vehicle " + veh);
        // set any stochastic modelParameters
        setStochasticParameters(veh);
//        veh.setXY();
        // initialize OBU
        if (veh.isEquipped()) {
            veh.OBU.init();
        }
        veh.setSpeed(withSpeed);
        return veh;
    }

    public void putOnLane(jLane onLane, AbstractVehicle vehicle) {
        vehicle.updateLane(onLane, 0, 0);
    }


    /**
     * Generates a vehicle with a default route and places it on a default lane.
     * The default route and lane should have been provided with the Builders used in the construction of
     * {@link #jClass(microModel.core.vehicle.Vehicle.Builder, microModel.core.driver.jDriver.Builder)} this instance
     * or alternatively through a previous call to {@link jClass#generateVehicle(microModel.core.road.jLane, double, int)}
     * @return Generated vehicle with default values.
     * @param id
     */
    public Vehicle generateVehicle(int id) {
        // start of with the default vehicle
        return generateVehicle(null, 30, id);
    }


    /**
     * Sets all stochastic modelParameters of the driver and vehicle. Also calls the
     * <tt>correlateParameter()</tt> methods of the vehicle and driver.
     * @param veh Vehicle for stochastic modelParameters
     */
    protected void setStochasticParameters(Vehicle veh) {
        // Set stochastic driver modelParameters
        if (stochasticDriverParameters.size()>0) {
            java.lang.reflect.Field[] fields = veh.getDriver().getClass().getFields();
            for (int i=1; i<fields.length; i++) {
                String param = fields[i].getName();
                if (stochasticDriverParameters.containsKey(param)) {
                    double value = stochasticDriverParameters.get(param).rand();
                    try {
                        fields[i].setDouble(veh.getDriver(), value);
                    } catch (Exception e) {
                        System.err.println("Unable to set parameter due to error: "+e.getMessage());
                    }
                }
            }
        }

        // Set stochastic vehicle modelParameters
        if (stochasticVehicleParameters.size()>0) {
            java.lang.reflect.Field[] fields = veh.getClass().getFields();
            for (int i=1; i<fields.length; i++) {
                String param = fields[i].getName();
                if (stochasticVehicleParameters.containsKey(param)) {
                    double value = stochasticVehicleParameters.get(param).rand();
                    try {
                        fields[i].setDouble(veh, value);
                    } catch (Exception e) {
                        System.err.println("Unable to set parameter due to error: "+e.getMessage());
                    }
                }
            }
        }

        // Set correlated modelParameters
        //veh.correlateParameters();
        // This is essentially what the correlateParameters() method of the driver used to do. I removed the method.
        veh.getDriver().set(IDMPlus.T, veh.getDriver().get(IDMPlus.T_MAX));;
    }
    
    /**
     * Returns the ID of this class.
     * @return ID of this class.
     */
    public Integer getId() {
        return id;
    }

    public Map<String, ProbabilityDistribution<Double>> getStochasticVehicleParameters() {
        return stochasticVehicleParameters;
    }

    public Map<String, ProbabilityDistribution<Double>> getStochasticDriverParameters() {
        return stochasticDriverParameters;
    }


    /**
     * Adds a stochastic parameter for vehicles of this class.
     * @param param Name of stochastic vehicle modelParameters.
     * @param distr Distribution of random parameter.
     */
    public void addStochasticVehicleParameter(String param, ProbabilityDistribution<Double> distr) {
        stochasticVehicleParameters.put(param, distr);
    }

    /**
     * Adds a stochastic parameter for drivers of this class.
     * @param param Name of stochastic driver modelParameters.
     * @param distr Distribution of random parameter.
     */
    public void addStochasticDriverParameter(String param, ProbabilityDistribution<Double> distr) {
        stochasticDriverParameters.put(param, distr);
    }

}