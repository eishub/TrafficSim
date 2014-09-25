package GUI.road;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.road.jLane;

import java.awt.*;

/**
 * Graphic class for lane labels.
 */
public class LaneLabel implements Graphic {

    /** The concerned lane object. */
    private jLane lane;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor based on a lane object.
     * @param lane The lane object.
     */
    public LaneLabel(NetworkCanvas networkCanvas, jLane lane) {
        this.networkCanvas = networkCanvas;
        this.lane = lane;
    }

    /**
     * Whether the lane exists.
     * @return Always <tt>true</tt>.
     */
    public boolean exists() {
        return true;
    }

    /**
     * Returns the bounding box.
     * @return Bounding box of the label.
     */
    public Rectangle.Double getGlobalBounds() {
        return null;
    }

    /**
     * Paints the lane id on the canvas.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        // id
        if (networkCanvas.popupItemChecked("Show lane IDs")) {
            Point point;
            if (lane.getTaper() !=null && lane.getTaper() ==lane && lane.getUp() ==null) {
                // diverge taper, move id location to right for overlap
                point = networkCanvas.getPoint((lane.getX()[0] + lane.getRight().getX()[0]) / 2, (lane.getY()[0] + lane.getRight().getY()[0]) / 2);
            } else {
                point = networkCanvas.getPoint(lane.getX()[0], lane.getY()[0]);
            }
            g.setColor(new Color(255, 255, 255));
            g.drawString(""+lane.id(), point.x, point.y);
        }
    }
}
