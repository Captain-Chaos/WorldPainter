/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.awt.Component;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

/**
 *
 * @author pepijn
 */
public class MapTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setText(((Node) value).getName());
        Icon icon = ((Node) value).getIcon();
        if (icon != null) {
            setIcon(icon);
        }
        return this;
    }
}