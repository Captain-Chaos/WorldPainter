/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft.mapexplorer;

import org.jnbt.CompoundTag;
import org.jnbt.ShortTag;
import org.jnbt.Tag;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class DataNode extends Node {
    DataNode(File dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public String getName() {
        return "data";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        ArrayList<Node> myChildren = new ArrayList<>();
        File idcountsDatFile = new File(dataDir, "idcounts.dat");
        if (idcountsDatFile.isFile()) {
            NBTFileNode idCountsDatNode = new NBTFileNode(idcountsDatFile, false);
            myChildren.add(idCountsDatNode);
            CompoundTag compoundTag = (CompoundTag) idCountsDatNode.getTag();
            for (Map.Entry<String, Tag> entry: compoundTag.getValue().entrySet()) {
                String name = entry.getKey();
                short idCount = ((ShortTag) entry.getValue()).getValue();
                for (int i = 0; i <= idCount; i++) {
                    String fileName = name + '_' + i + ".dat";
                    myChildren.add(new NBTFileNode(new File(dataDir, fileName), true));
                }
            }
        }
        return myChildren.toArray(new Node[myChildren.size()]);
    }
 
    private final File dataDir;
}