/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class DefaultPlugin extends AbstractPlugin implements LayerProvider, ContextProvider, WPContext {
    public DefaultPlugin() {
        super("Default", Version.VERSION);
    }
    
    // LayerProvider
    
    @Override
    public List<Layer> getLayers() {
        return Arrays.asList(Frost.INSTANCE, /*Caves.INSTANCE,*/ Caverns.INSTANCE, Chasms.INSTANCE, DeciduousForest.INSTANCE, PineForest.INSTANCE, SwampLand.INSTANCE, Jungle.INSTANCE, org.pepsoft.worldpainter.layers.Void.INSTANCE, Resources.INSTANCE/*, River.INSTANCE*/);
    }

    // ContextProvider

    @Override
    public WPContext getWPContextInstance() {
        return this;
    }
    
    // WPContext
    
    @Override
    public EventLogger getStatisticsRecorder() {
        return Configuration.getInstance();
    }

    @Override
    public MinecraftJarProvider getMinecraftJarProvider() {
        return Configuration.getInstance();
    }
}