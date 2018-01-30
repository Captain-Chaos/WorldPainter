package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.util.Checksum;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link MCInterface} which makes use of Minecraft 1.10 jar files to provide
 * biomes according to the Default world type.
 *
 * Created by Pepijn on 26-6-2016.
 */
public final class Minecraft1_12BiomeScheme extends Minecraft1_12JarBiomeScheme implements MCInterface {
    public Minecraft1_12BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
    }

    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

    static {
//                                                                                                                                                                                                                                                                    Landscape            Initialiser         NBT tag           NBT list tag
//                                                                                                                                                                                                                                                                           Buffer manager      Block data          NBT compound tag  Game profile serializer
//                                                                                                                                                                                                                                                                                  World generator     Block              NBT list tag      Generator settings
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) -116, (byte) 4, (byte) 67, (byte) -122, (byte) -117, (byte) -98, (byte) 70, (byte) -57, (byte) 125, (byte) 57, (byte) -37, (byte) 97, (byte) -57, (byte) 85, (byte) 103, (byte) -99}), new String[] {"bdq", "bdo", "amz", "ni", "awt", "aow", "gn", "fy", "ge", "gm", "gj", "ayx"}); // 1.12.2
    }
}