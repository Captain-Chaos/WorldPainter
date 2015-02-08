/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.awt.Window;
import org.pepsoft.worldpainter.WorldPainterDialog;

/**
 *
 * @author pepijn
 */
public abstract class CustomLayerDialog<T extends CustomLayer> extends WorldPainterDialog {
    public CustomLayerDialog(Window owner) {
        super(owner);
    }
    
    public abstract T getSelectedLayer();
    
    private static final long serialVersionUID = 1L;
}