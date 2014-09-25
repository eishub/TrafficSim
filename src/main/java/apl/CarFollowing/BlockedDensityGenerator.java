package apl.CarFollowing;

import apl.AgentDriver;
import apl.CarFollowing.DensityGenerator;
import apl.jSimEnvironment;
import microModel.core.jRoute;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;

import java.util.List;

public class BlockedDensityGenerator extends DensityGenerator {

    public BlockedDensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany) {
        super(environment, lane, routes, howMany);
    }

    public BlockedDensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany, double gap) {
        super(environment, lane, routes, howMany, gap);
    }

    @Override
    public void control() {
        jModel model = jModel.getInstance();
        if (model.getT() < model.getSimulationLength()) {
            // First place the vehicles in the queue on the road.
            if (!queue.isEmpty()) {
                AbstractVehicle vehicle = queue.peek();
                if (getLane().calculateSpaceHeadway() >= gap && getLane().calculateSpaceHeadway() >= vehicle.getLength()) {
                    driverGenerator.addToSimulation(vehicle);
                    register(vehicle, AgentDriver.TYPE);
                    queue.remove();
                }
            }
            // Then generate vehicles and add them to the queue.
            else {
                if (count == 0) {
                    AbstractVehicle vehicle = driverGenerator.generate(getLane(), 0, environment.VEHICLE_COUNTER++);
                    vehicle.setSpeed(getLane().getVLimInMetersPerSecond()/2);
                    register(vehicle, "BLOCKING");
                    driverGenerator.addToSimulation(vehicle);
                    count++;
                }
                else {
                    AbstractVehicle vehicle = driverGenerator.generate(getLane(), 0, environment.VEHICLE_COUNTER++);
                    vehicle.setSpeed(Math.min(getLane().getVLimInMetersPerSecond(), vehicle.getSafeSpeed(gap)));
                    if (getLane().calculateSpaceHeadway() >= gap && getLane().calculateSpaceHeadway() >= vehicle.getLength()) {
                        driverGenerator.addToSimulation(vehicle);
                        register(vehicle, AgentDriver.TYPE);
                    }
                    else {
                        queue.add(vehicle);
                    }
                    count++;
                }
            }
        }
    }

}
