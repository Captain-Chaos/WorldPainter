package org.pepsoft.util;

import java.lang.reflect.Array;
import java.util.*;

public class Palleted16Section<T> {
    public Palleted16Section(Class<T> type) {
        this.type = type;
        this.data = new PalletData<>(type);
    }

    public Palleted16Section(int size, long[] data, T[] palette, int minimumWordSize, boolean straddleLongs, Class<T> type) {
        this(type);

        // Sanity check
        for (int i = 0; i < palette.length; i++) {
            if ((palette[i] != null) && (! type.isAssignableFrom(palette[i].getClass()))) {
                throw new IllegalArgumentException("Palette[" + i + "] is not a " + type.getSimpleName() + " (actual type: " + palette[i].getClass().getName() + "; value: " + palette[i] + ")");
            }
        }

        final int wordSize = Math.max(minimumWordSize, (int) Math.ceil(Math.log(palette.length) / Math.log(2)));
        final int expectedPackedDataArrayLengthInBytes = wordSize * 4096 / 8;
        final int dataArrayLengthInBytes = data.length * 8;
        if (wordSize == 4) {
            // Optimised special case
            for (int w = 0; w < 4096; w += 16) {
                final long arrayValue = data[w >> 4];
                this.data.set(w     , palette[(int) (arrayValue  & 0x000000000000000fL)        ]);
                this.data.set(w +  1, palette[(int) ((arrayValue & 0x00000000000000f0L) >>   4)]);
                this.data.set(w +  2, palette[(int) ((arrayValue & 0x0000000000000f00L) >>   8)]);
                this.data.set(w +  3, palette[(int) ((arrayValue & 0x000000000000f000L) >>  12)]);
                this.data.set(w +  4, palette[(int) ((arrayValue & 0x00000000000f0000L) >>  16)]);
                this.data.set(w +  5, palette[(int) ((arrayValue & 0x0000000000f00000L) >>  20)]);
                this.data.set(w +  6, palette[(int) ((arrayValue & 0x000000000f000000L) >>  24)]);
                this.data.set(w +  7, palette[(int) ((arrayValue & 0x00000000f0000000L) >>  28)]);
                this.data.set(w +  8, palette[(int) ((arrayValue & 0x0000000f00000000L) >>  32)]);
                this.data.set(w +  9, palette[(int) ((arrayValue & 0x000000f000000000L) >>  36)]);
                this.data.set(w + 10, palette[(int) ((arrayValue & 0x00000f0000000000L) >>  40)]);
                this.data.set(w + 11, palette[(int) ((arrayValue & 0x0000f00000000000L) >>  44)]);
                this.data.set(w + 12, palette[(int) ((arrayValue & 0x000f000000000000L) >>  48)]);
                this.data.set(w + 13, palette[(int) ((arrayValue & 0x00f0000000000000L) >>  52)]);
                this.data.set(w + 14, palette[(int) ((arrayValue & 0x0f00000000000000L) >>  56)]);
                this.data.set(w + 15, palette[(int) ((arrayValue & 0xf000000000000000L) >>> 60)]);
            }
        } else if (dataArrayLengthInBytes != expectedPackedDataArrayLengthInBytes) {
            // A weird format where the values are packed per long (leaving bits unused). Unpack each long individually
            final long mask = (long) (Math.pow(2, wordSize)) - 1;
            final int bitsInUse = (64 / wordSize) * wordSize;
            int materialIndex = 0;
            outer:
            for (long packedData: data) {
                for (int offset = 0; offset < bitsInUse; offset += wordSize) {
                    this.data.set(materialIndex++, palette[(int) ((packedData & (mask << offset)) >>> offset)]);
                    if (materialIndex >= 4096) {
                        // The last long was not fully used
                        break outer;
                    }
                }
            }
        } else {
            final BitSet bitSet = BitSet.valueOf(data);
            for (int w = 0; w < 4096; w++) {
                final int wordOffset = w * wordSize;
                int index = 0;
                for (int b = 0; b < wordSize; b++) {
                    index |= bitSet.get(wordOffset + b) ? 1 << b : 0;
                }
                this.data.set(w, palette[index]);
            }
        }
    }

