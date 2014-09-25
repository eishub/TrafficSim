package GUI;

import GUI.extra.GeneratorQueue;
import GUI.road.LaneGraphic;
import GUI.road.LaneLabel;
import GUI.road.device.DetectorGraphic;
import GUI.road.device.DetectorLabel;
import GUI.vehicle.VehicleGraphic;
import com.itextpdf.text.*;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import microModel.core.road.device.jDetector;
import microModel.core.vehicle.Vehicle;
import microModel.jModel;

import javax.swing.*;
import java.awt.*;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Panel that displays the <tt>Graphic</tt>s objects and allows user zooming and
 * panning.
 */
public class NetworkCanvas extends JPanel {
    
    /** Vertical zoom factor. */
    private double vZoomFactor = 1;
    
    /** Horizontal zoom factor. */
    private double hZoomFactor = 1;
    
    /** Western most coordinates of the model. */
    private double minGlobalX;
    
    /** Eastern most coordinates of the model. */
    private double maxGlobalX;
    
    /** Nothern most coordinates of the model. */
    private double minGlobalY;
    
    /** Southern most coordinates of the model. */
    private double maxGlobalY;
    
    /** Number of horizontal pixels of network that cannot be shown. */
    private double canvasX;
    
    /** Number of vertical pixels of network that cannot be shown. */
    private double canvasY;
    
    /** Width of the entire network in pixels. */
    private double canvasW = 1;
    
    /** Height of the entire network in pixels. */
    private double canvasH = 1;
    
    /** Horizontal slider. */
    private JScrollBar hSlider;
    
    /** Vertical slider. */
    private JScrollBar vSlider;
    
    /** Policy which keeps the center point center when updating the view. */
    private int POLICY_KEEP = 0;
    
    /** Policy which centers the network in view. */
    private int POLICY_CENTER = 1;
    
    /** Policy which keeps a given point under the mouse when updating the view. */
    private int POLICY_POINT = 2;
    
    /** Stored center global point for policy <tt>POLICY_KEEP</tt>. */
    private Point.Double centerPoint = new Point.Double();
    
    /** Options menu. */
    private JPopupMenu popMenu;
    
    /** Corner square for if both sliders are shown. */
    private Component corner;
    
    /** Layer with user graphics. */
    private ArrayList<layer> layers;
    
    /** Layer for user backdrops. */
    private final int LAYER_BACKDROP = 5;
    
    /** Layer for lanes. */
    private final int LAYER_LANE = 4;
    
    /** Layer for detectors. */
    private final int LAYER_DETECTOR = 3;
    
    /** Layer for vehicles. */
    private final int LAYER_VEHICLE = 2;
    
    /** Layer for labels. */
    private final int LAYER_LABEL = 1;
    
    /** Layer for user overlays. */
    private final int LAYER_OVERLAY = 0;

    /** Parent GUI. */
    private jModelGUI gui;

    /** Whether destinations are shown upon the lanes. */
    private boolean showDestinations;
    
    /** Default set of distinct colors. */
    private static double[][] defaultColors = {
        {1,0,0}, {0,1,0}, {0,0,1}, {1,1,0},  {1,0,1},  {0,1,1},
        {.5,0,0},{0,.5,0},{0,0,.5},{.5,.5,0},{.5,0,.5},{0,.5,.5},
        {1,.5,0},{1,0,.5},{0,1,.5},{.5,1,0}, {.5,0,1}, {0,.5,1}
    };
    
    /** Default color map (dark red, red, yellow, green, dark green). */
    private static int[][] defaultMap = {
        {192, 255, 255, 0, 0},
        {0, 0, 255, 255, 192},
        {0, 0, 0, 0, 0}
    };
    
    /** Width of the sliders */
    private final int SLIDERWIDTH = 17;
    
    /** Minimum portion of the canvas that will be empty at zoom = 1 in pixels. */
    private final int EDGE = 50;
    
