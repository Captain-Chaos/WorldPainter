/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pepsoft.util.CSVDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.HorizontalOrientationScheme.CARDINAL_DIRECTIONS;
import static org.pepsoft.minecraft.HorizontalOrientationScheme.STAIR_CORNER;
import static org.pepsoft.util.MathUtils.mod;

/**
 * A representation of a Minecraft material, or one possible in- game block
 * type. Implements the Enumeration pattern, meaning there is only ever one
 * instance of this class for each block type (including properties), allowing
 * use of the equals operator (==) for comparing instances.
 *
 * <p>Supports modern post-Minecraft 1.13 materials based on names and
 * properties, as well as legacy pre-1.13 materials based on numerical IDs, and
 * provides continuity between them.
 *
 * @author pepijn
 */
@SuppressWarnings("deprecation") // only deprecated for client code
public final class Material implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Material.class);

    /**
     * Legacy constructor, which creates a pre-Minecraft 1.13 block type from a
     * block ID and data value.
     *
     * @param blockType The block ID of the legacy block for which to create a
     *                  material.
     * @param data The data value of the legacy block for which to create a
     *             material.
     */
    @SuppressWarnings("unchecked") // Guaranteed by contents of file
    private Material(int blockType, int data) {
        this.blockType = blockType;
        this.data = data;
        index = (blockType << 4) | data;

        Map<String, Object> blockSpec = LEGACY_BLOCK_SPECS_BY_COMBINED_ID.get(index);
        if (blockSpec != null) {
            String name = ((String) blockSpec.get("name")).intern();
            int p = name.indexOf(':');
            if (p != -1) {
                namespace = name.substring(0, p).intern();
                simpleName = name.substring(p + 1).intern();
            } else {
                namespace = null;
                simpleName = name;
            }
            Map<String, String> properties;
            if (blockSpec.containsKey("properties")) {
                properties = new HashMap<>();
                for (Map.Entry<String, String> entry: ((Map<String, String>) blockSpec.get("properties")).entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    properties.put(key, value);
                }
            } else {
                properties = null;
            }
            identity = new Identity(name, properties);
        } else {
            namespace = LEGACY;
            simpleName = ("block_" + blockType).intern();
            identity = new Identity(namespace + ":" + simpleName, singletonMap("data_value", Integer.toString(data)));
        }
        name = identity.name;
        stringRep = createStringRep();
        legacyStringRep = createLegacyStringRep();
        horizontalOrientationSchemes = determineHorizontalOrientations(identity);
        verticalOrientationScheme = determineVerticalOrientation(identity);

        Map<String, Object> spec = findSpec(identity);
        if (spec != null) {
            opacity = (int) spec.get("opacity");
            transparent = (opacity == 0);
            translucent = (opacity < 15);
            opaque = (opacity == 15);
            terrain = (boolean) spec.get("terrain");
            insubstantial = (boolean) spec.get("insubstantial");
            veryInsubstantial = (boolean) spec.get("veryInsubstantial");
            solid = !veryInsubstantial;
            resource = (boolean) spec.get("resource");
            tileEntity = (boolean) spec.get("tileEntity");
            treeRelated = (boolean) spec.get("treeRelated");
            vegetation = (boolean) spec.get("vegetation");
            blockLight = (int) spec.get("blockLight");
            lightSource = (blockLight > 0);
            natural = (boolean) spec.get("natural");
            dry = (boolean) spec.get("dry");
            category = determineCategory();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Legacy material " + blockType + ":" + data + " not found in materials database");
            }
            // Use reasonable defaults for unknown blocks
            opacity = 0;
            transparent = true;
            translucent = true;
            opaque = false;
            terrain = false;
            insubstantial = false;
            veryInsubstantial = false;
            solid = true;
            resource = false;
            tileEntity = false;
            treeRelated = false;
            vegetation = false;
            blockLight = 0;
            lightSource = false;
            natural = false;
            dry = true;
            category = CATEGORY_UNKNOWN;
        }

        if (namespace != null) {
            ALL_NAMESPACES.add(namespace);
            SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, name -> new HashSet<>(singleton(name))).add(simpleName);
        }
        if (! DEFAULT_MATERIALS_BY_NAME.containsKey(identity.name)) {
            DEFAULT_MATERIALS_BY_NAME.put(identity.name, this);
        }
    }

    /**
     * Creates a new material based on a block name and optionally one or more
     * properties in the form of an {@link Identity} object.
     *
     * @param identity The identity of the material to create.
     */
    @SuppressWarnings("unchecked") // Guaranteed by contents of file
    private Material(Identity identity) {
        // See if this modern material matches a legacy one to set a block type
        // and data value for backwards compatibility
        int legacyIndex = -1;
        if (LEGACY_BLOCK_SPECS_BY_NAME.containsKey(identity.name)) {
            blockSpecs:
            for (Map<String, Object> blockSpec: LEGACY_BLOCK_SPECS_BY_NAME.get(identity.name)) {
                if (blockSpec.containsKey("properties")) {
                    if (identity.properties != null) {
                        // The legacy block spec and supplied identify have
                        // properties; check if they all match; if so we can use the
                        // corresponding block ID and data value
                        for (Map.Entry<String, String> entry: ((Map<String, String>) blockSpec.get("properties")).entrySet()) {
                            if (!entry.getValue().equals(identity.properties.get(entry.getKey()))) {
                                continue blockSpecs;
                            }
                        }
                        // If we reach here, all properties matched
                        legacyIndex = (((Number) blockSpec.get("blockId")).intValue() << 4) | ((Number) blockSpec.get("dataValue")).intValue();
                        break;
                    }
                } else {
                    // The legacy block spec has no properties, so the name
                    // match should suffice. // TODO: what if it doesn't? What if the specified identity has properties?
                    legacyIndex = (((Number) blockSpec.get("blockId")).intValue() << 4) | ((Number) blockSpec.get("dataValue")).intValue();
                    break;
                }
            }
        }
        index = legacyIndex;
        if (index != -1) {
            blockType = index >> 4;
            data = index & 0xf;
            if (logger.isDebugEnabled()) {
                logger.debug("Matched " + identity + " to " + BLOCK_TYPE_NAMES[blockType] + "(" + blockType + "):" + data);
            }
        } else {
            blockType = -1;
            data = -1;
            if (logger.isDebugEnabled()) {
                logger.debug("Did not match " + identity + " to legacy block");
            }
        }

        this.identity = identity;
        name = identity.name;
        int p = name.indexOf(':');
        if (p != -1) {
            namespace = name.substring(0, p).intern();
            simpleName = name.substring(p + 1).intern();
        } else {
            namespace = null;
            simpleName = name;
        }
        stringRep = createStringRep();
        legacyStringRep = createLegacyStringRep();
        horizontalOrientationSchemes = determineHorizontalOrientations(identity);
        verticalOrientationScheme = determineVerticalOrientation(identity);

        Map<String, Object> spec = findSpec(identity);
        if (spec != null) {
            opacity = (int) spec.get("opacity");
            transparent = (opacity == 0);
            translucent = (opacity < 15);
            opaque = (opacity == 15);
            terrain = (boolean) spec.get("terrain");
            insubstantial = (boolean) spec.get("insubstantial");
            veryInsubstantial = (boolean) spec.get("veryInsubstantial");
            solid = !veryInsubstantial;
            resource = (boolean) spec.get("resource");
            tileEntity = (boolean) spec.get("tileEntity");
            treeRelated = (boolean) spec.get("treeRelated");
            vegetation = (boolean) spec.get("vegetation");
            blockLight = (int) spec.get("blockLight");
            lightSource = (blockLight > 0);
            natural = (boolean) spec.get("natural");
            dry = (boolean) spec.get("dry");
            category = determineCategory();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Modern material " + identity + " not found in materials database");
            }
            // Use reasonable defaults for unknown blocks
            opacity = 0;
            transparent = true;
            translucent = true;
            opaque = false;
            terrain = false;
            insubstantial = false;
            veryInsubstantial = false;
            solid = true;
            resource = false;
            tileEntity = false;
            treeRelated = false;
            vegetation = false;
            blockLight = 0;
            lightSource = false;
            natural = false;
            dry = true;
            category = CATEGORY_UNKNOWN;
        }

        ALL_NAMESPACES.add(namespace);
        SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, name -> new HashSet<>(singleton(name))).add(simpleName);
        if (! DEFAULT_MATERIALS_BY_NAME.containsKey(name)) {
            DEFAULT_MATERIALS_BY_NAME.put(name, this);
        }
    }

    @SuppressWarnings("unchecked") // Guaranteed by code
    private Map<String, Object> findSpec(Identity identity) {
        Set<Map<String, Object>> specs = MATERIAL_SPECS.get(identity.name);
        if (specs != null) {
            if (specs.size() == 1) {
                return specs.iterator().next();
            } else {
                // There are multiple specs; find a matching one
                specs:
                for (Map<String, Object> spec: specs) {
                    // The spec must specify properties (otherwise there could
                    // not be multiple for the same name), make sure they match
                    // the identity
                    Set<String> properties = (Set<String>) spec.get("properties");
                    for (String property: properties) {
                        int p = property.indexOf('=');
                        if (p != -1) {
                            // The spec specifies a specific value; check that
                            // the identity has the property and it is set to
                            // that value
                            String key = property.substring(0, p);
                            String value = property.substring(p + 1);
                            if (!identity.containsPropertyWithValue(key, value)) {
                                continue specs;
                            }
                        } else {
                            // The spec just specifies a property name; check
                            // that the identity has that property
                            if (!identity.properties.containsKey(property)) {
                                continue specs;
                            }
                        }
                    }
                    // If we reach here all properties matched
                    return spec;
                }
                // If we reach here none of the specs matched
                if (logger.isDebugEnabled()) {
                    logger.debug("There were multiple specs for identity " + identity + " but its properties did not match any of them");
                }
                return null;
            }
        } else {
            // If we reach here there are no specs for this identity's name
            return null;
        }
    }

    /**
     * Get the properties of this material.
     *
     * @return The properties of this material. May be <code>null</code>.
     */
    public Map<String, String> getProperties() {
        return identity.properties;
    }

    /**
     * Indicates whether a specific property is present on this material.
     *
     * @param property The property to check for presence.
     * @return <code>true</code> if the specified property is present.
     */
    public boolean hasProperty(Property<?> property) {
        return (identity.properties != null) && identity.properties.containsKey(property.name);
    }

    /**
     * Get the value of a property as the correct type. Convenience method which
     * transforms the property value from a string using an instance of the
     * {@link Property} helper class.
     *
     * @param property The property helper corresponding to the property of
     *                 which to get the value.
     * @param <T> The property type.
     * @return The value of the specified property transformed to the specified
     * type.
     */
    public <T> T getProperty(Property<T> property) {
        return (identity.properties != null) ? property.fromString(identity.properties.get(property.name)) : null;
    }

    /**
     * Get the value of a property as the correct type, or a default value if
     * the property is not set on the material. Convenience method which
     * transforms the property value from a string using an instance of the
     * {@link Property} helper class.
     *
     * @param property The property helper corresponding to the property of
     *                 which to get the value.
     * @param defaultValue The default value to return if the specified property
     *                     is not set on the material.
     * @param <T> The property type.
     * @return The value of the specified property transformed to the specified
     * type.
     */
    @SuppressWarnings("unchecked") // Responsibility of client
    public <T> T getProperty(Property<T> property, T defaultValue) {
        if (identity.properties != null) {
            String value = identity.properties.get(property.name);
            return (value != null) ? property.fromString(value) : defaultValue;
        } else {
            return defaultValue;
        }
    }

    /**
     * Convenience method to check whether a boolean-typed property is present
     * and set.
     *
     * @param property  The property to check for.
     * @return <code>true</code> if the property is present and set to
     * <code>true</code>.
     */
    public boolean is(Property<Boolean> property) {
        if (identity.properties != null) {
            return "true".equals(identity.properties.get(property.name));
        } else {
            return false;
        }
    }

    /**
     * Returns a material identical to this one, except with the specified
     * property set to the specified value.
     *
     * @param property The property that should be set.
     * @param value The value to which it should be set.
     * @return A material identical to this one, except with the specified
     * property set.
     */
    public <T> Material withProperty(Property<T> property, T value) {
        Map<String, String> newProperties = new HashMap<>();
        if (identity.properties != null) {
            newProperties.putAll(identity.properties);
        }
        newProperties.put(property.name, value.toString());
        return get(identity.name, newProperties);
    }

    /**
     * Indicates whether a specific property is present on this material.
     *
     * @param name The name of the property to check for presence.
     * @return <code>true</code> if the specified property is present.
     */
    public boolean hasProperty(String name) {
        return (identity.properties != null) && identity.properties.containsKey(name);
    }

    /**
     * Get the value of a property as a string.
     *
     * @param name The name of the property of which to get the value.
     * @return The value of the specified property as a string.
     */
    public String getProperty(String name) {
        return (identity.properties != null) ? identity.properties.get(name) : null;
    }

    /**
     * Returns a material identical to this one, except with the specified
     * property set to the specified value.
     *
     * @param name The name of the property that should be set.
     * @param value The value to which it should be set.
     * @return A material identical to this one, except with the specified
     * property set.
     */
    public Material withProperty(String name, String value) {
        Map<String, String> newProperties = new HashMap<>();
        if (identity.properties != null) {
            newProperties.putAll(identity.properties);
        }
        newProperties.put(name, value);
        return get(identity.name, newProperties);
    }

    /**
     * Returns a material identical to this one, except with the specified
     * properties set to the specified values. This variant takes four
     * properties.
     *
     * @param name1 The name of the first property that should be set.
     * @param value1 The value to which the first property should be set.
     * @param name2 The name of the second property that should be set.
     * @param value2 The value to which the second property should be set.
     * @param name3 The name of the third property that should be set.
     * @param value3 The value to which the third property should be set.
     * @param name4 The name of the fourth property that should be set.
     * @param value4 The value to which the fourth property should be set.
     * @return A material identical to this one, except with the specified
     * properties set.
     */
    public Material withProperties(String name1, String value1, String name2, String value2, String name3, String value3, String name4, String value4) {
        Map<String, String> newProperties = new HashMap<>();
        if (identity.properties != null) {
            newProperties.putAll(identity.properties);
        }
        newProperties.put(name1, value1);
        newProperties.put(name2, value2);
        newProperties.put(name3, value3);
        newProperties.put(name4, value4);
        return get(identity.name, newProperties);
    }

    /**
     * Get the cardinal direction this block is pointing, if applicable.
     * 
     * @return The cardinal direction in which this block is pointing, or
     *     <code>null</code> if it has no direction, or is not pointing in a
     *     cardinal direction (but for instance up or down)
     */
    public Direction getDirection() {
        if (horizontalOrientationSchemes != null) {
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                Direction direction = horizontalOrientationScheme.getDirection(this);
                if (direction != null) {
                    return direction;
                }
            }
        }
        return null;
    }
    
    /**
     * Get a material that is pointing in the specified direction, if applicable
     * for the block type. Throws an exception otherwise.
     * 
     * @param direction The direction in which the returned material should
     *     point
     * @return A material with the same block type, but pointing in the
     *     specified direction
     * @throws IllegalArgumentException If this block type does not have the
     *     concept of direction
     */
    public Material setDirection(Direction direction) {
        if (horizontalOrientationSchemes != null) {
            Material material = this;
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                material = horizontalOrientationScheme.setDirection(material, direction);
            }
            return material;
        }
        throw new IllegalArgumentException("Block type " + blockType + " has no direction");
    }
    
    /**
     * If applicable, return a Material that is rotated a specific number of
     * quarter turns.
     * 
     * @param steps The number of 90 degree turns to turn the material clockwise
     *              (when viewed from above). May be negative to turn the
     *              material anti clockwise
     * @return The rotated material (or the same one if rotation does not apply
     * to this material)
     */
    public Material rotate(int steps) {
        if ((horizontalOrientationSchemes != null) && ((steps % 4) != 0)) {
            Material material = this;
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                material = horizontalOrientationScheme.rotate(material, steps);
            }
            return material;
        }
        return this;
    }
    
    /**
     * If applicable, return a Material that is the mirror image of this one in
     * a specific axis.
     * 
     * @param axis Indicates the axis in which to mirror the material.
     * @return The mirrored material (or the same one if mirroring does not
     *     apply to this material)
     */
    public Material mirror(Direction axis) {
        if (horizontalOrientationSchemes != null) {
            Material material = this;
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                material = horizontalOrientationScheme.mirror(material, axis);
            }
            return material;
        }
        return this;
    }

    /**
     * Gets a vertically mirrored version of the material.
     *
     * @return A vertically mirrored version of the material.
     */
    public Material invert() {
        if (verticalOrientationScheme != null) {
            switch (verticalOrientationScheme) {
                case HALF:
                    return withProperty(HALF, getProperty(HALF).equals("top") ? "bottom" : "top");
                case TYPE:
                    return withProperty(TYPE, getProperty(TYPE).equals("top") ? "bottom" : "top");
                case UP:
                    return withProperty(UP, ! getProperty(UP));
                default:
                    throw new InternalError();
            }
        }
        return this;
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name The name to test this material for.
     * @return <code>true</code> if the material has the specified name.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamed(String name) {
        return (name == this.name) || name.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name1 One name to test this material for.
     * @param name2 Another name to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(String name1, String name2) {
        return (name1 == this.name)
            || (name2 == this.name)
            || name1.equals(this.name)
            || name2.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name1 One name to test this material for.
     * @param name2 Another name to test this material for.
     * @param name3 Another name to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(String name1, String name2, String name3) {
        return (name1 == this.name)
                || (name2 == this.name)
                || (name3 == this.name)
                || name1.equals(this.name)
                || name2.equals(this.name)
                || name3.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name1 One name to test this material for.
     * @param name2 Another name to test this material for.
     * @param name3 Another name to test this material for.
     * @param name4 Another name to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(String name1, String name2, String name3, String name4) {
        return (name1 == this.name)
                || (name2 == this.name)
                || (name3 == this.name)
                || (name4 == this.name)
                || name1.equals(this.name)
                || name2.equals(this.name)
                || name3.equals(this.name)
                || name4.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name1 One name to test this material for.
     * @param name2 Another name to test this material for.
     * @param name3 Another name to test this material for.
     * @param name4 Another name to test this material for.
     * @param name5 Another name to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(String name1, String name2, String name3, String name4, String name5) {
        return (name1 == this.name)
                || (name2 == this.name)
                || (name3 == this.name)
                || (name4 == this.name)
                || (name5 == this.name)
                || name1.equals(this.name)
                || name2.equals(this.name)
                || name3.equals(this.name)
                || name4.equals(this.name)
                || name5.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param names The names to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(String... names) {
        for (String name: names) {
            if ((name == this.name) || name.equals(this.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param names The names to test this material for.
     * @return <code>true</code> if the material has one of the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedOneOf(Collection<String> names) {
        for (String name: names) {
            if ((name == this.name) || name.equals(this.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name The name to test this material for.
     * @return <code>true</code> if the material <em>does not</em> have the
     * specified name.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNotNamed(String name) {
        return (name != this.name) && (! name.equals(this.name));
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param names The names to test this material for.
     * @return <code>true</code> if the material <em>does not</em> have any of
     * the specified names.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNotNamedOneOf(String... names) {
        for (String name: names) {
            if ((name == this.name) || name.equals(this.name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two materials in name only, disregarding their properties.
     *
     * @param material The material to compare this material with.
     * @return <code>true</code> if the specified material has the same name as
     * this one.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedSameAs(Material material) {
        return (material.name == this.name) || material.name.equals(this.name);
    }

    /**
     * Compare two materials in name only, disregarding their properties.
     *
     * @param material The material to compare this material with.
     * @return <code>true</code> if the specified material <em>does not</em>
     * have the same name as this one.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNotNamedSameAs(Material material) {
        return (material.name != this.name) && (! material.name.equals(this.name));
    }

    private HorizontalOrientationScheme[] determineHorizontalOrientations(Identity identity) {
        if (identity.properties != null) {
            List<HorizontalOrientationScheme> horizontalOrientationSchemes = new ArrayList<>();
            if (identity.properties.keySet().containsAll(ImmutableSet.of("north", "east", "south", "west"))) {
                horizontalOrientationSchemes.add(CARDINAL_DIRECTIONS);
            }
            if (identity.containsPropertyWithValues(MC_AXIS, "x", "y", "z")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.AXIS);
            }
            if (identity.containsPropertyWithValues(MC_FACING, "north", "east", "south", "west")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.FACING);
            }
            if (identity.containsPropertyWithValues(MC_ROTATION, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.ROTATION);
            }
            if (identity.containsPropertyWithValues(MC_SHAPE, "east_west", "south_west", "north_south", "north_east", "north_west", "south_east", "ascending_north", "ascending_east", "ascending_south", "ascending_west")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.SHAPE);
            }
            if (identity.containsPropertyWithValues(MC_SHAPE, "inner_left", "inner_right", "outer_left", "outer_right")) {
                horizontalOrientationSchemes.add(STAIR_CORNER);
            }
            if (identity.containsPropertyWithValues(MC_TYPE, "left", "right")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.TYPE);
            }
            if (identity.containsPropertyWithValues(MC_HINGE, "left", "right")) {
                horizontalOrientationSchemes.add(HorizontalOrientationScheme.HINGE);
            }
            if (horizontalOrientationSchemes.isEmpty()) {
                return null;
            } else {
                return horizontalOrientationSchemes.toArray(new HorizontalOrientationScheme[horizontalOrientationSchemes.size()]);
            }
        } else {
            return null;
        }
    }

    private VerticalOrientationScheme determineVerticalOrientation(Identity identity) {
        if (identity.containsPropertyWithValues("half", "top", "bottom")) {
            return VerticalOrientationScheme.HALF;
        } else if (identity.containsPropertyWithValues("up", "true", "false")) {
            return VerticalOrientationScheme.UP;
        } else if (identity.containsPropertyWithValues("type", "top", "bottom")) {
            return VerticalOrientationScheme.TYPE;
        } else {
            return null;
        }
    }

    @SuppressWarnings("StringEquality") // interned
    private int determineCategory() {
        // Determine the category
        if ((name == MC_AIR) || (name == MC_CAVE_AIR)) {
            return CATEGORY_AIR;
        } else if ((name == MC_WATER) || (name == MC_LAVA)) {
            return CATEGORY_FLUID;
        } else if (veryInsubstantial) {
            return CATEGORY_INSUBSTANTIAL;
        } else if (! natural) {
            return CATEGORY_MAN_MADE;
        } else if (resource) {
            return CATEGORY_RESOURCE;
        } else {
            return CATEGORY_NATURAL_SOLID;
        }
    }

    private String createStringRep() {
        StringBuilder sb = new StringBuilder();
        if (! namespace.equals(MINECRAFT)) {
            sb.append(namespace);
            sb.append(':');
        }
        sb.append(simpleName);
        if (identity.properties != null) {
            boolean[] first = {true};
            identity.properties.forEach((key, value) -> {
                if (value.equals("false") || value.equals("0")) {
                    // Skip
                } else {
                    if (first[0]) {
                        sb.append('(');
                        first[0] = false;
                    } else {
                        sb.append(", ");
                    }
                    if (value.equals("true")) {
                        sb.append(key);
                    } else {
                        sb.append(key);
                        sb.append(": ");
                        sb.append(value);
                    }
                }
            });
            if (! first[0]) {
                sb.append(')');
            }
        }
        return sb.toString();
    }

    private String createLegacyStringRep() {
        if (blockType < 0) {
            return createStringRep();
        } else if ((blockType < BLOCK_TYPE_NAMES.length) && (BLOCK_TYPE_NAMES[blockType] != null)) {
            if (data > 0) {
                return BLOCK_TYPE_NAMES[blockType] + " (" + data + ")";
            } else {
                return BLOCK_TYPE_NAMES[blockType];
            }
        } else if (data > 0) {
            return blockType + " (" + data + ")";
        } else {
            return Integer.toString(blockType);
        }
    }

    private String rotateValue(List<String> values, int index, int steps) {
        return values.get(mod(index + steps, values.size()));
    }

    /**
     * Get a legacy (pre-1.13) material by block ID. The data value is assumed
     * to be zero.
     *
     * @param blockType The block ID.
     * @return The requested material.
     * @deprecated Use {@link #get(Identity)}, {@link #get(String, Map)} or
     * {@link #get(String, Object...)}.
     */
    public static Material get(int blockType) {
        return getByCombinedIndex(blockType << 4);
    }

    /**
     * Get a legacy (pre-1.13) material by block ID and data value.
     *
     * @param blockType The block ID.
     * @param data The data value.
     * @return The requested material.
     * @deprecated Use {@link #get(Identity)}, {@link #get(String, Map)} or
     * {@link #get(String, Object...)}.
     */
    public static Material get(int blockType, int data) {
        return getByCombinedIndex((blockType << 4) | data);
    }

    /**
     * Get a legacy (pre-1.13) material by {@link Block} reference. The data
     * value is assumed to be zero.
     *
     * @param blockType The block type.
     * @return The requested material.
     * @deprecated Use {@link #get(Identity)}, {@link #get(String, Map)} or
     * {@link #get(String, Object...)}.
     */
    public static Material get(Block blockType) {
        return getByCombinedIndex(blockType.id << 4);
    }

    /**
     * Get a legacy (pre-1.13) material by {@link Block} reference and data
     * value.
     *
     * @param blockType The block type.
     * @param data The data value.
     * @return The requested material.
     * @deprecated Use {@link #get(Identity)}, {@link #get(String, Map)} or
     * {@link #get(String, Object...)}.
     */
    public static Material get(Block blockType, int data) {
        return getByCombinedIndex((blockType.id << 4) | data);
    }

    /**
     * Get a legacy (pre-1.13) material corresponding to a combined index
     * consisting of the block ID shifted left four bits and or-ed with the data
     * value. In other words the index is a 16-bit unsigned integer, with bit
     * 0-3 indicating the data value and bit 4-15 indicating the block ID.
     *
     * @param index The combined index of the material to get.
     * @return The indicated material.
     * @deprecated Use {@link #get(Identity)}, {@link #get(String, Map)} or
     * {@link #get(String, Object...)}.
     */
    public static Material getByCombinedIndex(int index) {
        if (index >= LEGACY_MATERIALS.length) {
            return get(new Identity("legacy:block_" + (index >> 4), singletonMap("data_value", Integer.toString(index & 0xf))));
        } else {
            return LEGACY_MATERIALS[index];
        }
    }

    /**
     * Get the single instance of the material with the given identity.
     *
     * @param identity The identity of the material to get.
     * @return The single instance of the specified material.
     */
    public static Material get(Identity identity) {
        synchronized (ALL_MATERIALS) {
            Material material = ALL_MATERIALS.get(identity);
            if (material == null) {
                material = new Material(identity);
                ALL_MATERIALS.put(identity, material);
            }
            return material;
        }
    }

    /**
     * Get the single instance of the material with the given name and
     * properties.
     *
     * @param name The name of the material to get.
     * @param properties The properties of the material to get. May be
     *                   <code>null</code>.
     * @return The single instance of the specified material.
     */
    public static Material get(String name, Map<String, String> properties) {
        return get(new Identity(name, properties));
    }

    /**
     * Get the single instance of the material with the given name and
     * properties.
     *
     * @param name The name of the material to get.
     * @param properties The properties of the material to get, as a list of
     *                   key-value pairs. The keys must be <code>String</code>s.
     *                   May be <code>null</code>.
     * @return The single instance of the specified material.
     */
    public static Material get(String name, Object... properties) {
        Map<String, String> propertyMap;
        if (properties != null) {
            propertyMap = new HashMap<>();
            for (int i = 0; i < properties.length; i += 2) {
                propertyMap.put((String) properties[i], properties[i + 1].toString());
            }
        } else {
            propertyMap = null;
        }
        return get(name, propertyMap);
    }

    /**
     * Get a known material by name only, disregarding the properties.
     *
     * @param name The name of the material to get.
     * @return A material with the specified name and unspecified properties, or
     * <code>null</code> if no material by that name is known.
     */
    public static Material getDefault(String name) {
        return DEFAULT_MATERIALS_BY_NAME.get(name);
    }

    public static Set<String> getAllNamespaces() {
        return Collections.unmodifiableSet(ALL_NAMESPACES);
    }

    public static Set<String> getAllSimpleNamesForNamespace(String namespace) {
        return SIMPLE_NAMES_BY_NAMESPACE.containsKey(namespace) ? Collections.unmodifiableSet(SIMPLE_NAMES_BY_NAMESPACE.get(namespace)) : Collections.EMPTY_SET;
    }

    public static Set<String> getAllNames() {
        return ALL_MATERIALS.values().stream().map(material -> material.name).collect(toSet());
    }

    public static Collection<Material> getAllMaterials() {
        return Collections.unmodifiableCollection(ALL_MATERIALS.values());
    }

    // Object

    public boolean equals(Object o) {
        return (o instanceof Material)
            && identity.equals(((Material) o).identity);
    }

    public int hashCode() {
        return identity.hashCode();
    }

    /**
     * Get the modern style (name and property-based) name of this material. For
     * brevity, the namespace is omitted if it isn't <code>minecraft</code> and
     * properties with value <code>"false"</code> or <code>"0"</code> are also
     * omitted.
     *
     * @return The modern style name of this material.
     */
    @Override
    public String toString() {
        return stringRep;
    }

    /**
     * For legacy materials (pre-1.13; with a numerical block ID), get the
     * legacy style block name for this material. For modern materials, returns
     * the same as {@link #toString()}.
     *
     * @return The legacy (pre-1.13) style block name for this material, if
     * applicable.
     */
    public String toLegacyString() {
        return legacyStringRep;
    }

    private Object readResolve() throws ObjectStreamException {
        // If identity is not set this is a legacy material with only a block ID
        // and data value, so in that case return the corresponding legacy
        // instance
        if (identity != null) {
            return get(identity);
        } else {
            int index = (blockType << 4) | data;
            if (index >= LEGACY_MATERIALS.length) {
                return get(new Identity("legacy:block_" + blockType, singletonMap("data_value", Integer.toString(data))));
            } else {
                return LEGACY_MATERIALS[index];
            }
        }
    }

    /**
     * How much light the block blocks from 0 (fully transparent) to 15 (fully
     * opaque).
     */
    public final transient int opacity;

    /**
     * The name of the block, including the namespace (if present; separated by
     * a colon). This value is guaranteed to be interned, so that it is valid to
     * compare it with <code>String</code> literals or constants using the
     * <code>==</code> operator.
     */
    public final transient String name;

    /**
     * Whether the block is fully transparent ({@link #opacity} == 0)
     */
    public final transient boolean transparent;

    /**
     * Whether the block is translucent ({@link #opacity} < 15)
     */
    public final transient boolean translucent;

    /**
     * Whether the block is fully opaque ({@link #opacity} == 15)
     */
    public final transient boolean opaque;

    /**
     * Whether the block is part of Minecraft-generated natural ground; more
     * specifically whether the block type should be assigned a terrain type
     * when importing a Minecraft map.
     */
    public final transient boolean terrain;

    /**
     * Whether the block is insubstantial, meaning that they are fully
     * transparent, not man-made, removing them would have no effect on the
     * surrounding blocks and be otherwise inconsequential. In other words
     * mostly decorative blocks that users presumably would not mind being
     * removed.
     */
    public final transient boolean insubstantial;

    /**
     * Whether the block is even more insubstantial. Implies
     * {@link #insubstantial} and adds air, water, lava and leaves.
     */
    public final transient boolean veryInsubstantial;

    /**
     * Whether the block is solid (meaning not {@link #insubstantial} or
     * {@link #veryInsubstantial}).
     */
    public final transient boolean solid;

    /**
     * Whether the block is a mineable ore or resource.
     */
    public final transient boolean resource;

    /**
     * Whether the block is a tile entity.
     */
    // TODOMC13 make this valid for MC 1.13 blocks
    public final transient boolean tileEntity;

    /**
     * Whether the block is part of or attached to naturally occurring
     * trees or giant mushrooms. Also includes saplings, but not normal
     * mushrooms.
     */
    public final transient boolean treeRelated;

    /**
     * Whether the block is a plant. Excludes {@link #treeRelated} blocks.
     */
    public final transient boolean vegetation;

    /**
     * The amount of blocklight emitted by this block.
     */
    public final transient int blockLight;

    /**
     * Whether the block is a source of blocklight ({@link #blockLight} > 0).
     */
    public final transient boolean lightSource;

    /**
     * Whether the block can occur as part of a pristine Minecraft-generated
     * landscape, <em>excluding</em> artificial structures such as abandoned
     * mineshafts, villages, temples, strongholds, etc.
     */
    public final transient boolean natural;

    /**
     * Whether the block can occur out of water.
     */
    public final transient boolean dry;

    /**
     * The horizontal orientation scheme(s) detected for this material, or
     * {@code null} if this material has no horizontal orientation, or one could
     * not be determined.
     */
    public final transient HorizontalOrientationScheme[] horizontalOrientationSchemes;

    /**
     * The vertical orientation scheme detected for this material, or
     * {@code null} if this material has no vertical orientation, or one could
     * not be determined.
     */
    public final transient VerticalOrientationScheme verticalOrientationScheme;

    /**
     * Type of block encoded in a single category
     */
    public final transient int category;

    /**
     * The legacy (pre-Minecraft 1.13) block ID of the material. For modern
     * materials which don't correspond to a pre-1.13 block this is -1.
     */
    public final int blockType;

    /**
     * The legacy (pre-Minecraft 1.13) data value of the material. For modern
     * materials which don't correspond to a pre-1.13 block this is -1.
     */
    public final int data;

    public final transient int index;

    /**
     * The simple name (excluding the namespace, i.e. the part after the colon)
     * of this material.
     */
    public final transient String simpleName;

    /**
     * The namespace (i.e. the part before the colon) of this material.
     */
    public final transient String namespace;

    private final Identity identity;
    private final transient String stringRep, legacyStringRep;

    private static final Map<Integer, Map<String, Object>> LEGACY_BLOCK_SPECS_BY_COMBINED_ID = new HashMap<>();
    private static final Map<String, Set<Map<String, Object>>> LEGACY_BLOCK_SPECS_BY_NAME = new HashMap<>();
    private static final Map<String, Set<Map<String, Object>>> MATERIAL_SPECS = new HashMap<>();

    static {
        // Read legacy MC block database
        try (Reader in = new InputStreamReader(Block.class.getResourceAsStream("legacy-mc-blocks.json"), Charset.forName("UTF-8"))) {
            @SuppressWarnings("unchecked") // Guaranteed by contents of file
            List<Object> items = (List<Object>) new JSONParser().parse(in);
            for (Object item: items) {
                if ((item instanceof String) && (((String) item).trim().startsWith("#"))) {
                    // Skip comment
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked") // Guaranteed by contents of file
                    Map<String, Object> blockSpec = (Map<String, Object>) item;
                    String name = (String) blockSpec.get("name");
                    int blockId = ((Number) blockSpec.get("blockId")).intValue();
                    int dataValue = ((Number) blockSpec.get("dataValue")).intValue();
                    LEGACY_BLOCK_SPECS_BY_COMBINED_ID.put((blockId << 4) | dataValue, blockSpec);
                    LEGACY_BLOCK_SPECS_BY_NAME.computeIfAbsent(name, s -> new HashSet<>()).add(blockSpec);
                } else {
                    throw new IllegalArgumentException("Unexpected item encountered: " + item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minecraft block database legacy-mc-blocks.json from classpath", e);
        } catch (ParseException e) {
            throw new RuntimeException("JSON parsing error while reading Minecraft block database legacy-mc-blocks.json from classpath", e);
        }

        // Read MC materials database
        try (Reader in = new InputStreamReader(Block.class.getResourceAsStream("mc-materials.csv"), Charset.forName("UTF-8"))) {
            CSVDataSource csvDataSource = new CSVDataSource();
            csvDataSource.openForReading(in);
            do {
                Map<String, Object> materialSpecs = new HashMap<>();
                String name = csvDataSource.getString("name");
                materialSpecs.put("name", name);
                String str = csvDataSource.getString("properties");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("properties", ImmutableSet.copyOf(str.split(",")));
                }
                materialSpecs.put("opacity", csvDataSource.getInt("opacity"));
                materialSpecs.put("terrain", csvDataSource.getBoolean("terrain"));
                materialSpecs.put("insubstantial", csvDataSource.getBoolean("insubstantial"));
                materialSpecs.put("veryInsubstantial", csvDataSource.getBoolean("veryInsubstantial"));
                materialSpecs.put("resource", csvDataSource.getBoolean("resource"));
                materialSpecs.put("tileEntity", csvDataSource.getBoolean("tileEntity"));
                materialSpecs.put("treeRelated", csvDataSource.getBoolean("treeRelated"));
                materialSpecs.put("vegetation", csvDataSource.getBoolean("vegetation"));
                materialSpecs.put("blockLight", csvDataSource.getInt("blockLight"));
                materialSpecs.put("natural", csvDataSource.getBoolean("natural"));
                materialSpecs.put("dry", csvDataSource.getBoolean("dry"));
                MATERIAL_SPECS.computeIfAbsent(name, s -> new HashSet<>()).add(materialSpecs);
                csvDataSource.next();
            } while (! csvDataSource.isEndOfFile());
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minecraft materials database materials.csv from classpath", e);
        }
    }

    /**
     * To save space we only store the 256-ish vanilla blocks as pre-created
     * legacy materials. 12-bit block ids above 255 are created on the fly.
     */
    private static final Material[] LEGACY_MATERIALS = new Material[4096];
    private static final Map<Identity, Material> ALL_MATERIALS = new HashMap<>();
    private static final Set<String> ALL_NAMESPACES = new HashSet<>();
    private static final Map<String, Set<String>> SIMPLE_NAMES_BY_NAMESPACE = new HashMap<>();
    private static final Map<String, Material> DEFAULT_MATERIALS_BY_NAME = new HashMap<>();

    static {
        for (int i = 0; i < 4096; i++) {
            LEGACY_MATERIALS[i] = new Material(i >> 4, i & 0xF);
            ALL_MATERIALS.put(LEGACY_MATERIALS[i].identity, LEGACY_MATERIALS[i]);
        }
    }

    // Legacy materials (with numerical IDs for compatibility with pre-1.13
    // Minecraft)

    public static final Material AIR = LEGACY_MATERIALS[(BLK_AIR) << 4];
    public static final Material DANDELION = LEGACY_MATERIALS[(BLK_DANDELION) << 4];
    public static final Material ROSE = LEGACY_MATERIALS[(BLK_ROSE) << 4];
    public static final Material GRASS_BLOCK = LEGACY_MATERIALS[(BLK_GRASS) << 4];
    public static final Material DIRT = LEGACY_MATERIALS[(BLK_DIRT) << 4];
    public static final Material STONE = LEGACY_MATERIALS[(BLK_STONE) << 4];
    public static final Material GRANITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_GRANITE)];
    public static final Material DIORITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_DIORITE)];
    public static final Material ANDESITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_ANDESITE)];
    public static final Material COBBLESTONE = LEGACY_MATERIALS[(BLK_COBBLESTONE) << 4];
    public static final Material SNOW = LEGACY_MATERIALS[(BLK_SNOW) << 4];
    public static final Material SNOW_EIGHT_LAYERS = LEGACY_MATERIALS[((BLK_SNOW) << 4) | 7];
    public static final Material DEAD_SHRUBS = LEGACY_MATERIALS[(BLK_DEAD_SHRUBS) << 4];
    public static final Material CACTUS = LEGACY_MATERIALS[(BLK_CACTUS) << 4];
    public static final Material SAND = LEGACY_MATERIALS[(BLK_SAND) << 4];
    public static final Material FIRE = LEGACY_MATERIALS[(BLK_FIRE) << 4];
    public static final Material GLOWSTONE = LEGACY_MATERIALS[(BLK_GLOWSTONE) << 4];
    public static final Material SOUL_SAND = LEGACY_MATERIALS[(BLK_SOUL_SAND) << 4];
    public static final Material LAVA = LEGACY_MATERIALS[(BLK_LAVA) << 4];
    public static final Material NETHERRACK = LEGACY_MATERIALS[(BLK_NETHERRACK) << 4];
    public static final Material END_STONE = LEGACY_MATERIALS[BLK_END_STONE << 4];
    public static final Material CHORUS_PLANT = LEGACY_MATERIALS[BLK_CHORUS_PLANT << 4];
    public static final Material COAL = LEGACY_MATERIALS[(BLK_COAL) << 4];
    public static final Material GRAVEL = LEGACY_MATERIALS[(BLK_GRAVEL) << 4];
    public static final Material REDSTONE_ORE = LEGACY_MATERIALS[(BLK_REDSTONE_ORE) << 4];
    public static final Material IRON_ORE = LEGACY_MATERIALS[(BLK_IRON_ORE) << 4];
    public static final Material WATER = LEGACY_MATERIALS[(BLK_WATER) << 4];
    public static final Material GOLD_ORE = LEGACY_MATERIALS[(BLK_GOLD_ORE) << 4];
    public static final Material LAPIS_LAZULI_ORE = LEGACY_MATERIALS[(BLK_LAPIS_LAZULI_ORE) << 4];
    public static final Material DIAMOND_ORE = LEGACY_MATERIALS[(BLK_DIAMOND_ORE) << 4];
    public static final Material BEDROCK = LEGACY_MATERIALS[(BLK_BEDROCK) << 4];
    public static final Material STATIONARY_WATER = LEGACY_MATERIALS[(BLK_STATIONARY_WATER) << 4];
    public static final Material STATIONARY_LAVA = LEGACY_MATERIALS[(BLK_STATIONARY_LAVA) << 4];
    public static final Material SNOW_BLOCK = LEGACY_MATERIALS[(BLK_SNOW_BLOCK) << 4];
    public static final Material SANDSTONE = LEGACY_MATERIALS[(BLK_SANDSTONE) << 4];
    public static final Material CLAY = LEGACY_MATERIALS[(BLK_CLAY) << 4];
    public static final Material MOSSY_COBBLESTONE = LEGACY_MATERIALS[(BLK_MOSSY_COBBLESTONE) << 4];
    public static final Material OBSIDIAN = LEGACY_MATERIALS[(BLK_OBSIDIAN) << 4];
    public static final Material FENCE = LEGACY_MATERIALS[(BLK_FENCE) << 4];
    public static final Material GLASS_PANE = LEGACY_MATERIALS[(BLK_GLASS_PANE) << 4];
    public static final Material STONE_BRICKS = LEGACY_MATERIALS[(BLK_STONE_BRICKS) << 4];
    public static final Material BRICKS = LEGACY_MATERIALS[(BLK_BRICKS) << 4];
    public static final Material COBWEB = LEGACY_MATERIALS[(BLK_COBWEB) << 4];
    public static final Material DIAMOND_BLOCK = LEGACY_MATERIALS[(BLK_DIAMOND_BLOCK) << 4];
    public static final Material GOLD_BLOCK = LEGACY_MATERIALS[(BLK_GOLD_BLOCK) << 4];
    public static final Material IRON_BLOCK = LEGACY_MATERIALS[(BLK_IRON_BLOCK) << 4];
    public static final Material LAPIS_LAZULI_BLOCK = LEGACY_MATERIALS[(BLK_LAPIS_LAZULI_BLOCK) << 4];
    public static final Material MYCELIUM = LEGACY_MATERIALS[(BLK_MYCELIUM) << 4];
    public static final Material FARMLAND = LEGACY_MATERIALS[(BLK_TILLED_DIRT) << 4];
    public static final Material ICE = LEGACY_MATERIALS[(BLK_ICE) << 4];
    public static final Material FROSTED_ICE = LEGACY_MATERIALS[(BLK_FROSTED_ICE) << 4];
    public static final Material PACKED_ICE = LEGACY_MATERIALS[BLK_PACKED_ICE << 4];
    public static final Material TORCH = LEGACY_MATERIALS[(BLK_TORCH) << 4];
    public static final Material COBBLESTONE_STAIRS = LEGACY_MATERIALS[(BLK_COBBLESTONE_STAIRS) << 4];
    public static final Material GLASS = LEGACY_MATERIALS[(BLK_GLASS) << 4];
    public static final Material WOODEN_STAIRS = LEGACY_MATERIALS[(BLK_WOODEN_STAIRS) << 4];
    public static final Material CHEST_NORTH = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (2)];
    public static final Material CHEST_SOUTH = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (3)];
    public static final Material CHEST_WEST = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (4)];
    public static final Material CHEST_EAST = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (5)];
    public static final Material WALL_SIGN = LEGACY_MATERIALS[(BLK_WALL_SIGN) << 4];
    public static final Material BRICK_STAIRS = LEGACY_MATERIALS[(BLK_BRICK_STAIRS) << 4];
    public static final Material STONE_BRICK_STAIRS = LEGACY_MATERIALS[(BLK_STONE_BRICK_STAIRS) << 4];
    public static final Material LADDER = LEGACY_MATERIALS[(BLK_LADDER) << 4];
    public static final Material TRAPDOOR = LEGACY_MATERIALS[(BLK_TRAPDOOR) << 4];
    public static final Material WHEAT = LEGACY_MATERIALS[(BLK_WHEAT) << 4];
    public static final Material LILY_PAD = LEGACY_MATERIALS[(BLK_LILY_PAD) << 4];
    public static final Material RED_MUSHROOM = LEGACY_MATERIALS[(BLK_RED_MUSHROOM) << 4];
    public static final Material BROWN_MUSHROOM = LEGACY_MATERIALS[(BLK_BROWN_MUSHROOM) << 4];
    public static final Material SUGAR_CANE = LEGACY_MATERIALS[(BLK_SUGAR_CANE) << 4];
    public static final Material EMERALD_ORE = LEGACY_MATERIALS[(BLK_EMERALD_ORE) << 4];
    public static final Material EMERALD_BLOCK = LEGACY_MATERIALS[(BLK_EMERALD_BLOCK) << 4];
    public static final Material PERMADIRT = LEGACY_MATERIALS[((BLK_DIRT) << 4) | (1)];
    public static final Material PODZOL = LEGACY_MATERIALS[((BLK_DIRT) << 4) | (2)];
    public static final Material RED_SAND = LEGACY_MATERIALS[((BLK_SAND) << 4) | (1)];
    public static final Material HARDENED_CLAY = LEGACY_MATERIALS[(BLK_HARDENED_CLAY) << 4];
    public static final Material WHITE_CLAY = LEGACY_MATERIALS[(BLK_STAINED_CLAY) << 4];
    public static final Material ORANGE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_ORANGE)];
    public static final Material MAGENTA_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_MAGENTA)];
    public static final Material LIGHT_BLUE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material YELLOW_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_YELLOW)];
    public static final Material LIME_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIME)];
    public static final Material PINK_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PINK)];
    public static final Material GREY_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREY)];
    public static final Material LIGHT_GREY_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_GREY)];
    public static final Material CYAN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_CYAN)];
    public static final Material PURPLE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PURPLE)];
    public static final Material BLUE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLUE)];
    public static final Material BROWN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BROWN)];
    public static final Material GREEN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREEN)];
    public static final Material RED_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_RED)];
    public static final Material BLACK_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLACK)];
    public static final Material RED_SANDSTONE = LEGACY_MATERIALS[(BLK_RED_SANDSTONE) << 4];
    public static final Material QUARTZ_ORE = LEGACY_MATERIALS[BLK_QUARTZ_ORE << 4];

    public static final Material GRASS = LEGACY_MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_TALL_GRASS)];
    public static final Material FERN = LEGACY_MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_FERN)];

    public static final Material WOOD_OAK = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_OAK)];
    public static final Material WOOD_BIRCH = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_BIRCH)];
    public static final Material WOOD_PINE = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_PINE)];
    public static final Material WOOD_JUNGLE = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_JUNGLE)];
    public static final Material WOOD_ACACIA = LEGACY_MATERIALS[((BLK_WOOD2) << 4) | (DATA_ACACIA)];
    public static final Material WOOD_DARK_OAK = LEGACY_MATERIALS[((BLK_WOOD2) << 4) | (DATA_DARK_OAK)];

    public static final Material LEAVES_OAK = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_OAK)];
    public static final Material LEAVES_BIRCH = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_BIRCH)];
    public static final Material LEAVES_PINE = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_PINE)];
    public static final Material LEAVES_JUNGLE = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_JUNGLE)];
    public static final Material LEAVES_ACACIA = LEGACY_MATERIALS[((BLK_LEAVES2) << 4) | (DATA_ACACIA)];
    public static final Material LEAVES_DARK_OAK = LEGACY_MATERIALS[((BLK_LEAVES2) << 4) | (DATA_DARK_OAK)];

    public static final Material WOODEN_PLANK_OAK = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_OAK)];
    public static final Material WOODEN_PLANK_BIRCH = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_BIRCH)];
    public static final Material WOODEN_PLANK_PINE = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_PINE)];
    public static final Material WOODEN_PLANK_JUNGLE = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_JUNGLE)];
    public static final Material WOODEN_PLANK_ACACIA = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_ACACIA)];
    public static final Material WOODEN_PLANK_DARK_WOOD = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_DARK_OAK)];

    public static final Material WOOL_WHITE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_WHITE)];
    public static final Material WOOL_ORANGE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_ORANGE)];
    public static final Material WOOL_MAGENTA = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_MAGENTA)];
    public static final Material WOOL_LIGHT_BLUE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material WOOL_YELLOW = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_YELLOW)];
    public static final Material WOOL_LIME = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIME)];
    public static final Material WOOL_PINK = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_PINK)];
    public static final Material WOOL_GREY = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_GREY)];
    public static final Material WOOL_LIGHT_GREY = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_GREY)];
    public static final Material WOOL_CYAN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_CYAN)];
    public static final Material WOOL_PURPLE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_PURPLE)];
    public static final Material WOOL_BLUE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BLUE)];
    public static final Material WOOL_BROWN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BROWN)];
    public static final Material WOOL_GREEN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_GREEN)];
    public static final Material WOOL_RED = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_RED)];
    public static final Material WOOL_BLACK = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BLACK)];

    public static final Material COBBLESTONE_SLAB = LEGACY_MATERIALS[((BLK_SLAB) << 4) | (DATA_SLAB_COBBLESTONE)];

    public static final Material DOOR_OPEN_LEFT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_LEFT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_OPEN_RIGHT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_RIGHT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];
    public static final Material DOOR_CLOSED_LEFT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_LEFT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_CLOSED_RIGHT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_RIGHT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];

    public static final Material BED_FOOT = LEGACY_MATERIALS[((BLK_BED) << 4) | (DATA_BED_FOOT)];
    public static final Material BED_HEAD = LEGACY_MATERIALS[((BLK_BED) << 4) | (DATA_BED_HEAD)];

    public static final Material COCOA_PLANT = LEGACY_MATERIALS[(BLK_COCOA_PLANT) << 4];
    public static final Material COCOA_PLANT_HALF_RIPE = LEGACY_MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x4)];
    public static final Material COCOA_PLANT_RIPE = LEGACY_MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x8)];

    public static final Material PUMPKIN_NO_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NO_FACE)];
    public static final Material PUMPKIN_NORTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NORTH_FACE)];
    public static final Material PUMPKIN_EAST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_EAST_FACE)];
    public static final Material PUMPKIN_SOUTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_SOUTH_FACE)];
    public static final Material PUMPKIN_WEST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_WEST_FACE)];

    // MC 1.13 block property access helpers

    public static final Property<Boolean>   SNOWY       = new Property<>(MC_SNOWY,       Boolean.class);
    public static final Property<Boolean>   NORTH       = new Property<>(MC_NORTH,       Boolean.class);
    public static final Property<Boolean>   EAST        = new Property<>(MC_EAST,        Boolean.class);
    public static final Property<Boolean>   SOUTH       = new Property<>(MC_SOUTH,       Boolean.class);
    public static final Property<Boolean>   WEST        = new Property<>(MC_WEST,        Boolean.class);
    public static final Property<Boolean>   UP          = new Property<>(MC_UP,          Boolean.class);
    public static final Property<Integer>   LAYERS      = new Property<>(MC_LAYERS,      Integer.class);
    public static final Property<String>    HALF        = new Property<>(MC_HALF,        String.class);
    public static final Property<Integer>   LEVEL       = new Property<>(MC_LEVEL,       Integer.class);
    public static final Property<Boolean>   WATERLOGGED = new Property<>(MC_WATERLOGGED, Boolean.class);
    public static final Property<Integer>   AGE         = new Property<>(MC_AGE,         Integer.class);
    public static final Property<Boolean>   PERSISTENT  = new Property<>(MC_PERSISTENT,  Boolean.class);
    public static final Property<Direction> FACING      = new Property<>(MC_FACING,      Direction.class);
    public static final Property<String>    AXIS        = new Property<>(MC_AXIS,        String.class);
    public static final Property<String>    TYPE        = new Property<>(MC_TYPE,        String.class);
    public static final Property<Integer>   PICKLES     = new Property<>(MC_PICKLES,     Integer.class);
    public static final Property<Integer>   MOISTURE    = new Property<>(MC_MOISTURE,    Integer.class);
    public static final Property<Integer>   ROTATION    = new Property<>(MC_ROTATION,    Integer.class);
    public static final Property<String>    SHAPE       = new Property<>(MC_SHAPE,       String.class);
    public static final Property<String>    HINGE       = new Property<>(MC_HINGE,       String.class);

    // Modern materials (based on MC 1.13 block names and properties)

    /**
     * A vine with no directions turned on, which is not a valid block in
     * Minecraft, so you must set at least one direction.
     */
    public static final Material VINE = get(MC_VINE, MC_NORTH, false, MC_EAST, false, MC_SOUTH, false, MC_WEST, false, MC_UP, false);
    public static final Material TERRACOTTA = get(MC_TERRACOTTA);
    public static final Material BLUE_ORCHID = get(MC_BLUE_ORCHID);
    public static final Material ALLIUM = get(MC_ALLIUM);
    public static final Material AZURE_BLUET = get(MC_AZURE_BLUET);
    public static final Material RED_TULIP = get(MC_RED_TULIP);
    public static final Material ORANGE_TULIP = get(MC_ORANGE_TULIP);
    public static final Material WHITE_TULIP = get(MC_WHITE_TULIP);
    public static final Material PINK_TULIP = get(MC_PINK_TULIP);
    public static final Material OXEYE_DAISY = get(MC_OXEYE_DAISY);
    /**
     * Lower half of a sunflower.
     */
    public static final Material SUNFLOWER = get(MC_SUNFLOWER, MC_HALF, "lower");
    /**
     * Lower half of a lilac.
     */
    public static final Material LILAC = get(MC_LILAC, MC_HALF, "lower");
    /**
     * Lower half of tall grass.
     */
    public static final Material TALL_GRASS = get(MC_TALL_GRASS, MC_HALF, "lower");
    /**
     * Lower half of a large fern.
     */
    public static final Material LARGE_FERN = get(MC_LARGE_FERN, MC_HALF, "lower");
    /**
     * Lower half of a rose bush.
     */
    public static final Material ROSE_BUSH = get(MC_ROSE_BUSH, MC_HALF, "lower");
    /**
     * Lower half of a peony.
     */
    public static final Material PEONY = get(MC_PEONY, MC_HALF, "lower");
    public static final Material OAK_SAPLING = get(MC_OAK_SAPLING, MC_STAGE, 0);
    public static final Material DARK_OAK_SAPLING = get(MC_DARK_OAK_SAPLING, MC_STAGE, 0);
    public static final Material PINE_SAPLING = get(MC_SPRUCE_SAPLING, MC_STAGE, 0);
    public static final Material BIRCH_SAPLING = get(MC_BIRCH_SAPLING, MC_STAGE, 0);
    public static final Material JUNGLE_SAPLING = get(MC_JUNGLE_SAPLING, MC_STAGE, 0);
    public static final Material ACACIA_SAPLING = get(MC_ACACIA_SAPLING, MC_STAGE, 0);
    public static final Material CARROTS = get(MC_CARROTS, MC_AGE, 0);
    public static final Material POTATOES = get(MC_POTATOES, MC_AGE, 0);
    /**
     * A pumpkin stem without direction; set the facing property before use.
     */
    public static final Material PUMPKIN_STEM = get(MC_PUMPKIN_STEM, MC_AGE, 0);
    public static final Material MELON_STEM = get(MC_MELON_STEM, MC_AGE, 0);
    public static final Material BEETROOTS = get(MC_BEETROOTS, MC_AGE, 0);
    public static final Material NETHER_WART = get(MC_NETHER_WART, MC_AGE, 0);
    public static final Material CHORUS_FLOWER = get(MC_CHORUS_FLOWER, MC_AGE, 0);
    public static final Material OAK_FENCE = get(MC_OAK_FENCE);
    public static final Material NETHER_BRICK_FENCE = get(MC_NETHER_BRICK_FENCE);
    public static final Material SPRUCE_FENCE = get(MC_SPRUCE_FENCE);
    public static final Material BIRCH_FENCE = get(MC_BIRCH_FENCE);
    public static final Material JUNGLE_FENCE = get(MC_JUNGLE_FENCE);
    public static final Material DARK_OAK_FENCE = get(MC_DARK_OAK_FENCE);
    public static final Material ACACIA_FENCE = get(MC_ACACIA_FENCE);
    public static final Material COBBLESTONE_WALL = get(MC_COBBLESTONE_WALL);
    public static final Material IRON_BARS = get(MC_IRON_BARS);
    public static final Material TUBE_CORAL = get(MC_TUBE_CORAL);
    public static final Material BRAIN_CORAL = get(MC_BRAIN_CORAL);
    public static final Material BUBBLE_CORAL = get(MC_BUBBLE_CORAL);
    public static final Material FIRE_CORAL = get(MC_FIRE_CORAL);
    public static final Material HORN_CORAL = get(MC_HORN_CORAL);
    public static final Material DEAD_TUBE_CORAL = get(MC_DEAD_TUBE_CORAL);
    public static final Material DEAD_BRAIN_CORAL = get(MC_DEAD_BRAIN_CORAL);
    public static final Material DEAD_BUBBLE_CORAL = get(MC_DEAD_BUBBLE_CORAL);
    public static final Material DEAD_FIRE_CORAL = get(MC_DEAD_FIRE_CORAL);
    public static final Material DEAD_HORN_CORAL = get(MC_DEAD_HORN_CORAL);
    public static final Material TUBE_CORAL_BLOCK = get(MC_TUBE_CORAL_BLOCK);
    public static final Material BRAIN_CORAL_BLOCK = get(MC_BRAIN_CORAL_BLOCK);
    public static final Material BUBBLE_CORAL_BLOCK = get(MC_BUBBLE_CORAL_BLOCK);
    public static final Material FIRE_CORAL_BLOCK = get(MC_FIRE_CORAL_BLOCK);
    public static final Material HORN_CORAL_BLOCK = get(MC_HORN_CORAL_BLOCK);
    public static final Material DEAD_TUBE_CORAL_BLOCK = get(MC_DEAD_TUBE_CORAL_BLOCK);
    public static final Material DEAD_BRAIN_CORAL_BLOCK = get(MC_DEAD_BRAIN_CORAL_BLOCK);
    public static final Material DEAD_BUBBLE_CORAL_BLOCK = get(MC_DEAD_BUBBLE_CORAL_BLOCK);
    public static final Material DEAD_FIRE_CORAL_BLOCK = get(MC_DEAD_FIRE_CORAL_BLOCK);
    public static final Material DEAD_HORN_CORAL_BLOCK = get(MC_DEAD_HORN_CORAL_BLOCK);
    public static final Material TUBE_CORAL_FAN = get(MC_TUBE_CORAL_FAN);
    public static final Material BRAIN_CORAL_FAN = get(MC_BRAIN_CORAL_FAN);
    public static final Material BUBBLE_CORAL_FAN = get(MC_BUBBLE_CORAL_FAN);
    public static final Material FIRE_CORAL_FAN = get(MC_FIRE_CORAL_FAN);
    public static final Material HORN_CORAL_FAN = get(MC_HORN_CORAL_FAN);
    public static final Material DEAD_TUBE_CORAL_FAN = get(MC_DEAD_TUBE_CORAL_FAN);
    public static final Material DEAD_BRAIN_CORAL_FAN = get(MC_DEAD_BRAIN_CORAL_FAN);
    public static final Material DEAD_BUBBLE_CORAL_FAN = get(MC_DEAD_BUBBLE_CORAL_FAN);
    public static final Material DEAD_FIRE_CORAL_FAN = get(MC_DEAD_FIRE_CORAL_FAN);
    public static final Material DEAD_HORN_CORAL_FAN = get(MC_DEAD_HORN_CORAL_FAN);
    public static final Material TUBE_CORAL_WALL_FAN = get(MC_TUBE_CORAL_WALL_FAN);
    public static final Material BRAIN_CORAL_WALL_FAN = get(MC_BRAIN_CORAL_WALL_FAN);
    public static final Material BUBBLE_CORAL_WALL_FAN = get(MC_BUBBLE_CORAL_WALL_FAN);
    public static final Material FIRE_CORAL_WALL_FAN = get(MC_FIRE_CORAL_WALL_FAN);
    public static final Material HORN_CORAL_WALL_FAN = get(MC_HORN_CORAL_WALL_FAN);
    public static final Material DEAD_TUBE_CORAL_WALL_FAN = get(MC_DEAD_TUBE_CORAL_WALL_FAN);
    public static final Material DEAD_BRAIN_CORAL_WALL_FAN = get(MC_DEAD_BRAIN_CORAL_WALL_FAN);
    public static final Material DEAD_BUBBLE_CORAL_WALL_FAN = get(MC_DEAD_BUBBLE_CORAL_WALL_FAN);
    public static final Material DEAD_FIRE_CORAL_WALL_FAN = get(MC_DEAD_FIRE_CORAL_WALL_FAN);
    public static final Material DEAD_HORN_CORAL_WALL_FAN = get(MC_DEAD_HORN_CORAL_WALL_FAN);
    /**
     * Kelp with age 0. For older kelp, set the "age" property up to 25.
     */
    public static final Material KELP = get(MC_KELP, MC_AGE, 0);
    public static final Material KELP_PLANT = get(MC_KELP_PLANT);
    public static final Material SEAGRASS = get(MC_SEAGRASS);
    /**
     * Lower half of tall sea grass.
     */
    public static final Material TALL_SEAGRASS = get(MC_TALL_SEAGRASS, MC_HALF, "lower");
    /**
     * One sea pickle. Set the "pickles" property up to 4 for more pickles.
     */
    public static final Material SEA_PICKLE = get(MC_SEA_PICKLE, MC_WATERLOGGED, true, MC_PICKLES, 1);

    // Namespaces

    public static final String MINECRAFT = "minecraft";
    public static final String LEGACY = "legacy";

    // Material type categories

    public static final int CATEGORY_AIR           = 0;
    public static final int CATEGORY_FLUID         = 1;
    public static final int CATEGORY_INSUBSTANTIAL = 2;
    public static final int CATEGORY_MAN_MADE      = 3;
    public static final int CATEGORY_RESOURCE      = 4;
    public static final int CATEGORY_NATURAL_SOLID = 5;
    public static final int CATEGORY_UNKNOWN       = 6;

    private static final long serialVersionUID = 2011101001L;

    /**
     * Note that an identity does <em>not</em> uniquely identify one material,
     * since it does not include the block ID and data value of legacy
     * materials, multiple ones of which map map to the same modern identity.
     */
    static final class Identity implements Serializable {
        Identity(String name, Map<String, String> properties) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (name.indexOf(':') == -1) {
                throw new IllegalArgumentException("name");
            }
            this.name = name;
            this.properties = ((properties != null) && (! properties.isEmpty())) ? ImmutableMap.copyOf(properties) : null;
        }

        /**
         * Determines whether the identity contains a property with a specific
         * name value.
         *
         * @param propertyName The property name to check for.
         * @param value The value to check for.
         * @return {@code true} if the identity contains a property with the
         * specified name, set to the specified value.
         */
        boolean containsPropertyWithValue(String propertyName, String value) {
            return (properties != null) && value.equals(properties.get(propertyName));
        }

        /**
         * Determines whether the identity contains a property with a specific
         * name and one of a specific number of values.
         *
         * @param propertyName The property name to check for.
         * @param values The list of values to check for.
         * @return {@code true} if the identity contains a property with the
         * specified name, set to one of the specified values.
         */
        boolean containsPropertyWithValues(String propertyName, String... values) {
            if (properties == null) {
                return false;
            }
            String value = properties.get(propertyName);
            return (value != null) && asList(values).contains(value);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Identity)
                && name.equals(((Identity) o).name)
                && Objects.equals(properties, ((Identity) o).properties);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 37 + ((properties != null) ? properties.hashCode() : 0);
        }

        @Override
        public String toString() {
            return properties != null ? name + properties : name;
        }

        final String name;
        final Map<String, String> properties;

        private static final long serialVersionUID = 1L;
    }
}