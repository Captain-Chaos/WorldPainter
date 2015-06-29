/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author SchmitzP
 */
public class GetLayerOp extends AbstractOperation<Layer> {
    protected GetLayerOp(ScriptingContext context) {
        super(context);
    }

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
        goCalled();

        if (fileName != null) {
            File file = sanityCheckFileName(fileName);
            try {
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                    return (Layer) in.readObject();
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