    /**
     * Constructs the network canvas.
     * @param gui GUI within which the canvas is placed.
     */
    protected NetworkCanvas(jModelGUI gui) {
        this.gui = gui;
        setBackground(new Color(0, 0, 0));
        setLayout(null);
        // add resize listener
        addComponentListener(
            new ComponentAdapter() {
                /** Invokes <tt>validateZoom()</tt> after a resize. */
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    validateZoom(POLICY_KEEP);
                }
            }
        );
        // mouse listener
        GeneralMouseListener gml = new GeneralMouseListener();
        addMouseListener(gml);
        addMouseMotionListener(gml);
        // sliders
        hSlider = new JScrollBar(SwingConstants.HORIZONTAL, 0, 0, 0, 0);
        vSlider = new JScrollBar(SwingConstants.VERTICAL, 0, 0, 0, 0);
        AdjustmentListener sl = new AdjustmentListener() {
            /** Invokes <tt>validateZoom()</tt> after a value change. */
            public void adjustmentValueChanged(AdjustmentEvent e) {
                validateZoom();
            }
        };
        hSlider.addAdjustmentListener(sl);
        vSlider.addAdjustmentListener(sl);
        corner = new Component() {
            /**
             * Paints the square.
             * @param g Graphics to paint with.
             */
            @Override
            public void paint(Graphics g) {
                g.setColor(vSlider.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        // popup menu
        popMenu = new JPopupMenu("Visualization options");
        addPopupItem("Vehicles as dots", false);
        addPopupItem("Show downstream", false);
        addPopupItem("Show upstream", false);
        popMenu.addSeparator();
        addPopupItem("Show lanes", true);
        addPopupItem("Show lane IDs", false);
        addPopupItem("Show generator queue", false);
        popMenu.addSeparator();
        addPopupItem("Show detectors", false);
        addPopupItem("Show detector IDs", false);
        popMenu.addSeparator();
        add(hSlider);
        add(vSlider);
        add(corner);
        // canvas layers
        layers = new ArrayList<layer>(6);
        for (int i=0; i<6; i++) {
            layers.add(new layer());
            add(layers.get(i));
        }
    }
    
    /**
     * Returns an interpolated color from a color map.
     * @param map Color map with values in the range 0-255. <tt>map[0][:]</tt> 
     * contains the red values, <tt>map[1][:]</tt> contains the green values and 
     * <tt>map[2][:]</tt> contains the blue values.
     * @param mapValues Ordered values of the colors at the indices <tt>[:]</tt> of the map.
     * @param value Value in the range <tt>mapValues</tt>.
     * @return Interpolated color from the given map.
     */
    public static Color fromMap(int[][] map, double[] mapValues, double value) {
        Color c = null;
        int m = map[0].length-1;
        boolean asc = true;
        if (mapValues[1]<mapValues[0]) {
            asc = false;
        }
        if ((asc && value<=mapValues[0]) || (!asc && value>=mapValues[m])) {
            // below lower bound
            c = new Color(map[0][0], map[1][0], map[2][0]);
        } else if ((asc && value>=mapValues[m]) || (!asc && value<=mapValues[0])) {
            // above upper bound
            c = new Color(map[0][m], map[1][m], map[2][m]);
        } else {
            int n=0;
            while ((asc && value>mapValues[n+1]) || (!asc && value<mapValues[n+1])) {
                n++;
            }
            double f = Math.abs((value-mapValues[n])/(mapValues[n+1]-mapValues[n]));
            c = new Color(
                    (int)((1-f)*map[0][n]+f*map[0][n+1]),
                    (int)((1-f)*map[1][n]+f*map[1][n+1]),
                    (int)((1-f)*map[2][n]+f*map[2][n+1]) );
        }
        return c;
    }
    
    /**
     * Returns an interpolated color from the default color map. The default 
     * color map is: dark red, red, yellow, green, dark green.
     * @param mapValues 5 ordered values of the colors in the map.
     * @param value Value in the range <tt>mapValues</tt>.
     * @return Interpolated color from the default map.
     */
    public static Color fromDefaultMap(double[] mapValues, double value) {
        return fromMap(defaultMap, mapValues, value);
    }
    
    /**
     * Returns 1 of 18 distinct colors depending on the value. For values above 
     * 18, the colors are repeated.
     * @param n Value for the color.
     * @return Color of the value.
     */
    public static Color nToColor(int n) {
        int m = n % 18;
        return new Color((float)defaultColors[m][0],
                (float)defaultColors[m][1], (float)defaultColors[m][2]);
    }
    
    /**
     * Method that fills the supplied panel with a legend displaying labels for
     * upto 6 different colors.
     * @param panel Panel to show the legend.
     * @param colors Colors for in the legend.
     * @param labels Labels for the colors.
     */
    public static void defaultLegend(JPanel panel, Color[] colors, String[] labels) {
        panel.setLayout(null);
        // absolute location info
        int h = (int) panel.getMaximumSize().getHeight();
        int w = (int) panel.getMaximumSize().getWidth();
        int b = 1;
        int width = (w-4*b)/3;
        int height = (h-3*b)/2;
        for (int k=0; k<colors.length; k++) {
            // indeces
            int j = (int) Math.floor(k/2)+1;
            int i = k-(j-1)*2+1;
            // square
            square s = new square(colors[k]);
            s.setBounds(j*b+(j-1)*width, i*b+(i-1)*height, height, height);
            panel.add(s);
            // label
            JLabel lab = new JLabel(labels[k]);
            lab.setBounds(j*b+(j-1)*width + b+height, i*b+(i-1)*height, width-b-height, height);
            lab.setFont(lab.getFont().deriveFont(9f).deriveFont(Font.PLAIN));
            panel.add(lab);
        }
    }
    
    /** 
     * Inner class to display a colored square in a legend.
     */
    private static class square extends javax.swing.JComponent {

        /** Color of the square. */
        private Color color;

        /**
         * Constructor that sets the color.
         * @param color Color of the square.
         */
        public square(Color color) {
            this.color = color;
        }

        /**
         * Paints the square.
         * @param g Graphics to paint with.
         */
        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    /**
     * Returns the width of the area inside possible scrollbars.
     * @return Width of the area inside possible scrollbars.
     */
    public int getNetWidth() {
        int w = super.getWidth();
        if (vSlider.isVisible()) {
            w = w - SLIDERWIDTH;
        }
        return w;
    }
    
    /**
     * Returns the height of the area inside possible scrollbars.
     * @return Height of the area inside possible scrollbars.
     */
    public int getNetHeight() {
        int h = super.getHeight();
        if (hSlider.isVisible()) {
            h = h - SLIDERWIDTH;
        }
        return h;
    }
    
    /**
     * Validates the zoom bounds, keeping the centre point at the centre.
     */
    private void validateZoom() {
        validateZoom(new Point(getNetWidth()/2, getNetHeight()/2), POLICY_POINT);
    }
    
    private void validateZoom(int sliderPolicy) {
        validateZoom(new Point(getNetWidth()/2, getNetHeight()/2), sliderPolicy);
    }
    
    private void validateZoom(Point point) {
        validateZoom(point, POLICY_POINT);
    }
    
    /**
     * Validates the zoom bounds and slider values following one of the slider
     * policies. For <tt>POLICY_KEEP</tt> the slider value is not changed which
     * is appropiate for if the canvas resized. This keeps the left and top of 
     * the network fixed, unless the value is outside the new valid value range.
     * For <tt>POLICY_CENTER</tt> the network is centered in the canvas which is 
     * appropiate for resetting. Finally for <tt>POLICY_POINT</tt> the location
     * at the mouse position will remain at that screen point which is 
     * appropiate for zooming.
     */
    private synchronized void validateZoom(Point point, int sliderPolicy) {
        
        // amount to the top/left of the point
        double left = (point.getX() + hSlider.getValue()) / canvasW;
        double top = (point.getY() + vSlider.getValue()) / canvasH;
        
        // get pixel size of network
        int w = getNetWidth();
        int h = getNetHeight();
        
        setPanelBounds(w, h);
        
        int valH = 0;
        if (sliderPolicy==POLICY_KEEP) {
            // temporarary
            valH = (int) ((centerPoint.x-minGlobalX)*canvasW/(maxGlobalX-minGlobalX) - getNetWidth()/2);
        } else if (sliderPolicy==POLICY_CENTER) {
            // center the network
            valH = (int) (-canvasX/2);
        } else if (sliderPolicy==POLICY_POINT) {
            // keep the global point under the mouse at the same location on screen
            valH = (int) (left*canvasW-point.getX());
        }
        int valV = 0;
        if (sliderPolicy==POLICY_KEEP) {
            // temporarary
            valV = (int) ((centerPoint.y-minGlobalY)*canvasH/(maxGlobalY-minGlobalY) - getNetHeight()/2);
        } else if (sliderPolicy==POLICY_CENTER) {
            // center the network
            valV = (int) (-canvasY/2);
        } else if (sliderPolicy==POLICY_POINT) {
            // keep the global point under the mouse at the same location on screen
            valV = (int) (top*canvasH-point.getY());
        }
        
        int min = 0;
        int max = 0;
        hSlider.setBounds(0, h, w, SLIDERWIDTH);
        min = (int)(-w/2);
        hSlider.setMinimum(min);
        max = (int)(canvasW+w/2);
        hSlider.setMaximum(max);
        max = max-w;
        valH = valH>max ? max : valH;
        valH = valH<min ? min : valH;
        hSlider.setValue(valH);
        hSlider.setVisibleAmount(w);
        hSlider.setBlockIncrement(w);

        vSlider.setBounds(w, 0, SLIDERWIDTH, h);
        min = (int)(-h/2);
        vSlider.setMinimum(min);
        max = (int)(canvasH+h/2);
        vSlider.setMaximum(max);
        max = max-h;
        valV = valV>max ? max : valV;
        valV = valV<min ? min : valV;
        vSlider.setValue(valV);
        vSlider.setVisibleAmount(h);
        vSlider.setBlockIncrement(h);

        corner.setBounds(w, h, SLIDERWIDTH, SLIDERWIDTH);
        
        // set the layers at the full size
        for (int i=0; i<layers.size(); i++) {
            layers.get(i).setBounds(0, 0, w, h);
        }
        
        // store center
        centerPoint = getGlobalPoint(w/2, h/2);
        
        // repaint
        repaint();
    }
    
    /**
     * Calculates the pixel width and height of the entire network and the 
     * amount that cannot be shown given the provided size.
     * @param w Net width of canvas [px].
     * @param h Net height of canvas [px].
     */
    private void setPanelBounds(int w, int h) {
        // get size at zoom level = 1
        double mppH = (maxGlobalX-minGlobalX) / (w-2*EDGE);
        double mppV = (maxGlobalY-minGlobalY) / (h-2*EDGE);
        double mpp = mppH > mppV ? mppH : mppV;
        canvasW = hZoomFactor*(maxGlobalX-minGlobalX)/mpp;
        canvasH = vZoomFactor*(maxGlobalY-minGlobalY)/mpp;
        canvasX = w-canvasW;
        canvasY = h-canvasH;
        DecimalFormat df = new DecimalFormat("0");
        gui.sizeLabel.setText("width="+df.format(mpp*w/hZoomFactor)+"m, height="
                +df.format(mpp*h/vZoomFactor)+"m");
    }
    
    /**
     * Translates a global position to a position within the canvas. This 
     * accounts for the zoom factors.
     * @param x Global x coordinates.
     * @param y Global y coordinates.
     * @return Point within the canvas.
     */
    public Point getPoint(double x, double y) {
        double x2 = -hSlider.getValue() + canvasW * (x-minGlobalX)/(maxGlobalX-minGlobalX);
        double y2 = -vSlider.getValue() + canvasH * (y-minGlobalY)/(maxGlobalY-minGlobalY);
        Point p = new Point();
        p.setLocation(x2, y2);
        return p;
    }
    
    /** 
     * Translates a position within the canvas to a global position.
     * @param x Pane x coordinates.
     * @param y Pane y coordinates.
     * @return Point in global coordinates.
     */
    private Point.Double getGlobalPoint(int x, int y) {
        double x2 = minGlobalX + (x+hSlider.getValue())*(maxGlobalX-minGlobalX)/canvasW;
        double y2 = minGlobalY + (y+vSlider.getValue())*(maxGlobalY-minGlobalY)/canvasH;
        return new Point.Double(x2, y2);
    }
    
    /**
     * Zooms in both horizontal and vertical direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoom(double factor, Point point) {
        hZoomFactor = hZoomFactor*factor;
        vZoomFactor = vZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Zooms in horizontal direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoomHorizontal(double factor, Point point) {
        hZoomFactor = hZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Zooms in vertical direction with a certain zoom factor.
     * @param factor Zoom factor.
     */
    protected void zoomVertical(double factor, Point point) {
        vZoomFactor = vZoomFactor*factor;
        validateZoom(point);
    }
    
    /**
     * Resets the zoom level to encapsulate everything.
     */
    protected void resetZoom() {
        hZoomFactor = 1;
        vZoomFactor = 1;
        validateZoom(POLICY_CENTER);
    }
    
    /**
     * Adds a grahpic to be drawn as a backdrop.
     * @param g Graphics object.
     */
    protected void addBackdrop(Graphic g) {
        layers.get(LAYER_BACKDROP).graphics.add(g);
    }
    
    /**
     * Adds a grahpic to be drawn as an overlay.
     * @param g Graphics object.
     */
    protected void addOverlay(Graphic g) {
        layers.get(LAYER_OVERLAY).graphics.add(g);
    }
    
    /**
     * Initializes the canvas which adds graphics for lanes and detectors aswell
     * as determining the global network bounding box.
     */
    protected synchronized void init() {
        jModel model = jModel.getInstance();
        // add graphics for lane objects
        for (int i=0; i< model.getNetwork().length; i++) {
            // first add taper (so the others are drawn over it)
            if (model.getNetwork()[i].getTaper() == model.getNetwork()[i]) {
                layers.get(LAYER_LANE).graphics.add(new LaneGraphic(this, model.getNetwork()[i]));
                layers.get(LAYER_LABEL).graphics.add(new LaneLabel(this, model.getNetwork()[i]));
                // add graphics for detector objects
                for (int j=0; j< model.getNetwork()[i].RSUcount(); j++) {
                    if (model.getNetwork()[i].getRSU(j) instanceof jDetector) {
                        layers.get(LAYER_DETECTOR).graphics.add(new DetectorGraphic(this, (jDetector) model.getNetwork()[i].getRSU(j)));
                        layers.get(LAYER_LABEL).graphics.add(new DetectorLabel(this, (jDetector) model.getNetwork()[i].getRSU(j)));
                    }
                }
                // add graphics for generator
                if (model.getNetwork()[i].getGenerator() != null) {
                    layers.get(LAYER_LABEL).graphics.add(new GeneratorQueue(this, model.getNetwork()[i]));
                }
            }
        }
        // now add non-tapers
        for (int i=0; i< model.getNetwork().length; i++) {
            if (model.getNetwork()[i].getTaper() != model.getNetwork()[i]) {
                layers.get(LAYER_LANE).graphics.add(new LaneGraphic(this, model.getNetwork()[i]));
                layers.get(LAYER_LABEL).graphics.add(new LaneLabel(this, model.getNetwork()[i]));
                // add graphics for detector objects
                for (int j=0; j< model.getNetwork()[i].RSUcount(); j++) {
                    if (model.getNetwork()[i].getRSU(j) instanceof jDetector) {
                        layers.get(LAYER_DETECTOR).graphics.add(new DetectorGraphic(this, (jDetector) model.getNetwork()[i].getRSU(j)));
                        layers.get(LAYER_LABEL).graphics.add(new DetectorLabel(this, (jDetector) model.getNetwork()[i].getRSU(j)));
                    }
                }
                // add graphics for generator
                if (model.getNetwork()[i].getGenerator() != null) {
                    layers.get(LAYER_LABEL).graphics.add(new GeneratorQueue(this, model.getNetwork()[i]));
                }
            }
        }
        // derive global ranges of network
        minGlobalX = Double.POSITIVE_INFINITY;
        maxGlobalX = Double.NEGATIVE_INFINITY;
        minGlobalY = Double.POSITIVE_INFINITY;
        maxGlobalY = Double.NEGATIVE_INFINITY;
        java.awt.geom.Rectangle2D.Double bounds;
        for (int i=0; i<layers.get(LAYER_LANE).graphics.size(); i++) {
            bounds = layers.get(LAYER_LANE).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
        for (int i=0; i<layers.get(LAYER_BACKDROP).graphics.size(); i++) {
            bounds = layers.get(LAYER_BACKDROP).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
        for (int i=0; i<layers.get(LAYER_OVERLAY).graphics.size(); i++) {
            bounds = layers.get(LAYER_OVERLAY).graphics.get(i).getGlobalBounds();
            if (bounds!=null) {
                minGlobalX = bounds.getMinX() < minGlobalX ? bounds.getMinX() : minGlobalX;
                maxGlobalX = bounds.getMaxX() > maxGlobalX ? bounds.getMaxX() : maxGlobalX;
                minGlobalY = bounds.getMinY() < minGlobalY ? bounds.getMinY() : minGlobalY;
                maxGlobalY = bounds.getMaxY() > maxGlobalY ? bounds.getMaxY() : maxGlobalY;
            }
        }
    }
    
    /**
     * Updates the canvas to show the current status of the model.
     */
    protected synchronized void update() {
        jModel model = jModel.getInstance();
        // copy vehicle array
        List<Vehicle> vehs = model.getVehicles();
        // loop existing graphics
        java.util.Iterator<Graphic> iter = layers.get(LAYER_VEHICLE).graphics.iterator();
        while (iter.hasNext()) {
            VehicleGraphic g = (VehicleGraphic) iter.next();
            if (!g.exists()) {
                iter.remove();
            } else {
                vehs.remove(g.getVehicle());
            }
        }
        // add graphics objects for all vehicles still in vehs (new vehicles)
        for (int i=0; i<vehs.size(); i++) {
            layers.get(LAYER_VEHICLE).graphics.add(new VehicleGraphic(this, vehs.get(i)));
        }
        // time
        Date date = model.currentTime();
        if (date==null) {
            gui.timeLabel.setText("t="+model.getT()+"s");
        } else {
            DateFormat df = new SimpleDateFormat("HH:mm:ss");
            gui.timeLabel.setText("t="+df.format(date));
        }
        // number of vehicles
        gui.nVehicles.setText(model.getVehicles().size()+" vehs");
        repaint();
    }
    
    /**
     * Creates an image of the canvas.
     * @return Image of the canvas.
     */
    protected synchronized BufferedImage createImage() {
        // assure a size as a multiple of 4
        int w = (int) Math.ceil((double)getNetWidth()/4)*4;
        int h = (int) Math.ceil((double)getNetHeight()/4)*4;
        BufferedImage bi = new BufferedImage(
                w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        paintComponent(g);
        for (int i=layers.size()-1; i>=0; i--) {
            layers.get(i).paintComponent(g);
        }
        return bi; 
    }

    protected synchronized void createPDF(String outputPath, String frameName) {
        int w = (int) Math.ceil((double)getNetWidth()/4)*4;
        int h = (int) Math.ceil((double)getNetHeight()/4)*4;
        FileOutputStream os = null;
        Document d = new Document();
        try {
            os = new FileOutputStream(new File(outputPath,frameName));
            PdfWriter writer = PdfWriter.getInstance(d, os);
            d.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate template = cb.createTemplate(w, h);
            Graphics2D g = template.createGraphics(w, h);
            g.setStroke(new BasicStroke(0f));
            Font font = g.getFont();
            g.setFont(font.deriveFont(0.05f));
            paintComponent(g);
            for (int i=layers.size()-1; i>=0; i--) {
                layers.get(i).paintComponent(g);
            }
            cb.addTemplate(template, 2, 2);
            g.dispose();
            os.flush();
        } catch (DocumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            d.close();
        }
    }
    
    /**
     * Class which forms a layer of graphics objects. These are painted 
     * automatichally as the layer is a child of the network canvas.
     */
    private class layer extends JPanel {
        
        /** Array of graphics of this layer. */
        private ArrayList<Graphic> graphics = new ArrayList<Graphic>();
        
        /**
         * Default constructor which sets the layer transparent.
         */
        public layer() {
            setOpaque(false);
        }
        
        /**
         * Paints the graphics of this layer.
         * @param g Graphics object of the layer.
         */
        @Override
        public void paintComponent(Graphics g) {
            jModel model = jModel.getInstance();
            // paint graphics of this layer
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i=0; i<graphics.size(); i++) {
                if (graphics.get(i).exists()) {
                    graphics.get(i).paint(g, NetworkCanvas.this);
                }
            }
        }
    }

    /**
     * Listener for mouse actions on the canvas.
     */
    private class GeneralMouseListener implements
            MouseListener,
            MouseMotionListener {
        
        /** Last point during a drag. */
        private Point lastDragPoint;
        
        /** Initial point of a drag. */
        private Point initDragPoint;
        
        /** The mouse button number that was last clicked. */
        private int button;
        
        /**
         * Resets the zoom in case of a double click.
         * @param e Mouse event.
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount()==2) {
                resetZoom();
            } else if (e.getButton()==3 && e.getClickCount()==1) {
                popMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        /**
         * Sets initial information for if the press becomes a drag.
         * @param e Mouse event.
         */
        public void mousePressed(MouseEvent e) {
            lastDragPoint = e.getPoint();
            initDragPoint = e.getPoint();
            button = e.getButton();
        }

        /** Empty. */
        public void mouseReleased(MouseEvent e) {}

        /** Empty. */
        public void mouseEntered(MouseEvent e) {}

        /** Empty. */
        public void mouseExited(MouseEvent e) {}

        /**
         * Pans or zooms, depending on the button that was last pressed.
         * @param e Mouse event.
         */
        public void mouseDragged(MouseEvent e) {
            int dx = lastDragPoint.x-e.getPoint().x;
            int dy = lastDragPoint.y-e.getPoint().y;
            if (button== MouseEvent.BUTTON1) {
                hSlider.setValue(hSlider.getValue()+dx);
                vSlider.setValue(vSlider.getValue()+dy);
            } else if (button== MouseEvent.BUTTON3) {
                double factor = 0;
                if (e.isControlDown()) {
                    factor = 1+.2*(Math.pow(1.01, dy)-1);
                } else{
                    factor = Math.pow(1.01, dy);
                }
                if (e.isShiftDown()) {
                    zoomVertical(factor, initDragPoint);
                } else if (e.isAltDown()) {
                    zoomHorizontal(factor, initDragPoint);
                } else {
                    zoom(factor, initDragPoint);
                }
            }
            lastDragPoint = e.getPoint();
        }

        /**
         * Updates the current position information.
         * @param e Mouse event.
         */
        public void mouseMoved(MouseEvent e) {
            Point.Double p = getGlobalPoint(e.getX(), e.getY());
            DecimalFormat df = new DecimalFormat("0");
            gui.posLabel.setText("x="+df.format(p.getX())+"m, y="+df.format(p.getY())+"m");
        }
    }
    
    /**
     * Adds a new check box menu item to the popup menu. After it is selected or
     * deselected the network canvas is repainted.
     * @param label Label for the menu item.
     * @param checked Initial checked state.
     */
    protected void addPopupItem(String label, boolean checked) {
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(label, checked);
        item.addActionListener(
            new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    repaint();
                }
            }
        );
        popMenu.add(item);
    }
    
    /**
     * Return whether the popup menu item with given label is selected.
     * @param label Label of requested menu item.
     * @return Whether the popup menu item with given label is selected.
     */
    public boolean popupItemChecked(String label) {
        for (int i=0; i<popMenu.getSubElements().length; i++) {
            javax.swing.JCheckBoxMenuItem item = (javax.swing.JCheckBoxMenuItem) popMenu.getSubElements()[i];
            if (item.getText().equals(label) && item.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public boolean showDestinations() {
        return showDestinations;
    }

    public void setShowDestinations(boolean showDestinations) {
        this.showDestinations = showDestinations;
    }

    public jModelGUI getGui() {
        return gui;
    }
}