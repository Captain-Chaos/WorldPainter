package org.pepsoft.util;

import java.io.DataInput;
import java.io.IOException;
import java.util.BitSet;

/**
 * Utility methods for working with data.
 *
 * <p>Created by Pepijn on 9-3-2017.
 */
public final class DataUtils {
    private DataUtils() {
        // Prevent instantiation
    }

    /**
     * Read an unsigned 32-bit integer from a data stream.
     *
     * @param in The stream to read from.
     * @return The next four bytes in the stream as an unsigned 32-bit integer.
     * @throws IOException If the stream throws an <code>IOException</code>
     * while reading the bytes.
     */
    public static long readUnsignedInt(DataInput in) throws IOException {
        int _int = in.readInt();
        return (_int < 0) ? MAX_UINT_32 + _int : _int;
    }

    /**
     * Decode data that consists of a matrix of equally sized numbers packed
     * into a one dimensional array.
     *
     * @param data        The data array to unpack.
     * @param wordSize    The word size in bits into which the numbers have been
     *                    packed.
     * @param columnCount The number of words per line or row of the original
     *                    matrix.
     * @return The unpacked data.
     */
    public static long[][] unpackDataArray(Object data, int wordSize, int columnCount) {
        BitSet bitSet;
        int lengthInWords;
        if (data instanceof byte[]) {
            bitSet = BitSet.valueOf((byte[]) data);
            lengthInWords = ((byte[]) data).length * 8 / wordSize;
        } else if (data instanceof long[]) {
            bitSet = BitSet.valueOf((long[]) data);
            lengthInWords = ((long[]) data).length * 64 / wordSize;
        } else if (data instanceof int[]) {
            int[] dataAsInts = (int[]) data;
            if (dataAsInts.length % 2 == 0) {
                long[] dataAsLongs = new long[dataAsInts.length / 2];
                for (int i = 0; i < dataAsLongs.length; i++) {
                    dataAsLongs[i] = (dataAsInts[i * 2] & 0x00000000ffffffffL) | ((long) dataAsInts[i * 2 + 1] << 32);
                }
                bitSet = BitSet.valueOf(dataAsLongs);
                lengthInWords = dataAsLongs.length * 64 / wordSize;
            } else {
                throw new IllegalArgumentException("Don't know how to process data of type int[] and odd length");
            }
        } else {
            throw new IllegalArgumentException("Don't know how to process data of type " + data.getClass());
        }
        // TODO: special case 2^n bit word sizes for performance
        int rowCount = (int) Math.ceil((double) lengthInWords / columnCount);
        long[][] matrix = new long[rowCount][];
        for (int row = 0; row < rowCount; row++) {
            matrix[row] = new long[columnCount];
            for (int column = 0; column < columnCount; column++) {
                int wordOffset = column * wordSize;
                long word = 0;
                for (int b = 0; b < wordSize; b++) {
                    word |= bitSet.get(wordOffset + b) ? 1L << b : 0L;
                }
                matrix[row][column] = word;
            }
        }
        return matrix;
    }

    private static final long MAX_UINT_32 = 4294967296L;
}