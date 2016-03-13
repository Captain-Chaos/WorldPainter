package org.pepsoft.minecraft.mapexplorer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.text.Collator;
import java.util.Arrays;

/**
 * Created by pepijn on 13-3-16.
 */
public class DirectoryNode extends Node {
    public DirectoryNode(File dir) {
        this.dir = dir;
    }

    @Override
    public String getName() {
        return dir.getName();
    }

    @Override
    public Icon getIcon() {
        return FILE_SYSTEM_VIEW.getSystemIcon(dir);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        File[] contents = dir.listFiles(pathname -> pathname.isDirectory() || pathname.getName().equals("level.dat"));
        if (contents != null) {
            Node[] children = new Node[contents.length];
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isDirectory()) {
                    File levelDatFile = new File(contents[i], "level.dat");
                    if (levelDatFile.isFile()) {
                        children[i] = new MapRootNode(levelDatFile);
                    } else {
                        children[i] = new DirectoryNode(contents[i]);
                    }
                }
            }
            Arrays.sort(children, (node1, node2) -> COLLATOR.compare(node1.getName(), node2.getName()));
            return children;
        } else {
            return new Node[0];
        }
    }

    private final File dir;

    private static final FileSystemView FILE_SYSTEM_VIEW = FileSystemView.getFileSystemView();
    private static final Collator COLLATOR = Collator.getInstance();
}
