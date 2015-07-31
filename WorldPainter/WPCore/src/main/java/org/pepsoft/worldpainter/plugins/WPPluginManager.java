/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author pepijn
 */
public class WPPluginManager {
    private WPPluginManager(UUID uuid, String logPrefix, ClassLoader classLoader) {
        allPlugins = org.pepsoft.util.PluginManager.findPlugins(Plugin.class, FILENAME, classLoader);
        Set<String> namesEncountered = new HashSet<>();
        for (Iterator<Plugin> i = allPlugins.iterator(); i.hasNext(); ) {
            Plugin plugin = i.next();
            if ((plugin.getUUIDs() != null) && (uuid != null) && (! plugin.getUUIDs().contains(uuid))) {
                logger.error(logPrefix + plugin.getName() + " plugin is not authorised for this installation; not loading it");
                i.remove();
                continue;
            }
            String name = plugin.getName();
            if (namesEncountered.contains(name)) {
                throw new RuntimeException("Multiple plugins with the same name (" + name + ") detected!");
            } else {
                namesEncountered.add(name);
            }
            logger.info(logPrefix + "Loaded plugin: " + name + " (version " + plugin.getVersion() + ")");
        }
    }
    
    public List<Plugin> getAllPlugins() {
        return Collections.unmodifiableList(allPlugins);
    }
    
    @SuppressWarnings("unchecked") // Guaranteed by Java
    public <T extends Plugin> List<T> getPlugins(Class<T> type) {
        List<T> plugins = new ArrayList<>(allPlugins.size());
        plugins.addAll(allPlugins.stream().filter(plugin -> type.isAssignableFrom(plugin.getClass())).map(plugin -> (T) plugin).collect(Collectors.toList()));
        return plugins;
    }
    
    public static synchronized void initialise(UUID uuid) {
        initialise(uuid, "", ClassLoader.getSystemClassLoader());
    }
    
    public static synchronized void initialise(UUID uuid, String logPrefix, ClassLoader classLoader) {
        if (instance != null) {
            throw new IllegalStateException("Already initialised");
        }
        instance = new WPPluginManager(uuid, logPrefix, classLoader);
    }
    
    public static synchronized WPPluginManager getInstance() {
        return instance;
    }
    
    private final List<Plugin> allPlugins;
    
    private static WPPluginManager instance;
    private static final String FILENAME = "org.pepsoft.worldpainter.plugins";
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPPluginManager.class);
}