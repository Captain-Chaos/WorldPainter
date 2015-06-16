/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes.impl.fancy;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.heightMaps.SumHeightMap;
import org.pepsoft.worldpainter.layers.DeciduousForest;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Jungle;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.layers.SwampLand;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;
import org.pepsoft.worldpainter.themes.Theme;

import java.util.Random;

/**
 *
 * @author SchmitzP
 */
public class FancyTheme implements Theme, Cloneable {
    public FancyTheme(int maxHeight, int waterHeight, HeightMap heightMap, Terrain baseTerrain) {
        setMaxHeight(maxHeight);
        setWaterHeight(waterHeight);
        setHeightMap(heightMap);
        setDesertMaxHeight(waterHeight + 20);
        Random random = new Random(heightMap.getSeed());
        setTemperatureMap(new SumHeightMap(new NoiseHeightMap(60f, 10.0, 2, random.nextLong()), new ConstantHeightMap(-20f)));
        setHumidityMap(new NoiseHeightMap(100f, 10.0, 2, random.nextLong()));
        setForestMap(new NoiseHeightMap(1f, 1.0, 3, random.nextLong()));
        setBaseTerrain(baseTerrain);
        snowLayer.setThickness(3);
        snowLayer.setEdgeWidth(3);
        snowLayer.setEdgeShape(GroundCoverLayer.EdgeShape.LINEAR);
    }

    @Override
    public void apply(Tile tile, int x, int y) {
        final int worldX = (tile.getX() << 7) | x, worldY = (tile.getY() << 7) | y;
        float temperature = temperatureMap.getHeight(worldX, worldY);
        float height = tile.getHeight(x, y);
        temperature = temperature - Math.max(height - waterHeight, 0) / 2f + randomNoiseMap.getHeight(worldX, worldY);
        float humidity = humidityMap.getHeight(worldX, worldY) + randomNoiseMap.getHeight(worldX, worldY);
        final float slopeNOSO = Math.abs(heightMap.getHeight(    worldX, worldY - 1) - heightMap.getHeight(    worldX, worldY + 1));
        final float slopeNWSE = Math.abs(heightMap.getHeight(worldX + 1, worldY - 1) - heightMap.getHeight(worldX - 1, worldY + 1));
        final float slopeEAWE = Math.abs(heightMap.getHeight(worldX + 1,     worldY) - heightMap.getHeight(worldX - 1,     worldY));
        final float slopeSENW = Math.abs(heightMap.getHeight(worldX + 1, worldY + 1) - heightMap.getHeight(worldX - 1, worldY - 1));
        final float slope = Math.max(Math.max(slopeNOSO, slopeNWSE), Math.max(slopeEAWE, slopeSENW));
        if (slope > 2f) {
            tile.setTerrain(x, y, TERRAIN_STONE_AND_GRAVEL);
        } else {
            if (slope > 1.5f) {
                tile.setTerrain(x, y, TERRAIN_DIRT_AND_GRAVEL);
            } else if (height < (waterHeight - 4)) {
                tile.setTerrain(x, y, Terrain.BEACHES);
            } else if ((height < (waterHeight + 2)) && isWaterNear(worldX, worldY)) {
                tile.setTerrain(x, y, Terrain.BEACHES);
                if ((temperature > 20) && (humidity > 55) && (slope < 0.75f) && (height < desertMaxHeight) && (forestMap.getHeight(worldX, worldY) > 0.35f)) {
                    tile.setLayerValue(Jungle.INSTANCE, x, y, 8);
                }
            } else if (temperature < -5) {
                tile.setTerrain(x, y, Terrain.BARE_GRASS);
            } else if (humidity < 40) {
                if ((slope < 0.75f) && (height < desertMaxHeight)) {
                    tile.setTerrain(x, y, Terrain.DESERT);
                } else {
                    tile.setTerrain(x, y, Terrain.SANDSTONE);
                }
            } else {
                tile.setTerrain(x, y, baseTerrain);
            }
            if ((height > (waterHeight - 4)) && (forestMap.getHeight(worldX, worldY) > 0.35f)) {
                if (temperature > 20) {
                    if (humidity > 55) {
                        if (height < waterHeight + 2) {
                            tile.setLayerValue(SwampLand.INSTANCE, x, y, 8);
                        } else {
                            tile.setLayerValue(Jungle.INSTANCE, x, y, 8);
                        }
                    } else if (humidity > 40) {
                        tile.setLayerValue(DeciduousForest.INSTANCE, x, y, 8);
                    }
                } else if (temperature > 10) {
                    if (humidity > 50) {
                        tile.setLayerValue(DeciduousForest.INSTANCE, x, y, 8);
                    }
                } else if (temperature > -20) {
                    if (humidity > 50) {
                        tile.setLayerValue(PineForest.INSTANCE, x, y, 8);
                    }
                }
            }
        }
        if (temperature < 0) {
            tile.setBitLayerValue(Frost.INSTANCE, x, y, true);
            if ((temperature < -10) && (humidity > 50) && (height > waterHeight) && (slope < 1.5f)) {
                tile.setBitLayerValue(snowLayer, x, y, true);
            }
        }
    }

