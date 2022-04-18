package org.pepsoft.worldpainter.mapexplorer;

import javax.swing.*;
import java.awt.*;

public interface Node {
    String getName();
    Icon getIcon();
    boolean isLeaf();
    Node[] getChildren();
    void doubleClicked();
    void showPopupMenu(Component invoker, int x, int y, ActionListener actionListener);

    enum Action { REFRESH }

    interface ActionListener {
        void performAction(Action action);
    }
}