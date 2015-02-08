/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.IOException;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.RegionFile;

/**
 *
 * @author pepijn
 */
public class ChunkNode implements Node {
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

    public boolean isLeaf() {
        return false;
    }

    public Node[] getChildren() {
        if (children == null) {
            loadChildren();
        }
        return children;
    }

    private void loadChildren() {
        try {
            NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(x, z));
            try {
                Tag tag = in.readTag();
                children = new Node[] {new TagNode(tag)};
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading from region file", e);
        }
    }

    private final RegionFile regionFile;
    private final int x, z;
    private Node[] children;
}