/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 *
 * @author pepijn
 */
public class HeightMapTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof HeightMap) {
            HeightMap heightMap = (HeightMap) value;
            String role = "output";
            if (value instanceof AbstractHeightMap) {
                DelegatingHeightMap parent = ((AbstractHeightMap) value).getParent();
                if (parent != null) {
                    role = parent.getRole(parent.getIndex(heightMap));
                }
            }
            String name = (heightMap).getName();
            if (name != null) {
                setText(role + " (" + name + ")");
            } else {
                setText(role);
            }
            if (value instanceof TransformingHeightMap) {
                setIcon(ICON_TRANSFORMING_HEIGHTMAP);
            } else if (value instanceof BitmapHeightMap) {
                setIcon(ICON_BITMAP_HEIGHTMAP);
            } else if (value instanceof DisplacementHeightMap) {
                setIcon(ICON_DISPLACEMENT_HEIGHTMAP);
            }
            if (value == focusHeightMap) {
                setBorder(focusBorder);
            } else if (getBorder() != null) {
                setBorder(null);
            }
        }
        return this;
    }

    public HeightMap getFocusHeightMap() {
        return focusHeightMap;
    }

    public void setFocusHeightMap(HeightMap focusHeightMap) {
        this.focusHeightMap = focusHeightMap;
    }

    private final Border focusBorder = BorderFactory.createLineBorder(Color.RED);
    private HeightMap focusHeightMap;

    private static final Icon ICON_DISPLACEMENT_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_anticlockwise.png");
    private static final Icon ICON_TRANSFORMING_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_cross.png");
    private static final Icon ICON_BITMAP_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/photo.png");
}