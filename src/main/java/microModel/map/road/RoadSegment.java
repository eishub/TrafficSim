package microModel.map.road;

import com.google.common.collect.Range;
import com.vividsolutions.jts.geom.Coordinate;

import microModel.core.road.LaneType;
import microModel.core.road.device.jDetector;
import microModel.core.road.jLane;
import microModel.core.traffic.AbstractDynamicDemandGenerator;
import microModel.jModel;
import microModel.map.CoordinateUtilities;
import microModel.map.DetectorUtilities;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import microModel.util.DetectorDataUtilities;
import microModel.util.DetectorDataUtilities.DynamicDemandInfo;
import microModel.util.DetectorInfoUtilities;
import microModel.util.TableData;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class RoadSegment {

    private final Logger logger = Logger.getLogger(RoadSegment.class);
    private static TableData<String> DETECTOR_INFO = null;
    private static TableData<Long> DETECTOR_DATA = null;
    public static int GLOBAL_LANE_COUNTER = 0;
    private final Coordinate coordinateOrigin;
    private final Integer id;
    private double speedLimit;
    private List<Integer> ups;
    private List<Integer> downs;
    private List<Coordinate> coordinates;
    /**
     * List containing the actual model Lanes ... There is an
     * ordered 1-to-1 relation between the elements in this list
     * and elements in the the {@link #laneInformationList} field.
     */
    private List<jLane> lanes;
    private List<String> detectorIDs;
    private List<DetectorDataUtilities.DynamicDemandInfo> originDemands;
    private List<Integer> destinations;

    /**
     * Lane information for the road segment ordered from the left most lane
     * to the right most lane. This list can be used to iterate over the actual
     * lanes in left to right order given the {@link #lanes} field.
     */
    private List<LaneInfo> laneInformationList;
    private Iterator<jLane> consumingLaneIterator = null;

    private RoadSegment(Integer id,
                        Coordinate coordinateOrigin,
                        double speedLimit,
                        List<Integer> upstreamSegmenets,
                        List<Integer> downStreamSegmenets,
                        List<Coordinate> coordinates,
                        List<LaneInfo> laneInfoList,
                        List<String> detectorIDs,
                        List<DetectorDataUtilities.DynamicDemandInfo> originDemands,
                        List<Integer> destinations) {

        this.id = id;
        this.coordinateOrigin = coordinateOrigin;
        this.speedLimit = speedLimit;
        this.ups = upstreamSegmenets;
        this.downs = downStreamSegmenets;
        this.coordinates = coordinates;

        this.laneInformationList = laneInfoList;
        this.detectorIDs = detectorIDs;
        this.originDemands = originDemands;
        this.destinations = destinations;


        Iterator<Integer> destinationIterator = this.destinations.iterator();
        lanes = new ArrayList<jLane>();
        int laneCounter = 0;
        LaneType type = null;
        for (LaneInfo info : laneInfoList) {

//            List<Coordinate> laneCoordinates = getLaneCoordinates(laneCounter++);
            List<Coordinate> laneCoordinates;
            if (type == null) {
                laneCoordinates = getLaneCoordinates2(laneCounter++, info.getLaneType());
                type = info.getLaneType().isTaper() ? info.getLaneType(): null;
            }
            else {
                laneCoordinates = getLaneCoordinates2(laneCounter++, type);
            }

            jLane lane = new jLane.Builder()
                    .withType(info.getLaneType())
                    .withID(GLOBAL_LANE_COUNTER++)
                    .withX(CoordinateUtilities.xValues(laneCoordinates))
                    .withY(CoordinateUtilities.yValues(laneCoordinates))
                    .build();
            initLane(lane, info);
            lane.setvLim(speedLimit);
            if (destinationIterator.hasNext()) {
                Integer destination = destinationIterator.next();
                lane.setDestination(destination);
            }
            lanes.add(lane);
        }
    }

    /**
     * Uses Regio-Lab detector info to setup the detectors on the lanes.
     */
    public void setupDetectors() {
        if (DETECTOR_INFO == null) {
            String path = jSettings.getInstance().get(BuiltInSettings.DETECTOR_INFO_FILE_PATH);
            try {
                DETECTOR_INFO = DetectorInfoUtilities.readDetectorInfoData(path);
            } catch (IOException e) {
                logger.error("Could not read Detector Info file @ " + path, e);
            }
        }

        Iterator<String> dIter = this.detectorIDs.iterator();
        Iterator<jLane> lIter = lanes.iterator();
        while (dIter.hasNext()) {
            jLane lane = lIter.next();
            String ID = dIter.next();
            if ("0".compareTo(ID) != 0 ) {
                List<String> row = DETECTOR_INFO.filter(DetectorInfoUtilities.ID_COLUMN, Range.singleton(ID)).getRow(0);
                double lat = Double.parseDouble(row.get(DetectorInfoUtilities.LATITUDE_COLUMN));
                double lon = Double.parseDouble(row.get(DetectorInfoUtilities.LONGITUDE_COLUMN));
                Coordinate dc = new Coordinate(lat,lon,0);
                dc = CoordinateUtilities.convertSphericalCoordinateToCartesian(dc, this.coordinateOrigin);
                dc = CoordinateUtilities.mirror(dc);
                double detectorPosistion = DetectorUtilities.detectorPositionOnLane(lane.getCoordinates(), dc);
                lane.addObserver(new jDetector(lane, detectorPosistion, 60, Integer.parseInt(ID)));
            }
            if (!lIter.hasNext()) {
                lIter = lanes.iterator();
            }
        }
    }

    /**
     * Uses the KML input file and Regio-Lab detector data files to setup dynamic demand generators.
     */
    public void setupGenerators(AbstractDynamicDemandGenerator.Builder generatorBuilder) {
        if(DETECTOR_DATA == null) {
            String path = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_FILE_PATH);
            try {
                DETECTOR_DATA = DetectorDataUtilities.readDetectorData(path);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Iterator<DetectorDataUtilities.DynamicDemandInfo> oIter = this.originDemands.iterator();
        Iterator<jLane> lIter = lanes.iterator();
        while(oIter.hasNext()) {
            jLane lane = lIter.next();
            DetectorDataUtilities.DynamicDemandInfo demand = oIter.next();
            if (demand.isEquationFormat()) {
                //Manipulate DETECTOR_DATA to fit equation and create appropriate generator
                // Do the adds first

                generatorBuilder.setLane(lane);
                TableData<Long> detectorData = calculateDemand(demand);
                generatorBuilder.setHeadwayDistribution(AbstractDynamicDemandGenerator.Distribution.UNIFORM);
                generatorBuilder.setDemandData(detectorData);
                generatorBuilder.build();
            }
            else {
                generatorBuilder.setLane(lane);
                generatorBuilder.setHeadwayDistribution(AbstractDynamicDemandGenerator.Distribution.UNIFORM);
                Long detectorID = Long.parseLong(demand.getAdds().get(0));
                TableData<Long> detectorData = DETECTOR_DATA.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(detectorID));
                generatorBuilder.setDemandData(detectorData);
                generatorBuilder.build();
            }
        }
    }

    /**
     * Calculates the positive dynamic demand to be used for a generator based on an equation format parsed from the KML input file.
     * @param demand The Dynamic demand equation parsed from input kml.
     * @return The table containing the information for the dynamic demand of the generator.
     */
    private TableData<Long> calculateDemand(DetectorDataUtilities.DynamicDemandInfo demand) {
        Iterator<String> adds = demand.getAdds().iterator();
        String detectorID = adds.next();
        long ID = Long.parseLong(detectorID);
        TableData<Long> filter = DETECTOR_DATA.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(ID));
        List<Long> totalDemand = filter.getColumn(DetectorDataUtilities.DEMAND_COLUMN);
        while (adds.hasNext()) {
            detectorID = adds.next();
            ID = Long.parseLong(detectorID);
            filter = DETECTOR_DATA.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(ID));
            List<Long> toAdd = filter.getColumn(DetectorDataUtilities.DEMAND_COLUMN);
            totalDemand = TableData.add(totalDemand, toAdd);
        }

        //Do the subs
        Iterator<String> subs = demand.getSubtracts().iterator();
        detectorID = subs.next();
        ID = Long.parseLong(detectorID);
        filter = DETECTOR_DATA.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(ID));
        List<Long> toSub = filter.getColumn(DetectorDataUtilities.DEMAND_COLUMN);
        totalDemand = TableData.subtract(totalDemand, toSub);
        while (subs.hasNext()) {
            detectorID = subs.next();
            ID = Long.parseLong(detectorID);
            filter = DETECTOR_DATA.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(ID));
            toSub = filter.getColumn(DetectorDataUtilities.DEMAND_COLUMN);
            totalDemand = TableData.subtract(totalDemand, toSub);
        }

        List<Long> totalPositiveDemand = DetectorDataUtilities.positiveDemand(totalDemand);
        Long[] totalPositiveDemandArray = new Long[totalPositiveDemand.size()];
        totalPositiveDemand.toArray(totalPositiveDemandArray);
        TableData<Long> detectorData = filter;
        detectorData.setColumn(DetectorDataUtilities.DEMAND_COLUMN, totalPositiveDemandArray);
        return detectorData;
    }


    /**
     * Returns an Iterator that iterates over the lanes of this RoadSegment instance.
     * However, subsequent calls to this method will return the same iterator so that
     * next() calls will continue iteration from the point where the last iteration stopped.
     * CAUTION: Using this is iterator is NOT thread safe.
     * @return Iterator over lanes.
     */
    private Iterator<jLane> getConsumingLaneIterator() {
        if(consumingLaneIterator == null) {
            consumingLaneIterator = lanes.iterator();
        }
        else if (!consumingLaneIterator.hasNext()) {
            consumingLaneIterator = lanes.iterator();
        }
        return consumingLaneIterator;
    }

    public void init(Map<Integer, RoadSegment> network) {
        connectLanes(network);
    }

    /**
     * Returns the lanes in the order from left to right (the order in which they were inserted).
     * @return List of jLane objects.
     */
    public List<jLane> getLanes() {
        return lanes;
    }

    private List<Coordinate> getLaneCoordinates(int lane) {
        double distance = -3.75;
        List<Coordinate> offset = CoordinateUtilities.parallelOffset(coordinates, distance * lane);
        return offset;
    }

    private List<Coordinate> getLaneCoordinates2(int lane, LaneType type) {
        double distance = 3.5;
        List<Coordinate> offset = CoordinateUtilities.parallelOffset2(coordinates, -1.75 + lane * distance, type);
        return offset;
    }

    private void connectLanes(Map<Integer, RoadSegment> network) {
        //Connect segment laneInformationList laterally (left, right)
        Iterator<jLane> laneIterator = lanes.iterator();
        jLane currentLane = null;
        jLane previousLane = null;
        do{
            currentLane = laneIterator.next();
            if (previousLane != null) {
                previousLane.connectLat(currentLane);
            }
            previousLane = currentLane;

        } while(laneIterator.hasNext());

        //Connect laneInformationList longitudinally (upstream, downstream)
        laneIterator = lanes.iterator();
        for (Integer downstreamSegmentId : downs) {
            RoadSegment downstreamSegment = network.get(downstreamSegmentId);
            Iterator<jLane> downstreamLaneIterator = downstreamSegment.getConsumingLaneIterator();
            do {
                jLane downStreamLane = downstreamLaneIterator.next();
                while (!downStreamLane.getType().connectsToUpStreamLane() && (downstreamLaneIterator.hasNext()))
                    downStreamLane = downstreamLaneIterator.next();

                if (laneIterator.hasNext()) {
                    currentLane = laneIterator.next();
                    while (!currentLane.getType().connectsToDownStreamLane() && (laneIterator.hasNext()))
                        currentLane = laneIterator.next();
                    downStreamLane.connectLong(currentLane);
                }

            } while (laneIterator.hasNext() && downstreamLaneIterator.hasNext());
        }
    }

    private void initLane(jLane currentLane, LaneInfo laneInfo) {
        Boolean leftTransitionAllowed = laneInfo.getLeftTransition().allowed();
        Boolean rightTransitionAllowed = laneInfo.getRightTransition().allowed();
        currentLane.setGoLeft(leftTransitionAllowed);
        currentLane.setGoRight(rightTransitionAllowed);
        if (laneInfo.getLaneType().isTaper()) {
            currentLane.setTaper(currentLane);
        }
    }

    public static class Builder {
        private Integer id;
        private double speedLimit;
        private List<Integer> upSegmenets = new ArrayList<Integer>();
        private List<Integer> downSegmenets = new ArrayList<Integer>();
        private List<Coordinate> coordinates = new ArrayList<Coordinate>();
        private List<LaneInfo> laneInfoList = new ArrayList<LaneInfo>();
        private List<String> detectorIDs = new ArrayList<String>();
        private List<Integer> destinations = new ArrayList<Integer>();
        private Coordinate coordinateOrigin;
        private List<DetectorDataUtilities.DynamicDemandInfo> originDemands = new ArrayList<DynamicDemandInfo>();

        public Builder(Integer id) {
            this.id = id;
        }

        public Builder setSpeedLimit(double speedLimit) {
            this.speedLimit = speedLimit;
            return this;
        }

        public Builder addUpSegment(Integer segmentId) {
            upSegmenets.add(segmentId);
            return this;
        }

        public Builder addDownSegment(Integer segmentId) {
            downSegmenets.add(segmentId);
            return this;
        }

        public Builder addLaneInformation(List<LaneInfo> laneInfoList) {
            this.laneInfoList.addAll(laneInfoList);
            return this;
        }

        public Builder setCoordinates(List<Coordinate> coordinates) {
            this.coordinates  = coordinates;
            return this;
        }

        public Builder convertSphericalCoordinatesToCartesian(Coordinate origin) {
            this.coordinateOrigin = origin;
            this.coordinates = CoordinateUtilities.convertSphericalCoordinatesToCartesian(coordinates, this.coordinateOrigin);
            return this;
        }

        public Builder mirrorCoordinates() {
            this.coordinates = CoordinateUtilities.mirror(coordinates);
            return this;
        }

        public Builder setDetectorIDs(List<String> ids) {
            this.detectorIDs = ids;
            return this;
        }
        public Builder setDestinations(List<Integer> destinations) {
            this.destinations = destinations;
            return this;
        }

        public Builder setOriginDemands(List<DetectorDataUtilities.DynamicDemandInfo> originDemands) {
            this.originDemands = originDemands;
            return this;
        }

        public RoadSegment build() {
            RoadSegment roadSegment = new RoadSegment(id,
                                                      coordinateOrigin,
                                                      speedLimit,
                                                      upSegmenets,
                                                      downSegmenets,
                                                      coordinates,
                                                      laneInfoList,
                                                      detectorIDs,
                                                      originDemands,
                                                      destinations);
            return roadSegment;
        }
    }
}
