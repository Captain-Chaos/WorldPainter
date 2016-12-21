package org.pepsoft.worldpainter.exporting;

import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.Platform;
import org.pepsoft.minecraft.mcpe.MCPEChunk;
import org.pepsoft.minecraft.mcpe.MCPEKey;
import org.pepsoft.minecraft.mcpe.MCPELevel;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Pepijn on 11-12-2016.
 */
public class MCPEWorldExporter extends AbstractWorldExporter {
    public MCPEWorldExporter(World2 world) {
        super(world);
    }

    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Backup existing level
        File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
        logger.info("Exporting world " + world.getName() + " to map at " + worldDir);
        if (worldDir.isDirectory()) {
            logger.info("Directory already exists; backing up to " + backupDir);
            if (! worldDir.renameTo(backupDir)) {
                throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
            }
        }

        // Export dimensions
        Dimension dim0 = world.getDimension(0);
        MCPELevel level = new MCPELevel();
        if ((dim0.getMinecraftSeed() < Integer.MIN_VALUE) || (dim0.getMinecraftSeed() > Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("Minecraft seed " + dim0.getMinecraftSeed() + " out of 32-bit unsigned range");
        }
        level.setRandomSeed((int) dim0.getMinecraftSeed());
        level.setLevelName(name);
        Point spawnPoint = world.getSpawnPoint();
        level.setSpawnX(spawnPoint.x);
        level.setSpawnY(Math.max(dim0.getIntHeightAt(spawnPoint), dim0.getWaterLevelAt(spawnPoint)));
        level.setSpawnZ(spawnPoint.y);
        if (Platform.MCPE.getGameTypes().contains(world.getGameType())) {
            level.setGameType(world.getGameType().ordinal());
        } else {
            throw new IllegalArgumentException("Game type " + world.getGameType() + " not supported for MCPE");
        }
        switch (world.getGenerator()) {
            case DEFAULT:
                level.setGenerator(1);
                break;
            case FLAT:
                level.setGenerator(2); // TODO ?
                break;
            default:
                throw new IllegalArgumentException("Generator " + world.getGenerator() + " not supported for MCPE");
        }
        // Save the level.dat file. This will also create a session.lock file, hopefully kicking out any Minecraft
        // instances which may have the map open:
        level.save(worldDir);

        // Write the level name
        try (PrintWriter out = new PrintWriter(new File(worldDir, "levelname.txt"), "UTF-8")) {
            out.write(name);
        }

        exportDimension(dim0, worldDir);

        return Collections.emptyMap();
    }

    private void exportDimension(Dimension dimension, File worldDir) throws IOException {
        File dbDir = new File(worldDir, "db");
        Options options = new Options();
        options.createIfMissing(true);
        options.compressionType(CompressionType.ZLIB);
        ChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, Collections.emptyMap(), Platform.MCPE, dimension.getMaxHeight());
        try (DB db = Iq80DBFactory.factory.open(dbDir, options)) {
            for (Tile tile: dimension.getTiles()) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        int chunkX = (tile.getX() << 3) + x, chunkZ = (tile.getY() << 3) + y;
                        MCPEChunk chunk = (MCPEChunk) chunkFactory.createChunk(chunkX, chunkZ).chunk;
                        MCPEKey key = new MCPEKey(chunkX, chunkZ, MCPEKey.TYPE_TERRAIN);
                        db.put(key.toBytes(), chunk.toBytes());
                    }
                }
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MCPEWorldExporter.class);
}