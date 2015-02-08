/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft.mapexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.jnbt.CompoundTag;
import org.jnbt.ShortTag;
import org.jnbt.Tag;

/**
 *
 * @author pepijn
 */
public class DataNode implements Node {
    DataNode(File dataDir) {
        this.dataDir = dataDir;
    }

    public boolean isLeaf() {
        return false;
    }

    public Node[] getChildren() {
        if (children == null) {
            loadChildren();
        }
        return children;
    }
    
    private void loadChildren() {
        ArrayList<Node> myChildren = new ArrayList<Node>();
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
        children = myChildren.toArray(new Node[myChildren.size()]);
    }
 
    private final File dataDir;
    private Node[] children;
}