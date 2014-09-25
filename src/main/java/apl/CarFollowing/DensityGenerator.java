package apl.CarFollowing;

import apl.AgentDriver;
import apl.AgentDriverGenerator;
import apl.jSimEnvironment;
import eis.exceptions.EntityException;
import microModel.core.traffic.AbstractQueuedTrafficGenerator;
import microModel.core.jRoute;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;

import java.util.List;

public class DensityGenerator extends AbstractQueuedTrafficGenerator {

    jSimEnvironment environment;
    AgentDriverGenerator driverGenerator;
    protected int count = 0;
    private int howMany;
    protected  double gap = 20;

    public DensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany) {
        super(lane);
        this.environment = environment;
        driverGenerator = new AgentDriverGenerator(routes);
        this.howMany = howMany;
    }

    public DensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany, double gap) {
        super(lane);
        this.environment = environment;
        driverGenerator = new AgentDriverGenerator(routes);
        this.howMany = howMany;
        this.gap = gap;
    }

    public void setDensity(double density) {
        this.gap = 1/density;
    }

    public void setGap(double gap) {
        this.gap = gap;
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
                if (getLane().calculateSpaceHeadway() >= gap && getLane().calculateSpaceHeadway() >= vehicle.getLength()) {
                    driverGenerator.addToSimulation(vehicle);
                    register(vehicle, AgentDriver.TYPE);
                    queue.remove();
                }
            }
            // Then generate vehicles and add them to the queue.
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

    protected void register(AbstractVehicle vehicle, String type) {
        try {
            environment.registerEntity("driver" + vehicle.getDriver().getID() , type, vehicle.getDriver());
        } catch (EntityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noControl() {}

    /**
     * density in vehicles per Km.
     * @return
     */
    public double getDensity() {
        return 1000/gap;
    }

    public boolean isDone() {
        return count == howMany && this.queue.isEmpty();
    }

    public int getQueue() {
        return queue.size();
    }
}

