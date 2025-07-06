package org.pepsoft.worldpainter;

import org.junit.Ignore;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;
import static org.pepsoft.worldpainter.DefaultPlugin.DEFAULT_JAVA_PLATFORMS;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.EXPORT_EVERYTHING;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

@Ignore
public class ExportTester extends RegressionIT {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnloadableWorldException {
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
            PluginManager.loadPlugins(new File(Configuration.getConfigDir(), "plugins"), trustedCert.getPublicKey(), DESCRIPTOR_PATH, Version.VERSION_OBJ, false);
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid(), WPContext.INSTANCE);

        new ExportTester().run(args);
    }

    private void run(String[] args) throws IOException, UnloadableWorldException {
        final Platform latestPlatform = DEFAULT_JAVA_PLATFORMS.get(DEFAULT_JAVA_PLATFORMS.size() - 1);
        final File worldDir = new File(args[0]);
        final File baseDir = new File(System.getProperty("java.io.tmpdir"), "WPExportTesterMaps");
        if (baseDir.exists()) {
            FileUtils.emptyDir(baseDir);
        } else {
            baseDir.mkdirs();
        }
        for (File file: worldDir.listFiles()) {
            if (file.isFile() && WORLD_PATTERN.matcher(file.getName()).matches() && (! WORLD_BACKUP_PATTERN.matcher(file.getName()).matches())) {
                final World2 world = loadWorld(file);
                final Platform platform = world.getPlatform();
                if (! DEFAULT_JAVA_PLATFORMS.contains(platform)) {
                    logger.warn("Don't know how to export platform {}; skipping", platform.displayName);
                    continue;
                }

                File mapDir;
                try {
                    mapDir = exportJavaWorld(world, baseDir);
                    verifyJavaMap(world, mapDir);
                } catch (Throwable t) {
                    logger.error(t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                }

                if ((platform != latestPlatform) && (latestPlatform.isCompatible(world) == null)) {
                    // Also test the latest platform
                    world.setPlatform(latestPlatform);
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
        final Collection<Dimension> dimensions;
        final WorldExportSettings exportSettings = (world.getExportSettings() != null) ? world.getExportSettings() : EXPORT_EVERYTHING;
        if (exportSettings.getDimensionsToExport() != null) {
            dimensions = world.getDimensions().stream()
                    .filter(dimension -> exportSettings.getDimensionsToExport().contains(dimension.getAnchor().dim))
                    .collect(toSet());
        } else {
            dimensions = world.getDimensions();
        }
        for (Dimension dimension: dimensions) {
            // Gather some blocks which really should exist in the exported map. This is a bit of a gamble though
            final Set<Material> expectedMaterials = new HashSet<>();
            final Terrain subsurfaceTerrain = dimension.getSubsurfaceMaterial();
            final Terrain surfaceTerrain = dimension.getTiles().iterator().next().getTerrain(0, 0);
            final Platform platform = world.getPlatform();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        expectedMaterials.add(subsurfaceTerrain.getMaterial(platform, dimension.getSeed(), x, y, z, 8));
                        expectedMaterials.add(surfaceTerrain.getMaterial(platform, dimension.getSeed(), x, y, z, 8));
                    }
                }
            }
            verifyJavaDimension(mapDir, dimension, expectedMaterials, exportSettings);
        }
    }

    private static final Pattern WORLD_PATTERN = Pattern.compile(".*\\.world");
    private static final Pattern WORLD_BACKUP_PATTERN = Pattern.compile(".*\\.\\d\\.world");
    private static final Logger logger = LoggerFactory.getLogger(ExportTester.class);
}