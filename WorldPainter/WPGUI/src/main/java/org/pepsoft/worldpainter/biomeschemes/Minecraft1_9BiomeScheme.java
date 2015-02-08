/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.pepsoft.util.Checksum;

/**
 *
 * @author pepijn
 */
public final class Minecraft1_9BiomeScheme extends MinecraftJarBiomeScheme {
    public Minecraft1_9BiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, md5Sum, HASHES_TO_CLASSNAMES, "1.9 prerelease 3 to 6 or RC2");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<Checksum, String[]>();
    
    static {
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 51, (byte) 72, (byte) 39, (byte) -37, (byte) -23, (byte) 24, (byte) 58, (byte) -10, (byte) -42, (byte) 80, (byte) -77, (byte) -109, (byte) 33, (byte) -87, (byte) -98, (byte) 33}),    new String[] {"to", "x"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -54, (byte) -28, (byte) 31, (byte) 55, (byte) 70, (byte) -45, (byte) -60, (byte) -60, (byte) 64, (byte) -78, (byte) -42, (byte) 58, (byte) 64, (byte) 55, (byte) 112, (byte) -25}),    new String[] {"uh", "y"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 98, (byte) 88, (byte) -60, (byte) -14, (byte) -109, (byte) -71, (byte) 57, (byte) 17, (byte) 126, (byte) -2, (byte) 100, (byte) 14, (byte) -38, (byte) 118, (byte) -36, (byte) -92}),  new String[] {"ug", "y"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 36, (byte) 104, (byte) 32, (byte) 81, (byte) 84, (byte) 55, (byte) 74, (byte) -2, (byte) 95, (byte) -100, (byte) -86, (byte) -70, (byte) 47, (byte) -5, (byte) -11, (byte) -8}),       new String[] {"uk", "z"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -67, (byte) 86, (byte) -99, (byte) 32, (byte) -35, (byte) 61, (byte) -40, (byte) -104, (byte) -1, (byte) 67, (byte) 113, (byte) -81, (byte) -101, (byte) -66, (byte) 20, (byte) -31}), new String[] {"uk", "z"});
    }
}