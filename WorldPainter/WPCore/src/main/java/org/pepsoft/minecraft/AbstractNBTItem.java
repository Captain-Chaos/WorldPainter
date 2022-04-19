/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

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
        tags = null;
    }

    protected AbstractNBTItem(Map<DataType, CompoundTag> tags) {
        tags.forEach((key, value) -> {
            if ((key == null) || (value == null)) {
                throw new NullPointerException();
            }
        });
        this.tags = new HashMap<>(tags);
        tag = null;
    }

    @Override
    public CompoundTag toNBT() {
        if (tag == null) {
            throw new UnsupportedOperationException("This NBT item has no single tag");
        }
        return tag;
    }

    @Override
    public Map<DataType, ? extends Tag> toMultipleNBT() {
        if (tags == null) {
            throw new UnsupportedOperationException("This NBT item has no multiple tags");
        }
        return tags;
    }

    protected final Map<String, Tag> getAllTags() {
        return tag.getValue();
    }

    protected final boolean containsTag(String name) {
        return tag.containsTag(name);
    }

    protected final Tag getTag(String name) {
        return tag.getTag(name);
    }

    protected final void setTag(String name, Tag tag) {
        this.tag.setTag(name, tag);
    }

    protected final Map<String, Tag> getMap(String name) {
        CompoundTag compoundTag = (CompoundTag) tag.getTag(name);
        return (compoundTag != null) ? compoundTag.getValue() : null;
    }

    protected final void setMap(String name, Map<String, Tag> value) {
        tag.setTag(name, (value != null) ? new CompoundTag(name, value) : null);
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

    protected final <T extends Tag> void setList(String name, Class<T> type, List<T> list) {
        tag.setTag(name, (list != null) ? new ListTag(name, type, list) : null);
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
        if (values != null) {
            List<Tag> list = new ArrayList<>(values.length);
            for (double value: values) {
                list.add(new DoubleTag(null, value));
            }
            tag.setTag(name, new ListTag(name, DoubleTag.class, list));
        } else {
            tag.setTag(name, null);
        }
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
        if (values != null) {
            List<Tag> list = new ArrayList<>(values.length);
            for (float value: values) {
                list.add(new FloatTag(null, value));
            }
            tag.setTag(name, new ListTag(name, FloatTag.class, list));
        } else {
            tag.setTag(name, null);
        }
    }

    protected final byte[] getByteArray(String name) {
        ByteArrayTag byteArrayTag = (ByteArrayTag) tag.getTag(name);
        return (byteArrayTag != null) ? byteArrayTag.getValue() : null;
    }

    protected final void setByteArray(String name, byte[] bytes) {
        tag.setTag(name, (bytes != null) ? new ByteArrayTag(name, bytes) : null);
    }

    protected final int[] getIntArray(String name) {
        IntArrayTag intArrayTag = (IntArrayTag) tag.getTag(name);
        return (intArrayTag != null) ? intArrayTag.getValue() : null;
    }

    protected final void setIntArray(String name, int[] values) {
        tag.setTag(name, (values != null) ? new IntArrayTag(name, values) : null);
    }

    protected final long[] getLongArray(String name) {
        LongArrayTag longArrayTag = (LongArrayTag) tag.getTag(name);
        return (longArrayTag != null) ? longArrayTag.getValue() : null;
    }

    protected final void setLongArray(String name, long[] values) {
        tag.setTag(name, (values != null) ? new LongArrayTag(name, values) : null);
    }

    protected final void removeTag(String name) {
        tag.setTag(name, null);
    }

    protected final Map<String, Tag> getAllTags(DataType type) {
        return tags.getOrDefault(type, EMPTY).getValue();
    }

    protected final boolean containsTag(DataType type, String name) {
        return tags.getOrDefault(type, EMPTY).containsTag(name);
    }

    protected final Tag getTag(DataType type, String name) {
        return tags.getOrDefault(type, EMPTY).getTag(name);
    }

    protected final void setTag(DataType type, String name, Tag tag) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, tag);
    }

    protected final Map<String, Tag> getMap(DataType type, String name) {
        CompoundTag compoundTag = (CompoundTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (compoundTag != null) ? compoundTag.getValue() : null;
    }

    protected final void setMap(DataType type, String name, Map<String, Tag> value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, (value != null) ? new CompoundTag(name, value) : null);
    }

    protected final long getLong(DataType type, String name) {
        return getLong(type, name, 0L);
    }

    protected final long getLong(DataType type, String name, long defaultValue) {
        LongTag longTag = (LongTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (longTag != null) ? longTag.getValue() : defaultValue;
    }

    protected final void setLong(DataType type, String name, long value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new LongTag(name, value));
    }

    protected final int getInt(DataType type, String name) {
        return getInt(type, name, 0);
    }

    protected final int getInt(DataType type, String name, int defaultValue) {
        IntTag intTag = (IntTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (intTag != null) ? intTag.getValue() : defaultValue;
    }

    protected final void setInt(DataType type, String name, int value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new IntTag(name, value));
    }

    protected final String getString(DataType type, String name) {
        return getString(type, name, null);
    }

    protected final String getString(DataType type, String name, String defaultValue) {
        StringTag stringTag = (StringTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (stringTag != null) ? stringTag.getValue() : defaultValue;
    }

    protected final void setString(DataType type, String name, String value) {
        if (value != null) {
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new StringTag(name, value));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final short getShort(DataType type, String name) {
        return getShort(type, name, (short) 0);
    }

    protected final short getShort(DataType type, String name, short defaultValue) {
        ShortTag shortTag = (ShortTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (shortTag != null) ? shortTag.getValue() : defaultValue;
    }

    protected final void setShort(DataType type, String name, short value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ShortTag(name, value));
    }

    protected final byte getByte(DataType type, String name) {
        return getByte(type, name, (byte) 0);
    }

    protected final byte getByte(DataType type, String name, byte defaultValue) {
        ByteTag byteTag = (ByteTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (byteTag != null) ? byteTag.getValue() : defaultValue;
    }

    protected final void setByte(DataType type, String name, byte value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ByteTag(name, value));
    }

    protected final boolean getBoolean(DataType type, String name) {
        return getBoolean(type, name, false);
    }

    protected final boolean getBoolean(DataType type, String name, boolean defaultValue) {
        ByteTag byteTag = (ByteTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (byteTag != null) ? (byteTag.getValue() != 0) : defaultValue;
    }

    protected final void setBoolean(DataType type, String name, boolean value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ByteTag(name, value ? (byte) 1 : (byte) 0));
    }

    protected final float getFloat(DataType type, String name) {
        return getFloat(type, name, 0.0f);
    }

    protected final float getFloat(DataType type, String name, float defaultValue) {
        FloatTag floatTag = (FloatTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (floatTag != null) ? floatTag.getValue() : defaultValue;
    }

    protected final void setFloat(DataType type, String name, float value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new FloatTag(name, value));
    }

    protected final double getDouble(DataType type, String name) {
        return getDouble(type, name, 0.0);
    }

    protected final double getDouble(DataType type, String name, double defaultValue) {
        DoubleTag doubleTag = (DoubleTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (doubleTag != null) ? doubleTag.getValue() : defaultValue;
    }

    protected final void setDouble(DataType type, String name, double value) {
        tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new DoubleTag(name, value));
    }

    protected final <T extends Tag> List<T> getList(DataType type, String name) {
        ListTag listTag = (ListTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (listTag != null) ? (List<T>) listTag.getValue() : null;
    }

    protected final <T extends Tag> void setList(DataType dataType, String name, Class<T> type, List<T> list) {
        if (list != null) {
            tags.computeIfAbsent(dataType, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ListTag(name, type, list));
        } else {
            tags.getOrDefault(dataType, EMPTY).setTag(name, null);
        }
    }

    protected final double[] getDoubleList(DataType type, String name) {
        List<DoubleTag> list = getList(type, name);
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

    protected final void setDoubleList(DataType type, String name, double[] values) {
        if (values != null) {
            List<Tag> list = new ArrayList<>(values.length);
            for (double value: values) {
                list.add(new DoubleTag(null, value));
            }
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ListTag(name, DoubleTag.class, list));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final float[] getFloatList(DataType type, String name) {
        List<FloatTag> list = getList(type, name);
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

    protected final void setFloatList(DataType type, String name, float[] values) {
        if (values != null) {
            List<Tag> list = new ArrayList<>(values.length);
            for (float value: values) {
                list.add(new FloatTag(null, value));
            }
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ListTag(name, FloatTag.class, list));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final byte[] getByteArray(DataType type, String name) {
        ByteArrayTag byteArrayTag = (ByteArrayTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (byteArrayTag != null) ? byteArrayTag.getValue() : null;
    }

    protected final void setByteArray(DataType type, String name, byte[] bytes) {
        if (bytes != null) {
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new ByteArrayTag(name, bytes));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final int[] getIntArray(DataType type, String name) {
        IntArrayTag intArrayTag = (IntArrayTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (intArrayTag != null) ? intArrayTag.getValue() : null;
    }

    protected final void setIntArray(DataType type, String name, int[] values) {
        if (values != null) {
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new IntArrayTag(name, values));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final long[] getLongArray(DataType type, String name) {
        LongArrayTag longArrayTag = (LongArrayTag) tags.getOrDefault(type, EMPTY).getTag(name);
        return (longArrayTag != null) ? longArrayTag.getValue() : null;
    }

    protected final void setLongArray(DataType type, String name, long[] values) {
        if (values != null) {
            tags.computeIfAbsent(type, t -> new CompoundTag("", new HashMap<>())).setTag(name, new LongArrayTag(name, values));
        } else {
            tags.getOrDefault(type, EMPTY).setTag(name, null);
        }
    }

    protected final void removeTag(DataType type, String name) {
        tags.getOrDefault(type, EMPTY).setTag(name, null);
    }

    @Override
    public String toString() {
        return (tag != null) ? tag.toString() : tags.toString();
    }

    @Override
    public AbstractNBTItem clone() {
        try {
            AbstractNBTItem clone = (AbstractNBTItem) super.clone();
            if (tag != null) {
                clone.tag = tag.clone();
            } else {
                clone.tags = new HashMap<>();
                tags.forEach((type, tag) -> clone.tags.put(type, tag.clone()));
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private CompoundTag tag;
    private Map<DataType, CompoundTag> tags;

    private static final CompoundTag EMPTY = new CompoundTag("", emptyMap());
    private static final long serialVersionUID = 1L;
}