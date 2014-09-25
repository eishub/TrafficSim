package GUI.vehicle;

import GUI.NetworkCanvas;
import GUI.vehicle.VehicleColor;
import microModel.core.vehicle.Enclosure;
import microModel.core.vehicle.Vehicle;

import java.awt.*;

public class TimeToCollision implements VehicleColor {

    // Return the color of the given vehicle
    public Color getColor(Vehicle vehicle) {
        // Calculate time to collision
        double ttc = Double.POSITIVE_INFINITY;
        if (vehicle.getVehicle(Enclosure.DOWNSTREAM) !=null && vehicle.getSpeed()> vehicle.getVehicle(Enclosure.DOWNSTREAM).getSpeed()) {
            double s = vehicle.getGap(vehicle.getVehicle(Enclosure.DOWNSTREAM));
            ttc = s/(vehicle.getSpeed()- vehicle.getVehicle(Enclosure.DOWNSTREAM).getSpeed());
        }
        // Select color
        if (ttc < 4) {
            return Color.RED;
        } else {
            return Color.WHITE;
        }
    }
    
    // Set legend info using default method
    public void setInfo(javax.swing.JPanel panel) {
        Color[] colors = {Color.RED, Color.WHITE};
        String[] labels = {"Critical", "Non-critical"};
        NetworkCanvas.defaultLegend(panel, colors, labels);
    }
    
    // Return nice readable string
    // if this method is not overridden, it shows up like myPackage.TimeToCollision@1372a1a
    @Override
    public String toString() {
        return "time to collision";
    }
}