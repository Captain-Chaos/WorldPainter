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

/**
 *
 * @author pepijn
 */
public final class Minecraft1_8LargeBiomeScheme extends Minecraft1_8JarBiomeScheme {
    public Minecraft1_8LargeBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES, "1.8.1 Large Biomes");
        try {
            Field field = worldGeneratorClass.getField("d");
            largeBiomesGenerator = field.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Not a valid 1.8.1 or 1.8 minecraft.jar", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Not a valid 1.8.1 or 1.8 minecraft.jar", e);
        }
    }
    
    @Override
    public final void setSeed(long seed) {
        if ((seed != this.seed) || (landscape == null)) {
            try {
                landscape = ((Object[]) getLandscapesMethod.invoke(null, seed, largeBiomesGenerator, null))[1];
                this.seed = seed;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while trying to set the seed", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while trying to set the seed", e);
            }
        }
    }
    
    private static Object largeBiomesGenerator;
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<Checksum, String[]>();

//                                                                                                                                                                                                                                                                   Landscape class
//                                                                                                                                                                                                                                                                          Buffer manager class
//                                                                                                                                                                                                                                                                                 World generator class
    static { //                                                                                                                                                                                                                                                                           Initialiser class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -122, (byte) 99, (byte) -95, (byte) 12, (byte) -20, (byte) -63, (byte) 14, (byte) -86, (byte) 104, (byte) 58, (byte) -110, (byte) 126, (byte) -11, (byte) 55, (byte) 24, (byte) 82}), new String[] {"bpa", "boy", "are", "od"}); // 1.8
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 92, (byte) -102, (byte) -81, (byte) 49, (byte) 25, (byte) -97, (byte) 118, (byte) 62, (byte) -7, (byte) 8, (byte) 92, (byte) -55, (byte) -74, (byte) -112, (byte) 43, (byte) 29}),    new String[] {"boy", "bow", "arb", "oe"}); // 1.8.1
    }
}