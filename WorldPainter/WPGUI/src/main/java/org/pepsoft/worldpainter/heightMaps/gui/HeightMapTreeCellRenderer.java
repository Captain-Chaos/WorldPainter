/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.AbstractHeightMap;
import org.pepsoft.worldpainter.heightMaps.DelegatingHeightMap;
import org.pepsoft.worldpainter.heightMaps.DisplacementHeightMap;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
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
            String role = null;
            StringBuilder name = new StringBuilder();
            if (value instanceof AbstractHeightMap) {
                DelegatingHeightMap parent = ((AbstractHeightMap) value).getParent();
                if (parent instanceof DisplacementHeightMap) {
                    role = parent.getRole(parent.getIndex(heightMap));
                    if (role.endsWith("HeightMap")) {
                        name.append(role.substring(0, role.length() - 9));
                    } else if (role.endsWith("Map")) {
                        name.append(role.substring(0, role.length() - 3));
                    } else {
                        name.append(role);
                    }
                }
            }
            if (heightMap.getName() != null) {
                if (name.length() > 0) {
                    name.append(": ");
                }
                name.append(heightMap.getName());
            }
            if (name.length() == 0) {
                name.append(heightMap.getClass().getSimpleName());
            }
            if (value == focusHeightMap) {
                setBorder(focusBorder);
            } else if (getBorder() != null) {
                setBorder(null);
            }
            setText(name.toString());
            setIcon(heightMap.getIcon());
            setToolTipText(getTooltipText(heightMap, role));
        }
        return this;
    }

    public HeightMap getFocusHeightMap() {
        return focusHeightMap;
    }

    public void setFocusHeightMap(HeightMap focusHeightMap) {
        this.focusHeightMap = focusHeightMap;
    }

    private String getTooltipText(HeightMap heightMap, String role) {
        StringBuilder sb = new StringBuilder("<html>");
        String name = heightMap.getName();
        if (name != null) {
            sb.append("Name: <strong>").append(name).append("</strong><br>");
        }
        String type = heightMap.getClass().getSimpleName();
        type = type.substring(0, type.length() - 9);
        sb.append("Type: <strong>").append(type).append("</strong><br>");
        if (role != null) {
            sb.append("Role: <strong>").append(role).append("</strong><br>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private final Border focusBorder = BorderFactory.createLineBorder(Color.RED);
    private HeightMap focusHeightMap;
}