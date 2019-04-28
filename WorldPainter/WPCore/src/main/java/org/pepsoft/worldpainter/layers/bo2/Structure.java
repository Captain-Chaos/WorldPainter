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

    @Override
    public Point3i getDimensions() {
        List size = ((ListTag) root.getTag("size")).getValue();
        return new Point3i(((IntTag) size.get(0)).getValue(),
                ((IntTag) size.get(2)).getValue(),
                ((IntTag) size.get(1)).getValue());
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return blocks.get(new Point3i(x, y, z));
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        if (getAttribute(ATTRIBUTE_IGNORE_AIR)) {
            Material material = blocks.get(new Point3i(x, y, z));
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
        Structure clone = (Structure) super.clone();
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
        return load(name, new FileInputStream(file));
    }

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    public static Structure load(String objectName, InputStream inputStream) throws IOException {
        CompoundTag root;
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new BufferedInputStream(inputStream)))) {
            root = (CompoundTag) in.readTag();
        }

        // Load the palette
        ListTag paletteTag = (ListTag) root.getTag("palette");
        Material[] palette = new Material[paletteTag.getValue().size()];
        for (int i = 0; i < palette.length; i++) {
            CompoundTag entryTag = (CompoundTag) paletteTag.getValue().get(i);
            String name = ((StringTag) entryTag.getTag("Name")).getValue();
            CompoundTag propertiesTag = (CompoundTag) entryTag.getTag("Properties");
            Map<String, String> properties = (propertiesTag != null)
                    ? propertiesTag.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((StringTag) entry.getValue()).getValue()))
                    : null;
            palette[i] = Material.get(name, properties);
        }

        // Load the blocks and tile entities
        Map<Point3i, Material> blocks = new HashMap<>();
        ListTag<CompoundTag> blocksTag = (ListTag<CompoundTag>) root.getTag("blocks");
        List<TileEntity> tileEntities = new ArrayList<>();
        for (CompoundTag blockTag: blocksTag.getValue()) {
            List<IntTag> posTags = ((ListTag<IntTag>) blockTag.getTag("pos")).getValue();
            int x = posTags.get(0).getValue();
            int y = posTags.get(2).getValue();
            int z = posTags.get(1).getValue();
            blocks.put(new Point3i(x, y, z), palette[((IntTag) blockTag.getTag("state")).getValue()]);
            CompoundTag nbtTag = (CompoundTag) blockTag.getTag("nbt");
            if (nbtTag != null) {
                // This block is a tile entity
                TileEntity tileEntity = TileEntity.fromNBT(nbtTag);
                tileEntity.setX(x);
                tileEntity.setY(z);
                tileEntity.setZ(y);
                tileEntities.add(tileEntity);
            }
        }

        // Load the entities
        ListTag<CompoundTag> entitiesTag = (ListTag<CompoundTag>) root.getTag("entities");
        List<Entity> entities = new ArrayList<>(entitiesTag.getValue().size());
        for (CompoundTag entityTag: entitiesTag.getValue()) {
            entities.add(Entity.fromNBT(entityTag));
        }

        // Remove palette, blocks and entities from the tag so we don't waste space
        root.setTag("palette", null);
        root.setTag("blocks", null);
        root.setTag("entities", null);

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