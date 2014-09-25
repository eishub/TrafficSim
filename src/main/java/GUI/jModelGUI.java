package GUI;

import GUI.vehicle.DefaultVehicleColors;
import GUI.vehicle.VehicleColor;
import microModel.jModel;
import microModel.settings.BuiltInSettings;
import microModel.settings.jSettings;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;

/**
 * Window in which a model can be visualized and run. By default all lanes,
 * detectors and vehicles are displayed.
 */
public class jModelGUI extends JFrame {

    /** Used to log debug information */
    private final Logger logger = Logger.getLogger(jModelGUI.class);

    /** Play button. */
    protected JToggleButton play;
    
    /** Step button. */
    protected JButton step;
    
    /** Simulation speed selection. */
    protected JComboBox simSpeed;
    
    /** Visualization step selection. */
    protected JComboBox visStep;
    
    /** Label to display the model time. */
    protected JLabel timeLabel;
    
    /** Label to display the number of vehicles. */
    protected JLabel nVehicles;
    
    /** Label to display the simulation speed. */
    protected JLabel speedLabel;
    
    /** Label to display the mouse position. */
    protected JLabel posLabel;
    
    /** Label to display the screen size. */
    protected JLabel sizeLabel;

    /** Vehicle color selection. */
    protected JComboBox vehCol;
    
    /** Vehicle color information/legend. */
    protected JPanel vehColInfo;
    
    /** Simulation Recording button. */
    protected JToggleButton recordSimulation;

    /** Trajectory recording button     */
    protected JToggleButton recordTrajectories;

    /** Trajectory recording button     */
    protected JToggleButton recordDetectors;

    /** Frame number. */
    private int frame = 0;
    
    /** Directory to save frame images. */
    private String frameDir;
    
    /** 
     * Boolean that is set true when the window is closed, which stops the 
     * simulation. 
     */
    private boolean isDisposed = false;
    
    /** Thread that perform the model run. */
    private Thread runThread;
    
    /** Network canvas, which displays the network. */
    private NetworkCanvas canvas;
    
