package org.pepsoft.worldpainter.tools;

import org.json.simple.JSONValue;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;

/**
 * A tool for obtaining a mapping of MC 1.12.2 block IDs and data values to
 * MC 1.13 block names and properties by creating a map containing all the
 * blocks, loading it in MC 1.13 and letting it do the conversion, and then
 * scanning the converted map.
 */
public class BlockNameHarvester {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Load the default platform descriptors so that they don't get blocked
        // by older versions of them which might be contained in the
        // configuration. Do this by loading and initialising (but not
        // instantiating) the DefaultPlugin class
        try {
            Class.forName("org.pepsoft.worldpainter.DefaultPlugin");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Load or initialise configuration
        Configuration config = Configuration.load(); // This will migrate the configuration directory if necessary
        if (config == null) {
            System.out.println("Creating new configuration");
            config = new Configuration();
        }
        Configuration.setInstance(config);
        System.out.println("Installation ID: " + config.getUuid());

        // Initialise the default plugins
        WPPluginManager.initialise(config.getUuid());

        if (args[0].equals("--create")) {
            File savesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
            File worldDir = new File(savesDir, "BlockNames");
            if (worldDir.isDirectory()) {
                FileUtils.deleteDir(worldDir);
            }
            Level level = new Level(DEFAULT_MAX_HEIGHT_ANVIL, JAVA_ANVIL);
            level.setSeed(0L);
            level.setName("BlockNames");
            level.setGameType(GAME_TYPE_CREATIVE);
            level.setHardcore(false);
            level.setDifficulty(DIFFICULTY_PEACEFUL);
            level.setAllowCommands(true);
            level.setMapFeatures(false);
            level.setGenerator(Generator.FLAT);
            level.setSpawnX(0);
            level.setSpawnY(5);
            level.setSpawnZ(0);
            level.save(worldDir);
            File regionDir = new File(worldDir, "region");
            regionDir.mkdirs();
            JavaMinecraftWorld world = new JavaMinecraftWorld(worldDir, DIM_NORMAL, DEFAULT_MAX_HEIGHT_ANVIL, JAVA_ANVIL, false, 256);
            try {
                for (int x = -32; x < 32; x++) {
                    for (int z = -32; z < 32; z++) {
                        if (((x % 16) == 0) && ((z % 16) == 0)) {
                            // Chunk corner; add a chunk
                            world.addChunk(new MC12AnvilChunk(x >> 4, z >> 4, DEFAULT_MAX_HEIGHT_ANVIL));
                        }
                        world.setMaterialAt(x, z, 0, BEDROCK);
                        world.setMaterialAt(x, z, 1, DIRT);
                        world.setMaterialAt(x, z, 2, DIRT);
                        world.setMaterialAt(x, z, 3, GRASS_BLOCK);
                        int index = z + 32 + 64 * (x + 32);
                        int blockId = index >> 4;
                        if ((blockId != BLK_PISTON_EXTENSION) && (blockId != 253) && (blockId != 254)
                                && (blockId != BLK_WATER) && (blockId != BLK_STATIONARY_WATER)
                                && (blockId != BLK_LAVA) && (blockId != BLK_STATIONARY_LAVA)
                                && (blockId != BLK_FIRE)) {
                            world.setMaterialAt(x, z, 4, Material.getByCombinedIndex(index));
                            if (blockId == BLK_CACTUS) {
                                world.setMaterialAt(x, z, 3, SAND);
                            } else if (BLOCKS[blockId].vegetation) {
                                world.setMaterialAt(x, z, 3, FARMLAND);
                            }
                        }
                    }
                }
            } finally {
                world.flush();
            }
        } else if (args[0].equals("--scan")) {
            List<Object> blockSpecs = new ArrayList<>();
            File savesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
            File worldDir = new File(savesDir, "BlockNames");
            JavaMinecraftWorld world = new JavaMinecraftWorld(worldDir, DIM_NORMAL, DEFAULT_MAX_HEIGHT_ANVIL, JAVA_ANVIL_1_13, true, 256);
            for (int x = -32; x < 32; x++) {
                for (int z = -32; z < 32; z++) {
                    int index = z + 32 + 64 * (x + 32);
                    int blockId = index >> 4;
                    int dataValue = index & 0xf;
                    Material material = world.getMaterialAt(x, z, 4);
                    System.out.printf("%s:%d -> %s%n", BLOCKS[blockId].name, dataValue, material.toString());
                    if ((material == AIR) && ((blockId != 0) || (dataValue != 0))) {
                        // For anything other than 0:0 this means the
                        // blockId:dataValue combo didn't correspond to a valid
                        // block, OR that the block has popped off.
                        // TODO: handle the latter case
                        continue;
                    }
                    Map<String, Object> blockSpec = new HashMap<>();
                    blockSpec.put("blockId", blockId);
                    blockSpec.put("dataValue", dataValue);
                    blockSpec.put("name", material.name);
                    if (material.getProperties() != null) {
                        blockSpec.put("properties", material.getProperties());
                    }
                    blockSpecs.add(blockSpec);
                }
            }
            try (Writer out = new OutputStreamWriter(new FileOutputStream("legacy-mc-blocks.json"), "UTF-8")) {
                out.write(JSONValue.toJSONString(blockSpecs));
            }
        }
    }
}