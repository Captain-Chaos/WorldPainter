/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.Tag;

/**
 *
 * @author pepijn
 */
public class TagNode implements Node {
    TagNode(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isLeaf() {
        return ! ((tag instanceof CompoundTag) || (tag instanceof ListTag));
    }

    public Node[] getChildren() {
        if (children == null) {
            loadChildren();
        }
        return children;
    }

    private void loadChildren() {
        List<Tag> tagList = new ArrayList<>();
        if (tag instanceof CompoundTag) {
            Map<String, Tag> tags = ((CompoundTag) tag).getValue();
            tagList.addAll(tags.values());
        } else if (tag instanceof ListTag) {
            List<Tag> tags = ((ListTag) tag).getValue();
            tagList.addAll(tags);
        }
        children = new Node[tagList.size()];
        for (int i = 0; i < children.length; i++) {
            children[i] = new TagNode(tagList.get(i));
        }
    }

    private final Tag tag;
    private Node[] children;
}