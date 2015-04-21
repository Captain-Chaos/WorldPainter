/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

/**
 *
 * @author pepijn
 */
public final class SystemUtils {
    private SystemUtils() {
        // Prevent instantiation
    }
    
    public static OS getOS() {
        return OPERATING_SYSTEM;
    }
    
    public static boolean isWindows() {
        return OPERATING_SYSTEM == OS.WINDOWS;
    }
    
    public static boolean isMac() {
        return OPERATING_SYSTEM == OS.MAC;
    }
    
    public static boolean isLinux() {
        return OPERATING_SYSTEM == OS.LINUX;
    }
    
    private static final OS OPERATING_SYSTEM;
    
    static {
        String os_name = System.getProperty("os.name").toLowerCase();
        if (os_name.contains("windows")) {
            OPERATING_SYSTEM = OS.WINDOWS;
        } else if (os_name.contains("os x")) {
            OPERATING_SYSTEM = OS.MAC;
        } else if (os_name.contains("linux")) {
            OPERATING_SYSTEM = OS.LINUX;
        } else {
            OPERATING_SYSTEM = OS.OTHER;
        }
    }
    
    public enum OS {WINDOWS, MAC, LINUX, OTHER}
}