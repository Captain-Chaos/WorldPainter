package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.IconUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.pepsoft.util.GUIUtils.UI_SCALE;

/**
 * @author SchmitzP
 */
public final class MixedMaterial implements Serializable, Comparable<MixedMaterial>, Cloneable {
    /**
     * Create a new "mixed material" which contains only one material.
     * 
     * @param name The name of the mixed material.
     * @param row A single row describing the material.
     * @param biome The default biome associated with this mixed material, or -1
     *     for no default biome.
     * @param colour The colour associated with this mixed material, or
     *     <code>null</code> for no default colour.
     */
    public MixedMaterial(final String name, final Row row, final int biome, final Integer colour) {
        this(name, new Row[] {row}, biome, Mode.SIMPLE, 1.0f, colour, null, 0, 0, false);
    }
    
    /**
     * Create a new noisy mixed material.
     * 
     * @param name The name of the mixed material.
     * @param rows The rows describing the materials to be used together with
     *     their occurrences.
     * @param biome The default biome associated with this mixed material, or -1
     *     for no default biome.
     * @param colour The colour associated with this mixed material, or
     *     <code>null</code> for no default colour.
     */
    public MixedMaterial(final String name, final Row[] rows, final int biome, final Integer colour) {
        this(name, rows, biome, Mode.NOISE, 1.0f, colour, null, 0, 0, false);
    }

    /**
     * Create a new blobby mixed material.
     * 
     * @param name The name of the mixed material.
     * @param rows The rows describing the materials to be used together with
     *     their occurrences.
     * @param biome The default biome associated with this mixed material, or -1
     *     for no default biome.
     * @param colour The colour associated with this mixed material, or
     *     <code>null</code> for no default colour.
     * @param scale The scale of the blobs. <code>1.0f</code> for default size.
     */
    public MixedMaterial(final String name, final Row[] rows, final int biome, final Integer colour, final float scale) {
        this(name, rows, biome, Mode.BLOBS, scale, colour, null, 0, 0, false);
    }

    /**
     * Create a new layered mixed material.
     * 
     * @param name The name of the mixed material.
     * @param rows The rows describing the materials to be used together with
     *     their heights.
     * @param biome The default biome associated with this mixed material, or -1
     *     for no default biome.
     * @param colour The colour associated with this mixed material, or
     *     <code>null</code> for no default colour.
     * @param variation The variation in layer height which should be applied,
     *     or <code>null</code> for no variation.
     * @param layerXSlope The slope of the layer for the x-axis.
     *     Must be zero if <code>repeat</code> is false.
     * @param layerYSlope The slope of the layer for the y-axis.
     *     Must be zero if <code>repeat</code> is false.
     * @param repeat Whether the layers should repeat vertically.
     */
    public MixedMaterial(final String name, final Row[] rows, final int biome, final Integer colour, final NoiseSettings variation, final double layerXSlope, final double layerYSlope, final boolean repeat) {
        this(name, rows, biome, Mode.LAYERED, 1.0f, colour, variation, layerXSlope, layerYSlope, repeat);
    }
    
