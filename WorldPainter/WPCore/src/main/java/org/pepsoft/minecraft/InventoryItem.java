/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.HashMap;
import org.jnbt.CompoundTag;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class InventoryItem extends AbstractNBTItem {
    public InventoryItem() {
        super(new CompoundTag("", new HashMap<>()));
    }

    public InventoryItem(int type, int damage, int count, int slot) {
        this();
        setType(type);
        setDamage(damage);
        setCount(count);
        setSlot(slot);
    }

    public InventoryItem(CompoundTag tag) {
        super(tag);
    }

    public final int getCount() {
        return getByte(TAG_COUNT);
    }

    public final void setCount(int count) {
        setByte(TAG_COUNT, (byte) count);
    }

    public final int getDamage() {
        return getShort(TAG_DAMAGE);
    }

    public final void setDamage(int damage) {
        setShort(TAG_DAMAGE, (short) damage);
    }

    public final int getSlot() {
        return getByte(TAG_SLOT);
    }

    public final void setSlot(int slot) {
        setByte(TAG_SLOT, (byte) slot);
    }

    public final int getType() {
        return getShort(TAG_ID);
    }

    public final void setType(int type) {
        setShort(TAG_ID, (short) type);
    }

    private static final long serialVersionUID = 1L;
}