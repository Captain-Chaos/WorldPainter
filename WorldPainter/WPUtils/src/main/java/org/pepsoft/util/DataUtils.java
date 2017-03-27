package org.pepsoft.util;

import java.io.DataInput;
import java.io.IOException;

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

    private static final long MAX_UINT_32 = 4294967296L;
}