package org.pepsoft.worldpainter.tools;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.pepsoft.minecraft.Constants.TAG_NAME;
import static org.pepsoft.minecraft.Constants.TAG_Y;

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

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
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
                diffList(prefix, name, Bytes.asList(((ByteArrayTag) tag1).getValue()), Bytes.asList(((ByteArrayTag) tag2).getValue()));
                break;
            case "CompoundTag":
                diffMap(prefix, name, ((CompoundTag) tag1).getValue(), ((CompoundTag) tag2).getValue());
                break;
            case "DoubleTag":
                diffValue(prefix, name, ((DoubleTag) tag1).getValue(), ((DoubleTag) tag2).getValue());
                break;
            case "FloatTag":
                diffValue(prefix, name, ((FloatTag) tag1).getValue(), ((FloatTag) tag2).getValue());
                break;
            case "IntArrayTag":
                diffList(prefix, name, Ints.asList(((IntArrayTag) tag1).getValue()), Ints.asList(((IntArrayTag) tag2).getValue()));
                break;
            case "ListTag":
                if (name.equals("Sections")) {
                    diffMap(prefix, name,
                            ((ListTag<CompoundTag>) tag1).getValue().stream().collect(toMap(tag -> ((ByteTag) tag.getTag(TAG_Y)).intValue(), identity())),
                            ((ListTag<CompoundTag>) tag2).getValue().stream().collect(toMap(tag -> ((ByteTag) tag.getTag(TAG_Y)).intValue(), identity())));
                } else if (name.equals("Palette")) {
                    diffMap(prefix, name,
                            ((ListTag<CompoundTag>) tag1).getValue().stream().collect(toMap(tag -> ((StringTag) tag.getTag(TAG_NAME)).getValue(), identity())),
                            ((ListTag<CompoundTag>) tag2).getValue().stream().collect(toMap(tag -> ((StringTag) tag.getTag(TAG_NAME)).getValue(), identity())));
                } else {
                    if (((ListTag<?>) tag1).getType() != ((ListTag<?>) tag2).getType()) {
                        System.out.printf("%s.%s.type %s != %s%n", prefix, name, ((ListTag<?>) tag1).getType().getSimpleName(), ((ListTag<?>) tag2).getType().getSimpleName());
                    } else {
                        diffList(prefix, name, ((ListTag<?>) tag1).getValue(), ((ListTag<?>) tag2).getValue());
                    }
                }
                break;
            case "LongArrayTag":
                diffList(prefix, name, Longs.asList(((LongArrayTag) tag1).getValue()), Longs.asList(((LongArrayTag) tag2).getValue()));
                break;
            case "ByteTag":
            case "IntTag":
            case "LongTag":
            case "ShortTag":
                diffValue(prefix, name, ((NumberTag) tag1).longValue(), ((NumberTag) tag2).longValue());
                break;
            case "StringTag":
                diffValue(prefix, name, ((StringTag) tag1).getValue(), ((StringTag) tag2).getValue());
                break;
            default:
                throw new IllegalArgumentException("Tags of type " + tag1.getClass().getSimpleName() + " not supported");
        }
    }

    private static void diffList(String prefix, String name, List<?> list1, List<?> list2) {
        if (list1.size() != list2.size()) {
            System.out.printf("%s.%s.length %d != %d%n", prefix, name, list1.size(), list2.size());
        } else {
            for (int i = 0; i < list1.size(); i++) {
                final Object value1 = list1.get(i);
                if (value1 instanceof Tag) {
                    diff(String.format("%s.%s[%d]", prefix, name, i), (Tag) value1, (Tag) list2.get(i));
                } else if (! Objects.equals(value1, list2.get(i))) {
                    System.out.printf("%s.%s[%d] %s != %s%n", prefix, name, i, value1, list2.get(i));
                }
            }
        }
    }

    private static void diffValue(String prefix, String name, Object value1, Object value2) {
        if (! Objects.equals(value1, value2)) {
            System.out.printf("%s.%s %s != %s%n", prefix, name, value1, value2);
        }
    }

    private static <K, V extends Tag> void diffMap(String prefix, String name, Map<K, V> map1, Map<K, V> map2) {
        if (map1.size() != map2.size()) {
            System.out.printf("%s.%s.size %d != %d%n", prefix, name, map1.size(), map2.size());
        }
        for (Map.Entry<K, V> entry: map1.entrySet()) {
            final K key = entry.getKey();
            if (map2.containsKey(key)) {
                diff(String.format("%s.%s[%s]", prefix, name, key), entry.getValue(), map2.get(key));
            } else {
                System.out.printf("%s.%s[%s] missing from tag 2%n", prefix, name, key);
            }
        }
        for (K key: map2.keySet()) {
            if (! map1.containsKey(key)) {
                System.out.printf("%s.%s[%s] missing from tag 1%n", prefix, name, key);
            }
        }
    }
}