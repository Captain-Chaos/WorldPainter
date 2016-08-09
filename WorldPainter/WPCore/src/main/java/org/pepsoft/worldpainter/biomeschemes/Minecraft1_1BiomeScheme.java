/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.pepsoft.util.Checksum;
import org.pepsoft.worldpainter.BiomeScheme;

/**
 * A {@link BiomeScheme} which makes use of Minecraft 1.1 jar files to provide
 * biomes.
 *
 * @author pepijn
 */
public final class Minecraft1_1BiomeScheme extends MinecraftJarBiomeScheme {
    public Minecraft1_1BiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, md5Sum, HASHES_TO_CLASSNAMES, "1.1");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();
    
    static {
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -23, (byte) 35, (byte) 2, (byte) -46, (byte) -84, (byte) -37, (byte) -89, (byte) -55, (byte) 126, (byte) 13, (byte) -115, (byte) -15, (byte) -31, (byte) 13, (byte) 32, (byte) 6}), new String[] {"vc", "ab"});
    }
}