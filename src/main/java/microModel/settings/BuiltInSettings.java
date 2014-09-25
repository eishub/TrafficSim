package microModel.settings;

import microModel.core.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * A central place holder for all types of simulation settings.
 */
public final class BuiltInSettings {

    private BuiltInSettings() {/* Should not be instantiated */}

    /** A list containing of all adjustable simulation settings */
    public static final List<Parameter<?>> PARAMETERS = new ArrayList<Parameter<?>>();

    /** Simulation frame record format */
    public static final Parameter<String> SIMULATION_RECORD_FORMAT = new Parameter<String>("recordFormat", "PDF");
    static { PARAMETERS.add(SIMULATION_RECORD_FORMAT); }

    /** Simulation step size in seconds */
    public static final Parameter<Double> SIMULATION_STEP_SIZE = new Parameter<Double>("stepSize", 0.5);
    static { PARAMETERS.add(SIMULATION_STEP_SIZE); }

    /** Simulation length in seconds */
    public static final Parameter<Double> SIMULATION_DURATION = new Parameter<Double>("duration", 200.0);
    static { PARAMETERS.add(SIMULATION_DURATION); }

    /** Run Simulation in DEBUG mode. */
    public static final Parameter<Boolean> DEBUG = new Parameter<Boolean>("debug", true);
    static { PARAMETERS.add(DEBUG); }

    /** Keep track of internal car-following or agent actions */
    public static final Parameter<Boolean> DEBUG_MODEL = new Parameter<Boolean>("debugModel", false);
    static { PARAMETERS.add(DEBUG_MODEL); }

    /** Keep track of trajectory data for analysis */
    public static final Parameter<Boolean> DEBUG_TRAJECTORY = new Parameter<Boolean>("debugTrajectory", false);
    static { PARAMETERS.add(DEBUG_TRAJECTORY); }

    /** Determines whether the logs are output as serialized java objects or text files. */
    public static final Parameter<Boolean> DEBUG_TRAJECTORY_OUTPUT_SERIALIZED_OBJECTS = new Parameter<Boolean>("debugTrajectorySerialized", false);
    static { PARAMETERS.add(DEBUG_TRAJECTORY_OUTPUT_SERIALIZED_OBJECTS); }

    /** Trajectory sampling rate in seconds */
    public static final Parameter<Double> DEBUG_TRAJECTORY_SAMPLING_RATE = new Parameter<Double>("trajectoryPeriod", 0.5);
    static { PARAMETERS.add(DEBUG_TRAJECTORY_SAMPLING_RATE); }

    /** Number of Trajectories to keep in memory before saving to disk */
    public static final Parameter<Integer> DEBUG_TRAJECTORY_BUFFER = new Parameter<Integer>("trajectoryBuffer", 50);
    static { PARAMETERS.add(DEBUG_TRAJECTORY_BUFFER); }

    /** Keep track of detector data for analysis */
    public static final Parameter<Boolean> DEBUG_DETECTOR = new Parameter<Boolean>("debugDetector", true);
    static { PARAMETERS.add(DEBUG_DETECTOR); }

    public static final Parameter<Boolean> DEBUG_DETECTOR_OUTPUT_SERIALIZED_OBJETCS = new Parameter<Boolean>("debugDetectorSerialized", true);
    static { PARAMETERS.add(DEBUG_DETECTOR_OUTPUT_SERIALIZED_OBJETCS); }

    /** Detector data aggregation period in seconds. */
    public static final Parameter<Integer> DETECTOR_PERIOD = new Parameter<Integer>("detectorPeriod", 5);
    static { PARAMETERS.add(DETECTOR_PERIOD); }

    /** Simulation logging Output Path */
    public static final Parameter<String> OUTPUT_PATH = new Parameter<String>("outputDir", "/tmp/jSim/output");
    static { PARAMETERS.add(OUTPUT_PATH); }


    //KML input parsing modelParameters. These specify the format of the annotations used in the KML input file for additional information about the road network.
    public static final Parameter<String> LANE_TRANSITION_ALLOWED = new Parameter<String>("FORMAT_laneChangeAllowed", ":");
    static { PARAMETERS.add(LANE_TRANSITION_ALLOWED); }

    public static final Parameter<String> LANE_TRANSITION_NOT_ALLOWED = new Parameter<String>("FORMAT_laneChangeNotAllowed", "|");
    static { PARAMETERS.add(LANE_TRANSITION_NOT_ALLOWED); }

    public static final Parameter<String> NORMAL_LANE = new Parameter<String>("FORMAT_normalLane", "n");
    static { PARAMETERS.add(NORMAL_LANE); }

    public static final Parameter<String> ADDED_LANE = new Parameter<String>("FORMAT_addedLane", "a");
    static { PARAMETERS.add(ADDED_LANE); }

    public static final Parameter<String> SUBTRACTED_LANE = new Parameter<String>("FORMAT_subtractedLane", "s");
    static { PARAMETERS.add(SUBTRACTED_LANE); }

    public static final Parameter<String> EXTRA_LANE = new Parameter<String>("FORMAT_extraLane", "e");
    static { PARAMETERS.add(EXTRA_LANE); }

    public static final Parameter<String> MERGE_LANE = new Parameter<String>("FORMAT_mergeLane", "m");
    static { PARAMETERS.add(MERGE_LANE); }

