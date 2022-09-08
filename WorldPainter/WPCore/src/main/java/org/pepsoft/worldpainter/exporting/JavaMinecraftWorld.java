/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Platform;

import java.io.File;

/**
 *
 * @author pepijn
 */
public class JavaMinecraftWorld extends CachingMinecraftWorld {
    public JavaMinecraftWorld(File worldDir, int dimension, int maxHeight, Platform platform, boolean readOnly, int cacheSize) {
        super(worldDir, dimension, maxHeight, platform, readOnly, cacheSize);
        if (! worldDir.isDirectory()) {
            throw new IllegalArgumentException(worldDir + " does not exist or is not a directory");
        }
    }
}