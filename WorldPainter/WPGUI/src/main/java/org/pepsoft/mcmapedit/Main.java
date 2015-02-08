/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.mcmapedit;

import org.pepsoft.worldpainter.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.pepsoft.worldpainter.browser.WPTrustManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.WPLogManager;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.plugins.Plugin;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.vo.EventVO;

/**
 *
 * @author pepijn
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Force language to English for now. TODO: remove this once the first
        // translations are implemented
        Locale.setDefault(Locale.US);
        
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        // Configure logging
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        boolean debugLogging = "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"));
        Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    StringWriter sw = new StringWriter();
                    StringBuffer sb = sw.getBuffer();
                    java.util.Formatter formatter = new java.util.Formatter(sb);
                    String loggerName = record.getLoggerName();
                    if (loggerName.length() > 30) {
                        loggerName = loggerName.substring(loggerName.length() - 30);
                    }
                    long date = record.getMillis();
                    long millis = date % 1000;
                    formatter.format("[%tF %<tT.%03d] {%-6s} (%30s) %s%n", date, millis, record.getLevel().getName(), loggerName, record.getMessage());
                    if (record.getThrown() != null) {
                        record.getThrown().printStackTrace(new PrintWriter(sw, false));
                    }
                    return sb.toString();
                }
            };
        for (Handler handler: rootLogger.getHandlers()) {
            handler.setLevel(debugLogging ? Level.FINER : Level.INFO);
            handler.setFormatter(formatter);
        }
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
        }
        try {
            FileHandler fileHandler = new FileHandler(configDir.getAbsolutePath() + "/logfile%g.txt", 10 * 1024 * 1024, 2, true);
            fileHandler.setLevel(debugLogging ? Level.FINER : Level.INFO);
            fileHandler.setFormatter(formatter);
            rootLogger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (debugLogging) {
            Logger pepsoftLogger = Logger.getLogger("org.pepsoft");
            pepsoftLogger.setLevel(Level.FINE);
            additionalLoggers.add(pepsoftLogger);
        }
        logger.info("Starting MinecraftMapEditor " + Version.VERSION);

        // Load or initialise configuration
        Configuration config = null;
        try {
            config = Configuration.load(); // This will migrate the configuration directory if necessary
        } catch (IOException e) {
            configError(e);
        } catch (ClassNotFoundException e) {
            configError(e);
        } catch (NullPointerException e) {
            configError(e);
        }
        if (config == null) {
            if (! logger.isLoggable(Level.FINE)) {
                // If debug logging is on, the Configuration constructor will
                // already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

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
            logger.log(Level.SEVERE, "Certificate exception while loading trusted root certificate", e);
        } catch (NoSuchAlgorithmException  e) {
            logger.log(Level.SEVERE, "No such algorithm exception while loading trusted root certificate", e);
        } catch (KeyManagementException e) {
            logger.log(Level.SEVERE, "Key management exception while loading trusted root certificate", e);
        }
        
        // Load the plugins
        if (trustedCert != null) {
            org.pepsoft.util.PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey());
        } else {
            logger.severe("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());
        
        String httpAgent = "MinecraftMapEditor " + Version.VERSION + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ";";
//        System.out.println(httpAgent);
        System.setProperty("http.agent", httpAgent);
        
        final long start = System.currentTimeMillis();
        config.setLaunchCount(config.getLaunchCount() + 1);
        Runtime.getRuntime().addShutdownHook(new Thread("Configuration Saver") {
            @Override
            public void run() {
                try {
                    Configuration config = Configuration.getInstance();
                    MouseOrTabletOperation.flushEvents(config);
                    BetterAction.flushEvents(config);
                    EventVO sessionEvent = new EventVO("minecraftmapeditor.session").setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start)).duration(System.currentTimeMillis() - start);
                    StringBuilder sb = new StringBuilder();
                    List<Plugin> plugins = WPPluginManager.getInstance().getAllPlugins();
                    for (Plugin plugin: plugins) {
                        if (! plugin.getName().equals("Default")) {
                            if (sb.length() > 0) {
                                sb.append(',');
                            }
                            sb.append("{name=");
                            sb.append(plugin.getName().replaceAll("[ \\t\\n\\x0B\\f\\r\\.]", ""));
                            sb.append(",version=");
                            sb.append(plugin.getVersion());
                            sb.append('}');
                        }
                    }
                    if (sb.length() > 0) {
                        sessionEvent.setAttribute(ATTRIBUTE_KEY_PLUGINS, sb.toString());
                    }
                    config.logEvent(sessionEvent);
                    config.save();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "I/O error saving configuration", e);
                }
                logger.info("Shutting down MinecraftMapEditor");
                ((WPLogManager) LogManager.getLogManager()).realReset();
            }
        });
        
        // Make the "action:" and "bitcoin:" URLs used in various places work:
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if (protocol.equals("action")) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            throw new UnsupportedOperationException("Not supported");
                        }
                    };
                } else if (protocol.equals("bitcoin")) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            throw new UnsupportedOperationException("Not supported");
                        }
                    };
                } else {
                    return null;
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Use the system look and feel
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                    // We tried...
                } catch (InstantiationException e) {
                    // We tried...
                } catch (IllegalAccessException e) {
                    // We tried...
                } catch (UnsupportedLookAndFeelException e) {
                    // We tried...
                }

                // Don't paint values above sliders in GTK look and feel
                UIManager.put("Slider.paintValue", Boolean.FALSE);

                App.setMode(App.Mode.MINECRAFTMAPEDITOR);
                App app = App.getInstance();
                app.setVisible(true);
            }
        });
    }
    
    private static void configError(Exception e) {
        JOptionPane.showMessageDialog(null, "Could not read configuration file! Resetting configuration.\n\nException type: " + e.getClass().getSimpleName() + "\nMessage: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    /**
     * A list of references to package loggers which had diverging log levels
     * set, so that they don't get garbage collected before an actual logger is
     * created in the package.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final List<Logger> additionalLoggers = new ArrayList<Logger>();
}