/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class ExtraDimensionNode implements Node {
    public ExtraDimensionNode(File dimensionDir, int version) {
        this.dimensionDir = dimensionDir;
        this.version = version;
        regionDir = new File(dimensionDir, "region");
    }
    
    public String getDimension() {
        return dimensionDir.getName();
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
 
    public RegionFileNode[] getRegionNodes() {
        final Pattern regionFilenamePattern = (version == SUPPORTED_VERSION_1) ? regionFilenamePatternVersion1 : regionFilenamePatternVersion2;
        File[] files = regionDir.listFiles(pathname -> pathname.isFile() && regionFilenamePattern.matcher(pathname.getName()).matches());
        if (files == null) {
            return new RegionFileNode[0];
        }
        RegionFileNode[] nodes = new RegionFileNode[files.length];
        for (int i = 0; i < files.length; i++) {
            nodes[i] = new RegionFileNode(files[i]);
        }
        // Sort by coordinates, x coordinate first
        Arrays.sort(nodes, (r1, r2) -> {
            if (r1.getX() < r2.getX()) {
                return -1;
            } else if (r1.getX() > r2.getX()) {
                return 1;
            } else {
                if (r1.getZ() < r2.getZ()) {
                    return -1;
                } else if (r1.getZ() > r2.getZ()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return nodes;
    }
    
    private void loadChildren() {
        children = getRegionNodes();
    }
    
    private final File dimensionDir, regionDir;
    private final int version;
    private final Pattern regionFilenamePatternVersion1 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
    private final Pattern regionFilenamePatternVersion2 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
    private Node[] children;
}