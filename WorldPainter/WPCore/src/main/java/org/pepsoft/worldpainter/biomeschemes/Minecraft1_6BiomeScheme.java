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
 * A {@link BiomeScheme} which makes use of Minecraft 1.6 jar files to provide
 * biomes according to the Default world type.
 *
 * @author pepijn
 */
public final class Minecraft1_6BiomeScheme extends Minecraft1_2JarBiomeScheme {
    public Minecraft1_6BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES, "1.6.2 or 1.6.4 Default");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

//                                                                                                                                                                                                                                                                   Landscape class
//                                                                                                                                                                                                                                                                          Buffer manager class
    static { //                                                                                                                                                                                                                                                                    World generator class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 29, (byte) 67, (byte) -51, (byte) -70, (byte) -117, (byte) -105, (byte) 82, (byte) -41, (byte) -11, (byte) 87, (byte) -85, (byte) 125, (byte) 62, (byte) 54, (byte) 89, (byte) 100}), new String[] {"akn", "akl", "acf"}); // 1.6.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 46, (byte) 80, (byte) 68, (byte) -11, (byte) 53, (byte) -98, (byte) -126, (byte) 36, (byte) 85, (byte) 81, (byte) 22, (byte) 122, (byte) 35, (byte) 127, (byte) 49, (byte) 103}),     new String[] {"akq", "ako", "acg"}); // 1.6.4
    }
}