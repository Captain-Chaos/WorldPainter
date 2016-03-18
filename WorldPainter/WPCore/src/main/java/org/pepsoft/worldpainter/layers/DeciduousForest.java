/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import java.util.Random;
import org.pepsoft.worldpainter.layers.trees.BirchTree;
import org.pepsoft.worldpainter.layers.trees.OakTree;
import org.pepsoft.worldpainter.layers.trees.TreeType;

/**
 *
 * @author pepijn
 */
public class DeciduousForest extends TreeLayer {
    private DeciduousForest() {
        super("Deciduous", "a deciduous forest", 40, 'd');
    }

    @Override
    public TreeType pickTree(Random random) {
        if (random.nextInt(10) == 0) {
            return BIRCH_TREE;
        } else {
            return OAK_TREE;
        }
    }

    public static final DeciduousForest INSTANCE = new DeciduousForest();

    private static final OakTree   OAK_TREE   = new OakTree();
    private static final BirchTree BIRCH_TREE = new BirchTree();
    private static final long serialVersionUID = 2011041601L;
}