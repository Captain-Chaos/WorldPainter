package org.pepsoft.worldpainter;

import com.google.common.primitives.Ints;
import org.pepsoft.minecraft.Constants;
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

import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

public class ExportTester extends RegressionIT {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
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
                if ((! world.getPlatform().equals(DefaultPlugin.JAVA_ANVIL)) && (! world.getPlatform().equals(DefaultPlugin.JAVA_MCREGION))) {
                    logger.warn("Don't know how to export platform {}; skipping", world.getPlatform().displayName);
                    continue;
                }
                File mapDir = exportJavaWorld(world, baseDir);
                verifyJavaMap(world, mapDir);
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
        verifyJavaWorld(mapDir, world.getPlatform().equals(DefaultPlugin.JAVA_MCREGION)? Constants.SUPPORTED_VERSION_1 : Constants.SUPPORTED_VERSION_2);
        Collection<Dimension> dimensions;
        if (world.getDimensionsToExport() != null) {
            dimensions = world.getDimensionsToExport().stream().map(world::getDimension).collect(Collectors.toSet());
        } else {
            dimensions = Arrays.asList(world.getDimensions());
        }
        for (Dimension dimension: dimensions) {
            // Gather some blocks which really should exist in the exported map. This is a bit of a gamble though
            Set<Integer> blockTypes = new HashSet<>();
            Terrain subsurfaceTerrain = dimension.getSubsurfaceMaterial();
            Terrain surfaceTerrain = dimension.getTiles().iterator().next().getTerrain(0, 0);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        blockTypes.add(subsurfaceTerrain.getMaterial(dimension.getSeed(), x, y, z, 8).blockType);
                        blockTypes.add(surfaceTerrain.getMaterial(dimension.getSeed(), x, y, z, 8).blockType);
                    }
                }
            }
            int[] expectedBlockTypes = Ints.toArray(blockTypes);
            verifyJavaDimension(mapDir, dimension, expectedBlockTypes);
        }
    }

    private static final Pattern WORLD_PATTERN = Pattern.compile(".*\\.world");
    private static final Pattern WORLD_BACKUP_PATTERN = Pattern.compile(".*\\.\\d\\.world");
    private static final Logger logger = LoggerFactory.getLogger(ExportTester.class);
}