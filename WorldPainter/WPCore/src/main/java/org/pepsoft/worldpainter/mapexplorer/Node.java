package org.pepsoft.worldpainter.mapexplorer;

import javax.swing.*;

public interface Node {
    String getName();
    Icon getIcon();
    boolean isLeaf();
    Node[] getChildren();
    void doubleClicked();
}