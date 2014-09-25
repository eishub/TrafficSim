package GUI.vehicle;

import GUI.Graphic;
import GUI.NetworkCanvas;
import microModel.core.vehicle.Enclosure;
import microModel.core.vehicle.Vehicle;
import microModel.jModel;

import java.awt.*;

/**
 * Graphic class for vehicles.
 */
public class VehicleGraphic implements Graphic {

    /** The concerned vehicle object. */
    private Vehicle vehicle;
    private NetworkCanvas networkCanvas;

    /**
     * Constructor that sets the vehicle.
     * @param vehicle Concerned vehicle.
     */
    public VehicleGraphic(NetworkCanvas networkCanvas, Vehicle vehicle) {
        this.networkCanvas = networkCanvas;
        this.vehicle = vehicle;
    }

    /**
     * Returnes the concerned vehicle.
     * @return Vehicle of this vehicle Graphic.
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Checks whether the vehicle is still in the simulation.
     * @return Whether the vehicle is still in the simulation.
     */
    public boolean exists() {
        jModel model = jModel.getInstance();
        return model.getVehicles().contains(vehicle);
    }

    /**
     * Returns the bounding box of the vehicle position.
     * @return By default <tt>null</tt>.
     */
    public Rectangle.Double getGlobalBounds() {
        return null;
    }

    /**
     * Paints the vehicle on the canvas.
     * @param g Graphics to paint with.
     * @param canvas Canvas that is painted on.
     */
    public void paint(Graphics g, NetworkCanvas canvas) {
        Graphics2D g2 = (Graphics2D) g;
        VehicleColor vehCol = (VehicleColor) networkCanvas.getGui().getVehCol().getSelectedItem();
        g2.setColor(vehCol.getColor(vehicle));
        if (getVehicle().isCrashed()) {
            Point point = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
            Color color = g2.getColor();
            g2.setColor(new Color(225,50,50));
            g2.fillOval(point.x-7, point.y-7, 14, 14);
            g2.setColor(color);
            g2.fillOval(point.x - 4, point.y - 4, 8, 8);
            g2.drawString(String.valueOf(vehicle.getDriver().getID() + " CRASH!"), point.x-4 + 10, point.y-4 + 10);
        }
        else if (networkCanvas.popupItemChecked("Vehicles as dots")) {
            Point point = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
            g2.fillOval(point.x-4, point.y-4, 7, 7);
            g2.setColor(new Color(255,5,5));
            g2.drawString(String.valueOf(vehicle.getDriver().getID()), point.x-4 + 10, point.y-4 + 10);
        } else {
            Polygon pol = new Polygon();
            double w = 2;
            Point point = new Point();
            point = networkCanvas.getPoint(vehicle.getCoordinates().x + vehicle.heading.y * w / 2,
                    vehicle.getCoordinates().y - vehicle.heading.x * w / 2);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(vehicle.getCoordinates().x - vehicle.heading.y * w / 2,
                    vehicle.getCoordinates().y + vehicle.heading.x * w / 2);
            pol.addPoint(point.x, point.y);
            double x2 = vehicle.getCoordinates().x - vehicle.heading.x*vehicle.getLength();
            double y2 = vehicle.getCoordinates().y - vehicle.heading.y*vehicle.getLength();
            point = networkCanvas.getPoint(x2 - vehicle.heading.y * w / 2,
                    y2 + vehicle.heading.x * w / 2);
            pol.addPoint(point.x, point.y);
            point = networkCanvas.getPoint(x2 + vehicle.heading.y * w / 2,
                    y2 - vehicle.heading.x * w / 2);
            pol.addPoint(point.x, point.y);
            g2.fillPolygon(pol);
            g2.setColor(new Color(255,5,5));
            g2.drawString(String.valueOf(vehicle.getDriver().getID()), point.x + 10, point.y + 10);
        }

        if (networkCanvas.popupItemChecked("Show downstream")) {
            if (vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM) !=null) {
                g2.setColor(new Color(255, 0, 0));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point.Double p3 = vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getLane().XY(vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getX() - vehicle.getVehicle(Enclosure.LEFT_DOWNSTREAM).getLength());
                Point p2 = networkCanvas.getPoint(p3.x, p3.y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            if (vehicle.getVehicle(Enclosure.DOWNSTREAM) !=null) {
                g2.setColor(new Color(0, 255, 0));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point.Double p3 = vehicle.getVehicle(Enclosure.DOWNSTREAM).getLane().XY(vehicle.getVehicle(Enclosure.DOWNSTREAM).getX() - vehicle.getVehicle(Enclosure.DOWNSTREAM).getLength());
                Point p2 = networkCanvas.getPoint(p3.x, p3.y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            if (vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM) !=null) {
                g2.setColor(new Color(0, 0, 255));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point.Double p3 = vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getLane().XY(vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getX() - vehicle.getVehicle(Enclosure.RIGHT_DOWNSTREAM).getLength());
                Point p2 = networkCanvas.getPoint(p3.x, p3.y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
        if (networkCanvas.popupItemChecked("Show upstream")) {
            if (vehicle.getVehicle(Enclosure.LEFT_UPSTREAM) !=null) {
                g2.setColor(new Color(255, 0, 255));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point p2 = networkCanvas.getPoint(vehicle.getVehicle(Enclosure.LEFT_UPSTREAM).getCoordinates().x, vehicle.getVehicle(Enclosure.LEFT_UPSTREAM).getCoordinates().y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            if (vehicle.getVehicle(Enclosure.UPSTREAM) !=null) {
                g2.setColor(new Color(255, 255, 0));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point p2 = networkCanvas.getPoint(vehicle.getVehicle(Enclosure.UPSTREAM).getCoordinates().x, vehicle.getVehicle(Enclosure.UPSTREAM).getCoordinates().y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            if (vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM) !=null) {
                g2.setColor(new Color(0, 255, 255));
                Point p1 = networkCanvas.getPoint(vehicle.getCoordinates().x, vehicle.getCoordinates().y);
                Point p2 = networkCanvas.getPoint(vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM).getCoordinates().x, vehicle.getVehicle(Enclosure.RIGHT_UPSTREAM).getCoordinates().y);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

    }
}
