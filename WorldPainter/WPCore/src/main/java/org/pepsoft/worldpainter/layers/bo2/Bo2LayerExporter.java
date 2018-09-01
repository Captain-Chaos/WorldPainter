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

import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 * An exporter of {@link Bo2Layer}s.
 *
 * <p>This class is <strong>not</strong> thread safe.
 *
 * @author pepijn
 */
public class Bo2LayerExporter extends WPObjectExporter<Bo2Layer> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public Bo2LayerExporter(Bo2Layer layer) {
        super(layer);
    }
    
    @Override
    public List<Fixup> render(final Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
        final int maxHeight = dimension.getMaxHeight();
        final int maxZ = maxHeight - 1;
        final List<Fixup> fixups = new ArrayList<>();
        final int density = layer.getDensity() * 64;
        for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
            for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                // Set the seed and randomizer according to the chunk
                // coordinates to make sure the chunk is always rendered the
                // same, no matter how often it is rendered
                final long seed = dimension.getSeed() + (chunkX >> 4) * 65537 + (chunkY >> 4) * 4099;
                final Random random = new Random(seed);
                objectProvider.setSeed(seed);
                for (int x = chunkX; x < chunkX + 16; x++) {
                    for (int y = chunkY; y < chunkY + 16; y++) {
                        final int height = dimension.getIntHeightAt(x, y);
                        if ((height == -1) || (height >= maxZ)) {
                            // height == -1 means no tile present
                            continue;
                        }
                        final int strength = dimension.getLayerValueAt(layer, x, y);
                        if ((strength > 0) && (random.nextInt(density) <= strength * strength)) {
                            WPObject object = objectProvider.getObject();
                            final Placement placement = getPlacement(minecraftWorld, dimension, x, y, height + 1, object, random);
                            if (placement == Placement.NONE) {
                                continue;
                            }
                            if (object.getAttribute(ATTRIBUTE_RANDOM_ROTATION)) {
                                if (random.nextBoolean()) {
                                    object = new MirroredObject(object, false);
                                }
                                int rotateSteps = random.nextInt(4);
                                if (rotateSteps > 0) {
                                    object = new RotatedObject(object, rotateSteps);
                                }
                            }
                            final int z = (placement == Placement.ON_LAND) ? height + 1 : dimension.getWaterLevelAt(x, y) + 1;
                            if (! isSane(object, x, y, z, maxHeight)) {
                                continue;
                            }
                            prepareForExport(object, dimension);
                            if (! isRoom(minecraftWorld, dimension, object, x, y, z, placement)) {
                                continue;
                            }
                            if (! fitsInExportedArea(exportedArea, object, x, y)) {
                                // There is room on our side of the border, but
                                // the object extends outside the exported area,
                                // so it might clash with an object from another
                                // area. Schedule a fixup to retest whether
                                // there is room after all the objects have been
                                // placed on both sides of the border
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
    }

    @Override
    public Fixup apply(Dimension dimension, Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        final long seed = dimension.getSeed() ^ ((long) location.x << 40) ^ ((long) location.y << 20) ^ (location.z);
        applyRandom.setSeed(seed);
        if ((intensity > 0) && (applyRandom.nextInt(layer.getDensity() * 20) <= intensity * intensity / 225)) {
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
            objectProvider.setSeed(seed);
            WPObject object = objectProvider.getObject();
            Material existingMaterial = minecraftWorld.getMaterialAt(location.x, location.y, location.z);
            Material materialBelow = minecraftWorld.getMaterialAt(location.x, location.y, location.z - 1);
            if ((object.getAttribute(ATTRIBUTE_SPAWN_IN_LAVA) && existingMaterial.isNamed(MC_LAVA))
                    || (object.getAttribute(ATTRIBUTE_SPAWN_IN_WATER) && existingMaterial.isNamed(MC_WATER))
                    || (object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND) && (! materialBelow.veryInsubstantial))
                    || (! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION) && materialBelow.veryInsubstantial)) {
                if (object.getAttribute(ATTRIBUTE_RANDOM_ROTATION)) {
                    if (applyRandom.nextBoolean()) {
                        object = new MirroredObject(object, false);
                    }
                    int rotateSteps = applyRandom.nextInt(4);
                    if (rotateSteps > 0) {
                        object = new RotatedObject(object, rotateSteps);
                    }
                }
                if (! isSane(object, location.x, location.y, location.z, minecraftWorld.getMaxHeight())) {
                    return null;
                }
                prepareForExport(object, dimension);
                if (! isRoom(minecraftWorld, dimension, object, location.x, location.y, location.z, Placement.ON_LAND)) {
                    return null;
                }
                if (! fitsInExportedArea(exportedArea, object, location.x, location.y)) {
                    // There is room on our side of the border, but the object
                    // extends outside the exported area, so it might clash with an
                    // object from another area. Schedule a fixup to retest whether
                    // there is room after all the objects have been placed on both
                    // sides of the border
                    return new WPObjectFixup(object, location.x, location.y, location.z, Placement.ON_LAND);
                }
                renderObject(minecraftWorld, dimension, object, location.x, location.y, location.z);
            }
        }
        return null;
    }

    /**
     * Determines whether an object fits completely into the area currently
     * being rendere in the horizontal dimensions.
     *
     * @return <code>true</code> if the object fits.
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
        final int waterLevel = dimension.getWaterLevelAt(x, y);
        final boolean flooded = waterLevel >= z;
        if (flooded && (spawnUnderWater || spawnUnderLava || spawnOnWater || spawnOnLava)) {
            boolean lava = dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y);
            if (lava ? (spawnUnderLava && spawnOnLava) : (spawnUnderWater && spawnOnWater)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " potentially placeable under or on water or lava");
                }
                return random.nextBoolean() ? Placement.ON_LAND : Placement.FLOATING;
            } else if (lava ? spawnUnderLava : spawnUnderWater) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " potentially placeable under water or lava");
                }
                return Placement.ON_LAND;
            } else if (lava ? spawnOnLava : spawnOnWater) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " potentially placeable on water or lava");
                }
                return Placement.FLOATING;
            }
        } else if (! flooded) {
            Material materialUnderCoords = (z > 0) ? minecraftWorld.getMaterialAt(x, y, z - 1) : AIR;
            if (object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND) && (! materialUnderCoords.veryInsubstantial)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " potentially placeable on land");
                }
                return Placement.ON_LAND;
            } else if ((! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION)) && materialUnderCoords.veryInsubstantial) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " potentially placeable in the air");
                }
                return Placement.ON_LAND;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Object " + object.getName() + " @ " + x + "," + y + "," + z + " not placeable");
        }
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