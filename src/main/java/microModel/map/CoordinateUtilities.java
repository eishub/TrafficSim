package microModel.map;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import microModel.core.road.LaneType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: arman
 * Date: 8/27/12
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public final class CoordinateUtilities {

    /** Earth Radius at Equator in Meters     */
    private static final double EARTH_RADIUS_AT_EQUATOR = 6378137;
    /** Earth radius at poles in Meters */
    private  static final double EARTH_RADIUS_AT_POLES = 6356752.3;

    /**
     * Calculates the lateral distance in meters between two Coordinates.
     * @param p1 Coordinate 1
     * @param p2 Coordinate 2
     * @return Distance in meters.
     */
    private static double dLat(Coordinate p1, Coordinate p2) {
        double l = (p1.y + p2.y) / 2;
        double r = earthRadiusAtLongitude(l);
        r = sliceRadiusAtLongitude(l, r);
        double dAng = Math.abs(p1.x - p2.x);
        return 2 * Math.PI * r * dAng/360;
    }

    /**
     * Calculates the longitudinal distance in meters between two coordinates.
     * @param p1 Coordinate 1
     * @param p2 Coordinate 2
     * @return Distance in meters.
     */
    private static double dLong(Coordinate p1, Coordinate p2) {
        double l = (p1.y + p2.y) / 2;
        double r = earthRadiusAtLongitude(l);
        double dAng = Math.abs(p1.y - p2.y);
        return 2 * Math.PI * r * dAng/360;
    }


    /**
     * Walks through the list of coordinates and records the minimum values of longitude and latitude values
     * observed
     * @param list list of Coordinates to walk through
     * @return Coordinate with the minimum values
     */
    public static Coordinate minCoordinate(List<Coordinate> list) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        for (Coordinate c : list) {
            if (c.x < minX)
                minX = c.x;
            if (c.y < minY)
                minY = c.y;
        }
        return new Coordinate(minY, minX);

    }

    /**
     * Walks through the list of coordinates and records the maximum values of longitude and latitude values
     * observed
     * @param list list of Coordinates to walk through
     * @return Coordinate with the maximum values
     */
    public static Coordinate maxCoordinate(List<Coordinate> list) {
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Coordinate c: list) {
            if (c.x > maxX)
                maxX = c.x;
            if (c.y > maxY)
                maxY = c.y;
        }
        return new Coordinate(maxY, maxX);
    }


    /**
     * Converts the coordinates values relative to a point of origin.
     * @param list list of coordinates to convert to relative coordinates.
     * @param origin the origin point to use for the conversion.
     * @return a new list of coordinates relative the origin point.
     */
    public static List<Coordinate> relativeCoordinates(List<Coordinate> list, Coordinate origin) {
        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        for (Coordinate c: list) {
            double relativeX = dLat(c, origin);
            double relativeY = dLong(c, origin);
            coordinates.add(new Coordinate(relativeY, relativeX));
        }
        return coordinates;
    }

    /**
     * Extracts the x-values of a list of coordinates
     * @param list List of coordinates
     * @return Array of x values.
     */
    public static double[] xValues(List<Coordinate> list) {
        double[] xs = new double[list.size()];
        int counter = 0;
        for (Coordinate c: list) {
            xs[counter++] = c.y;
        }
        return xs;
    }

    /**
     * Extracts the y-values of a list of coordinates
     * @param list List of coordinates
     * @return Array of y values.
     */
    public static double[] yValues(List<Coordinate> list) {
        double[] ys = new double[list.size()];
        int counter = 0;
        for (Coordinate c: list) {
            ys[counter++] = c.x;
        }
        return ys;
    }

    /**
     * combined two arrays of x and y coordinates into a list of Coordinates
     * @param x
     * @param y
     * @return
     */
    public static List<Coordinate> combine(double[] x, double[] y) {
        if (x.length != y.length) {
            return null;
        }
        List<Coordinate> combined = new ArrayList<Coordinate>();
        for (int i=0; i<x.length; i++) {
            combined.add(new Coordinate(y[i], x[i]));
        }
        return combined;
    }

    /**
     * Calculates the radius of the Earth at a given longitude in meters.
     * @param longitude
     * @return Earth radius at given longitude in meters.
     */
    private static double earthRadiusAtLongitude(double longitude) {
        return Math.sqrt(
                (
                    Math.pow(Math.pow(EARTH_RADIUS_AT_EQUATOR,2) * Math.cos(longitude),2) +
                    Math.pow(Math.pow(EARTH_RADIUS_AT_POLES,2) * Math.sin(longitude),2)
                )
                /
                (
                    Math.pow(EARTH_RADIUS_AT_EQUATOR * Math.cos(longitude),2) +
                    Math.pow( EARTH_RADIUS_AT_POLES * Math.sin(longitude),2)
                )
        );
    }

    /**
     * Calculates Radius of horizontal slice at given longitude, for which the earth radius was calculated.
     * @param longitude
     * @param earthRadius
     * @return Slice radius in meters.
     */
    private static double sliceRadiusAtLongitude(double longitude, double earthRadius) {
        return earthRadius * Math.cos(longitude * 2 * Math.PI / 360);
    }

    public static Coordinate convertSphericalCoordinateToCartesian(Coordinate c, Coordinate origin) {
        if (c.equals3D(origin)) {
            return new Coordinate(0,0);
        }
        else {
            double dx = dLat(c, origin);
            double dy = dLong(c, origin);
            double r = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
            double ang = Math.atan(dy/dx) + -90 * Math.PI/180;
            double x = Math.cos(ang) * r;
            double y = Math.sin(ang) * r;
            return new Coordinate(x, y) ;
        }

    }

    public static List<Coordinate> convertSphericalCoordinatesToCartesian(List<Coordinate> coordinates, Coordinate origin) {
        List<Coordinate> cartesian = new ArrayList<Coordinate>();
        for (Coordinate c: coordinates) {
            cartesian.add(convertSphericalCoordinateToCartesian(c, origin));
        }
        return cartesian;
    }

    public static Coordinate mirror(Coordinate c) {
        return new Coordinate(c.y, c.x);
    }

    public static List<Coordinate> mirror(List<Coordinate> coordinates) {
        List<Coordinate> cartesian = new ArrayList<Coordinate>();
        for(Coordinate c: coordinates) {
            cartesian.add(mirror(c));
        }
        return cartesian;
    }


    /**
     * Creates a parallel curve to the given list of coordinates at given distance.
     * @param coordinates coordinates of the curve
     * @param distance  distance of the parallel curve to create from the original curve
     * @return List of coordinates for the parallel curve.
     */
    public static List<Coordinate> parallelOffset(List<Coordinate> coordinates, double distance) {
        /* TODO: this comes close to what I want but not entirely what I want.
            see: http://www.spatialdbadvisor.com/oracle_spatial_tips_tricks/224/configurable-buffer-jts-and-oracle
            for what it does. The problem is that it connects the two ends of the parallel line to the ends of
            the original line!
        */
        if (distance == 0) {
            return coordinates;
        }
        CoordinateArraySequence points = new CoordinateArraySequence(coordinates.toArray(new Coordinate[0]));
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
        LineString ls = new LineString(points, geometryFactory);
        BufferParameters bp = new BufferParameters();
        bp.setSingleSided(true);
        bp.setEndCapStyle(BufferParameters.CAP_FLAT);
        Geometry g = BufferOp.bufferOp(ls, distance, bp);
        List<Coordinate> parallelPolygon = Arrays.asList(g.getCoordinates());
        return parallelPolygon.subList(1, parallelPolygon.size()-1);

    }



    /**
     * This method duplicates what was used in the original jModel to calculate parallel line coordinates in the Matlab code.
     * @param coordinates
     * @param distance
     * @param type
     * @return
     */
    public static List<Coordinate> parallelOffset2(List<Coordinate> coordinates, double distance, LaneType type) {
        double[] x = CoordinateUtilities.xValues(coordinates);
        double[] y = CoordinateUtilities.yValues(coordinates);

        double laneLength = 0;
        if (type.isTaper()) {
            for(int i=0; i<coordinates.size()-1; i++) {
                double dx = x[i+1] - x[i];
                double dy = y[i+1] - y[i];
                laneLength += Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
            }
        }

        double[] xOut = new double[coordinates.size()];
        double[] yOut = new double[coordinates.size()];

        double f = (type.equals(LaneType.DIVERGE)) ? -3.5 : 0;

        Coordinate c = moveRightAndIntersect(distance + f, new double[]{x[0] - (x[1] - x[0]), x[0], x[1]}, new double[]{y[0] - (y[1] - y[0]), y[0], y[1]});
        xOut[0] = c.x;
        yOut[0] = c.y;
        double dx = x[1] - x[0];
        double dy = y[1] - y[0];
        double xCumul = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));

        for (int i=1; i<coordinates.size()-1; i++) {
            if (type.equals(LaneType.MERGE)) {
                f = -3.5 * xCumul/laneLength;
            }
            else if (type.equals(LaneType.DIVERGE)) {
                f = -3.5 * (laneLength - xCumul)/laneLength;
            }
            else {
                f = 0;
            }
            c = moveRightAndIntersect(distance + f, new double[]{x[i-1], x[i], x[i+1]}, new double[]{y[i-1], y[i], y[i+1]});
            xOut[i] = c.x;
            yOut[i] = c.y;
            dx = x[i+1] - x[i];
            dy = y[i+1] - y[i];
            xCumul = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
        }

        f = (type.equals(LaneType.MERGE)) ? -3.5 : 0;
        int n = coordinates.size()-1;
        c = moveRightAndIntersect(distance + f, new double[]{x[n-1], x[n], x[n] + (x[n] - x[n-1])}, new double[]{y[n-1], y[n], y[n] + (y[n] - y[n-1])});
        xOut[n] = c.x;
        yOut[n] = c.y;

        return CoordinateUtilities.combine(xOut, yOut);
    }

    private static Coordinate moveRightAndIntersect(double distance, double[] xs, double[] ys) {

        double dx1 = xs[1] - xs[0];
        double dx2 = xs[2] - xs[1];
        double dy1 = ys[1] - ys[0];
        double dy2 = ys[2] - ys[1];

        double f1 = 1/Math.sqrt(Math.pow(dx1,2) + Math.pow(dy1,2));
        double f2 = 1/Math.sqrt(Math.pow(dx2,2) + Math.pow(dy2,2));

        double xRight1  = xs[0] - dy1 * f1 * distance;
        double xRight2a = xs[1] - dy1 * f1 * distance;
        double xRight2b = xs[1] - dy2 * f2 * distance;
        double xRight3  = xs[2] - dy2 * f2 * distance;
        double yRight1  = ys[0] + dx1 * f1 * distance;
        double yRight2a = ys[1] + dx1 * f1 * distance;
        double yRight2b = ys[1] + dx2 * f2 * distance;
        double yRight3  = ys[2] + dx2 * f2 * distance;

        double a1 = (yRight2a-yRight1) / (xRight2a-xRight1);
        double b1 = yRight1 - xRight1 * a1;
        double a2 = (yRight3-yRight2b) / (xRight3-xRight2b);
        double b2 = yRight2b - xRight2b * a2;

        double x,y;
        if (Math.abs(a1-a2)<0.001) {
                x = xRight2a;
                y = yRight2a;
        }
        else {
            x = -(b1-b2)/(a1-a2);
            y = a1*x+b1;
        }
        return new Coordinate(x, y, 0);
    }
}
