/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public class SetLayer extends Action {
    public SetLayer(Layer layer, int value) {
        this.layer = layer;
        this.value = value;
    }
    
    @Override
    public void apply(Context context) {
        
    }
    
    private final Layer layer;
    private final int value;
}