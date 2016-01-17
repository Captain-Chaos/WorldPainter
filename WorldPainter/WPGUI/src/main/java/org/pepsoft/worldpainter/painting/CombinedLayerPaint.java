package org.pepsoft.worldpainter.painting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.CombinedLayer;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;

/**
 * Created by pepijn on 28-05-15.
 */
public final class CombinedLayerPaint extends LayerPaint {
    public CombinedLayerPaint(CombinedLayer layer) {
        super(layer);
    }

    @Override
    public void apply(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        if (brush.getRadius() == 0) {
            // Special case: if the radius is 0, assume that the user wants to paint complete pixels instead of trying
            // to apply the brush
            applyPixel(dimension, centreX, centreY);
            return;
        }
        final int effectiveRadius = brush.getEffectiveRadius();
        final int x1 = centreX - effectiveRadius, y1 = centreY - effectiveRadius, x2 = centreX + effectiveRadius, y2 = centreY + effectiveRadius;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
        final Terrain terrain = ((CombinedLayer) layer).getTerrain();
        final int biome = ((CombinedLayer) layer).getBiome();
        final boolean terrainConfigured = terrain != null;
        final boolean biomeConfigured = biome != -1;
        if ((tileX1 == tileX2) && (tileY1 == tileY2)) {
            // The bounding box of the brush is entirely on one tile; optimize by painting directly to the tile
            final Tile tile = dimension.getTileForEditing(tileX1, tileY1);
            if (tile == null) {
                return;
            }
            final int x1InTile = x1 & TILE_SIZE_MASK, y1InTile = y1 & TILE_SIZE_MASK, x2InTile = x2 & TILE_SIZE_MASK, y2InTile = y2 & TILE_SIZE_MASK;
            final int tileXInWorld = tileX1 << TILE_SIZE_BITS, tileYInWorld = tileY1 << TILE_SIZE_BITS;
            if (dither) {
                for (int y = y1InTile; y <= y2InTile; y++) {
                    for (int x = x1InTile; x <= x2InTile; x++) {
                        final int currentValue = tile.getLayerValue(layer, x, y);
                        final float strength = dynamicLevel * getStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if (strength != 0f) {
                            int targetValue = 1 + (int) (strength * 14 + 0.5f);
                            if (targetValue > currentValue) {
                                tile.setLayerValue(layer, x, y, targetValue);
                            }
                            if (terrainConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                tile.setTerrain(x, y, terrain);
                            }
                            if (biomeConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, biome);
                            }
                        }
                    }
                }
            } else {
                for (int y = y1InTile; y <= y2InTile; y++) {
                    for (int x = x1InTile; x <= x2InTile; x++) {
                        final int currentValue = tile.getLayerValue(layer, x, y);
                        final float strength = dynamicLevel * getStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if (strength != 0f) {
                            int targetValue = 1 + (int) (strength * 14 + 0.5f);
                            if (targetValue > currentValue) {
                                tile.setLayerValue(layer, x, y, targetValue);
                            }
                            if (dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y) > 0.75f) {
                                if (terrainConfigured) {
                                    tile.setTerrain(x, y, terrain);
                                }
                                if (biomeConfigured) {
                                    tile.setLayerValue(Biome.INSTANCE, x, y, biome);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            if (dither) {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final int currentValue = dimension.getLayerValueAt(layer, x, y);
                        final float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
                            int targetValue = 1 + (int) (strength * 14 + 0.5f);
                            if (targetValue > currentValue) {
                                dimension.setLayerValueAt(layer, x, y, targetValue);
                            }
                            if (terrainConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                dimension.setTerrainAt(x, y, terrain);
                            }
                            if (biomeConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                dimension.setLayerValueAt(Biome.INSTANCE, x, y, biome);
                            }
                        }
                    }
                }
            } else {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final int currentValue = dimension.getLayerValueAt(layer, x, y);
                        final float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
                            int targetValue = 1 + (int) (strength * 14 + 0.5f);
                            if (targetValue > currentValue) {
                                dimension.setLayerValueAt(layer, x, y, targetValue);
                            }
                            if (dynamicLevel * getFullStrength(centreX, centreY, x, y) > 0.75f) {
                                if (terrainConfigured) {
                                    dimension.setTerrainAt(x, y, terrain);
                                }
                                if (biomeConfigured) {
                                    dimension.setLayerValueAt(Biome.INSTANCE, x, y, biome);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void remove(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        if (brush.getRadius() == 0) {
            // Special case: if the radius is 0, assume that the user wants to remove complete pixels instead of trying
            // to apply the brush
            removePixel(dimension, centreX, centreY);
            return;
        }
        final int effectiveRadius = brush.getEffectiveRadius();
        final int x1 = centreX - effectiveRadius, y1 = centreY - effectiveRadius, x2 = centreX + effectiveRadius, y2 = centreY + effectiveRadius;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
        final boolean terrainConfigured = ((CombinedLayer) layer).getTerrain() != null;
        final boolean biomeConfigured = ((CombinedLayer) layer).getBiome() != -1;
        if ((tileX1 == tileX2) && (tileY1 == tileY2) && (! terrainConfigured)) {
            // The bounding box of the brush is entirely on one tile; optimize by painting directly to the tile
            final Tile tile = dimension.getTileForEditing(tileX1, tileY1);
            if (tile == null) {
                return;
            }
            final int x1InTile = x1 & TILE_SIZE_MASK, y1InTile = y1 & TILE_SIZE_MASK, x2InTile = x2 & TILE_SIZE_MASK, y2InTile = y2 & TILE_SIZE_MASK;
            final int tileXInWorld = tileX1 << TILE_SIZE_BITS, tileYInWorld = tileY1 << TILE_SIZE_BITS;
            if (dither) {
                for (int y = y1InTile; y <= y2InTile; y++) {
                    for (int x = x1InTile; x <= x2InTile; x++) {
                        final int currentValue = tile.getLayerValue(layer, x, y);
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if (strength != 0f) {
                            int targetValue = 14 - (int) (strength * 14 + 0.5f);
                            if (targetValue < currentValue) {
                                tile.setLayerValue(layer, x, y, targetValue);
                            }
                            if (biomeConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                            }
                        }
                    }
                }
            } else {
                for (int y = y1InTile; y <= y2InTile; y++) {
                    for (int x = x1InTile; x <= x2InTile; x++) {
                        final int currentValue = tile.getLayerValue(layer, x, y);
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if (strength != 0f) {
                            int targetValue = 14 - (int) (strength * 14 + 0.5f);
                            if (targetValue < currentValue) {
                                tile.setLayerValue(layer, x, y, targetValue);
                            }
                            if (biomeConfigured && (strength > 0.75f)) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                            }
                        }
                    }
                }
            }
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            if (dither) {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final int currentValue = dimension.getLayerValueAt(layer, x, y);
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
                            int targetValue = 14 - (int) (strength * 14 + 0.5f);
                            if (targetValue < currentValue) {
                                dimension.setLayerValueAt(layer, x, y, targetValue);
                            }
                            if (terrainConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                dimension.applyTheme(x, y);
                            }
                            if (biomeConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
                            }
                        }
                    }
                }
            } else {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final int currentValue = dimension.getLayerValueAt(layer, x, y);
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
                            int targetValue = 14 - (int) (strength * 14 + 0.5f);
                            if (targetValue < currentValue) {
                                dimension.setLayerValueAt(layer, x, y, targetValue);
                            }
                            if (strength > 0.75f) {
                                if (terrainConfigured) {
                                    dimension.applyTheme(x, y);
                                }
                                if (biomeConfigured) {
                                    dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void applyPixel(Dimension dimension, int x, int y) {
        final Tile tile = dimension.getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
            final int value = 1 + (int) (brush.getLevel() * 14 + 0.5f);
            if (tile.getLayerValue(layer, xInTile, yInTile) < value) {
                tile.setLayerValue(layer, xInTile, yInTile, value);
            }
            final Terrain terrain = ((CombinedLayer) layer).getTerrain();
            if (terrain != null) {
                tile.setTerrain(xInTile, yInTile, terrain);
            }
            final int biome = ((CombinedLayer) layer).getBiome();
            if (biome != -1) {
                tile.setLayerValue(Biome.INSTANCE, xInTile, yInTile, biome);
            }
        }
    }

    @Override
    public void removePixel(Dimension dimension, int x, int y) {
        if (((CombinedLayer) layer).getTerrain() != null) {
            // applyTheme() only exists at Dimension-level, so go via the dimension
            dimension.setLayerValueAt(layer, x, y, 0);
            dimension.applyTheme(x, y);
            if (((CombinedLayer) layer).getBiome() != -1) {
                dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
            }
        } else {
            final Tile tile = dimension.getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
            if (tile != null) {
                final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
                tile.setLayerValue(layer, xInTile, yInTile, 0);
                if (((CombinedLayer) layer).getBiome() != -1) {
                    tile.setLayerValue(Biome.INSTANCE, xInTile, yInTile, 255);
                }
            }
        }
    }
}