/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.worldpainter.mapexplorer.AbstractNode;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.io.IOException;

/**
 *
 * @author pepijn
 */
public class ChunkNode extends AbstractNode {
    ChunkNode(RegionFile regionFile, int x, int z) {
        this.regionFile = regionFile;
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String getName() {
        return "Chunk " + x + ", " + z;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        try {
            try (NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(x, z))) {
                Tag tag = in.readTag();
                return new Node[]{new TagNode(tag)};
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading from region file", e);
        }
    }

    private final RegionFile regionFile;
    private final int x, z;
}