package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;

public class BlockNameHarvester {
    public static void main(String[] args) throws IOException, ClassNotFoundException, CertificateException {
        // Load or initialise configuration
        Configuration config = Configuration.load(); // This will migrate the configuration directory if necessary
        if (config == null) {
            System.out.println("Creating new configuration");
            config = new Configuration();
        }
        Configuration.setInstance(config);
        System.out.println("Installation ID: " + config.getUuid());

        // Load trusted WorldPainter root certificate
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate trustedCert = (X509Certificate) certificateFactory.generateCertificate(ClassLoader.getSystemResourceAsStream("wproot.pem"));

        // Load the plugins
        File pluginsDir = new File(Configuration.getConfigDir(), "plugins");
        if (pluginsDir.isDirectory()) {
            PluginManager.loadPlugins(pluginsDir, trustedCert.getPublicKey());
        }
        WPPluginManager.initialise(config.getUuid());

        if (args[0].equals("--create")) {
            File savesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
            File worldDir = new File(savesDir, "BlockNames");
            if (worldDir.isDirectory()) {
                FileUtils.deleteDir(worldDir);
            }
            Level level = new Level(DEFAULT_MAX_HEIGHT_2, JAVA_ANVIL);
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
            JavaMinecraftWorld world = new JavaMinecraftWorld(worldDir, DIM_NORMAL, DEFAULT_MAX_HEIGHT_2, JAVA_ANVIL, false, 256);
            try {
                for (int x = -32; x < 32; x++) {
                    for (int z = -32; z < 32; z++) {
                        if (((x % 16) == 0) && ((z % 16) == 0)) {
                            // Chunk corner; add a chunk
                            world.addChunk(new ChunkImpl2(x >> 4, z >> 4, DEFAULT_MAX_HEIGHT_2));
                        }
                        world.setMaterialAt(x, z, 0, BEDROCK);
                        world.setMaterialAt(x, z, 1, DIRT);
                        world.setMaterialAt(x, z, 2, DIRT);
                        world.setMaterialAt(x, z, 3, GRASS);
                        world.setMaterialAt(x, z, 4, Material.getByCombinedIndex(x + 32 + 64 * (z + 32)));
                    }
                }
            } finally {
                world.flush();
            }
        } else if (args[0].equals("--scan")) {

        }
    }
}