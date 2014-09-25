package apl;

import com.google.common.collect.Lists;
import microModel.core.traffic.DriverGenerator;
import microModel.core.jRoute;
import microModel.core.vehicle.Vehicle;
import microModel.random.Gaussian;

import java.util.List;
import java.util.Random;

public class AgentDriverGenerator extends DriverGenerator {

    private List<jRoute> routes;

    public AgentDriverGenerator(List<jRoute> routes) {
        super(new Vehicle.Builder(null), new AgentDriver.Builder(AgentDriverGenerator.randomRoute(routes)));
        this.routes = Lists.newArrayList(routes);
        vehicleClass.addStochasticDriverParameter("fSpeed", new Gaussian().mean(123.7 / 120).std(12 / 120));
    }

    private static jRoute randomRoute(List<jRoute> routes) {
        Random randomRouteIndex = new Random(0);
        return routes.get(randomRouteIndex.nextInt(routes.size()));
    }

    public List<jRoute> getRoutes() {
        return routes;
    }
}