    /**
     * Constructor which generates the window content.
     */
    public jModelGUI() {
        // window title
        super("jModel");
        
        // set icon
        try {
            setIconImage(ImageIO.read(getClass().getResource("/merge.png")));
        } catch (Exception e) {
            
        }
        
        // for use in Matlab, otherwise closes the entire JVM, including Matlab
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // set output dir for frames by output dir in model
        jSettings settings = jSettings.getInstance();
        File f = new File(settings.get(BuiltInSettings.OUTPUT_PATH), "frames");
        try {
            f.mkdir();
            frameDir = f.getPath();
        } catch (Exception e) {
            logger.debug("Could not create directory to export frames.");
        }

        // listener at frame level, within the NetworkCanvas does not work in Matlab
        addMouseWheelListener(new MouseWheelListener() {
            /**
             * Converts the point in the event to the coordinates system of the
             * network canvas and invokes the appropiate zoom method.
             * @param e Mouse wheel event.
             */
            public void mouseWheelMoved(MouseWheelEvent e) {
                Point point = SwingUtilities.convertPoint(jModelGUI.this, e.getPoint(), canvas);
                if (canvas.contains(point)) {
                    double factor = 0;
                    if (e.isControlDown()) {
                        factor = 1+.2*(Math.pow(1.05, -e.getUnitsToScroll())-1);
                    } else{
                        factor = Math.pow(1.05, -e.getUnitsToScroll());
                    }
                    if (e.isShiftDown()) {
                        canvas.zoomVertical(factor, point);
                    } else if (e.isAltDown()) {
                        canvas.zoomHorizontal(factor, point);
                    } else {
                        canvas.zoom(factor, point);
                    }
                }
            }
        });
        
        // toolbar component heights
        int hButton = 25;
        int h = 21;
        
        // toolbar
        JToolBar toolBar = new JToolBar("Simulation control");
        toolBar.setFloatable(false);
        
        // pause button
        play = new JToggleButton(getIcon("play"));
        play.setMinimumSize(new Dimension(hButton, hButton));
        play.setPreferredSize(new Dimension(hButton, hButton));
        play.setMaximumSize(new Dimension(hButton, hButton));
        play.setToolTipText("Pause or restart the simulation");
        play.addActionListener(new ActionListener() {
            /**
             * Starts or stops the simulation, enables or disables the step 
             * button and changes the play button icon.
             * @param e Action event.
             */
            public void actionPerformed(ActionEvent e) {
                if (!play.isSelected()) {
                    play.setIcon(getIcon("play"));
                    step.setEnabled(true);
                } else {
                    play.setIcon(getIcon("pause"));
                    step.setEnabled(false);
                    // Runner thread
                    runThread = new Thread(new runner());
                    runThread.start();
                }
            }
        });
        play.setSelected(false);
        toolBar.add(play);
        
        // step button
        step = new JButton(getIcon("step"));
        step.setMinimumSize(new Dimension(hButton, hButton));
        step.setPreferredSize(new Dimension(hButton, hButton));
        step.setMaximumSize(new Dimension(hButton, hButton));
        step.setToolTipText("Perform a single step during pause");
        step.addActionListener(new ActionListener() {
            /**
             * Performs a single step, updates the canvas and writes a frame.
             * @param e Action event.
             */
            public void actionPerformed(ActionEvent e) {
                // run the model
                jModel model = jModel.getInstance();
                model.run(1);
                // visualize the model
                canvas.update();
                // write movie frame
                writeFrame();
                // stop if duration has passed
                if (model.isSimulationFinished()) {
                    close();
                }
            }
        });
        toolBar.add(step);
        toolBar.addSeparator();
        
        // simulation speed
        simSpeed = new JComboBox(simulationSpeed.values());
        simSpeed.setSelectedItem(simulationSpeed.X1);
        simSpeed.setEditable(true);
        simSpeed.setMinimumSize(new Dimension(40, h));
        simSpeed.setPreferredSize(new Dimension(80, h));
        simSpeed.setMaximumSize(new Dimension(80, h));
        simSpeed.setToolTipText("Simulation speed");
        simSpeed.addActionListener(new ActionListener() {
            /**
             * Sets the visualization step selection off, or on at "max" value.
             * @param e Action event.
             */
            public void actionPerformed(ActionEvent e) {
                if (simSpeed.getSelectedItem().toString().toLowerCase().startsWith("max")) {
                    visStep.setEnabled(true);
                } else {
                    visStep.setEnabled(false);
                }
            }
        });
        toolBar.add(simSpeed);
        
        // visualization step
        visStep = new JComboBox(simulationStep.values());
        visStep.setSelectedItem(simulationStep.S1);
        visStep.setEditable(true);
        visStep.setEnabled(false);
        visStep.setMinimumSize(new Dimension(40, h));
        visStep.setPreferredSize(new Dimension(80, h));
        visStep.setMaximumSize(new Dimension(80, h));
        visStep.setToolTipText("Visualization step");
        toolBar.add(visStep);
        toolBar.addSeparator();
        
        // vehicle color
        vehCol = new JComboBox(DefaultVehicleColors.values());
        vehCol.setMinimumSize(new Dimension(50, h));
        vehCol.setPreferredSize(new Dimension(150, h));
        vehCol.setMaximumSize(new Dimension(150, h));
        vehCol.setToolTipText("Vehicle color");
        vehCol.addActionListener(new ActionListener() {
            /**
             * Updates the vehicle color information (e.g. legend) and repaints 
             * the network canvas.
             * @param e Action event.
             */
            public void actionPerformed(ActionEvent e) {
                // remove contents of vehicle color information
                vehColInfo.removeAll();
                // set default layout for user defined classes
                vehColInfo.setLayout(new java.awt.FlowLayout());
                // call the selected objects setInfo() method
                VehicleColor vehColObject = (VehicleColor) vehCol.getSelectedItem();
                vehColObject.setInfo(vehColInfo);
                // in case of destination selected, let the lanes show it
                if (vehColObject!= DefaultVehicleColors.DESTINATION) {
                    canvas.setShowDestinations(false);
                } else {
                    canvas.setShowDestinations(true);
                }
                // re-layout and repaint the vehicle color information
                vehColInfo.validate();
                vehColInfo.repaint();
                // repaint the network canvas
                canvas.repaint();
            }
        });
        toolBar.add(vehCol);
        
        // vehicle color info
        vehColInfo = new JPanel();
        vehColInfo.setMinimumSize(new Dimension(50, hButton));
        vehColInfo.setPreferredSize(new Dimension(225, hButton));
        vehColInfo.setMaximumSize(new Dimension(225, hButton));
        vehColInfo.setToolTipText("Vehicle color info");
        vehColInfo.setOpaque(false);
        toolBar.add(vehColInfo);
        toolBar.addSeparator();
        // show the info
        VehicleColor vc = (VehicleColor) vehCol.getSelectedItem();
        vc.setInfo(vehColInfo);
        
        // record button
        recordSimulation = new JToggleButton(getIcon("rec"));
        recordSimulation.setMinimumSize(new Dimension(hButton, hButton));
        recordSimulation.setPreferredSize(new Dimension(hButton, hButton));
        recordSimulation.setMaximumSize(new Dimension(hButton, hButton));
        recordSimulation.setToolTipText("Record each visualization to image");
        recordSimulation.addActionListener(new ActionListener() {
            /**
             * Sets the window not resizable during recording to preserve an 
             * equal frame size.
             */
            public void actionPerformed(ActionEvent e) {
                if (recordSimulation.isSelected()) {
                    setResizable(false);
                } else {
                    setResizable(true);
                }
            }
        });
        recordSimulation.setSelected(false);
        toolBar.add(recordSimulation);

        // Detector record button
        recordDetectors = new JToggleButton(getIcon("detector"));
        recordDetectors.setMinimumSize(new Dimension(hButton, hButton));
        recordDetectors.setPreferredSize(new Dimension(hButton, hButton));
        recordDetectors.setMaximumSize(new Dimension(hButton, hButton));
        recordDetectors.setToolTipText("Record Detector data");
        recordDetectors.addActionListener(new ActionListener() {
            /**
             * Sets the window not resizable during recording to preserve an
             * equal frame size.
             */
            public void actionPerformed(ActionEvent e) {
                if (recordDetectors.isSelected()) {
                    jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, true);
                } else {
                    jSettings.getInstance().put(BuiltInSettings.DEBUG_DETECTOR, false);
                }
            }
        });
        recordDetectors.setSelected(jSettings.getInstance().get(BuiltInSettings.DEBUG_DETECTOR));
        toolBar.add(recordDetectors);

