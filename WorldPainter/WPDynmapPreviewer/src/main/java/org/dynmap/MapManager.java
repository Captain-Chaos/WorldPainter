package org.dynmap;

import org.dynmap.hdmap.HDMapManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Private implementation of <code>MapManager</code> from dynmap in order to work offline.
 *
 * <p>Created by Pepijn Schmitz on 05-06-15.
 */
public class MapManager {
    public DynmapCore.CompassMode getCompassMode() {
        return DynmapCore.CompassMode.NEWNORTH;
    }

    public boolean getBetterGrass() {
        return false;
    }

    public boolean useBrightnessTable() {
        return true;
    }

    private void init() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "WorldPainter");
        ConfigurationNode configNode = new ConfigurationNode(config);
        ((WPHDMapManager) hdmapman).init(configNode);
    }

    public final HDMapManager hdmapman = new WPHDMapManager();

    public static final MapManager mapman = new MapManager(); /* Our singleton */

    static {
        mapman.init();
    }
}