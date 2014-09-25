package GeoTools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Parser;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class GeoToolsTest {
    private InputStream inputStream;

    @Before
    public void init() {
        inputStream = getClass().getResourceAsStream("/A16.kml");
    }

    @Test
    public void testGeoTools() {
        Parser parser = new Parser(new KMLConfiguration());
        SimpleFeature document = null;
        try {
            document = (SimpleFeature) parser.parse(inputStream);
            Collection<SimpleFeature> folders = (Collection<SimpleFeature>) document.getAttribute(KML.Feature.getLocalPart());
            for (SimpleFeature folder: folders) {
                Collection<SimpleFeature> placemarks = (Collection<SimpleFeature>) folder.getAttribute(KML.Feature.getLocalPart());
                for (SimpleFeature placemark: placemarks) {
                    LineString lineString = (LineString) placemark.getDefaultGeometry();
                    String description = (String) placemark.getAttribute("description");
                    OffsetCurveBuilder ocb = new OffsetCurveBuilder(new PrecisionModel(PrecisionModel.FLOATING), new BufferParameters());
                    Coordinate[] offsetCurve = ocb.getOffsetCurve(lineString.getCoordinates(), 3.0);
                    LineString offsetLineString = new LineString(new CoordinateArraySequence(offsetCurve),new GeometryFactory());

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
}
