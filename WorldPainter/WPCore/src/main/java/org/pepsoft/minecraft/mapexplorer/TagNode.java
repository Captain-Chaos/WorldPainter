/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.*;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.awt.*;
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
    public TagNode(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
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
            sb.append(((StringTag) tag).getValue());
            sb.append('"');
        } else if (tag instanceof ByteArrayTag) {
            sb.append(": ");
            sb.append(((ByteArrayTag) tag).getValue().length);
            sb.append(" bytes of data");
        } else if (tag instanceof IntArrayTag) {
            sb.append(": ");
            sb.append(((IntArrayTag) tag).getValue().length * 4);
            sb.append(" bytes of data");
        } else if (tag instanceof LongArrayTag) {
            sb.append(": ");
            sb.append(((LongArrayTag) tag).getValue().length * 8);
            sb.append(" bytes of data");
        } else if (tag instanceof ByteTag) {
            sb.append(": ");
            sb.append(((ByteTag) tag).getValue());
        } else if (tag instanceof ShortTag) {
            sb.append(": ");
            sb.append(((ShortTag) tag).getValue());
        } else if (tag instanceof IntTag) {
            sb.append(": ");
            sb.append(((IntTag) tag).getValue());
        } else if (tag instanceof FloatTag) {
            sb.append(": ");
            sb.append(((FloatTag) tag).getValue());
        } else if (tag instanceof LongTag) {
            sb.append(": ");
            sb.append(((LongTag) tag).getValue());
        } else if (tag instanceof DoubleTag) {
            sb.append(": ");
            sb.append(((DoubleTag) tag).getValue());
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
    public void doubleClicked() {
        if (tag instanceof ByteArrayTag) {
            byte[] bytes = ((ByteArrayTag) tag).getValue();
            for (int i = 0; i < bytes.length; i += 16) {
                if ((i % 256) == 0) {
                    System.out.println();
                }
                for (int j = 0; j < 16; j++) {
                    int k = i + j;
                    if (k < bytes.length) {
                        System.out.printf("%02x ", bytes[k]);
                    }
                }
                System.out.println();
            }
        } else if (tag instanceof IntArrayTag) {
            int[] ints = ((IntArrayTag) tag).getValue();
            for (int i = 0; i < ints.length; i += 4) {
                if ((i % 64) == 0) {
                    System.out.println();
                }
                for (int j = 0; j < 4; j++) {
                    int k = i + j;
                    if (k < ints.length) {
                        System.out.printf("%08x ", ints[k]);
                    }
                }
                System.out.println();
            }
        } else if (tag instanceof LongArrayTag) {
            long[] longs = ((LongArrayTag) tag).getValue();
            for (int i = 0; i < longs.length; i += 2) {
                if ((i % 32) == 0) {
                    System.out.println();
                }
                for (int j = 0; j < 2; j++) {
                    int k = i + j;
                    if (k < longs.length) {
                        System.out.printf("%016x ", longs[k]);
                    }
                }
                System.out.println();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
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