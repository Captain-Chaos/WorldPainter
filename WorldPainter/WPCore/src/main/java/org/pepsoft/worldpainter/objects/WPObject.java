/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Dimension;

import javax.vecmath.Point3i;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.pepsoft.minecraft.Constants.TAG_FACING;
import static org.pepsoft.minecraft.Constants.TAG_FACING_;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.util.WPObjectUtils.wouldConnect;

/**
 * A three-dimensional object, consisting of Minecraft blocks, which can be placed in a map.
 *
 * <p>This class uses the WorldPainter coordinate system (z is vertical).
 *
 * @author pepijn
 */
public interface WPObject extends Serializable, Cloneable {
    /**
     * Get the name of the object.
     * 
     * @return The name of the object.
     */
    String getName();
    
    /**
     * Set the name of the object.
     * 
     * @param name The new name of the object.
     */
    void setName(String name);
    
    /**
     * Get the dimensions of the object.
     * 
     * @return The dimensions of the object.
     */
    Point3i getDimensions();

    /**
     * Get the offset to apply to this object when placing it. In other words
     * these are the reverse of the coordinates of the "anchor" block of this
     * object.
     *
     * <p>This is a convenience method which must return the same as invoking
     * {@code getAttribute(ATTRIBUTE_OFFSET)}. See
     * {@link #getAttribute(AttributeKey)} and {@link #ATTRIBUTE_OFFSET}.
     *
     * @return The offset to apply to this object when placing it.
     */
    Point3i getOffset();

    /**
     * Get the material to place at the specified relative coordinates. Should only be invoked for coordinates for which
     * {@link #getMask(int, int, int)} returns {@code true}. These coordinates are zero-based and must never be
     * negative.
     * 
     * @param x The relative X coordinate.
     * @param y The relative Y coordinate.
     * @param z The relative Z coordinate.
     * @return The material to place at the specified relative coordinates.
     */
    Material getMaterial(int x, int y, int z);

    /**
     * Return all unique materials contained in this object.
     *
     * @return All unique materials contained in this object.
     */
    default Set<Material> getAllMaterials() {
        Set<Material> allMaterials = new HashSet<>();
        visitBlocks((object, x, y, z, material) -> allMaterials.add(material));
        return allMaterials;
    }

    /**
     * Determine whether a block should be placed at the specified relative coordinates. These coordinates are
     * zero-based and must never be negative.
     * 
     * @param x The relative X coordinate.
     * @param y The relative Y coordinate.
     * @param z The relative Z coordinate.
     * @return {@code true} if a block should be placed at the specified relative coordinates.
     */
    boolean getMask(int x, int y, int z);
    
    /**
     * Get any entities contained in the object. The entities' coordinates
     * should be relative to the object, not absolute.
     * 
     * @return Any entities contained in the object. May be {@code null}.
     */
    List<Entity> getEntities();
    
    /**
     * Get any tile entities contained in the object. The entities' coordinates
     * should be relative to the object, not absolute.
     * 
     * @return Any tile entities contained in the object. May be
     *     {@code null}.
     */
    List<TileEntity> getTileEntities();

    /**
     * Make preparations, if necessary, for exporting the object. For example
     * retrieving and applying a mapping. This method will be invoked by
     * WorldPainter before {@link #getMask(int, int, int)} or
     * {@link #getMaterial(int, int, int)} are invoked.
     *
     * @param dimension The dimension for which the object is being exported.
     */
    void prepareForExport(Dimension dimension); // TODO <-- switch skull blocks to the right versions here

    /**
     * Get a live view of the object metadata.
     * 
     * @return A live view of the object metadata. May be {@code null}.
     */
    Map<String, Serializable> getAttributes();

    /**
     * Determine whether the object contains a value for a particular attribute.
     *
     * @param attributeKey The attribute key to check.
     * @return {@code true} if a value is set for the specified attribute.
     */
    default boolean hasAttribute(AttributeKey<?> attributeKey) {
        Map<String, Serializable> attributes = getAttributes();
        return (attributes != null) && attributes.containsKey(attributeKey.key);
    }

