/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.HashMap;
import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class TileEntity extends AbstractNBTItem {
    public TileEntity(String id) {
        super(new CompoundTag("", new HashMap<String, Tag>()));
        if (id == null) {
            throw new NullPointerException();
        }
        setString(TAG_ID, id);
    }

    public TileEntity(CompoundTag tag) {
        super(tag);
    }

    public String getId() {
        return getString(TAG_ID);
    }

    public int getX() {
        return getInt(TAG_X);
    }

    public void setX(int x) {
        setInt(TAG_X, x);
    }

    public int getY() {
        return getInt(TAG_Y);
    }

    public void setY(int y) {
        setInt(TAG_Y, y);
    }

    public int getZ() {
        return getInt(TAG_Z);
    }

    public void setZ(int z) {
        setInt(TAG_Z, z);
    }

    public static TileEntity fromNBT(Tag tileEntityTag) {
        CompoundTag tag = (CompoundTag) tileEntityTag;
        String id = ((StringTag) tag.getTag(TAG_ID)).getValue();
        if (id.equals(ID_CHEST)) {
            return new Chest(tag);
        } else if (id.equals(ID_SIGN)) {
            return new WallSign(tag);
        } else {
            return new TileEntity(tag);
        }
    }
    
    private static final long serialVersionUID = 1L;
}