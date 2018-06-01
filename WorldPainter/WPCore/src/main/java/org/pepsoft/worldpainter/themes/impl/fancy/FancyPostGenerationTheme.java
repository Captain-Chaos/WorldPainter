package org.pepsoft.worldpainter.themes.impl.fancy;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightMapTileFactory;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_2BiomeScheme;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;

public class FancyPostGenerationTheme extends FancyTheme {
    public FancyPostGenerationTheme(Dimension dimension, Terrain baseTerrain) {
        super(dimension.getMaxHeight(), ((HeightMapTileFactory) dimension.getTileFactory()).getWaterHeight(), null, baseTerrain);
        this.dimension = dimension;
    }

    @Override
    protected float getHeight(int x, int y) {
        return dimension.getHeightAt(x, y);
    }

    public void install() {
        snowLayer = null;
        for (CustomLayer layer: dimension.getCustomLayers()) {
            if ((layer instanceof GroundCoverLayer) && layer.getName().equals("Mountain Snow")) {
                snowLayer = (GroundCoverLayer) layer;
                break;
            }
        }
        if (snowLayer == null) {
            snowLayer = new GroundCoverLayer("Mountain Snow", MixedMaterial.create(Constants.BLK_SNOW), 0xffffff);
            snowLayer.setThickness(5);
            snowLayer.setEdgeWidth(15);
            snowLayer.setEdgeShape(GroundCoverLayer.EdgeShape.SMOOTH);
            dimension.getCustomLayers().add(snowLayer);
        }

        terrainDirtAndGravel = null;
        for (Terrain terrain: Terrain.getConfiguredValues()) {
            if (terrain.getName().equals("Mountain Dirt/Gravel")) {
                terrainDirtAndGravel = terrain;
                break;
            }
        }
        if (terrainDirtAndGravel == null) {
            MixedMaterial material = new MixedMaterial("Mountain Dirt/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.DIRT, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f);
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                if (! Terrain.isCustomMaterialConfigured(i)) {
                    Terrain.setCustomMaterial(i, material);
                    terrainDirtAndGravel = Terrain.getCustomTerrain(i);
                    break;
                }
            }
            if (terrainDirtAndGravel == null) {
                throw new IllegalStateException("No empty custom terrain slots available");
            }
        }

        terrainStoneAndGravel = null;
        for (Terrain terrain: Terrain.getConfiguredValues()) {
            if (terrain.getName().equals("Mountain Stone/Gravel")) {
                terrainStoneAndGravel = terrain;
                break;
            }
        }
        if (terrainStoneAndGravel == null) {
            MixedMaterial material = new MixedMaterial("Mountain Stone/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.STONE, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f);
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                if (! Terrain.isCustomMaterialConfigured(i)) {
                    Terrain.setCustomMaterial(i, material);
                    terrainStoneAndGravel = Terrain.getCustomTerrain(i);
                    break;
                }
            }
            if (terrainStoneAndGravel == null) {
                throw new IllegalStateException("No empty custom terrain slots available");
            }
        }
    }

    private final Dimension dimension;
}
