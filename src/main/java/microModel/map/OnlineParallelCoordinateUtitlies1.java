package microModel.map;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

import java.util.ArrayList;
import java.util.List;

public class OnlineParallelCoordinateUtitlies1 {

    /**
     * Found this online here: http://rsiaf.googlecode.com/svn/rs-gis-core/trunk/src/main/java/com/revolsys/gis/jts/JtsGeometryUtil.java
     *
     */
    public static LineString createParallelLineString(final LineString line,
                                                      final int orientation, final double distance) {
        GeometryFactory factory = line.getFactory();
        CoordinateSequence coordinates = line.getCoordinateSequence();
        List<Coordinate> newCoordinates = new ArrayList<Coordinate>();
        Coordinate coordinate = coordinates.getCoordinate(0);
        LineSegment lastLineSegment = null;
        int coordinateCount = coordinates.size();
        for (int i = 0; i < coordinateCount; i++) {
            Coordinate nextCoordinate = null;
            LineSegment lineSegment = null;
            if (i < coordinateCount - 1) {
                nextCoordinate = coordinates.getCoordinate(i + 1);
                lineSegment = new LineSegment(coordinate, nextCoordinate);
                lineSegment = offset(lineSegment, distance, orientation);
            }
            if (lineSegment == null) {
                newCoordinates.add(lastLineSegment.p1);
            } else if (lastLineSegment == null) {
                newCoordinates.add(lineSegment.p0);
            } else {
                Coordinate intersection = lastLineSegment.intersection(lineSegment);
                if (intersection != null) {
                    newCoordinates.add(intersection);
                } else {
                    // newCoordinates.add(lastLineSegment.p1);
                    newCoordinates.add(lineSegment.p0);
                }
            }

            coordinate = nextCoordinate;
            lastLineSegment = lineSegment;
        }
        CoordinateSequence newCoords = PackedCoordinateSequenceFactory.DOUBLE_FACTORY.create(newCoordinates.toArray(new Coordinate[0]));
        return factory.createLineString(newCoords);
    }

    public static LineSegment offset(final LineSegment line,
                                     final double distance, final int orientation) {
        double angle = line.angle();
        if (orientation == Angle.CLOCKWISE) {
            angle -= Angle.PI_OVER_2;
        } else {
            angle += Angle.PI_OVER_2;
        }
        Coordinate c1 = offset(line.p0, angle, distance);
        Coordinate c2 = offset(line.p1, angle, distance);
        return new LineSegment(c1, c2);
    }

    public static Coordinate offset(final Coordinate coordinate,
                                    final double angle, final double distance) {
        double newX = coordinate.x + distance * Math.cos(angle);
        double newY = coordinate.y + distance * Math.sin(angle);
        Coordinate newCoordinate = new Coordinate(newX, newY);
        return newCoordinate;

    }

}
