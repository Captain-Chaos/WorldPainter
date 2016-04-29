/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author Pepijn Schmitz
 * @param <L> The layer type of which this is an editor.
 */
public abstract class AbstractLayerEditor<L extends Layer> extends JPanel implements LayerEditor<L> {
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public L getLayer() {
        return layer;
    }

    @Override
    public void setLayer(L layer) {
        this.layer = layer;
    }

    @Override
    public void setContext(LayerEditorContext context) {
        this.context = context;
    }

    protected L layer;
    protected LayerEditorContext context;

    private static final long serialVersionUID = 1L;
}