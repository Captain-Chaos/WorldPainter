package org.pepsoft.util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A configurable-sized cube of values packed into a {@code long} array of indexes and a linear palette.
 */
public class PackedArrayCube<T> {
    /**
     * Create an empty packed array cube of the specified size.
     *
     * @param size            Length of one edge of the cube.
     * @param minimumWordSize The minimum word size for the packed data. In Minecraft this varies, from 4 for block
     *                        states (resulting in unnecessarily large arrays) to 1 for biomes.
     */
    @SuppressWarnings("unchecked") // Guaranteed by Java library
    public PackedArrayCube(int size, int minimumWordSize, Class<T> type) {
        this.minimumWordSize = minimumWordSize;
        this.type = type;
        bitsPerCoordinate = (int) Math.ceil(Math.log(size) / Math.log(2));
        arraySize = size * size * size;
        values = (T[]) Array.newInstance(type, arraySize);
    }

    /**
     * Create a packed array cube of the specified size by unpacking an existing data array and palette.
     *
     * @param size            Length of one edge of the cube.
     * @param data            The data array to unpack.
     * @param palette         The palette of values.
     * @param minimumWordSize The minimum word size for the packed data. In Minecraft this varies, from 4 for block
     *                        states (resulting in unnecessarily large arrays) to 1 for biomes.
     */
    public PackedArrayCube(int size, long[] data, T[] palette, int minimumWordSize, Class<T> type) {
        this(size, minimumWordSize, type);

        final int wordSize = Math.max(minimumWordSize, (int) Math.ceil(Math.log(palette.length) / Math.log(2)));
        final int expectedPackedDataArrayLengthInBytes = wordSize * arraySize / 8;
        final int dataArrayLengthInBytes = data.length * 8;
        if (wordSize == 4) {
            // Optimised special case
            for (int w = 0; w < arraySize; w += 16) {
                final long arrayValue = data[w >> 4];
                values[w]      = palette[(int) (arrayValue  & 0x000000000000000fL)];
                values[w +  1] = palette[(int) ((arrayValue & 0x00000000000000f0L) >>   4)];
                values[w +  2] = palette[(int) ((arrayValue & 0x0000000000000f00L) >>   8)];
                values[w +  3] = palette[(int) ((arrayValue & 0x000000000000f000L) >>  12)];
                values[w +  4] = palette[(int) ((arrayValue & 0x00000000000f0000L) >>  16)];
                values[w +  5] = palette[(int) ((arrayValue & 0x0000000000f00000L) >>  20)];
                values[w +  6] = palette[(int) ((arrayValue & 0x000000000f000000L) >>  24)];
                values[w +  7] = palette[(int) ((arrayValue & 0x00000000f0000000L) >>  28)];
                values[w +  8] = palette[(int) ((arrayValue & 0x0000000f00000000L) >>  32)];
                values[w +  9] = palette[(int) ((arrayValue & 0x000000f000000000L) >>  36)];
                values[w + 10] = palette[(int) ((arrayValue & 0x00000f0000000000L) >>  40)];
                values[w + 11] = palette[(int) ((arrayValue & 0x0000f00000000000L) >>  44)];
                values[w + 12] = palette[(int) ((arrayValue & 0x000f000000000000L) >>  48)];
                values[w + 13] = palette[(int) ((arrayValue & 0x00f0000000000000L) >>  52)];
                values[w + 14] = palette[(int) ((arrayValue & 0x0f00000000000000L) >>  56)];
                values[w + 15] = palette[(int) ((arrayValue & 0xf000000000000000L) >>> 60)];
            }
        } else if (dataArrayLengthInBytes != expectedPackedDataArrayLengthInBytes) {
            // A weird format where the values are packed per long (leaving bits unused). Unpack each long individually
            final long mask = (long) (Math.pow(2, wordSize)) - 1;
            final int bitsInUse = (64 / wordSize) * wordSize;
            int materialIndex = 0;
            outer:
            for (long packedData: data) {
                for (int offset = 0; offset < bitsInUse; offset += wordSize) {
                    values[materialIndex++] = palette[(int) ((packedData & (mask << offset)) >>> offset)];
                    if (materialIndex >= arraySize) {
                        // The last long was not fully used
                        break outer;
                    }
                }
            }
        } else {
            final BitSet bitSet = BitSet.valueOf(data);
            for (int w = 0; w < arraySize; w++) {
                final int wordOffset = w * wordSize;
                int index = 0;
                for (int b = 0; b < wordSize; b++) {
                    index |= bitSet.get(wordOffset + b) ? 1 << b : 0;
                }
                values[w] = palette[index];
            }
        }
    }

    public T getValue(int x, int y, int z) {
        return values[offset(x, y, z)];
    }

    public void setValue(int x, int y, int z, T value) {
        values[offset(x, y, z)] = value;
    }

