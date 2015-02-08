/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.CombiningHeightMap;
import org.pepsoft.worldpainter.heightMaps.DisplacementHeightMap;

/**
 *
 * @author pepijn
 */
public class HeightMapTreeModel implements TreeModel {
    public HeightMapTreeModel(HeightMap rootHeightMap) {
        this.rootHeightMap = rootHeightMap;
    }

    @Override
    public Object getRoot() {
        return rootHeightMap;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof CombiningHeightMap) {
            if (index == 0) {
                return ((CombiningHeightMap) parent).getHeightMap1();
            } else if (index == 1) {
                return ((CombiningHeightMap) parent).getHeightMap2();
            } else {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
        } else if (parent instanceof DisplacementHeightMap) {
            if (index == 0) {
                return ((DisplacementHeightMap) parent).getBaseHeightMap();
            } else if (index == 1) {
                return ((DisplacementHeightMap) parent).getAngleMap();
            } else if (index == 2) {
                return ((DisplacementHeightMap) parent).getDistanceMap();
            } else {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof CombiningHeightMap) {
            return 2;
        } else if (parent instanceof DisplacementHeightMap) {
            return 3;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return (! (node instanceof CombiningHeightMap)) && (! (node instanceof DisplacementHeightMap));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof CombiningHeightMap) {
            if (child == ((CombiningHeightMap) parent).getHeightMap1()) {
                return 0;
            } else if (child == ((CombiningHeightMap) parent).getHeightMap2()) {
                return 1;
            } else {
                throw new IllegalArgumentException("Not a child of specified parent");
            }
        } else if (parent instanceof DisplacementHeightMap) {
            if (child == ((DisplacementHeightMap) parent).getBaseHeightMap()) {
                return 0;
            } else if (child == ((DisplacementHeightMap) parent).getAngleMap()) {
                return 1;
            } else if (child == ((DisplacementHeightMap) parent).getDistanceMap()) {
                return 2;
            } else {
                throw new IllegalArgumentException("Not a child of specified parent");
            }
        } else {
            throw new IllegalArgumentException("Not a child of specified parent");
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // Do nothing
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // Do nothing
    }
    
    private final HeightMap rootHeightMap;
}