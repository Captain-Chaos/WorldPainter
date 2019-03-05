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

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;
import org.pepsoft.worldpainter.heightMaps.TransformingHeightMap;
import org.pepsoft.worldpainter.importing.HeightMapImporter;
import org.pepsoft.worldpainter.themes.Theme;

import java.util.Random;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;

/**
 *
 * @author SchmitzP
 */
public class ImportHeightMapOp extends AbstractOperation<World2> {
    protected ImportHeightMapOp(ScriptingContext context) {
        super(context);
    }

    public ImportHeightMapOp fromHeightMap(HeightMap heightMap) {
        this.heightMap = (BitmapHeightMap) heightMap;
        return this;
    }

    public ImportHeightMapOp scale(int scale) {
        this.scale = scale;
        return this;
    }

    public ImportHeightMapOp shift(int x, int y) {
        offsetX = x;
        offsetY = y;
        return this;
    }
    
    public ImportHeightMapOp fromLevels(int imageLow, int imageHigh) throws ScriptException {
        if (fromLevelsSpecified) {
            throw new ScriptException("Only one mapped range supported, for now");
        }
        importer.setImageLowLevel(imageLow);
        importer.setImageHighLevel(imageHigh);
        fromLevelsSpecified = true;
        return this;
    }
    
    public ImportHeightMapOp toLevels(int worldLow, int worldHigh) throws ScriptException {
        if (toLevelsSpecified) {
            throw new ScriptException("Only one mapped range supported, for now");
        }
        importer.setWorldLowLevel(worldLow);
        importer.setWorldHighLevel(worldHigh);
        toLevelsSpecified = true;
        return this;
    }

    @Override
    public World2 go() throws ScriptException {
        goCalled();

        if (heightMap == null) {
            throw new ScriptException("heightMap not set");
        }
        HeightMap adjustedHeightMap = heightMap;
        if ((scale != 100) || (offsetX != 0) || (offsetY != 0)) {
            if (scale != 100) {
                heightMap.setSmoothScaling(true);
            }
            adjustedHeightMap = new TransformingHeightMap(heightMap.getName() + " transformed", heightMap, scale, scale, offsetX, offsetY, 0);
        }
        importer.setHeightMap(adjustedHeightMap);
        importer.setImageFile(heightMap.getImageFile());
        HeightMapTileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), Terrain.GRASS, DEFAULT_MAX_HEIGHT_2, 58, 62, false, true, 20, 1.0);
        Theme defaults = Configuration.getInstance().getHeightMapDefaultTheme();
        if (defaults != null) {
            tileFactory.setTheme(defaults);
        }
        importer.setTileFactory(tileFactory);
        String name = heightMap.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        importer.setName(name);
        // TODO autoselect this and make it configurable:
        importer.setPlatform(JAVA_ANVIL);
        try {
            return importer.importToNewWorld(null);
        } catch (ProgressReceiver.OperationCancelled e) {
            // Can never happen since we don't pass a progress receiver in
            throw new InternalError();
        }
    }
    
    private final HeightMapImporter importer = new HeightMapImporter();
    private BitmapHeightMap heightMap;
    private boolean fromLevelsSpecified, toLevelsSpecified;
    private int scale = 100, offsetX, offsetY;
}