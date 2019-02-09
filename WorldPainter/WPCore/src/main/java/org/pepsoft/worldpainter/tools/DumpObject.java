package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.CustomObjectManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

/**
 * Created by Pepijn Schmitz on 02-09-15.
 */
public class DumpObject {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
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
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(DumpObject.class.getResourceAsStream("/wproot.pem"));
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

        String filename = args[0];
        File file = new File(filename);
        int p = filename.lastIndexOf('.');
        Object object;
        switch (filename.substring(p + 1).toLowerCase()) {
            case "layer":
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                    object = in.readObject();
                }
                break;
            default:
                object = CustomObjectManager.getInstance().loadObject(file);
                break;
        }
        dump(object);
    }

    private static void dump(Object object) {
        if (object instanceof Bo2Layer) {
            ((Bo2Layer) object).getObjectProvider().getAllObjects().forEach(DumpObject::dump);
        } else if (object instanceof WPObject) {
            WPObject wpObject = (WPObject) object;
            wpObject.prepareForExport(FAKE_DIMENSION);
            logger.info("Name: " + wpObject.getName());
            Point3i dim = wpObject.getDimensions();
            logger.info("    Dimensions: " + dim);
            logger.info("    Offset: " + wpObject.getOffset());
            if (wpObject.getAttributes() != null) {
                logger.info("    Attributes:");
                wpObject.getAttributes().forEach((key, value) -> logger.info("        " + key + ": " + value));
            }
            if (wpObject.getEntities() != null) {
                logger.info("    Entities:");
                wpObject.getEntities().forEach(entity -> logger.info("        " + entity.getId() + " @ " + entity.getPos()[0] + "," + entity.getPos()[2] + "," + entity.getPos()[1] + "; data: " + entity.toNBT()));
            }
            if (wpObject.getTileEntities() != null) {
                logger.info("    Tile entities:");
                wpObject.getTileEntities().forEach(entity -> logger.info("        " + entity.getId() + " @ " + entity.getY() + "," + entity.getZ() + "," + entity.getY() + "; data: " + entity.toNBT()));
            }
            logger.info("    Blocks:");
            int blockCount = 0;
            for (int z = 0; z < dim.z; z++) {
                for (int x = 0; x < dim.x; x++) {
                    for (int y = 0; y < dim.y; y++) {
                        if (wpObject.getMask(x, y, z)) {
                            blockCount++;
                            if (blockCount <= 100) {
                                logger.info("        " + x + "," + y + "," + z + ": " + wpObject.getMaterial(x, y, z));
                            }
                        }
                    }
                }
            }
            if (blockCount > 100) {
                logger.info("        ... and " + (blockCount - 100) + " more");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized object type " + object.getClass());
        }
    }

    private static final Dimension FAKE_DIMENSION;

    static {
        TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(0L, Terrain.GRASS, DEFAULT_MAX_HEIGHT_ANVIL, 58, 62, false, true, 20.0f, 1.0);
        FAKE_DIMENSION = new World2(DefaultPlugin.JAVA_ANVIL, 0L, tileFactory, DEFAULT_MAX_HEIGHT_ANVIL).getDimension(DIM_NORMAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(DumpObject.class);
}