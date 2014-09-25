package GUI.road;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.road.jLane;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Graphic class for lanes.
 */
public class LaneGraphic implements Graphic {

    /** The concerned lane object. */
    private jLane lane;

    /** Nested Poly object that houses shape information. */
    private Poly pol;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor based on a lane object.
     * @param lane The lane object.
     */
    public LaneGraphic(NetworkCanvas networkCanvas, jLane lane) {
        this.networkCanvas = networkCanvas;
        this.lane = lane;
        pol = new Poly(lane);
    }

    /**
     * Whether the lane exists.
     * @return Always <tt>true</tt>.
     */
    public boolean exists() {
        return true;
    }

    /**
     * Returns the bounding box of the lane coordinates arrays with an
     * additional border of 1.75m (half a default lane width).
     * @return Bounding box of the lane.
     */
    public Rectangle.Double getGlobalBounds() {
        double minGlobalX = Double.POSITIVE_INFINITY;
        double maxGlobalX = Double.NEGATIVE_INFINITY;
        double minGlobalY = Double.POSITIVE_INFINITY;
        double maxGlobalY = Double.NEGATIVE_INFINITY;
        for (int j=0; j< lane.getX().length; j++) {
            minGlobalX = lane.getX()[j] < minGlobalX ? lane.getX()[j] :  minGlobalX;
            maxGlobalX = lane.getX()[j] > maxGlobalX ? lane.getX()[j] :  maxGlobalX;
            minGlobalY = lane.getY()[j] < minGlobalY ? lane.getY()[j] :  minGlobalY;
            maxGlobalY = lane.getY()[j] > maxGlobalY ? lane.getY()[j] :  maxGlobalY;
        }
        return new Rectangle.Double(minGlobalX-1.75, minGlobalY-1.75,
                (maxGlobalX-minGlobalX)+3.5, (maxGlobalY-minGlobalY)+3.5);
    }

