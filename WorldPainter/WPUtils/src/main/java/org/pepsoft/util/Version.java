/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author pepijn
 */
public final class Version implements Comparable<Version>, Serializable {
    public Version(int... parts) {
        this.parts = parts.clone();
        for (int part: parts) {
            if (part < 0) {
                throw new IllegalArgumentException("Negative numbers not allowed");
            }
        }
    }

    public int[] getParts() {
        return parts.clone();
    }

    public boolean isAtLeast(Version version) {
        return compareTo(version) >= 0;
    }

    @Override
    public int compareTo(Version o) {
        for (int i = 0; i < Math.max(parts.length, o.parts.length); i++) {
            if (i < parts.length) {
                if (i < o.parts.length) {
                    // Part present in both
                    if (parts[i] < o.parts[i]) {
                        return -1;
                    } else if (parts[i] > o.parts[i]) {
                        return 1;
                    }
                    // Parts are the same, continue to next part (if any)
                } else {
                    // Part only present in us; assume other part is 0
                    if (parts[i] > 0) {
                        return 1;
                    }
                    // Parts are the same, continue to next part (if any)
                }
            } else {
                // Part only present in other; assume our part is 0
                if (o.parts[i] > 0) {
                    return -1;
                }
                // Parts are the same, continue to next part (if any)
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Arrays.hashCode(this.parts);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Version other = (Version) obj;
        if (!Arrays.equals(this.parts, other.parts)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Arrays.stream(parts).mapToObj(Integer::toString).collect(Collectors.joining("."));
    }

    /**
     * Create a new <code>Version</code> from a string of the form
     * <code>x.y.z</code> (with any number of parts).
     *
     * @param str The string to parse.
     * @return The resulting Version object.
     * @throws NumberFormatException If there are non-numeric characters in the
     *     string.
     */
    public static Version parse(String str) {
        String[] partStrs = str.split("\\.");
        int[] parts = new int[partStrs.length];
        for (int i = 0; i < partStrs.length; i++) {
            parts[i] = Integer.parseInt(partStrs[i]);
        }
        return new Version(parts);
    }

    private final int[] parts;

    private static final long serialVersionUID = 1L;
}