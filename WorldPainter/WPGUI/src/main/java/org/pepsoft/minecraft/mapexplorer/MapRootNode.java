/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.pepsoft.minecraft.Level;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class MapRootNode implements Node {
    public MapRootNode(File levelDatFile) {
        this.levelDatFile = levelDatFile;
        worldDir = levelDatFile.getParentFile();
        checkReadableDir(worldDir);
        checkReadableFile(levelDatFile);
        try {
            Level level = Level.load(levelDatFile);
            version = level.getVersion();
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading level.dat file", e);
        }
        regionDir = new File(worldDir, "region");
        checkReadableDir(regionDir);
        File potentialDataDir = new File(worldDir, "data");
        File potentialIdcountsDatFile = new File(potentialDataDir, "idcounts.dat");
        if (potentialIdcountsDatFile.isFile()) {
            dataDir = potentialDataDir;
        } else {
            dataDir = null;
        }
        File potentialOldLevelDatFile = new File(worldDir, "level.dat_old");
        if (potentialOldLevelDatFile.isFile()) {
            oldLevelDatFile = potentialOldLevelDatFile;
        } else {
            oldLevelDatFile = null;
        }
    }
    
    public MapRootNode(File baseDir, String worldName) {
        worldDir = new File(baseDir, worldName);
        checkReadableDir(worldDir);
        levelDatFile = new File(worldDir, "level.dat");
        checkReadableFile(levelDatFile);
        try {
            Level level = Level.load(levelDatFile);
            version = level.getVersion();
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading level.dat file", e);
        }
        regionDir = new File(worldDir, "region");
        checkReadableDir(regionDir);
        File potentialDataDir = new File(worldDir, "data");
        File potentialIdcountsDatFile = new File(potentialDataDir, "idcounts.dat");
        if (potentialIdcountsDatFile.isFile()) {
            dataDir = potentialDataDir;
        } else {
            dataDir = null;
        }
        File potentialOldLevelDatFile = new File(worldDir, "level.dat_old");
        if (potentialOldLevelDatFile.isFile()) {
            oldLevelDatFile = potentialOldLevelDatFile;
        } else {
            oldLevelDatFile = null;
        }
    }

    public String getWorldName() {
        return worldDir.getName();
    }
    
    public NBTFileNode getLevelDatNode() {
        return new NBTFileNode(levelDatFile, true);
    }
    
    public NBTFileNode getOldLevelDatNode() {
        return new NBTFileNode(oldLevelDatFile, true);
    }
    
    public DataNode getDataNode() {
        return new DataNode(dataDir);
    }

    public RegionFileNode[] getRegionNodes() {
        final Pattern regionFilenamePattern = (version == SUPPORTED_VERSION_1) ? regionFilenamePatternVersion1 : regionFilenamePatternVersion2;
        File[] files = regionDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && regionFilenamePattern.matcher(pathname.getName()).matches();
            }
        });
        RegionFileNode[] nodes = new RegionFileNode[files.length];
        for (int i = 0; i < files.length; i++) {
            nodes[i] = new RegionFileNode(files[i]);
        }
        // Sort by coordinates, x coordinate first
        Arrays.sort(nodes, new Comparator<RegionFileNode>() {
            public int compare(RegionFileNode r1, RegionFileNode r2) {
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
            }
        });
        return nodes;
    }

    public ExtraDimensionNode[] getDimensionNodes() {
        File[] files = worldDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && dimensionDirPattern.matcher(pathname.getName()).matches();
            }
        });
        ExtraDimensionNode[] nodes = new ExtraDimensionNode[files.length];
        for (int i = 0; i < files.length; i++) {
            nodes[i] = new ExtraDimensionNode(files[i], version);
        }
        return nodes;
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
        List<Node> childrenList = new ArrayList<Node>();
        childrenList.add(getLevelDatNode());
        if (oldLevelDatFile != null) {
            childrenList.add(getOldLevelDatNode());
        }
        childrenList.addAll(Arrays.asList(getRegionNodes()));
        childrenList.addAll(Arrays.asList(getDimensionNodes()));
        if (dataDir != null) {
            childrenList.add(getDataNode());
        }
        children = childrenList.toArray(new Node[childrenList.size()]);
    }

    private void checkReadableDir(File file) {
        if (! file.isDirectory()) {
            throw new IllegalArgumentException(file + " does not exist, or is not a directory");
        }
    }

    private void checkReadableFile(File file) {
        if (! file.isFile()) {
            throw new IllegalArgumentException(file + " does not exist, or is not a regular file");
        }
    }

    private void checkReadable(File file) {
        if (! file.canRead()) {
            throw new IllegalArgumentException(file + " is not readable");
        }
    }

    private final File worldDir, regionDir, levelDatFile, dataDir, oldLevelDatFile;
    private final Pattern regionFilenamePatternVersion1 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
    private final Pattern regionFilenamePatternVersion2 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
    private final Pattern dimensionDirPattern = Pattern.compile("DIM.*");
    private final int version;
    private Node[] children;
}