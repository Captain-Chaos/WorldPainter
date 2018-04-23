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
public abstract class AbstractEditLayerDialog<T extends Layer> extends WorldPainterDialog {
    public AbstractEditLayerDialog(Window owner) {
        super(owner);
    }

    public abstract T getLayer();

    private static final long serialVersionUID = 1L;
}