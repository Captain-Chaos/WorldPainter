/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.util.Arrays;

/**
 * A utility class that wraps a byte array, making the <code>equals()</code> and
 * <code>hashCode()</code> methods work as expected, so that it can be used as
 * a key in <code>Set</code>s and <code>Map</code>s.
 *
 * @author pepijn
 */
public class Checksum {
    public Checksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public byte[] getBytes() {
        return checksum.clone();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Checksum)
            && Arrays.equals(checksum, ((Checksum) obj).checksum);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(checksum);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(checksum.length * 2);
        for (byte aChecksum : checksum) {
            int _byte = aChecksum & 0xFF;
            if (_byte < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(_byte));
        }
        return sb.toString();
    }
    
    private final byte[] checksum;
}