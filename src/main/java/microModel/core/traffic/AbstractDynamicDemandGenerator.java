package microModel.core.traffic;

import apl.jSimEnvironment;
import com.google.common.collect.Range;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;
import microModel.jModel;
import microModel.random.ProbabilityDistribution;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import microModel.util.TableData;

import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractDynamicDemandGenerator extends AbstractQueuedTrafficGenerator {

    /** Column number in which the timestamp is stored */
    private final static int DATA_TIME_COLUMN_INDEX = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_TIME_COLUMN_INDEX);
    /** Column number in which the demand value is stored */
    private final static int DATA_DEMAND_COLUMN_INDEX = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_DEMAND_COLUMN_INDEX);
    /** Column number in which the mean speed is stored */
    private final static int DATA_SPEED_COLUMN_INDEX = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_SPEED_COLUMN_INDEX);
    /** The vehicle generator. */
    private final DriverGenerator generator;
    /** Current demand level [veh/h] */
    protected long demand = 0;
    /** Current mean speed level [m/s] */
    protected double meanSpeed = 0;
    /**
     * Queue containing time stamp values for releasing queued vehicles.
     * This is filled up depending on the demand value and the distribution specified as the headway distribution.
     */
    private Queue<Double> vehicleReleaseTimes = new LinkedList<Double>();
    /** The current time stamp used from data to generate demand. */
    private Long currentTimeStamp = null;
    /** The demand data. This is a timeseries indicating the demand at specific simulation times. */
    private TableData<Long> demandData;
    private Distribution headwayDistribution = Distribution.UNIFORM;


    protected AbstractDynamicDemandGenerator(jLane lane, DriverGenerator generator, TableData<Long> demandData, Distribution headwayDistribution) {
        super(lane);
        this.generator = generator;
        this.demandData = demandData;
        this.headwayDistribution = headwayDistribution;
    }

    @Override
    public void init() {
        jModel model = jModel.getInstance();
        TableData<Long> futureData = demandData.filter(DATA_TIME_COLUMN_INDEX, Range.atMost(model.getAbsoluteT().longValue()));
        int lastRow = futureData.rowSize() - 1;
        currentTimeStamp = futureData.get(lastRow, DATA_TIME_COLUMN_INDEX);
        demand = futureData.get(lastRow, DATA_DEMAND_COLUMN_INDEX);
        meanSpeed = futureData.get(lastRow, DATA_SPEED_COLUMN_INDEX) * 1000 / 3600;
        queueVehicles();
    }

    /**
     * Adds vehicles to the queue to be released on the lane
     * based on the current demand.
     */
    private void queueVehicles() {
        for (int i = 0; i < demand; i++) {
            AbstractVehicle vehicle = generator.generate(getLane(), 0, jSimEnvironment.VEHICLE_COUNTER++);
            vehicle.setSpeed(meanSpeed);
            queue.add(vehicle);
        }
        distributeVehicles();
    }

    /**
     * Generates release times for newly queued vehicles according to the
     * headway distribution specified for this generator.
     */
    private void distributeVehicles() {
        jModel model = jModel.getInstance();
        long interval = interval();

        if (Distribution.UNIFORM == headwayDistribution) {
            for (int i = 0; i < demand; i++) {
                vehicleReleaseTimes.add(model.getT() + i * (interval / demand));
            }
        } else if (Distribution.EXPONENTIAL == headwayDistribution) {
            //TODO
        } else if (Distribution.POISSON == headwayDistribution) {
            //TODO
        }
    }

    @Override
    public void control() {
        jModel model = jModel.getInstance();
        if (model.getT() < model.getSimulationLength()) {

            /* Find the most recent time stamp in the data */

            TableData<Long> futureData = demandData.filter(DATA_TIME_COLUMN_INDEX, Range.atMost(model.getAbsoluteT().longValue()));
            int lastRow = futureData.rowSize() - 1;
            Long mostRecentTimeStamp = futureData.get(lastRow, DATA_TIME_COLUMN_INDEX);

            /* Determine if a new timestamp has been encountered. */

            if (currentTimeStamp != mostRecentTimeStamp) {
                /* New interval has started.
                   Need to use the new demand value and generate new series of cars.*/
                currentTimeStamp = mostRecentTimeStamp;
                demand = futureData.get(lastRow, DATA_DEMAND_COLUMN_INDEX);
                meanSpeed = futureData.get(lastRow, DATA_SPEED_COLUMN_INDEX) * 1000 / 3600;
                queueVehicles();
            }

            /* Release queued vehicles according to headway distribution. */

            if (!queue.isEmpty()) {
                AbstractVehicle vehicle = queue.peek();
                Double releaseTime = vehicleReleaseTimes.peek();
                if (model.getT() >= releaseTime) {
                    generator.addToSimulation(vehicle);
                    register(vehicle);
                    queue.remove();
                    vehicleReleaseTimes.remove();
                }
            }
        }
    }

    @Override
    public void noControl() {
        // Do Nothing.
    }

    protected abstract void register(AbstractVehicle vehicle);

    /**
     * <p> Determines the data sampling rate. This is the time
     * difference between the timestamps at which detector data
     * is aggregated.
     * </p><p>
     * It assumes that the interval size remains constant in the data.
     * </p>
     *
     * @return the data sample rate [s].
     */
    private Long interval() {
        Long timeStamp1 = demandData.get(0, DATA_TIME_COLUMN_INDEX);
        Long timeStamp2 = demandData.get(1, DATA_TIME_COLUMN_INDEX);
        return timeStamp2 - timeStamp1;
    }

    /**
     * Determines a headway value based on generator settings.
     *
     * @return Headway [s].
     */
    //TODO: remove this is no longer needed
    private double headway() {
        double headway = 0;
        if (demand > 0) {
            double dt = 60 / demand; // average headway
            if (headwayDistribution == Distribution.UNIFORM) {
                // always the average headway
                headway = dt;
            } else if (headwayDistribution == Distribution.EXPONENTIAL) {
                // note: r = -log(uniform)/gamma & mean = 1/gamma
                headway = -Math.log(ProbabilityDistribution.RANDOM.nextDouble()) * dt;
            }
        } else {
            // no demand
            headway = Double.POSITIVE_INFINITY;
        }
        return headway;
    }


    /** Enumeration of possible headway distributions. */
    public enum Distribution {
        /**
         * Exponential Distribution. Very short headways may result. The default
         * vehicle generation is able to deal with this by delaying vehicle
         * generation and generating a queued vehicle (at following headway).
         */
        EXPONENTIAL,
        /** Vehicles are uniformely spread over time (fixed headway). */
        UNIFORM,
        POISSON;
    }

    public static abstract class Builder {
        protected jLane lane;
        protected DriverGenerator generator;
        protected TableData<Long> demandData;
        protected Distribution headwayDistribution;

        public Builder(DriverGenerator generator) {
            this.generator = generator;
        }

        public Builder setLane(jLane lane) {
            this.lane = lane;
            return this;
        }

        public Builder setDemandData(TableData<Long> demandData) {
            this.demandData = demandData;
            return this;
        }

        public Builder setHeadwayDistribution(Distribution headwayDistribution) {
            this.headwayDistribution = headwayDistribution;
            return this;
        }

        public abstract AbstractDynamicDemandGenerator build();
    }
}
