/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.WorldPainterModalFrame;

import java.awt.*;

/**
 *
 * @author pepijn
 */
public abstract class AbstractEditLayerDialog<T extends Layer> extends WorldPainterModalFrame {
    public AbstractEditLayerDialog(Window owner) {
        super(owner);
    }

    public abstract T getLayer();

    private static final long serialVersionUID = 1L;
}