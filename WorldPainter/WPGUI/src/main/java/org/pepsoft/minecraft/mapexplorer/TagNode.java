/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.*;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class TagNode extends Node {
    TagNode(Tag tag) {
        this.tag = tag;
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        String name = tag.getName();
        if ((name != null) && (name.trim().length() > 0)) {
            sb.append(name.trim());
            sb.append(" (");
        }
        sb.append(tag.getClass().getSimpleName());
        if ((name != null) && (name.trim().length() > 0)) {
            sb.append(')');
        }
        if (tag instanceof StringTag) {
            sb.append(": \"");
            sb.append(tag.getValue());
            sb.append('"');
        } else if (tag instanceof ByteArrayTag) {
            sb.append(": ");
            sb.append(((ByteArrayTag) tag).getValue().length);
            sb.append(" bytes of data");
        } else if (tag instanceof IntArrayTag) {
            sb.append(": ");
            sb.append(((IntArrayTag) tag).getValue().length * 4);
            sb.append(" bytes of data");
        } else if (! ((tag instanceof CompoundTag) || (tag instanceof ListTag))) {
            sb.append(": ");
            sb.append(tag.getValue());
        }
        return sb.toString();
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return ! ((tag instanceof CompoundTag) || (tag instanceof ListTag));
    }

    @Override
    protected Node[] loadChildren() {
        List<Tag> tagList = new ArrayList<>();
        if (tag instanceof CompoundTag) {
            Map<String, Tag> tags = ((CompoundTag) tag).getValue();
            tagList.addAll(tags.values());
            Collections.sort(tagList, (tag1, tag2) -> COLLATOR.compare(tag1.getName(), tag2.getName()));
        } else if (tag instanceof ListTag) {
            List<Tag> tags = ((ListTag) tag).getValue();
            tagList.addAll(tags);
        }
        return tagList.stream().map(TagNode::new).toArray(Node[]::new);
    }

    private final Tag tag;

    private static final Collator COLLATOR = Collator.getInstance();
}