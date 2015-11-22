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
        return getLong(name, 0L);
    }

    protected final long getLong(String name, long defaultValue) {
        LongTag longTag = (LongTag) tag.getTag(name);
        return (longTag != null) ? longTag.getValue() : defaultValue;
    }

    protected final void setLong(String name, long value) {
        tag.setTag(name, new LongTag(name, value));
    }

    protected final int getInt(String name) {
        return getInt(name, 0);
    }

    protected final int getInt(String name, int defaultValue) {
        IntTag intTag = (IntTag) tag.getTag(name);
        return (intTag != null) ? intTag.getValue() : defaultValue;
    }

    protected final void setInt(String name, int value) {
        tag.setTag(name, new IntTag(name, value));
    }

    protected final String getString(String name) {
        return getString(name, null);
    }

    protected final String getString(String name, String defaultValue) {
        StringTag stringTag = (StringTag) tag.getTag(name);
        return (stringTag != null) ? stringTag.getValue() : defaultValue;
    }

    protected final void setString(String name, String value) {
        if (value != null) {
            tag.setTag(name, new StringTag(name, value));
        } else {
            tag.setTag(name, null);
        }
    }

    protected final short getShort(String name) {
        return getShort(name, (short) 0);
    }

    protected final short getShort(String name, short defaultValue) {
        ShortTag shortTag = (ShortTag) tag.getTag(name);
        return (shortTag != null) ? shortTag.getValue() : defaultValue;
    }

    protected final void setShort(String name, short value) {
        tag.setTag(name, new ShortTag(name, value));
    }

    protected final byte getByte(String name) {
        return getByte(name, (byte) 0);
    }

    protected final byte getByte(String name, byte defaultValue) {
        ByteTag byteTag = (ByteTag) tag.getTag(name);
        return (byteTag != null) ? byteTag.getValue() : defaultValue;
    }

    protected final void setByte(String name, byte value) {
        tag.setTag(name, new ByteTag(name, value));
    }

    protected final boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    protected final boolean getBoolean(String name, boolean defaultValue) {
        ByteTag byteTag = (ByteTag) tag.getTag(name);
        return (byteTag != null) ? (byteTag.getValue() != 0) : defaultValue;
    }

    protected final void setBoolean(String name, boolean value) {
        tag.setTag(name, new ByteTag(name, value ? (byte) 1 : (byte) 0));
    }
    
    protected final float getFloat(String name) {
        return getFloat(name, 0.0f);
    }

    protected final float getFloat(String name, float defaultValue) {
        FloatTag floatTag = (FloatTag) tag.getTag(name);
        return (floatTag != null) ? floatTag.getValue() : defaultValue;
    }
    
    protected final void setFloat(String name, float value) {
        tag.setTag(name, new FloatTag(name, value));
    }
    
    protected final double getDouble(String name) {
        return getDouble(name, 0.0);
    }

    protected final double getDouble(String name, double defaultValue) {
        DoubleTag doubleTag = (DoubleTag) tag.getTag(name);
        return (doubleTag != null) ? doubleTag.getValue() : defaultValue;
    }
    
    protected final void setDouble(String name, double value) {
        tag.setTag(name, new DoubleTag(name, value));
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