        // Trajectory record button
        recordTrajectories = new JToggleButton(getIcon("trajectory"));
        recordTrajectories.setMinimumSize(new Dimension(hButton, hButton));
        recordTrajectories.setPreferredSize(new Dimension(hButton, hButton));
        recordTrajectories.setMaximumSize(new Dimension(hButton, hButton));
        recordTrajectories.setToolTipText("Record Vehicle Trajectories");
        recordTrajectories.addActionListener(new ActionListener() {
            /**
             * Sets the window not resizable during recording to preserve an
             * equal frame size.
             */
            public void actionPerformed(ActionEvent e) {
                if (recordTrajectories.isSelected()) {
                    jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, true);
                } else {
                    jSettings.getInstance().put(BuiltInSettings.DEBUG_TRAJECTORY, false);
                }
            }
        });
        recordTrajectories.setSelected(jSettings.getInstance().get(BuiltInSettings.DEBUG_TRAJECTORY));
        toolBar.add(recordTrajectories);

        // status bar
        hButton = 15;
        JToolBar statusBar = new JToolBar("Status bar");
        statusBar.setFloatable(false);
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        
        // time label
        timeLabel = new JLabel();
        timeLabel.setMinimumSize(new Dimension(40, hButton));
        timeLabel.setPreferredSize(new Dimension(80, hButton));
        timeLabel.setMaximumSize(new Dimension(80, hButton));
        timeLabel.setToolTipText("Simulation time");
        statusBar.add(timeLabel);
        statusBar.addSeparator();
        
        // time label
        nVehicles = new JLabel();
        nVehicles.setMinimumSize(new Dimension(40, hButton));
        nVehicles.setPreferredSize(new Dimension(80, hButton));
        nVehicles.setMaximumSize(new Dimension(80, hButton));
        nVehicles.setToolTipText("Number of vehicles");
        statusBar.add(nVehicles);
        statusBar.addSeparator();
        
        // speed label
        speedLabel = new JLabel();
        speedLabel.setMinimumSize(new Dimension(40, hButton));
        speedLabel.setPreferredSize(new Dimension(80, hButton));
        speedLabel.setMaximumSize(new Dimension(80, hButton));
        speedLabel.setToolTipText("Simulation speed");
        statusBar.add(speedLabel);
        statusBar.addSeparator();
        
        // position label
        posLabel = new JLabel();
        posLabel.setMinimumSize(new Dimension(70, hButton));
        posLabel.setPreferredSize(new Dimension(140, hButton));
        posLabel.setMaximumSize(new Dimension(140, hButton));
        posLabel.setToolTipText("Mouse position");
        statusBar.add(posLabel);
        statusBar.addSeparator();
        
        // window size label
        sizeLabel = new JLabel();
        sizeLabel.setMinimumSize(new Dimension(100, hButton));
        sizeLabel.setPreferredSize(new Dimension(200, hButton));
        sizeLabel.setMaximumSize(new Dimension(200, hButton));
        sizeLabel.setToolTipText("Windows size");
        statusBar.add(sizeLabel);
        statusBar.addSeparator();
        
        // canvas
        canvas = new NetworkCanvas(this);
        canvas.init();
        
        // frame
        setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            @Override 
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        setSize(new Dimension(700, 400));
        add(toolBar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        // final preparation
        setVisible(true);
        canvas.resetZoom();
        
        jModel.getInstance().setGui(this);
    }
    
    /**
     * Adds a user-defined vehicle color indication.
     * @param userVehCol User-defined vehicle color indication.
     */
    public void addVehicleColor(VehicleColor userVehCol) {
        vehCol.addItem(userVehCol);
    }
    
    /**
     * Adds a grahpic to be drawn as a backdrop.
     * @param g Graphics object.
     */
    public void addBackdrop(Graphic g) {
        canvas.addBackdrop(g);
    }
    
    /**
     * Adds a grahpic to be drawn as an overlay.
     * @param g Graphics object.
     */
    public void addOverlay(Graphic g) {
        canvas.addOverlay(g);
    }
    
    /**
     * Adds a new check box menu item to the popup menu. After it is selected or
     * deselected the network canvas is repainted.
     * @param label Label for the menu item.
     * @param checked Initial checked state.
     */
    public void addPopupItem(String label, boolean checked) {
        canvas.addPopupItem(label, checked);
    }
    
    /**
     * Sets the background color of the network canvas.
     * @param r Red [0...255].
     * @param g Green [0...255].
     * @param b Blue [0...255].
     */
    public void setBackground(int r, int g, int b) {
        canvas.setBackground(new Color(r, g, b));
    }
    
    /**
     * Draws and returns any of a number of icons.
     * @param label Label of icon: <tt>"play"</tt>, <tt>"pause"</tt>, 
     * <tt>"step"</tt> or <tt>"recordSimulation"</tt>.
     * @return Requested icon.
     */
    private ImageIcon getIcon(String label) {
        BufferedImage im =
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) im.createGraphics();
        if (label.equals("play")) {
            g2.setColor(new Color(61, 152, 222));
            g2.fillPolygon(new int[] {6, 6, 12}, new int[] {1, 13, 7}, 3);
        } else if (label.equals("pause")) {
            g2.setColor(new Color(61, 152, 222));
            g2.fillPolygon(new int[] {4, 4, 7, 7}, new int[] {2, 13, 13, 2}, 4);
            g2.fillPolygon(new int[] {10, 10, 13, 13}, new int[] {2, 13, 13, 2}, 4);
        } else if (label.equals("step")) {
            g2.setColor(new Color(61, 152, 222));
            g2.fillPolygon(new int[] {6, 6, 10, 6, 6, 12}, new int[] {1, 3, 7, 11, 13, 7}, 6);
        } else if (label.equals("rec")) {
            g2.setColor(new Color(236, 127, 44));
            g2.fillOval(3, 3, 10, 10);
        } else if (label.equals("trajectory")) {
            g2.setColor(new Color(236, 200, 43));
            g2.fillOval(3, 3, 10, 10);
        } else if (label.equals("detector")) {
            g2.setColor(new Color(255, 192, 238));
            g2.fillOval(3, 3, 10, 10);
        } else if (label.equals("icon")) {
            
        }
        return new ImageIcon(im);
    }
    
    private BufferedImage getFrameIcon() {
        BufferedImage im =
                new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) im.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 97, 166));
        g2.fillRoundRect(0, 0, 31, 31, 4, 4);
        g2.setColor(new Color(255, 255, 255));
        g2.drawRoundRect(1, 1, 28, 28, 4, 4);
        g2.drawLine(3, 15, 13, 5);
        g2.drawLine(9, 21, 19, 11);
        g2.drawLine(15, 27, 15, 17);
        g2.drawLine(22, 27, 22, 20);
        g2.drawLine(22, 20, 25, 17);
        g2.fillPolygon(new int[] {11, 16, 16}, new int[] {3, 8, 3}, 3);
        g2.fillPolygon(new int[] {17, 22, 22}, new int[] {9, 14, 9}, 3);
        g2.fillPolygon(new int[] {23, 28, 28}, new int[] {15, 20, 15}, 3);
        return im;
    }
    
    /**
     * Writes a frame, if appropiate.
     */
    private void writeFrame() {
        if (recordSimulation.isSelected() && frameDir!=null) {
            DecimalFormat nf = new DecimalFormat("00000");
            File f = new File(frameDir);
            f.mkdirs();
            if ("PDF".compareTo(jSettings.getInstance().get(BuiltInSettings.SIMULATION_RECORD_FORMAT)) == 0) {
                canvas.createPDF(frameDir, "frame"+nf.format(frame)+".pdf");
            }
            else {
                f = new File(frameDir, "frame"+nf.format(frame) +".png");
                f.getParentFile().mkdirs();

                BufferedImage im = canvas.createImage();

                try {
                    ImageIO.write(im, "png", f);
                } catch (Exception ex) {

                }
            }
            frame++;
        }
    }
    
    /**
     * Lets a thread wait untill the simulation has finished.
     */
    public synchronized void waitFor() {
        try {
            wait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Closes the figure by waiting for the model run to stop, storing the 
     * required data and notifying any waiting thread.
     */
    private void close() {
        isDisposed = true; // trigger the runThread to stop
        if (runThread!=null && runThread!=Thread.currentThread()) {
            try {
                runThread.join(100);
            } catch (Exception ex) {
                
            }
        }
        jModel.getInstance().saveLogsToDisk();
        setVisible(false);
        dispose();
        synchronized (this) {
            notifyAll();
        }
    }
    
    public void forceClose() {
        isDisposed = true; // trigger the runThread to stop
        if (runThread!=null && runThread!=Thread.currentThread()) {
            try {
                runThread.join(100);
            } catch (Exception ex) {
                
            }
        }
        jModel.getInstance().saveLogsToDisk();
        setVisible(false);
        dispose();
        synchronized (this) {
            notifyAll();
        }
    	
    }
    
    
    /**
     * Runnable that runs the model and updates the GUI.
     */
    private class runner implements java.lang.Runnable {
        
        private double aggTSim;
        
        /**
         * Runs the simulation while applicable.
         */
        public void run() {
            jModel model = jModel.getInstance();
            while (play.isSelected() && !isDisposed && !model.isSimulationFinished()) {
                
                // store time to keep track of speed
                long t = System.currentTimeMillis();
                 
                // get the selected speed
                Object obj = simSpeed.getSelectedItem();
                boolean isMax = false;
                double speed = 1;
                if (obj instanceof simulationSpeed) {
                    // a predefined enum is selected
                    simulationSpeed item = (simulationSpeed) simSpeed.getSelectedItem();
                    if (item==simulationSpeed.MAXIMUM) {
                        speed = Double.POSITIVE_INFINITY;
                        isMax = true;
                    } else {
                        speed = Double.parseDouble(item.toString().substring(1));
                    }
                } else if (obj.toString().toLowerCase().startsWith("max")) {
                    // user typed something that starts with "max"
                    speed = Double.POSITIVE_INFINITY;
                    isMax = true;
                } else {
                    // user typed something else
                    String str = obj.toString();
                    // remove possible first "x"
                    if (str.startsWith("x") || str.startsWith("X")) {
                        str = str.substring(1);
                    }
                    // try to translate to a number
                    try {
                        speed = Double.parseDouble(str);
                    } catch (Exception e) {
                        // keep speed = 1
                    }
                    // no negative speed
                    speed = speed < 0 ? 1 : speed;
                }
                
                // get the number of steps to perform
                int n = 1;
                if (isMax) {
                    // at maximum speed, the number of steps can be set
                    try {
                        // predefined enum or user typed string, both to string
                        n = Integer.parseInt(visStep.getSelectedItem().toString());
                    } catch (Exception e) {
                        // keep speed = 1
                    }
                }   
                
                // run the model
                model.run(n);
                // visualize the model
                canvas.update();
                // write movie frame
                writeFrame();
                
                // sleep any remaining time for the correct simulation speed
                boolean update = true;
                long dtAct = System.currentTimeMillis()-t;
                if (!isMax) {
                    long dtReq = (long) (1000*(n* model.getStepSize() /speed));
                    if (dtReq>dtAct) {
                        try {
                            Thread.sleep((long)(dtReq-dtAct));
                        } catch (Exception e) {

                        }
                        dtAct = dtReq; // actual duration after wait
                    } 
                    speed = speed * dtReq/dtAct;
                } else {
                    aggTSim = aggTSim + 1000*n* model.getStepSize();
                    if (dtAct>0) {
                        speed = aggTSim / (double) dtAct;
                        aggTSim = 0;
                    } else {
                        update = false;
                    }
                }
                
                if (update) {
                    java.text.DecimalFormatSymbols dfs = new java.text.DecimalFormatSymbols();
                    dfs.setDecimalSeparator('.');
                    DecimalFormat df;
                    if (speed >= 100) {
                        df = new DecimalFormat("0", dfs);
                    } else if (speed >= 10) {
                        df = new DecimalFormat("0.0", dfs);
                    } else {
                        df = new DecimalFormat("0.000", dfs);
                    }
                    speedLabel.setText(df.format(speed)+"x");
                }
            }
            // close if stopped for simulation end
            if (model.isSimulationFinished() && !isDisposed) {
                close();
            }
        }
    }
    
    /**
     * Default simulation speeds enumeration.
     */
    private enum simulationSpeed {
        /** 0.5 times reality. */
        X05, 
        /** 1 time reality. */
        X1, 
        /** 2 times reality. */
        X2, 
        /** 5 times reality. */
        X5, 
        /** 10 times reality. */
        X10, 
        /** 25 times reality. */
        X25, 
        /** 100 times reality. */
        X100, 
        /** Maximum speed (does not wait between steps). */
        MAXIMUM;

        /**
         * Returns the name as a more readable string.
         * @return More readable string of the name.
         */
        @Override
        public java.lang.String toString() {
            return name().replace("X0", "X0.").toLowerCase();
        }
    }
    
    /**
     * Default simulation steps enumeration.
     */
    private enum simulationStep {
        /** Every time step. */
        S1, 
        /** Every 2 time steps. */
        S2, 
        /** Every 5 time steps. */
        S5, 
        /** Every 10 time steps. */
        S10, 
        /** Every 25 time steps. */
        S25, 
        /** Every 100 time steps. */
        S100;

        /**
         * Returns the name as a more readable string.
         * @return More readable string of the name.
         */
        @Override
        public String toString() {
            if (name().startsWith("S")) {
                return name().substring(1);
            } else {
                return name().toLowerCase();
            }
        }
    }

    public JComboBox getVehCol() {
        return vehCol;
    }
}