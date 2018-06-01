package org.pepsoft.util;

import java.awt.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * An unlimited (except by the range of the <code>int</code> type)
 * two-dimensional field of bits, initially <code>false</code>.
 *
 * <p>This class is <strong>not</strong> thread-safe.
 */
public class BitField {
    /**
     * Set a bit.
     *
     * @param x The X coordinate of the bit to set.
     * @param y The Y coordinate of the bit to set.
     */
    public void set(int x, int y) {
        if ((x != cachedX) || (y != cachedY)) {
            cachedBits = booleans.computeIfAbsent(new Point(x >> 7, y >> 7), c -> new BitSet(16384));
            cachedX = x;
            cachedY = y;
        }
        if (cachedBits == null) {
            // This might happen if reset() or get() was previously invoked for
            // these coordinates and there was no bitset created for them yet
            cachedBits = new BitSet(16384);
            booleans.put(new Point(x >> 7, y >> 7), cachedBits);
        }
        cachedBits.set(((x & 0x7f) << 7) | (y & 0x7f));
    }

    /**
     * Reset a bit.
     *
     * @param x The X coordinate of the bit to reset.
     * @param y The Y coordinate of the bit to reset.
     */
    public void reset(int x, int y) {
        if ((x != cachedX) || (y != cachedY)) {
            cachedBits = booleans.get(new Point(x >> 7, y >> 7));
            cachedX = x;
            cachedY = y;
        }
        if (cachedBits != null) {
            cachedBits.clear(((x & 0x7f) << 7) | (y & 0x7f));
        }
    }

    /**
     * Get a bit.
     *
     * @param x The X coordinate of the bit to get.
     * @param y The Y coordinate of the bit to get.
     * @return <code>true</code> if the bit is set, <code>false</code>
     * otherwise.
     */
    public boolean get(int x, int y) {
        if ((x != cachedX) || (y != cachedY)) {
            cachedBits = booleans.get(new Point(x >> 7, y >> 7));
            cachedX = x;
            cachedY = y;
        }
        if (cachedBits != null) {
            return cachedBits.get(((x & 0x7f) << 7) | (y & 0x7f));
        } else {
            return false;
        }
    }

    /**
     * Get the bounding box of all the bits that are set.
     *
     * @return The bounding box of all the bits that are set, or
     * <code>null</code> if <em>no</em> bits are set.
     */
    public Rectangle getBoundingBox() {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Perform a task for all bits that are set.
     *
     * @param visitor The task to perform.
     * @return <code>true</code> if the visitor returned <code>true</code> for
     * every bit and every bit was therefore visited. <code>false</code> if the
     * visitor returned <code>false</code> for some bit and not all bits may
     * have been visited.
     */
    public boolean visitSetBits(BitVisitor visitor) {
        for (Map.Entry<Point, BitSet> entry: booleans.entrySet()) {
            int xOffset = entry.getKey().x << 7;
            int yOffset = entry.getKey().y << 7;
            BitSet bits = entry.getValue();
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
                int x = (i & 0x3f80) >> 7;
                int y = i & 0x7f;
                if (! visitor.visitBit(xOffset | x, yOffset | y, true)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int cachedX, cachedY;
    private BitSet cachedBits;

    private final Map<Point, BitSet> booleans = new HashMap<>();

    /**
     * A visitor of bits on a 2D plane.
     */
    @FunctionalInterface
    public interface BitVisitor {
        /**
         * Visit a specific bit.
         *
         * @param x The X coordinate of the bit on the 2D plane.
         * @param y The Y coordinate of the bit on the 2D plane.
         * @param bit Whether the bit is set.
         * @return Should return <code>true</code> to indicate that processing
         * should continue, or <code>false</code> to indicate that processing
         * may be aborted.
         */
        boolean visitBit(int x, int y, boolean bit);
    }
}