package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Caves;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.GeometryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

/**
 * Created by Pepijn on 15-1-2017.
 */
public class CavesExporter extends AbstractLayerExporter<Caves> implements SecondPassLayerExporter {
    public CavesExporter() {
        super(Caves.INSTANCE, new CavesSettings());
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        CavesSettings settings = (CavesSettings) getSettings();
        int minZ = Math.max(settings.getMinimumLevel(), dimension.isBottomless() ? 0 : 1),
                maxZForWorld = Math.min(settings.getMaximumLevel(), minecraftWorld.getMaxHeight() - 1);
        boolean surfaceBreaking = settings.isSurfaceBreaking();
        Random random = new Random();
        CaveSettings caveSettings = new CaveSettings();
        caveSettings.minZ = minZ;
        // Grow the area we will check for spawning caves, such that parts of
        // caves which start outside the exported area are still rendered inside
        // the exported area
        Rectangle spawnArea = (Rectangle) exportedArea.clone();
        spawnArea.grow(MAX_CAVE_LENGTH, MAX_CAVE_LENGTH);
        // Go tile by tile, so we can quickly check whether the tile even
        // exists and contains the layer and if not skip it entirely
        int tileX1 = spawnArea.x >> TILE_SIZE_BITS, tileX2 = (spawnArea.x + spawnArea.width - 1) >> TILE_SIZE_BITS;
        int tileY1 = spawnArea.y >> TILE_SIZE_BITS, tileY2 = (spawnArea.y + spawnArea.height - 1) >> TILE_SIZE_BITS;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                Tile tile = dimension.getTile(tileX, tileY);
                if ((tile == null) || (! tile.hasLayer(Caves.INSTANCE))) {
                    continue;
                }
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                        int x = (tileX << TILE_SIZE_BITS) | xInTile, y = (tileY << TILE_SIZE_BITS) | yInTile;
                        int value = tile.getLayerValue(Caves.INSTANCE, xInTile, yInTile);
                        if (value > 0) {
                            int height = tile.getIntHeight(xInTile, yInTile);
                            int maxZ = Math.min(maxZForWorld, height - (surfaceBreaking ? 0 : dimension.getTopLayerDepth(x, y, height)));
                            random.setSeed(dimension.getSeed() + x * 65537 + y);
                            for (int z = minZ; z <= maxZ; z++) {
                                if (value > random.nextInt(CAVE_CHANCE)) {
                                    caveSettings.start = new Point3i(x, y, z);
                                    caveSettings.length = MathUtils.clamp(0, (int) ((random.nextGaussian() + 2.0) * (MAX_CAVE_LENGTH / 3.0) + 0.5), MAX_CAVE_LENGTH);
                                    createTunnel(minecraftWorld, dimension, new Random(random.nextLong()), caveSettings);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void createTunnel(MinecraftWorld world, Dimension dimension, Random random, CaveSettings settings) {
        Point3d location = new Point3d(settings.start.x, settings.start.y, settings.start.z);
        Vector3d direction = getRandomDirection(random);
        double length = 0.0, minRadius = settings.minRadius, maxRadius = settings.maxRadius,
                radius = (maxRadius + minRadius) / 2.0, radiusDelta = 0.0, radiusChangeSpeed = settings.radiusChangeSpeed;
        int maxLength = settings.length, twistiness = settings.twistiness;
        if (logger.isTraceEnabled()) {
            logger.trace("Creating tunnel @ {},{},{} of length {}; radius: {} - {} (variability: {}); twistiness: {}",
                    settings.start.x, settings.start.y, settings.start.z, maxLength, settings.minRadius, settings.maxRadius,
                    radiusChangeSpeed, twistiness);
        }
        while (length < maxLength) {
            if (dimension.getLayerValueAt(Caves.INSTANCE, (int) location.x, (int) location.y) < 1) {
                // Don't stray into areas where the layer isn't present at all
                return;
            }
            excavate(world, dimension, random, settings, location, radius);
            length += direction.length();
            location.add(direction);
            Vector3d dirChange = getRandomDirection(random);
            dirChange.scale(random.nextDouble() / (5 - twistiness));
            direction.add(dirChange);
            direction.normalize();
            if (radiusChangeSpeed > 0.0) {
                radius = MathUtils.clamp(minRadius, radius + radiusDelta, maxRadius);
                radiusDelta += random.nextDouble() * 2 * radiusChangeSpeed - radiusChangeSpeed;
            }
        }
    }

    private void excavate(MinecraftWorld world, Dimension dimension, Random random, CaveSettings settings, Point3d location, double radius) {
        boolean intrudingStone = settings.intrudingStone, roughWalls = settings.roughWalls, removeFloatingBlocks = settings.removeFloatingBlocks;
        int minZ = settings.minZ;
        // TODO: change visitFilledSphere so the sphere doesn't have single-block spikes at the x, y, and z axes
        GeometryUtil.visitFilledSphere((int) Math.ceil(radius), ((dx, dy, dz, d) -> {
            if (d > radius) {
                return true;
            }
            int z = (int) (location.z + dz);
            // TODO: efficiently check maxZ per x,y:
            if (z >= minZ) {
                int x = (int) (location.x + dx);
                int y = (int) (location.y + dy);
                int maxZ = dimension.getIntHeightAt(x, y);
                if (z > maxZ) {
                    return true;
                }
                int existingBlock = world.getBlockTypeAt(x, y, z);
                if (existingBlock == BLK_AIR) {
                    // Already excavated
                    return true;
                }
                boolean blockExcavated = false;
                if ((roughWalls || intrudingStone) && (radius - d <= 1)) {
                    // Remember: this is not near the wall of the tunnel; it is
                    // near the edge of the sphere we're currently excavating,
                    // so only remove things, don't add them
                    if (intrudingStone) {
                        if (((existingBlock != BLK_STONE)
                                    || (world.getDataAt(x, y, z) == DATA_STONE_STONE))
                                && ((! roughWalls)
                                    || random.nextBoolean())) {
                            // Treat andesite, etc. as "harder" than regular stone
                            // so it protrudes slightly into the cave
                            world.setMaterialAt(x, y, z, Material.AIR);
                            blockExcavated = true;
                        }
                    } else if (random.nextBoolean()) {
                        world.setMaterialAt(x, y, z, Material.AIR);
                        blockExcavated = true;
                    }
                } else {
                    world.setMaterialAt(x, y, z, Material.AIR);
                    blockExcavated = true;
                }
                if (blockExcavated && removeFloatingBlocks && (radius - d <= 2)) {
                    checkForFloatingBlock(world, x - 1, y, z, maxZ);
                    checkForFloatingBlock(world, x, y - 1, z, maxZ);
                    checkForFloatingBlock(world, x + 1, y, z, maxZ);
                    checkForFloatingBlock(world, x, y + 1, z, maxZ);
                    if (z > 1) {
                        checkForFloatingBlock(world, x, y, z - 1, maxZ);
                    }
                    if (z < maxZ) {
                        checkForFloatingBlock(world, x, y, z + 1, maxZ);
                    }
                }
            }
            return true;
        }));
    }

    static void checkForFloatingBlock(MinecraftWorld world, int x, int y, int z, int maxZ) {
        Material material = world.getMaterialAt(x, y, z);
        if (material.isNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_PODZOL, MC_FARMLAND, MC_GRASS_PATH, MC_SAND, MC_RED_SAND, MC_GRAVEL)) {
            if (((z > 0) && (!world.getMaterialAt(x, y, z - 1).solid))
                    && ((z < maxZ) && (!world.getMaterialAt(x, y, z + 1).solid))) {
                // The block is only one layer thick
                world.setMaterialAt(x, y, z, Material.AIR);
                // TODO: this isn't removing nearly all one-block thick dirt. Why?
            }
        } else if (material.isNotNamedOneOf(MC_AIR, MC_WATER, MC_LAVA)) {
            if ((! world.getMaterialAt(x - 1, y, z).solid)
                    && (! world.getMaterialAt(x, y - 1, z).solid)
                    && (! world.getMaterialAt(x + 1, y, z).solid)
                    && (! world.getMaterialAt(x, y + 1, z).solid)
                    && ((z > 0) && (! world.getMaterialAt(x, y, z - 1).solid))
                    && ((z < maxZ) && (! world.getMaterialAt(x, y, z + 1).solid))) {
                // The block is floating in the air
                // TODO: this does not take leaves into account, which count as an insubstantial block but can be attached to other leaves!
                world.setMaterialAt(x, y, z, Material.AIR);
            }
        }
    }

    private Vector3d getRandomDirection(Random random) {
        double x1 = random.nextDouble() * 2 - 1, x2 = random.nextDouble() * 2 - 1;
        while (x1 * x1 + x2 * x2 >= 1) {
            x1 = random.nextDouble() * 2 - 1;
            x2 = random.nextDouble() * 2 - 1;
        }
        return new Vector3d(2 * x1 * Math.sqrt(1 - x1 * x1 - x2 * x2),
                2 * x2 * Math.sqrt(1 - x1 * x1 - x2 * x2),
                1 - 2 * (x1 * x1 + x2 * x2));
    }

    private static final int MAX_CAVE_LENGTH = 128;
    private static final int CAVE_CHANCE = 131072;

    private static final Logger logger = LoggerFactory.getLogger(CavesExporter.class);

    /**
     * Settings for an individual cave.
     */
    static class CaveSettings {
        Point3i start;
        int length, minZ, minRadius = 2, maxRadius = 4, twistiness = 2;
        boolean intrudingStone = true, roughWalls, removeFloatingBlocks = true;
        double radiusChangeSpeed = 0.2;
    }

    /**
     * Settings for the Caves layer.
     */
    public static class CavesSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return cavesEverywhereLevel > 0;
        }

        @Override
        public Layer getLayer() {
            return Caves.INSTANCE;
        }

        @Override
        public ExporterSettings clone() {
            try {
                return (ExporterSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public int getWaterLevel() {
            return waterLevel;
        }

        public void setWaterLevel(int waterLevel) {
            this.waterLevel = waterLevel;
        }

        public int getCavesEverywhereLevel() {
            return cavesEverywhereLevel;
        }

        public void setCavesEverywhereLevel(int cavesEverywhereLevel) {
            this.cavesEverywhereLevel = cavesEverywhereLevel;
        }

        public boolean isFloodWithLava() {
            return floodWithLava;
        }

        public void setFloodWithLava(boolean floodWithLava) {
            this.floodWithLava = floodWithLava;
        }

        public boolean isSurfaceBreaking() {
            return surfaceBreaking;
        }

        public void setSurfaceBreaking(boolean surfaceBreaking) {
            this.surfaceBreaking = surfaceBreaking;
        }

        public boolean isLeaveWater() {
            return leaveWater;
        }

        public void setLeaveWater(boolean leaveWater) {
            this.leaveWater = leaveWater;
        }

        public int getMinimumLevel() {
            return minimumLevel;
        }

        public void setMinimumLevel(int minimumLevel) {
            this.minimumLevel = minimumLevel;
        }

        public int getMaximumLevel() {
            return maximumLevel;
        }

        public void setMaximumLevel(int maximumLevel) {
            this.maximumLevel = maximumLevel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CavesSettings that = (CavesSettings) o;

            if (waterLevel != that.waterLevel) return false;
            if (cavesEverywhereLevel != that.cavesEverywhereLevel) return false;
            if (floodWithLava != that.floodWithLava) return false;
            if (surfaceBreaking != that.surfaceBreaking) return false;
            if (leaveWater != that.leaveWater) return false;
            if (minimumLevel != that.minimumLevel) return false;
            return maximumLevel == that.maximumLevel;
        }

        @Override
        public int hashCode() {
            int result = waterLevel;
            result = 31 * result + cavesEverywhereLevel;
            result = 31 * result + (floodWithLava ? 1 : 0);
            result = 31 * result + (surfaceBreaking ? 1 : 0);
            result = 31 * result + (leaveWater ? 1 : 0);
            result = 31 * result + minimumLevel;
            result = 31 * result + maximumLevel;
            return result;
        }

        private int waterLevel, cavesEverywhereLevel;
        private boolean floodWithLava, surfaceBreaking = true, leaveWater = true;
        private int minimumLevel = 8, maximumLevel = Integer.MAX_VALUE;

        private static final long serialVersionUID = 1L;
    }
}
