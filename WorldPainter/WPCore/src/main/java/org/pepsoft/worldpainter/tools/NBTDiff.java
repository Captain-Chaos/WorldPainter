package org.pepsoft.worldpainter.tools;

import org.jnbt.*;
import org.pepsoft.minecraft.JavaLevel;
import org.pepsoft.minecraft.RegionFileCache;
import org.pepsoft.worldpainter.AbstractTool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NBTDiff extends AbstractTool {
    public static void main(String[] args) throws IOException {
        initialisePlatform();

        final File levelDatFile1 = new File(args[0], "level.dat");
        final File levelDatFile2 = new File(args[1], "level.dat");
        final int chunkX = Integer.parseInt(args[2]);
        final int chunkZ = Integer.parseInt(args[3]);
        final Tag chunk1 = loadChunk(levelDatFile1, chunkX, chunkZ);
        final Tag chunk2 = loadChunk(levelDatFile2, chunkX, chunkZ);

        diff("", chunk1, chunk2);
    }

    private static Tag loadChunk(File levelDatFile, int chunkX, int chunkZ) throws IOException {
        final JavaLevel level = JavaLevel.load(levelDatFile);
        try (InputStream chunkIn = RegionFileCache.getChunkDataInputStream(levelDatFile.getParentFile(), chunkX, chunkZ, level.getVersion())) {
            if (chunkIn != null) {
                try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                    return in.readTag();
                }
            } else {
                throw new IllegalArgumentException(String.format("Chunk %d,%d not present!", chunkX, chunkZ));
            }
        }
    }

    private static void diff(String prefix, Tag tag1, Tag tag2) {
        if (tag1.getClass() != tag2.getClass()) {
            throw new IllegalArgumentException("Tags not of the same type");
        }
        final String name = tag1.getName();
        if (! Objects.equals(name, tag2.getName())) {
            System.out.printf("%s.%s.name \"%s\" != \"%s\"%n", prefix, name, name, tag2.getName());
        }
        switch (tag2.getClass().getSimpleName()) {
            case "ByteArrayTag":
                final byte[] byteArray1 = ((ByteArrayTag) tag1).getValue();
                final byte[] byteArray2 = ((ByteArrayTag) tag2).getValue();
                if (byteArray1.length != byteArray2.length) {
                    System.out.printf("%s.%s.length %d != %d%n", prefix, name, byteArray1.length, byteArray2.length);
                } else {
                    for (int i = 0; i < byteArray1.length; i++) {
                        if (byteArray1[i] != byteArray2[i]) {
                            System.out.printf("%s.%s[%d] %d != %d%n", prefix, name, i, byteArray1[i], byteArray2[i]);
                        }
                    }
                }
                break;
            case "CompoundTag":
                final Map<String, Tag> mapValue1 = ((CompoundTag) tag1).getValue();
                final Map<String, Tag> mapValue2 = ((CompoundTag) tag2).getValue();
                if (mapValue1.size() != mapValue2.size()) {
                    System.out.printf("%s.%s.size %d != %d%n", prefix, name, mapValue1.size(), mapValue2.size());
                }
                for (Map.Entry<String, Tag> entry: mapValue1.entrySet()) {
                    final String key = entry.getKey();
                    if (mapValue2.containsKey(key)) {
                        diff(String.format("%s.%s[%s]", prefix, name, key), entry.getValue(), mapValue2.get(key));
                    } else {
                        System.out.printf("%s.%s[%s] missing from tag 2%n", prefix, name, key);
                    }
                }
                for (String key: mapValue2.keySet()) {
                    if (! mapValue1.containsKey(key)) {
                        System.out.printf("%s.%s[%s] missing from tag 1%n", prefix, name, key);
                    }
                }
                break;
            case "DoubleTag":
                final double doubleValue1 = ((DoubleTag) tag1).getValue();
                final double doubleValue2 = ((DoubleTag) tag2).getValue();
                if (doubleValue1 != doubleValue2) {
                    System.out.printf("%s.%s %f != %f%n", prefix, name, doubleValue1, doubleValue2);
                }
                break;
            case "FloatTag":
                final float floatValue1 = ((FloatTag) tag1).getValue();
                final float floatValue2 = ((FloatTag) tag2).getValue();
                if (floatValue1 != floatValue2) {
                    System.out.printf("%s.%s %f != %f%n", prefix, name, floatValue1, floatValue2);
                }
                break;
            case "IntArrayTag":
                final int[] intArray1 = ((IntArrayTag) tag1).getValue();
                final int[] intArray2 = ((IntArrayTag) tag2).getValue();
                if (intArray1.length != intArray2.length) {
                    System.out.printf("%s.%s.length %d != %d%n", prefix, name, intArray1.length, intArray2.length);
                } else {
                    for (int i = 0; i < intArray1.length; i++) {
                        if (intArray1[i] != intArray2[i]) {
                            System.out.printf("%s.%s[%d] %d != %d%n", prefix, name, i, intArray1[i], intArray2[i]);
                        }
                    }
                }
                break;
            case "ListTag":
                if (((ListTag<?>) tag1).getType() != ((ListTag<?>) tag2).getType()) {
                    System.out.printf("%s.%s.type %s != %s%n", prefix, name, ((ListTag<?>) tag1).getType().getSimpleName(), ((ListTag<?>) tag2).getType().getSimpleName());
                } else {
                    final List<? extends Tag> list1 = ((ListTag<?>) tag1).getValue();
                    final List<? extends Tag> list2 = ((ListTag<?>) tag2).getValue();
                    if (list1.size() != list2.size()) {
                        System.out.printf("%s.%s.size %d != %d%n", prefix, name, list1.size(), list2.size());
                    } else {
                        for (int i = 0; i < list1.size(); i++) {
                            diff(String.format("%s.%s[%d]", prefix, name, i), list1.get(i), list2.get(i));
                        }
                    }
                }
                break;
            case "LongArrayTag":
                final long[] longArray1 = ((LongArrayTag) tag1).getValue();
                final long[] longArray2 = ((LongArrayTag) tag2).getValue();
                if (longArray1.length != longArray2.length) {
                    System.out.printf("%s.%s.length %d != %d%n", prefix, name, longArray1.length, longArray2.length);
                } else {
                    for (int i = 0; i < longArray1.length; i++) {
                        if (longArray1[i] != longArray2[i]) {
                            System.out.printf("%s.%s[%d] %d != %d%n", prefix, name, i, longArray1[i], longArray2[i]);
                        }
                    }
                }
                break;
            case "ByteTag":
            case "IntTag":
            case "LongTag":
            case "ShortTag":
                final long numValue1 = ((NumberTag) tag1).longValue();
                final long numValue2 = ((NumberTag) tag2).longValue();
                if (numValue1 != numValue2) {
                    System.out.printf("%s.%s %d != %d%n", prefix, name, numValue1, numValue2);
                }
                break;
            case "StringTag":
                final String stringValue1 = ((StringTag) tag1).getValue();
                final String stringValue2 = ((StringTag) tag2).getValue();
                if (! stringValue1.equals(stringValue2)) {
                    System.out.printf("%s.%s \"%s\" != \"%s\"%n", prefix, name, stringValue1, stringValue2);
                }
                break;
            default:
                throw new IllegalArgumentException("Tags of type " + tag1.getClass().getSimpleName() + " not supported");
        }
    }
}