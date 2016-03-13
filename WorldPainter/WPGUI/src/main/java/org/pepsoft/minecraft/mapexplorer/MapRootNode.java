/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.minecraft.Level;
import org.pepsoft.util.IconUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;

/**
 *
 * @author pepijn
 */
public class MapRootNode extends Node {
    public MapRootNode(File levelDatFile) {
        this.levelDatFile = levelDatFile;
        worldDir = levelDatFile.getParentFile();
    }
    
    public String getWorldName() {
        return worldDir.getName();
    }

    @Override
    public String getName() {
        return getWorldName();
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    public boolean isLeaf() {
        return false;
    }

    protected Node[] loadChildren() {
        int version;
        try {
            Level level = Level.load(levelDatFile);
            version = level.getVersion();
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading level.dat file", e);
        }
        File regionDir = new File(worldDir, "region");
        File potentialDataDir = new File(worldDir, "data");
        File potentialIdcountsDatFile = new File(potentialDataDir, "idcounts.dat");
        File dataDir;
        if (potentialIdcountsDatFile.isFile()) {
            dataDir = potentialDataDir;
        } else {
            dataDir = null;
        }
        File potentialOldLevelDatFile = new File(worldDir, "level.dat_old");
        File oldLevelDatFile;
        if (potentialOldLevelDatFile.isFile()) {
            oldLevelDatFile = potentialOldLevelDatFile;
        } else {
            oldLevelDatFile = null;
        }

        List<Node> childrenList = new ArrayList<>();
        childrenList.add(new NBTFileNode(levelDatFile, true));
        if (oldLevelDatFile != null) {
            childrenList.add(new NBTFileNode(oldLevelDatFile, true));
        }
        childrenList.addAll(Arrays.asList(getRegionNodes(regionDir, version)));
        childrenList.addAll(Arrays.asList(getDimensionNodes(version)));
        if (dataDir != null) {
            childrenList.add(new DataNode(dataDir));
        }
        return childrenList.toArray(new Node[childrenList.size()]);
    }

    private RegionFileNode[] getRegionNodes(File regionDir, int version) {
        final Pattern regionFilenamePattern = (version == SUPPORTED_VERSION_1) ? regionFilenamePatternVersion1 : regionFilenamePatternVersion2;
        File[] files = regionDir.listFiles(pathname -> pathname.isFile() && regionFilenamePattern.matcher(pathname.getName()).matches());
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

    private ExtraDimensionNode[] getDimensionNodes(int version) {
        File[] files = worldDir.listFiles(pathname -> pathname.isDirectory() && dimensionDirPattern.matcher(pathname.getName()).matches());
        ExtraDimensionNode[] nodes = new ExtraDimensionNode[files.length];
        for (int i = 0; i < files.length; i++) {
            nodes[i] = new ExtraDimensionNode(files[i], version);
        }
        return nodes;
    }

    private final File worldDir, levelDatFile;
    private final Pattern regionFilenamePatternVersion1 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
    private final Pattern regionFilenamePatternVersion2 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
    private final Pattern dimensionDirPattern = Pattern.compile("DIM.*");

    private static final Icon ICON = IconUtils.scaleIcon(IconUtils.loadIcon("org/pepsoft/worldpainter/icons/grass.png"), 16);
}