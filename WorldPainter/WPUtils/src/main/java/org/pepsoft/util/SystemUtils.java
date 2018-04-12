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

    public static Version getJavaVersion() {
        return JAVA_VERSION;
    }

    public static final Version JAVA_VERSION = Version.parse(System.getProperty("java.specification.version"));
    public static final Version JAVA_11 = new Version(11);
    public static final Version JAVA_10 = new Version(10);
    public static final Version JAVA_9  = new Version(9);
    public static final Version JAVA_8  = new Version(1, 8);

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