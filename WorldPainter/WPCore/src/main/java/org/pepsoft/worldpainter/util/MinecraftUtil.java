/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import java.io.File;

/**
 *
 * @author pepijn
 */
public class MinecraftUtil {
    private MinecraftUtil() {
        // Prevent instantiation
    }
    
    public static File findMinecraftDir() {
        File candidate;
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            candidate = new File(appData, ".minecraft");
            if (candidate.isDirectory()) {
                return candidate;
            }
        }
        candidate = new File(System.getProperty("user.home"), "Library/Application Support/minecraft");
        if (candidate.isDirectory()) {
            return candidate;
        }
        candidate = new File(System.getProperty("user.home"), ".minecraft");
        if (candidate.isDirectory()) {
            return candidate;
        }
        return null;
    }
    
    public static File findMinecraftJar(MinecraftJarProvider minecraftJarProvider) {
        for (int i = 10; i >= 1; i--) {
            File candidate = minecraftJarProvider.getMinecraftJar(i);
            if ((candidate != null) && candidate.isFile() && candidate.canRead()) {
                return candidate;
            }
        }
        File minecraftDir = findMinecraftDir();
        if (minecraftDir != null) {
            File candidate = new File(minecraftDir, "bin/minecraft.jar");
            if (candidate.isFile() && candidate.canRead()) {
                return candidate;
            }
        }
        return null;
    }
}