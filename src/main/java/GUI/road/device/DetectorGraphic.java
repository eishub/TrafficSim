package GUI.road.device;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.road.device.jDetector;

import java.awt.*;

/**
 * Graphic class for detectors.
 */
public class DetectorGraphic implements Graphic {

    /** The concerned detector. */
    private jDetector detector;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor that sets the detector.
     */
    public DetectorGraphic(NetworkCanvas networkCanvas, jDetector detector) {
        this.networkCanvas = networkCanvas;
        this.detector = detector;
    }

    /**
     * Whether the detector still exists in simulation.
     * @return By default <tt>true</tt>.
     */
    public boolean exists() {
        return true;
    }

    /**
     * Returns the bounding box of the detector position.
     * @return By default <tt>null</tt>.
     */
    public Rectangle.Double getGlobalBounds() {
        return null;
    }

    /**
     * Paints the detector on the canvas.
     * @param g Graphics to paint with.
     * @param canvas Canvas that is painted on.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        if (networkCanvas.popupItemChecked("Show detectors")) {
            Point.Double p = detector.lane.XY(detector.x());
            Point.Double h = detector.lane.heading(detector.x());
            Polygon pol = new Polygon();

            Point point;
            point = networkCanvas.getPoint(p.x + h.y, p.y - h.x);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x - h.y, p.y + h.x);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x - h.y - h.x, p.y + h.x - h.y);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x + h.y - h.x, p.y - h.x - h.y);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x + h.y + h.x, p.y - h.x + h.y);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x - h.y + h.x, p.y + h.x + h.y);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(p.x - h.y, p.y + h.x);
            pol.addPoint(point.x, point.y);

            g.setColor(new Color(64, 64, 64));
            g.drawPolyline(pol.xpoints, pol.ypoints, pol.npoints);
        }
    }
}
