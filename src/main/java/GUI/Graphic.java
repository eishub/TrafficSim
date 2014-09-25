package GUI;

import java.awt.*;

/**
 * Interface for graphics on the network canvas. Sub classes should implement 
 * the methods as described. Required information for these methods, probably at
 * least some object from the simulation, should be provided to the sub class 
 * for instance through the constructor. To add a Graphic to the network canvas
 * for painting, use <tt>jModelGUI.addBackdrop(Graphic)</tt> or
 * <tt>jModelGUI.addOverlay(Graphic)</tt>.<br>
 * <br>
 * Within the implementation of the <tt>paint</tt> method, the network canvas is
 * supplied. This should only be used in two ways:<br>
 * 1) Use the size information to paint information on an absolute screen 
 * location (e.g. the upper left corner).<br>
 * 2) Use conveniance methods to supply colors and positions:<br>
 * <br>
 * - <tt>getPoint</tt> supplies the on-canvas point (in pixels) of a global
 * point. Use this to translate any global location to the point where it needs
 * to be painted. Do not worry about objects falling outside of the visible 
 * range, these are automatichally clipped (not drawn).<br>
 * - <tt>fromDefaultMap</tt> returns an interpolated color on the default color
 * map. The value extend of the map can be defined.<br>
 * - <tt>fromMap</tt> returns an interpolated color in a user defined map.<br>
 * - <tt>nToColor</tt> returns a distinct color for the values 1-18 and repeats
 * the colors for higher values.<br>
 * <br>
 * Note that the latter three are static methods which can be called on the
 * class instead of an object.
 */
public interface Graphic {
    
    /**
     * Returns the global bounding box of this object which ensures that it is
     * displayed within the network canvas. It may return <tt>null</tt> in case
     * the bounding box is not applicable/required. By default the entire 
     * network of lanes is on the network canvas.
     * @return Global bounding box of this graphics.
     */
    public Rectangle.Double getGlobalBounds();
    
    /**
     * Returns whether the object that is visualized is still in the simulation.
     * If not, the Graphic is removed from the GUI. Return <tt>true</tt> if not
     * applicable.
     * @return Whether the visualized object exists.
     */
    public boolean exists();
    
    /**
     * Allows direct painting on the network canvas by using the supplied 
     * graphics object. The canvas itself is supplied to query its size when 
     * painting information at an absolute window location (e.g. upper left 
     * corner of the canvas) and for use of some conveniance methods.
     * @param g Graphics object to paint with.
     * @param canvas Network canvas that is painted on.
     */
    public void paint(Graphics g, NetworkCanvas canvas);
}