    /**
     * Paints the lane on the canvas.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        if (networkCanvas.popupItemChecked("Show lanes")) {
            // area
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(128, 128, 128));
            g2.fillPolygon(pol.area());

            // initialize graphics for lines
            g2.setColor(new Color(255, 255, 255));
            PolyLine line;
            setStroke(g2, 1, 1f, 0f);

            // up line
            if (lane.getUp() ==null && lane.getGenerator() ==null) {
                line = pol.upEdge();
                g2.drawPolyline(line.x, line.y, line.n);
            }

            // down line
            if (lane.getDown() ==null && lane.getDestination() ==0) {
                line = pol.downEdge();
                g2.drawPolyline(line.x, line.y, line.n);
            }

            // left line
            if (lane.getLeft() ==null || (!lane.isGoLeft() && !lane.getLeft().isGoRight())) {
                // continuous normal
                line = pol.leftEdge();
                setStroke(g2, 1, 1f, line.x[0]);
            } else if (lane.isGoLeft() && lane.getLeft().isGoRight()) {
                // dashed normal
                line = pol.leftEdge();
                setStroke(g2, 2, 1f, line.x[0]);
            } else if (!lane.isGoLeft() && lane.getLeft().isGoRight()) {
                // continuous near
                line = pol.leftNearEdge();
                setStroke(g2, 1, 1f, line.x[0]);
            } else {
                // dashed near
                line = pol.leftNearEdge();
                setStroke(g2, 2, 1f, line.x[0]);
            }
            g2.drawPolyline(line.x, line.y, line.n);

            // right line
            boolean drawRight = false;
            if (lane.getRight() ==null || (!lane.isGoRight() && !lane.getRight().isGoLeft())) {
                // continuous normal
                // also right if both not allowed, may be non-adjacent but
                // linked lanes for synchronization
                line = pol.rightEdge();
                setStroke(g2, 1, 1f, line.x[0]);
                drawRight = true;
            } else if (lane.getRight().isGoLeft() && !lane.isGoRight()) {
                // continuous near
                line = pol.rightNearEdge();
                setStroke(g2, 1, 1f, line.x[0]);
                drawRight = true;
            } else if (!lane.getRight().isGoLeft() && lane.isGoRight()) {
                // dashed near
                line = pol.rightNearEdge();
                setStroke(g2, 2, 1f, line.x[0]);
                drawRight = true;
            }
            if (drawRight) {
                g2.drawPolyline(line.x, line.y, line.n);
            }

            // destination
            if (networkCanvas.showDestinations() && lane.getDestination() !=0) {
                g2.setColor(NetworkCanvas.nToColor(lane.getDestination()));
                setStroke(g2, 1, 3f, 0f);
                line = pol.downEdge();
                g2.drawPolyline(line.x, line.y, line.n);
            }
            setStroke(g2, 3, 1f, 0f);
        }
    }

    /**
     * Sets a stroke for the Graphics object.
     * @param g2 Graphics object.
     * @param type 1=continuous, 2=dashed, 3=default <tt>BasicStroke</tt>
     * @param width Width of the line in pixels (type 1 or 2 only)
     * @param x First x-coordinates to determine the phase for horizontal lines
     */
    private void setStroke(Graphics2D g2, int type, float width, float x) {
        if (type==3) {
            // 3 = basic stroke
            g2.setStroke(new BasicStroke());
        } else {
            float[] dash = null; // 1 = continuous
            if (type==2) {
                // 2 = dashed
                dash = new float[2];
                dash[0] = 3f;
                dash[1] = 9f;
            }
            float phase = (float) (x - Math.floor((double)x/12)*12);
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 1.0f, dash, phase));
        }
    }

    /**
     * Nested class that derives and houses shape information.
     */
    private class Poly {

        /** x coordinates of entire area. */
        private double[] xArea;

        /** y coordinates of entire area. */
        private double[] yArea;

        /** x coordinates of left side. */
        private double[] xLeftEdge;

        /** y coordinates of left side. */
        private double[] yLeftEdge;

        /** x coordinates of right side. */
        private double[] xRightEdge;

        /** y coordinates of right side. */
        private double[] yRightEdge;

        /** x coordinates of left side in case of dual line markings. */
        private double[] xNearLeftEdge;

        /** y coordinates of left side in case of dual line markings. */
        private double[] yNearLeftEdge;

        /** x coordinates of right side in case of dual line markings. */
        private double[] xNearRightEdge;

        /** y coordinates of right side in case of dual line markings. */
        private double[] yNearRightEdge;

        /** Number of points in lane position vector. */
        private int n;

        /**
         * Constructor that derives all needed information.
         * @param lane LaneType object.
         */
        private Poly(jLane lane) {
            // check whether the lane is a taper
            boolean mergeTaper = false;
            boolean divergeTaper = false;
            boolean addRight = false;
            boolean addLeft = false;
            boolean subRight = false;
            boolean subLeft = false;
            if (lane.getTaper() ==lane && lane.getDown() ==null) {
                mergeTaper = true;
            } else if (lane.getTaper() ==lane && lane.getUp() ==null) {
                divergeTaper = true;
            }
            if (lane.getDown() ==null && lane.getRight() ==null && lane.getLeft() !=null) {
                subRight = true;
            } else if (lane.getDown() ==null && lane.getRight() !=null && lane.getLeft() ==null) {
                subLeft = true;
            }
            if (lane.getUp() ==null && lane.getRight() ==null && lane.getLeft() !=null) {
                addRight = true;
            } else if (lane.getUp() ==null && lane.getRight() !=null && lane.getLeft() ==null) {
                addLeft = true;
            }

            // set width numbers
            double w = 1.75; // half lane width
            double near = 0.375; // half distance between dual lane marking
            double f = 1; // factor that reduces width along tapers

            // first point
            n = lane.getX().length;
            xLeftEdge = new double[n];
            yLeftEdge = new double[n];
            xRightEdge = new double[n];
            yRightEdge = new double[n];
            xNearLeftEdge = new double[n];
            yNearLeftEdge = new double[n];
            xNearRightEdge = new double[n];
            yNearRightEdge = new double[n];
            Point2D.Double[] start = new Point2D.Double[0];
            Point2D.Double[] startNear = new Point2D.Double[0];
            if (divergeTaper) {
                f = -1;
            }
            if (lane.getUp() !=null) {
                int nUp = lane.getUp().getX().length;
                start = intersect(w*f, w,
                        lane.getUp().getX()[nUp-2], lane.getX()[0], lane.getX()[1],
                        lane.getUp().getY()[nUp-2], lane.getY()[0], lane.getY()[1]);
                startNear = intersect(w*f-near, w-near,
                        lane.getUp().getX()[nUp-2], lane.getX()[0], lane.getX()[1],
                        lane.getUp().getY()[nUp-2], lane.getY()[0], lane.getY()[1]);
            } else {
                start = intersect(w*f, w,
                        lane.getX()[0] - (lane.getX()[1]- lane.getX()[0]), lane.getX()[0], lane.getX()[1],
                        lane.getY()[0] - (lane.getY()[1]- lane.getY()[0]), lane.getY()[0], lane.getY()[1]);
                startNear = intersect(w*f-near, w-near,
                        lane.getX()[0] - (lane.getX()[1]- lane.getX()[0]), lane.getX()[0], lane.getX()[1],
                        lane.getY()[0] - (lane.getY()[1]- lane.getY()[0]), lane.getY()[0], lane.getY()[1]);
            }
            xLeftEdge[0] = start[0].x;
            yLeftEdge[0] = start[0].y;
            xRightEdge[0] = start[1].x;
            yRightEdge[0] = start[1].y;
            xNearLeftEdge[0] = startNear[0].x;
            yNearLeftEdge[0] = startNear[0].y;
            xNearRightEdge[0] = startNear[1].x;
            yNearRightEdge[0] = startNear[1].y;

            // middle points
            Point2D.Double[] point = new Point2D.Double[0];
            Point2D.Double[] pointNear = new Point2D.Double[0];
            f = 1; // default for no taper
            double dx = lane.getX()[1]- lane.getX()[0];
            double dy = lane.getY()[1]- lane.getY()[0];
            double len = Math.sqrt(dx*dx + dy*dy); // cumulative length for tapers
            for (int i=1; i<n-1; i++) {
                if (mergeTaper) {
                    // reducing width
                    f = 1 - 2*len/ lane.getL();
                    dx = lane.getX()[i+1]- lane.getX()[i];
                    dy = lane.getY()[i+1]- lane.getY()[i];
                    len = len + Math.sqrt(dx*dx + dy*dy);
                } else if (divergeTaper) {
                    // increasing width
                    f = -1 + 2*len/ lane.getL();
                    dx = lane.getX()[i+1]- lane.getX()[i];
                    dy = lane.getY()[i+1]- lane.getY()[i];
                    len = len + Math.sqrt(dx*dx + dy*dy);
                }
                point = intersect(w*f, w,
                        lane.getX()[i-1], lane.getX()[i], lane.getX()[i+1],
                        lane.getY()[i-1], lane.getY()[i], lane.getY()[i+1]);
                pointNear = intersect(w*f-near, w-near,
                        lane.getX()[i-1], lane.getX()[i], lane.getX()[i+1],
                        lane.getY()[i-1], lane.getY()[i], lane.getY()[i+1]);
                xLeftEdge[i] = point[0].x;
                yLeftEdge[i] = point[0].y;
                xRightEdge[i] = point[1].x;
                yRightEdge[i] = point[1].y;
                xNearLeftEdge[i] = pointNear[0].x;
                yNearLeftEdge[i] = pointNear[0].y;
                xNearRightEdge[i] = pointNear[1].x;
                yNearRightEdge[i] = pointNear[1].y;
            }

            // last point
            Point2D.Double[] end = new Point2D.Double[0];
            Point2D.Double[] endNear = new Point2D.Double[0];
            if (mergeTaper) {
                f = -1;
            } else {
                f = 1;
            }
            if (lane.getDown() !=null) {
                end = intersect(w*f, w,
                        lane.getX()[n-2], lane.getX()[n-1], lane.getDown().getX()[1],
                        lane.getY()[n-2], lane.getY()[n-1], lane.getDown().getY()[1]);
                endNear = intersect(w*f-near, w-near,
                        lane.getX()[n-2], lane.getX()[n-1], lane.getDown().getX()[1],
                        lane.getY()[n-2], lane.getY()[n-1], lane.getDown().getY()[1]);
            } else {
                end = intersect(w*f, w,
                        lane.getX()[n-2], lane.getX()[n-1], lane.getX()[n-1]+(lane.getX()[n-1]- lane.getX()[n-2]),
                        lane.getY()[n-2], lane.getY()[n-1], lane.getY()[n-1]+(lane.getY()[n-1]- lane.getY()[n-2]));
                endNear = intersect(w*f-near, w-near,
                        lane.getX()[n-2], lane.getX()[n-1], lane.getX()[n-1]+(lane.getX()[n-1]- lane.getX()[n-2]),
                        lane.getY()[n-2], lane.getY()[n-1], lane.getY()[n-1]+(lane.getY()[n-1]- lane.getY()[n-2]));
            }
            xLeftEdge[n-1] = end[0].x;
            yLeftEdge[n-1] = end[0].y;
            xRightEdge[n-1] = end[1].x;
            yRightEdge[n-1] = end[1].y;
            xNearLeftEdge[n-1] = endNear[0].x;
            yNearLeftEdge[n-1] = endNear[0].y;
            xNearRightEdge[n-1] = endNear[1].x;
            yNearRightEdge[n-1] = endNear[1].y;

            // combine area from edges
            xArea = new double[n*2+1];
            yArea = new double[n*2+1];
            for (int i=0; i<n; i++) {
                xArea[i] = xRightEdge[i];
                yArea[i] = yRightEdge[i];
            }
            for (int i=0; i<n; i++) {
                xArea[i+n] = xLeftEdge[n-i-1];
                yArea[i+n] = yLeftEdge[n-i-1];
            }
            xArea[n*2] = xRightEdge[0];
            yArea[n*2] = yRightEdge[0];
        }

        /**
         * Method that finds the intersection of the left and right side of
         * two lane sections.
         * @param wLeft Width towards the left [m].
         * @param wRight Width towards the right [m].
         * @param x1 1st x coordinates of upstream section [m].
         * @param x2 x coordinates of common point [m].
         * @param x3 2nd coordinates of downstream section [m].
         * @param y1 1st y coordinates of upstream section [m].
         * @param y2 y coordinates of common point [m].
         * @param y3 2nd y coordinates of downstream section [m].
         * @return Two points at the left and right side intersections.
         */
        private Point2D.Double[] intersect(
                double wLeft, double wRight,
                double x1, double x2, double x3,
                double y1, double y2, double y3) {

            // get headings
            double dx1 = x2-x1;
            double dy1 = y2-y1;
            double dx2 = x3-x2;
            double dy2 = y3-y2;

            // normalization factors
            double f1 = 1/Math.sqrt(dx1*dx1 + dy1*dy1);
            double f2 = 1/Math.sqrt(dx2*dx2 + dy2*dy2);

            // get coordinates of left adjacent lanes
            double xLeft1  = x1+dy1*f1*wLeft;
            double xLeft2a = x2+dy1*f1*wLeft;
            double xLeft2b = x2+dy2*f2*wLeft;
            double xLeft3  = x3+dy2*f2*wLeft;
            double yLeft1  = y1-dx1*f1*wLeft;
            double yLeft2a = y2-dx1*f1*wLeft;
            double yLeft2b = y2-dx2*f2*wLeft;
            double yLeft3  = y3-dx2*f2*wLeft;

            // get coordinates of right adjacent lanes
            double xRight1  = x1-dy1*f1*wRight;
            double xRight2a = x2-dy1*f1*wRight;
            double xRight2b = x2-dy2*f2*wRight;
            double xRight3  = x3-dy2*f2*wRight;
            double yRight1  = y1+dx1*f1*wRight;
            double yRight2a = y2+dx1*f1*wRight;
            double yRight2b = y2+dx2*f2*wRight;
            double yRight3  = y3+dx2*f2*wRight;

            // intersect left lines
            double a1 = (yLeft2a-yLeft1)/(xLeft2a-xLeft1);
            double b1 = yLeft1 - xLeft1*a1;
            double a2 = (yLeft3-yLeft2b)/(xLeft3-xLeft2b);
            double b2 = yLeft2b - xLeft2b*a2;
            double xLeft;
            double yLeft;
            if (Math.abs(a1-a2)<0.001) {
                xLeft = xLeft2a;
                yLeft = yLeft2a;
            } else {
                xLeft = -(b1-b2)/(a1-a2);
                yLeft = a1*xLeft+b1;
            }

            // intersect right lines
            a1 = (yRight2a-yRight1)/(xRight2a-xRight1);
            b1 = yRight1 - xRight1*a1;
            a2 = (yRight3-yRight2b)/(xRight3-xRight2b);
            b2 = yRight2b - xRight2b*a2;
            double xRight;
            double yRight;
            if (Math.abs(a1-a2)<0.001) {
                xRight = xRight2a;
                yRight = yRight2a;
            } else {
                xRight = -(b1-b2)/(a1-a2);
                yRight = a1*xRight+b1;
            }

            // gather output
            Point2D.Double[] out = new Point2D.Double[2];
            out[0] = new Point2D.Double(xLeft, yLeft);
            out[1] = new Point2D.Double(xRight, yRight);
            return out;
        }

        /**
         * Returns a polygon of the lane area.
         * @return Polygon of lane area.
         */
        private Polygon area() {
            int[] x = new int[2*n+1];
            int[] y = new int[2*n+1];
            Point point = new Point();
            for (int i=0; i<xArea.length; i++) {
                point = networkCanvas.getPoint(xArea[i], yArea[i]);
                x[i] = point.x;
                y[i] = point.y;
            }
            return new Polygon(x, y, n*2);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the left side.
         * @return Polyline of the left side.
         */
        private PolyLine leftEdge() {
            int[] x = new int[n];
            int[] y = new int[n];
            Point point = new Point();
            for (int i=0; i<xLeftEdge.length; i++) {
                point = networkCanvas.getPoint(xLeftEdge[i], yLeftEdge[i]);
                x[i] = point.x;
                y[i] = point.y;
            }
            return new PolyLine(x, y, n);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the right side.
         * @return Polyline of the right side.
         */
        private PolyLine rightEdge() {
            int[] x = new int[n];
            int[] y = new int[n];
            Point point = new Point();
            for (int i=0; i<xRightEdge.length; i++) {
                point = networkCanvas.getPoint(xRightEdge[i], yRightEdge[i]);
                x[i] = point.x;
                y[i] = point.y;
            }
            return new PolyLine(x, y, n);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the left side in case of dual line markings.
         * @return Polyline of the left side.
         */
        private PolyLine leftNearEdge() {
            int[] x = new int[n];
            int[] y = new int[n];
            Point point = new Point();
            for (int i=0; i<xNearLeftEdge.length; i++) {
                point = networkCanvas.getPoint(xNearLeftEdge[i], yNearLeftEdge[i]);
                x[i] = point.x;
                y[i] = point.y;
            }
            return new PolyLine(x, y, n);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the right side in case of dual line markings.
         * @return Polyline of the right side.
         */
        private PolyLine rightNearEdge() {
            int[] x = new int[n];
            int[] y = new int[n];
            Point point = new Point();
            for (int i=0; i<xNearRightEdge.length; i++) {
                point = networkCanvas.getPoint(xNearRightEdge[i], yNearRightEdge[i]);
                x[i] = point.x;
                y[i] = point.y;
            }
            return new PolyLine(x, y, n);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the upstream side.
         * @return Polyline of the upstream side.
         */
        private PolyLine upEdge() {
            int[] x = new int[2];
            int[] y = new int[2];
            Point point = new Point();
            point = networkCanvas.getPoint(xLeftEdge[0], yLeftEdge[0]);
            x[0] = point.x;
            y[0] = point.y;
            point = networkCanvas.getPoint(xRightEdge[0], yRightEdge[0]);
            x[1] = point.x;
            y[1] = point.y;
            return new PolyLine(x, y, 2);
        }

        /**
         * Returns a <tt>PolyLine</tt> of the downstream side.
         * @return Polyline of the downstream side.
         */
        private PolyLine downEdge() {
            int[] x = new int[2];
            int[] y = new int[2];
            Point point = new Point();
            point = networkCanvas.getPoint(xLeftEdge[n - 1], yLeftEdge[n - 1]);
            x[0] = point.x;
            y[0] = point.y;
            point = networkCanvas.getPoint(xRightEdge[n - 1], yRightEdge[n - 1]);
            x[1] = point.x;
            y[1] = point.y;
            return new PolyLine(x, y, 2);
        }
    }

    /**
     * Nested class that defines a Poly-line.
     */
    private class PolyLine {

        /** x coordinates. */
        private int[] x;

        /** y coordinates. */
        private int[] y;

        /** Number of coordinates. */
        private int n;

        /**
         * Constructor for the Poly line.
         * @param x x coordinates [m].
         * @param y y coordinates [m].
         * @param n Number of coordinates.
         */
        private PolyLine(int[] x, int[] y, int n) {
            this.x = x;
            this.y = y;
            this.n = n;
        }
    }
}
