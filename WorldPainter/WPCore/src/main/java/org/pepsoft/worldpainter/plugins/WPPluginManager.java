/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import org.pepsoft.util.Version;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.StartupMessages;
import org.pepsoft.worldpainter.WPContext;

import java.io.File;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

/**
 * A manager of WorldPainter {@link Plugin}s.
 *
 * @author pepijn
 */
public class WPPluginManager {
    private WPPluginManager(UUID uuid, ClassLoader classLoader, WPContext context) {
        allPlugins = PluginManager.findPlugins(Plugin.class, DESCRIPTOR_PATH, classLoader);
        PluginManager.getErrors().forEach(StartupMessages::addError);
        PluginManager.getMessages().forEach(StartupMessages::addMessage);
        Set<String> namesEncountered = new HashSet<>();
        for (Iterator<Plugin> i = allPlugins.iterator(); i.hasNext(); ) {
            Plugin plugin = i.next();
            if ((plugin.getUUIDs() != null) && (uuid != null) && (! plugin.getUUIDs().contains(uuid))) {
                String message = plugin.getName() + " plugin is not authorised for this installation";
                StartupMessages.addError(message);
                logger.error(message + "; not loading it");
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
        for (Plugin plugin : allPlugins) {
            try {
                plugin.init(context);
                logger.info("Initialised plugin: " + plugin.getName() + " (version " + plugin.getVersion() + ")");
            } catch (RuntimeException e) {
                String message = "Exception initializing plugin " + plugin.getName() + " (" + plugin.getVersion() + ")\n(Type: " + e.getClass().getSimpleName() + "; message; " + e.getMessage() + ")" ;
                StartupMessages.addError(message);
                logger.error(message, e);
                allPlugins.remove(plugin);
            }
        }
    }

    /**
     * Get a list of all loaded WorldPainter plugins.
     *
     * @return A list of all loaded WorldPainter plugins.
     */
    public List<Plugin> getAllPlugins() {
        return unmodifiableList(allPlugins);
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
     * Initialise the WorldPainter plugin manager for a particular WorldPainter installation. This method or
     * {@link #initialise(UUID, ClassLoader, WPContext)} should be invoked only once, before {@link #getInstance()} is
     * invoked.
     *
     * <p><strong>Please note!</strong> If plugins should be loaded from plugin jars,
     * {@link PluginManager#loadPlugins(File, PublicKey, String, Version, boolean)} must be invoked <em>before</em> this
     * method, to ensure the jars are discovered.
     *
     * <p>This method also calls the plugins' {@link Plugin#init(WPContext)} method with the provided context.
     *
     * @param uuid The unique identifier of the WorldPainter installation for which to initialise the WorldPainter
     *             plugin manager.
     * @param context The context to provide to the plugins' {@code init} methods.
     */
    public static synchronized void initialise(UUID uuid, WPContext context) {
        initialise(uuid, ClassLoader.getSystemClassLoader(), context);
    }
    
    /**
     * Initialise the WorldPainter plugin manager for a particular WorldPainter installation. This method or
     * {@link #initialise(UUID, WPContext)} should be invoked only once, before {@link #getInstance()} is invoked.
     *
     * <p><strong>Please note!</strong> If plugins should be loaded from plugin jars,
     * {@link PluginManager#loadPlugins(File, PublicKey, String, Version, boolean)} must be invoked <em>before</em> this
     * method, to ensure the jars are discovered.
     *
     * <p>This method also calls the plugins' {@link Plugin#init(WPContext)} method with the provided context.
     *
     * @param uuid The unique identifier of the WorldPainter installation for which to initialise the WorldPainter
     *             plugin manager.
     * @param classLoader The class loader from which to discover the default/system plugins (those not loaded from
     *                    plugin jars).
     * @param context The context to provide to the plugins' {@code init} methods.
     */
    public static synchronized void initialise(UUID uuid, ClassLoader classLoader, WPContext context) {
        if (instance != null) {
            throw new IllegalStateException("Already initialised");
        }
        instance = new WPPluginManager(uuid, classLoader, context);
    }

    /**
     * Obtain the single instance of the WorldPainter plugin manager. Note that the plugin manager must be initialised
     * first by invoking {@link #initialise(UUID, WPContext)} or {@link #initialise(UUID, ClassLoader, WPContext)}.
     *
     * @return The single instance of the WorldPainter plugin manager.
     */
    public static synchronized WPPluginManager getInstance() {
        return instance;
    }
    
    private final List<Plugin> allPlugins;

    /**
     * The resource filename in which WorldPainter plugin descriptors are stored. Pass this into the {@code filename}
     * parameter of the {@link PluginManager#loadPlugins(File, PublicKey, String, Version, boolean)} method.
     */
    public static final String DESCRIPTOR_PATH = "org.pepsoft.worldpainter.plugins";

    private static WPPluginManager instance;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPPluginManager.class);
}