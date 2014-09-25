package microModel.util;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DetectorDataUtilities {

    private static final Logger logger = Logger.getLogger(DetectorDataUtilities.class);
    /**
     * Detector data format:
     * An example line looks like this:
     * 3189,2009-05-13 15:00:00,1,20,97
     *
     * Each line consists of 5 comma-separated fields:
     *
     * 1	Detector code
     * 2	Time stamp (YYYY-MM-DD HH:MM:SS)
     * 3	Lane number (lanes are counted from 1; left-most lane in the road way)
     * 4	Number of vehicles counted in the measurement interval
     * 5	Mean speed of the detected vehicles (regretfully this is not the harmonic mean speed) in km/h
     */
    public static final String DELIMITER = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_DELIMITER);
    public static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat(jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_TIMESTAMP_FORMAT));
    public static final int ID_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_ID_COLUMN_INDEX);
    public static final int TIMESTAMP_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_TIME_COLUMN_INDEX);
    public static final int LANE_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_LANE_COLUMN_INDEX);
    public static final int DEMAND_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_DEMAND_COLUMN_INDEX);
    public static final int SPEED_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_DATA_SPEED_COLUMN_INDEX);

    /**
     * Reads a detector measurement data file.
     * @param path the path to the file
     * @return A Table of Long valued data with the described columns.
     * @throws ParseException
     * @throws IOException
     */
    public final static TableData<Long> readDetectorData(String path) throws ParseException, IOException {
        //cache parsing results for faster perfomance.
        Map<String, Long> cache = new TreeMap<String, Long>();
        TableData<Long> data = new TableData<Long>();
        URL url = Resources.getResource(DetectorDataUtilities.class, path);
        List<String> lines = Resources.readLines(url, Charset.defaultCharset());

        long start = System.nanoTime();

        for (String line : lines) {
            String[] columns = line.split(DELIMITER);
            Long[] row = new Long[5];

            if (cache.containsKey(columns[ID_COLUMN])) {
                row[ID_COLUMN] = cache.get(columns[ID_COLUMN]);
            }
            else {
                row[ID_COLUMN] = Long.parseLong(columns[ID_COLUMN]);
                cache.put(columns[ID_COLUMN],row[ID_COLUMN]);
            }

            if (cache.containsKey(columns[TIMESTAMP_COLUMN])) {
                row[TIMESTAMP_COLUMN] = cache.get(columns[TIMESTAMP_COLUMN]);
            }
            else {
                row[TIMESTAMP_COLUMN] = TIMESTAMP.parse(columns[TIMESTAMP_COLUMN]).getTime()/1000;
                cache.put(columns[TIMESTAMP_COLUMN], row[TIMESTAMP_COLUMN]);
            }

            if (cache.containsKey(columns[LANE_COLUMN])) {
                row[LANE_COLUMN] = cache.get(columns[LANE_COLUMN]);
            }
            else {
                row[LANE_COLUMN] = Long.parseLong(columns[LANE_COLUMN]);
                cache.put(columns[LANE_COLUMN], row[LANE_COLUMN]);
            }

            if (cache.containsKey(columns[DEMAND_COLUMN])) {
                row[DEMAND_COLUMN] = cache.get(columns[DEMAND_COLUMN]);
            }
            else {
                row[DEMAND_COLUMN] = Long.parseLong(columns[DEMAND_COLUMN]);
                cache.put(columns[DEMAND_COLUMN], row[DEMAND_COLUMN]);
            }

            if (cache.containsKey(columns[SPEED_COLUMN])) {
                row[SPEED_COLUMN] = cache.get(columns[SPEED_COLUMN]);
            }
            else {
                row[SPEED_COLUMN] = Long.parseLong(columns[SPEED_COLUMN]);
                cache.put(columns[SPEED_COLUMN], row[SPEED_COLUMN]);
            }

            data.addRow(row);
        }
        long end = System.nanoTime();
        long duration = end - start;
        logger.debug("File Import Duration: " + duration);
        return data;
    }

    /**
     * Converts an array of demand values that may contain negative values to positive demand values
     * in such a way that the total demand remains the same.
     * @param demand The demand values
     * @return array of demand values that does not contain negative values.
     */
    public final static List<Long> positiveDemand(List<Long> demand) {
        Long[] pDemand = new Long[demand.size()];
        long total = 0;
        for (int i = 0; i < demand.size(); i++) {
            total += demand.get(i);
            if (total < 0 ) {
                pDemand[i] =  Long.valueOf(0);
            }
            else {
                pDemand[i] = total;
                total = 0;
            }
        }
        return Lists.newArrayList(pDemand);
    }

    public static class DynamicDemandInfo {
        private List<String> adds;
        private List<String> subtracts;
        private boolean isEquationFormat;

        public DynamicDemandInfo(List<String> adds, List<String> subtracts, boolean isEquationFormat) {
            this.adds = adds;
            this.subtracts = subtracts;
            this.isEquationFormat = isEquationFormat;
        }

        public List<String> getAdds() {
            return adds;
        }

        public List<String> getSubtracts() {
            return subtracts;
        }

        public boolean isEquationFormat() {
            return isEquationFormat;
        }
    }

}
