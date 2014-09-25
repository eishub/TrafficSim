package microModel.map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class KMLUtilities {

    public static List<SimpleFeature> getPlacemarks(InputStream kml) {
        List<SimpleFeature> result = new ArrayList<SimpleFeature>();

        Parser kmlParser = new Parser(new KMLConfiguration());
        try {
            SimpleFeature document = (SimpleFeature) kmlParser.parse(kml);
            Collection<SimpleFeature> folders = (Collection<SimpleFeature>) document.getAttribute(KML.Feature.getLocalPart());
            for(SimpleFeature folder: folders) {
                Collection<SimpleFeature> placemarks = (Collection<SimpleFeature>) folder.getAttribute(KML.Feature.getLocalPart());
                result.addAll(placemarks);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static LineString getLineString(Geometry g) {
        if (g instanceof LineString)
            return (LineString) g;
        return null;
    }
}
