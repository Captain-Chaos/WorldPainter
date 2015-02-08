/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.layers.trees.Tree;

/**
 *
 * @author pepijn
 */
@Deprecated // Only exists to be able to load old worlds
public abstract class Trees extends Layer {
    private Trees() {
        // Prevent instantiation
        super(null, null, null, 0);
        treeType = null;
    }

    public final Class<? extends Tree> getType() {
        return treeType;
    }

    private final Class<? extends Tree> treeType;
    
    private static final long serialVersionUID = 2011032901L;
}