    MixedMaterial(final String name, final Row[] rows, final int biome, final Mode mode, final float scale, final Integer colour, final NoiseSettings variation, final double layerXSlope, final double layerYSlope, final boolean repeat) {
        this.name = name;
        this.rows = rows;
        this.biome = biome;
        this.mode = mode;
        this.scale = scale;
        this.colour = colour;
        this.variation = variation;
        this.layerXSlope = layerXSlope;
        this.layerYSlope = layerYSlope;
        this.repeat = repeat;
        init();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getBiome() {
        return biome;
    }

    public Mode getMode() {
        return mode;
    }

    public float getScale() {
        return scale;
    }

    public Integer getColour() {
        return colour;
    }

    public NoiseSettings getVariation() {
        return variation;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public double getLayerXSlope() {
        return layerXSlope;
    }

    public double getLayerYSlope() {
        return layerYSlope;
    }

    public BufferedImage getIcon(ColourScheme colourScheme) {
        if (colourScheme != null) {
            final BufferedImage icon = new BufferedImage(16 * UI_SCALE, 16 * UI_SCALE, BufferedImage.TYPE_INT_RGB);
            // Draw the terrain
            if (colour != null) {
                for (int x = 1; x < 16 * UI_SCALE - 1; x++) {
                    for (int y = 1; y < 16 * UI_SCALE - 1; y++) {
                        icon.setRGB(x, y, colour);
                    }
                }
            } else {
                for (int x = 1; x < 16 * UI_SCALE - 1; x++) {
                    for (int y = 1; y < 16 * UI_SCALE - 1; y++) {
                        icon.setRGB(x, y, colourScheme.getColour(getMaterial(0, x * 2, 0, 15 - y * 2f)));
                    }
                }
            }
            return icon;
        } else {
            return UNKNOWN_ICON;
        }
    }
    
    public Material getMaterial(long seed, int x, int y, float z) {
        switch (mode) {
            case SIMPLE:
                return simpleMaterial;
            case NOISE:
                return materials[random.nextInt(totalCount)];
            case BLOBS:
                double xx = x / Constants.TINY_BLOBS, yy = y / Constants.TINY_BLOBS, zz = z / Constants.TINY_BLOBS;
                if (seed + 1 != noiseGenerators[0].getSeed()) {
                    for (int i = 0; i < noiseGenerators.length; i++) {
                        noiseGenerators[i].setSeed(seed + i + 1);
                    }
                }
                Material material = sortedRows[sortedRows.length - 1].material;
                for (int i = noiseGenerators.length - 1; i >= 0; i--) {
                    final float rowScale = sortedRows[i].scale * this.scale;
                    if (noiseGenerators[i].getPerlinNoise(xx / rowScale, yy / rowScale, zz / rowScale) >= sortedRows[i].chance) {
                        material = sortedRows[i].material;
                    }
                }
                return material;
            case LAYERED:
                if (layerNoiseheightMap != null) {
                    if (layerNoiseheightMap.getSeed() != seed) {
                        layerNoiseheightMap.setSeed(seed);
                    }
                    z += layerNoiseheightMap.getValue(x, y, z) - layerNoiseOffset;
                }
                if (repeat) {
                    if (layerXSlope != 0.0) {
                        z += layerXSlope * x;
                    }
                    if (layerYSlope != 0.0) {
                        z += layerYSlope * y;
                    }
                    return materials[Math.floorMod(Math.round(z), materials.length)];
                } else {
                    final int iZ = Math.round(z);
                    if (iZ < 0) {
                        return materials[0];
                    } else if (iZ >= materials.length) {
                        return materials[materials.length - 1];
                    } else {
                        return materials[iZ];
                    }
                }
            default:
                throw new InternalError();
        }
    }
    
    public Material getMaterial(long seed, int x, int y, int z) {
        switch (mode) {
            case SIMPLE:
                return simpleMaterial;
            case NOISE:
                return materials[random.nextInt(totalCount)];
            case BLOBS:
                double xx = x / Constants.TINY_BLOBS, yy = y / Constants.TINY_BLOBS, zz = z / Constants.TINY_BLOBS;
                if (seed + 1 != noiseGenerators[0].getSeed()) {
                    for (int i = 0; i < noiseGenerators.length; i++) {
                        noiseGenerators[i].setSeed(seed + i + 1);
                    }
                }
                Material material = sortedRows[sortedRows.length - 1].material;
                for (int i = noiseGenerators.length - 1; i >= 0; i--) {
                    final float rowScale = sortedRows[i].scale * this.scale;
                    if (noiseGenerators[i].getPerlinNoise(xx / rowScale, yy / rowScale, zz / rowScale) >= sortedRows[i].chance) {
                        material = sortedRows[i].material;
                    }
                }
                return material;
            case LAYERED:
                float fZ = z;
                if (layerNoiseheightMap != null) {
                    if (layerNoiseheightMap.getSeed() != seed) {
                        layerNoiseheightMap.setSeed(seed);
                    }
                    fZ += layerNoiseheightMap.getValue(x, y, z) - layerNoiseOffset;
                }
                if (repeat) {
                    if (layerXSlope != 0.0) {
                        fZ += layerXSlope * x;
                    }
                    if (layerYSlope != 0.0) {
                        fZ += layerYSlope * y;
                    }
                    return materials[Math.floorMod(Math.round(fZ), materials.length)];
                } else {
                    final int iZ = Math.round(fZ);
                    if (iZ < 0) {
                        return materials[0];
                    } else if (iZ >= materials.length) {
                        return materials[materials.length - 1];
                    } else {
                        return materials[iZ];
                    }
                }
            default:
                throw new InternalError();
        }
    }

    /**
     * Get the single material of which this mixed material consists, or
     * <code>null</code> if it contains more than one material.
     *
     * @return The single material of which this mixed material consists, or
     * <code>null</code> if it contains more than one material.
     */
    public Material getSingleMaterial() {
        return simpleMaterial;
    }

    public Row[] getRows() {
        return rows;
    }

    /**
     * Get the height of the pattern, or -1 if this does not apply because there
     * is no pattern.
     *
     * @return The height of the pattern, or -1 if this does not apply.
     */
    public int getPatternHeight() {
        return patternHeight;
    }

    void edit(final String name, final Row[] rows, final int biome, final Mode mode, final float scale, final Integer colour, final NoiseSettings variation, final double layerXSlope, final double layerYSlope, final boolean repeat) {
        this.name = name;
        this.rows = rows;
        this.biome = biome;
        this.mode = mode;
        this.scale = scale;
        this.colour = colour;
        this.variation = variation;
        this.layerXSlope = layerXSlope;
        this.layerYSlope = layerYSlope;
        this.repeat = repeat;
        init();
    }

    // Comparable
    
    @Override
    public int compareTo(MixedMaterial o) {
        if (name != null) {
            return (o.name != null) ? name.compareTo(o.name) : 1;
        } else {
            return (o.name != null) ? -1 : 0;
        }
    }

    // java.lang.Object
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MixedMaterial) && id.equals(((MixedMaterial) obj).id);
    }

