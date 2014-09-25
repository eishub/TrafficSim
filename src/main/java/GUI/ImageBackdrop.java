package GUI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageBackdrop implements Graphic {
    
    private BufferedImage im;
    private double x;
    private double y;
    private double w;
    private double h;
    
    // Constructor that loads the image and sets the span of the image
    public ImageBackdrop(String im, double x, double y, double w, double h) {
        try {
            this.im = ImageIO.read(getClass().getResource(im).openStream());
        } catch (Exception e) {
            System.err.println("Image '"+im+"' could not be read.");
        }
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // Not an object that can disappear
    public boolean exists() {
        return true;
    }

    // Not part of the physical world
    public Rectangle.Double getGlobalBounds() {
        return null;
    }

    // Paints the image if popup item is checked
    public void paint(Graphics g, NetworkCanvas canvas) {
        if (canvas.popupItemChecked("Show backdrop")) {
            java.awt.Point p1 = canvas.getPoint(x, y);
            java.awt.Point p2 = canvas.getPoint(x+w, y+h);
            g.drawImage(im, p1.x, p1.y, p2.x-p1.x, p2.y-p1.y, null);
        }
    }
}