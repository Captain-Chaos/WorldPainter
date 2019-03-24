package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.Checksum;
import org.pepsoft.worldpainter.BiomeScheme;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link BiomeScheme} which makes use of Minecraft 1.12 jar files to provide
 * biomes according to the Large Biomes world type.
 *
 * <p>Created by Pepijn on 26-6-2016.
 */
public final class Minecraft1_12LargeBiomeScheme extends Minecraft1_12JarBiomeScheme {
    public Minecraft1_12LargeBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
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

    static {
//                                                                                                                                                                                                                                                                    Landscape            Initialiser
//                                                                                                                                                                                                                                                                           Buffer manager      Generator settings
//                                                                                                                                                                                                                                                                                  World generator
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -116, (byte) 4, (byte) 67, (byte) -122, (byte) -117, (byte) -98, (byte) 70, (byte) -57, (byte) 125, (byte) 57, (byte) -37, (byte) 97, (byte) -57, (byte) 85, (byte) 103, (byte) -99}), new String[] {"bdq", "bdo", "amz", "ni", "ayx"}); // 1.12.2
    }
}