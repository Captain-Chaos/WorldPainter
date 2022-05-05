/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import java.util.HashMap;

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

    public InventoryItem(String id, int count, int slot) {
        this();
        setId(id);
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
        return getShort(TAG_ID_);
    }

    public final void setType(int type) {
        setShort(TAG_ID_, (short) type);
    }

    public final String getId() {
        return getString(TAG_ID_);
    }

    public final void setId(String id) {
        setString(TAG_ID_, id);
    }

    public final CompoundTag getTag() {
        return (CompoundTag) getTag(TAG_TAG_);
    }

    public final void setTag(CompoundTag tag) {
        setTag(TAG_TAG_, tag);
    }

    private static final long serialVersionUID = 1L;
}