/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Material;

/**
 *
 * @author pepijn
 */
public class Bo2BlockSpec implements Serializable {
    public Bo2BlockSpec(Point3i coords, Material material, int[] branch) {
        if ((coords == null) || (material == null)) {
            throw new NullPointerException();
        }
        this.coords = coords;
        this.branch = branch;
        this.material = material;
    }

    public Point3i getCoords() {
        return coords;
    }

    public int[] getBranch() {
        return (branch != null) ? branch.clone() : null;
    }

    public Material getMaterial() {
        return material;
    }
    
    private Object readResolve() throws ObjectStreamException {
        // Legacy support
        if (material == null) {
            return new Bo2BlockSpec(coords, Material.get(blockId, data), branch);
        } else {
            return this;
        }
    }

    private final Point3i coords;
    @Deprecated
    private int blockId = -1, data = -1;
    private final int[] branch;
    private final Material material;

    private static final long serialVersionUID = 1L;
}