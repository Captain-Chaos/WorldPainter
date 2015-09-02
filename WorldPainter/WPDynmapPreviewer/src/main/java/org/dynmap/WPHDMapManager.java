package org.dynmap;

import org.dynmap.hdmap.*;

/**
 * An alternative implementation of {@link HDMapManager} which creates a hard
 * coded perspective, shader and lighting instead of instantiating them from a
 * configuration file. Used by this module's private implementation of
 * {@link MapManager}.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
class WPHDMapManager extends HDMapManager {
    void init(ConfigurationNode configNode) {
        DynmapCore core = new DynmapCore();
        perspectives.put("default", new IsoHDPerspective(core, configNode));
        TexturePack.loadTextureMapping(core, configNode);
        shaders.put("default", new TexturePackHDShader(core, configNode));
//        shaders.put("default", new DefaultHDShader(core, configNode));
        shaders.put("caves", new CaveHDShader(core, configNode));
        lightings.put("default", new DefaultHDLighting(core, configNode));
    }
}