/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import org.pepsoft.util.undo.*;
import org.pepsoft.util.undo.Cloneable;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.util.*;
import java.util.List;

/**
 *
 * @author SchmitzP
 */
public final class ObjectUtils {
    private ObjectUtils() {
        // Prevent instantiation
    }

    /**
     * Make a deep copy of an object. Only a restricted set of types is
     * supported.
     *
     * @param <T> The type of the object.
     * @param object The object to copy.
     * @return A deep copy of the object.
     * @throws OutOfMemoryError If there is not enough memory to copy the
     *     object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T copyObject(T object) {
        // Point isn't actually immutable, but it is used as such by WorldPainter, at least in all data structures
        // managed by an undo manager
        if ((object == null) || (object instanceof Number) || (object instanceof Character)
                || (object instanceof Boolean) || (object instanceof String) || (object instanceof Enum)
                || (object instanceof Point) || (object instanceof Immutable)) {
            // Object is null or immutable; making a copy not necessary
            return object;
        } else {
            if (object instanceof BitSet) {
                return (T) ((BitSet) object).clone();
            } else if (object instanceof EnumSet) {
                return (T) ((EnumSet) object).clone();
            } else if (object instanceof byte[]) {
                return (T) ((byte[]) object).clone();
            } else if (object instanceof short[]) {
                return (T) ((short[]) object).clone();
            } else if (object instanceof int[]) {
                return (T) ((int[]) object).clone();
            } else if (object instanceof long[]) {
                return (T) ((long[]) object).clone();
            } else if (object instanceof float[]) {
                return (T) ((float[]) object).clone();
            } else if (object instanceof double[]) {
                return (T) ((double[]) object).clone();
            } else if (object instanceof String[]) {
                return (T) ((String[]) object).clone();
            } else if (object instanceof Map) {
                final Map<Object, Object> copy;
                if (object instanceof SortedMap) {
                    copy = new TreeMap<>();
                } else {
                    copy = new HashMap<>(((Map) object).size());
                }
                for (Map.Entry entry: ((Map<?, ?>) object).entrySet()) {
                    // TODO: map keys are never copied, should we document that?
                    copy.put(entry.getKey(), copyObject(entry.getValue()));
                }
                return (T) copy;
            } else if (object instanceof List) {
                final List<Object> copy;
                if (object instanceof RandomAccess) {
                    copy = new ArrayList<>(((List) object).size());
                } else {
                    copy = new LinkedList<>();
                }
                for (Object entry: (List) object) {
                    copy.add(copyObject(entry));
                }
                return (T) copy;
            } else if (object instanceof Set) {
                final Set<Object> copy;
                if (object instanceof SortedSet) {
                    copy = new TreeSet<>();
                } else {
                    copy = new HashSet<>(((Set) object).size());
                }
                for (Object entry: (Set) object) {
                    copy.add(copyObject(entry));
                }
                return (T) copy;
            } else if (object instanceof Cloneable) {
                return ((Cloneable<T>) object).clone();
            } else {
                throw new UnsupportedOperationException("Don't know how to copy a " + object.getClass());
            }
        }
    }

    public static DataBuffer clone(DataBuffer dataBuffer) {
        if (dataBuffer instanceof DataBufferByte) {
            return clone((DataBufferByte) dataBuffer);
        } else if (dataBuffer instanceof DataBufferDouble) {
            return clone((DataBufferDouble) dataBuffer);
        } else if (dataBuffer instanceof DataBufferFloat) {
            return clone((DataBufferFloat) dataBuffer);
        } else if (dataBuffer instanceof DataBufferInt) {
            return clone((DataBufferInt) dataBuffer);
        } else if (dataBuffer instanceof DataBufferShort) {
            return clone((DataBufferShort) dataBuffer);
        } else if (dataBuffer instanceof DataBufferUShort) {
            return clone((DataBufferUShort) dataBuffer);
        } else {
            throw new UnsupportedOperationException("Don't know how to clone " + dataBuffer.getClass().getName());
        }
    }
    
    public static DataBufferByte clone(DataBufferByte dataBuffer) {
        return new DataBufferByte(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }

    public static DataBufferDouble clone(DataBufferDouble dataBuffer) {
        return new DataBufferDouble(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }

    public static DataBufferFloat clone(DataBufferFloat dataBuffer) {
        return new DataBufferFloat(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }

    public static DataBufferInt clone(DataBufferInt dataBuffer) {
        return new DataBufferInt(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }
    
    public static DataBufferShort clone(DataBufferShort dataBuffer) {
        return new DataBufferShort(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }

    public static DataBufferUShort clone(DataBufferUShort dataBuffer) {
        return new DataBufferUShort(clone(dataBuffer.getBankData()), dataBuffer.getSize(), dataBuffer.getOffsets());
    }
    
    public static byte[][] clone(byte[][] array) {
        byte[][] clone = new byte[array.length][];
        for (int i = 0; i < array.length; i++) {
            clone[i] = array[i].clone();
        }
        return clone;
    }

    public static double[][] clone(double[][] array) {
        double[][] clone = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            clone[i] = array[i].clone();
        }
        return clone;
    }

    public static float[][] clone(float[][] array) {
        float[][] clone = new float[array.length][];
        for (int i = 0; i < array.length; i++) {
            clone[i] = array[i].clone();
        }
        return clone;
    }

    public static int[][] clone(int[][] array) {
        int[][] clone = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            clone[i] = array[i].clone();
        }
        return clone;
    }

    public static short[][] clone(short[][] array) {
        short[][] clone = new short[array.length][];
        for (int i = 0; i < array.length; i++) {
            clone[i] = array[i].clone();
        }
        return clone;
    }
}