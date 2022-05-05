/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.CSVDataSource;
import org.pepsoft.worldpainter.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.HorizontalOrientationScheme.CARDINAL_DIRECTIONS;
import static org.pepsoft.minecraft.HorizontalOrientationScheme.STAIR_CORNER;
import static org.pepsoft.util.ObjectMapperHolder.OBJECT_MAPPER;
import static org.pepsoft.worldpainter.Constants.UNKNOWN_MATERIAL_COLOUR;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;

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
@SuppressWarnings({"unused"}) // Public API
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
        name = identity.name.intern();
        stringRep = createStringRep();
        legacyStringRep = createLegacyStringRep();
        horizontalOrientationSchemes = determineHorizontalOrientations(identity);
        verticalOrientationScheme = determineVerticalOrientation(identity);

        Map<String, Object> spec = findSpec(identity);
        if (spec != null) {
            opacity = (int) spec.get("opacity");
            terrain = (boolean) spec.get("terrain");
            insubstantial = (boolean) spec.get("insubstantial");
            veryInsubstantial = (boolean) spec.get("veryInsubstantial");
            resource = (boolean) spec.get("resource");
            tileEntity = (boolean) spec.get("tileEntity");
            tileEntityId = tileEntity ? (String) spec.get("tileEntityId") : null;
            treeRelated = (boolean) spec.get("treeRelated");
            vegetation = (boolean) spec.get("vegetation");
            blockLight = (int) spec.get("blockLight");
            natural = (boolean) spec.get("natural");
            watery = (boolean) spec.get("watery");
            colour = spec.containsKey("colour") ? ((int) spec.get("colour")) : UNKNOWN_MATERIAL_COLOUR;
            category = determineCategory();
            possibleProperties = (Set<String>) spec.get("properties");
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Legacy material " + blockType + ":" + data + " not found in materials database");
            }
            // Use reasonable defaults and guesses for unknown blocks
            opacity = guessOpacity(name);
            terrain = false;
            insubstantial = false;
            veryInsubstantial = false;
            resource = guessResource(name);
            tileEntity = false;
            tileEntityId = null;
            treeRelated = guessTreeRelated(name);
            vegetation = false;
            blockLight = 0;
            natural = false;
            watery = false;
            colour = UNKNOWN_MATERIAL_COLOUR;
            category = CATEGORY_UNKNOWN;
            possibleProperties = null;
        }

        lightSource = (blockLight > 0);
        transparent = (opacity == 0);
        translucent = (opacity < 15);
        opaque = (opacity == 15);
        solid = ! veryInsubstantial;
        hasPropertySnowy = hasProperty(MC_SNOWY);
        canSupportSnow = determineCanSupportSnow();

        if (namespace != null) {
            ALL_NAMESPACES.add(namespace);
            SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, s -> new HashSet<>()).add(simpleName);
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
        } else if (identity.name.startsWith("legacy:block_") && (identity.properties != null) && identity.properties.containsKey("data_value")) {
            // Legacy non-vanilla block; decode block ID and data value from
            // name and properties
            blockType = Integer.parseInt(identity.name.substring(13));
            data = Integer.parseInt(identity.properties.get("data_value"));
            if (logger.isDebugEnabled()) {
                logger.debug("Matched " + identity + " to legacy non-vanilla block with ID " + blockType + " and data value " + data);
            }
        } else {
            blockType = -1;
            data = -1;
            if (logger.isTraceEnabled()) {
                logger.trace("Did not match " + identity + " to legacy block");
            }
        }

        this.identity = identity;
        name = identity.name.intern();
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
            terrain = (boolean) spec.get("terrain");
            insubstantial = (boolean) spec.get("insubstantial");
            veryInsubstantial = (boolean) spec.get("veryInsubstantial");
            resource = (boolean) spec.get("resource");
            tileEntity = (boolean) spec.get("tileEntity");
            tileEntityId = tileEntity ? (String) spec.get("tileEntityId") : null;
            treeRelated = (boolean) spec.get("treeRelated");
            vegetation = (boolean) spec.get("vegetation");
            blockLight = (int) spec.get("blockLight");
            natural = (boolean) spec.get("natural");
            watery = (boolean) spec.get("watery");
            colour = spec.containsKey("colour") ? ((int) spec.get("colour")) : UNKNOWN_MATERIAL_COLOUR;
            category = determineCategory();
            possibleProperties = (Set<String>) spec.get("properties");
        } else {
            if (logger.isDebugEnabled()) {
                if ((namespace != null) && namespace.equals(MINECRAFT)) {
                    logger.warn("Modern material {} not found in materials database", identity);
                } else {
                    logger.debug("Modern material {} not found in materials database", identity);
                }
            }
            // Use reasonable defaults and guesses for unknown blocks
            opacity = guessOpacity(name);
            terrain = false;
            insubstantial = false;
            veryInsubstantial = false;
            resource = guessResource(name);
            tileEntity = false;
            tileEntityId = null;
            treeRelated = guessTreeRelated(name);
            vegetation = false;
            blockLight = 0;
            natural = false;
            watery = false;
            colour = UNKNOWN_MATERIAL_COLOUR;
            category = CATEGORY_UNKNOWN;
            possibleProperties = null;
        }

        lightSource = (blockLight > 0);
        transparent = (opacity == 0);
        translucent = (opacity < 15);
        opaque = (opacity == 15);
        solid = ! veryInsubstantial;
        hasPropertySnowy = hasProperty(MC_SNOWY);
        canSupportSnow = determineCanSupportSnow();

        ALL_NAMESPACES.add(namespace);
        SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, s -> new HashSet<>()).add(simpleName);
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
                    // The spec must specify a discriminator (otherwise there could not be multiple for the same name),
                    // make sure the properties in it match the identity
                    Set<String> discriminator = (Set<String>) spec.get("discriminator");
                    for (String property: discriminator) {
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
     * @return The properties of this material. May be {@code null}.
     */
    public Map<String, String> getProperties() {
        return identity.properties;
    }

    /**
     * Indicates whether a specific property is present on this type of material, regardless of whether it is set on the
     * current instance.
     *
     * @param property The property to check for presence.
     * @return {@code true} if the specified property is present on this type of material.
     */
    public boolean hasProperty(Property<?> property) {
        return (possibleProperties != null) && possibleProperties.contains(property.name);
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
     * type, or {@code null} if the property is not set.
     */
    public <T> T getProperty(Property<T> property) {
        return getProperty(property, null);
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
     * type, or {@code defaultValue} if the property is not set.
     */
    public <T> T getProperty(Property<T> property, T defaultValue) {
        return ((identity.properties != null) && (identity.properties.containsKey(property.name))) ? property.fromString(identity.properties.get(property.name)) : defaultValue;
    }

    /**
     * Convenience method to check whether a boolean-typed property is present
     * and set.
     *
     * @param property  The property to check for.
     * @return {@code true} if the property is present and set to
     * {@code true}.
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
     * Indicates whether a specific property is present on this type of material, regardless of whether it is set on the
     * current instance.
     *
     * @param name The name of the property to check for presence.
     * @return {@code true} if the specified property is present on this type of material.
     */
    public boolean hasProperty(String name) {
        return (possibleProperties != null) && possibleProperties.contains(name);
    }

    /**
     * Indicates whether a specific property is currently set on this material.
     *
     * @param name The name of the property to check for presence.
     * @return {@code true} if the specified property is currently set on this material.
     */
    public boolean isPropertySet(String name) {
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
     * Returns a material identical to this one, except with the specified property removed.
     *
     * @param name The name of the property that should be removed.
     * @return A material identical to this one, except with the specified property removed.
     */
    public Material withoutProperty(String name) {
        Map<String, String> newProperties;
        if (identity.properties != null) {
            newProperties = new HashMap<>(identity.properties);
            newProperties.remove(name);
            if (newProperties.isEmpty()) {
                newProperties = null;
            }
        } else {
            newProperties = null;
        }
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
     *     {@code null} if it has no direction, or is not pointing in a
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
     * @param platform The platform for which to perform the transformation.
     *                 This ensures that only materials compatible with the
     *                 specified platform will be returned, and may mean that
     *                 the transformation is not performed to avoid returning an
     *                 incompatible material.
     * @return The rotated material (or the same one if rotation does not apply
     * to this material)
     */
    public Material rotate(int steps, Platform platform) {
        if (((horizontalOrientationSchemes != null) || (legacyHorizontalOrientationSchemesForRotating != null)) && ((steps % 4) != 0)) {
            Material material = this;
            if (platform.capabilities.contains(NAME_BASED)) {
                if (horizontalOrientationSchemes != null) {
                    for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                        material = horizontalOrientationScheme.rotate(material, steps);
                    }
                }
            } else {
                if (! legacyHorizontalOrientationSchemesForRotatingSet) {
                    legacyHorizontalOrientationSchemesForRotating = determineLegacyHorizontalOrientationsForRotating();
                    legacyHorizontalOrientationSchemesForRotatingSet = true;
                }
                if (legacyHorizontalOrientationSchemesForRotating != null) {
                    for (HorizontalOrientationScheme horizontalOrientationScheme: legacyHorizontalOrientationSchemesForRotating) {
                        material = horizontalOrientationScheme.rotate(material, steps);
                    }
                }
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
     * @param platform The platform for which to perform the transformation.
     *                 This ensures that only materials compatible with the
     *                 specified platform will be returned, and may mean that
     *                 the transformation is not performed to avoid returning an
     *                 incompatible material.
     * @return The mirrored material (or the same one if mirroring does not
     *     apply to this material)
     */
    public Material mirror(Direction axis, Platform platform) {
        if ((horizontalOrientationSchemes != null) || (legacyHorizontalOrientationSchemesForMirroring != null)) {
            Material material = this;
            if (platform.capabilities.contains(NAME_BASED)) {
                if (horizontalOrientationSchemes != null) {
                    for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                        material = horizontalOrientationScheme.mirror(material, axis);
                    }
                }
            } else {
                if (! legacyHorizontalOrientationSchemesForMirroringSet) {
                    legacyHorizontalOrientationSchemesForMirroring = determineLegacyHorizontalOrientationsForMirroring();
                    legacyHorizontalOrientationSchemesForMirroringSet = true;
                }
                if (legacyHorizontalOrientationSchemesForMirroring != null) {
                    for (HorizontalOrientationScheme horizontalOrientationScheme: legacyHorizontalOrientationSchemesForMirroring) {
                        material = horizontalOrientationScheme.mirror(material, axis);
                    }
                }
            }
            return material;
        }
        return this;
    }

    /**
     * Gets a vertically mirrored version of the material.
     *
     * @param platform The platform for which to perform the transformation.
     *                 This ensures that only materials compatible with the
     *                 specified platform will be returned, and may mean that
     *                 the transformation is not performed to avoid returning an
     *                 incompatible material.
     * @return A vertically mirrored version of the material.
     */
    public Material invert(Platform platform) {
        if ((verticalOrientationScheme != null) || (legacyVerticalOrientationScheme != null)) {
            VerticalOrientationScheme scheme;
            if (platform.capabilities.contains(NAME_BASED)) {
                scheme = verticalOrientationScheme;
            } else {
                if (! legacyVerticalOrientationSchemeSet) {
                    legacyVerticalOrientationScheme = determineLegacyVerticalOrientation();
                    legacyVerticalOrientationSchemeSet = true;
                }
                scheme = legacyVerticalOrientationScheme;
            }
            if (scheme != null) {
                switch (scheme) {
                    case HALF:
                        return withProperty(HALF, getProperty(HALF).equals("top") ? "bottom" : "top");
                    case TYPE:
                        return withProperty(TYPE, getProperty(TYPE).equals("top") ? "bottom" : "top");
                    case UP:
                        return withProperty(UP, !getProperty(UP));
                    case UP_DOWN:
                        return withProperty(UP, getProperty(DOWN)).withProperty(DOWN, getProperty(UP));
                    default:
                        throw new InternalError();
                }
            }
        }
        return this;
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name The name to test this material for.
     * @return {@code true} if the material has the specified name.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material has one of the specified names.
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
     * @return {@code true} if the material <em>does not</em> have the
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
     * @return {@code true} if the material <em>does not</em> have any of
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
     * @return {@code true} if the specified material has the same name as
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
     * @return {@code true} if the specified material <em>does not</em>
     * have the same name as this one.
     */
    public boolean isNotNamedSameAs(Material material) {
        return ! material.name.equals(this.name);
    }

    /**
     * Indicate whether the block is filled with water (in addition to whatever
     * else may be there).
     */
    public boolean containsWater() {
        return watery || is(WATERLOGGED) || (isNamed(MC_WATER) && (getProperty(LEVEL) == 0));
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
                return horizontalOrientationSchemes.toArray(new HorizontalOrientationScheme[0]);
            }
        } else {
            return null;
        }
    }

    private HorizontalOrientationScheme[] determineLegacyHorizontalOrientationsForRotating() {
        if (horizontalOrientationSchemes != null) {
            List<HorizontalOrientationScheme> legacySchemes = new ArrayList<>(horizontalOrientationSchemes.length);
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                if (isLegacyMaterial(horizontalOrientationScheme.rotate(this, 1))
                        && isLegacyMaterial(horizontalOrientationScheme.rotate(this, 2))
                        && isLegacyMaterial(horizontalOrientationScheme.rotate(this, 3))) {
                    legacySchemes.add(horizontalOrientationScheme);
                } else {
                    logger.info("Disabling horizontal orientation scheme " + horizontalOrientationScheme + " for rotating legacy material " + name);
                }
            }
            return legacySchemes.isEmpty() ? null : legacySchemes.toArray(new HorizontalOrientationScheme[0]);
        } else {
            return null;
        }
    }

    private HorizontalOrientationScheme[] determineLegacyHorizontalOrientationsForMirroring() {
        if (horizontalOrientationSchemes != null) {
            List<HorizontalOrientationScheme> legacySchemes = new ArrayList<>(horizontalOrientationSchemes.length);
            for (HorizontalOrientationScheme horizontalOrientationScheme: horizontalOrientationSchemes) {
                if (isLegacyMaterial(horizontalOrientationScheme.mirror(this, Direction.NORTH))
                        && isLegacyMaterial(horizontalOrientationScheme.mirror(this, Direction.EAST))) {
                    legacySchemes.add(horizontalOrientationScheme);
                } else {
                    logger.info("Disabling horizontal orientation scheme " + horizontalOrientationScheme + " for mirroring legacy material " + name);
                }
            }
            return legacySchemes.isEmpty() ? null : legacySchemes.toArray(new HorizontalOrientationScheme[0]);
        } else {
            return null;
        }
    }

    private VerticalOrientationScheme determineLegacyVerticalOrientation() {
        if (verticalOrientationScheme != null) {
            Material invertedMaterial;
            switch (verticalOrientationScheme) {
                case HALF:
                    invertedMaterial = withProperty(HALF, getProperty(HALF).equals("top") ? "bottom" : "top");
                    break;
                case TYPE:
                    invertedMaterial = withProperty(TYPE, getProperty(TYPE).equals("top") ? "bottom" : "top");
                    break;
                case UP:
                    invertedMaterial = withProperty(UP, !getProperty(UP));
                    break;
                case UP_DOWN:
                    invertedMaterial = withProperty(UP, getProperty(DOWN)).withProperty(DOWN, getProperty(UP));
                    break;
                default:
                    throw new InternalError();
            }
            if (isLegacyMaterial(invertedMaterial)) {
                return verticalOrientationScheme;
            } else {
                logger.info("Disabling vertical orientation scheme " + verticalOrientationScheme + " for legacy material " + name);
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isLegacyMaterial(Material material) {
        return material.blockType != -1;
    }

    private VerticalOrientationScheme determineVerticalOrientation(Identity identity) {
        if (identity.containsPropertyWithValues("half", "top", "bottom")) {
            return VerticalOrientationScheme.HALF;
        } else if (identity.containsPropertyWithValues("up", "true", "false")) {
            if (identity.containsPropertyWithValues("down", "true", "false")) {
                return VerticalOrientationScheme.UP_DOWN;
            } else {
                return VerticalOrientationScheme.UP;
            }
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

    private boolean determineCanSupportSnow() {
        return hasPropertySnowy
                || (solid
                    && opaque
                    && (! ("bottom".equals(getProperty(MC_TYPE)) && (name.endsWith("_slab") || name.endsWith("_stairs"))))
                    && (! NO_SNOW_ON.contains(name)))
                || SNOW_ON.contains(name)
                || name.endsWith("_leaves");
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

    /**
     * Get a legacy (pre-1.13) material by block ID. The data value is assumed
     * to be zero.
     *
     * @param blockType The block ID.
     * @return The requested material.
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
     * {@link #get(String, Object...)}.
     */
    public static Material get(int blockType, int data) {
        return getByCombinedIndex((blockType << 4) | data);
    }

    /**
     * Get a legacy (pre-1.13) material corresponding to a combined index
     * consisting of the block ID shifted left four bits and or-ed with the data
     * value. In other words the index is a 16-bit unsigned integer, with bit
     * 0-3 indicating the data value and bit 4-15 indicating the block ID.
     *
     * @param index The combined index of the material to get.
     * @return The indicated material.
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
                if (! DEFAULT_MATERIALS_BY_NAME.containsKey(identity.name)) {
                    DEFAULT_MATERIALS_BY_NAME.put(identity.name, material);
                }
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
     *                   {@code null}.
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
     *                   key-value pairs. The keys must be {@code String}s.
     *                   May be {@code null}.
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
     * {@code null} if no material by that name is known.
     */
    public static Material getDefault(String name) {
        return DEFAULT_MATERIALS_BY_NAME.get(name);
    }

    public static Set<String> getAllNamespaces() {
        return Collections.unmodifiableSet(ALL_NAMESPACES);
    }

    public static Set<String> getAllSimpleNamesForNamespace(String namespace) {
        return SIMPLE_NAMES_BY_NAMESPACE.containsKey(namespace) ? Collections.unmodifiableSet(SIMPLE_NAMES_BY_NAMESPACE.get(namespace)) : Collections.emptySet();
    }

    public static Set<String> getAllNames() {
        return ALL_MATERIALS.values().stream().map(material -> material.name).collect(toSet());
    }

    public static Collection<Material> getAllMaterials() {
        return Collections.unmodifiableCollection(ALL_MATERIALS.values());
    }

    // Object

    public boolean equals(Object o) {
        if (! (o instanceof Material)) {
            return false;
        }
        if ((blockType != -1) && ((blockType != ((Material) o).blockType) || (data != ((Material) o).data))) {
            return false;
        }
        return identity.equals(((Material) o).identity);
    }

    public int hashCode() {
        return ((blockType != -1) ? ((blockType * 17) + data) : 1) * 4099 + identity.hashCode();
    }

    /**
     * Get the modern style (name and property-based) name of this material. For
     * brevity, the namespace is omitted if it isn't {@code minecraft} and
     * properties with value {@code "false"} or {@code "0"} are also
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

    /**
     * Get the full, non abreviated identity of this material as a string.
     */
    public String toFullString() {
        return identity.toString() + ((blockType != -1) ? (" (id=" + blockType + "; data=" + data + ")") : "");
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

    public static int guessOpacity(String name) {
        if (name.contains("slab") || name.contains("stairs") || name.contains("block") || name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphea") || name.contains("bricks")) {
            return 15;
        } else if (name.contains("leaves")) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean guessResource(String name) {
        return name.contains("ore");
    }

    public static boolean guessTreeRelated(String name) {
        return name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphea") || name.endsWith("_leaves") || name.endsWith("_sapling");
    }

    /**
     * How much light the block blocks from 0 (fully transparent) to 15 (fully
     * opaque).
     */
    public final transient int opacity;

    /**
     * The name of the block, including the namespace (if present; separated by a colon). This string is interned, so
     * that the {@code ==} operator may be used to make comparisons against it.
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
    public final transient boolean tileEntity;

    /**
     * If {@link #tileEntity}, the name of the tile entity.
     */
    public final transient String tileEntityId;

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
     * Whether the block always contains water (and can therefore only exist
     * "under water") rather than having a waterlogged property.
     */
    public final transient boolean watery;

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
     * The simple name (excluding the namespace, i.e. the part after the colon) of this material. This string is
     * interned, so that the {@code ==} operator may be used to make comparisons against it.
     */
    public final transient String simpleName;

    /**
     * The namespace (i.e. the part before the colon) of this material. This string is interned, so that the {@code ==}
     * operator may be used to make comparisons against it.
     */
    public final transient String namespace;

    /**
     * Whether snow may be placed on this block.
     */
    public final transient boolean canSupportSnow;

    /**
     * The colour of this material as an {@code int} in ARGB format.
     */
    public final transient int colour;

    /**
     * The properties this type of material has, regardless of whether they are set on the current instance.
     */
    public final transient Set<String> possibleProperties;

    // Optimised versions of hasProperty(...):

    /**
     * Whether the material has the property {@code snowy}.
     */
    public final transient boolean hasPropertySnowy;

    private final Identity identity;
    private final transient String stringRep, legacyStringRep;

    private transient HorizontalOrientationScheme[] legacyHorizontalOrientationSchemesForMirroring;
    private transient boolean legacyHorizontalOrientationSchemesForMirroringSet;
    private transient HorizontalOrientationScheme[] legacyHorizontalOrientationSchemesForRotating;
    private transient boolean legacyHorizontalOrientationSchemesForRotatingSet;
    private transient VerticalOrientationScheme legacyVerticalOrientationScheme;
    private transient boolean legacyVerticalOrientationSchemeSet;

    private static final Map<Integer, Map<String, Object>> LEGACY_BLOCK_SPECS_BY_COMBINED_ID = new HashMap<>();
    private static final Map<String, Set<Map<String, Object>>> LEGACY_BLOCK_SPECS_BY_NAME = new HashMap<>();
    private static final Map<String, Set<Map<String, Object>>> MATERIAL_SPECS = new HashMap<>();

    static {
        // Read legacy MC block database
        try (Reader in = new InputStreamReader(requireNonNull(Material.class.getResourceAsStream("legacy-mc-blocks.json")), UTF_8)) {
            @SuppressWarnings("unchecked") // Guaranteed by contents of file
            List<Object> items = (List<Object>) OBJECT_MAPPER.readValue(in, List.class);
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
        }

        // Read MC materials database
        try (Reader in = new InputStreamReader(requireNonNull(Material.class.getResourceAsStream("mc-materials.csv")), UTF_8)) {
            CSVDataSource csvDataSource = new CSVDataSource();
            csvDataSource.openForReading(in);
            do {
                Map<String, Object> materialSpecs = new HashMap<>();
                String name = csvDataSource.getString("name");
                materialSpecs.put("name", name);
                String str = csvDataSource.getString("discriminator");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("discriminator", ImmutableSet.copyOf(str.split(",")));
                }
                str = csvDataSource.getString("properties");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("properties", ImmutableSet.copyOf(str.split(",")));
                }
                materialSpecs.put("opacity", csvDataSource.getInt("opacity"));
                materialSpecs.put("terrain", csvDataSource.getBoolean("terrain"));
                materialSpecs.put("insubstantial", csvDataSource.getBoolean("insubstantial"));
                materialSpecs.put("veryInsubstantial", csvDataSource.getBoolean("veryInsubstantial"));
                materialSpecs.put("resource", csvDataSource.getBoolean("resource"));
                materialSpecs.put("tileEntity", csvDataSource.getBoolean("tileEntity"));
                str = csvDataSource.getString("tileEntityId");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("tileEntityId", str);
                }
                materialSpecs.put("treeRelated", csvDataSource.getBoolean("treeRelated"));
                materialSpecs.put("vegetation", csvDataSource.getBoolean("vegetation"));
                materialSpecs.put("blockLight", csvDataSource.getInt("blockLight"));
                materialSpecs.put("natural", csvDataSource.getBoolean("natural"));
                materialSpecs.put("watery", csvDataSource.getBoolean("watery"));
                str = csvDataSource.getString("colour");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("colour", Integer.parseUnsignedInt(str, 16));
                }
                str = csvDataSource.getString("colourOrigin");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("colourOrigin", str);
                }
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

    private static final Set<String> SNOW_ON = ImmutableSet.of(MC_SNOW_BLOCK, MC_POWDER_SNOW);
    private static final Set<String> NO_SNOW_ON = ImmutableSet.of(MC_END_PORTAL_FRAME, MC_PACKED_ICE, MC_GRASS_PATH, MC_DIRT_PATH, MC_FARMLAND);

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
    /**
     * Lava that maps to flowing lava in Minecraft 1.12 and older. Minecraft 1.15+ does not make that distinction.
     */
    public static final Material LAVA = LEGACY_MATERIALS[(BLK_LAVA) << 4];
    public static final Material NETHERRACK = LEGACY_MATERIALS[(BLK_NETHERRACK) << 4];
    public static final Material END_STONE = LEGACY_MATERIALS[BLK_END_STONE << 4];
    public static final Material CHORUS_PLANT = LEGACY_MATERIALS[BLK_CHORUS_PLANT << 4];
    public static final Material COAL = LEGACY_MATERIALS[(BLK_COAL) << 4];
    public static final Material GRAVEL = LEGACY_MATERIALS[(BLK_GRAVEL) << 4];
    public static final Material REDSTONE_ORE = LEGACY_MATERIALS[(BLK_REDSTONE_ORE) << 4];
    public static final Material IRON_ORE = LEGACY_MATERIALS[(BLK_IRON_ORE) << 4];
    /**
     * Water that maps to flowing water in Minecraft 1.12 and older. Minecraft 1.15+ does not make that distinction.
     */
    public static final Material WATER = LEGACY_MATERIALS[(BLK_WATER) << 4];
    public static final Material GOLD_ORE = LEGACY_MATERIALS[(BLK_GOLD_ORE) << 4];
    public static final Material LAPIS_LAZULI_ORE = LEGACY_MATERIALS[(BLK_LAPIS_LAZULI_ORE) << 4];
    public static final Material DIAMOND_ORE = LEGACY_MATERIALS[(BLK_DIAMOND_ORE) << 4];
    public static final Material BEDROCK = LEGACY_MATERIALS[(BLK_BEDROCK) << 4];
    /**
     * Water that maps to stationary water in Minecraft 1.12 and older. Minecraft 1.15+ does not make that distinction.
     */
    public static final Material STATIONARY_WATER = LEGACY_MATERIALS[(BLK_STATIONARY_WATER) << 4];
    /**
     * Lava that maps to stationary lava in Minecraft 1.12 and older. Minecraft 1.15+ does not make that distinction.
     */
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

    public static final Material CARVED_PUMPKIN_NORTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NORTH_FACE)];
    public static final Material CARVED_PUMPKIN_EAST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_EAST_FACE)];
    public static final Material CARVED_PUMPKIN_SOUTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_SOUTH_FACE)];
    public static final Material CARVED_PUMPKIN_WEST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_WEST_FACE)];
    public static final Material MELON = LEGACY_MATERIALS[BLK_MELON << 4];
    public static final Material JACK_O_LANTERN_NORTH_FACE = LEGACY_MATERIALS[((BLK_JACK_O_LANTERN) << 4) | (DATA_PUMPKIN_NORTH_FACE)];
    public static final Material JACK_O_LANTERN_EAST_FACE = LEGACY_MATERIALS[((BLK_JACK_O_LANTERN) << 4) | (DATA_PUMPKIN_EAST_FACE)];
    public static final Material JACK_O_LANTERN_SOUTH_FACE = LEGACY_MATERIALS[((BLK_JACK_O_LANTERN) << 4) | (DATA_PUMPKIN_SOUTH_FACE)];
    public static final Material JACK_O_LANTERN_WEST_FACE = LEGACY_MATERIALS[((BLK_JACK_O_LANTERN) << 4) | (DATA_PUMPKIN_WEST_FACE)];

    /**
     * Lava that maps to sideways flowing, non-permanent lava in all Minecraft versions.
     */
    public static final Material FLOWING_LAVA = LEGACY_MATERIALS[(BLK_STATIONARY_LAVA << 4) | 2];
    /**
     * Water that maps to sideways flowing, non-permanent water in all Minecraft versions.
     */
    public static final Material FLOWING_WATER = LEGACY_MATERIALS[(BLK_STATIONARY_WATER << 4) | 1];
    /**
     * Lava that maps to falling, non-permanent lava in all Minecraft versions.
     */
    public static final Material FALLING_LAVA = LEGACY_MATERIALS[(BLK_STATIONARY_LAVA << 4) | 10];
    /**
     * Water that maps to falling, non-permanent water in all Minecraft versions.
     */
    public static final Material FALLING_WATER = LEGACY_MATERIALS[(BLK_STATIONARY_WATER << 4) | 9];

    // MC 1.13+ block property access helpers

    public static final Property<Boolean>   SNOWY       = new Property<>(MC_SNOWY,       Boolean.class);
    public static final Property<Boolean>   NORTH       = new Property<>(MC_NORTH,       Boolean.class);
    public static final Property<Boolean>   EAST        = new Property<>(MC_EAST,        Boolean.class);
    public static final Property<Boolean>   SOUTH       = new Property<>(MC_SOUTH,       Boolean.class);
    public static final Property<Boolean>   WEST        = new Property<>(MC_WEST,        Boolean.class);
    public static final Property<Boolean>   UP          = new Property<>(MC_UP,          Boolean.class);
    public static final Property<Boolean>   DOWN        = new Property<>(MC_DOWN,       Boolean.class);
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
    public static final Property<Boolean>   BERRIES     = new Property<>(MC_BERRIES,     Boolean.class);
    public static final Property<Integer>   DISTANCE    = new Property<>(MC_DISTANCE,    Integer.class);

    // Modern materials (based on MC 1.13+ block names and properties)

    /**
     * A vine with no directions turned on, which is not a valid block in
     * Minecraft, so you must set at least one direction.
     */
    public static final Material VINE = get(MC_VINE, MC_NORTH, false, MC_EAST, false, MC_SOUTH, false, MC_WEST, false, MC_UP, false, MC_DOWN, false); // "down" is not a valid property, but without it we don't get the right vertical orientation scheme and Minecraft appears to ignore it
    public static final Material TERRACOTTA = get(MC_TERRACOTTA);
    public static final Material BLUE_ORCHID = get(MC_BLUE_ORCHID);
    public static final Material ALLIUM = get(MC_ALLIUM);
    public static final Material AZURE_BLUET = get(MC_AZURE_BLUET);
    public static final Material RED_TULIP = get(MC_RED_TULIP);
    public static final Material ORANGE_TULIP = get(MC_ORANGE_TULIP);
    public static final Material WHITE_TULIP = get(MC_WHITE_TULIP);
    public static final Material PINK_TULIP = get(MC_PINK_TULIP);
    public static final Material OXEYE_DAISY = get(MC_OXEYE_DAISY);
    public static final Material SUNFLOWER_LOWER = get(MC_SUNFLOWER, MC_HALF, "lower");
    public static final Material LILAC_LOWER = get(MC_LILAC, MC_HALF, "lower");
    public static final Material TALL_GRASS_LOWER = get(MC_TALL_GRASS, MC_HALF, "lower");
    public static final Material LARGE_FERN_LOWER = get(MC_LARGE_FERN, MC_HALF, "lower");
    public static final Material ROSE_BUSH_LOWER = get(MC_ROSE_BUSH, MC_HALF, "lower");
    public static final Material PEONY_LOWER = get(MC_PEONY, MC_HALF, "lower");
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
    public static final Material TALL_SEAGRASS_LOWER = get(MC_TALL_SEAGRASS, MC_HALF, "lower");
    /**
     * One sea pickle. Set the "pickles" property up to 4 for more pickles.
     */
    public static final Material SEA_PICKLE_1 = get(MC_SEA_PICKLE, MC_WATERLOGGED, true, MC_PICKLES, 1);
    public static final Material CORNFLOWER = get(MC_CORNFLOWER);
    public static final Material LILY_OF_THE_VALLEY = get(MC_LILY_OF_THE_VALLEY);
    public static final Material WITHER_ROSE = get(MC_WITHER_ROSE);
    /**
     * Sweet Berry Bush with age 0. For older Sweet Berry Bush, set the "age" property up to 3.
     */
    public static final Material SWEET_BERRY_BUSH = get(MC_SWEET_BERRY_BUSH, MC_AGE, 0);
    public static final Material OAK_SIGN = get(MC_OAK_SIGN);
    public static final Material DEEPSLATE_X = get(MC_DEEPSLATE, MC_AXIS, "x");
    public static final Material DEEPSLATE_Y = get(MC_DEEPSLATE, MC_AXIS, "y");
    public static final Material DEEPSLATE_Z = get(MC_DEEPSLATE, MC_AXIS, "z");
    public static final Material DEEPSLATE_COAL_ORE = get(MC_DEEPSLATE_COAL_ORE);
    public static final Material DEEPSLATE_COPPER_ORE = get(MC_DEEPSLATE_COPPER_ORE);
    public static final Material DEEPSLATE_LAPIS_ORE = get(MC_DEEPSLATE_LAPIS_ORE);
    public static final Material DEEPSLATE_IRON_ORE = get(MC_DEEPSLATE_IRON_ORE);
    public static final Material DEEPSLATE_GOLD_ORE = get(MC_DEEPSLATE_GOLD_ORE);
    public static final Material DEEPSLATE_REDSTONE_ORE = get(MC_DEEPSLATE_REDSTONE_ORE);
    public static final Material DEEPSLATE_DIAMOND_ORE = get(MC_DEEPSLATE_DIAMOND_ORE);
    public static final Material DEEPSLATE_EMERALD_ORE = get(MC_DEEPSLATE_EMERALD_ORE);
    public static final Material TUFF = get(MC_TUFF);
    public static final Material COPPER_ORE = get(MC_COPPER_ORE);
    public static final Material NETHER_GOLD_ORE = get(MC_NETHER_GOLD_ORE);
    public static final Material ANCIENT_DEBRIS = get(MC_ANCIENT_DEBRIS);
    public static final Material BASALT = get(MC_BASALT);
    public static final Material BLACKSTONE = get(MC_BLACKSTONE);
    public static final Material SOUL_SOIL = get(MC_SOUL_SOIL);
    public static final Material GRASS_PATH = get(MC_GRASS_PATH);
    public static final Material DIRT_PATH = get(MC_DIRT_PATH);
    public static final Material WARPED_NYLIUM = get(MC_WARPED_NYLIUM);
    public static final Material CRIMSON_NYLIUM = get(MC_CRIMSON_NYLIUM);
    public static final Material ROOTED_DIRT = get(MC_ROOTED_DIRT);
    public static final Material INFESTED_DEEPSLATE = get(MC_INFESTED_DEEPSLATE);
    public static final Material BAMBOO_NO_LEAVES = get(MC_BAMBOO, MC_STAGE, 0, MC_AGE, 0, MC_LEAVES, "none");
    public static final Material BAMBOO_SMALL_LEAVES = get(MC_BAMBOO, MC_STAGE, 0, MC_AGE, 0, MC_LEAVES, "small");
    public static final Material BAMBOO_LARGE_LEAVES_STAGE_1 = get(MC_BAMBOO, MC_STAGE, 1, MC_AGE, 0, MC_LEAVES, "large");
    public static final Material AZALEA = get(MC_AZALEA);
    public static final Material FLOWERING_AZALEA = get(MC_FLOWERING_AZALEA);
    public static final Material CRIMSON_FUNGUS = get(MC_CRIMSON_FUNGUS);
    public static final Material WARPED_FUNGUS = get(MC_WARPED_FUNGUS);
    public static final Material CRIMSON_ROOTS = get(MC_CRIMSON_ROOTS);
    public static final Material WARPED_ROOTS = get(MC_WARPED_ROOTS);
    public static final Material NETHER_SPROUTS = get(MC_NETHER_SPROUTS);
    public static final Material TWISTING_VINES_PLANT = get(MC_TWISTING_VINES_PLANT);
    public static final Material TWISTING_VINES_25 = get(MC_TWISTING_VINES, MC_AGE, 25);
    public static final Material GLOW_LICHEN_DOWN = get(MC_GLOW_LICHEN, MC_UP, false, MC_DOWN, true);
    public static final Material MOSS_CARPET = get(MC_MOSS_CARPET);
    public static final Material BIG_DRIPLEAF_STEM_SOUTH = get(MC_BIG_DRIPLEAF_STEM, MC_FACING, "south");
    public static final Material BIG_DRIPLEAF_SOUTH = get(MC_BIG_DRIPLEAF, MC_FACING, "south");
    public static final Material PUMPKIN = get(MC_PUMPKIN);
    public static final Material CALCITE = get(MC_CALCITE);
    public static final Material SPORE_BLOSSOM = get(MC_SPORE_BLOSSOM);
    public static final Material WEEPING_VINES = get(MC_WEEPING_VIVES);
    public static final Material WEEPING_VINES_PLANT = get(MC_WEEPING_VIVES_PLANT);
    public static final Material HANGING_ROOTS = get(MC_HANGING_ROOTS);
    public static final Material CAVE_VINES = get(MC_CAVE_VINES);
    public static final Material CAVE_VINES_PLANT = get(MC_CAVE_VINES_PLANT);

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

    /**
     * A map of modern tile entity IDs mapped to the modern block IDs they are associated with.
     */
    public static final Map<String, Set<String>> TILE_ENTITY_MAP;

    static {
        final Map<String, Set<String>> tileEntityMap = new HashMap<>();
        for (Map.Entry<String, Set<Map<String, Object>>> entry: MATERIAL_SPECS.entrySet()) {
            final String name = entry.getKey();
            for (Map<String, Object> spec: entry.getValue()) {
                if (spec.containsKey("tileEntityId")) {
                    tileEntityMap.computeIfAbsent((String) spec.get("tileEntityId"), k -> new HashSet<>()).add(name);
                }
            }
        }
        TILE_ENTITY_MAP = unmodifiableMap(tileEntityMap);
    }

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
                throw new IllegalArgumentException("name \"" + name + "\"");
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