    // Cloneable

    @Override
    public MixedMaterial clone() {
        return new MixedMaterial(name.startsWith("Copy of ") ? name : "Copy of " + name, rows, biome, mode, scale, colour, (variation != null) ? variation.clone() : null, layerXSlope, layerYSlope, repeat);
    }

    /**
     * Utility method for creating a simple mixed material, consisting of one
     * block type with data value 0, with a default name targeted to a specific
     * platform.
     *
     * @deprecated Use {@link #create(Platform, Material)}
     * @param platform The platform to which to target the name.
     * @param blockType The block type the mixed material should consist of
     * @return A new mixed material with the specified block type, and the
     *     block type's name
     */
    public static MixedMaterial create(final Platform platform, final int blockType) {
        return create(platform, Material.get(blockType));
    }

    /**
     * Utility method for creating a simple mixed material, consisting of one
     * material, with a default name targeted to a specific platform.
     *
     * @param platform The platform to which to target the name.
     * @param material The simple material the mixed material should consist of
     * @return A new mixed material with the specified material and an
     *     appropriate name
     */
    public static MixedMaterial create(final Platform platform, final Material material) {
        return create((platform == DefaultPlugin.JAVA_ANVIL_1_13) /* TODO make dynamic */ ? material.toString() : material.toLegacyString(), material);
    }

    /**
     * Utility method for creating a simple mixed material, consisting of one
     * material, with a specific name.
     *
     * @param name The name to give the mixed material.
     * @param material The simple material the mixed material should consist of
     * @return A new mixed material with the specified material and an
     *     appropriate name
     */
    public static MixedMaterial create(final String name, final Material material) {
        return new MixedMaterial(name, new Row(material, 3, 1.0f), -1, null);
    }

