/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

/**
 *
 * @author pepijn
 */
public class NBTFileNode implements Node {
    NBTFileNode(File file) {
        this.file = file;
    }
    
    NBTFileNode(File file, boolean compressed) {
        this.file = file;
        this.compressed = compressed;
    }
    
    public String getName() {
        return file.getName();
    }
    
    public Tag getTag() {
        if (children == null) {
            loadChildren();
        }
        return tag;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Node[] getChildren() {
        if (children == null) {
            loadChildren();
        }
        return children;
    }

    private void loadChildren() {
        try {
            if (compressed == null) {
                FileInputStream in = new FileInputStream(file);
                try {
                    byte[] magicNumber = new byte[2];
                    // TODO: not strictly correct, but will probably always work in
                    // practice. Famous last words
                    in.read(magicNumber);
                    compressed = (magicNumber[0] == (byte) 0x1f) && (magicNumber[1] == (byte) 0x8b);
                } finally {
                    in.close();
                }
            }
            NBTInputStream in = new NBTInputStream(compressed ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file));
            try {
                tag = in.readTag();
                children = new Node[] {new TagNode(tag)};
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading level.dat file", e);
        }
    }

    private final File file;
    private Boolean compressed;
    private Node[] children;
    private Tag tag;
}