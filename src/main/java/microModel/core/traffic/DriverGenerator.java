package microModel.core.traffic;

import microModel.core.driver.jDriver;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.core.vehicle.Vehicle;
import microModel.core.vehicle.jClass;
import microModel.jModel;

public abstract class DriverGenerator {

    protected Vehicle.Builder vehicleBuilder;
    protected jDriver.Builder driverBuilder;
    protected jClass vehicleClass;

    protected DriverGenerator(Vehicle.Builder vehicleBuilder, jDriver.Builder driverBuilder) {
        this.vehicleBuilder = vehicleBuilder;
        this.driverBuilder = driverBuilder;
        vehicleClass = new jClass(this.vehicleBuilder, this.driverBuilder);
    }

    public AbstractVehicle generate(jLane lane, double initialSpeed, int id) {
        Vehicle vehicleInstance = vehicleClass.generateVehicle(lane, initialSpeed, id);
        return vehicleInstance;
    }

    public void addToSimulation(AbstractVehicle vehicle) {
        if (vehicle.getLane() == null) {
            return;
        }
        jModel model = jModel.getInstance();
        model.addVehicle(vehicle);
        vehicleClass.putOnLane(vehicle.getLane(), vehicle);
    }
}