    @Override
    public final int getWaterHeight() {
        return waterHeight;
    }

    @Override
    public final void setWaterHeight(int waterLevel) {
        this.waterHeight = waterLevel;
    }

    @Override
    public final int getMaxHeight() {
        return maxHeight;
    }

    public final void setMaxHeight(int maxHeight) {
        setMaxHeight(maxHeight, HeightTransform.IDENTITY);
    }

    @Override
    public final void setMaxHeight(int maxHeight, HeightTransform transform) {
        this.maxHeight = maxHeight;
    }

    @Override
    public final long getSeed() {
        return humidityMap.getSeed();
    }

    @Override
    public final void setSeed(long seed) {
        humidityMap.setSeed(seed);
        temperatureMap.setSeed(seed);
    }

    public final HeightMap getHumidityMap() {
        return humidityMap;
    }

    public final void setHumidityMap(HeightMap humidityMap) {
        this.humidityMap = humidityMap;
    }

    public final HeightMap getTemperatureMap() {
        return temperatureMap;
    }

    public final void setTemperatureMap(HeightMap temperatureMap) {
        this.temperatureMap = temperatureMap;
    }

    public final HeightMap getHeightMap() {
        return heightMap;
    }

    public final void setHeightMap(HeightMap heightMap) {
        this.heightMap = heightMap;
    }

    public final float getRockySlope() {
        return rockySlope;
    }

    public final void setRockySlope(float rockySlope) {
        this.rockySlope = rockySlope;
    }

    public final int getDesertMaxHeight() {
        return desertMaxHeight;
    }

    public final void setDesertMaxHeight(int desertMaxHeight) {
        this.desertMaxHeight = desertMaxHeight;
    }

    public final HeightMap getForestMap() {
        return forestMap;
    }

    public final void setForestMap(HeightMap forestMap) {
        this.forestMap = forestMap;
    }

    public final Terrain getBaseTerrain() {
        return baseTerrain;
    }

    public final void setBaseTerrain(Terrain baseTerrain) {
        this.baseTerrain = baseTerrain;
    }

    @Override
    public Theme clone() {
        try {
            FancyTheme clone = (FancyTheme) super.clone();
            clone.forestMap = forestMap.clone();
            clone.heightMap = heightMap.clone();
            clone.humidityMap = humidityMap.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWaterNear(int x, int y) {
        if (heightMap.getHeight(x, y) < waterHeight) {
            return true;
        }
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                if (heightMap.getHeight(x + dx, y + dy) < waterHeight) {
                    return true;
                }
            }
        }
        return false;
    }

    private int maxHeight, waterHeight, desertMaxHeight;
    /**
     * Humidity in %.
     */
    private HeightMap humidityMap;
    /**
     * Temperature in °C.
     */
    private HeightMap temperatureMap;
    private HeightMap heightMap;
    private float rockySlope = 1.5f;
    private HeightMap forestMap;
    private Terrain baseTerrain;
    private final HeightMap randomNoiseMap = new SumHeightMap(new ConstantHeightMap(-5f), new NoiseHeightMap(10f, 1.0, 3));
    private final GroundCoverLayer snowLayer = new GroundCoverLayer("Deep Snow", MixedMaterial.create(Constants.BLK_SNOW_BLOCK), 0xffffff);
    
    private static final Terrain TERRAIN_DIRT_AND_GRAVEL = Terrain.CUSTOM_1;
    private static final Terrain TERRAIN_STONE_AND_GRAVEL = Terrain.CUSTOM_2;
    private static final long serialVersionUID = 1L;
}