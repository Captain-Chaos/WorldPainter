/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

/**
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
     * Get the material to place at the specified relative coordinates. Should
     * only be invoked for coordinates for which {@link #getMask(int, int, int)}
     * returns <code>true</code>.
     * 
     * @param x The relative X coordinate.
     * @param y The relative Y coordinate.
     * @param z The relative Z coordinate.
     * @return The material to place at the specified relative coordinates.
     */
    Material getMaterial(int x, int y, int z);
    
    /**
     * Determine whether a block should be placed at the specified relative
     * coordinates.
     * 
     * @param x The relative X coordinate.
     * @param y The relative Y coordinate.
     * @param z The relative Z coordinate.
     * @return <code>true</code> if a block should be placed at the specified
     *     relative coordinates.
     */
    boolean getMask(int x, int y, int z);
    
    /**
     * Get any entities contained in the object. The entities' coordinates
     * should be relative to the object, not absolute.
     * 
     * @return Any entities contained in the object. May be <code>null</code>.
     */
    List<Entity> getEntities();
    
    /**
     * Get any tile entities contained in the object. The entities' coordinates
     * should be relative to the object, not absolute.
     * 
     * @return Any tile entities contained in the object. May be
     *     <code>null</code>.
     */
    List<TileEntity> getTileEntities();
    
    /**
     * Get a live view of the object metadata.
     * 
     * @return A live view of the object metadata. May be <code>null</code>.
     */
    Map<String, Serializable> getAttributes();
    
    /**
     * Convencience method for getting the value of an attribute stored in the
     * external metadata, if any. Should return the value of the attribute if it
     * is present, or the specified default value if it is not.
     * 
     * @param <T> The type of the attribute.
     * @param key The key of the attribute.
     * @param _default The value to return if the attribute is not set.
     * @return The value of the specified attribute, or the specified default
     *     value if the attribute is not set.
     */
    <T extends Serializable> T getAttribute(String key, T _default);
    
    /**
     * Store external metadata about the object.
     * 
     * @param attributes The external metadata to store.
     */
    void setAttributes(Map<String, Serializable> attributes);
    
    /**
     * Convenience method for setting the value of an attribute stored in the
     * external metadata, if any. Setting the value to <code>null</code> will
     * delete the attribute from the store. If the store becomes empty it is
     * deleted entirely.
     * 
     * @param key The key of the attribute to set or delete.
     * @param value The value of the attribute to set, or <code>null</code> to
     *     delete it.
     */
    void setAttribute(String key, Serializable value);
    
    /**
     * Create a clone of the object. The block data is immutable so may be
     * shared, but all mutable settings are copied so they can be changed
     * independently.
     * 
     * @return A clone of the object.
     */
    WPObject clone();
    
    // Standard attribute keys
    static final String ATTRIBUTE_FILE             = "WPObject.file";            // Type String
    static final String ATTRIBUTE_OFFSET           = "WPObject.offset";          // Type Point3i
    static final String ATTRIBUTE_RANDOM_ROTATION  = "WPObject.randomRotation";  // Type Boolean
    static final String ATTRIBUTE_NEEDS_FOUNDATION = "WPObject.needsFoundation"; // Type Boolean; default: true
    static final String ATTRIBUTE_SPAWN_IN_WATER   = "WPObject.spawnInWater";    // Type Boolean; default: false
    static final String ATTRIBUTE_SPAWN_IN_LAVA    = "WPObject.spawnInLava";     // Type Boolean; default: false
    static final String ATTRIBUTE_SPAWN_ON_LAND    = "WPObject.spawnOnLand";     // Type Boolean; default: true
    static final String ATTRIBUTE_SPAWN_ON_WATER   = "WPObject.spawnOnWater";    // Type Boolean; default: false
    static final String ATTRIBUTE_SPAWN_ON_LAVA    = "WPObject.spawnOnLava";     // Type Boolean; default: false
    static final String ATTRIBUTE_FREQUENCY        = "WPObject.frequency";       // Type Integer
    /**
     * Collision mode. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td>{@link #COLLISION_MODE_ALL}</td><td>Will collide with (and therefore not render) any above ground block other than air</td></tr>
     * <tr><td><strong>{@link #COLLISION_MODE_SOLID}</strong></td><td>Will collide with (and therefore not render) any above ground <em>solid</em> block (i.e. not air, grass, water, flowers, leaves, etc.). Default value</td></tr>
     * <tr><td>{@link #COLLISION_MODE_NONE}</td><td>Will not collide with <em>any</em> above ground block (and therefore intersect any other object already there!)</td></tr></table>
     */
    static final String ATTRIBUTE_COLLISION_MODE   = "WPObject.collisionMode";   // Type Integer; see COLLISION_MODE_* constants
    /**
     * Underground rendering mode. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td><strong>{@link #COLLISION_MODE_ALL}</strong></td><td>Every underground block belonging to the object will be rendered (including air blocks), regardless of what is already there. Default value</td></tr>
     * <tr><td>{@link #COLLISION_MODE_SOLID}</td><td>Every <em>solid</em> (i.e. not air, grass, water, flowers, leaves, etc.) underground block belonging to the object will be rendered regardless of what is already there. Non-solid blocks will be rendered only if the existing block is air</td></tr>
     * <tr><td>{@link #COLLISION_MODE_NONE}</td><td>Underground blocks belonging to the object will only be rendered if the existing block is air</td></tr></table>
     */
    static final String ATTRIBUTE_UNDERGROUND_MODE = "WPObject.undergroundMode"; // Type Integer; see COLLISION_MODE_* constants
    /**
     * Whether to change leaf blocks so that they do or do not decay. Possible values:
     * 
     * <p><table><tr><th>Value</th><th>Meaning</th></tr>
     * <tr><td><strong>{@link #LEAF_DECAY_NO_CHANGE}</strong></td><td>Leaf blocks are copied unchanged from the custom object. Default value</td></tr>
     * <tr><td>{@link #LEAF_DECAY_ON}</td><td>All leaf blocks are set to decay regardless of their setting in the custom object</td></tr>
     * <tr><td>{@link #LEAF_DECAY_OFF}</td><td>All leaf blocks are set to <em>not</em> decay regardless of their setting in the custom object</td></tr></table>
     */
    static final String ATTRIBUTE_LEAF_DECAY_MODE  = "WPObject.leafDecay";       // Type Integer; see LEAF_DECAY_* constants
    /**
     * When set, describes a block ID (index 0) and data (index 1) combination
     * which will be replaced with air blocks when this object is rendered.
     * Mainly meant to be able to create voids underground using schematics,
     * which is otherwise not possible since there is no way to tell whether an
     * air block from a schematic is supposed to be placed or not.
     */
    static final String ATTRIBUTE_REPLACE_WITH_AIR = "WPObject.replaceWithAir";  // Type int[]; default: null
    
    static final int COLLISION_MODE_ALL   = 1;
    static final int COLLISION_MODE_SOLID = 2;
    static final int COLLISION_MODE_NONE  = 3;
    
    static final int LEAF_DECAY_NO_CHANGE = 1;
    static final int LEAF_DECAY_ON        = 2;
    static final int LEAF_DECAY_OFF       = 3;
}