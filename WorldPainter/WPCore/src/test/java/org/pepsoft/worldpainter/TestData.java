package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;

import java.awt.*;

import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.Terrain.GRASS;

public final class TestData {
    private TestData() {
        // Prevent instantiation
    }

    public static TileFactory createTileFactory(int terrainHeight) {
        return new HeightMapTileFactory(SEED, new ConstantHeightMap(terrainHeight), MIN_HEIGHT, MAX_HEIGHT, false, THEME);
    }

    public static Dimension createDimension(Rectangle area, int terrainHeight) {
        final TileFactory tileFactory = createTileFactory(terrainHeight);
        final Dimension dimension = new Dimension(WORLD, "Surface", SEED, tileFactory, NORMAL_DETAIL);
        final int tileX1 = area.x / TILE_SIZE, tileX2 = (area.x + area.width - 1) / TILE_SIZE, tileY1 = area.y / TILE_SIZE, tileY2 = (area.y + area.height - 1) / TILE_SIZE;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                dimension.addTile(tileFactory.createTile(tileX, tileY));
            }
        }
        return dimension;
    }

    /**
     * Create an in-memory {@link MinecraftWorld} with the specified {@code area} and filled with blocks up to and
     * including {@code terrainHeight}, consisting of one layer of bedrock, up to 63 layers of deepslate, as many layers
     * of stone as required, three layers of dirt and one layer of grass block.
     */
    public static MinecraftWorld createMinecraftWorld(Rectangle area, int terrainHeight, Material terrainMaterial) {
        final Box volume = new Box(area.x, area.width, area.y, area.height, MIN_HEIGHT, MAX_HEIGHT);
        final Material[] lowestBlocks = new Material[terrainHeight - MIN_HEIGHT + 1];
        for (int z = 0; z <= terrainHeight - MIN_HEIGHT; z++) {
            if (z >= terrainHeight - MIN_HEIGHT) {
                lowestBlocks[z] = terrainMaterial;
            } else if (z == 0) {
                lowestBlocks[z] = BEDROCK;
            } else if (z >= terrainHeight - MIN_HEIGHT - 3) {
                lowestBlocks[z] = DIRT;
            } else if (z >= -MIN_HEIGHT) {
                lowestBlocks[z] = STONE;
            } else {
                lowestBlocks[z] = DEEPSLATE_Y;
            }
        }
        return new MinecraftWorldObject("Test", volume, MAX_HEIGHT, lowestBlocks, null);
    }

    public static final Platform PLATFORM = DefaultPlugin.JAVA_ANVIL_1_18;

    public static final int MIN_HEIGHT = PLATFORM.minZ;

    public static final int MAX_HEIGHT = PLATFORM.standardMaxHeight;

    public static final World2 WORLD = new World2(PLATFORM, MAX_HEIGHT);

    public static final long SEED = 0L;

    public static final Theme THEME = SimpleTheme.createSingleTerrain(GRASS, MIN_HEIGHT, MAX_HEIGHT, 62);
}