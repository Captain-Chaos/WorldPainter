package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

import static java.lang.Math.round;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.Dimension.Role.FLOATING_FLOOR;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.CAVE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.CUSTOM_DIMENSION;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.FIXED_HEIGHT;

/**
 * A helper class that can perform all the floor and roof height calculations for a particular TunnelLayer and
 * Dimension.
 */
public class TunnelLayerHelper {
    public TunnelLayerHelper(TunnelLayer layer, Dimension dimension) {
        this.layer = layer;
        floorDimension = ((layer.floorMode == CUSTOM_DIMENSION) && (layer.floorDimensionId != null) && (dimension != null))
                ? dimension.getWorld().getDimension(new Anchor(dimension.getAnchor().dim, (layer.getLayerMode() == CAVE) ? CAVE_FLOOR : FLOATING_FLOOR, dimension.getAnchor().invert, layer.floorDimensionId))
                : null;
        if (layer.floorNoise != null) {
            floorNoise = new NoiseHeightMap(layer.floorNoise, FLOOR_NOISE_SEED_OFFSET);
            if (dimension != null) {
                floorNoise.setSeed(dimension.getSeed());
            }
            floorNoiseOffset = layer.floorNoise.getRange();
        } else {
            floorNoise = null;
            floorNoiseOffset = 0;
        }
        if (layer.roofNoise != null) {
            roofNoise = new NoiseHeightMap(layer.roofNoise, ROOF_NOISE_SEED_OFFSET);
            if (dimension != null) {
                roofNoise.setSeed(dimension.getSeed());
            }
            roofNoiseOffset = layer.roofNoise.getRange();
        } else {
            roofNoise = null;
            roofNoiseOffset = 0;
        }
        if (dimension != null) {
            // Cache wall distance to increase performance
            final int maxWallDepth = Math.max(layer.floorWallDepth, layer.roofWallDepth);
            wallDistanceCache = dimension.getDistancesToEdge(layer, maxWallDepth);
            if ((layer.getLayerMode() == FLOATING) && (layer.getRoofMode() == FIXED_HEIGHT)) {
                // For fixed height floor dimension bottom, bake highest edge level within wall distance also
                edgeHeightCache = dimension.getEdgeHeights(layer, maxWallDepth);
            } else {
                edgeHeightCache = new ConstantHeightMap(layer.floorLevel);
            }
        } else {
            wallDistanceCache = null;
            edgeHeightCache = new ConstantHeightMap(layer.floorLevel);
        }
    }

