package org.pepsoft.worldpainter.layers.bo2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
    private Structure(CompoundTag root, String name, List<Map<Point3i, Material>> blocks, List<Entity> entities, Map<Integer, List<TileEntity>> tileEntities) {
        this.root = root;
        this.name = name;
        this.blocks = null; // TODO remove in a future update (3.0 release)
        this.palettes = blocks;
        this.entities = entities;
        this.tileEntities = tileEntities;
        this.rng = new Random();
    }

    @Override
    public WPObject getObject() {
        return this;
        /* TODO: add support for selecting palettes
        int size = this.blocks.size();
        if (size == 1)
            return this;

        Map<Integer, Map<Point3i, Material>> blocks = Maps.newHashMap();
        Map<Integer, List<TileEntity>> tiles = Maps.newHashMap();
        int index = rng.nextInt(size);
        blocks.put(0, this.blocks.get(index));
        tiles.put(0, this.tileEntities.get(index));
        return new Structure(root, name, blocks, entities, tiles);
        */
    }

    @Override
    public List<WPObject> getAllObjects() {
        return Collections.singletonList(this);
    }

    @Override
    public void setSeed(long seed) {
        this.rng.setSeed(seed);
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
        return palettes.get(0).get(new Point3i(x, y, z));
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        if (getAttribute(ATTRIBUTE_IGNORE_AIR)) {
            Material material = palettes.get(0).get(new Point3i(x, y, z));
            return (material != null) && (material != AIR);
        } else {
            return palettes.get(0).containsKey(new Point3i(x, y, z));
        }
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return tileEntities.get(0);
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

        // Load the palette(s)
        ListTag paletteTag = (ListTag) root.getTag(TAG_PALETTE_);
        ArrayList<ListTag> palettes = new ArrayList<>();
        if (paletteTag != null) {
            // single palette
            palettes.add(paletteTag);
        } else {
            // assume use of multiple palettes
            paletteTag = (ListTag) root.getTag(TAG_PALETTES_);
            for (int i = 0; i < paletteTag.getValue().size(); i++) {
                palettes.add((ListTag)paletteTag.getValue().get(i));
            }
        }

        // Load the blocks and tile entities
        List<Map<Point3i, Material>> blocks = new ArrayList<>();
        Map<Integer, List<TileEntity>> tileEntities = Maps.newHashMap();
        ListTag<CompoundTag> blocksTag = (ListTag<CompoundTag>) root.getTag(TAG_BLOCKS_);

        for (int i = 0; i < palettes.size(); i++) {
            paletteTag = palettes.get(i);
            Material[] palette = new Material[paletteTag.getValue().size()];

            CompoundTag entryTag = (CompoundTag) paletteTag.getValue().get(i);
            String name = ((StringTag) entryTag.getTag(TAG_NAME)).getValue();
            CompoundTag propertiesTag = (CompoundTag) entryTag.getTag(TAG_PROPERTIES);
            Map<String, String> properties = (propertiesTag != null)
                    ? propertiesTag.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((StringTag) entry.getValue()).getValue()))
                    : null;
            palette[i] = Material.get(name, properties);

            Map<Point3i, Material> tempBlocks = Maps.newHashMap();
            List<TileEntity> tempTiles = new ArrayList<>();
            for (CompoundTag blockTag: blocksTag.getValue()) {
                List<IntTag> posTags = ((ListTag<IntTag>) blockTag.getTag(TAG_POS_)).getValue();
                int x = posTags.get(0).getValue();
                int y = posTags.get(2).getValue();
                int z = posTags.get(1).getValue();
                tempBlocks.put(new Point3i(x, y, z), palette[((IntTag) blockTag.getTag(TAG_STATE_)).getValue()]);
                CompoundTag nbtTag = (CompoundTag) blockTag.getTag(TAG_NBT_);
                if (nbtTag != null) {
                    // This block is a tile entity
                    TileEntity tileEntity = TileEntity.fromNBT(nbtTag);
                    tileEntity.setX(x);
                    tileEntity.setY(z);
                    tileEntity.setZ(y);
                    tempTiles.add(tileEntity);
                }
            }

            blocks.add(tempBlocks);
            tileEntities.put(i, tempTiles);
        }

        // Load the entities
        ListTag<CompoundTag> entitiesTag = (ListTag<CompoundTag>) root.getTag(TAG_ENTITIES_);
        List<Entity> entities = new ArrayList<>(entitiesTag.getValue().size());
        for (CompoundTag entityTag: entitiesTag.getValue()) {
            entities.add(Entity.fromNBT(entityTag));
        }

        // Remove palette, blocks and entities from the tag so we don't waste space
        root.setTag(TAG_PALETTE_, null);
        root.setTag(TAG_PALETTES_, null);
        root.setTag(TAG_BLOCKS_, null);
        root.setTag(TAG_ENTITIES_, null);

        return new Structure(root, objectName, blocks, (! entities.isEmpty()) ? ImmutableList.copyOf(entities) : null, (! tileEntities.isEmpty()) ? ImmutableMap.copyOf(tileEntities) : null);
    }

    private Object readResolve() {
        if (blocks != null) { // old versions have non-null blocks field
            return new Structure(root, name, new ArrayList<>(Collections.singletonList(blocks)), entities, tileEntities);
        }
        return this;
    }

    private final CompoundTag root;
    @Deprecated private final Map<Point3i, Material> blocks;
    private final List<Map<Point3i, Material>> palettes;
    private String name;
    private Map<String, Serializable> attributes;
    private final List<Entity> entities;
    private final Map<Integer, List<TileEntity>> tileEntities;
    private Random rng;

    public static final AttributeKey<Boolean> ATTRIBUTE_IGNORE_AIR = new AttributeKey<>("Structure.ignoreAir", true);

    private static final long serialVersionUID = 1L;
}