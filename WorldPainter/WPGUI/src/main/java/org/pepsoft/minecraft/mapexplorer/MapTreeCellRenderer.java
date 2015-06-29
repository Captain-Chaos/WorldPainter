/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.awt.Component;
import javax.swing.JTree;
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
        if (value instanceof MapRootNode) {
            MapRootNode rootNode = (MapRootNode) value;
            setText(rootNode.getWorldName());
        } else if (value instanceof NBTFileNode) {
            setText(((NBTFileNode) value).getName());
        } else if (value instanceof TagNode) {
            TagNode tagNode = (TagNode) value;
            Tag tag = tagNode.getTag();
            StringBuilder sb = new StringBuilder();
            sb.append(tag.getClass().getSimpleName());
            String name = tag.getName();
            if ((name != null) && (name.trim().length() > 0)) {
                sb.append(" \"");
                sb.append(name.trim());
                sb.append('"');
            }
            if (tag instanceof StringTag) {
                sb.append(": \"");
                sb.append(tag.getValue());
                sb.append('"');
            } else if (tag instanceof ByteArrayTag) {
                sb.append(": ");
                sb.append(((ByteArrayTag) tag).getValue().length);
                sb.append(" bytes of data");
            } else if (! ((tag instanceof CompoundTag) || (tag instanceof ListTag))) {
                sb.append(": ");
                sb.append(tag.getValue());
            }
            setText(sb.toString());
        } else if (value instanceof RegionFileNode) {
            RegionFileNode regionFileNode = (RegionFileNode) value;
            setText("Region " + regionFileNode.getX() + ", " + regionFileNode.getZ());
        } else if (value instanceof ChunkNode) {
            ChunkNode chunkNode = (ChunkNode) value;
            setText("Chunk " + chunkNode.getX() + ", " + chunkNode.getZ());
        } else if (value instanceof DataNode) {
            setText("data");
        } else if (value instanceof ExtraDimensionNode) {
            String dim = ((ExtraDimensionNode) value).getDimension();
            switch (dim) {
                case "DIM-1":
                    setText("Nether");
                    break;
                case "DIM1":
                    setText("Ender");
                    break;
                default:
                    setText(dim);
                    break;
            }
        }
        return this;
    }
}