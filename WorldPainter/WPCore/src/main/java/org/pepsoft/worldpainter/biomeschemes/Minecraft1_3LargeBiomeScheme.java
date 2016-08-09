/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.pepsoft.util.Checksum;
import org.pepsoft.worldpainter.BiomeScheme;

/**
 * A {@link BiomeScheme} which makes use of Minecraft 1.3 through 1.5 jar files
 * to provide biomes according to the Large Biomes world type.
 *
 * @author pepijn
 */
public final class Minecraft1_3LargeBiomeScheme extends Minecraft1_2JarBiomeScheme {
    public Minecraft1_3LargeBiomeScheme(File minecraftJar, Checksum md5Sum) {
        super(minecraftJar, null, md5Sum, HASHES_TO_CLASSNAMES, "1.5.2 (or 1.3.1 - 1.5.1) Large Biomes");
        try {
            Field field = worldGeneratorClass.getField("d");
            largeBiomesGenerator = field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Not a valid 1.5.2 (or 1.3.1 - 1.5.1) minecraft.jar", e);
        }
    }
    
    @Override
    public final void setSeed(long seed) {
        if ((seed != this.seed) || (landscape == null)) {
            try {
                landscape = ((Object[]) getLandscapesMethod.invoke(null, seed, largeBiomesGenerator))[1];
                this.seed = seed;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while trying to set the seed", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while trying to set the seed", e);
            }
        }
    }
    
    private static Object largeBiomesGenerator;
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

//                                                                                                                                                                                                                                                                       Landscape class
//                                                                                                                                                                                                                                                                              Buffer manager class
    static { //                                                                                                                                                                                                                                                                        World generator class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 38, (byte) 108, (byte) -53, (byte) -55, (byte) 121, (byte) -118, (byte) -3, (byte) 46, (byte) -83, (byte) -13, (byte) -42, (byte) -64, (byte) 27, (byte) 76, (byte) 86, (byte) 42}),      new String[] {"adb", "acz", "uz"});
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -106, (byte) -106, (byte) -103, (byte) -15, (byte) 62, (byte) 91, (byte) -66, (byte) 127, (byte) 18, (byte) -28, (byte) 10, (byte) -60, (byte) -13, (byte) 43, (byte) 125, (byte) -102}), new String[] {"adc", "ada", "va"}); // 1.3.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 119, (byte) 17, (byte) 117, (byte) -64, (byte) 23, (byte) 120, (byte) -22, (byte) 103, (byte) 57, (byte) 91, (byte) -58, (byte) -111, (byte) -102, (byte) 90, (byte) -99, (byte) -59}),   new String[] {"afu", "afs", "xp"}); // 1.4.2
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -114, (byte) -128, (byte) -5, (byte) 1, (byte) -77, (byte) 33, (byte) -58, (byte) -77, (byte) -57, (byte) -17, (byte) -54, (byte) 57, (byte) 122, (byte) 62, (byte) -22, (byte) 53}),     new String[] {"agw", "agu", "yn"}); // 1.4.7
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 92, (byte) 18, (byte) 25, (byte) -40, (byte) 105, (byte) -72, (byte) 125, (byte) 35, (byte) 61, (byte) -29, (byte) 3, (byte) 54, (byte) -120, (byte) -20, (byte) 117, (byte) 103}),       new String[] {"ait", "air", "aal"}); // 1.5.1
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 104, (byte) -105, (byte) -61, (byte) 40, (byte) 127, (byte) -71, (byte) 113, (byte) -55, (byte) -13, (byte) 98, (byte) -21, (byte) 58, (byte) -78, (byte) 15, (byte) 93, (byte) -35}),    new String[] {"ait", "air", "aal"}); // 1.5.2
    }
}