    /**
     * Convencience method for getting the value of an attribute stored in the
     * external metadata, if any. Should return the value of the attribute if it
     * is present, or a default value (which may be {@code null}) if it is
     * not.
     * 
     * @param <T> The type of the attribute.
     * @param key The key of the attribute.
     * @return The value of the specified attribute, or the specified default
     *     value if the attribute is not set.
     */
    default <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        return key.get(getAttributes());
    }
    
    /**
     * Store external metadata about the object.
     * 
     * @param attributes The external metadata to store.
     */
    void setAttributes(Map<String, Serializable> attributes);
    
    /**
     * Convenience method for setting the value of an attribute stored in the
     * external metadata, if any. Setting the value to {@code null} will
     * delete the attribute from the store. If the store becomes empty it is
     * deleted entirely.
     * 
     * @param <T> The type of the attribute.
     * @param key The key of the attribute to set or delete.
     * @param value The value of the attribute to set, or {@code null} to
     *     delete it.
     */
    <T extends Serializable> void setAttribute(AttributeKey<T> key, T value);
    
    /**
     * Create a clone of the object. The block data is immutable so may be
     * shared, but all mutable settings are copied so they can be changed
     * independently.
     * 
     * @return A clone of the object.
     */
    WPObject clone();

    /**
     * Guestimate an offset for the object. If an offset cannot be calculated
     * because the object is entirely empty, {@code null} will be returned.
     *
     * <p><strong>Note</strong> that this method calls {@link #getDimensions()}
     * and {@link #getMask(int, int, int)}, so they must return valid values.
     */
    default Point3i guestimateOffset() {
        final Point3i dims = getDimensions();
        int offsetZ = Integer.MIN_VALUE, lowestX = 0, highestX = 0, lowestY = 0, highestY = 0;
        for (int z = 0; (z < dims.z) && (offsetZ == Integer.MIN_VALUE); z++) {
            for (int x = 0; x < dims.x; x++) {
                for (int y = 0; y < dims.y; y++) {
                    if (getMask(x, y, z)) {
                        if (offsetZ == Integer.MIN_VALUE) {
                            offsetZ = z;
                            lowestX = highestX = x;
                            lowestY = highestY = y;
                        } else {
                            if (x < lowestX) {
                                lowestX = x;
                            } else if (x > highestX) {
                                highestX = x;
                            }
                            if (y < lowestY) {
                                lowestY = y;
                            } else if (y > highestY) {
                                highestY = y;
                            }
                        }
                    }
                }
            }
        }
        return (offsetZ > Integer.MIN_VALUE) ? new Point3i(-(lowestX + highestX) / 2, -(lowestY + highestY) / 2, -offsetZ) : null;
    }
    
    /**
     * Visit all blocks in the object. The order in which the blocks are visited
     * is undefined.
     * 
     * @param visitor The visitor to invoke for each block. If the visitor
     * returns {@code false} the operation is aborted.
     * @return {@code true} if all blocks were visited or
     * {@code false} if the visitor returned {@code false} at some
     * point.
     */
    default boolean visitBlocks(BlockVisitor visitor) {
        final Point3i dim = getDimensions();
        for (int z = 0; z < dim.z; z++) {
            for (int x = 0; x < dim.x; x++) {
                for (int y = 0; y < dim.y; y++) {
                    if (getMask(x, y, z)) {
                        if (! visitor.visitBlock(this, x, y, z, getMaterial(x, y, z))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Will guess whether the {@link #ATTRIBUTE_CONNECT_BLOCKS} attribute should be set and if so set it. Should only be
     * called for custom object formats which have legacy, numerical block IDs, for which you might want to set that
     * property.
     */
    default void guessConnectBlocks() {
        final AtomicBoolean setConnectBlocks = new AtomicBoolean();
        visitBlocks((object, x, y, z, material) -> {
            if (material.connectingBlock) {
                // First check if one of the connecting properties is set; if so then this object must contain modern
                // materials and may be assumed to have the right properties set already. This is probably redundant.
                if (material.is(WEST) || material.is(NORTH) || material.is(EAST) || material.is(SOUTH)) {
                    setConnectBlocks.set(false);
                    return false;
                } else {
                    // The material has none of its connecting properties set. Check if there is a block around it in
                    // the object to which it should have been connected
                    final Point3i dim = getDimensions();
                    if (((x > 0) && getMask(x - 1, y, z) && wouldConnect(material, getMaterial(x - 1, y, z)))
                            || ((y > 0) && getMask(x, y - 1, z) && wouldConnect(material, getMaterial(x, y - 1, z)))
                            || ((x < (dim.x - 1)) && getMask(x + 1, y, z) && wouldConnect(material, getMaterial(x + 1, y, z)))
                            || ((y < (dim.y - 1)) && getMask(x, y + 1, z) && wouldConnect(material, getMaterial(x, y + 1, z)))) {
                        // There is a block around to which this block should have been connected, so conclude for now
                        // that the attribute should be set, but keep looking for proof to the contrary
                        setConnectBlocks.set(true);
                    }
                }
            }
            return true;
        });
        if (setConnectBlocks.get()) {
            setAttribute(ATTRIBUTE_CONNECT_BLOCKS, true);
        }
    }

    /**
     * TODO only for formats with named blocks which could conceivably contain the waterlogged property
     */
    default void guessManageWaterlogged() {
        final AtomicBoolean resetManageWaterblocks = new AtomicBoolean();
        visitBlocks((object, x, y, z, material) -> {
            if (material.is(WATERLOGGED)) {
                resetManageWaterblocks.set(true);
                return false;
            }
            return true;
        });
        if (resetManageWaterblocks.get()) {
            setAttribute(ATTRIBUTE_MANAGE_WATERLOGGED, false);
        }
    }

    /**
     * Dumps the object to the console.
     */
    default void dump() {
        final Point3i dim = getDimensions();
        final Map<Point3i, List<Entity>> entities = new HashMap<>();
        if (getEntities() != null) {
            for (Entity entity : getEntities()) {
                final Point3i pos = new Point3i(
                        (int) Math.floor(entity.getRelPos()[0]),
                        (int) Math.floor(entity.getRelPos()[2]),
                        (int) Math.floor(entity.getRelPos()[1])
                );
                entities.computeIfAbsent(pos, k -> new ArrayList<>()).add(entity);
            }
        }
        for (int z = 0; z < dim.z; z++) {
            final List<Entity> entitiesEncountered = new ArrayList<>(entities.size());
            System.out.println("X-->");
            for (int y = 0; y < dim.y; y++) {
                for (int row = 0; row < 4; row++) {
                    switch (row) {
                        case 0:
                            for (int x = 0; x < dim.x; x++) {
                                System.out.print("+------");
                            }
                            System.out.print('+');
                            break;
                        case 1:
                            for (int x = 0; x < dim.x; x++) {
                                if (getMask(x, y, z)) {
                                    System.out.printf("|%6.6s", getMaterial(x, y, z).simpleName);
                                } else {
                                    System.out.print("|      ");
                                }
                            }
                            System.out.print('|');
                            break;
                        case 2:
                            for (int x = 0; x < dim.x; x++) {
                                final Point3i pos = new Point3i(x, y, z);
                                if (entities.containsKey(pos)) {
                                    final Entity entity = entities.get(pos).get(0);
                                    entitiesEncountered.add(entity);
                                    final String id = entity.getId();
                                    System.out.printf("|%d:%4.4s", entitiesEncountered.size(), id.substring(id.indexOf(':') + 1));
                                } else {
                                    System.out.print("|      ");
                                }
                            }
                            System.out.print('|');
                            break;
                        case 3:
                            for (int x = 0; x < dim.x; x++) {
                                final Point3i pos = new Point3i(x, y, z);
                                if (entities.containsKey(pos) && (entities.get(pos).size() > 1)) {
                                    final Entity entity = entities.get(pos).get(1);
                                    entitiesEncountered.add(entity);
                                    final String id = entity.getId();
                                    System.out.printf("|%d:%4.4s", entitiesEncountered.size(), id.substring(id.indexOf(':') + 1));
                                } else {
                                    System.out.print("|      ");
                                }
                            }
                            System.out.print('|');
                            break;
                    }
                    if ((y * 4 + row) == 0) {
                        System.out.print(" Y");
                    } else if ((y * 4 + row) == 1) {
                        System.out.print(" |");
                    } else if ((y * 4 + row) == 2) {
                        System.out.print(" v");
                    }
                    System.out.println();
                }
            }
            for (int x = 0; x < dim.x; x++) {
                System.out.print("+------");
            }
            System.out.println("+ Z: " + z);
            for (int i = 0; i < entitiesEncountered.size(); i++) {
                final Entity entity = entitiesEncountered.get(i);
                final StringBuilder sb = new StringBuilder();
                sb.append(entity.getId());
                sb.append(", relPos: ");
                sb.append(Arrays.toString(entity.getRelPos()));
                CompoundTag nbt = entity.toNBT();
                if (nbt.containsTag(TAG_FACING_)) {
                    sb.append(", facing: ");
                    sb.append(((ByteTag) nbt.getTag(TAG_FACING_)).getValue());
                }
                if (nbt.containsTag(TAG_FACING)) {
                    sb.append(", Facing: ");
                    sb.append(((ByteTag) nbt.getTag(TAG_FACING)).getValue());
                }
                System.out.println("*" + (i + 1) + ": " + sb);
            }
            System.out.println();
        }
    }

    // Standard attribute values
    int COLLISION_MODE_ALL   = 1;
    int COLLISION_MODE_SOLID = 2;
    int COLLISION_MODE_NONE  = 3;

    int LEAF_DECAY_NO_CHANGE = 1;
    int LEAF_DECAY_ON        = 2;
    int LEAF_DECAY_OFF       = 3;

    int HEIGHT_MODE_TERRAIN = 1;
    int HEIGHT_MODE_FIXED   = 2;

    // Standard attribute keys
    AttributeKey<File>    ATTRIBUTE_FILE   = new AttributeKey<>("WPObject.file");
    AttributeKey<Point3i> ATTRIBUTE_OFFSET = new AttributeKey<>("WPObject.offset", new Point3i());
    /**
     * Random rotation <strong>and</strong> mirroring, for historical reasons. See {@link #ATTRIBUTE_RANDOM_ROTATION_ONLY}
     * and {@link #ATTRIBUTE_RANDOM_MIRRORING_ONLY} for separate attributes. If one or both of those attributes are
     * set, this one must be set to {@code false}, since otherwise it defaults to {@code true} and will override the
     * separate attributes.
     */
    AttributeKey<Boolean> ATTRIBUTE_RANDOM_ROTATION            = new AttributeKey<>("WPObject.randomRotation", true);
    AttributeKey<Boolean> ATTRIBUTE_NEEDS_FOUNDATION           = new AttributeKey<>("WPObject.needsFoundation", true);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_IN_WATER             = new AttributeKey<>("WPObject.spawnInWater", false);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_IN_LAVA              = new AttributeKey<>("WPObject.spawnInLava", false);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_ON_LAND              = new AttributeKey<>("WPObject.spawnOnLand", true);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_ON_WATER             = new AttributeKey<>("WPObject.spawnOnWater", false);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_ON_LAVA              = new AttributeKey<>("WPObject.spawnOnLava", false);
    AttributeKey<Integer> ATTRIBUTE_FREQUENCY                  = new AttributeKey<>("WPObject.frequency", 100);
    AttributeKey<Boolean> ATTRIBUTE_SPAWN_ON_WATER_NO_COLLIDE  = new AttributeKey<>("WPObject.spawnOnWater.noCollide", false);
    /**
     * Collision mode. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td>{@link #COLLISION_MODE_ALL}</td><td>Will collide with (and therefore not render) any above ground block other than air</td></tr>
     * <tr><td><strong>{@link #COLLISION_MODE_SOLID}</strong></td><td>Will collide with (and therefore not render) any above ground <em>solid</em> block (i.e. not air, grass, water, flowers, leaves, etc.). Default value</td></tr>
     * <tr><td>{@link #COLLISION_MODE_NONE}</td><td>Will not collide with <em>any</em> above ground block (and therefore intersect any other object already there!)</td></tr></table>
     */
    AttributeKey<Integer> ATTRIBUTE_COLLISION_MODE = new AttributeKey<>("WPObject.collisionMode", COLLISION_MODE_SOLID); // See COLLISION_MODE_* constants
    /**
     * Underground rendering mode. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td><strong>{@link #COLLISION_MODE_ALL}</strong></td><td>Every underground block belonging to the object will be rendered (including air blocks), regardless of what is already there. Default value</td></tr>
     * <tr><td>{@link #COLLISION_MODE_SOLID}</td><td>Every <em>solid</em> (i.e. not air, grass, water, flowers, leaves, etc.) underground block belonging to the object will be rendered regardless of what is already there. Non-solid blocks will be rendered only if the existing block is air</td></tr>
     * <tr><td>{@link #COLLISION_MODE_NONE}</td><td>Underground blocks belonging to the object will only be rendered if the existing block is air</td></tr></table>
     */
    AttributeKey<Integer> ATTRIBUTE_UNDERGROUND_MODE = new AttributeKey<>("WPObject.undergroundMode", COLLISION_MODE_ALL); // See COLLISION_MODE_* constants
    /**
     * Whether to change leaf blocks so that they do or do not decay. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td><strong>{@link #LEAF_DECAY_NO_CHANGE}</strong></td><td>Leaf blocks are copied unchanged from the custom object. Default value</td></tr>
     * <tr><td>{@link #LEAF_DECAY_ON}</td><td>All leaf blocks are set to decay regardless of their setting in the custom object</td></tr>
     * <tr><td>{@link #LEAF_DECAY_OFF}</td><td>All leaf blocks are set to <em>not</em> decay regardless of their setting in the custom object</td></tr></table>
     */
    AttributeKey<Integer> ATTRIBUTE_LEAF_DECAY_MODE = new AttributeKey<>("WPObject.leafDecay", LEAF_DECAY_NO_CHANGE); // See LEAF_DECAY_* constants
    /**
     * @deprecated Use {@link #ATTRIBUTE_REPLACE_WITH_AIR_MATERIAL}
     */
    @Deprecated
    AttributeKey<int[]> ATTRIBUTE_REPLACE_WITH_AIR = new AttributeKey<>("WPObject.replaceWithAir");
    /**
     * When set, describes a material which will be replaced with air blocks
     * when this object is rendered. Mainly meant to be able to create voids
     * underground using schematics, which is otherwise not possible since there
     * is no way to tell whether an air block from a schematic is supposed to be
     * placed or not.
     */
    AttributeKey<Material> ATTRIBUTE_REPLACE_WITH_AIR_MATERIAL = new AttributeKey<>("WPObject.replaceWithAirMaterial");
    /**
     * When set, the blocks on the lowest level of the object will be copied
     * downwards until they meet a solid block, if they end up being placed
     * floating in the air. This allows objects to have "legs", "roots" or a
     * "foundation" which will be extended by WorldPainter to meet the ground.
     */
    AttributeKey<Boolean> ATTRIBUTE_EXTEND_FOUNDATION     = new AttributeKey<>("WPObject.extendFoundation", false);
    AttributeKey<Boolean> ATTRIBUTE_RANDOM_ROTATION_ONLY  = new AttributeKey<>("WPObject.randomRotationOnly", false);
    AttributeKey<Boolean> ATTRIBUTE_RANDOM_MIRRORING_ONLY = new AttributeKey<>("WPObject.randomMirroringOnly", false);
    AttributeKey<Integer> ATTRIBUTE_HEIGHT_MODE           = new AttributeKey<>("WPObject.heightMode", HEIGHT_MODE_TERRAIN);
    AttributeKey<Integer> ATTRIBUTE_VERTICAL_OFFSET       = new AttributeKey<>("WPObject.verticalOffset", 0);
    AttributeKey<Integer> ATTRIBUTE_Y_VARIATION           = new AttributeKey<>("WPObject.yVariation", 0);
    /**
     * Whether certain connecting blocks such as fences, glass panes and iron bars should have their west, north, east
     * and south properties automatically set if there is a connecting block in that direction. Does not currently
     * include stone walls. Default value: {@code false}.
     */
    AttributeKey<Boolean> ATTRIBUTE_CONNECT_BLOCKS        = new AttributeKey<>("WPObject.connectBlocks", false);
    /**
     * Whether the {@link Material#WATERLOGGED} property of the blocks should be automatically managed (set if there is
     * already water where the blocks are placed and reset if the target location is dry) or exported as set in the
     * object.
     */
    AttributeKey<Boolean> ATTRIBUTE_MANAGE_WATERLOGGED    = new AttributeKey<>("WPObject.manageWaterlogged", true);

    @FunctionalInterface
    interface BlockVisitor {
        boolean visitBlock(WPObject object, int x, int y, int z, Material material);
    }
}