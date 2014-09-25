/**
 * Created by arman on 2/6/14.
 */
package apl.Merging;

import apl.AgentDriver;
import apl.AgentDriverGenerator;
import apl.jSimEnvironment;
import eis.exceptions.EntityException;
import microModel.core.jRoute;
import microModel.core.road.jLane;
import microModel.core.traffic.AbstractQueuedTrafficGenerator;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;

import java.util.List;

public class TimeHeadwayGenerator extends AbstractQueuedTrafficGenerator {


    private jSimEnvironment environment;
    private AgentDriverGenerator driverGenerator;
    private double timeHeadway;
    private double vehicleSpeed;

    public TimeHeadwayGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, double timeHeadway, double vehicleSpeed) {
        super(lane);
        this.environment = environment;
        driverGenerator = new AgentDriverGenerator(routes);
        this.timeHeadway = timeHeadway;
        this.vehicleSpeed = vehicleSpeed;
    }


    @Override
    public void init() {}

    @Override
    public void control() {
        jModel model = jModel.getInstance();
        if (model.getT() < model.getSimulationLength()) {
            // First place the vehicles in the queue on the road.
            if (!queue.isEmpty()) {
                AbstractVehicle vehicle = queue.peek();
                if (getLane().calculateSpaceHeadway()/vehicleSpeed >= timeHeadway && getLane().calculateSpaceHeadway() >= vehicle.getLength()) {
                    driverGenerator.addToSimulation(vehicle);
                    register(vehicle, AgentDriver.TYPE);
                    queue.remove();
                }
            }
            // Then generate vehicles and add them to the queue.
            else {
                AbstractVehicle vehicle = driverGenerator.generate(getLane(), 0, environment.VEHICLE_COUNTER++);
                vehicle.setSpeed(vehicleSpeed);
                if (getLane().calculateSpaceHeadway()/vehicleSpeed >= timeHeadway && getLane().calculateSpaceHeadway() >= vehicle.getLength()) {
                    driverGenerator.addToSimulation(vehicle);
                    register(vehicle, AgentDriver.TYPE);
                }
                else {
                    queue.add(vehicle);
                }
            }
        }
    }

    protected void register(AbstractVehicle vehicle, String type) {
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
