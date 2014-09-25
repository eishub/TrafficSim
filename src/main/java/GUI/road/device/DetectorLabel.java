package GUI.road.device;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.road.device.jDetector;

import java.awt.*;

/**
 * Graphic class for detector ids.
 */
public class DetectorLabel implements Graphic {
    /** The concerned detector. */
    private jDetector detector;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor that sets the detector.
     */
    public DetectorLabel(NetworkCanvas networkCanvas, jDetector detector) {
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
     * Paints the detector id on the canvas.
     * @param g Graphics to paint with.
     * @param canvas Canvas that is painted on.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        // id
        if (networkCanvas.popupItemChecked("Show detector IDs")) {
            Point.Double p = detector.lane.XY(detector.x());
            Point point = networkCanvas.getPoint(p.x, p.y);
            g.setColor(new Color(255, 255, 255));
            g.drawString(""+detector.id(), point.x, point.y);
        }
    }
}
