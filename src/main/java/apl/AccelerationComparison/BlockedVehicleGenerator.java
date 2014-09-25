package apl.AccelerationComparison;

import apl.AgentDriverGenerator;
import apl.jSimEnvironment;
import eis.exceptions.EntityException;
import microModel.core.traffic.AbstractQueuedTrafficGenerator;
import microModel.core.jRoute;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;

import java.util.List;

public class BlockedVehicleGenerator extends AbstractQueuedTrafficGenerator {
    private final String type;
    jSimEnvironment environment;
    AgentDriverGenerator driverGenerator;
    private int count = 0;

    public BlockedVehicleGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, String type) {
        super(lane);
        this.environment = environment;
        this.type = type;
        driverGenerator = new AgentDriverGenerator(routes);
    }

    @Override
    public void init() {}

    @Override
    public void control() {
        if (count == 0) {
            AbstractVehicle vehicle = driverGenerator.generate(getLane(), 0, environment.VEHICLE_COUNTER++);
            vehicle.setSpeed(getLane().getVLimInMetersPerSecond()/2);
            register(vehicle, "BLOCKING");
            driverGenerator.addToSimulation(vehicle);
            count++;
        }
        if (count == 1 && jModel.getInstance().getT() > 4) {
            AbstractVehicle vehicle = driverGenerator.generate(getLane(), 0, environment.VEHICLE_COUNTER++);
            vehicle.setSpeed(getLane().getVLimInMetersPerSecond()/2);
            register(vehicle, type);
            driverGenerator.addToSimulation(vehicle);
            count++;
        }
//        if (count == 2 && jModel.getInstance().getT() > 5) {
//            AbstractVehicle vehicle = driverGenerator.generate(lane, 0, environment.VEHICLE_COUNTER++);
//            vehicle.setSpeed(lane.getVLimInMetersPerSecond());
//            driverGenerator.addToSimulation(lane, vehicle);
//            register(vehicle, type);
//            count++;
//        }
//        if (count == 3 && jModel.getInstance().getT() > 7) {
//            AbstractVehicle vehicle = driverGenerator.generate(lane, 0, environment.VEHICLE_COUNTER++);
//            vehicle.setSpeed(lane.getVLimInMetersPerSecond());
//            driverGenerator.addToSimulation(lane, vehicle);
//            register(vehicle, type);
//            count++;
//        }


    }

    private void register(AbstractVehicle vehicle, String type) {
        try {
            environment.registerEntity("driver" + vehicle.getDriver().getID() , type, vehicle.getDriver());
        } catch (EntityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noControl() {}

    public int getQueue() {
        return queue.size();
    }

}
