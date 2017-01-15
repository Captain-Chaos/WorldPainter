package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Caves;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.GeometryUtil;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Created by Pepijn on 15-1-2017.
 */
public class CavesExporter extends AbstractLayerExporter<Caves> implements SecondPassLayerExporter {
    public CavesExporter() {
        super(Caves.INSTANCE, new CavesSettings());
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final CavesSettings settings = (CavesSettings) getSettings();
        final int minZ = Math.max(settings.getMinimumLevel(), dimension.isBottomless() ? 0 : 1),
                maxZForWorld = Math.min(settings.getMaximumLevel(), minecraftWorld.getMaxHeight() - 1);
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        area = (Rectangle) exportedArea.clone();
        area.grow(MAX_CAVE_LENGTH, MAX_CAVE_LENGTH);
        final Random random = new Random();
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                final int value = dimension.getLayerValueAt(Caves.INSTANCE, x, y);
                if (value > 0) {
                    random.setSeed(dimension.getSeed() + x * 65537 + y);
                    final int height = dimension.getIntHeightAt(x, y);
                    final int maxZ = Math.min(maxZForWorld, height - (surfaceBreaking ? 0 : dimension.getTopLayerDepth(x, y, height)));
                    for (int z = minZ; z <= maxZ; z++) {
                        if (value > random.nextInt(CAVE_CHANCE)) {
                            createTunnel(minecraftWorld, random, minZ, maxZ, new Point3d(x, y, z));
                        }
                    }
                }
            }
        }
        return null;
    }

    private void createTunnel(MinecraftWorld world, Random random, int minZ, int maxZ, Point3d location) {
        Vector3d direction = getRandomDirection(random);
        double l = 0.0;
        int radius = 3;
        while (l < MAX_CAVE_LENGTH) {
            excavate(world, location, minZ, maxZ, radius);
            l += direction.length();
            location.add(direction);
            Vector3d dirChange = getRandomDirection(random);
            dirChange.scale(random.nextDouble() / 5);
            direction.add(dirChange);
            direction.normalize();
        }
    }

    private void excavate(MinecraftWorld world, Point3d location, int minZ, int maxZ, int radius) {
        GeometryUtil.visitFilledSphere(radius, ((dx, dy, dz, d) -> {
            final int z = (int) (location.z + dz + 0.5);
            if ((z >= minZ) && (z <= maxZ)) {
                final int x = (int) (location.x + dx + 0.5);
                final int y = (int) (location.y + dy + 0.5);
                world.setMaterialAt(x, y, z, Material.AIR);
            }
            return true;
        }));
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

    private static final int MAX_CAVE_LENGTH = 64;
    private static final int CAVE_CHANCE = 1048576;

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
        private int minimumLevel = 0, maximumLevel = Integer.MAX_VALUE;

        private static final long serialVersionUID = 1L;
    }
}
