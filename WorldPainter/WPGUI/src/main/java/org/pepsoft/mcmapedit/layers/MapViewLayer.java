/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.mcmapedit.layers;

import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public class MapViewLayer extends Layer {
    public MapViewLayer(String name, int z) {
        super("org.pepsoft.MapViewLayer." + name, name, name, DataSize.BYTE, 5);
        setZ(z);
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
    
    private int z;
}