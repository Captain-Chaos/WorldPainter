package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A <code>DirectoryNode</code> which is part of a Minecraft map and can contain
 * region files, NBT files and other special node types.
 *
 * <p>Created by Pepijn on 15-6-2016.
 */
public class JavaMinecraftDirectoryNode extends DirectoryNode {
    public JavaMinecraftDirectoryNode(File dir) {
        super(dir);
    }

    @Override
    protected Node[] loadChildren() {
        File[] contents = file.listFiles();
        @SuppressWarnings("ConstantConditions") // Can't happen since this node is only created for existing directories
        List<Node> children = new ArrayList<>(contents.length);
        for (File file: contents) {
            if (file.isDirectory()) {
                children.add(new JavaMinecraftDirectoryNode(file));
            } else if (file.isFile()) {
                String name = file.getName();
                if (regionFilenamePatternVersion1.matcher(name).matches() || regionFilenamePatternVersion2.matcher(name).matches()) {
                    children.add(new RegionFileNode(file));
                } else {
                    String lowercaseName = name.toLowerCase();
                    if (lowercaseName.endsWith(".nbt") || lowercaseName.endsWith(".dat") || lowercaseName.endsWith(".dat_old")) {
                        children.add(new NBTFileNode(file));
                    } else if (isGzip(file)) {
                        // Gamble that any gzipped file is an NBT file
                        children.add(new NBTFileNode(file));
//                    } else if (lowercaseName.endsWith(".json")) {
                        // TODO
                    } else {
                        children.add(new FileSystemNode(file));
                    }
                }
            }
        }
        children.sort((node1, node2) -> DirectoryNode.COLLATOR.compare(node1.getName(), node2.getName()));
        return children.toArray(new Node[children.size()]);
    }

    private boolean isGzip(File file) {
        if (file.length() >= 18) {
            try (FileInputStream in = new FileInputStream(file)) {
                return (in.read() == 0x1f) && (in.read() == 0x8b);
            } catch (IOException e) {
                throw new RuntimeException("I/O error while trying to determine whether " + file + " is a gzip file");
            }
        } else {
            return false;
        }
    }

    private final Pattern regionFilenamePatternVersion1 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
    private final Pattern regionFilenamePatternVersion2 = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
}