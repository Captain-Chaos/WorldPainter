package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.text.Collator;
import java.util.Arrays;

/**
 * Created by pepijn on 13-3-16.
 */
public class DirectoryNode extends FileSystemNode {
    public DirectoryNode(File dir) {
        super(dir);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        File[] contents = file.listFiles(File::isDirectory);
        if (contents != null) {
            Node[] children = new Node[contents.length];
            for (int i = 0; i < contents.length; i++) {
                File levelDatFile = new File(contents[i], "level.dat");
                if (levelDatFile.isFile()) {
                    children[i] = new MapRootNode(contents[i]);
                } else {
                    children[i] = new DirectoryNode(contents[i]);
                }
            }
            Arrays.sort(children, (node1, node2) -> COLLATOR.compare(node1.getName(), node2.getName()));
            return children;
        } else {
            return new Node[0];
        }
    }

    protected static final Collator COLLATOR = Collator.getInstance();
}
