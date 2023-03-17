package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.TextUtils.getLine;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_15;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_18;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.BIOME_NAMES;

/**
 * A tool for obtaining a mapping of MC 1.17 biome IDs MC 1.18 biomes names by creating a map containing all the biomes,
 * loading it in MC 1.18 and letting it do the conversion, and then scanning the converted map.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class BiomeNameHarvester {
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
            File worldDir = new File(savesDir, "BiomeNames");
            if (worldDir.isDirectory()) {
                FileUtils.deleteDir(worldDir);
            }
            JavaLevel level = JavaLevel.create(JAVA_ANVIL_1_15, 0, DEFAULT_MAX_HEIGHT_ANVIL);
            level.setSeed(0L);
            level.setName("BiomeNames");
            level.setGameType(GAME_TYPE_CREATIVE);
            level.setHardcore(false);
            level.setDifficulty(DIFFICULTY_PEACEFUL);
            level.setAllowCommands(true);
            level.setMapFeatures(false);
            level.setGenerator(DIM_NORMAL, new SuperflatGenerator(SuperflatPreset.defaultPreset(JAVA_ANVIL_1_15)));
            level.setSpawnX(0);
            level.setSpawnY(5);
            level.setSpawnZ(0);
            level.save(worldDir);
            File regionDir = new File(worldDir, "region");
            regionDir.mkdirs();
            JavaMinecraftWorld world = new JavaMinecraftWorld(worldDir, DIM_NORMAL, 0, DEFAULT_MAX_HEIGHT_ANVIL, JAVA_ANVIL_1_15, false, 256);
            try {
                for (int x = -8; x < 8; x++) {
                    for (int z = -8; z < 8; z++) {
                        final int biomeId = (x + 8) * 16 + z + 8;
                        final MC115AnvilChunk chunk = new MC115AnvilChunk(x, z, DEFAULT_MAX_HEIGHT_ANVIL);
                        for (int xx = 0; xx < 16; xx++) {
                            for (int zz = 0; zz < 16; zz++) {
                                chunk.setMaterialAt(xx, zz, 0, BEDROCK);
                                chunk.setMaterialAt(xx, zz, 1, DIRT);
                                chunk.setMaterialAt(xx, zz, 2, DIRT);
                                chunk.setMaterialAt(xx, zz, 3, GRASS_BLOCK);
                                if ((xx == 0) || (zz == 0)) {
                                    chunk.setMaterialAt(xx, zz, 4, WOOL_MAGENTA);
                                } else if ((xx == 8) && (zz == 8)) {
                                    chunk.setMaterialAt(xx, zz, 4, OAK_SIGN);
                                    final Sign sign = new Sign();
                                    final String name = ((biomeId < BIOME_NAMES.length) && (BIOME_NAMES[biomeId] != null)) ? BIOME_NAMES[biomeId] : "";
                                    sign.setText("Biome ID: " + biomeId, getLine(name, 15, 0), getLine(name, 15, 1), getLine(name, 15, 2));
                                    sign.setX((x << 4) | xx);
                                    sign.setY(4);
                                    sign.setZ((z << 4) | zz);
                                    chunk.getTileEntities().add(sign);
                                }
                                if (((xx % 4) == 0) && ((zz % 4) == 0)) {
                                    for (int yy = 0; yy < 16; yy++) {
                                        chunk.set3DBiome(xx >> 2, yy, zz >> 2, biomeId);
                                    }
                                }
                            }
                        }
                        world.addChunk(chunk);
                    }
                }
            } finally {
                world.flush();
            }
        } else if (args[0].equals("--scan")) {
            Map<Integer, String> biomes = new HashMap<>();
            Map<String, Set<Integer>> legacyIds = new HashMap<>();
            File savesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
            File worldDir = new File(savesDir, "BiomeNames");
            JavaMinecraftWorld world = new JavaMinecraftWorld(worldDir, DIM_NORMAL, 0, DEFAULT_MAX_HEIGHT_ANVIL, JAVA_ANVIL_1_18, true, DEFAULT_MAX_HEIGHT_1_18);
            for (int x = -8; x < 8; x++) {
                for (int z = -8; z < 8; z++) {
                    final int biomeId = (x + 8) * 16 + z + 8;
                    MC118AnvilChunk chunk = (MC118AnvilChunk) world.getChunk(x, z);
                    for (int y = 0; y < 16; y++) {
                        String biome = chunk.getNamedBiome(2, y, 2);
                        if (biomes.containsKey(biomeId) && (! biomes.get(biomeId).equals(biome))) {
                            System.err.println("Biome ID " + biomeId + " maps to multiple named biomes (at least " + biomes.get(biomeId) + " and " + biome + ")");
                        } else {
                            biomes.put(biomeId, biome);
                        }
                        legacyIds.computeIfAbsent(biome, key -> new HashSet<>()).add(biomeId);
                    }
                }
            }

//            for (int i = 0; i < 256; i++) {
//                if ((i < BIOME_NAMES.length) && (BIOME_NAMES[i] != null)) {
//                    System.out.printf("public static final Biome BIOME_%s = createLegacyBiome(%d, \"minecraft:%s\", \"%s\");%n",
//                            toTechnicalName(BIOME_NAMES[i]).toUpperCase(),
//                            i,
//                            toTechnicalName(BIOME_NAMES[i]),
//                            biomes.get(i).getModernName());
//                }
//            }
//
//            System.out.println();
//            for (Biome biome: legacyIds.keySet()) {
//                System.out.printf("public static final Biome BIOME_%s = createModernBiome(\"%s\");%n",
//                        biome.getModernName().substring(biome.getModernName().indexOf(':') + 1).toUpperCase(),
//                        biome.getModernName());
//            }

            for (int i = 0; i < 256; i++) {
                if ((i < BIOME_NAMES.length) && (BIOME_NAMES[i] != null)) {
                    System.out.printf("    public static final int BIOME_%s = %d;%n", toTechnicalName(BIOME_NAMES[i]).toUpperCase(), i);
                }
                if (((i + 1) % 10) == 0) {
                    System.out.println();
                }
            }

//            for (int i = 0; i < 256; i++) {
//                if ((i < BIOME_NAMES.length) && (BIOME_NAMES[i] != null)) {
//                    System.out.printf("    \"%s\",%n", biomes.get(i).getModernName());
//                } else {
//                    System.out.println("    null,");
//                }
//                if (((i + 1) % 10) == 0) {
//                    System.out.println();
//                }
//            }
        }
    }

    private static String toTechnicalName(String displayName) {
        return displayName.toLowerCase().replace(' ', '_');
    }
}