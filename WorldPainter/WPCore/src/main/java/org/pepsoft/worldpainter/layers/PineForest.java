/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.layers.trees.PineTree;
import org.pepsoft.worldpainter.layers.trees.TreeType;

import java.util.Random;

/**
 *
 * @author pepijn
 */
public class PineForest extends TreeLayer {
    private PineForest() {
        super("Pine", "a pine forest", 41, 'n');
    }

    @Override
    public TreeType pickTree(Random random) {
        return PINE_TREE;
    }
    
    public static final PineForest INSTANCE = new PineForest();

    private static final PineTree PINE_TREE = new PineTree();
    private static final long serialVersionUID = 2011060301L;
}