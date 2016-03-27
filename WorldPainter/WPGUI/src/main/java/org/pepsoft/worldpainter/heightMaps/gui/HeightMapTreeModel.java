/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.DelegatingHeightMap;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class HeightMapTreeModel implements TreeModel {
    public HeightMapTreeModel(HeightMap rootHeightMap) {
        this.rootHeightMap = rootHeightMap;
    }

    public void notifyListeners() {
        TreeModelEvent event = new TreeModelEvent(this, new Object[] {rootHeightMap});
        for (TreeModelListener listener: listeners) {
            listener.treeStructureChanged(event);
        }
    }

    // TreeModel

    @Override
    public Object getRoot() {
        return rootHeightMap;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof DelegatingHeightMap) {
            return ((DelegatingHeightMap) parent).getHeightMap(index);
        }
        throw new IndexOutOfBoundsException(Integer.toString(index));
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof DelegatingHeightMap) {
            return ((DelegatingHeightMap) parent).getHeightMapCount();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return ! (node instanceof DelegatingHeightMap);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof DelegatingHeightMap) {
            return ((DelegatingHeightMap) parent).getIndex((HeightMap) child);
        }
        throw new IllegalArgumentException("Not a child of specified parent");
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
    
    private final HeightMap rootHeightMap;
    private final List<TreeModelListener> listeners = new ArrayList<>();
}