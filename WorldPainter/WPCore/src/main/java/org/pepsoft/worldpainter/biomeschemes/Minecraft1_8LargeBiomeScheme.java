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
 * A {@link BiomeScheme} which makes use of Minecraft 1.8 and 1.9 jar files to
 * provide biomes according to the Large Biomes world type.
 *
 * @author pepijn
 */
public class Minecraft1_8LargeBiomeScheme extends Minecraft1_8JarBiomeScheme {
    public Minecraft1_8LargeBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
    }

    public Minecraft1_8LargeBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum, Map<Checksum, String[]> hashesToClassNames) {
        super(minecraftJar, libDir, md5Sum, hashesToClassNames);
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

    @Override
    protected void init(String[] classNames, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        super.init(classNames, classLoader);

        Field field = worldGeneratorClass.getField("d");
        largeBiomesGenerator = field.get(null);
    }

    private Object largeBiomesGenerator;
    
    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

//                                                                                                                                                                                                                                                                       Landscape class
//                                                                                                                                                                                                                                                                              Buffer manager class
//                                                                                                                                                                                                                                                                                     World generator class
    static { //                                                                                                                                                                                                                                                                               Initialiser class
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -122, (byte) 99, (byte) -95, (byte) 12, (byte) -20, (byte) -63, (byte) 14, (byte) -86, (byte) 104, (byte) 58, (byte) -110, (byte) 126, (byte) -11, (byte) 55, (byte) 24, (byte) 82}),     new String[] {"bpa", "boy", "are", "od"}); // 1.8
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 92, (byte) -102, (byte) -81, (byte) 49, (byte) 25, (byte) -97, (byte) 118, (byte) 62, (byte) -7, (byte) 8, (byte) 92, (byte) -55, (byte) -74, (byte) -112, (byte) 43, (byte) 29}),        new String[] {"boy", "bow", "arb", "oe"}); // 1.8.1
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -108, (byte) 55, (byte) -76, (byte) -114, (byte) 5, (byte) 27, (byte) 12, (byte) -24, (byte) -127, (byte) 124, (byte) -104, (byte) -98, (byte) 89, (byte) -25, (byte) -103, (byte) 79}),  new String[] {"asf", "asd", "ads", "kc"}); // 1.8.3
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 101, (byte) -123, (byte) -119, (byte) 23, (byte) -111, (byte) -32, (byte) -41, (byte) 93, (byte) -59, (byte) 76, (byte) -5, (byte) 84, (byte) 122, (byte) -122, (byte) 109, (byte) -80}), new String[] {"ase", "asc", "adr", "kb"}); // 1.8.8
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 57, (byte) 96, (byte) -103, (byte) -30, (byte) 62, (byte) 88, (byte) 48, (byte) -99, (byte) 33, (byte) 113, (byte) 58, (byte) -75, (byte) -41, (byte) -63, (byte) -47, (byte) -88}),      new String[] {"ase", "asc", "adr", "kb"}); // 1.8.9
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -66, (byte) 112, (byte) 66, (byte) 2, (byte) 51, (byte) 112, (byte) -101, (byte) 61, (byte) 119, (byte) -113, (byte) 118, (byte) 39, (byte) -35, (byte) 80, (byte) -34, (byte) 98}),      new String[] {"axv", "axt", "ahy", "kn"}); // 1.9
    }
}