package org.pepsoft.worldpainter.importing;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.mcpe.MCPEChunk;
import org.pepsoft.minecraft.mcpe.MCPEKey;
import org.pepsoft.minecraft.mcpe.MCPELevel;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Frost;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.importing.JavaMapImporter.NATURAL_BLOCKS;
import static org.pepsoft.worldpainter.importing.JavaMapImporter.SPECIAL_TERRAIN_MAPPING;
import static org.pepsoft.worldpainter.importing.JavaMapImporter.TERRAIN_MAPPING;
import static org.pepsoft.minecraft.mcpe.MCPEConstants.MAX_HEIGHT;

/**
 * Created by Pepijn on 10-12-2016.
 */
public class MCPEMapImporter {
    public MCPEMapImporter(File levelDatFile) {
        this.levelDatFile = levelDatFile;
    }

    public World2 doImport() throws IOException {
        try {
            return doImport(null);
        } catch (ProgressReceiver.OperationCancelled e) {
            throw new InternalError();
        }
    }

    public World2 doImport(ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Read the level.dat file
        MCPELevel level = MCPELevel.load(levelDatFile);

        // Create an empty world
        World2 world = new World2(MAX_HEIGHT);
        long seed = level.getRandomSeed();
        TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(seed, Terrain.GRASS, MAX_HEIGHT, 58, 62, false, true, 20, 1.0);
        Dimension dimension = new Dimension(seed, tileFactory, DIM_NORMAL, MAX_HEIGHT);
        dimension.setEventsInhibited(true);
        world.addDimension(dimension);
        final boolean importBiomes = true;

        // Process the chunks from the leveldb
        File worldDir = levelDatFile.getParentFile();
        File dbDir = new File(worldDir, "db");
        File tmpDbDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        try {
            // Copy the db directory in order not to accidentally damage or
            // corrupt it, since leveldb cannot open it in read only mode
            FileUtils.copyDir(dbDir, tmpDbDir);

            // Open the database
            Options options = new Options();
            options.createIfMissing(false);
            try (DB db = Iq80DBFactory.factory.open(tmpDbDir, options)) {

                // Iterate over all key value pairs
                DBIterator iterator = db.iterator();
                for (iterator.seekToFirst(); iterator.hasNext(); ) {
                    Map.Entry<byte[], byte[]> row = iterator.next();
                    MCPEKey key = new MCPEKey(row.getKey());
                    if (key.type == MCPEKey.TYPE_TERRAIN) {

                        // It is terrain data; import it
                        MCPEChunk chunk = new MCPEChunk(key.x, key.z, row.getValue());
                        Tile tile = dimension.getTileForEditing(key.x >> 3, key.z >> 3);
                        if (tile == null) {
                            tile = tileFactory.createTile(key.x >> 3, key.z >> 3);
                            dimension.addTile(tile);
                        }
                        boolean manMadeStructuresBelowGround = false;
                        boolean manMadeStructuresAboveGround = false;
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                float height = -1.0f;
                                int waterLevel = 0;
                                boolean floodWithLava = false, frost = false;
                                Terrain terrain = Terrain.BEDROCK;
                                for (int y = MAX_HEIGHT - 1; y >= 0; y--) {
                                    int blockType = chunk.getBlockType(x, y, z);
                                    int data = chunk.getDataValue(x, y, z);
                                    if (! NATURAL_BLOCKS.get(blockType)) {
                                        if (height == -1.0f) {
                                            manMadeStructuresAboveGround = true;
                                        } else {
                                            manMadeStructuresBelowGround = true;
                                        }
                                    }
                                    if ((blockType == BLK_SNOW) || (blockType == BLK_ICE)) {
                                        frost = true;
                                    }
                                    if (((blockType == BLK_ICE) || (blockType == BLK_FROSTED_ICE) || (((blockType == BLK_STATIONARY_WATER) || (blockType == BLK_WATER) || (blockType == BLK_STATIONARY_LAVA) || (blockType == BLK_LAVA)) && (data == 0))) && (waterLevel == 0)) {
                                        waterLevel = y;
                                        if ((blockType == BLK_LAVA) || (blockType == BLK_STATIONARY_LAVA)) {
                                            floodWithLava = true;
                                        }
                                    } else if (height == -1.0f) {
                                        final Material material = Material.get(blockType, data);
                                        if (SPECIAL_TERRAIN_MAPPING.containsKey(material)) {
                                            // Special terrain found
                                            height = y - 0.4375f; // Value that falls in the middle of the lowest one eigthth which will still round to the same integer value and will receive a one layer thick smooth snow block (principle of least surprise)
                                            terrain = SPECIAL_TERRAIN_MAPPING.get(material);
                                        } else if (TERRAIN_MAPPING.containsKey(blockType)) {
                                            // Terrain found
                                            height = y - 0.4375f; // Value that falls in the middle of the lowest one eigthth which will still round to the same integer value and will receive a one layer thick smooth snow block (principle of least surprise)
                                            terrain = TERRAIN_MAPPING.get(blockType);
                                        }
                                    }
                                }
                                // Use smooth snow, if present, to better approximate world height, so smooth snow will survive merge
                                final int intHeight = (int) (height + 0.5f);
                                if ((height != -1.0f) && (intHeight < MAX_HEIGHT) && (chunk.getBlockType(x, intHeight + 1, z) == BLK_SNOW)) {
                                    int data = chunk.getDataValue(x, intHeight + 1, z);
                                    height += data * 0.125;

                                }
                                if ((waterLevel == 0) && (height >= 61.5f)) {
                                    waterLevel = 62;
                                }

                                final int blockX = (chunk.getxPos() << 4) | x;
                                final int blockY = (chunk.getzPos() << 4) | z;
                                final Point coords = new Point(blockX, blockY);
                                dimension.setTerrainAt(coords, terrain);
                                dimension.setHeightAt(coords, Math.max(height, 0.0f));
                                dimension.setWaterLevelAt(blockX, blockY, waterLevel);
                                if (frost) {
                                    dimension.setBitLayerValueAt(Frost.INSTANCE, blockX, blockY, true);
                                }
                                if (floodWithLava) {
                                    dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, blockX, blockY, true);
                                }
                                if (height == -1.0f) {
                                    dimension.setBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, blockX, blockY, true);
                                }
                                if (importBiomes && chunk.isBiomesAvailable()) {
                                    final int biome = chunk.getBiome(x, z);
                                    // If the biome is set (around the edges of the map Minecraft sets it to
                                    // 255, presumably as a marker that it has yet to be calculated), copy
                                    // it to the dimension. However, if it matches what the automatic biome
                                    // would be, don't copy it, so that WorldPainter will automatically
                                    // adjust the biome when the user makes changes
                                    if ((biome != 255) && (biome != dimension.getAutoBiome(blockX, blockY))) {
                                        dimension.setLayerValueAt(Biome.INSTANCE, blockX, blockY, biome);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (tmpDbDir.isDirectory()) {
                FileUtils.deleteDir(tmpDbDir);
            }
        }

        dimension.setEventsInhibited(false);
        return world;
    }

    public static void main(String[] args) throws IOException {
        MCPEMapImporter importer = new MCPEMapImporter(new File(args[0]));
        System.out.println(importer.doImport());
    }

    private final File levelDatFile;
}