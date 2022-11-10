/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import org.pepsoft.worldpainter.dynmap.DynmapPreviewer;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.CustomObjectManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 *
 * @author pepijn
 */
public class WPObjectPreviewer extends JPanel implements PropertyChangeListener {
    public WPObjectPreviewer() {
        previewer = new DynmapPreviewer();
        setLayout(new BorderLayout());
        add(previewer, BorderLayout.CENTER);
        setPreferredSize(new Dimension(200, -1));
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public org.pepsoft.worldpainter.Dimension getDimension() {
        return dimension;
    }

    public void setDimension(org.pepsoft.worldpainter.Dimension dimension) {
        this.dimension = dimension;
    }

    public void setObject(WPObject object) {
        previewer.setObject(object, dimension);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File file = (File) evt.getNewValue();
            if ((file == null) || (! file.isFile())) {
                return;
            }
            try {
                setObject(CustomObjectManager.getInstance().loadObject(file));
            } catch (Throwable t) {
                logger.error("Exception while trying to generate preview for " + file, t);
            }
        }
    }
    
    private final DynmapPreviewer previewer;
    private org.pepsoft.worldpainter.Dimension dimension;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPObjectPreviewer.class);
    private static final long serialVersionUID = 1L;
}