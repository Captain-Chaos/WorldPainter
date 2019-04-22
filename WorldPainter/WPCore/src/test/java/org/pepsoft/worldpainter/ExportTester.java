package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.PluginManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

public class ExportTester extends RegressionIT {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
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
            if (! logger.isDebugEnabled()) {
                // If debug logging is on, the Configuration constructor will
                // already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(ExportTester.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (trustedCert != null) {
            PluginManager.loadPlugins(new File(Configuration.getConfigDir(), "plugins"), trustedCert.getPublicKey(), FILENAME);
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());

        new ExportTester().run(args);
    }

    private void run(String[] args) throws IOException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
        File worldDir = new File(args[0]);
        File baseDir = new File(System.getProperty("java.io.tmpdir"), "WPExportTesterMaps");
        if (baseDir.exists()) {
            FileUtils.emptyDir(baseDir);
        } else {
            baseDir.mkdirs();
        }
        for (File file: worldDir.listFiles()) {
            if (file.isFile() && WORLD_PATTERN.matcher(file.getName()).matches() && (! WORLD_BACKUP_PATTERN.matcher(file.getName()).matches())) {
                World2 world = loadWorld(file);
                if ((! (world.getPlatform() == JAVA_ANVIL))
                        && (! (world.getPlatform() == JAVA_MCREGION))
                        && (! (world.getPlatform() == JAVA_ANVIL_1_13))) {
                    logger.warn("Don't know how to export platform {}; skipping", world.getPlatform().displayName);
                    continue;
                }

                File mapDir;
                try {
                    mapDir = exportJavaWorld(world, baseDir);
                    verifyJavaMap(world, mapDir);
                } catch (Throwable t) {
                    logger.error(t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                }

                if ((world.getPlatform() != JAVA_ANVIL_1_13) && (world.getMaxHeight() == DEFAULT_MAX_HEIGHT_ANVIL)) {
                    // Also test the new Minecraft 1.13 support
                    world.setPlatform(JAVA_ANVIL_1_13);
                    try {
                        mapDir = exportJavaWorld(world, baseDir);
                        verifyJavaMap(world, mapDir);
                    } catch (Throwable t) {
                        logger.error(t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    private World2 loadWorld(File file) throws IOException, UnloadableWorldException {
        // Load the world
        logger.info("Loading world {}", file.getName());
        WorldIO worldIO = new WorldIO();
        worldIO.load(new FileInputStream(file));
        return worldIO.getWorld();
    }

    private void verifyJavaMap(World2 world, File mapDir) throws IOException {
        verifyJavaWorld(mapDir, (world.getPlatform() == JAVA_MCREGION)? Constants.VERSION_MCREGION : Constants.VERSION_ANVIL);
        Collection<Dimension> dimensions;
        if (world.getDimensionsToExport() != null) {
            dimensions = world.getDimensionsToExport().stream().map(world::getDimension).collect(Collectors.toSet());
        } else {
            dimensions = Arrays.asList(world.getDimensions());
        }
        for (Dimension dimension: dimensions) {
            // Gather some blocks which really should exist in the exported map. This is a bit of a gamble though
            Set<Material> expectedMaterials = new HashSet<>();
            Terrain subsurfaceTerrain = dimension.getSubsurfaceMaterial();
            Terrain surfaceTerrain = dimension.getTiles().iterator().next().getTerrain(0, 0);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        expectedMaterials.add(subsurfaceTerrain.getMaterial(dimension.getSeed(), x, y, z, 8));
                        expectedMaterials.add(surfaceTerrain.getMaterial(dimension.getSeed(), x, y, z, 8));
                    }
                }
            }
            verifyJavaDimension(mapDir, dimension, expectedMaterials);
        }
    }

    private static final Pattern WORLD_PATTERN = Pattern.compile(".*\\.world");
    private static final Pattern WORLD_BACKUP_PATTERN = Pattern.compile(".*\\.\\d\\.world");
    private static final Logger logger = LoggerFactory.getLogger(ExportTester.class);
}