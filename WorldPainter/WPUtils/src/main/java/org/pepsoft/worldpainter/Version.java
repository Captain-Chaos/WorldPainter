/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author pepijn
 */
public class Version {
    private Version() {
        // Prevent instantiation
    }

    public static boolean isSnapshot() {
        return VERSION.contains("SNAPSHOT");
    }
    
    public static final String VERSION;
    public static final String BUILD;
    public static final org.pepsoft.util.Version VERSION_OBJ;

    static {
        Properties versionProps = new Properties();
        try {
            versionProps.load(Version.class.getResourceAsStream("/org.pepsoft.worldpainter.properties"));
            VERSION = versionProps.getProperty("org.pepsoft.worldpainter.version");
            BUILD = versionProps.getProperty("org.pepsoft.worldpainter.build");
            VERSION_OBJ = org.pepsoft.util.Version.parse(VERSION);
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading version number from classpath", e);
        }
    }
}