    public static final Parameter<String> DIVERGE_LANE = new Parameter<String>("FORMAT_divergeLane", "d");
    static { PARAMETERS.add(DIVERGE_LANE); }

    public static final Parameter<String> SPEED_LIMIT_HEADER = new Parameter<String>("FORMAT_speedLimitHeader", "vlim:");
    static { PARAMETERS.add(SPEED_LIMIT_HEADER); }

    public static final Parameter<String> LANE_HEADER = new Parameter<String>("FORMAT_lanesHeader", "lanes:");
    static { PARAMETERS.add(LANE_HEADER); }

    public static final Parameter<String> DOWNSTREAM_SEGMENT_HEADER = new Parameter<String>("FORMAT_downstreamSegmentHeader", "down:");
    static { PARAMETERS.add(DOWNSTREAM_SEGMENT_HEADER); }

    public static final Parameter<String> UPSTREAM_SEGMENT_HEADER = new Parameter<String>("FORMAT_upstreamSegmentHeader", "up:");
    static { PARAMETERS.add(UPSTREAM_SEGMENT_HEADER); }

    public static final Parameter<String> DESTINATION_HEADER = new Parameter<String>("FORMAT_destinationSegmentHeader", "destination:");
    static { PARAMETERS.add(DESTINATION_HEADER); }

    public static final Parameter<String> DETECTOR_HEADER = new Parameter<String>("FORMAT_detectorSegmentHeader", "detectors:");
    static { PARAMETERS.add(DETECTOR_HEADER); }

    public static final Parameter<String> ORIGIN_HEADER = new Parameter<String>("FORMAT_originSegmentHeader", "origin:");
    static { PARAMETERS.add(ORIGIN_HEADER); }

    //Import Detector info (eg coordinates and locations) from KML and additional files
    public static final Parameter<Boolean> IMPORT_DETECTOR_FROM_KML = new Parameter<Boolean>("importDetectorFromKML", Boolean.FALSE);
    static { PARAMETERS.add(IMPORT_DETECTOR_FROM_KML); }

    public static final Parameter<String> DETECTOR_INFO_FILE_PATH = new Parameter<String>("detectorInfoFilePath", "/detector_data/detectors.info");
    static { PARAMETERS.add(DETECTOR_INFO_FILE_PATH); }


    //Import Origin data (eg generated demand values) from kml and additional files
    public static final Parameter<Boolean> IMPORT_ORIGIN_FROM_KML = new Parameter<Boolean>("importOriginFromKML", Boolean.FALSE);
    static { PARAMETERS.add(IMPORT_ORIGIN_FROM_KML); }

    public static final Parameter<String> DETECTOR_DATA_FILE_PATH = new Parameter<String>("detectorDataFilePath", "/detector_data/detectors.data");
    static { PARAMETERS.add(DETECTOR_DATA_FILE_PATH); }

    //Detector data format
    public static final Parameter<String> DETECTOR_DATA_DELIMITER = new Parameter<String>("detectorDataDelimiter", ",");
    static { PARAMETERS.add(DETECTOR_DATA_DELIMITER); }

    public static final Parameter<String> DETECTOR_DATA_TIMESTAMP_FORMAT = new Parameter<String>("detectorDataDelimiter", "yyyy-MM-dd HH:mm:ss");
    static { PARAMETERS.add(DETECTOR_DATA_TIMESTAMP_FORMAT); }

    public static final Parameter<Integer> DETECTOR_DATA_ID_COLUMN_INDEX = new Parameter<Integer>("detectorDataIDColumnIndex", 0);
    static { PARAMETERS.add(DETECTOR_DATA_ID_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_DATA_TIME_COLUMN_INDEX = new Parameter<Integer>("detectorDataTimeColumnIndex", 1);
    static { PARAMETERS.add(DETECTOR_DATA_TIME_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_DATA_LANE_COLUMN_INDEX = new Parameter<Integer>("detectorDataLaneColumnIndex", 2);
    static { PARAMETERS.add(DETECTOR_DATA_LANE_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_DATA_DEMAND_COLUMN_INDEX = new Parameter<Integer>("detectorDataDemandColumnIndex", 3);
    static { PARAMETERS.add(DETECTOR_DATA_DEMAND_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_DATA_SPEED_COLUMN_INDEX = new Parameter<Integer>("detectorDataSpeedColumnIndex", 4);
    static { PARAMETERS.add(DETECTOR_DATA_SPEED_COLUMN_INDEX); }

    //Detector info format
    public static final Parameter<String> DETECTOR_INFO_DELIMITER = new Parameter<String>("detectorInfoDelimiter", ",");
    static { PARAMETERS.add(DETECTOR_INFO_DELIMITER); }

    public static final Parameter<Integer> DETECTOR_INFO_ID_COLUMN_INDEX = new Parameter<Integer>("detectorInfoIDColumnIndex", 0);
    static { PARAMETERS.add(DETECTOR_INFO_ID_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_INFO_LATITUDE_COLUMN_INDEX = new Parameter<Integer>("detectorInfoLatitudeColumnIndex", 1);
    static { PARAMETERS.add(DETECTOR_INFO_LATITUDE_COLUMN_INDEX); }

    public static final Parameter<Integer> DETECTOR_INFO_LONGITUDE_COLUMN_INDEX = new Parameter<Integer>("detectorInfoLongitudeColumnIndex", 2);
    static { PARAMETERS.add(DETECTOR_INFO_LONGITUDE_COLUMN_INDEX); }

}
