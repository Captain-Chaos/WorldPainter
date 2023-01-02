package org.pepsoft.worldpainter.layers.bo2;

import com.google.common.collect.ImmutableList;
import org.jnbt.*;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.objects.AbstractObject;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AIR;

/**
 * Created by Pepijn on 26-6-2016.
 */
public class Structure extends AbstractObject implements Bo2ObjectProvider {
    private Structure(CompoundTag root, String name, Map<Point3i, Material> blocks, List<Entity> entities, List<TileEntity> tileEntities) {
        this.root = root;
        this.name = name;
        this.blocks = blocks;
        this.entities = entities;
        this.tileEntities = tileEntities;
    }

    @Override
    public WPObject getObject() {
        return this;
    }

    @Override
    public List<WPObject> getAllObjects() {
        return Collections.singletonList(this);
    }

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    @Override
    public Point3i getDimensions() {
        final List<IntTag> size = ((ListTag<IntTag>) root.getTag("size")).getValue();
        return new Point3i(size.get(0).getValue(),
                size.get(2).getValue(),
                size.get(1).getValue());
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return blocks.get(new Point3i(x, y, z));
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        if (getAttribute(ATTRIBUTE_IGNORE_AIR)) {
            final Material material = blocks.get(new Point3i(x, y, z));
            return (material != null) && (material != AIR);
        } else {
            return blocks.containsKey(new Point3i(x, y, z));
        }
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return tileEntities;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        if (value != null) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key.key, value);
        } else if (attributes != null) {
            attributes.remove(key.key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        }
    }

    // Cloneable

    @Override
    public Structure clone() {
        final Structure clone = (Structure) super.clone();
        if (attributes != null) {
            clone.attributes = new HashMap<>(attributes);
        }
        return clone;
    }

    public static Structure load(File file) throws IOException {
        String name = file.getName();
        if (name.toLowerCase().endsWith(".nbt")) {
            name = name.substring(0, name.length() - 4).trim();
        }
        final Structure structure = load(name, new FileInputStream(file));
        structure.setAttribute(ATTRIBUTE_FILE, file);
        return structure;
    }

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    public static Structure load(String objectName, InputStream inputStream) throws IOException {
        final CompoundTag root;
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new BufferedInputStream(inputStream)))) {
            root = (CompoundTag) in.readTag();
        }

        // Load the palette
        final ListTag<CompoundTag> paletteTag = (ListTag<CompoundTag>) root.getTag(TAG_PALETTE_);
        if (paletteTag == null) {
            throw new IllegalArgumentException(TAG_PALETTE_ + " tag missing from object " + objectName + " (root tag contents: " + root.getValue() + ")");
        }
        final Material[] palette = new Material[paletteTag.getValue().size()];
        for (int i = 0; i < palette.length; i++) {
            final CompoundTag entryTag = paletteTag.getValue().get(i);
            final String name = ((StringTag) entryTag.getTag(TAG_NAME)).getValue();
            final CompoundTag propertiesTag = (CompoundTag) entryTag.getTag(TAG_PROPERTIES);
            final Map<String, String> properties = (propertiesTag != null)
                    ? propertiesTag.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((StringTag) entry.getValue()).getValue()))
                    : null;
            palette[i] = Material.get(name, properties);
        }

        // Load the blocks and tile entities
        final Map<Point3i, Material> blocks = new HashMap<>();
        final ListTag<CompoundTag> blocksTag = (ListTag<CompoundTag>) root.getTag(TAG_BLOCKS_);
        if (blocksTag == null) {
            throw new IllegalArgumentException(TAG_BLOCKS_ + " tag missing from object " + objectName + " (root tag contents: " + root.getValue() + ")");
        }
        final List<TileEntity> tileEntities = new ArrayList<>();
        for (CompoundTag blockTag: blocksTag.getValue()) {
            final List<IntTag> posTags = ((ListTag<IntTag>) blockTag.getTag(TAG_POS_)).getValue();
            final int x = posTags.get(0).getValue();
            final int y = posTags.get(2).getValue();
            final int z = posTags.get(1).getValue();
            blocks.put(new Point3i(x, y, z), palette[((IntTag) blockTag.getTag(TAG_STATE_)).getValue()]);
            final CompoundTag nbtTag = (CompoundTag) blockTag.getTag(TAG_NBT_);
            if (nbtTag != null) {
                // This block is a tile entity
                final TileEntity tileEntity = TileEntity.fromNBT(nbtTag);
                tileEntity.setX(x);
                tileEntity.setY(z);
                tileEntity.setZ(y);
                tileEntities.add(tileEntity);
            }
        }

        // Load the entities
        final ListTag<CompoundTag> entitiesTag = (ListTag<CompoundTag>) root.getTag(TAG_ENTITIES_);
        final List<Entity> entities = new ArrayList<>();
        if (entitiesTag != null) {
            for (CompoundTag entityTag: entitiesTag.getValue()) {
                double[] relPos = null;
                if (entityTag.getTag(TAG_POS_) instanceof ListTag) {
                    relPos = ((ListTag<DoubleTag>) entityTag.getTag(TAG_POS_)).getValue().stream().mapToDouble(DoubleTag::getValue).toArray();
                }
                if (entityTag.getTag(TAG_NBT_) instanceof CompoundTag) {
                    entityTag = (CompoundTag) entityTag.getTag(TAG_NBT_);
                }
                entities.add(Entity.fromNBT(entityTag, relPos));
            }
        }

        // Remove palette, blocks and entities from the tag so we don't waste space
        root.setTag(TAG_PALETTE_, null);
        root.setTag(TAG_BLOCKS_, null);
        root.setTag(TAG_ENTITIES_, null);

        return new Structure(root, objectName, blocks, (! entities.isEmpty()) ? ImmutableList.copyOf(entities) : null, (! tileEntities.isEmpty()) ? ImmutableList.copyOf(tileEntities) : null);
    }

    private final CompoundTag root;
    private final Map<Point3i, Material> blocks;
    private String name;
    private Map<String, Serializable> attributes;
    private final List<Entity> entities;
    private final List<TileEntity> tileEntities;

    public static final AttributeKey<Boolean> ATTRIBUTE_IGNORE_AIR = new AttributeKey<>("Structure.ignoreAir", true);

    private static final long serialVersionUID = 1L;
}