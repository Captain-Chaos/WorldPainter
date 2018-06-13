/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import java.io.*;
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
    public void doubleClicked() {
        // Do nothing
    }

    @Override
    protected Node[] loadChildren() {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            in.mark(2);
            Tag tag;
            if ((in.read() == 0x1f) && (in.read() == 0x8b)) {
                // Gzip signature
                in.reset();
                tag = new NBTInputStream(new GZIPInputStream(in)).readTag();
            } else {
                in.reset();
                tag = new NBTInputStream(in).readTag();
            }
            return new Node[] {new TagNode(tag)};
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading level.dat file", e);
        }
    }

    private static final Icon ICON = IconUtils.scaleIcon(IconUtils.loadScaledIcon("org/pepsoft/worldpainter/mapexplorer/nbtfile.png"), 16);
}