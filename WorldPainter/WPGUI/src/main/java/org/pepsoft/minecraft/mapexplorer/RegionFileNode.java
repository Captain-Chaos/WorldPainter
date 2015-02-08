/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.pepsoft.minecraft.RegionFile;

/**
 *
 * @author pepijn
 */
public class RegionFileNode implements Node {
    RegionFileNode(File file) {
        this.file = file;
        StringTokenizer tokenizer = new StringTokenizer(file.getName(), ".");
        tokenizer.nextToken();
        x = Integer.parseInt(tokenizer.nextToken());
        z = Integer.parseInt(tokenizer.nextToken());
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Node[] getChildren() {
        if (children == null) {
            loadChildren();
        }
        return children;
    }

    private void loadChildren() {
        try {
            List<Node> chunks = new ArrayList<Node>();
            RegionFile regionFile = new RegionFile(file);
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                    if (regionFile.containsChunk(chunkX, chunkZ)) {
                        chunks.add(new ChunkNode(regionFile, chunkX, chunkZ));
                    }
                }
            }
            children = chunks.toArray(new Node[chunks.size()]);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading region file", e);
        }
    }

    private final File file;
    private final int x, z;
    private Node[] children;
}