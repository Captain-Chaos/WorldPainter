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
 * A {@link BiomeScheme} which makes use of Minecraft 1.7 jar files to provide
 * biomes according to the Default world type.
 *
 * @author pepijn
 */
public final class Minecraft1_7BiomeScheme extends Minecraft1_7JarBiomeScheme {
    public Minecraft1_7BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES, "1.7.9 or 1.7.2 Default");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

//                                                                                                                                                                                                                                                                       Landscape class
//                                                                                                                                                                                                                                                                              Buffer manager class
    static { //                                                                                                                                                                                                                                                                        World generator class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 122, (byte) 48, (byte) 69, (byte) 84, (byte) -3, (byte) -22, (byte) -121, (byte) -102, (byte) 121, (byte) -98, (byte) -2, (byte) 110, (byte) -82, (byte) -35, (byte) -116, (byte) -107}), new String[] {"avz", "avx", "afy"}); // 1.7.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 95, (byte) 124, (byte) -57, (byte) -21, (byte) 1, (byte) -53, (byte) -39, (byte) 53, (byte) -87, (byte) -105, (byte) 60, (byte) -74, (byte) -23, (byte) -60, (byte) -63, (byte) 14}),     new String[] {"axm", "axk", "ahl"}); // 1.7.9
    }
}