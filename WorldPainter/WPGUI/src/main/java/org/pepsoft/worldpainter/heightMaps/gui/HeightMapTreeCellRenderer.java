/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.pepsoft.worldpainter.HeightMap;

/**
 *
 * @author pepijn
 */
public class HeightMapTreeCellRenderer extends DefaultTreeCellRenderer {
    public HeightMapTreeCellRenderer(HeightMap rootHeightMap) {
        this.rootHeightMap = rootHeightMap;
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof HeightMap) {
            String name = ((HeightMap) value).getName();
            if (name != null) {
                setText(name + " (" + value.getClass().getSimpleName() + ")");
            } else {
                setText(value.getClass().getSimpleName());
            }
        }
        return this;
    }
    
    private final HeightMap rootHeightMap;
}