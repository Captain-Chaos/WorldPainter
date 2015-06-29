/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.FloatTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongTag;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

/**
 * A data structure based on an NBT tag or hierarchy of tags.
 *
 * @author pepijn
 */
public abstract class AbstractNBTItem implements NBTItem, Serializable, Cloneable {
    protected AbstractNBTItem(CompoundTag tag) {
        if (tag == null) {
            throw new NullPointerException();
        }
        this.tag = tag;
    }

    @Override
    public Tag toNBT() {
        return tag;
    }
    
    protected final boolean containsTag(String name) {
        return tag.containsTag(name);
    }

    protected final long getLong(String name) {
        LongTag longTag = (LongTag) tag.getTag(name);
        return (longTag != null) ? longTag.getValue() : 0;
    }

    protected final void setLong(String name, long value) {
        tag.setTag(name, new LongTag(name, value));
    }

    protected final int getInt(String name) {
        IntTag intTag = (IntTag) tag.getTag(name);
        return (intTag != null) ? intTag.getValue() : 0;
    }

    protected final void setInt(String name, int value) {
        tag.setTag(name, new IntTag(name, value));
    }

    protected final String getString(String name) {
        StringTag stringTag = (StringTag) tag.getTag(name);
        return (stringTag != null) ? stringTag.getValue() : null;
    }

    protected final void setString(String name, String value) {
        if (value != null) {
            tag.setTag(name, new StringTag(name, value));
        } else {
            tag.setTag(name, null);
        }
    }

    protected final short getShort(String name) {
        ShortTag shortTag = (ShortTag) tag.getTag(name);
        return (shortTag != null) ? shortTag.getValue() : 0;
    }

    protected final void setShort(String name, short value) {
        tag.setTag(name, new ShortTag(name, value));
    }

    protected final byte getByte(String name) {
        ByteTag byteTag = (ByteTag) tag.getTag(name);
        return (byteTag != null) ? byteTag.getValue() : 0;
    }

    protected final void setByte(String name, byte value) {
        tag.setTag(name, new ByteTag(name, value));
    }

    protected final boolean getBoolean(String name) {
        ByteTag byteTag = (ByteTag) tag.getTag(name);
        return (byteTag != null) ? (byteTag.getValue() != 0) : false;
    }

    protected final void setBoolean(String name, boolean value) {
        tag.setTag(name, new ByteTag(name, value ? (byte) 1 : (byte) 0));
    }

    protected final <T extends Tag> List<T> getList(String name) {
        ListTag listTag = (ListTag) tag.getTag(name);
        return (listTag != null) ? (List<T>) listTag.getValue() : null;
    }

    protected final <T extends Tag> void setList(String name, Class<T> type, List<Tag> list) {
        tag.setTag(name, new ListTag(name, type, list));
    }
    
    protected final double[] getDoubleList(String name) {
        List<DoubleTag> list = getList(name);
        if (list != null) {
            double[] array = new double[list.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = list.get(i).getValue();
            }
            return array;
        } else {
            return null;
        }
    }

    protected final void setDoubleList(String name, double[] values) {
        List<Tag> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(new DoubleTag(null, value));
        }
        tag.setTag(name, new ListTag(name, DoubleTag.class, list));
    }

    protected final float[] getFloatList(String name) {
        List<FloatTag> list = getList(name);
        if (list != null) {
            float[] array = new float[list.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = list.get(i).getValue();
            }
            return array;
        } else {
            return null;
        }
    }

    protected final void setFloatList(String name, float[] values) {
        List<Tag> list = new ArrayList<>(values.length);
        for (float value : values) {
            list.add(new FloatTag(null, value));
        }
        tag.setTag(name, new ListTag(name, FloatTag.class, list));
    }

    protected final byte[] getByteArray(String name) {
        ByteArrayTag byteArrayTag = (ByteArrayTag) tag.getTag(name);
        return (byteArrayTag != null) ? byteArrayTag.getValue() : null;
    }

    protected final void setByteArray(String name, byte[] bytes) {
        tag.setTag(name, new ByteArrayTag(name, bytes));
    }

    protected final int[] getIntArray(String name) {
        IntArrayTag intArrayTag = (IntArrayTag) tag.getTag(name);
        return (intArrayTag != null) ? intArrayTag.getValue() : null;
    }

    protected final void setIntArray(String name, int[] values) {
        tag.setTag(name, new IntArrayTag(name, values));
    }

    @Override
    public String toString() {
        return tag.toString();
    }
    
    @Override
    public AbstractNBTItem clone() {
        try {
            AbstractNBTItem clone = (AbstractNBTItem) super.clone();
            clone.tag = tag.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private CompoundTag tag;
    
    private static final long serialVersionUID = 1L;
}