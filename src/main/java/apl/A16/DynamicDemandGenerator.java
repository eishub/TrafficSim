package apl.A16;

import apl.AgentDriver;
import apl.AgentDriverGenerator;
import apl.jSimEnvironment;
import eis.exceptions.EntityException;
import microModel.core.road.jLane;
import microModel.core.traffic.AbstractDynamicDemandGenerator;
import microModel.core.traffic.DriverGenerator;
import microModel.core.vehicle.AbstractVehicle;
import microModel.util.TableData;

public class DynamicDemandGenerator extends AbstractDynamicDemandGenerator {

    private jSimEnvironment environment;

    public static class Builder extends AbstractDynamicDemandGenerator.Builder {
        private jSimEnvironment environment;

        public Builder(AgentDriverGenerator generator) {
            super(generator);
        }

        public Builder withEnvironment(jSimEnvironment environment) {
            this.environment = environment;
            return this;
        }

        @Override
        public DynamicDemandGenerator build() {
            return new DynamicDemandGenerator(lane, generator, demandData, headwayDistribution, environment);
        }
    }

    private DynamicDemandGenerator(jLane lane, DriverGenerator generator, TableData<Long> demandData, Distribution headwayDistribution, jSimEnvironment environment) {
        super(lane, generator, demandData, headwayDistribution);
        this.environment = environment;
    }

    @Override
    public void register(AbstractVehicle vehicle) {
        try {
            environment.registerEntity("driver" + vehicle.getDriver().getID() , AgentDriver.TYPE, vehicle.getDriver());
        } catch (EntityException e) {
            e.printStackTrace();
        }
    }
}
