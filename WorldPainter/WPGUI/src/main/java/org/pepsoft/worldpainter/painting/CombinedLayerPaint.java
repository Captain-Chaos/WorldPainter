package org.pepsoft.worldpainter.painting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.CombinedLayer;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;

/**
 * Created by pepijn on 28-05-15.
 */
public final class CombinedLayerPaint extends NibbleLayerPaint {
    public CombinedLayerPaint(CombinedLayer layer) {
        super(layer);
    }

    @Override
    public void apply(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        super.apply(dimension, centreX, centreY, dynamicLevel);
        final CombinedLayer combinedLayer = (CombinedLayer) layer;
        if (combinedLayer.isApplyTerrainAndBiomeOnExport() || (brush.getRadius() == 0)) {
            return;
        }
        final Terrain terrain = combinedLayer.getTerrain();
        final int biome = combinedLayer.getBiome();
        final boolean terrainConfigured = terrain != null;
        final boolean biomeConfigured = biome != -1;
        if ((! terrainConfigured) && (! biomeConfigured)) {
            return;
        }
        final Rectangle boundingBox = brush.getBoundingBox();
        final int x1 = centreX + boundingBox.x, y1 = centreY + boundingBox.y, x2 = x1 + boundingBox.width - 1, y2 = y1 + boundingBox.height - 1;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
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
                        final float strength = dynamicLevel * getStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if (strength != 0f) {
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
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            if (dither) {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
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

    @Override
    public void remove(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        super.remove(dimension, centreX, centreY, dynamicLevel);
        final CombinedLayer combinedLayer = (CombinedLayer) layer;
        if (combinedLayer.isApplyTerrainAndBiomeOnExport() || (brush.getRadius() == 0)) {
            return;
        }
        final boolean terrainConfigured = combinedLayer.getTerrain() != null;
        final boolean biomeConfigured = combinedLayer.getBiome() != -1;
        if ((! terrainConfigured) && (! biomeConfigured)) {
            return;
        }
        final Rectangle boundingBox = brush.getBoundingBox();
        final int x1 = centreX + boundingBox.x, y1 = centreY + boundingBox.y, x2 = x1 + boundingBox.width - 1, y2 = y1 + boundingBox.height - 1;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
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
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                        if ((strength != 0f) && ((strength > 0.95f) || (Math.random() < strength))) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                        }
                    }
                }
            } else {
                for (int y = y1InTile; y <= y2InTile; y++) {
                    for (int x = x1InTile; x <= x2InTile; x++) {
                        if (dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y) > 0.75f) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                        }
                    }
                }
            }
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            if (dither) {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        final float strength = dynamicLevel * getFullStrength(centreX, centreY, x, y);
                        if (strength != 0f) {
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
                        if (dynamicLevel * getFullStrength(centreX, centreY, x, y) > 0.75f) {
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

    @Override
    public void applyPixel(Dimension dimension, int x, int y) {
        super.applyPixel(dimension, x, y);;
        final CombinedLayer combinedLayer = (CombinedLayer) layer;
        final Terrain terrain = combinedLayer.getTerrain();
        final int biome = combinedLayer.getBiome();
        final boolean terrainConfigured = terrain != null;
        final boolean biomeConfigured = biome != -1;
        if ((! combinedLayer.isApplyTerrainAndBiomeOnExport()) && (terrainConfigured || biomeConfigured)) {
            final Tile tile = dimension.getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
            if (tile != null) {
                final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
                if (terrainConfigured) {
                    tile.setTerrain(xInTile, yInTile, terrain);
                }
                if (biomeConfigured) {
                    tile.setLayerValue(Biome.INSTANCE, xInTile, yInTile, biome);
                }
            }
        }
    }

    @Override
    public void removePixel(Dimension dimension, int x, int y) {
        super.removePixel(dimension, x, y);
        final CombinedLayer combinedLayer = (CombinedLayer) layer;
        final boolean terrainConfigured = combinedLayer.getTerrain() != null;
        final boolean biomeConfigured = combinedLayer.getBiome() != -1;
        if ((! combinedLayer.isApplyTerrainAndBiomeOnExport()) && (terrainConfigured || biomeConfigured)) {
            if (terrainConfigured) {
                // applyTheme() only exists at Dimension-level, so go via the dimension
                dimension.applyTheme(x, y);
                if (biomeConfigured) {
                    dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
                }
            } else {
                final Tile tile = dimension.getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
                if (tile != null) {
                    tile.setLayerValue(Biome.INSTANCE, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, 255);
                }
            }
        }
    }
}