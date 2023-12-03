package org.pepsoft.worldpainter;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

/**
 * An abstract base class for WorldPainter command line tools.
 */
public abstract class AbstractTool {
    protected static void initialisePlatform() {
        // Force language to English for now. TODO: remove this once the first translations are implemented
        Locale.setDefault(Locale.US);

        // Use a file lock to make sure only one instance is running with autosave enabled
        File configDir = Configuration.getConfigDir();

        // Configure logging
        logger.info("Starting WorldPainter {} ({})", Version.VERSION, Version.BUILD);
        logger.info("Running on {} version {}; architecture: {}", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        logger.info("Running on {} Java version {}; maximum heap size: {} MB", System.getProperty("java.vendor"), System.getProperty("java.specification.version"), Runtime.getRuntime().maxMemory() / 1048576);

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
        Configuration config = null;
        try {
            config = Configuration.load(); // This will migrate the configuration directory if necessary
        } catch (IOException | Error | RuntimeException | ClassNotFoundException e) {
            configError(e);
        }
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

        if (config.getPreviousVersion() >= 0) {
            // Perform legacy migration actions
            if (config.getPreviousVersion() < 18) {
                // The dynmap data may have been copied from Minecraft 1.13, in
                // which case it doesn't work, so delete it if it exists
                File dynmapDir = new File(Configuration.getConfigDir(), "dynmap");
                if (dynmapDir.isDirectory()) {
                    FileUtils.deleteDir(dynmapDir);
                }
            }
        }

        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(AbstractTool.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (trustedCert != null) {
            PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey(), DESCRIPTOR_PATH, Version.VERSION_OBJ, false);
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());
        // Load all the platform descriptors to ensure that when worlds
        // containing older versions of them are loaded later they are replaced
        // with the current versions, rather than the other way around
        for (Platform platform: PlatformManager.getInstance().getAllPlatforms()) {
            logger.info("Available platform: {}", platform.displayName);
        }
        String httpAgent = "WorldPainter " + Version.VERSION + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ";";
        System.setProperty("http.agent", httpAgent);
    }

    private static void configError(Throwable e) {
        // Try to preserve the config file
        File configFile = Configuration.getConfigFile();
        if (configFile.isFile() && configFile.canRead()) {
            File backupConfigFile = new File(configFile.getParentFile(), configFile.getName() + ".old");
            try {
                FileUtils.copyFileToFile(configFile, backupConfigFile, true);
            } catch (IOException e1) {
                logger.error("I/O error while trying to preserve faulty config file", e1);
            }
        }

        // Report the error
        logger.error("Exception while initialising configuration", e);
    }

    static {
        System.setProperty("logback.configurationFile", "logback-tools.xml");
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractTool.class);
}
