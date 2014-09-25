package microModel.util;

import com.google.common.io.Resources;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public class DetectorInfoUtilities {
    /**
     * Detector info format:
     * An example line looks like this:
     * 3901,20,L,40200,2,(4.588464,51.966842),Lane
     *
     * Each line consists of several comma separated values. We only care about the ID, and the (LAT, Long) values.
     *
     * 1	ID
     * 2	_dc_
     * 3	_dc_
     * 4	_dc_
     * 5	_dc_
     * 6    (LATITUDE
     * 7    LONGITUDE)
     * 8    _dc_
     *
     * _dc_ = Don't care.
     */

    public static final String DELIMITER = jSettings.getInstance().get(BuiltInSettings.DETECTOR_INFO_DELIMITER);
    public static final int ID_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_INFO_ID_COLUMN_INDEX);
    public static final int LATITUDE_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_INFO_LATITUDE_COLUMN_INDEX);
    public static final int LONGITUDE_COLUMN = jSettings.getInstance().get(BuiltInSettings.DETECTOR_INFO_LONGITUDE_COLUMN_INDEX);

    private static final int ID_COLUMN_INDEX_IN_FORMAT = 0;
    private static final int LATITUDE_COLUMN_INDEX_IN_FORMAT = 5;
    private static final int LONGITUDE_COLUMN_INDEX_IN_FORMAT = 6;


    public static final TableData<String> readDetectorInfoData(String path) throws IOException {
        TableData<String> data = new TableData<String>();
        URL url = Resources.getResource(DetectorDataUtilities.class, path);
        List<String> lines = Resources.readLines(url, Charset.defaultCharset());
        for (String line : lines) {
            String[] columns = line.split(DELIMITER);
            String[] row = new String[3];
            row[ID_COLUMN] = columns[ID_COLUMN_INDEX_IN_FORMAT];
            row[LATITUDE_COLUMN] = columns[LATITUDE_COLUMN_INDEX_IN_FORMAT].replaceAll("[\\(|\\)]","");
            row[LONGITUDE_COLUMN] = columns[LONGITUDE_COLUMN_INDEX_IN_FORMAT].replaceAll("[\\(|\\)]", "");

            data.addRow(row);
        }
        return data;
    }
}
