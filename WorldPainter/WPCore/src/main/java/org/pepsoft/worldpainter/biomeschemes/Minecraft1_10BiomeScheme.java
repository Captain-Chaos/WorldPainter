package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.Checksum;
import org.pepsoft.worldpainter.BiomeScheme;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link BiomeScheme} which makes use of Minecraft 1.10 jar files to provide
 * biomes according to the Default world type.
 *
 * Created by Pepijn on 26-6-2016.
 */
public final class Minecraft1_10BiomeScheme extends Minecraft1_8BiomeScheme {
    public Minecraft1_10BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
    }

    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

    static {
//                                                                                                                                                                                                                                                                    Landscape            Initialiser
//                                                                                                                                                                                                                                                                           Buffer manager
//                                                                                                                                                                                                                                                                                  World generator
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 55, (byte) -8, (byte) 13, (byte) 38, (byte) 104, (byte) 114, (byte) -20, (byte) 17, (byte) 86, (byte) 10, (byte) -80, (byte) -119, (byte) 95, (byte) 124, (byte) -10, (byte) 59}),     new String[] {"ayo", "aym", "aii", "kq"}); // 1.10.2
    }
}