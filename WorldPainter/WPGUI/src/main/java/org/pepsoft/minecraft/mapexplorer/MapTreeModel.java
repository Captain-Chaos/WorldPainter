/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.AbstractNode;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class MapTreeModel implements TreeModel {
    public TreePath getPath(File dir) {
        LinkedList<File> components = new LinkedList<>();
        while (dir != null) {
            components.add(0, dir);
            dir = dir.getParentFile();
        }
        // Components now contains the path's components in the correct order
        Object[] path = new Object[components.size() + 1];
        path[0] = rootNode;
        Node node = rootNode;
        int index = 1;
        for (File component: components) {
            String componentName = FileSystemNode.getName(component);
            for (Node childNode: node.getChildren()) {
                if (childNode.getName().equals(componentName)) {
                    path[index++] = childNode;
                    node = childNode;
                    break;
                }
            }
        }
        return new TreePath(path);
    }

    public void refresh(TreePath path) {
        final TreeModelEvent event = new TreeModelEvent(this, path);
        listeners.forEach(listener -> listener.treeStructureChanged(event));
    }

    // TreeModel

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((AbstractNode) parent).getChildren()[index];
    }

    @Override
    public int getChildCount(Object parent) {
        return ((AbstractNode) parent).getChildren().length;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((AbstractNode) node).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Do nothing
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        Node[] children = ((Node) parent).getChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i].equals(child)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    private final RootNode rootNode = new RootNode();
    private final List<TreeModelListener> listeners = new ArrayList<>();
}