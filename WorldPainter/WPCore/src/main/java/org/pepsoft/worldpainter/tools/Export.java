/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Platform;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.PluginManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;

/**
 *
 * @author pepijn
 */
public class Export {
    public static void main(String args[]) throws IOException, ClassNotFoundException, OperationCancelled, CertificateException {
//        Logger rootLogger = Logger.getLogger("");
//        rootLogger.setLevel(Level.OFF);
        
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

        File worldFile = new File(args[0]);
        System.out.println("Loading " + worldFile);
        World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(worldFile)))) {
            world = (World2) in.readObject();
        }

        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }
        if (world.getPlatform() == null) {
            if (world.getMaxHeight() == Constants.DEFAULT_MAX_HEIGHT_2) {
                world.setPlatform(Platform.JAVA_ANVIL);
            } else {
                world.setPlatform(Platform.JAVA_MCREGION);
            }
        }
        
        File exportDir;
        if (args.length > 1) {
            exportDir = new File(args[1]);
        } else {
            File minecraftDir = MinecraftUtil.findMinecraftDir();
            exportDir = new File(minecraftDir, "saves");
        }
        System.out.println("Exporting to " + exportDir);
        System.out.println("+---------+---------+---------+---------+---------+");
        JavaWorldExporter exporter = new JavaWorldExporter(world);
        exporter.export(exportDir, world.getName(), exporter.selectBackupDir(new File(exportDir, FileUtils.sanitiseName(world.getName()))), new ProgressReceiver() {
            @Override
            public void setProgress(float progressFraction) throws OperationCancelled {
                int progress = (int) (progressFraction * 50);
                while (progress > previousProgress) {
                    System.out.print('.');
                    previousProgress++;
                }
            }

            @Override
            public void exceptionThrown(Throwable exception) {
                exception.printStackTrace();
                System.exit(1);
            }

            @Override public void reset() {
                System.out.println();
                previousProgress = -1;
            }
            
            @Override public void done() {}
            @Override public void setMessage(String message) throws OperationCancelled {}
            @Override public void checkForCancellation() throws OperationCancelled {}
            @Override public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {}
            
            private int previousProgress = -1;
        });
        System.out.println();
        System.out.println("World " + world.getName() + " exported successfully");
    }
}