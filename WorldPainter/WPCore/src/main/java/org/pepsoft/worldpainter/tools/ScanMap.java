package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;

public class ScanMap {
    public static void main(String[] args) throws IOException {
        final File levelDatFile = new File(args[0], "level.dat");
        final Level level = Level.load(levelDatFile);
        final int version = level.getVersion(), maxHeight = level.getMaxHeight(), dataVersion = level.getDataVersion();
        final File regionDir = new File(levelDatFile.getParentFile(), "region");
        final Pattern regionFilePattern = (version == SUPPORTED_VERSION_1)
                ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
                : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        final File[] regionFiles = regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
        for (File file: regionFiles) {
            try (RegionFile regionFile = new RegionFile(file, true)) {
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        if (regionFile.containsChunk(x, z)) {
                            final Tag tag;
                            final InputStream chunkData = regionFile.getChunkDataInputStream(x, z);
                            try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                tag = in.readTag();
                            } catch (RuntimeException e) {
                                System.err.printf("%s while reading tag for chunk %d,%d%n", e.getClass().getSimpleName(), x, z);
                                e.printStackTrace();
                                continue;
                            }
                            try {
                                final Chunk chunk = (version == SUPPORTED_VERSION_1)
                                    ? new ChunkImpl((CompoundTag) tag, maxHeight)
                                    : ((dataVersion >= 1477)
                                        ? new ChunkImpl3((CompoundTag) tag, maxHeight)
                                        : new ChunkImpl2((CompoundTag) tag, maxHeight));
                            } catch (RuntimeException e) {
                                System.err.printf("%s while parsing tag for chunk %d,%d%n", e.getClass().getSimpleName(), x, z);
                                e.printStackTrace();
                                System.err.println(tag.toString());
                            }
                        }
                    }
                }
            }
        }
    }
}