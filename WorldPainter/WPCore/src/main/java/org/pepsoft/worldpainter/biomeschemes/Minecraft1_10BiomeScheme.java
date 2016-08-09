package org.pepsoft.worldpainter.biomeschemes;

import org.jnbt.CompoundTag;
import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Checksum;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link MCInterface} which makes use of Minecraft 1.10 jar files to provide
 * biomes according to the Default world type.
 *
 * Created by Pepijn on 26-6-2016.
 */
public final class Minecraft1_10BiomeScheme extends Minecraft1_8BiomeScheme implements MCInterface {
    public Minecraft1_10BiomeScheme(File minecraftJar, File libDir, Checksum md5Sum) {
        super(minecraftJar, libDir, md5Sum, HASHES_TO_CLASSNAMES);
    }

    @Override
    public Material decodeStructureMaterial(CompoundTag tag) {
        return helper.decodeStructureMaterial(tag);
    }

    @Override
    protected void init(String[] classNames, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        super.init(classNames, classLoader);

        String blockDataClassName             = classNames[ 4];
        String blockClassName                 = classNames[ 5];
        String nbtTagClassName                = classNames[ 6];
        String nbtCompoundTagClassName        = classNames[ 7];
        String nbtListTagClassName            = classNames[ 8];
        String nbtStringTagClassName          = classNames[ 9];
        String gameProfileSerializerClassName = classNames[10];

        Class<?> blockDataClass = classLoader.loadClass(blockDataClassName);
        Class<?> blockClass = classLoader.loadClass(blockClassName);
        Class<?> nbtTagClass = classLoader.loadClass(nbtTagClassName);
        Class<?> nbtCompoundTagClass = classLoader.loadClass(nbtCompoundTagClassName);
        Class<?> nbtListTagClass = classLoader.loadClass(nbtListTagClassName);
        Class<?> nbtStringTagClass = classLoader.loadClass(nbtStringTagClassName);
        Class<?> gameProfileSerializerClass = classLoader.loadClass(gameProfileSerializerClassName);

        helper = new MC10InterfaceHelper(nbtCompoundTagClass,
                nbtCompoundTagClass.getMethod("a", String.class, nbtTagClass),
                nbtListTagClass,
                nbtListTagClass.getMethod("a", nbtTagClass),
                nbtStringTagClass.getConstructor(String.class),
                gameProfileSerializerClass.getMethod("d", nbtCompoundTagClass),
                blockDataClass.getMethod("t"),
                blockClass.getMethod("a", blockClass),
                blockClass.getMethod("e", blockDataClass));
    }

    private MC10InterfaceHelper helper;

    private static final Map<Checksum, String[]> HASHES_TO_CLASSNAMES = new HashMap<>();

    static {
//                                                                                                                                                                                                                                                                    Landscape            Initialiser         NBT tag           NBT list tag
//                                                                                                                                                                                                                                                                           Buffer manager      Block data          NBT compound tag  Game profile serialzer
//                                                                                                                                                                                                                                                                                  World generator     Block              NBT list tag
        HASHES_TO_CLASSNAMES.put(new Checksum(new byte[] {(byte) 55, (byte) -8, (byte) 13, (byte) 38, (byte) 104, (byte) 114, (byte) -20, (byte) 17, (byte) 86, (byte) 10, (byte) -80, (byte) -119, (byte) 95, (byte) 124, (byte) -10, (byte) 59}),     new String[] {"ayo", "aym", "aii", "kq", "ars", "akf", "ef", "dr", "dx", "ee", "eb"}); // 1.10.2
    }
}