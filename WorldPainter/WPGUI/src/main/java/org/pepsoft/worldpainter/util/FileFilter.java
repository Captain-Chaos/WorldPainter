package org.pepsoft.worldpainter.util;

public abstract class FileFilter extends javax.swing.filechooser.FileFilter {
    /**
     * Get the list of filename extensions to allow, as a semicolon-separated list of wildcard patterns, without spaces.
     * (e.g. {@code *.png;.jpg;*.jpeg}.
     */
    public abstract String getExtensions();
}