/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author pepijn
 */
public class MapTreeModel implements TreeModel {
    public MapTreeModel(File file) {
        rootNode = (file.getName().equalsIgnoreCase("level.dat") && new File(file.getParentFile(), "region").isDirectory() ) ? new MapRootNode(file) : new NBTFileNode(file);
    }
    
    public MapTreeModel(File baseDir, String worldName) {
        rootNode = new MapRootNode(baseDir, worldName);
    }

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