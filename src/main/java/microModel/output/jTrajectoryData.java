package microModel.output;

import java.io.Serializable;

/**
 * Storable trajectory data from a <tt>VehicleLogBuffer</tt> object.
 */
public class jTrajectoryData implements Serializable {

    /** Time [s] array. */
    public double[] t;
    
    /** Position [m] array. */
    public double[] x;
    
    /** Speed [m/s] array. */
    public double[] v;
    
    /** Acceleration [m/s^2] array. */
    public double[] a;

    /** Gaps to lead vehicle. */
    public double[] gaps;
    
    /** Lane ID array. */
    public int[] lane;
    
    /** Lane change progress [0...1] array. */
    public double[] lcProgress;
    
    /** Vehicle class ID. */
    public int classID;

    /**
     * Constructs a data object from the given <tt>VehicleLogBuffer</tt>.
     * @param buffer Buffer of which the data needs to be stored.
     */
    public jTrajectoryData(VehicleLogBuffer buffer) {
        t = buffer.t();
        x = buffer.x();
        v = buffer.v();
        a = buffer.a();
        gaps = buffer.gaps();
        lane = buffer.laneID();
        lcProgress = buffer.lcProgress();
        classID = buffer.vehicle.classID;
    }
}