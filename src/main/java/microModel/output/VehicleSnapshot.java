package microModel.output;

import microModel.core.road.LatDirection;
import microModel.core.road.jLane;
import microModel.core.vehicle.Enclosure;
import microModel.core.vehicle.Vehicle;
import microModel.jModel;

/** Immutable Snapshot of a vehicle's state. */
public class VehicleSnapshot {

    /** Time of snapshot. */
    private double t;
    /** Position of vehicle on the lane. */
    private double x;
    /** Speed of  vehicle [m/s]. */
    private double v;
    /** Acceleration of vehicle [m/s^2]. */
    private double a;
    /** gap to leading vehicle if any */
    private double gap = Double.MAX_VALUE;
    /** LaneType at which the vehicle is. */
    private jLane lane;
    /** LaneType changing progress of vehicle, including direction [-1...1]. */
    private double lcProgress;

    /**
     * Constructs a snapshot of the given vehicle.
     *
     * @param veh
     */
    public VehicleSnapshot(Vehicle veh) {
        jModel model = jModel.getInstance();
        t = model.getT();
        x = veh.getX();
        v = veh.getSpeed();
        a = veh.getAcceleration();
        gap = veh.getGap(veh.getVehicle(Enclosure.DOWNSTREAM));
        lane = veh.getLane();
        if (veh.lcDirection == LatDirection.LEFT) {
            lcProgress = -veh.laneChangeProgress; // [-1...0]
        } else {
            lcProgress = veh.laneChangeProgress; // [0...1]
        }
    }

    /** @return {@link #t} */
    public double getT() {
        return t;
    }

    /** @return {@link #x} */
    public double getX() {
        return x;
    }

    /** @return {@link #v} */
    public double getV() {
        return v;
    }

    /** @return {@link #a} */
    public double getA() {
        return a;
    }

    /** @return {@link #gap} */
    public double getGap() {
        return gap;
    }

    /** @return {@link #lane} */
    public jLane getLane() {
        return lane;
    }

    /** @return {@link #lcProgress} */
    public double getLcProgress() {
        return lcProgress;
    }
}