package microModel.map;

import GUI.jModelGUI;
import microModel.core.road.jLane;
import microModel.jModel;
import microModel.map.road.RoadSegment;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KMLImporterTest {

    @Test
    public void setupRoadNetwork() {
        jSettings.getInstance().put(BuiltInSettings.IMPORT_DETECTOR_FROM_KML, Boolean.TRUE);
        KMLImporter imp = new KMLImporter("/A16.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }

        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        //Don't try to log or output results
        jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, Boolean.FALSE);
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.FALSE);

        jModel model = new jModel.Builder((lanes.toArray(new jLane[0]))).build(0);
        jModelGUI gui = new jModelGUI();
    }

    @Test
    public void setup2Lane() {
        KMLImporter imp = new KMLImporter("/2-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }


        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        //Don't try to log or output results
        jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, Boolean.FALSE);
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.FALSE);

        jModel model = new jModel.Builder((lanes.toArray(new jLane[0]))).build(0);
        jModelGUI gui = new jModelGUI();
    }

    @Test
    public void setup3Lane() {
        KMLImporter imp = new KMLImporter("/3-Lanes.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }


        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        //Don't try to log or output results
        jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, Boolean.FALSE);
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.FALSE);

        jModel model = new jModel.Builder((lanes.toArray(new jLane[0]))).build(0);
        jModelGUI gui = new jModelGUI();

    }

    @Test
    public void merge() {
        KMLImporter imp = new KMLImporter("/merge.kml");
        Map<Integer,RoadSegment> network = imp.setupRoadSegments();
        for (RoadSegment rs: network.values()) {
            rs.init(network);
        }


        List<jLane> lanes = new ArrayList<jLane>();
        for (RoadSegment rs: network.values()) {
            lanes.addAll(rs.getLanes());
        }
        //Don't try to log or output results
        jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, Boolean.FALSE);
        jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, Boolean.FALSE);

        jModel model = new jModel.Builder((lanes.toArray(new jLane[0]))).build(0);
        jModelGUI gui = new jModelGUI();
    }
}
