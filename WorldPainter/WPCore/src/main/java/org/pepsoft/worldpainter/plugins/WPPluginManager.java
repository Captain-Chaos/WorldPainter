/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import org.pepsoft.util.PluginManager;

import java.io.File;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A manager of WorldPainter {@link Plugin}s.
 *
 * @author pepijn
 */
public class WPPluginManager {
    private WPPluginManager(UUID uuid, ClassLoader classLoader) {
        allPlugins = PluginManager.findPlugins(Plugin.class, FILENAME, classLoader);
        Set<String> namesEncountered = new HashSet<>();
        for (Iterator<Plugin> i = allPlugins.iterator(); i.hasNext(); ) {
            Plugin plugin = i.next();
            if ((plugin.getUUIDs() != null) && (uuid != null) && (! plugin.getUUIDs().contains(uuid))) {
                logger.error(plugin.getName() + " plugin is not authorised for this installation; not loading it");
                i.remove();
                continue;
            }
            String name = plugin.getName();
            if (namesEncountered.contains(name)) {
                throw new RuntimeException("Multiple plugins with the same name (" + name + ") detected!");
            } else {
                namesEncountered.add(name);
            }
            logger.info("Loaded plugin: " + name + " (version " + plugin.getVersion() + ")");
        }
    }

    /**
     * Get a list of all loaded WorldPainter plugins.
     *
     * @return A list of all loaded WorldPainter plugins.
     */
    public List<Plugin> getAllPlugins() {
        return Collections.unmodifiableList(allPlugins);
    }

    /**
     * Get a list of all loaded WorldPainter plugins which implement a
     * particular type.
     *
     * @param type The type of plugin to return.
     * @param <T> The type of plugin to return.
     * @return A list of all loaded WorldPainter plugins which implement the
     * specified type
     */
    @SuppressWarnings("unchecked") // Guaranteed by Java
    public <T extends Plugin> List<T> getPlugins(Class<T> type) {
        return allPlugins.stream()
            .filter(plugin -> type.isAssignableFrom(plugin.getClass()))
            .map(plugin -> (T) plugin)
            .collect(Collectors.toList());
    }

    /**
     * Initialise the WorldPainter plugin manager for a particular WorldPainter
     * installation. This method or {@link #initialise(UUID, ClassLoader)}
     * should be invoked only once, before {@link #getInstance()} is invoked.
     *
     * <p><strong>Please note!</strong> If plugins should be loaded from plugin
     * jars, {@link PluginManager#loadPlugins(File, PublicKey, String)} must be
     * invoked <em>before</em> this method, to ensure the jars are discovered.
     *
     * @param uuid The unique identifier of the WorldPainter installation for
     *             which to initialise the WorldPainter plugin manager.
     */
    public static synchronized void initialise(UUID uuid) {
        initialise(uuid, ClassLoader.getSystemClassLoader());
    }
    
    /**
     * Initialise the WorldPainter plugin manager for a particular WorldPainter
     * installation. This method or {@link #initialise(UUID)} should be invoked
     * only once, before {@link #getInstance()} is invoked.
     *
     * <p><strong>Please note!</strong> If plugins should be loaded from plugin
     * jars, {@link PluginManager#loadPlugins(File, PublicKey, String)} must be
     * invoked <em>before</em> this method, to ensure the jars are discovered.
     *
     * @param uuid The unique identifier of the WorldPainter installation for
     *             which to initialise the WorldPainter plugin manager.
     * @param classLoader The class loader from which to discover the default/
     *                    system plugins (those not loaded from plugin jars).
     */
    public static synchronized void initialise(UUID uuid, ClassLoader classLoader) {
        if (instance != null) {
            throw new IllegalStateException("Already initialised");
        }
        instance = new WPPluginManager(uuid, classLoader);
    }

    /**
     * Obtain the single instance of the WorldPainter plugin manager. Note that
     * the plugin manager must be initialised first by invoking
     * {@link #initialise(UUID)} or {@link #initialise(UUID, ClassLoader)}.
     *
     * @return The single instance of the WorldPainter plugin manager.
     */
    public static synchronized WPPluginManager getInstance() {
        return instance;
    }
    
    private final List<Plugin> allPlugins;

    /**
     * The resource filename in which WorldPainter plugin descriptors are
     * stored. Pass this into the {@code filename} parameter of the
     * {@link PluginManager#loadPlugins(File, PublicKey, String)} method.
     */
    public static final String FILENAME = "org.pepsoft.worldpainter.plugins";

    private static WPPluginManager instance;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPPluginManager.class);
}