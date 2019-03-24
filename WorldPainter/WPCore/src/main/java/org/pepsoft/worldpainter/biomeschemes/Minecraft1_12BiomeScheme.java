package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.Checksum;
import org.pepsoft.worldpainter.BiomeScheme;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link BiomeScheme} which makes use of Minecraft 1.12 jar files to provide
 * biomes according to the Default world type.
 *
 * Created by Pepijn on 26-6-2016.
 */
public final class Minecraft1_12BiomeScheme extends Minecraft1_12JarBiomeScheme {
    public Minecraft1_12BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
    }

    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

    static {
//                                                                                                                                                                                                                                                                    Landscape            Initialiser
//                                                                                                                                                                                                                                                                           Buffer manager      Generator settings
//                                                                                                                                                                                                                                                                                  World generator
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -116, (byte) 4, (byte) 67, (byte) -122, (byte) -117, (byte) -98, (byte) 70, (byte) -57, (byte) 125, (byte) 57, (byte) -37, (byte) 97, (byte) -57, (byte) 85, (byte) 103, (byte) -99}), new String[] {"bdq", "bdo", "amz", "ni", "ayx"}); // 1.12.2
    }
}