    private static int idx(int x, int y, int z) {return x+(16*y)+(16*16*z);}
    public T getValue(int x, int y, int z) {
        return this.data.get(idx(x,y,z));
    }

    public void setValue(int x, int y, int z, T value) {
        this.data.set(idx(x,y,z), value);
    }

    public void fill(T value) {
        this.data.fill(value);
    }

    public boolean isEmpty() {
        for (long a : this.data.data) {
            if (a!=0) return false;
        }
        return true;
    }

    /**
     * Pack the data into a palette and a {@code long} array. {@code null} values are not replaced and if any of the
     * values are {@code null}, the palette will contain a {@code null} entry.
     *
     * @return The packed data.
     */
    public PackedArrayCube.PackedData<T> pack() {
        return pack(null);
    }

    /**
     * Pack the data into a palette and a {@code long} array.
     *
     * @param nullSubstitute The value to replace {@code null} values with, if any. May be {@code null}, in which case
     *                       one of the palette entries may be {@code null}.
     * @return The packed data.
     */
    @SuppressWarnings("unchecked") // Guaranteed by Java library
    public PackedArrayCube.PackedData<T> pack(T nullSubstitute) {
        // Create the palette. We have to do this first, because otherwise we don't know how many bits the indices will
        // be and therefore how big to make the data array
        final Map<T, Integer> reversePalette = new HashMap<>();
        final List<T> palette = new LinkedList<>();
        for (int i = 0; i < 4096; i++) {
            T value = this.data.get(i);
            if (value == null) {
                value = nullSubstitute;
            }
            if (! reversePalette.containsKey(value)) {
                reversePalette.put(value, palette.size());
                palette.add(value);
            }
        }

        // Create the data array and fill it, using the appropriate length palette indices so that it just fits
        final int paletteIndexSize = Math.max((int) Math.ceil(Math.log(palette.size()) / Math.log(2)), 4);
        final long[] data;
        if (paletteIndexSize == 4) {
            // Optimised special case
            data = new long[4096 >> 4];
            for (int i = 0; i < 4096; i += 16) {
                data[i >> 4] =
                               reversePalette.get(substituteNull(this.data.get(i     ),  nullSubstitute))
                    |         (reversePalette.get(substituteNull(this.data.get(i +  1), nullSubstitute))  <<  4)
                    |         (reversePalette.get(substituteNull(this.data.get(i +  2), nullSubstitute))  <<  8)
                    |         (reversePalette.get(substituteNull(this.data.get(i +  3), nullSubstitute))  << 12)
                    |         (reversePalette.get(substituteNull(this.data.get(i +  4), nullSubstitute))  << 16)
                    |         (reversePalette.get(substituteNull(this.data.get(i +  5), nullSubstitute))  << 20)
                    |         (reversePalette.get(substituteNull(this.data.get(i +  6), nullSubstitute))  << 24)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i +  7), nullSubstitute))) << 28)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i +  8), nullSubstitute))) << 32)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i +  9), nullSubstitute))) << 36)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 10), nullSubstitute))) << 40)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 11), nullSubstitute))) << 44)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 12), nullSubstitute))) << 48)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 13), nullSubstitute))) << 52)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 14), nullSubstitute))) << 56)
                    | ((long) (reversePalette.get(substituteNull(this.data.get(i + 15), nullSubstitute))) << 60);
            }
        } else {
            final int wordsPerLong = 64 / paletteIndexSize;
            final int dataSize = 4096 / wordsPerLong + (((4096 % wordsPerLong) == 0) ? 0 : 1); // Round up
            final BitSet dataBits = new BitSet(dataSize * 64);
            for (int i = 0; i < 4096; i++) {
                final int offset = (i / wordsPerLong) * 64 + (i % wordsPerLong) * paletteIndexSize;
                final int index = reversePalette.get(substituteNull(this.data.get(i), nullSubstitute));
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
        return new PackedArrayCube.PackedData<T>(data, palette.toArray((T[]) Array.newInstance(type, palette.size())));
    }

    private static <J> J substituteNull(J value, J nullSubstitute) {
        return (value == null) ? nullSubstitute : value;
    }

    private final Class<T> type;
    private final PalletData<T> data;


    //2,4,8,16 pallet sizes
    private static final class PalletData<J> {
        private long[] data;
        private J[] pallet;
        private int bits;
        private int ibits;

        public PalletData(Class<J> clz) {
            this.bits = 2; this.ibits = Integer.numberOfTrailingZeros(64/this.bits);
            this.data = new long[4096/(64/this.bits)];
            this.pallet = (J[]) Array.newInstance(clz, 1<<this.bits);
        }

        public J get(int idx) {
            long a = this.data[idx>>this.ibits];
            idx = (idx&((1<<this.ibits)-1))*this.bits;
            return this.pallet[(int) ((a>>idx)&((1<<this.bits)-1))];
        }

        public void set(int idx, J obj) {
            long objId = this.findOrGrow(obj);

            int p = idx>>this.ibits;
            idx = (idx&((1<<this.ibits)-1))*this.bits;
            long msk = ((1L<<this.bits)-1)<<idx;
            objId <<= idx;
            long a = this.data[p];
            a&=~msk;
            a|=objId;
            this.data[p] = a;
        }

        private int findOrGrow(J obj) {
            if (obj == null) {return 0;}
            //Idx 0 is specialcase
            for (int i = 1; i < this.pallet.length; i++) {
                J v = this.pallet[i];
                if (v == obj) {//object test
                    return i;
                } else if (v == null) {
                    this.pallet[i] = obj;
                    return i;
                }
            }
            int id = this.pallet.length;
            this.pallet = Arrays.copyOf(this.pallet, this.pallet.length*2);//create new size pallet
            this.pallet[id] = obj;
            if (this.pallet.length == 1<<(this.bits+1)) {
                int ob = this.bits;
                this.bits *= 2;
                this.ibits = Integer.numberOfTrailingZeros(64/this.bits);

                long[] newData = new long[4096/(64/this.bits)];
                for (int i = 0; i < this.data.length; i++) {
                    long a = this.data[i];
                    newData[i*2] = expand(a&0xFFFFFFFFL,ob);
                    newData[i*2+1] = expand(a>>>32,ob);
                }
                this.data = newData;
            }
            return id;
        }

        private static long expand(long in, int bits) {
            long out = 0;
            long msk = (1L<<bits)-1;
            for (int i = 0; i < 32/bits; i++) {
                out |= (in&msk)<<(bits*2*i);
                in>>>=bits;
            }
            return out;
        }

        public void fill(J value) {
            if (value == null) {
                Arrays.fill(this.pallet, null);
                Arrays.fill(this.data, 0);
            } else {
                if (this.bits != 2) {
                    this.bits = 2;this.ibits = Integer.numberOfTrailingZeros(64/this.bits);
                    this.data = new long[4096/(64/this.bits)];
                    this.pallet = (J[]) Array.newInstance(this.pallet.getClass().componentType(), 1<<this.bits);
                }
                this.pallet[1] = value;
                Arrays.fill(this.data, 0b01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01_01L);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(Long.toBinaryString(PalletData.expand(0b101,2)));
        PalletData<Object> test = new PalletData<>(Object.class);
        Object[] aa = new Object[4096];
        for (int i = 0; i < 4096; i++) {
            aa[i] = new Object();
            test.set(i, aa[i]);
            if (test.get(i) != aa[i]) {
                test.get(i);
                throw new IllegalStateException();
            }
        }
        for (int i = 0; i < 4096; i++) {
            if (test.get(i) != aa[i]) {
                throw new IllegalStateException();
            }
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
        for (int i = 0; i < 4096; i++) {
            test.set(i, new Object());
        }
    }
}