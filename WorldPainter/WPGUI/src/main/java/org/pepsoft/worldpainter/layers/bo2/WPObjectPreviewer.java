/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import org.pepsoft.worldpainter.dynmap.DynMapPreviewer;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 *
 * @author pepijn
 */
public class WPObjectPreviewer extends JPanel implements PropertyChangeListener {
    public WPObjectPreviewer() {
        previewer = new DynMapPreviewer();
        setLayout(new BorderLayout());
        add(previewer, BorderLayout.CENTER);
        setPreferredSize(new Dimension(200, -1));
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setObject(WPObject object) {
        previewer.setObject(object);
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
                logger.error("Exception while trying to generate preview for " + file, t);
            }
        }
    }
    
    private final DynMapPreviewer previewer;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPObjectPreviewer.class);
    private static final long serialVersionUID = 1L;
}