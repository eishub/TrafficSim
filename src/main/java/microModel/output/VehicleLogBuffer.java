package microModel.output;

import microModel.core.road.jLane;
import microModel.core.vehicle.Vehicle;
import microModel.jModel;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Trajectory of a vehicle. This is basically an array of <tt>VehicleSnapshot</tt> objects.
 */
public class VehicleLogBuffer {

    /** Array of <tt>VehicleSnapshot</tt> objects. */
    protected List<VehicleSnapshot> vehicleSnapshots = new ArrayList<VehicleSnapshot>();
    
    /** Vehicle of this trajectory. */
    public Vehicle vehicle;

    /** Time when last snap-shot of vehicle was stored. */
    protected double previousSnapshotTime;

    /**
     * Constructor linking this trajectory to a vehicle.
     */
    public VehicleLogBuffer(Vehicle vehicle) {
        jModel model = jModel.getInstance();
        jSettings settings = jSettings.getInstance();
        this.vehicle = vehicle;
        previousSnapshotTime = model.getT() - settings.get(BuiltInSettings.DEBUG_TRAJECTORY_SAMPLING_RATE);
    }

    /**
     * Appends current data of vehicle to an internal array at appropriate interval.
     */
    public synchronized void log() {
        jModel model = jModel.getInstance();
        jSettings settings = jSettings.getInstance();

        if (settings.get(BuiltInSettings.DEBUG_TRAJECTORY) &&
                model.getT() - previousSnapshotTime >= settings.get(BuiltInSettings.DEBUG_TRAJECTORY_SAMPLING_RATE)) {
            // create new VehicleSnapshot and add to vector
            vehicleSnapshots.add(new VehicleSnapshot(vehicle));
            // update last sampling time
            previousSnapshotTime = previousSnapshotTime + settings.get(BuiltInSettings.DEBUG_TRAJECTORY_SAMPLING_RATE);
        }
    }
    
    /**
     * Composes time array.
     * @return Array of time [s].
     */
    public double[] t() {
        double[] t = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            t[i] = vehicleSnapshots.get(i).getT();
        }
        return t;
    }

    /**
     * Composes position array.
     * @return x Array of positions [m].
     */
    public double[] x() {
        double[] x = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            x[i] = vehicleSnapshots.get(i).getX();
        }
        return x;
    }

    /**
     * Composes speed array.
     * @return v Array of speeds [m/s].
     */
    public double[] v() {
        double[] v = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            v[i] = vehicleSnapshots.get(i).getV();
        }
        return v;
    }

    /**
     * Composes acceleration array.
     * @return a Array of accelerations [m/s^2].
     */
    public double[] a() {
        double[] a = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            a[i] = vehicleSnapshots.get(i).getA();
        }
        return a;
    }

    public double[] gaps() {
        double[] gaps = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            gaps[i] = vehicleSnapshots.get(i).getGap();
        }
        return gaps;
    }

    /**
     * Composes lane change progress array.
     * @return laneChangeProgress Array of lane change progress [0...1].
     */
    public double[] lcProgress() {
        double[] lcProgress = new double[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            lcProgress[i] = vehicleSnapshots.get(i).getLcProgress();
        }
        return lcProgress;
    }

    /**
     * Composes lane array.
     * @return lane Array of lane IDs.
     */
    public int[] laneID() {
        int[] lane = new int[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            lane[i] = vehicleSnapshots.get(i).getLane().getId();
        }
        return lane;
    }

    /**
     * Composes lane array.
     * @return lane Array of <tt>jLane</tt> objects.
     */
    public jLane[] lane() {
        jLane[] lane = new jLane[vehicleSnapshots.size()];
        for (int i=0; i< vehicleSnapshots.size(); i++) {
            lane[i] = vehicleSnapshots.get(i).getLane();
        }
        return lane;
    }

    /**
     * NOTE: calling this method will clear the buffer.
     */
    public synchronized void saveTrajectoryLogToDisk() {
        jTrajectoryData data = prepareBufferForSave();
        jSettings settings = jSettings.getInstance();
        //make sure the output directory exists and if not create it
        File f = new File(settings.get(BuiltInSettings.OUTPUT_PATH), "trajectories");
        f.mkdirs();

        DecimalFormat df = new DecimalFormat("000000");
        try {
            File file;
            int counter = 0;
            do {
                file = new File(settings.get(BuiltInSettings.OUTPUT_PATH) +
                    File.separator +
                    "trajectories" +
                    File.separator +
                    "trajectory" +
                    df.format(vehicle.getDriver().getID()) +
                    ".dat"
                );
            } while (file.exists());

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.close();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
}

    private jTrajectoryData prepareBufferForSave() {
        jTrajectoryData data = new jTrajectoryData(this);
        this.vehicleSnapshots.clear();
        return data;
    }
}