/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.objects.WPObjectRenderer;

/**
 *
 * @author pepijn
 */
class Previewer extends JPanel implements PropertyChangeListener {
    public Previewer(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        setLayout(new BorderLayout());
        add(label);
        setPreferredSize(new Dimension(200, -1));
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setObject(WPObject object) {
        if (object == null) {
            label.setIcon(null);
        } else {
            WPObjectRenderer renderer = new WPObjectRenderer(object, colourScheme, 8);
            BufferedImage image = renderer.render();
            if ((image.getWidth() > 200) || (image.getHeight() > 200)) {
                image = rescale(image);
            }
            label.setIcon(new ImageIcon(image));
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File file = (File) evt.getNewValue();
            if (file == null) {
                return;
            }
            WPObject object = null;
            try {
                String filename = file.getName().toLowerCase();
                if (filename.toLowerCase().endsWith(".bo2")) {
                    object = Bo2Object.load(file);
                } else if (filename.toLowerCase().endsWith(".schematic")) {
                    object = Schematic.load(file);
                }
                setObject(object);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Exception while trying to generate preview for " + file, t);
            }
        }
    }

    private BufferedImage rescale(BufferedImage image) {
        float ratio = (float) image.getHeight() / image.getWidth();
        int newWidth;
        int newHeight;
        if (image.getWidth() > image.getHeight()) {
            newWidth = 200;
            newHeight = (int) (200 * ratio);
        } else {
            newHeight = 200;
            newWidth = (int) (200 / ratio);
        }
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledImage.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(image, 0, 0, newWidth, newHeight, this);
        } finally {
            g2.dispose();
        }
        return scaledImage;
    }
    
    private final JLabel label;
    private final ColourScheme colourScheme;

    private static final Logger logger = Logger.getLogger(Previewer.class.getName());
    private static final long serialVersionUID = 1L;
}