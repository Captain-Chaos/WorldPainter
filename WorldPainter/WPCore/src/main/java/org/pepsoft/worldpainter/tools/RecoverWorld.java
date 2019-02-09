/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.PluginManager;
import org.pepsoft.util.WPCustomObjectInputStream;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.AbstractObject;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

/**
 *
 * @author pepijn
 */
public class RecoverWorld {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int defaultMaxHeight = Integer.parseInt(args[1]);

        // Load or initialise configuration
        Configuration config;
        try {
            config = Configuration.load(); // This will migrate the configuration directory if necessary
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
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

        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(RecoverWorld.class.getResourceAsStream("/wproot.pem"));
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

        // Read as much data as possible. Use a trick via InstanceKeeper to get
        // hold of the objects as they are created during deserialisation, even
        // if the readObject() method throws an exception and never returns an
        // instance
        final List<World2> worlds = new ArrayList<>();
        final Map<Dimension, List<Tile>> tiles = new HashMap<>();
        @SuppressWarnings("unchecked")
        final List<Tile>[] tileListHolder = new List[1];
        InstanceKeeper.setInstantiationListener(World2.class, worlds::add);
        InstanceKeeper.setInstantiationListener(Dimension.class, dimension -> {
            List<Tile> tileList = new ArrayList<>();
            tiles.put(dimension, tileList);
            tileListHolder[0] = tileList;
        });
        InstanceKeeper.setInstantiationListener(Tile.class, tile -> tileListHolder[0].add(tile));
        File file = new File(args[0]);
        try {
            try (WPCustomObjectInputStream wrappedIn = new WPCustomObjectInputStream(new GZIPInputStream(new FileInputStream(file)), PluginManager.getPluginClassLoader(), AbstractObject.class)) {
                Map<String, Object> metadata = null;
                World2 world;
                Object object = wrappedIn.readObject();
                if (object instanceof Map) {
                    metadata = (Map<String, Object>) object;
                    object = wrappedIn.readObject();
                }
                if (object instanceof World2) {
                    world = (World2) object;
                    if (metadata != null) {
                        world.setMetadata(metadata);
                    }
                } else if (object instanceof World) {
                    throw new RuntimeException("Old worlds (pre-0.2) not supported");
                } else {
                    throw new RuntimeException("Object of unexpected type " + object.getClass() + " encountered");
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: I/O error while reading world; world most likely corrupted! (Type: " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ")");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: class not found while reading world; world most likely corrupted! (Type: " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ")");
        }
        
        // Reconstitute as much of the data as possible
        System.out.println(worlds.size() + " worlds read");
        System.out.println(tiles.size() + " dimensions read");
        World2 newWorld = null;
        for (Map.Entry<Dimension, List<Tile>> entry: tiles.entrySet()) {
            Dimension dimension = entry.getKey();
            List<Tile> tileList = entry.getValue();
            System.out.println(tileList.size() + " tiles read for dimension " + dimension.getName());
            int maxHeight;
            if (dimension.getMaxHeight() != 0) {
                maxHeight = dimension.getMaxHeight();
            } else {
                maxHeight = defaultMaxHeight;
            }
            if (newWorld == null) {
                if (worlds.size() > 0) {
                    World2 world = worlds.get(0);
                    if (world.getPlatform() != null) {
                        newWorld = new World2(world.getPlatform(), maxHeight);
                    } else if (maxHeight == DEFAULT_MAX_HEIGHT_ANVIL) {
                        newWorld = new World2(JAVA_ANVIL, maxHeight);
                    } else {
                        newWorld = new World2(JAVA_MCREGION, maxHeight);
                    }
                    if (world.getName() != null) {
                        newWorld.setName(worlds.get(0).getName() + " (recovered)");
                    }
                    newWorld.setCreateGoodiesChest(world.isCreateGoodiesChest());
                    try {
                        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                            newWorld.setMixedMaterial(i, world.getMixedMaterial(i));
                        }
                    } catch (NullPointerException e) {
                        System.err.println("Custom material settings lost");
                    }
                    newWorld.setGameType(world.getGameType());
                    if (world.getGenerator() != null) {
                        newWorld.setGenerator(world.getGenerator());
                    } else {
                        System.err.println("Landscape generator setting lost");
                    }
                    newWorld.setImportedFrom(world.getImportedFrom());
                    newWorld.setMapFeatures(world.isMapFeatures());
                    if (world.getSpawnPoint() != null) {
                        newWorld.setSpawnPoint(world.getSpawnPoint());
                    } else {
                        System.err.println("Spawn point setting lost; resetting to 0,0");
                    }
                    if (world.getUpIs() != null) {
                        newWorld.setUpIs(world.getUpIs());
                    } else {
                        System.err.println("North direction setting lost; resetting to north is up");
                    }
                } else {
                    System.err.println("No world recovered; all world settings lost");
                    if (maxHeight == DEFAULT_MAX_HEIGHT_ANVIL) {
                        newWorld = new World2(JAVA_ANVIL, maxHeight);
                    } else {
                        newWorld = new World2(JAVA_MCREGION, maxHeight);
                    }
                }
                newWorld.addHistoryEntry(HistoryEntry.WORLD_RECOVERED);
                if (newWorld.getName() == null) {
                    String worldName = file.getName();
                    if (worldName.toLowerCase().endsWith(".world")) {
                        worldName = worldName.substring(0, worldName.length() - 6);
                    }
                    newWorld.setName(worldName + " (recovered)");
                }
            }
            TileFactory tileFactory = dimension.getTileFactory();
            if (tileFactory == null) {
                System.err.println("Dimension " + dimension.getName() + " tile factory lost; creating default tile factory");
                tileFactory = TileFactoryFactory.createNoiseTileFactory(dimension.getSeed(), Terrain.GRASS, maxHeight, 58, 62, false, true, 20, 1.0);
            }
            Dimension newDimension = new Dimension(newWorld, dimension.getMinecraftSeed(), tileFactory, dimension.getDim(), maxHeight);
            try {
                for (Map.Entry<Layer, ExporterSettings> settingsEntry: dimension.getAllLayerSettings().entrySet()) {
                    if (settingsEntry.getValue() != null) {
                        newDimension.setLayerSettings(settingsEntry.getKey(), settingsEntry.getValue());
                    } else {
                        System.err.println("Layer settings for layer " + settingsEntry.getKey().getName() + " lost for dimension " + dimension.getName());
                    }
                }
            } catch (NullPointerException e) {
                System.err.println("Layer settings lost for dimension " + dimension.getName());
            }
            newDimension.setBedrockWall(dimension.isBedrockWall());
            if ((dimension.getBorderLevel() > 0) && (dimension.getBorderSize() > 0)) {
                newDimension.setBorder(dimension.getBorder());
                newDimension.setBorderLevel(dimension.getBorderLevel());
                newDimension.setBorderSize(dimension.getBorderSize());
            } else {
                System.err.println("Border settings lost for dimension " + dimension.getName());
            }
            if (dimension.getContourSeparation() > 0) {
                newDimension.setContoursEnabled(dimension.isContoursEnabled());
                newDimension.setContourSeparation(dimension.getContourSeparation());
            } else {
                System.err.println("Contour settings lost for dimension " + dimension.getName());
            }
            newDimension.setDarkLevel(dimension.isDarkLevel());
            if (dimension.getGridSize() > 0) {
                newDimension.setGridEnabled(dimension.isGridEnabled());
                newDimension.setGridSize(dimension.getGridSize());
            } else {
                System.err.println("Grid settings lost for dimension " + dimension.getName());
            }
            newDimension.setMinecraftSeed(dimension.getMinecraftSeed());
            newDimension.setOverlay(dimension.getOverlay());
            newDimension.setOverlayEnabled(dimension.isOverlayEnabled());
            newDimension.setOverlayOffsetX(dimension.getOverlayOffsetX());
            newDimension.setOverlayOffsetY(dimension.getOverlayOffsetY());
            newDimension.setOverlayScale(dimension.getOverlayScale());
            newDimension.setOverlayTransparency(dimension.getOverlayTransparency());
            newDimension.setPopulate(dimension.isPopulate());
            if (dimension.getSubsurfaceMaterial() != null) {
                newDimension.setSubsurfaceMaterial(dimension.getSubsurfaceMaterial());
            } else {
                System.err.println("Sub surface material lost for dimension " + dimension.getName() + "; resetting to STONE_MIX");
                newDimension.setSubsurfaceMaterial(Terrain.STONE_MIX);
            }
            newWorld.addDimension(newDimension);
            for (Tile tile: tileList) {
                tile.repair(maxHeight, System.err);
                newDimension.addTile(tile);
            }
        }
        
        // Write to a new file
        String filename = file.getName();
        if (filename.toLowerCase().endsWith(".world")) {
            filename = filename.substring(0, filename.length() - 6);
        }
        filename = filename + ".recovered.world";
        WorldIO worldIO = new WorldIO(newWorld);
        File outFile = new File(file.getParentFile(), filename);
        try (OutputStream out = new FileOutputStream(outFile)) {
            worldIO.save(out);
        }
        System.out.println("Recovered world written to " + outFile);
    }

    private static final Logger logger = LoggerFactory.getLogger(RecoverWorld.class);
}