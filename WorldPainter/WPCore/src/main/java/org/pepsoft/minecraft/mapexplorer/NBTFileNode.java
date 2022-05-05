/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.*;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public class NBTFileNode extends FileSystemNode {
    NBTFileNode(File file) {
        super(file);
    }
    
    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }


    @Override
    protected Node[] loadChildren() {
        try {
            Tag tag;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                in.mark(2);
                if ((in.read() == 0x1f) && (in.read() == 0x8b)) {
                    // Gzip signature
                    in.reset();
                    tag = new NBTInputStream(new GZIPInputStream(in)).readTag();
                } else {
                    in.reset();
                    try (NBTInputStream levelIn = new NBTInputStream(in)) {
                        tag = levelIn.readTag();
                        if (! (tag instanceof CompoundTag)) {
                            List<Tag> tags = new ArrayList<>();
                            while (! (tag instanceof EndTag)) {
                                tags.add(tag);
                                tag = levelIn.readTag();
                            }
                            tag = new ListTag<>("", Tag.class, tags);
                        }
                    }
                }
            } catch (EOFException e) {
                // Gamble that this means little endian content
                try (NBTInputStream in = new NBTInputStream(new FileInputStream(file), true)) {
                    tag = in.readTag();
                    if (! (tag instanceof CompoundTag)) {
                        List<Tag> tags = new ArrayList<>();
                        while (! (tag instanceof EndTag)) {
                            tags.add(tag);
                            tag = in.readTag();
                        }
                        tag = new ListTag<>("", Tag.class, tags);
                    }
                }
            }
            return new Node[] {new TagNode(tag)};
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading level.dat file", e);
        }
    }

    private static final Icon ICON = IconUtils.scaleIcon(IconUtils.loadScaledIcon("org/pepsoft/worldpainter/mapexplorer/nbtfile.png"), 16);
}