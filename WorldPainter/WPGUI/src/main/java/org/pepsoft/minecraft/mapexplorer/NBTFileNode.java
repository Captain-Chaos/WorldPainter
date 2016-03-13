/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public class NBTFileNode extends Node {
    NBTFileNode(File file, boolean compressed) {
        this.file = file;
        this.compressed = compressed;
    }
    
    public String getName() {
        return file.getName();
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    public Tag getTag() {
        getChildren(); // Trigger loading of children if necessary
        return tag;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        try {
            try (NBTInputStream in = new NBTInputStream(compressed ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file))) {
                tag = in.readTag();
                return new Node[] {new TagNode(tag)};
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading level.dat file", e);
        }
    }

    private final File file;
    private final boolean compressed;
    private Tag tag;
}