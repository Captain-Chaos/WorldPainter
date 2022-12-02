/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.utils.Lm;
import org.intellij.lang.annotations.Language;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.GUIUtils;
import org.pepsoft.util.SystemUtils;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.Plugin;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowWarning;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_PLUGINS;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_SAFE_MODE;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

/**
 *
 * @author pepijn
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // Force language to English for now. TODO: remove this once the first translations are implemented
        Locale.setDefault(Locale.US);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        // Set some hardcoded system properties we always want set:
        if (SystemUtils.isMac()) {
            // Use the Mac style top of screen menu bar
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        // Work around a bug in the JIDE Docking Framework which otherwise causes duplicate mouse events on focus
        // switches resulting in uncommanded edits
        System.setProperty("docking.focusWorkaround1", "true");
        // Disable Java2D's automatic UI scaling, as it does not do a good job with the editor view; we want to do it
        // ourselves
        System.setProperty("sun.java2d.uiScale.enabled", "false");

        // Use a file lock to make sure only one instance is running with autosave enabled
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
        }
        Path lockFilePath = new File(configDir, "wpsession.lock").toPath();
        try {
            Files.createFile(lockFilePath);
        } catch (FileAlreadyExistsException e) {
            // We can't yet conclude another instance is running, because it may have crashed and left the lock file
            // behind
        }
        FileChannel lockFileChannel = FileChannel.open(lockFilePath, StandardOpenOption.WRITE);
        FileLock lock = lockFileChannel.tryLock();
        boolean autosaveInhibited;
        if (lock == null) {
            lockFileChannel.close();
            autosaveInhibited = true;
        } else {
            Runtime.getRuntime().addShutdownHook(new Thread("Lock File Eraser") {
                @Override
                public void run() {
                    try {
                        lock.release();
                        lockFileChannel.close();
                        Files.delete(lockFilePath);
                    } catch (IOException e) {
                        logger.error("Could not delete lock file " + lockFilePath, e);
                    }
                }
            });
            autosaveInhibited = false;
        }

        // Configure logging
        String logLevel;
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "DEBUG";
        } else if ("extra".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "TRACE";
        } else {
            logLevel = "INFO";
        }
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(logContext);
            logContext.reset();
            System.setProperty("org.pepsoft.worldpainter.configDir", configDir.getAbsolutePath());
            System.setProperty("org.pepsoft.worldpainter.logLevel", logLevel);
            configurator.doConfigure(ClassLoader.getSystemResourceAsStream("logback-main.xml"));
        } catch (JoranException e) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(logContext);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        logger.info("Starting WorldPainter " + Version.VERSION + " (" + Version.BUILD + ")");
        logger.info("Running on {} version {}; architecture: {}", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        logger.info("Running on {} Java version {}; maximum heap size: {} MB", System.getProperty("java.vendor"), System.getProperty("java.specification.version"), Runtime.getRuntime().maxMemory() / 1000000);
        if (autosaveInhibited) {
            logger.warn("Another instance of WorldPainter is already running; disabling autosave");
        }

        // Parse the command line
        File myFile = null;
        boolean safeMode = "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.safeMode"));
        for (String arg: args) {
            if (arg.trim().equalsIgnoreCase("--safe")) {
                safeMode = true;
            } else if (new File(arg).isFile() && (myFile == null)) {
                myFile = new File(arg);
            } else {
                throw new IllegalArgumentException("Unrecognised or invalid command line option, or file does not exist: " + arg);
            }
        }
        final File file = myFile;
        if (safeMode) {
            logger.info("WorldPainter running in safe mode");
            System.setProperty("org.pepsoft.worldpainter.safeMode", "true");
        }

        // If the config file does not exist, also reset the persistent settings that are not stored in that, since the
        // user may be trying to reset the configuration
        final boolean snapshot = Version.isSnapshot();
        if (! Configuration.getConfigFile().isFile()) {
            try {
                Preferences prefs = Preferences.userNodeForPackage(Main.class);
                prefs.remove((snapshot ? "snapshot." : "") + "accelerationType");
                prefs.flush();
                prefs = Preferences.userNodeForPackage(GUIUtils.class);
                prefs.remove((snapshot ? "snapshot." : "") + "manualUIScale");
                prefs.flush();
            } catch (BackingStoreException e) {
                logger.error("Error resetting user preferences", e);
            }
        }

        // Set the acceleration mode. For some reason we don't fully understand, loading the Configuration from disk
        // initialises Java2D, so we have to do this *before* then.
        AccelerationType accelerationType;
        String accelTypeName = Preferences.userNodeForPackage(Main.class).get((snapshot ? "snapshot." : "") + "accelerationType", null);
        if (accelTypeName != null) {
            accelerationType = AccelerationType.valueOf(accelTypeName);
        } else {
            accelerationType = AccelerationType.DEFAULT;
            // TODO: Experiment with which ones work well and use them by default!
        }
        if (! safeMode) {
            switch (accelerationType) {
                case UNACCELERATED:
                    // Try to disable all accelerated pipelines we know of:
                    System.setProperty("sun.java2d.d3d", "false");
                    System.setProperty("sun.java2d.opengl", "false");
                    System.setProperty("sun.java2d.xrender", "false");
                    System.setProperty("apple.awt.graphics.UseQuartz", "false");
                    logger.info("Hardware acceleration method: unaccelerated");
                    break;
                case DIRECT3D:
                    // Direct3D should already be the default on Windows, but enable a few things which are off by
                    // default:
                    System.setProperty("sun.java2d.translaccel", "true");
                    System.setProperty("sun.java2d.ddscale", "true");
                    logger.info("Hardware acceleration method: Direct3D");
                    break;
                case OPENGL:
                    System.setProperty("sun.java2d.opengl", "True");
                    logger.info("Hardware acceleration method: OpenGL");
                    break;
                case XRENDER:
                    System.setProperty("sun.java2d.xrender", "True");
                    logger.info("Hardware acceleration method: XRender");
                    break;
                case QUARTZ:
                    System.setProperty("apple.awt.graphics.UseQuartz", "true");
                    logger.info("Hardware acceleration method: Quartz");
                    break;
                default:
                    logger.info("Hardware acceleration method: default");
                    break;
            }
        } else {
            logger.info("[SAFE MODE] Hardware acceleration method: default");
        }

        // Load the default platform descriptors so that they don't get blocked by older versions of them which might be
        // contained in the configuration. Do this by loading and initialising (but not instantiating) the DefaultPlugin
        // class
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
                // If debug logging is on, the Configuration constructor will already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        // Load the transient settings into the config object
        config.setSafeMode(safeMode);
        config.setAutosaveInhibited(autosaveInhibited);
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

        if (config.getPreviousVersion() >= 0) {
            // Perform legacy migration actions
            if (config.getPreviousVersion() < 18) {
                // The dynmap data may have been copied from Minecraft 1.13, in which case it doesn't work, so delete it
                // if it exists
                File dynmapDir = new File(Configuration.getConfigDir(), "dynmap");
                if (dynmapDir.isDirectory()) {
                    FileUtils.deleteDir(dynmapDir);
                }
            }
        }

        if (config.isAutosaveEnabled() && autosaveInhibited) {
            StartupMessages.addWarning("Another instance of WorldPainter is already running.\nAutosave will therefore be disabled in this instance of WorldPainter!");
        }

        // Store the acceleration type in the config object so the Preferences dialog can edit it
        config.setAccelerationType(accelerationType);

        // Start background scan for Minecraft jars
        BiomeSchemeManager.initialiseInBackground();
        
        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(Main.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (! safeMode) {
            if (trustedCert != null) {
                PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey(), DESCRIPTOR_PATH);
            } else {
                logger.error("Trusted root certificate not available; not loading plugins");
            }
        } else {
            logger.info("[SAFE MODE] Not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());
        // Load all the platform descriptors to ensure that when worlds containing older versions of them are loaded
        // later they are replaced with the current versions, rather than the other way around
        for (Platform platform : PlatformManager.getInstance().getAllPlatforms()) {
            logger.info("Available platform: {}", platform.displayName);
        }
        String httpAgent = "WorldPainter " + Version.VERSION + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ";";
        System.setProperty("http.agent", httpAgent);

        // Load the private context, if any, which provides services which we only want the official distribution of
        // WorldPainter to perform, such as check for updates and submit usage data
        for (PrivateContext aPrivateContextLoader: ServiceLoader.load(PrivateContext.class)) {
            if (privateContext == null) {
                privateContext = aPrivateContextLoader;
            } else {
                throw new IllegalStateException("More than one private context found on classpath");
            }
        }
        if (privateContext == null) {
            logger.debug("No private context found on classpath; update checks and usage data submission disabled");
            config.setPingAllowed(false);
        }

        // Check for updates (if update checker is available)
        if (privateContext != null) {
            privateContext.checkForUpdates();
        }

        final long start = System.currentTimeMillis();
        config.setLaunchCount(config.getLaunchCount() + 1);
        Runtime.getRuntime().addShutdownHook(new Thread("Configuration Saver") {
            @Override
            public void run() {
                try {
                    Configuration config = Configuration.getInstance();
                    MouseOrTabletOperation.flushEvents(config);
                    BetterAction.flushEvents(config);
                    EventVO sessionEvent = new EventVO("worldpainter.session").setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start)).duration(System.currentTimeMillis() - start);
                    StringBuilder sb = new StringBuilder();
                    List<Plugin> plugins = WPPluginManager.getInstance().getAllPlugins();
                    plugins.stream()
                            .filter(plugin -> ! plugin.getClass().getName().startsWith("org.pepsoft.worldpainter"))
                            .forEach(plugin -> {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append("{name=");
                        sb.append(plugin.getName().replaceAll("[ \\t\\n\\x0B\\f\\r\\.]", ""));
                        sb.append(",version=");
                        sb.append(plugin.getVersion());
                        sb.append('}');
                    });
                    if (sb.length() > 0) {
                        sessionEvent.setAttribute(ATTRIBUTE_KEY_PLUGINS, sb.toString());
                    }
                    sessionEvent.setAttribute(ATTRIBUTE_KEY_SAFE_MODE, config.isSafeMode());
                    config.logEvent(sessionEvent);
                    config.save();

                    // Store the acceleration type and manual GUI scale separately, because we need them before we can
                    // load the config:
                    Preferences prefs = Preferences.userNodeForPackage(Main.class);
                    prefs.put((snapshot ? "snapshot." : "") + "accelerationType", config.getAccelerationType().name());
                    prefs.flush();
                    prefs = Preferences.userNodeForPackage(GUIUtils.class);
                    prefs.putFloat((snapshot ? "snapshot." : "") + "manualUIScale", config.getUiScale());
                    prefs.flush();
                } catch (IOException e) {
                    logger.error("I/O error saving configuration", e);
                } catch (BackingStoreException e) {
                    logger.error("Backing store exception saving acceleration type", e);
                }
                logger.info("Shutting down WorldPainter");
            }
        });
        
        // Make the "action:" URLs used in various places work:
        URL.setURLStreamHandlerFactory(protocol -> {
            switch (protocol) {
                case "action":
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            throw new UnsupportedOperationException("Not supported");
                        }
                    };
                default:
                    return null;
            }
        });

        final World2 world;
        final File autosaveFile = new File(configDir, "autosave.world");
        if ((file == null) && (autosaveInhibited || (! config.isAutosaveEnabled()) || (! autosaveFile.isFile()))) {
            if (! safeMode) {
                world = WorldFactory.createDefaultWorld(config, new Random().nextLong());
//                world = WorldFactory.createFancyWorld(config, new Random().nextLong());
            } else {
                logger.info("[SAFE MODE] Using default configuration for default world");
                world = WorldFactory.createDefaultWorld(new Configuration(), new Random().nextLong());
            }
        } else {
            world = null;
        }

        // Install JIDE licence, if present
        InputStream in = ClassLoader.getSystemResourceAsStream("jide_licence.properties");
        if (in != null) {
            try {
                Properties jideLicenceProps = new Properties();
                jideLicenceProps.load(in);
                Lm.verifyLicense(jideLicenceProps.getProperty("companyName"), jideLicenceProps.getProperty("projectName"), jideLicenceProps.getProperty("licenceKey"));
            } finally {
                in.close();
            }
        }

        final Configuration.LookAndFeel lookAndFeel = (config.getLookAndFeel() != null) ? config.getLookAndFeel() : Configuration.LookAndFeel.SYSTEM;
        SwingUtilities.invokeLater(() -> {
            Configuration myConfig = Configuration.getInstance();
            if (myConfig.isSafeMode()) {
                GUIUtils.setUIScale(1.0f);
                logger.info("[SAFE MODE] Not installing visual theme");
            } else {
                // Install configured look and feel
                try {
                    String laf;
                    switch (lookAndFeel) {
                        case SYSTEM:
                            laf = UIManager.getSystemLookAndFeelClassName();
                            break;
                        case METAL:
                            laf = "javax.swing.plaf.metal.MetalLookAndFeel";
                            break;
                        case NIMBUS:
                            laf = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
                            break;
                        case DARK_METAL:
                            laf = "org.netbeans.swing.laf.dark.DarkMetalLookAndFeel";
                            break;
                        case DARK_NIMBUS:
                            laf = "org.netbeans.swing.laf.dark.DarkNimbusLookAndFeel";
                            break;
                        default:
                            throw new InternalError();
                    }
                    logger.debug("Installing look and feel: " + laf);
                    UIManager.setLookAndFeel(laf);
                    LookAndFeelFactory.installJideExtension();
                    if (((lookAndFeel == Configuration.LookAndFeel.DARK_METAL)
                            || (lookAndFeel == Configuration.LookAndFeel.DARK_NIMBUS))) {
                        // Patch some things to make dark themes look better
                        VoidRenderer.setColour(UIManager.getColor("Panel.background").getRGB());
                        if (lookAndFeel == Configuration.LookAndFeel.DARK_METAL) {
                            UIManager.put("ContentContainer.background", UIManager.getColor("desktop"));
                            UIManager.put("JideTabbedPane.foreground", new Color(222, 222, 222));
                        }
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    logger.warn("Could not install selected look and feel", e);
                }

                if (getUIScale() != 1.0f) {
                    // Scale the look and feel to the UI
                    GUIUtils.scaleLookAndFeel(getUIScale());
                }
            }

            // Don't paint values above sliders in GTK look and feel
            UIManager.put("Slider.paintValue", Boolean.FALSE);

            final App app = App.getInstance();
            app.setVisible(true);
            // Swing quirk:
            if (myConfig.isMaximised() && (System.getProperty("org.pepsoft.worldpainter.size") == null)) {
                app.setExtendedState(Frame.MAXIMIZED_BOTH);
            }

            // Do this later to give the app the chance to properly set itself up
            SwingUtilities.invokeLater(() -> {
                if (Version.isSnapshot() && ! myConfig.isMessageDisplayed(SNAPSHOT_MESSAGE_KEY)) {
                    String result = JOptionPane.showInputDialog(app, SNAPSHOT_MESSAGE, "Snapshot Release", WARNING_MESSAGE);
                    if (result == null) {
                        // Cancel was pressed
                        System.exit(0);
                    }
                    while (! result.toLowerCase().replace(" ", "").equals("iunderstand")) {
                        DesktopUtils.beep();
                        result = JOptionPane.showInputDialog(app, SNAPSHOT_MESSAGE, "Snapshot Release", WARNING_MESSAGE);
                        if (result == null) {
                            // Cancel was pressed
                            System.exit(0);
                        }
                    }
                    myConfig.setMessageDisplayed(SNAPSHOT_MESSAGE_KEY);
                }
                if (world != null) {
                    // On a Mac we may be doing this unnecessarily because we may be opening a .world file, but it has
                    // proven difficult to detect that. TODO
                    app.setWorld(world, true);
                } else if ((! autosaveInhibited) && myConfig.isAutosaveEnabled() && autosaveFile.isFile()) {
                    logger.info("Recovering autosaved world");
                    app.open(autosaveFile);
                    StartupMessages.addWarning("WorldPainter was not shut down correctly.\nYour world has been recovered from the most recent autosave.\nMake sure to Save it if you want to keep it!");
                } else {
                    app.open(file);
                }
                for (String error: StartupMessages.getErrors()) {
                    beepAndShowError(app, error, "Startup Error");
                }
                for (String error: StartupMessages.getWarnings()) {
                    beepAndShowWarning(app, error, "Startup Warning");
                }
                if (StartupMessages.getErrors().isEmpty() && StartupMessages.getWarnings().isEmpty()) {
                    // Don't bother the user with this if we've already bothered them with errors and/or warnings
                    if (! DonationDialog.maybeShowDonationDialog(app)) {
                        MerchDialog.maybeShowMerchDialog(app);
                    }
                }
            });
        });
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
        StartupMessages.addError("Could not read configuration file! Configuration was reset.\n\nException type: " + e.getClass().getSimpleName() + "\nMessage: " + e.getMessage());
    }

    @Language("HTML")
    private static final String SNAPSHOT_MESSAGE = "<html><h1>Warning: Snapshot Release</h1>" +
            "<p>This is a snapshot release of WorldPainter. It is for testing <em>only</em>!" +
            "<p>Any worlds you edit with this version <strong>may not be loadable</strong> by the next production version<br>when that is released and <strong>will not be loadable</strong> by the current production version!" +
            "<p><strong>Make backups</strong> of any existing worlds you wish to test with this release, in a safe location." +
            "<p>Any or all work you do with this test release may be lost, and if you don't create backups,<br>you may lose your current worlds." +
            "<p>Please report bugs on GitHub: https://github.com/Captain-Chaos/WorldPainter" +
            "<p>Type \"I understand\" below to proceed with testing the next release of WorldPainter:</p></html>";
    private static final String SNAPSHOT_MESSAGE_KEY = "org.pepsoft.worldpainter.snapshotWarning";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    static PrivateContext privateContext;
}