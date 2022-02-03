/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.mapexplorer;

/**
 *
 * @author pepijn
 */
public abstract class AbstractNode implements Node {
    @Override
    public final Node[] getChildren() {
        if (children == null) {
            children = loadChildren();
        }
        return children;
    }

    @Override
    public void doubleClicked() {
        // Do nothing
    }

    protected abstract Node[] loadChildren();

    private Node[] children;
}