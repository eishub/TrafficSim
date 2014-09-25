package GUI.vehicle;

import GUI.NetworkCanvas;
import apl.AgentDriver;
import microModel.core.driver.model.IDMPlus;
import microModel.core.driver.model.LMRS;
import microModel.core.vehicle.Vehicle;

import java.awt.*;

/**
 * Enumeration of vehicle color selections with additional functionality to
 * return the color and to set the color information (e.g. legend).
 */
public enum DefaultVehicleColors implements VehicleColor {
    
    /** Default white. */
    NONE, 
    /** Velocity on continuous map. */
    VELOCITY, 
    /** Acceleration on continuous map. */
    ACCELERATION, 
    /** Destination. */
    DESTINATION,
    /** Desired headway on continuous map. */
    DESIRED_HEADWAY,
    /** LaneType change preparation (yielding, synchronizing etc.). */
    LANE_CHANGE_PROCESS, 
    /** Desired speed on continuous map. */
    DESIRED_SPEED, 
    /** Class id. */
    CLASS_ID;

    /** Default acceleration values of the default color map. */
    private final double[] ACCELERATIONS = new double[] {-10, -5, -2, 0, 2};
    
    /** Default velocity values of the default color map. */
    private final double[] VELOCITIES = new double[] {0, 30, 60, 90, 120};
    
    /** Default headway values of the default color map. */
    private final double[] HEADWAYS = new double[] {.5, .75, 1, 1.25, 1.5};
    
    /** Default desired speed values of the default color map. */
    private final double[] DESIRED_SPEEDS = new double[] {.6, .75, .9, 1.05, 1.2}; 

    /**
     * Returns the value as a better looking string for the drop-down box.
     * @return Better looking string.
     */
    @Override
    public java.lang.String toString() {
        return name().toLowerCase().replace("_", " ");
    }

    /**
     * Returns the color for the given vehicle based on a default color indication.
     * @param vehicle Vehicle to determine the color for.
     * @return Color for the vehicle.
     */
    public Color getColor(Vehicle vehicle) {
        Color c = new Color(255, 255, 255);
        if (vehicle.getDriver() instanceof AgentDriver) {
            c = new Color(8, 22, 200);
        }
        if (this==VELOCITY) {
            c = NetworkCanvas.fromDefaultMap(VELOCITIES, vehicle.getSpeed() * 3.6);
        } else if (this==DESTINATION) {
            c = NetworkCanvas.nToColor(vehicle.driver.getRoute().destinations()[0]);
        } else if (this==CLASS_ID) {
            c = NetworkCanvas.nToColor(vehicle.classID);
        } else if (this==LANE_CHANGE_PROCESS) {
            boolean indic = vehicle.isIndicatingLeft() || vehicle.isIndicatingRight();
            boolean sync = vehicle.getDriver().get(LMRS.LEFT_SYNC) || vehicle.getDriver().get(LMRS.RIGHT_SYNC);
            boolean yield = vehicle.getDriver().get(LMRS.LEFT_YIELD) || vehicle.getDriver().get(LMRS.RIGHT_YIELD);
            if (indic) {
                c = new Color(255, 64, 0);
            } else if (sync) {
                c = new Color(255, 204, 0);
            } else if (yield) {
                c = new Color(0, 204, 0);
            } 
        } else if (this==ACCELERATION) {
            c = NetworkCanvas.fromDefaultMap(ACCELERATIONS, vehicle.getAcceleration());
        } else if (this==DESIRED_HEADWAY) {
            c = NetworkCanvas.fromDefaultMap(HEADWAYS, vehicle.getDriver().get(IDMPlus.T));
        } else if (this==DESIRED_SPEED) {
            c = NetworkCanvas.fromDefaultMap(DESIRED_SPEEDS,
                    vehicle.getDriver().get(IDMPlus.V0) / vehicle.getLane().getVLimInMetersPerSecond());
        } else if (this==NONE) {
            // white
        }
        return c;
    }

    /** 
     * Fills the color information panel with the appropiate information/legend
     * depending on a default selection value.
     * @param panel Panel to be filled with the information.
     */
    public void setInfo(javax.swing.JPanel panel) {
        panel.setLayout(null);
        int h = (int) panel.getMaximumSize().getHeight();
        int w = (int) panel.getMaximumSize().getWidth();
        int b = 1;
        int width = (w-4*b)/3;
        int height = (h-3*b)/2;
        java.text.DecimalFormat df0 = new java.text.DecimalFormat("0");
        java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");
        float fontSize = 9f;
        if (this==VELOCITY) {
            Color[] colors = new Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = NetworkCanvas.fromDefaultMap(VELOCITIES, VELOCITIES[k]);
                labels[k] = df0.format(VELOCITIES[k])+"km/h";
            }
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESTINATION) {
            Color[] colors = new Color[6];
            String[] labels = new String[6];
            for (int k=0; k<6; k++) {
                colors[k] = NetworkCanvas.nToColor(k + 1);
                labels[k] = "destination "+(k+1);
            }
            labels[5] = "etc.";
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==CLASS_ID) {
            Color[] colors = new Color[6];
            String[] labels = new String[6];
            for (int k=0; k<6; k++) {
                colors[k] = NetworkCanvas.nToColor(k + 1);
                labels[k] = "class "+(k+1);
            }
            labels[5] = "etc.";
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==LANE_CHANGE_PROCESS) {
            Color[] colors = {new Color(255, 204, 0), new Color(255, 64, 0),
                new Color(0, 204, 0), new Color(255, 255, 255)};
            String[] labels = {"synchronizing", "indicating", "yielding", "none"};
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==ACCELERATION) {
            Color[] colors = new Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = NetworkCanvas.fromDefaultMap(ACCELERATIONS, ACCELERATIONS[k]);
                labels[k] = "<html>"+df0.format(ACCELERATIONS[k])+"m/s<sup>2</sup></html>";
            }
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESIRED_HEADWAY) {
            Color[] colors = new Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = NetworkCanvas.fromDefaultMap(HEADWAYS, HEADWAYS[k]);
                labels[k] = df2.format(HEADWAYS[k])+"s";
            }
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESIRED_SPEED) {
            Color[] colors = new Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = NetworkCanvas.fromDefaultMap(DESIRED_SPEEDS, DESIRED_SPEEDS[k]);
                labels[k] = df2.format(DESIRED_SPEEDS[k])+"x";
            }
            NetworkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==NONE) {
            javax.swing.JLabel lab = new javax.swing.JLabel("Select vehicle color indication here.");
            lab.setBounds(b, h/2-height/2, w-2*b, height);
            lab.setFont(lab.getFont().deriveFont(fontSize).deriveFont(java.awt.Font.PLAIN));
            panel.add(lab);
        }
    }
}