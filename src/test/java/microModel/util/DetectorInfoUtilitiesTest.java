package microModel.util;

import com.google.common.collect.Range;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class DetectorInfoUtilitiesTest {
    private String dataFile = "/detector_data/detectors.info";

    @Test
    public void parse() {
        try {
            TableData<String> data = DetectorInfoUtilities.readDetectorInfoData(dataFile);
            TableData<String> filtered = data.filter(DetectorInfoUtilities.ID_COLUMN, Range.singleton("3900"));
            List<String> row = filtered.getRow(0);
            Assert.assertEquals("3900", row.get(DetectorInfoUtilities.ID_COLUMN));
            Assert.assertEquals("4.588464", row.get(DetectorInfoUtilities.LATITUDE_COLUMN));
            Assert.assertEquals("51.966842", row.get(DetectorInfoUtilities.LONGITUDE_COLUMN));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
