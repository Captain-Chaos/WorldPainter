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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.util.GeometryUtil.visitFilledAbsoluteSphere;

/**
 * Created by Pepijn on 15-1-2017.
 */
public class CavesExporter extends AbstractLayerExporter<Caves> implements SecondPassLayerExporter {
    public CavesExporter() {
        super(Caves.INSTANCE, new CavesSettings());
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        final CavesSettings settings = (CavesSettings) getSettings();
        final int minZ = Math.max(settings.getMinimumLevel(), dimension.isBottomless() ? 0 : 1),
                maxZForWorld = Math.min(settings.getMaximumLevel(), minecraftWorld.getMaxHeight() - 1),
                minimumLevel = settings.getCavesEverywhereLevel();
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final Random random = new Random();
        final CaveSettings caveSettings = new CaveSettings();
        caveSettings.minZ = minZ;
        caveSettings.waterLevel = settings.waterLevel;
        caveSettings.floodWithLava = settings.floodWithLava;
        // Grow the area we will check for spawning caves, such that parts of
        // caves which start outside the exported area are still rendered inside
        // the exported area
        final Rectangle spawnArea = (Rectangle) exportedArea.clone();
        spawnArea.grow((int) Math.ceil(MAX_CAVE_LENGTH + caveSettings.maxRadius), (int) Math.ceil(MAX_CAVE_LENGTH + caveSettings.maxRadius));
        // Go tile by tile, so we can quickly check whether the tile even
        // exists and contains the layer and if not skip it entirely
        final int tileX1 = spawnArea.x >> TILE_SIZE_BITS, tileX2 = (spawnArea.x + spawnArea.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = spawnArea.y >> TILE_SIZE_BITS, tileY2 = (spawnArea.y + spawnArea.height - 1) >> TILE_SIZE_BITS;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                final Tile tile = dimension.getTile(tileX, tileY);
                if ((tile == null) || ((minimumLevel == 0) && (! tile.hasLayer(Caves.INSTANCE)))) {
                    continue;
                }
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                        final int x = (tileX << TILE_SIZE_BITS) | xInTile, y = (tileY << TILE_SIZE_BITS) | yInTile;
                        final int cavesValue = Math.max(minimumLevel, tile.getLayerValue(Caves.INSTANCE, xInTile, yInTile));
                        if (cavesValue > 0) {
                            final int height = tile.getIntHeight(xInTile, yInTile);
                            final int maxZ = Math.min(maxZForWorld, height - (surfaceBreaking ? 0 : dimension.getTopLayerDepth(x, y, height)));
                            random.setSeed(dimension.getSeed() + x * 65537L + y);
                            for (int z = minZ; z <= maxZ; z++) {
                                if (cavesValue > random.nextInt(CAVE_CHANCE)) {
                                    caveSettings.start = new Point3i(x, y, z);
                                    caveSettings.length = MathUtils.clamp(0, (int) ((random.nextGaussian() + 2.0) * (MAX_CAVE_LENGTH / 3.0) + 0.5), MAX_CAVE_LENGTH);
                                    createTunnel(minecraftWorld, exportedArea, dimension, new Random(random.nextLong()), caveSettings, surfaceBreaking, minimumLevel);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void createTunnel(MinecraftWorld world, Rectangle exportedArea, Dimension dimension, Random random, CaveSettings tunnelSettings, boolean surfaceBreaking, int minimumLevel) {
        Point3d location = new Point3d(tunnelSettings.start.x + random.nextDouble() - 0.5, tunnelSettings.start.y + random.nextDouble() - 0.5, tunnelSettings.start.z + random.nextDouble() - 0.5);
        Vector3d direction = getRandomDirection(random);
        final float minRadius = tunnelSettings.minRadius, maxRadius = tunnelSettings.maxRadius,
                radiusChangeSpeed = tunnelSettings.radiusChangeSpeed, twistiness = tunnelSettings.twistiness;
        float length = 0.0f, radius = (maxRadius + minRadius) / 2.0f, radiusDelta = 0.0f;
        final int maxLength = tunnelSettings.length;
        if (logger.isTraceEnabled()) {
            logger.trace("Creating tunnel @ {},{},{} of length {}; radius: {} - {} (variability: {}); twistiness: {}",
                    tunnelSettings.start.x, tunnelSettings.start.y, tunnelSettings.start.z, maxLength, tunnelSettings.minRadius, tunnelSettings.maxRadius,
                    radiusChangeSpeed, twistiness);
        }
        long segmentSeed = random.nextLong();
        while (length < maxLength) {
            if ((minimumLevel == 0) && (dimension.getLayerValueAt(Caves.INSTANCE, (int) location.x, (int) location.y) < 1)) {
                // Don't stray into areas where the layer isn't present at all
                return;
            }
            if (((location.x + radius) >= exportedArea.x) && ((location.x - radius) <= (exportedArea.x + exportedArea.width))
                    && ((location.y + radius) >= exportedArea.y) && ((location.y - radius) <= (exportedArea.y + exportedArea.height))) {
                excavate(world, dimension, new Random((long) (segmentSeed + length)), tunnelSettings, location, radius, surfaceBreaking);
            }
            length += direction.length();
            location.add(direction);
            final Vector3d dirChange = getRandomDirection(random);
            dirChange.scale(random.nextDouble() / (5 - twistiness));
            direction.add(dirChange);
            direction.normalize();
            if (radiusChangeSpeed > 0.0) {
                radius = MathUtils.clamp(minRadius, radius + radiusDelta, maxRadius);
                radiusDelta += random.nextDouble() * 2 * radiusChangeSpeed - radiusChangeSpeed;
            }
        }
    }

    private void excavate(MinecraftWorld world, Dimension dimension, Random random, CaveSettings settings, Point3d location, double radius, boolean surfaceBreaking) {

        // TODOMC13: remove water above openings

        final boolean intrudingStone = settings.intrudingStone,
                roughWalls = settings.roughWalls,
                removeFloatingBlocks = settings.removeFloatingBlocks,
                floodWithLava = settings.floodWithLava;
        final int minZ = settings.minZ, waterLevel = settings.waterLevel;
        visitFilledAbsoluteSphere(location.x, location.y, location.z, (float) radius, ((x, y, z, d) -> {
            // TODO: efficiently check maxZ per x,y:
            if (z >= minZ) {
                int terrainHeight = dimension.getIntHeightAt(x, y);
                int maxZ = terrainHeight - (surfaceBreaking ? 0 : dimension.getTopLayerDepth(x, y, terrainHeight));
                if (z > maxZ) {
                    return true;
                }
                Material material = world.getMaterialAt(x, y, z);
                if (material.isNamedOneOf(MC_AIR, MC_CAVE_AIR)
                        || ((z <= waterLevel) && (floodWithLava ? material.isNamed(MC_LAVA) : material.isNamed(MC_WATER)))) {
                    // Already excavated
                    return true;
                }
                boolean blockExcavated = false;
                if ((roughWalls || intrudingStone) && (radius - d <= 1)) {
                    // Remember: this is not near the wall of the tunnel; it is
                    // near the edge of the sphere we're currently excavating,
                    // so only remove things, don't add them
                    if (intrudingStone) {
                        if (material.isNotNamedOneOf(MC_GRANITE, MC_DIORITE, MC_ANDESITE)
                                && ((!roughWalls)
                                || random.nextBoolean())) {
                            // Treat andesite, etc. as "harder" than regular stone
                            // so it protrudes slightly into the cave
                            excavateBlock(world, x, y, z, settings);
                            blockExcavated = true;
                        }
                    } else if (random.nextBoolean()) {
                        // TODO: do this better, with continuous and consistent variation
                        excavateBlock(world, x, y, z, settings);
                        blockExcavated = true;
                    }
                } else {
                    excavateBlock(world, x, y, z, settings);
                    blockExcavated = true;
                }
                if (blockExcavated && removeFloatingBlocks && (radius - d <= 2)) {
                    checkForFloatingBlock(world, x - 1, y, z, maxZ, settings);
                    checkForFloatingBlock(world, x, y - 1, z, maxZ, settings);
                    checkForFloatingBlock(world, x + 1, y, z, maxZ, settings);
                    checkForFloatingBlock(world, x, y + 1, z, maxZ, settings);
                    if (z > 1) {
                        checkForFloatingBlock(world, x, y, z - 1, maxZ, settings);
                    }
                    if (z < maxZ) {
                        checkForFloatingBlock(world, x, y, z + 1, maxZ, settings);
                    }
                }
            }
            return true;
        }));
    }

    static void excavateBlock(MinecraftWorld world, int x, int y, int z, CaveSettings settings) {
        world.setMaterialAt(x, y, z, (z <= settings.waterLevel)
                ? (settings.floodWithLava ? STATIONARY_LAVA : STATIONARY_WATER)
                : AIR);
    }

    /**
     * Check if the indicated block is a "floating block" and if so remove it.
     */
    static void checkForFloatingBlock(MinecraftWorld world, int x, int y, int z, int maxZ, CaveSettings settings) {
        Material material = world.getMaterialAt(x, y, z);
        if (material.isNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_PODZOL, MC_FARMLAND, MC_GRASS_PATH, MC_SAND, MC_RED_SAND, MC_GRAVEL)) {
            if (((z > 0) && (!world.getMaterialAt(x, y, z - 1).solid))
                    && ((z <= maxZ) && (!world.getMaterialAt(x, y, z + 1).solid))) {
                // The block is only one layer thick
                excavateBlock(world, x, y, z, settings);
                // TODO: this isn't removing nearly all one-block thick dirt. Why?
            }
        } else if (material.isNotNamedOneOf(MC_AIR, MC_WATER, MC_LAVA) && (! material.name.endsWith("_leaves"))) {
            if ((! world.getMaterialAt(x - 1, y, z).solid)
                    && (! world.getMaterialAt(x, y - 1, z).solid)
                    && (! world.getMaterialAt(x + 1, y, z).solid)
                    && (! world.getMaterialAt(x, y + 1, z).solid)
                    && ((z > 0) && (! world.getMaterialAt(x, y, z - 1).solid))
                    && ((z <= maxZ) && (! world.getMaterialAt(x, y, z + 1).solid))) {
                // The block is floating in the air
                excavateBlock(world, x, y, z, settings);
            }
        }
    }

    private Vector3d getRandomDirection(Random random) {
        double x1 = random.nextDouble() * 2 - 1, x2 = random.nextDouble() * 2 - 1;
        while (x1 * x1 + x2 * x2 >= 1) {
            x1 = random.nextDouble() * 2 - 1;
            x2 = random.nextDouble() * 2 - 1;
        }
        double a = Math.sqrt(1 - x1 * x1 - x2 * x2);
        return new Vector3d(2 * x1 * a, 2 * x2 * a, 1 - 2 * (x1 * x1 + x2 * x2));
    }

    private static final int MAX_CAVE_LENGTH = 128;
    private static final int CAVE_CHANCE = 131072;

    private static final Logger logger = LoggerFactory.getLogger(CavesExporter.class);

    /**
     * Settings for an individual cave.
     */
    static class CaveSettings {
        Point3i start;
        int length, minZ, waterLevel;
        float minRadius = 1.5f, maxRadius = 3.25f, twistiness = 3, radiusChangeSpeed = 0.2f;
        boolean intrudingStone = true, roughWalls, removeFloatingBlocks = true, floodWithLava;
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
