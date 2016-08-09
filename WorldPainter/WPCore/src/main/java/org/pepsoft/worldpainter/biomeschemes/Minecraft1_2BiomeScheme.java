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
 * A {@link BiomeScheme} which makes use of Minecraft 1.2 through 1.5 jar files
 * to provide biomes according to the Default world type.
 *
 * @author pepijn
 */
public final class Minecraft1_2BiomeScheme extends Minecraft1_2JarBiomeScheme {
    public Minecraft1_2BiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, null, md5Sum, HASHES_TO_CLASSNAMES, "1.5.2 Default (or 1.2.3 - 1.5.1)");
    }
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

//                                                                                                                                                                                                                                                                       Landscape class
//                                                                                                                                                                                                                                                                              Buffer manager class
    static { //                                                                                                                                                                                                                                                                        World generator class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 18, (byte) -10, (byte) -60, (byte) -79, (byte) -67, (byte) -52, (byte) 99, (byte) -16, (byte) 41, (byte) -29, (byte) -64, (byte) -120, (byte) -93, (byte) 100, (byte) -72, (byte) -28}),  new String[] {"wl",  "ac",  "vt"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 37, (byte) 66, (byte) 62, (byte) -85, (byte) 109, (byte) -121, (byte) 7, (byte) -7, (byte) 108, (byte) -58, (byte) -83, (byte) -118, (byte) 33, (byte) -89, (byte) 37, (byte) 10}),       new String[] {"wp",  "ad",  "vx"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -114, (byte) -121, (byte) 120, (byte) 7, (byte) -118, (byte) 23, (byte) 90, (byte) 51, (byte) 96, (byte) 58, (byte) 88, (byte) 82, (byte) 87, (byte) -14, (byte) -123, (byte) 99}),       new String[] {"wp",  "ad",  "vx"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 38, (byte) 108, (byte) -53, (byte) -55, (byte) 121, (byte) -118, (byte) -3, (byte) 46, (byte) -83, (byte) -13, (byte) -42, (byte) -64, (byte) 27, (byte) 76, (byte) 86, (byte) 42}),      new String[] {"adb", "acz", "uz"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -106, (byte) -106, (byte) -103, (byte) -15, (byte) 62, (byte) 91, (byte) -66, (byte) 127, (byte) 18, (byte) -28, (byte) 10, (byte) -60, (byte) -13, (byte) 43, (byte) 125, (byte) -102}), new String[] {"adc", "ada", "va"}); // 1.3.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 119, (byte) 17, (byte) 117, (byte) -64, (byte) 23, (byte) 120, (byte) -22, (byte) 103, (byte) 57, (byte) 91, (byte) -58, (byte) -111, (byte) -102, (byte) 90, (byte) -99, (byte) -59}),   new String[] {"afu", "afs", "xp"}); // 1.4.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -114, (byte) -128, (byte) -5, (byte) 1, (byte) -77, (byte) 33, (byte) -58, (byte) -77, (byte) -57, (byte) -17, (byte) -54, (byte) 57, (byte) 122, (byte) 62, (byte) -22, (byte) 53}),     new String[] {"agw", "agu", "yn"}); // 1.4.7
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 92, (byte) 18, (byte) 25, (byte) -40, (byte) 105, (byte) -72, (byte) 125, (byte) 35, (byte) 61, (byte) -29, (byte) 3, (byte) 54, (byte) -120, (byte) -20, (byte) 117, (byte) 103}),       new String[] {"ait", "air", "aal"}); // 1.5.1
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 104, (byte) -105, (byte) -61, (byte) 40, (byte) 127, (byte) -71, (byte) 113, (byte) -55, (byte) -13, (byte) 98, (byte) -21, (byte) 58, (byte) -78, (byte) 15, (byte) 93, (byte) -35}),    new String[] {"ait", "air", "aal"}); // 1.5.2
    }
}