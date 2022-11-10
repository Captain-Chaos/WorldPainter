package org.dynmap;

import org.dynmap.hdmap.*;
import org.pepsoft.worldpainter.dynmap.DynmapBlockStateHelper;

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
        perspectives.put("default", new IsoHDPerspective(DynmapCore.INSTANCE, configNode));
        final DefaultHDShader solidShader = new DefaultHDShader(DynmapCore.INSTANCE, configNode);
        shaders.put("solid", solidShader);
        shaders.put("default", solidShader);
        configNode.put("texturepack", "standard");
        DynmapBlockStateHelper.initialise();
        HDBlockModels.loadModels(DynmapCore.INSTANCE, configNode);
        TexturePack.loadTextureMapping(DynmapCore.INSTANCE, configNode);
        // Force initialisation of texture pack to get early errors:
        TexturePack.getTexturePack(DynmapCore.INSTANCE, "standard");
        final TexturePackHDShader texturedShader = new TexturePackHDShader(DynmapCore.INSTANCE, configNode);
        shaders.put("textured", texturedShader);
        shaders.put("default", texturedShader);
        shaders.put("caves", new CaveHDShader(DynmapCore.INSTANCE, configNode));
        lightings.put("default", new DefaultHDLighting(DynmapCore.INSTANCE, configNode));
    }
}