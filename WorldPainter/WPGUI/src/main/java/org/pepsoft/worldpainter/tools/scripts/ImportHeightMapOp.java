/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.util.Random;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.HeightMapTileFactory;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.TileFactoryFactory;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;
import org.pepsoft.worldpainter.importing.HeightMapImporter;
import org.pepsoft.worldpainter.themes.Theme;

/**
 *
 * @author SchmitzP
 */
public class ImportHeightMapOp extends AbstractOperation<World2> {
    public ImportHeightMapOp fromHeightMap(HeightMap heightMap) {
        this.heightMap = (BitmapHeightMap) heightMap;
        return this;
    }

    public ImportHeightMapOp scale(int scale) {
        importer.setScale(scale);
        return this;
    }

    public ImportHeightMapOp shift(int x, int y) {
        importer.setOffsetX(x);
        importer.setOffsetY(y);
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
        if (heightMap == null) {
            throw new ScriptException("heightMap not set");
        }
        importer.setImage(heightMap.getImage());
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
        try {
            return importer.doImport(null);
        } catch (ProgressReceiver.OperationCancelled e) {
            // Can never happen since we don't pass a progress receiver in
            throw new InternalError();
        }
    }
    
    private final HeightMapImporter importer = new HeightMapImporter();
    private BitmapHeightMap heightMap;
    private boolean fromLevelsSpecified, toLevelsSpecified;
}