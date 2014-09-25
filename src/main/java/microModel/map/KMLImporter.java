package microModel.map;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import microModel.core.road.LaneTransition;
import microModel.core.road.LaneType;
import microModel.map.road.LaneInfo;
import microModel.map.road.RoadSegment;
import microModel.settings.BuiltInSettings;
import microModel.util.DetectorDataUtilities;
import microModel.util.DetectorDataUtilities.DynamicDemandInfo;

import org.opengis.feature.simple.SimpleFeature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * This Class is ro replace the the Matlab kml2net.m file in the original jModel project.
 * It is to provide the functionality to import and convert a KML map to Java Objects that
 * make up the network in which the simulation is run.
 */
public class KMLImporter {
    private InputStream kml;
    private Coordinate origin;

    public KMLImporter(String path) {
        try {
            this.kml = getClass().getResource(path).openStream();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Map<Integer, RoadSegment> setupRoadSegments() {
        Map<Integer, RoadSegment.Builder> segmentBuilders = new HashMap<Integer, RoadSegment.Builder>();

        List<SimpleFeature> placemarks = KMLUtilities.getPlacemarks(kml);

        //Find a suitable coordinates to use as the (relative) origin point in the simulation.
        List<Coordinate> minimumCoordinates = new ArrayList<Coordinate>();
        List<Coordinate> maximumCoordinates = new ArrayList<Coordinate>();
        for (SimpleFeature p : placemarks) {
            String laneName = (String) p.getAttribute("name");
            int segmentId = Integer.parseInt(laneName);

            //Parse description of the placemark.
            String desc = (String) p.getAttribute("description");
            RoadSegment.Builder builder = new RoadSegment.Builder(segmentId);

            Double speedLimit = DescriptionParser.getSpeedLimit(desc);
            builder.setSpeedLimit(speedLimit);

            List<Integer> downstreamSegments = DescriptionParser.getDownSegments(desc);
            for(Integer segment: downstreamSegments) {
                builder.addDownSegment(segment);
            }

            List<Integer> upstreamSegments = DescriptionParser.getUpSegments(desc);
            for(Integer segment: upstreamSegments) {
                builder.addUpSegment(segment);
            }

            List<LaneInfo> laneInfoList = DescriptionParser.getLanes(desc);
            builder.addLaneInformation(laneInfoList);

            //TODO: parse destinations sections completely ... (missing the detector id equations)
            List<Integer> destinations = DescriptionParser.getDestinations(desc);
            builder.setDestinations(destinations);

            List<String> detectorIDs = DescriptionParser.getDetectors(desc);
            builder.setDetectorIDs(detectorIDs);

            //TODO: parse origins sections
            List<DetectorDataUtilities.DynamicDemandInfo> originDemands = DescriptionParser.getOriginDemands(desc);
            builder.setOriginDemands(originDemands);

            LineString ls = KMLUtilities.getLineString((Geometry) p.getDefaultGeometry());
            List<Coordinate> segmentSphericalCoordinates = Arrays.asList(ls.getCoordinates());

            builder.setCoordinates(segmentSphericalCoordinates);

            minimumCoordinates.add(CoordinateUtilities.minCoordinate(segmentSphericalCoordinates));
            maximumCoordinates.add(CoordinateUtilities.maxCoordinate(segmentSphericalCoordinates));

            segmentBuilders.put(segmentId, builder);

        }
        Coordinate minimum = CoordinateUtilities.minCoordinate(minimumCoordinates);
        Coordinate maximum = CoordinateUtilities.maxCoordinate(maximumCoordinates);
        Coordinate relativeOrigin = new Coordinate(minimum.x, maximum.y, 0.0);
        origin = relativeOrigin;


        Map<Integer, RoadSegment> network = new HashMap<Integer, RoadSegment>();
        for (Integer segmentId: segmentBuilders.keySet()) {
            RoadSegment.Builder builder = segmentBuilders.get(segmentId);
            builder.convertSphericalCoordinatesToCartesian(relativeOrigin);
            builder.mirrorCoordinates();
            RoadSegment rs = builder.build();
            network.put(segmentId, rs);
        }
        RoadSegment.GLOBAL_LANE_COUNTER = 0;
        return network;
    }


    private static class DescriptionParser {

        private static int DESTINATION_COUNTER = 1;

        public static Double getSpeedLimit(String description) {
            Double result = new Double(0);
            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.SPEED_LIMIT_HEADER.value()) >= 0) {
                String[] tmpArray = copy.split(BuiltInSettings.SPEED_LIMIT_HEADER.value());
                String speedLimit = tmpArray[1].split("\n")[0];
                result = Double.parseDouble(speedLimit.trim());
            }
            return result;
        }

        public static List<Integer> getUpSegments(String description) {
            List<Integer> result = new ArrayList<Integer>();

            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.UPSTREAM_SEGMENT_HEADER.value()) >= 0) {
                String[] tmpArray = copy.split(BuiltInSettings.UPSTREAM_SEGMENT_HEADER.value());
                String ups = tmpArray[1].split("\n")[0];

                for (String up : ups.split(",")) {
                    result.add(Integer.parseInt(up.trim()));
                }

            }
            return result;
        }

        public static List<Integer> getDownSegments(String description) {
            List<Integer> result = new ArrayList<Integer>();

            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.DOWNSTREAM_SEGMENT_HEADER.value()) >= 0) {
                String[] tmpArray = copy.split(BuiltInSettings.DOWNSTREAM_SEGMENT_HEADER.value());
                String downs = tmpArray[1].split("\n")[0];

                for (String down : downs.split(",")) {
                    result.add(Integer.parseInt(down.trim()));
                }

            }
            return result;
        }

        public static List<LaneInfo> getLanes(String description) {
            List<LaneInfo> result = new ArrayList<LaneInfo>();

            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.LANE_HEADER.value()) >= 0) {
                String[] tmpArray = copy.split(BuiltInSettings.LANE_HEADER.value());
                String lanes = tmpArray[1].split("\n")[0];

                if (!lanes.startsWith(BuiltInSettings.LANE_TRANSITION_NOT_ALLOWED.value())) {
                    lanes = BuiltInSettings.LANE_TRANSITION_NOT_ALLOWED.value() + lanes;
                }
                if (!lanes.endsWith(BuiltInSettings.LANE_TRANSITION_NOT_ALLOWED.value())) {
                    lanes = lanes + BuiltInSettings.LANE_TRANSITION_NOT_ALLOWED.value();
                }


                for (int i =0; i<lanes.length(); i+=3) {
                    String leftTransition = lanes.substring(i, i+1);
                    String laneType = lanes.substring(i+1, i+2);
                    String rightTransition = lanes.substring(i+2, i+3);

                    result.add(new LaneInfo(LaneTransition.forType(leftTransition),LaneType.forType(laneType), LaneTransition.forType(rightTransition)));
                }
            }
            return result;
        }

        public static List<Integer> getDestinations(String description) {
            List<Integer> result = new ArrayList<Integer>();
            int numberLanes = getLanes(description).size();
            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.DESTINATION_HEADER.value()) >= 0) {
                for (int i=0; i<numberLanes; i++) {
                    result.add(new Integer(DESTINATION_COUNTER));
                }
                DESTINATION_COUNTER++;
            }
            return result;
        }


        public static List<String> getDetectors(String description) {
            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.DETECTOR_HEADER.value()) >=0 ) {
                String[] tmpArray = copy.split(BuiltInSettings.DETECTOR_HEADER.value());
                String detectorsDescription = tmpArray[1].split("\n")[0];
                String[] detectors = detectorsDescription.split(",");
                return Arrays.asList(detectors);
            }
            return new ArrayList<String>();
        }

        private static boolean isEquationFormattedDemand(String description) {
            if (description.indexOf("+") >= 0 || description.indexOf("-") >= 0) {
                return true;
            }
            return false;
        }

        private static DetectorDataUtilities.DynamicDemandInfo getDemandEquation(String equation) {
                String[] parts = equation.split("-");
                String addsEq = parts[0];
                String subtractsEq = parts[1];
                List<String> adds = Lists.newArrayList(addsEq.split("\\+"));
                List<String> subtracts = Lists.newArrayList(subtractsEq.split("\\+"));
                return new DetectorDataUtilities.DynamicDemandInfo(adds, subtracts, true);
        }

        public static List<DetectorDataUtilities.DynamicDemandInfo> getOriginDemands(String description) {
            String copy = description.toLowerCase();
            if (copy.indexOf(BuiltInSettings.ORIGIN_HEADER.value()) >=0 ) {
                String[] tmpArray = copy.split(BuiltInSettings.ORIGIN_HEADER.value());
                String originsDescription = tmpArray[1].split("\n")[0];
                if (isEquationFormattedDemand(originsDescription)) {
                    return Lists.newArrayList(getDemandEquation(originsDescription));
                }
                else {
                    ArrayList<DetectorDataUtilities.DynamicDemandInfo> list = new ArrayList<DynamicDemandInfo>();
                    String[] demands = originsDescription.split(",");
                    for (String demand: demands) {
                        list.add(new DetectorDataUtilities.DynamicDemandInfo(Lists.newArrayList(demand), null, false));
                    }
                    return list;
                }

            }
            return new ArrayList<DetectorDataUtilities.DynamicDemandInfo>();
        }

    }
}
