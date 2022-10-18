/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import com.google.common.collect.Sets;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.IncidentalLayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.exporters.WPObjectExporter;
import org.pepsoft.worldpainter.objects.MirroredObject;
import org.pepsoft.worldpainter.objects.RotatedObject;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 * An exporter of {@link Bo2Layer}s.
 *
 * <p>This class is <strong>not</strong> thread safe.
 *
 * @author pepijn
 */
public class Bo2LayerExporter extends WPObjectExporter<Bo2Layer> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public Bo2LayerExporter(Dimension dimension, Platform platform, Bo2Layer layer) {
        super(dimension, platform, null, layer);
    }

    @Override
    public Set<Stage> getStages() {
        return singleton(ADD_FEATURES);
    }

    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        try {
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
            final List<Fixup> fixups = new ArrayList<>();
            final int density = layer.getDensity() * 64;
            for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
                for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                    if (! dimension.isTilePresent(chunkX >> TILE_SIZE_BITS, chunkY >> TILE_SIZE_BITS)) {
                        continue;
                    }
                    // Set the seed and randomizer according to the chunk coordinates to make sure the chunk is always
                    // rendered the same, no matter how often it is rendered
                    final long seed = dimension.getSeed() + (chunkX >> 4) * 65537L + (chunkY >> 4) * 4099L;
                    final Random random = new Random(seed);
                    objectProvider.setSeed(seed);
                    for (int x = chunkX; x < chunkX + 16; x++) {
                        for (int y = chunkY; y < chunkY + 16; y++) {
                            final int strength = dimension.getLayerValueAt(layer, x, y);
                            if ((strength > 0) && (random.nextInt(density) <= strength * strength)) {
                                WPObject object = objectProvider.getObject();
                                final int variation = object.getAttribute(ATTRIBUTE_Y_VARIATION);
                                final int height = ((object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) ? (dimension.getIntHeightAt(x, y) + 1) : 0)
                                        + object.getAttribute(ATTRIBUTE_VERTICAL_OFFSET)
                                        + ((variation > 0) ? (random.nextInt(variation + 1) - ((variation + 1) / 2)) : 0); // Bias odd variation downwards
                                if ((height < minHeight) || (height >= maxHeight)) {
                                    continue;
                                }
                                final Placement placement = getPlacement(minecraftWorld, dimension, x, y, height, object, random);
                                if (placement == Placement.NONE) {
                                    continue;
                                }
                                final boolean randomRotationAndMirroring = object.getAttribute(ATTRIBUTE_RANDOM_ROTATION);
                                if ((randomRotationAndMirroring || object.getAttribute(ATTRIBUTE_RANDOM_MIRRORING_ONLY))
                                        && random.nextBoolean()) {
                                    // Preserve previous behaviour of only mirroring in X axis for un-migrated objects:
                                    object = new MirroredObject(object, (! randomRotationAndMirroring) && random.nextBoolean(), platform);
                                }
                                if (randomRotationAndMirroring || object.getAttribute(ATTRIBUTE_RANDOM_ROTATION_ONLY)) {
                                    int rotateSteps = random.nextInt(4);
                                    if (rotateSteps > 0) {
                                        object = new RotatedObject(object, rotateSteps, platform);
                                    }
                                }
                                final int z = (placement == Placement.ON_LAND) ? height : dimension.getWaterLevelAt(x, y) + 1;
                                if (! isSane(object, x, y, z, minHeight, maxHeight)) {
                                    continue;
                                }
                                prepareForExport(object, dimension);
                                if (! isRoom(minecraftWorld, dimension, object, x, y, z, placement)) {
                                    continue;
                                }
                                if (! fitsInExportedArea(exportedArea, object, x, y)) {
                                    // There is room on our side of the border, but the object extends outside the
                                    // exported area, so it might clash with an object from another area. Schedule a
                                    // fixup to retest whether there is room after all the objects have been placed on
                                    // both sides of the border
                                    fixups.add(new WPObjectFixup(object, x, y, z, placement));
                                    continue;
                                }
                                renderObject(minecraftWorld, dimension, object, x, y, z);
                            }
                        }
                    }
                }
            }
            return fixups;
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " (layer: " + layer.getName() + ")", e);
        }
    }

    @Override
    public Fixup apply(Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final long seed = dimension.getSeed() + location.x + location.y * 4099L + location.z * 65537L + layer.hashCode();
        applyRandom.setSeed(seed);
        if ((intensity > 0) && (applyRandom.nextInt(layer.getDensity() * 20) <= intensity * intensity / 225)) {
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
            objectProvider.setSeed(seed + 1);
            WPObject object = objectProvider.getObject();
            final int variation = object.getAttribute(ATTRIBUTE_Y_VARIATION);
            final int height = ((object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) ? location.z : 0)
                    + object.getAttribute(ATTRIBUTE_VERTICAL_OFFSET)
                    + ((variation > 0) ? (applyRandom.nextInt(variation + 1) - ((variation + 1) / 2)) : 0); // Bias odd variation downwards
            final Material existingMaterial = minecraftWorld.getMaterialAt(location.x, location.y, height);
            final Material materialBelow = minecraftWorld.getMaterialAt(location.x, location.y, height - 1);
            if ((object.getAttribute(ATTRIBUTE_SPAWN_IN_LAVA) && existingMaterial.isNamed(MC_LAVA))
                    || (object.getAttribute(ATTRIBUTE_SPAWN_IN_WATER) && existingMaterial.isNamed(MC_WATER))
                    || (object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND) && (! materialBelow.veryInsubstantial))
                    || (! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION) && materialBelow.veryInsubstantial)) {
                final boolean randomRotationAndMirroring = object.getAttribute(ATTRIBUTE_RANDOM_ROTATION);
                if ((randomRotationAndMirroring || object.getAttribute(ATTRIBUTE_RANDOM_MIRRORING_ONLY))
                        && applyRandom.nextBoolean()) {
                    // Preserve previous behaviour of only mirroring in X axis for un-migrated objects:
                    object = new MirroredObject(object, (! randomRotationAndMirroring) && applyRandom.nextBoolean(), platform);
                }
                if (randomRotationAndMirroring || object.getAttribute(ATTRIBUTE_RANDOM_ROTATION_ONLY)) {
                    int rotateSteps = applyRandom.nextInt(4);
                    if (rotateSteps > 0) {
                        object = new RotatedObject(object, rotateSteps, platform);
                    }
                }
                if (! isSane(object, location.x, location.y, height, minecraftWorld.getMinHeight(), minecraftWorld.getMaxHeight())) {
                    return null;
                }
                prepareForExport(object, dimension);
                if (! isRoom(minecraftWorld, dimension, object, location.x, location.y, height, Placement.ON_LAND)) {
                    return null;
                }
                if (! fitsInExportedArea(exportedArea, object, location.x, location.y)) {
                    // There is room on our side of the border, but the object extends outside the exported area, so it
                    // might clash with an object from another area. Schedule a fixup to retest whether there is room
                    // after all the objects have been placed on both sides of the border
                    return new WPObjectFixup(object, location.x, location.y, height, Placement.ON_LAND);
                }
                renderObject(minecraftWorld, dimension, object, location.x, location.y, height);
            }
        }
        return null;
    }

    /**
     * Determines whether an object fits completely into the area currently
     * being rendere in the horizontal dimensions.
     *
     * @return {@code true} if the object fits.
     */
    private boolean fitsInExportedArea(final Rectangle exportedArea, final WPObject object, final int x, final int y) {
        final Point3i dimensions = object.getDimensions();
        final Point3i offset = object.getOffset();
        // Check whether the objects fits completely inside the exported area.
        // This is to avoid objects getting cut off at area boundaries
        return ! ((x + offset.x < exportedArea.x)
            || (x + offset.x + dimensions.x - 1 > exportedArea.x + exportedArea.width - 1)
            || (y + offset.y < exportedArea.y)
            || (y + offset.y + dimensions.y - 1 > exportedArea.y + exportedArea.height - 1));
    }

    /**
     * Determines whether an object's attributes allow it to be placed at a
     * certain location, and if so where along the vertical axis.
     *
     * @return An indication of where along the vertical axis the object may
     *     be placed, which may be {@link Placement#NONE} if it may not be
     *     placed at all.
     */
    private Placement getPlacement(final MinecraftWorld minecraftWorld, final Dimension dimension, final int x, final int y, final int z, final WPObject object, final Random random) {
        final boolean spawnUnderWater = object.getAttribute(ATTRIBUTE_SPAWN_IN_WATER), spawnUnderLava = object.getAttribute(ATTRIBUTE_SPAWN_IN_LAVA);
        final boolean spawnOnWater = object.getAttribute(ATTRIBUTE_SPAWN_ON_WATER), spawnOnLava = object.getAttribute(ATTRIBUTE_SPAWN_ON_LAVA);
        final boolean flooded;
        if (object.getAttribute(ATTRIBUTE_HEIGHT_MODE) == HEIGHT_MODE_TERRAIN) {
            flooded = dimension.getWaterLevelAt(x, y) >= z;
        } else {
            flooded = (z > dimension.getIntHeightAt(x, y)) && (dimension.getWaterLevelAt(x, y) >= z);
        }
        if (flooded && (spawnUnderWater || spawnUnderLava || spawnOnWater || spawnOnLava)) {
            boolean lava = dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y);
            if (lava ? (spawnUnderLava && spawnOnLava) : (spawnUnderWater && spawnOnWater)) {
                logger.trace("Object {} @ {},{},{} potentially placeable under or on water or lava", object.getName(), x, y, z);
                return random.nextBoolean() ? Placement.ON_LAND : Placement.FLOATING;
            } else if (lava ? spawnUnderLava : spawnUnderWater) {
                logger.trace("Object {} @ {},{},{} potentially placeable under water or lava", object.getName(), x, y, z);
                return Placement.ON_LAND;
            } else if (lava ? spawnOnLava : spawnOnWater) {
                logger.trace("Object {} @ {},{},{} potentially placeable on water or lava", object.getName(), x, y, z);
                return Placement.FLOATING;
            }
        } else if (! flooded) {
            Material materialUnderCoords = (z > minecraftWorld.getMinHeight()) ? minecraftWorld.getMaterialAt(x, y, z - 1) : AIR;
            if (object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND) && (! materialUnderCoords.veryInsubstantial)) {
                logger.trace("Object {} @ {},{},{} potentially placeable on land", object.getName(), x, y, z);
                return Placement.ON_LAND;
            } else if ((! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION)) && materialUnderCoords.veryInsubstantial) {
                logger.trace("Object {} @ {},{},{} potentially placeable in the air", object.getName(), x, y, z);
                return Placement.ON_LAND;
            }
        }
        logger.trace("Object {} @ {},{},{} not placeable", object.getName(), x, y, z);
        return Placement.NONE;
    }

    /**
     * In case the object is from a plugin and needs to be processed by the
     * plugin before being exported, and that hasn't happened yet for this
     * object, do that now.
     *
     * @param object The object to prepare for export.
     */
    private void prepareForExport(WPObject object, Dimension dimension) {
        if (! preparedObjects.contains(object)) {
            object.prepareForExport(dimension);
            preparedObjects.add(object);
        }
    }

    private final Random applyRandom = new Random();
    private final Set<WPObject> preparedObjects = Sets.newIdentityHashSet();

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Bo2LayerExporter.class);
}