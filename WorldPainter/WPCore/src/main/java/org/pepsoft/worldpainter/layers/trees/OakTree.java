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
public class OakTree extends DeciduousTree {
    public OakTree() {
        super(WOOD_OAK, LEAVES_OAK);
    }

    public OakTree(Material leafMaterial) {
        super(WOOD_OAK, leafMaterial);
    }

    private static final long serialVersionUID = 1L;
}