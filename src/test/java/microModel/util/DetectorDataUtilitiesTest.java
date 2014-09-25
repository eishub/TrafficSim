package microModel.util;

import com.google.common.collect.Range;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class DetectorDataUtilitiesTest {
//    private String dataFile = "/detector_data/example.data";
    private String dataFile = "/detector_data/detectors.data";
    @Test
    public void parse() {
        try {
            TableData<Long> data = DetectorDataUtilities.readDetectorData(dataFile);
            TableData filtered = data.filter(DetectorDataUtilities.ID_COLUMN, Range.singleton(3189L));
            List<Long> row = filtered.getRow(0);
            //Check if first row of the data is the same as the example.data file in resources.
            Assert.assertEquals(3189L, row.get(DetectorDataUtilities.ID_COLUMN).longValue());
            Assert.assertEquals(0, new Date(row.get(DetectorDataUtilities.TIMESTAMP_COLUMN)*1000).compareTo(DetectorDataUtilities.TIMESTAMP.parse("2009-05-13 15:00:00")));
            Assert.assertEquals(1, row.get(DetectorDataUtilities.LANE_COLUMN).longValue());
            Assert.assertEquals(20, row.get(DetectorDataUtilities.DEMAND_COLUMN).longValue());
            Assert.assertEquals(97, row.get(DetectorDataUtilities.SPEED_COLUMN).longValue());

        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
