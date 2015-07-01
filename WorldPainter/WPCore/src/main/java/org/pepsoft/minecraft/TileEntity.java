/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.HashMap;
import org.jnbt.CompoundTag;
import org.jnbt.StringTag;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class TileEntity extends AbstractNBTItem {
    public TileEntity(String id) {
        super(new CompoundTag("", new HashMap<>()));
        if (id == null) {
            throw new NullPointerException();
        }
        setString(TAG_ID, id);
    }

    protected TileEntity(CompoundTag tag) {
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

    public static TileEntity fromNBT(CompoundTag tileEntityTag) {
        String id = ((StringTag) tileEntityTag.getTag(TAG_ID)).getValue();
        switch (id) {
            case ID_CHEST:
                return new Chest(tileEntityTag);
            case ID_SIGN:
                return new WallSign(tileEntityTag);
            default:
                return new TileEntity(tileEntityTag);
        }
    }
    
    private static final long serialVersionUID = 1L;
}