    public int calculateRoofLevel(int x, int y, int terrainHeight, int minZ, int maxZ, int floorLevel) {
        switch (layer.roofMode) {
            case CONSTANT_DEPTH:
                return clamp(minZ, clamp(layer.roofMin, terrainHeight - layer.roofLevel, layer.roofMax) + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            case FIXED_HEIGHT:
                return clamp(minZ, layer.roofLevel + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            case INVERTED_DEPTH:
                return clamp(minZ, clamp(layer.roofMin, layer.roofLevel - (terrainHeight - layer.roofLevel), layer.roofMax) + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            case FIXED_HEIGHT_ABOVE_FLOOR:
                return clamp(minZ, clamp(layer.roofMin, floorLevel + layer.roofLevel, layer.roofMax) + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            default:
                throw new InternalError("layer.roofMode " + layer.roofMode);
        }
    }

    public int calculateBottomLevel(int x, int y, int minZ, int maxZ, int floorLevel, float distanceToWall) {
        // The multiplier indicates the normalised distance to the edge for purposes of calculating the edge shape; it
        // goes from 0.0 at the outer edge to 1.0 at the inner edge
        final double multiplier;
        switch (layer.getBottomEdgeShape()) {
            case LINEAR:
                multiplier = Math.min(distanceToWall / layer.roofWallDepth, 1.0);
                break;
            case ROUNDED:
                multiplier = Math.min(Math.sqrt(distanceToWall / layer.roofWallDepth), 1.0); // TODO is this the right formula for a quarter circle?
                break;
            case SHEER:
                multiplier = 1.0;
                break;
            case SMOOTH:
                throw new UnsupportedOperationException("not implemented yet");
            default:
                throw new InternalError();
        }
        switch (layer.roofMode) {
//            case CONSTANT_DEPTH:
//                return clamp(minZ, clamp(layer.roofMin, terrainHeight - layer.roofLevel, layer.roofMax) + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            case FIXED_HEIGHT:
                if (multiplier >= 1.0) {
                    return (int) round(clamp(minZ, layer.roofLevel
                            + ((roofNoise != null) ? (roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ));
                } else {
                    return (int) round(clamp(minZ, edgeHeightCache.getHeight(x, y) * (1.0 - multiplier)
                            + layer.roofLevel * multiplier
                            + ((roofNoise != null) ? ((roofNoise.getHeight(x, y) - roofNoiseOffset) * multiplier) : 0), maxZ));
                }
//            case INVERTED_DEPTH:
//                return clamp(minZ, clamp(layer.roofMin, layer.roofLevel - (terrainHeight - layer.roofLevel), layer.roofMax) + ((roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0), maxZ);
            case FIXED_HEIGHT_ABOVE_FLOOR:
                return (int) round(clamp(minZ, clamp(layer.roofMin, floorLevel - (multiplier * layer.roofLevel), layer.roofMax) + ((roofNoise != null) ? ((roofNoise.getHeight(x, y) - roofNoiseOffset) * multiplier) : 0.0), maxZ));
            default:
                throw new UnsupportedOperationException("layer.roofMode " + layer.roofMode);
        }
    }

    public int calculateFloorLevel(int x, int y, int terrainHeight, int minZ, int maxZ) {
        switch (layer.floorMode) {
            case CONSTANT_DEPTH:
                return clamp(minZ, clamp(layer.floorMin, terrainHeight - layer.floorLevel, layer.floorMax) + ((floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0), maxZ);
            case FIXED_HEIGHT:
                return clamp(minZ, layer.floorLevel + ((floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0), maxZ);
            case INVERTED_DEPTH:
                return clamp(minZ, clamp(layer.floorMin, layer.floorLevel - (terrainHeight - layer.floorLevel), layer.floorMax) + ((floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0), maxZ);
            case CUSTOM_DIMENSION:
                return clamp(minZ, (floorDimension != null) ? floorDimension.getIntHeightAt(x, y) : 32, maxZ);
            default:
                throw new InternalError("layer.floorMode " + layer.floorMode);
        }
    }

    public int calculateTopLedgeHeight(int x, int y) {
        return calculateTopLedgeHeight((float) wallDistanceCache.getHeight(x, y) - 1);
    }

    public int calculateTopLedgeHeight(float distanceToWall) {
        if (distanceToWall > layer.roofWallDepth) {
            return 0;
        } else {
            final float a = layer.roofWallDepth - distanceToWall;
            return (int) round(layer.roofWallDepth - Math.sqrt(layer.roofWallDepth * layer.roofWallDepth - a * a));
        }
    }

    public int calculateBottomLedgeHeight(int x, int y) {
        return calculateBottomLedgeHeight((float) wallDistanceCache.getHeight(x, y) - 1);
    }

    public int calculateBottomLedgeHeight(float distanceToWall) {
        if (distanceToWall > layer.floorWallDepth) {
            return 0;
        } else {
            final float a = layer.floorWallDepth - distanceToWall;
            return (int) round(layer.floorWallDepth - Math.sqrt(layer.floorWallDepth * layer.floorWallDepth - a * a));
        }
    }

    public float getDistanceToWall(int x, int y) {
        return (float) wallDistanceCache.getHeight(x, y);
    }

    private final TunnelLayer layer;
    private final Dimension floorDimension;
    private final NoiseHeightMap floorNoise, roofNoise;
    private final int floorNoiseOffset, roofNoiseOffset;
    private final HeightMap wallDistanceCache, edgeHeightCache;

    private static final long FLOOR_NOISE_SEED_OFFSET = 177766561L;
    private static final long ROOF_NOISE_SEED_OFFSET = 184818453L;
}