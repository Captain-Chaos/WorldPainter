/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author pepijn
 */
public class RegionFileNode extends Node {
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
    public String getName() {
        return "Region " + x + ", " + z;
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        try {
            List<Node> chunks = new ArrayList<>();
            RegionFile regionFile = new RegionFile(file);
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                    if (regionFile.containsChunk(chunkX, chunkZ)) {
                        chunks.add(new ChunkNode(regionFile, chunkX, chunkZ));
                    }
                }
            }
            return chunks.toArray(new Node[chunks.size()]);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading region file", e);
        }
    }

    private final File file;
    private final int x, z;

    private static final Icon ICON = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/plugin.png");
}