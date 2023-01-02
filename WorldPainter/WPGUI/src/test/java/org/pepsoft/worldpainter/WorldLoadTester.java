package org.pepsoft.worldpainter;

import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

public class WorldLoadTester {
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
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(WorldLoadTester.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (trustedCert != null) {
            PluginManager.loadPlugins(new File(Configuration.getConfigDir(), "plugins"), trustedCert.getPublicKey(), DESCRIPTOR_PATH, Version.VERSION_OBJ, false);
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());

        new WorldLoadTester().run(args);
    }

    private void run(String[] args) throws IOException, UnloadableWorldException {
        final App app = App.getInstance();
        final File worldsDir = new File(args[0]);
        for (File file: worldsDir.listFiles()) {
            if (file.isFile() && WORLD_PATTERN.matcher(file.getName()).matches() && (! WORLD_BACKUP_PATTERN.matcher(file.getName()).matches())) {
                final World2 world;
                try {
                    world = loadWorld(file);
                } catch (RuntimeException e) {
                    logger.error("Error loading file {}", file.getName(), e);
                    continue;
                }
                logger.info("World {} loaded into memory successfully", world.getName());
                app.clearWorld();
                try {
                    app.setWorld(world, true);
                } catch (RuntimeException e) {
                    logger.error("Error setting world {} on App", world.getName(), e);
                    continue;
                }
                logger.info("World {} loaded into App successfully", world.getName());
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

    private static final Pattern WORLD_PATTERN = Pattern.compile(".*\\.world");
    private static final Pattern WORLD_BACKUP_PATTERN = Pattern.compile(".*\\.\\d\\.world");
    private static final Logger logger = LoggerFactory.getLogger(WorldLoadTester.class);
}