    /**
     * Perform a task during which any new materials deserialised <em>on the
     * same thread</em> will be duplicated and given new identities, instead of
     * being replaced with existing instances with the same identity if
     * available.
     *
     * @param task The task to perform.
     * @param <V> The return type of the task. May be {@link Void} for tasks
     *           which do not return a value.
     * @return The return value of the task or <code>null</code> if it does not
     *     return a value.
     * @throws RuntimeException If the task throws a checked exception it will
     * be wrapped in a <code>RuntimeException</code>.
     */
    public static <V> V duplicateNewMaterialsWhile(Callable<V> task) {
        DUPLICATE_MATERIALS.set(true);
        try {
            return task.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DUPLICATE_MATERIALS.set(false);
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy
        if (mode == null) {
            if (rows.length == 1) {
                mode = Mode.SIMPLE;
            } else if (noise) {
                mode = Mode.NOISE;
            } else {
                mode = Mode.BLOBS;
            }
        }
        
        init();
    }
    
    private Object readResolve() throws ObjectStreamException {
        return DUPLICATE_MATERIALS.get() ? MixedMaterialManager.getInstance().registerAsNew(this) : MixedMaterialManager.getInstance().register(this);
    }
    
    private void init() {
        totalCount = 0;
        for (Row row: rows) {
            totalCount += row.occurrence;
        }
        switch (mode) {
            case SIMPLE:
                if (rows.length != 1) {
                    throw new IllegalArgumentException("Only one row allowed for SIMPLE mode");
                }
                simpleMaterial = rows[0].material;
                patternHeight = -1;
                break;
            case NOISE:
                if (rows.length < 2) {
                    throw new IllegalArgumentException("Multiple rows required for NOISE mode");
                }
                materials = new Material[totalCount];
                int index = 0;
                for (Row row: rows) {
                    for (int i = 0; i < row.occurrence; i++) {
                        materials[index++] = row.material;
                    }
                }
                random = new Random();
                patternHeight = -1;
                break;
            case BLOBS:
                if (rows.length < 2) {
                    throw new IllegalArgumentException("Multiple rows required for BLOBS mode");
                }
                sortedRows = Arrays.copyOf(rows, rows.length);
                Arrays.sort(sortedRows, (r1, r2) -> r1.occurrence - r2.occurrence);
                noiseGenerators = new PerlinNoise[rows.length - 1];
                float cumulativePermillage = 0f;
                for (int i = 0; i < noiseGenerators.length; i++) {
                    noiseGenerators[i] = new PerlinNoise(0);
                    float permillage = sortedRows[i].occurrence * 1000f / totalCount;
                    cumulativePermillage += permillage * (1000 - cumulativePermillage) / 1000;
                    sortedRows[i].chance = PerlinNoise.getLevelForPromillage(cumulativePermillage);
                }
                patternHeight = -1;
                break;
            case LAYERED:
                if (rows.length < 2) {
                    throw new IllegalArgumentException("Multiple rows required for LAYERED mode");
                }
                if ((! repeat) && ((layerXSlope != 0) || (layerYSlope != 0))) {
                    throw new IllegalArgumentException("Angle may not be non-zero if repeat is false");
                }
                materials = new Material[totalCount];
                index = totalCount - 1;
                for (Row row: rows) {
                    for (int i = 0; i < row.occurrence; i++) {
                        materials[index--] = row.material;
                    }
                }
                if (variation != null) {
                    layerNoiseheightMap = new NoiseHeightMap(variation, NOISE_SEED_OFFSET);
                    layerNoiseOffset = variation.getRange();
                } else {
                    layerNoiseheightMap = null;
                    layerNoiseOffset = 0;
                }
                patternHeight = totalCount;
                break;
        }
    }

    private final UUID id = UUID.randomUUID();
    private String name;
    private int biome;
    private Row[] rows;
    @Deprecated
    private final boolean noise = false;
    private float scale;
    private Integer colour;
    private Mode mode;
    private NoiseSettings variation;
    private boolean repeat;
    private double layerXSlope, layerYSlope;
    private transient Row[] sortedRows;
    private transient PerlinNoise[] noiseGenerators;
    private transient Material[] materials;
    private transient Random random;
    private transient Material simpleMaterial;
    private transient NoiseHeightMap layerNoiseheightMap;
    private transient int layerNoiseOffset, patternHeight, totalCount;

    public static class Row implements Serializable {
        public Row(Material material, int count, float scale) {
            this.material = material;
            this.occurrence = count;
            this.scale = scale;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + (this.material != null ? this.material.hashCode() : 0);
            hash = 23 * hash + this.occurrence;
            hash = 23 * hash + Float.floatToIntBits(this.scale);
            hash = 23 * hash + Float.floatToIntBits(this.chance);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Row other = (Row) obj;
            if (this.material != other.material && (this.material == null || !this.material.equals(other.material))) {
                return false;
            }
            if (this.occurrence != other.occurrence) {
                return false;
            }
            if (Float.floatToIntBits(this.scale) != Float.floatToIntBits(other.scale)) {
                return false;
            }
            if (Float.floatToIntBits(this.chance) != Float.floatToIntBits(other.chance)) {
                return false;
            }
            return true;
        }

        public String toString() {
            return material.toString();
        }

        public final Material material;
        public final int occurrence; // Now referred to as "count" and no longer required to be a permillage
        public final float scale;
        float chance;

        private static final long serialVersionUID = 1L;
    }
    
    public enum Mode {SIMPLE, BLOBS, NOISE, LAYERED}

    private static final BufferedImage UNKNOWN_ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/icons/unknown_pattern.png");
    private static final long NOISE_SEED_OFFSET = 55904327L;
    private static final ThreadLocal<Boolean> DUPLICATE_MATERIALS = ThreadLocal.withInitial(() -> false);
    private static final long serialVersionUID = 1L;
}
