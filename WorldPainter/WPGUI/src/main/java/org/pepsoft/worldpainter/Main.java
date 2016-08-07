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
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.browser.WPTrustManager;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.plugins.Plugin;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_PLUGINS;

/**
 *
 * @author pepijn
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // Force language to English for now. TODO: remove this once the first
        // translations are implemented
        Locale.setDefault(Locale.US);
        
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        // Configure logging
        String logLevel;
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "DEBUG";
        } else if ("extra".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "TRACE";
        } else {
            logLevel = "INFO";
        }
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
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

        // Set the acceleration mode. For some reason we don't fully understand,
        // loading the Configuration from disk initialises Java2D, so we have to
        // do this *before* then.
        // But only do this if a config file exists. If it does not, someone may
        // be trying to reset the configuration, so make sure that the
        // acceleration type setting is reset too in that case.
        AccelerationType accelerationType;
        if (Configuration.getConfigFile().isFile()) {
            String accelTypeName = Preferences.userNodeForPackage(Main.class).get("accelerationType", null);
            if (accelTypeName != null) {
                accelerationType = AccelerationType.valueOf(accelTypeName);
            } else {
                accelerationType = AccelerationType.DEFAULT;
                // TODO: Experiment with which ones work well and use them by default!
            }
            switch (accelerationType) {
                case UNACCELERATED:
                    // Try to disable all accelerated pipelines we know of:
                    System.setProperty("sun.java2d.d3d", "false");
                    System.setProperty("sun.java2d.opengl", "false");
                    System.setProperty("sun.java2d.xrender", "false");
                    System.setProperty("apple.awt.graphics.UseQuartz", "false");
                    break;
                case DIRECT3D:
                    // Direct3D should already be the default on Windows, but
                    // enable a few things which are off by default:
                    System.setProperty("sun.java2d.translaccel", "true");
                    System.setProperty("sun.java2d.ddscale", "true");
                    break;
                case OPENGL:
                    System.setProperty("sun.java2d.opengl", "True");
                    break;
                case XRENDER:
                    System.setProperty("sun.java2d.xrender", "True");
                    break;
                case QUARTZ:
                    System.setProperty("apple.awt.graphics.UseQuartz", "true");
                    break;
            }
        } else {
            accelerationType = AccelerationType.DEFAULT;
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

        // Store the acceleration type in the config object so the Preferences
        // dialog can edit it
        config.setAccelerationType(accelerationType);

        // Start background scan for Minecraft jars
        BiomeSchemeManager.initialiseInBackground();
        
        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(Main.class.getResourceAsStream("/wproot.pem"));
            
            WPTrustManager trustManager = new WPTrustManager(trustedCert);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        } catch (NoSuchAlgorithmException  e) {
            logger.error("No such algorithm exception while loading trusted root certificate", e);
        } catch (KeyManagementException e) {
            logger.error("Key management exception while loading trusted root certificate", e);
        }
        
        // Load the plugins
        if (trustedCert != null) {
            org.pepsoft.util.PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey());
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());
        
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
                            .filter(plugin -> ! (plugin.getName().equals("Default") || plugin.getName().equals("DefaultLayerEditorProvider")))
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
                    config.logEvent(sessionEvent);
                    config.save();

                    // Store the acceleration type separately, because we need
                    // it before we can load the config:
                    Preferences prefs = Preferences.userNodeForPackage(Main.class);
                    prefs.put("accelerationType", config.getAccelerationType().name());
                    prefs.flush();
                } catch (IOException e) {
                    logger.error("I/O error saving configuration", e);
                } catch (BackingStoreException e) {
                    logger.error("Backing store exception saving acceleration type", e);
                }
                logger.info("Shutting down WorldPainter");
            }
        });
        
        // Make the "action:" and "bitcoin:" URLs used in various places work:
        URL.setURLStreamHandlerFactory(protocol -> {
            switch (protocol) {
                case "action":
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            throw new UnsupportedOperationException("Not supported");
                        }
                    };
                case "bitcoin":
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
        final File file;
        if ((args.length > 0) && new File(args[0]).isFile()) {
            file = new File(args[0]);
            world = null;
        } else {
            file = null;
            world = WorldFactory.createDefaultWorld(config, new Random().nextLong());
//            world = WorldFactory.createFancyWorld(config, new Random().nextLong());
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
                if ((lookAndFeel == Configuration.LookAndFeel.DARK_METAL)
                        || (lookAndFeel == Configuration.LookAndFeel.DARK_NIMBUS)) {
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

            // Don't paint values above sliders in GTK look and feel
            UIManager.put("Slider.paintValue", Boolean.FALSE);

            final App app = App.getInstance();
            app.setVisible(true);
            // Swing quirk:
            if (Configuration.getInstance().isMaximised() && (System.getProperty("org.pepsoft.worldpainter.size") == null)) {
                app.setExtendedState(Frame.MAXIMIZED_BOTH);
            }

            // Do this later to give the app the chance to properly set
            // itself up
            SwingUtilities.invokeLater(() -> {
                if (world != null) {
                    // On a Mac we may be doing this unnecessarily because we
                    // may be opening a .world file, but it has proven difficult
                    // to detect that. TODO
                    app.setWorld(world);
                } else {
                    app.open(file);
                }
                DonationDialog.maybeShowDonationDialog(app);
            });
        });
    }

    private static void configError(Throwable e) {
        JOptionPane.showMessageDialog(null, "Could not read configuration file! Resetting configuration.\n\nException type: " + e.getClass().getSimpleName() + "\nMessage: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    static PrivateContext privateContext;
}