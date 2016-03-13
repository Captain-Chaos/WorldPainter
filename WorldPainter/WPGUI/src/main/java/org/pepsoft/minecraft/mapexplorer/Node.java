/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import javax.swing.*;

/**
 *
 * @author pepijn
 */
public abstract class Node {
    public abstract String getName();
    public abstract Icon getIcon();
    public abstract boolean isLeaf();

    public final Node[] getChildren() {
        if (children == null) {
            children = loadChildren();
        }
        return children;
    }

    protected abstract Node[] loadChildren();

    private Node[] children;
}