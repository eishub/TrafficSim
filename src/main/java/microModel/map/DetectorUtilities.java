package microModel.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.*;

import java.util.List;

public class DetectorUtilities {

    /**
     * This class has been copied directly from the source code of the following class found
     * on the internet.
     * com.vividsolutions.jump.algorithm.LengthToPoint
     */
    private static final class LengthToPoint
    {
        public static double lengthAlongSegment(LineSegment seg, Coordinate pt)
        {
            double projFactor = seg.projectionFactor(pt);
            double len = 0.0;
            if (projFactor <= 0.0)
                len = 0.0;
            else if (projFactor <= 1.0)
                len = projFactor * seg.getLength();
            else
                len = seg.getLength();
            return len;
        }

        /**
         * Computes the length along a LineString to the point on the line nearest a given point.
         */
        public static double length(LineString line, Coordinate inputPt)
        {
            LengthToPoint lp = new LengthToPoint(line, inputPt);
            return lp.getLength();
        }

        private double minDistanceToPoint;
        private double locationLength;

        public LengthToPoint(LineString line, Coordinate inputPt)
        {
            computeLength(line, inputPt);
        }

        public double getLength()
        {
            return locationLength;
        }

        private void computeLength(LineString line, Coordinate inputPt)
        {
            minDistanceToPoint = Double.MAX_VALUE;
            double baseLocationDistance = 0.0;
            Coordinate[] pts = line.getCoordinates();
            LineSegment seg = new LineSegment();
            for (int i = 0; i < pts.length - 1; i++) {
                seg.p0 = pts[i];
                seg.p1 = pts[i + 1];
                updateLength(seg, inputPt, baseLocationDistance);
                baseLocationDistance += seg.getLength();

            }
        }

        private void updateLength(LineSegment seg, Coordinate inputPt, double segStartLocationDistance)
        {
            double dist = seg.distance(inputPt);
            if (dist > minDistanceToPoint) return;
            minDistanceToPoint = dist;
            // found new minimum, so compute location distance of point
            double projFactor = seg.projectionFactor(inputPt);
            if (projFactor <= 0.0)
                locationLength = segStartLocationDistance;
            else if (projFactor <= 1.0)
                locationLength = segStartLocationDistance + projFactor * seg.getLength();
            else
                locationLength = segStartLocationDistance + seg.getLength();
        }
    }

    public static double detectorPositionOnLane(List<Coordinate> laneCoordinates, Coordinate detectorCoordinate) {
        Coordinate[] cArray = new Coordinate[laneCoordinates.size()];
        laneCoordinates.toArray(cArray);
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
        LineString line = gf.createLineString(cArray);
        LengthToPoint lengthToPoint = new LengthToPoint(line, detectorCoordinate);

        return lengthToPoint.getLength();
    }


}
