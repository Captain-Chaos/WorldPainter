/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.trees;

import org.pepsoft.minecraft.Material;
import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public class BirchTree extends DeciduousTree {
    public BirchTree() {
        super(WOOD_BIRCH, LEAVES_BIRCH);
    }

    public BirchTree(Material leafMaterial) {
        super(WOOD_BIRCH, leafMaterial);
    }

    private static final long serialVersionUID = 1L;
}