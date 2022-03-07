package org.pepsoft.worldpainter.tools;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.plugins.MapImporterProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.importing.MapImporter.ReadOnlyOption.MAN_MADE;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

public class Import {
    public static void main(String[] args) throws CertificateException, IOException, ClassNotFoundException, ProgressReceiver.OperationCancelled {
        // Load the default platform descriptors so that they don't get blocked
        // by older versions of them which might be contained in the
        // configuration. Do this by loading and initialising (but not
        // instantiating) the DefaultPlugin class
        Class.forName("org.pepsoft.worldpainter.DefaultPlugin");

        // Load or initialise configuration
        Configuration config = Configuration.load(); // This will migrate the configuration directory if necessary
        if (config == null) {
            System.out.println("Creating new configuration");
            config = new Configuration();
        }
        Configuration.setInstance(config);
        System.out.println("Installation ID: " + config.getUuid());

        // Load trusted WorldPainter root certificate
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final X509Certificate trustedCert = (X509Certificate) certificateFactory.generateCertificate(ClassLoader.getSystemResourceAsStream("wproot.pem"));

        // Load the plugins
        final File pluginsDir = new File(Configuration.getConfigDir(), "plugins");
        if (pluginsDir.isDirectory()) {
            PluginManager.loadPlugins(pluginsDir, trustedCert.getPublicKey(), DESCRIPTOR_PATH);
        }
        WPPluginManager.initialise(config.getUuid());

        // Import the map
        final File mapDir = new File(args[0]);
        final PlatformManager platformManager = PlatformManager.getInstance();
        PlatformProvider.MapInfo mapInfo = platformManager.identifyMap(mapDir);
        final PlatformProvider platformProvider = platformManager.getPlatformProvider(mapInfo.platform);
        final TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(0, Terrain.GRASS, mapInfo.platform.minZ, mapInfo.maxHeight, 58, 62, false, true, 20, 1.0);
        final MapImporter importer = ((MapImporterProvider) platformProvider).getImporter(mapDir, tileFactory, null, MAN_MADE, ImmutableSet.of(DIM_NORMAL, DIM_NETHER, DIM_END) /* TODO */);
        System.out.println("+---------+---------+---------+---------+---------+");
        World2 world = importer.doImport(new TextProgressReceiver());
        System.out.println("Successfully imported world " + world.getName());
    }
}