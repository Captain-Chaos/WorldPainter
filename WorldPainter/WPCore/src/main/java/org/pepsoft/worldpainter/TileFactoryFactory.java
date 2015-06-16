/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.DisplacementHeightMap;
import org.pepsoft.worldpainter.heightMaps.MaximisingHeightMap;
import org.pepsoft.worldpainter.heightMaps.NinePatchHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.heightMaps.ProductHeightMap;
import org.pepsoft.worldpainter.heightMaps.SumHeightMap;
import org.pepsoft.worldpainter.themes.impl.fancy.FancyTheme;
import org.pepsoft.worldpainter.themes.SimpleTheme;

import java.util.Random;

/**
 *
 * @author pepijn
 */
public final class TileFactoryFactory {
    private TileFactoryFactory() {
        // Prevent instantiation
    }
    
    public static HeightMapTileFactory createNoiseTileFactory(long seed, Terrain terrain, int maxHeight, int baseHeight, int waterLevel, boolean floodWithLava, boolean beaches, float range, double scale) {
        return new HeightMapTileFactory(seed, new SumHeightMap(new ConstantHeightMap(baseHeight), new NoiseHeightMap(range, scale, 1, 0)), maxHeight, floodWithLava, SimpleTheme.createDefault(terrain, maxHeight, waterLevel, true, beaches));
    }
    
    public static HeightMapTileFactory createFlatTileFactory(long seed, Terrain terrain, int maxHeight, int height, int waterLevel, boolean floodWithLava, boolean beaches) {
        return new HeightMapTileFactory(seed, new ConstantHeightMap(height), maxHeight, floodWithLava, SimpleTheme.createDefault(terrain, maxHeight, waterLevel, false, beaches));
    }
    
    public static HeightMapTileFactory createFancyTileFactory(long seed, Terrain terrain, int maxHeight, int baseHeight, int waterLevel, boolean floodWithLava, float range, double scale) {
//        final HeightMapTileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(Terrain.GRASS, World2.DEFAULT_MAX_HEIGHT, 58, 62, false, true, 20.0f, 1.0);
        HeightMap oceanFloor = new ConstantHeightMap("Ocean Floor", waterLevel - 22);
        HeightMap continent;
//        continent = new NinePatchHeightMap(200, 100, 50, 58f);
        continent = new NinePatchHeightMap("Continent", 0, 500, 50, baseHeight - (waterLevel - 22));
        Random random = new Random(seed);
        HeightMap hills = new ProductHeightMap(
                "Hills",
                new NoiseHeightMap(1.0f, 10f, 1, random.nextLong()),
                new NoiseHeightMap(range, scale, 2, random.nextLong()));
//                new SumHeightMap(
//                    new NoiseHeightMap(range, scale, 2),
//                    new ConstantHeightMap(-5f)));
        continent = new SumHeightMap(
                new SumHeightMap(
                    oceanFloor,
                    continent),
                hills);
        HeightMap mountainsLimit = new NinePatchHeightMap(0, 500, 200, 1f);
        HeightMap mountainsHeight = new ProductHeightMap(
            new ProductHeightMap(
                new NoiseHeightMap(1.0f, 10f, 1, random.nextLong()),
                mountainsLimit),
            new NoiseHeightMap(256f, 5f, 5, random.nextLong()));
        HeightMap mountainsAngleMap = new NoiseHeightMap((float) (Math.PI * 2), 2.5, 1, random.nextLong());
        HeightMap mountainsDistanceMap = new NoiseHeightMap(25f, 2.5, 1, random.nextLong());
        HeightMap mountains = new DisplacementHeightMap("Mountains", mountainsHeight, mountainsAngleMap, mountainsDistanceMap);
        HeightMap heightMap = new MaximisingHeightMap(continent, mountains);
        return new HeightMapTileFactory(seed, heightMap, 256, false, new FancyTheme(maxHeight, 62, heightMap, terrain));
    }
}