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
public final class Minecraft1_0BiomeScheme extends MinecraftJarBiomeScheme {
    public Minecraft1_0BiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, md5Sum, HASHES_TO_CLASSNAMES, "1.0.0");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<Checksum, String[]>();
    
    static {
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 56, (byte) 32, (byte) -46, (byte) 34, (byte) -71, (byte) 93, (byte) 11, (byte) -116, (byte) 82, (byte) 13, (byte) -107, (byte) -106, (byte) -89, (byte) 86, (byte) -90, (byte) -26}), new String[] {"jx", "bm"});
    }
}