    public void fill(T value) {
        Arrays.fill(values, value);
    }

    public boolean isEmpty() {
        for (int i = 0; i < arraySize; i++) {
            if (values[i] != null) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked") // Guaranteed by Java library
    public PackedData pack() {
        // Create the palette. We have to do this first, because otherwise we don't know how many bits the indices will
        // be and therefore how big to make the data array
        final Map<T, Integer> reversePalette = new HashMap<>();
        final List<T> palette = new LinkedList<>();
        for (T value: values) {
            if (! reversePalette.containsKey(value)) {
                reversePalette.put(value, palette.size());
                palette.add(value);
            }
        }

        // Create the data array and fill it, using the appropriate length palette indices so that it just fits
        final int paletteIndexSize = Math.max((int) Math.ceil(Math.log(palette.size()) / Math.log(2)), minimumWordSize);
        final long[] data;
        if ((paletteIndexSize == 4) && ((values.length % 16) == 0)) {
            // Optimised special case
            data = new long[values.length >> 4];
            for (int i = 0; i < values.length; i += 16) {
                data[i >> 4] =
                       reversePalette.get(values[i]     )
                    | (reversePalette.get(values[i +  1]) << 4)
                    | (reversePalette.get(values[i +  2]) << 8)
                    | (reversePalette.get(values[i +  3]) << 12)
                    | (reversePalette.get(values[i +  4]) << 16)
                    | (reversePalette.get(values[i +  5]) << 20)
                    | (reversePalette.get(values[i +  6]) << 24)
                    | ((long) (reversePalette.get(values[i +  7])) << 28)
                    | ((long) (reversePalette.get(values[i +  8])) << 32)
                    | ((long) (reversePalette.get(values[i +  9])) << 36)
                    | ((long) (reversePalette.get(values[i + 10])) << 40)
                    | ((long) (reversePalette.get(values[i + 11])) << 44)
                    | ((long) (reversePalette.get(values[i + 12])) << 48)
                    | ((long) (reversePalette.get(values[i + 13])) << 52)
                    | ((long) (reversePalette.get(values[i + 14])) << 56)
                    | ((long) (reversePalette.get(values[i + 15])) << 60);
            }
        } else {
            final boolean straddleLongs = false;
            if (straddleLongs) { // TODOMC118 is this ever the case for Minecraft 1.18+?
                final BitSet dataBits = new BitSet(arraySize * paletteIndexSize);
                for (int i = 0; i < arraySize; i++) {
                    final int offset = i * paletteIndexSize;
                    final int index = reversePalette.get(values[i]);
                    for (int j = 0; j < paletteIndexSize; j++) {
                        if ((index & (1 << j)) != 0) {
                            dataBits.set(offset + j);
                        }
                    }
                }
                final long[] dataArray = dataBits.toLongArray();
                // Pad with zeros if necessary TODOMC118 why?
                final int requiredLength = 64 * paletteIndexSize; // TODOMC118 where does this 64 come from?
                if (dataArray.length != requiredLength) {
                    final long[] expandedArray = new long[requiredLength];
                    System.arraycopy(dataArray, 0, expandedArray, 0, dataArray.length);
                    data = expandedArray;
                } else {
                    data = dataArray;
                }
            } else {
                final int wordsPerLong = 64 / paletteIndexSize;
                final int dataSize = arraySize / wordsPerLong + (((arraySize % wordsPerLong) == 0) ? 0 : 1); // Round up
                final BitSet dataBits = new BitSet(dataSize * 64);
                for (int i = 0; i < arraySize; i++) {
                    final int offset = (i / wordsPerLong) * 64 + (i % wordsPerLong) * paletteIndexSize;
                    final int index = reversePalette.get(values[i]);
                    for (int j = 0; j < paletteIndexSize; j++) {
                        if ((index & (1 << j)) != 0) {
                            dataBits.set(offset + j);
                        }
                    }
                }
                final long[] dataArray = dataBits.toLongArray();
                if (dataArray.length == dataSize) {
                    data = dataArray;
                } else {
                    // If the last bits of the BitSet are zero, toLongArray() does not return those longs, but
                    // Minecraft can't handle that
                    data = Arrays.copyOf(dataArray, dataSize);
                }
            }
        }
        return new PackedData(data, palette.toArray((T[]) Array.newInstance(type, palette.size())));
    }

    private int offset(int x, int y, int z) {
        return x | ((y | (z << bitsPerCoordinate)) << bitsPerCoordinate);
    }

    private final Class<T> type;
    private final int arraySize, minimumWordSize, bitsPerCoordinate;
    private final T[] values;

    public class PackedData {
        public PackedData(long[] data, T[] palette) {
            this.data = data;
            this.palette = palette;
        }

        public final long[] data;
        public final T[] palette;
    }
}