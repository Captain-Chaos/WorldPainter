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
public final class Minecraft1_8_1BiomeScheme extends MinecraftJarBiomeScheme {
    public Minecraft1_8_1BiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, md5Sum, HASHES_TO_CLASSNAMES, "1.8.1");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<Checksum, String[]>();
    
    static {
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -8, (byte) -59, (byte) -94, (byte) -52, (byte) -45, (byte) -68, (byte) -103, (byte) 103, (byte) -110, (byte) -69, (byte) -28, (byte) 54, (byte) -40, (byte) -52, (byte) 8, (byte) -68}), new String[] {"rj", "w"});
    }
}