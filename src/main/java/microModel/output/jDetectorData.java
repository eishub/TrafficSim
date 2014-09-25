package microModel.output;

import microModel.core.road.device.jDetector;

/**
 * Storable detector data from a <tt>jDetector</tt> object.
 */
public class jDetectorData implements java.io.Serializable {

    /** Flow counts. */
    public int[] q;

    /** Average speeds. */
    public double[] v;

    /** LaneType ID of lane where the detector is positioned. */
    public int lane;

    /** Location of detector on lane. */
    public double x;
    
    /**
     * Constructs a data object from the given <tt>jDetector</tt>.
     * @param detector Detector of which the data needs to be stored.
     */
    public jDetectorData(jDetector detector) {
        q = new int[detector.qHist.size()];
        for (int i=0; i<detector.qHist.size(); i++) {
            q[i] = detector.qHist.get(i);
        }
        v = new double[detector.vHist.size()];
        for (int i=0; i<detector.vHist.size(); i++) {
            v[i] = detector.vHist.get(i);
        }
        lane = detector.lane.getId();
        x = detector.getX();
    }
}