package org.pepsoft.worldpainter.layers.bo2;

import org.jnbt.*;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.objects.AbstractObject;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by Pepijn on 26-6-2016.
 */
public class Structure extends AbstractObject implements Bo2ObjectProvider {
    private Structure(CompoundTag root, String name, Map<Point3i, Material> blocks) {
        this.root = root;
        this.name = name;
        this.blocks = blocks;
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
        return blocks.containsKey(new Point3i(x, y, z));
    }

    @Override
    public List<Entity> getEntities() {
        // TODO
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        // TODO
        return null;
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

        // Load the blocks
        Map<Point3i, Material> blocks = new HashMap<>();
        ListTag<CompoundTag> blocksTag = (ListTag<CompoundTag>) root.getTag("blocks");
        for (CompoundTag blockTag: blocksTag.getValue()) {
            List<IntTag> posTags = ((ListTag<IntTag>) blockTag.getTag("pos")).getValue();
            blocks.put(new Point3i(posTags.get(0).getValue(), posTags.get(2).getValue(), posTags.get(1).getValue()), palette[((IntTag) blockTag.getTag("state")).getValue()]);
        }

        // Remove palette and blocks from the tag so we don't waste space
        root.setTag("palette", null);
        root.setTag("blocks", null);

        return new Structure(root, objectName, blocks);
    }

    private final CompoundTag root;
    private final Map<Point3i, Material> blocks;
    private String name;
    private Map<String, Serializable> attributes;

    private static final long serialVersionUID = 1L;
}