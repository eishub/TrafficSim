package GUI.extra;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.road.jLane;

import java.awt.*;

/**
 * Graphic class for generator queue label.
 */
public class GeneratorQueue implements Graphic {

    /** The concerned lane object. */
    private jLane lane;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor based on a lane object.
     * @param lane The lane object.
     */
    public GeneratorQueue(NetworkCanvas networkCanvas, jLane lane) {
        this.networkCanvas = networkCanvas;
        this.lane = lane;
    }

    /**
     * Whether the generator exists.
     * @return Always <tt>true</tt>.
     */
    public boolean exists() {
        return true;
    }

    /**
     * Returns the bounding box.
     * @return Bounding box of the generator.
     */
    public Rectangle.Double getGlobalBounds() {
        return null;
    }

    /**
     * Paints the generator queu on the canvas.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        // id
        if (networkCanvas.popupItemChecked("Show generator queue")) {
            Point point = networkCanvas.getPoint(lane.getX()[0], lane.getY()[0]);
            g.setColor(new Color(255, 255, 255));
            g.drawString(""+ lane.getGenerator().getQueue(), point.x, point.y);
        }
    }
}
