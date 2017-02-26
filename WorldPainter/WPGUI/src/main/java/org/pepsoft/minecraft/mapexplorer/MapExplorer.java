/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Main;
import org.pepsoft.worldpainter.MouseAdapter;
import org.pepsoft.worldpainter.browser.WPTrustManager;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 *
 * @author pepijn
 */
public class MapExplorer {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Load or initialise configuration
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
        }
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
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(Main.class.getResourceAsStream("/wproot.pem"));

            WPTrustManager trustManager = new WPTrustManager(trustedCert);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("No such algorithm exception while loading trusted root certificate", e);
        } catch (KeyManagementException e) {
            logger.error("Key management exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (trustedCert != null) {
            PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey());
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Minecraft Map Explorer");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            MapTreeModel treeModel = new MapTreeModel();
//            File minecraftDir = MinecraftUtil.findMinecraftDir();
//            File defaultDir;
//            if (minecraftDir != null) {
//                defaultDir = new File(minecraftDir, "saves");
//            } else {
//                defaultDir = new File(System.getProperty("user.home"));
//            }
            JTree tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.setCellRenderer(new MapTreeCellRenderer());
            JScrollPane scrollPane = new JScrollPane(tree);
//            tree.expandPath(treeModel.getPath(defaultDir));
//            tree.scrollPathToVisible(treeModel.getPath(defaultDir));
            // Automatically expand any nodes if they only have one child
            tree.addTreeExpansionListener(new TreeExpansionListener() {
                @Override
                public void treeExpanded(TreeExpansionEvent event) {
                    Object node = event.getPath().getLastPathComponent();
                    if ((! treeModel.isLeaf(node)) && (treeModel.getChildCount(node) == 1)) {
                        tree.expandPath(event.getPath().pathByAddingChild(treeModel.getChild(node, 0)));
                    }
                }

                @Override public void treeCollapsed(TreeExpansionEvent event) {}
            });
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        if (path != null) {
                            ((Node) path.getLastPathComponent()).doubleClicked();
                        }
                    }
                }
            });
            frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static final Logger logger = LoggerFactory.getLogger(MapExplorer.class);
}