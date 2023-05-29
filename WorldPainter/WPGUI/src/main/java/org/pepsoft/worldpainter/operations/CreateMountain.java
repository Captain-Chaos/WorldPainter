package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.BitField;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.util.GeometryUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class CreateMountain extends MouseOrTabletOperation {
    public CreateMountain(WorldPainter view) {
        super("CreateMountain", "Instantly create a mountain", view, "operation.createMountain");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        // 1. determine lowest level around edges of brush (use a percentage factor to avoid very small low areas having too large an impact; say the point where 66% of the edge is above? 50%)
        // 2. configure the angle? or the summit height? either?
        // 3. use the brush shape? or just a circle?
        // 4. using what we determined in 1. and whatever we decide and the user configures for 2. and 3. determine the height of the summit
        // 5. optional bias of the slope according to the cardinal direction to create skewed mountains
        // 6. snake out from the summit, going mostly down but sometimes up
        // 7. when creating a local summit (going from going up to going down), have a chance of spawning one or more diverging ridges
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
//        GroundCoverLayer deepSnowLayer = null;
//        boolean shouldAddLayer = true;
//        for (Layer layer: dimension.getCustomLayers()) {
//            if ((layer instanceof GroundCoverLayer) && (layer.getName().equals("Mountain Snow"))) {
//                deepSnowLayer = (GroundCoverLayer) layer;
//                shouldAddLayer = false;
//                break;
//            }
//        }
//        if (deepSnowLayer == null) {
//            deepSnowLayer = new GroundCoverLayer("Mountain Snow", MixedMaterial.create(Material.SNOW), Color.WHITE.getRGB());
//            deepSnowLayer.setThickness(5);
//            deepSnowLayer.setEdgeShape(GroundCoverLayer.EdgeShape.SMOOTH);
//            deepSnowLayer.setEdgeWidth(15);
//            deepSnowLayer.setSmooth(true);
//            // TODO: add some variation
//        }

//        FancyPostGenerationTheme theme = new FancyPostGenerationTheme(dimension, Terrain.GRASS);
//        theme.install();
//
//        AbstractTheme mountainTheme = new AbstractTheme(dimension) {
//            @Override
//            public void apply(Tile tile, int x, int y, float height, int waterLevel, float slope) {
//
//            }
//        };

        int maxY = dimension.getMaxHeight() - 1;
        float summit = Math.min(dimension.getHeightAt(centreX, centreY) + 128, dimension.getMaxHeight() - 1);
        Random random = new Random();
        int noOfRidges = 3 + random.nextInt(2);
        float[] ridgeDirections = new float[noOfRidges]; // TODO: have a few more ridgeDirections for later ridges to give more variety
        Set<Ridge> ridges = new HashSet<>();
//        float firstRidgeDirection = (float) (random.nextLong() * MathUtils.TWO_PI / noOfRidges);
        for (int i = 0; i < ridgeDirections.length; i++) {
            ridgeDirections[i] = (float) (/*firstRidgeDirection +*/ MathUtils.TWO_PI * i / noOfRidges + random.nextDouble() / 2 - 0.25);
            ridges.add(new Ridge(centreX, centreY, summit, ridgeDirections[i], 5f, -5f)); // TODO: vary these
        }

        // Keep track of which blocks were raised, to that we can apply the
        // theme to them once, at the end; TODO: make this configurable
        BitField touchedBlocks = new BitField();

        dimension.setEventsInhibited(true);
//        Dimension snapshot = dimension.getSnapshot();
//        dimension.rememberChanges();
        try {
            raiseCone(dimension, /*snapshot,*/ centreX, centreY, summit, touchedBlocks);
            Set<Ridge> addRidges = new HashSet<>();
            do {
                for (Iterator<Ridge> i = ridges.iterator(); i.hasNext(); ) {
                    Ridge ridge = i.next();
                    int intX = Math.round(ridge.x);
                    int intY = Math.round(ridge.y);
                    float newX = (float) (ridge.x + Math.sin(ridge.θ) * ridge.dXY);
                    float newY = (float) (ridge.y - Math.cos(ridge.θ) * ridge.dXY);
                    int intNewX = Math.round(newX);
                    int intNewY = Math.round(newY);
                    float newZ = ridge.z + ridge.dZ;
                    // TODO this causes jagged edges; somehow we're not lining up the line segments vertically correctly
                    float dZ = (newZ - ridge.z) / MathUtils.getDistance(intX, intY, intNewX, intNewY);
                    GeometryUtil.visitLine(intX, intY, intNewX, intNewY, (x, y, d) -> {
                        // Skip the first block as that will have been done
                        // already in the previous iteration
                        if (d >= 1) {
                            // TODO this causes jagged edges; somehow we're not lining up the line segments vertically correctly
                            raiseCone(dimension, /*snapshot,*/ x, y, ridge.z + d * dZ, touchedBlocks);
                        }
                        return true;
                    });
                    float heightAtHead = dimension.getHeightAt(intNewX, intNewY);
                    if ((heightAtHead == -Float.MAX_VALUE) || (heightAtHead >= newZ)) {
                        // Off the map, or reached higher ground
                        i.remove();
                    } else {
                        ridge.x = newX;
                        ridge.y = newY;
                        ridge.previousZ = ridge.z;
                        ridge.z = newZ;
                        if (random.nextInt(10) == 0) {
                            ridge.θ += random.nextDouble() - 0.5;
                        }
                        boolean wasRising = ridge.dZ > 0;
                        ridge.dZ += random.nextDouble() * 2 - 1;
                        if (ridge.z + ridge.dZ > maxY) {
                            ridge.dZ = -ridge.dZ;
                        }
                        boolean isFalling = ridge.dZ < 0;
                        if ((wasRising && isFalling) || (random.nextInt(10) == 0)) { // TODO: make configurable
                            addRidges.add(new Ridge(ridge.x, ridge.y, ridge.z, ridge.θ + (float) (random.nextBoolean() ? Math.PI / 2 : -Math.PI / 2), 5f, ridge.start.dZ * 0.9f)); // TODO: make configurable? inherit from current ridge?
                        }
                    }
                }
                if (! addRidges.isEmpty()) {
                    ridges.addAll(addRidges);
                    addRidges.clear();
                }
            } while (! ridges.isEmpty());

            // Apply the theme to every block that was raised
            touchedBlocks.visitSetBits((x, y, b) -> {
                // TODO: go tile by tile to improve performance, or do this whole thing much better
                dimension.applyTheme(x, y);
                return true;
            });
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    // TODO optimize this by not going down to bedrock for every one
    // TODO flare out at the bottom
    // TODO add random variation
    private void raiseCone(Dimension dimension, /*Dimension snapshot,*/ int x, int y, float z, BitField touchedBlocks) {
        GeometryUtil.visitFilledCircle((int) Math.ceil(z / 1.5), (dx, dy, d) -> {
            int localX = x + dx;
            int localY = y + dy;
            float localZ = z - d * 2 + dx / 1.5f - Math.min(d / 50, 1) * (float) RANDOM_VARIATION.getHeight(localX, localY); // TODO: make slope and bias configurable, and take it into account for radius of visited circle
            float existingHeight = dimension.getHeightAt(localX, localY);
            if ((existingHeight != -Float.MAX_VALUE) && localZ >= existingHeight) {
                dimension.setHeightAt(localX, localY, localZ);
                touchedBlocks.set(localX, localY);
            }
            return true;
        });
    }

    static class Ridge implements Cloneable {
        public Ridge(float x, float y, float z, float θ, float dXY, float dZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.θ = θ;
            this.dXY = dXY;
            this.dZ = dZ;
            previousZ = z;
            start = clone();
        }

        @Override
        public Ridge clone() {
            try {
                return (Ridge) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * The location where the head of the ridge has reached.
         */
        float x, y, z;
        /**
         * The height where the ridge had previously reached.
         */
        float previousZ;
        /**
         * The direction in which the ridge is travelling as a clockwise radian
         * where θ == 0 is due north.
         */
        float θ;
        /**
         * The speed at which the ridge is travelling horizontally, and
         * vertically. The ridge will terminate when it reaches existing ground
         * level.
         */
        float dXY, dZ;
        /**
         * A copy of the settings with which this ridge was created.
         */
        Ridge start;
    }

    private static final NoiseHeightMap RANDOM_VARIATION = new NoiseHeightMap(20.0, 1.0, 3);
}