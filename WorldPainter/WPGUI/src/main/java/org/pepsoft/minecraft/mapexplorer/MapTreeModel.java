/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.LinkedList;

/**
 *
 * @author pepijn
 */
public class MapTreeModel implements TreeModel {
    public MapTreeModel() {
        rootNode = new RootNode();
    }

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

    // TreeModel

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((Node) parent).getChildren()[index];
    }

    @Override
    public int getChildCount(Object parent) {
        return ((Node) parent).getChildren().length;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((Node) node).isLeaf();
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
        // Do nothing
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // Do nothing
    }

    private final Node rootNode;
}