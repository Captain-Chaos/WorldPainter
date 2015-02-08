/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author SchmitzP
 */
public class GetLayerOp extends AbstractOperation<Layer> {
    public GetLayerOp fromFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public GetLayerOp fromWorld(World2 world) {
        this.world = world;
        return this;
    }
    
    public GetLayerOp withName(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public Layer go() throws ScriptException {
        if (fileName != null) {
            File file = sanityCheckFileName(fileName);
            try {
                ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
                try {
                    return (Layer) in.readObject();
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new ScriptException("I/O error while loading layer " + fileName, e);
            } catch (ClassCastException e) {
                throw new ScriptException(fileName + " is not a WorldPainter custom layer file", e);
            } catch (ClassNotFoundException e) {
                throw new ScriptException("Class not found exception while loading layer " + fileName + " (not a WorldPainter custom layer?)", e);
            }
        } else if (world != null) {
            if ((name == null) || name.isEmpty()) {
                throw new ScriptException("name not set");
            }
            for (Dimension dimension: world.getDimensions()) {
                for (Layer layer: dimension.getAllLayers(false)) {
                    if (layer.getName().equals(name)) {
                        return layer;
                    }
                }
            }
            throw new ScriptException("World contains no layer named \"" + name + "\"");
        } else {
            if ((name == null) || name.isEmpty()) {
                throw new ScriptException("name not set");
            }
            
            // Special cases
            if (name.equals("Biomes")) {
                return Biome.INSTANCE;
            } else if (name.equals("Annotations")) {
                return Annotations.INSTANCE;
            }
            
            List<LayerProvider> layerProviders = WPPluginManager.getInstance().getPlugins(LayerProvider.class);
            for (LayerProvider layerProvider: layerProviders) {
                for (Layer layer: layerProvider.getLayers()) {
                    if (layer.getName().equals(name)) {
                        return layer;
                    }
                }
            }
            throw new ScriptException("No default layer named \"" + name + "\" exists and no world specified");
        }
    }

    private World2 world;
    private String name, fileName;
}