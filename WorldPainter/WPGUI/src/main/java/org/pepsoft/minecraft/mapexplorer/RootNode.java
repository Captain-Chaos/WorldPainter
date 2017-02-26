package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

/**
 * Created by pepijn on 13-3-16.
 */
public class RootNode extends Node {
    public RootNode() {
    }

    @Override
    public String getName() {
        return "Root";
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
        return Arrays.stream(File.listRoots())
                .map(DirectoryNode::new)
                .toArray(Node[]::new);
    }
}
