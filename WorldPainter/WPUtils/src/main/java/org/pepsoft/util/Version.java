/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A version number in decimal dotted notation, with an optional alphanumerical
 * extension separated by a dash.
 *
 * <p>When comparing the extension is only considered if the versions being
 * compared are numerically equal, where a version with an extension is
 * considered smaller than a version without an extension, and if both versions
 * have an extension they are compared alphanumerically.
 *
 * @author pepijn
 */
public final class Version implements Comparable<Version>, Serializable {
    public Version(int... parts) {
        this(parts, null);
    }

    public Version(int part0, String extension) {
        this(new int[] { part0 }, extension);
    }

    public Version(int part0, int part1, String extension) {
        this(new int[] { part0, part1 }, extension);
    }

    public Version(int part0, int part1, int part2,  String extension) {
        this(new int[] { part0, part1, part2 }, extension);
    }

    public Version(int[] parts, String extension) {
        this.parts = parts.clone();
        this.extension = extension;
        for (int part: parts) {
            if (part < 0) {
                throw new IllegalArgumentException("Negative numbers not allowed");
            }
        }
    }

    public int[] getParts() {
        return parts.clone();
    }

    public String getExtension() {
        return extension;
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
        if (this.extension != null) {
            if (o.extension != null) {
                return this.extension.compareTo(o.extension);
            } else {
                // A version with an extension (e.g. -SNAPSHOT) is considered newer than an otherwise identical version
                // without an extension
                return 1;
            }
        } else if (o.extension != null) {
            // A version without an extension is considered older than an otherwise identical version with an extension
            // (e.g. -SNAPSHOT)
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Arrays.hashCode(this.parts);
        hash = 41 * hash + Objects.hashCode(extension);
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
        if (! Arrays.equals(this.parts, other.parts)) {
            return false;
        }
        return Objects.equals(this.extension, other.extension);
    }

    @Override
    public String toString() {
        return Arrays.stream(parts).mapToObj(Integer::toString).collect(Collectors.joining("."))
                + ((extension != null) ? ("-" + extension) : "");
    }

    /**
     * Create a new {@code Version} from a string of the form
     * {@code x.y.z} or {@code x.y.z-EXT} (with any number of parts).
     *
     * @param str The string to parse.
     * @return The resulting Version object.
     * @throws NumberFormatException If there are non-numeric characters in the
     *     string.
     */
    public static Version parse(String str) {
        String extension;
        int p = str.indexOf('-');
        if (p != -1) {
            extension = str.substring(p + 1);
            str = str.substring(0, p);
        } else {
            extension = null;
        }
        String[] partStrs = str.split("\\.");
        int[] parts = new int[partStrs.length];
        for (int i = 0; i < partStrs.length; i++) {
            parts[i] = Integer.parseInt(partStrs[i]);
        }
        return new Version(parts, extension);
    }

    private final int[] parts;
    private final String extension;

    private static final long serialVersionUID = 1L;
}