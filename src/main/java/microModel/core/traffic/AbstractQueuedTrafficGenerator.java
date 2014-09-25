package microModel.core.traffic;

import microModel.core.device.AbstractController;
import microModel.core.road.jLane;
import microModel.core.vehicle.AbstractVehicle;

import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractQueuedTrafficGenerator extends AbstractController {
    /** Lane at the start of which vehicles need to be generated. */
    private final jLane lane;

    protected Queue<AbstractVehicle> queue;

    protected AbstractQueuedTrafficGenerator(jLane lane) {
        this.lane = lane;
        lane.setGenerator(this);
        queue = new LinkedList<AbstractVehicle>();
    }

    public jLane getLane() {
        return lane;
    }

    public int getQueue() {
        if (queue == null) {
            return 0;
        }
        return queue.size();
    }
}
