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
        setString(TAG_ID_, id);
    }

    protected TileEntity(CompoundTag tag) {
        super(tag);
    }

    public String getId() {
        return getString(TAG_ID_);
    }

    public int getX() {
        return getInt(TAG_X_);
    }

    public void setX(int x) {
        setInt(TAG_X_, x);
    }

    public int getY() {
        return getInt(TAG_Y_);
    }

    public void setY(int y) {
        setInt(TAG_Y_, y);
    }

    public int getZ() {
        return getInt(TAG_Z_);
    }

    public void setZ(int z) {
        setInt(TAG_Z_, z);
    }

    public static TileEntity fromNBT(CompoundTag tileEntityTag) {
        String id = ((StringTag) tileEntityTag.getTag(TAG_ID_)).getValue();
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