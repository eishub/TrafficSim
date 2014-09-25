package apl.CarFollowing;

import apl.AgentDriver;
import apl.CarFollowing.BlockedDensityGenerator;
import apl.jSimEnvironment;
import eis.exceptions.EntityException;
import microModel.core.jRoute;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;

import java.util.List;
import java.util.Random;

public class BlockedMixedDensityGenerator extends BlockedDensityGenerator {

    private static final String[] TYPES = new String[] {AgentDriver.TYPE, "IDM"};

    public BlockedMixedDensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany) {
        super(environment, lane, routes, howMany);
    }

    public BlockedMixedDensityGenerator(jSimEnvironment environment, jLane lane, List<jRoute> routes, int howMany, double gap) {
        super(environment, lane, routes, howMany, gap);
    }

    @Override
    protected void register(AbstractVehicle vehicle, String type) {
        Random rand = new Random();
        int t = rand.nextInt(2);
        String driverType = type.compareTo("BLOCKING") == 0 ? type : TYPES[t];
        try {
            environment.registerEntity("driver" + vehicle.getDriver().getID() , driverType, vehicle.getDriver());
        } catch (EntityException e) {
            e.printStackTrace();
        }
    }
}
