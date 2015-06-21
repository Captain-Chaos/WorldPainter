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
        perspectives.put("default", new IsoHDPerspective(null, configNode));
//        shaders.put("default", new TexturePackHDShader(null, configNode));
        shaders.put("default", new DefaultHDShader(null, configNode));
        shaders.put("caves", new CaveHDShader(null, configNode));
        lightings.put("default", new DefaultHDLighting(null, configNode));
    }
}