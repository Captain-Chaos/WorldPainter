package org.pepsoft.worldpainter.layers.bo2;

import org.jetbrains.annotations.NotNull;
import org.jnbt.*;
import org.pepsoft.minecraft.AbstractNBTItem;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.DynamicList;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exception.WPRuntimeException;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.minecraft.Material.MINECRAFT;

/**
 * A Sponge schematic according to the specification at
 * <a href="https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md">https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md</a>.
 */
public final class Schem extends AbstractNBTItem implements WPObject {
    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    public Schem(CompoundTag tag, String fallBackName) {
        super(tag);
        final int version = getInt(TAG_VERSION);
        final StringTag nameTag = (getMap(TAG_METADATA) != null) ? (StringTag) getMap(TAG_METADATA).get(TAG_NAME) : null;
        this.name = (nameTag != null) ? nameTag.getValue() : fallBackName;
        width = getShort(TAG_WIDTH);
        height = getShort(TAG_HEIGHT);
        length = getShort(TAG_LENGTH);

        final Map<String, Tag> paletteMap;
        final Tag blockDataTag;
        final Tag tileEntitiesTag;
        final Tag entitiesTag;
        switch (version) {
            case 1:
                paletteMap = getMap(TAG_PALETTE);
                blockDataTag = getTag(TAG_BLOCK_DATA);
                tileEntitiesTag = getTag(TAG_TILE_ENTITIES);
                entitiesTag = null;
                // Save memory
                removeTag(TAG_PALETTE);
                removeTag(TAG_BLOCK_DATA);
                removeTag(TAG_TILE_ENTITIES);
                break;
            case 2:
                paletteMap = getMap(TAG_PALETTE);
                blockDataTag = getTag(TAG_BLOCK_DATA);
                tileEntitiesTag = getTag(TAG_BLOCK_ENTITIES);
                entitiesTag = getTag(TAG_ENTITIES);
                // Save memory
                removeTag(TAG_PALETTE);
                removeTag(TAG_BLOCK_DATA);
                removeTag(TAG_BLOCK_ENTITIES);
                removeTag(TAG_ENTITIES);
                break;
            case 3:
                final CompoundTag blocksTag = (CompoundTag) getTag(TAG_BLOCKS);
                paletteMap = ((CompoundTag) blocksTag.getTag(TAG_PALETTE)).getValue();
                blockDataTag = blocksTag.getTag(TAG_DATA);
                tileEntitiesTag = blocksTag.getTag(TAG_BLOCK_ENTITIES);
                entitiesTag = getTag(TAG_ENTITIES);
                // Save memory
                removeTag(TAG_BLOCKS);
                removeTag(TAG_ENTITIES);
                break;
            default:
                throw new IllegalArgumentException("Schem version " + version + " not supported");
        }

        final List<Material> paletteList = new DynamicList<>();
        paletteMap.forEach((key, value) -> {
            Material material = decodeMaterial(key);
            paletteList.set(((IntTag) value).getValue(), material);
        });
        palette = paletteList.toArray(new Material[paletteList.size()]);
        if (blockDataTag instanceof IntArrayTag) {
            // TODO since this is supposed to be varints; if we get here are these really straight integers?
            blocks = ((IntArrayTag) blockDataTag).getValue();
        } else if (blockDataTag instanceof ByteArrayTag) {
            final byte[] bytes = ((ByteArrayTag) blockDataTag).getValue();
            blocks = new int[width * height * length];
            final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = readVarInt(bais);
            }
        } else {
            throw new IllegalArgumentException("Unsupported tag type for BlockData or Blocks/Data: " + blockDataTag.getClass().getSimpleName());
        }

        if (tileEntitiesTag instanceof ListTag) {
            tileEntities = new ArrayList<>(((ListTag<?>) tileEntitiesTag).getValue().size());
            for (CompoundTag tileEntityTag: ((ListTag<CompoundTag>) tileEntitiesTag).getValue()) {
                tileEntities.add(getTileEntity(tileEntityTag));
            }
        } else if (tileEntitiesTag != null) {
            throw new IllegalArgumentException("Unsupported tag type for TileEntities or Blocks/BlockEntities: " + tileEntitiesTag.getClass().getSimpleName());
        } else {
            tileEntities = null;
        }

        if (entitiesTag instanceof ListTag) {
            entities = new ArrayList<>(((ListTag<CompoundTag>) entitiesTag).getValue().size());
            for (CompoundTag entityTag: ((ListTag<CompoundTag>) entitiesTag).getValue()) {
                entities.add(getEntity(entityTag, version));
            }
        } else if (entitiesTag != null) {
            throw new IllegalArgumentException("Unsupported tag type for Entities: " + entitiesTag.getClass().getSimpleName());
        } else {
            entities = null;
        }

        Point3i offset;
        int[] schemOffset = getIntArray(TAG_OFFSET);
        if ((schemOffset != null) && (schemOffset[0] > -width) && (schemOffset[0] <= 0) && (schemOffset[2]> -length) && (schemOffset[2] <= 0) && (schemOffset[1] > -height) && (schemOffset[1] <= 0)) {
            // Schematic offset points inside the object; use it as the default
            offset = new Point3i(schemOffset[0], schemOffset[2], schemOffset[1]);
        } else {
            offset = guestimateOffset();
        }
        if ((offset != null) && ((offset.x != 0) || (offset.y != 0) || (offset.z != 0))) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(ATTRIBUTE_OFFSET.key, offset);
        }
    }

    // WPObject

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
        return new Point3i(width, length, height);
    }

    @Override
    public Point3i getOffset() {
        return getAttribute(ATTRIBUTE_OFFSET);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return palette[blocks[x + y * width + z * width * length]];
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        Material material = palette[blocks[x + y * width + z * width * length]];
        return material != AIR;
    }

    @Override
    public Set<Material> getAllMaterials() {
        return new HashSet<>(asList(palette));
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
    public void prepareForExport(Dimension dimension) {
        // Do nothing
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    @SuppressWarnings("unchecked") // Responsibility of caller
    public <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        return key.get(attributes);
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

    // Object

    @Override
    public Schem clone() {
        Schem clone = (Schem) super.clone();
        if (attributes != null) {
            clone.attributes = new HashMap<>(attributes);
        }
        return clone;
    }

    public static Schem load(File file) throws IOException {
        final Schem schem = load(new FileInputStream(file), file.getName().substring(0, file.getName().lastIndexOf('.')));
        schem.setAttribute(ATTRIBUTE_FILE, file);
        return schem;
    }

    public static Schem load(InputStream stream, String fallBackName) throws IOException {
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(stream))) {
            return new Schem((CompoundTag) in.readTag(), fallBackName);
        }
    }

    private Material decodeMaterial(String str) {
        String name;
        Map<String, String> properties;
        int p = str.indexOf('[');
        if (p != -1) {
            name = str.substring(0, p);
            properties = Arrays.stream(str.substring(p + 1, str.length() - 1)
                    .split(","))
                    .map(String::trim)
                    .collect(toMap(s -> s.substring(0, s.indexOf('=')).trim(),
                            s -> s.substring(s.indexOf('=') + 1).trim()));
        } else {
            name = str;
            properties = null;
        }
        if (name.indexOf(':') == -1) {
            name = MINECRAFT + ':' + name;
        }
        return Material.get(name, properties);
    }

    private int readVarInt(InputStream is) {
        try {
            int i = 0;
            int byteCount = 0;
            while (true) {
                final int _byte = is.read();
                i |= (_byte & 0x7F) << byteCount++ * 7;
                if (byteCount > 5) {
                    throw new WPRuntimeException("VarInt too big");
                }
                if ((_byte & 0x80) != 128) {
                    break;
                }
            }
            return i;
        } catch (IOException e) {
            throw new WPRuntimeException(e);
        }
    }

    @NotNull
    private TileEntity getTileEntity(CompoundTag tileEntityTag) {
        String id = null;
        int x = 0, y = 0, z = 0;
        for (Iterator<Map.Entry<String, Tag>> i = tileEntityTag.getValue().entrySet().iterator(); i.hasNext(); ) {
            Tag extraTag = i.next().getValue();
            if (extraTag.getName().equals(TAG_ID)) {
                id = ((StringTag) extraTag).getValue();
                i.remove();
            } else if (extraTag.getName().equals(TAG_POS)) {
                x = ((IntArrayTag) extraTag).getValue()[0];
                y = ((IntArrayTag) extraTag).getValue()[1];
                z = ((IntArrayTag) extraTag).getValue()[2];
                i.remove();
            }
        }
        tileEntityTag.setTag(TAG_ID_, new StringTag(TAG_ID_, id));
        tileEntityTag.setTag(TAG_X_, new IntTag(TAG_X_, x));
        tileEntityTag.setTag(TAG_Y_, new IntTag(TAG_Y_, y));
        tileEntityTag.setTag(TAG_Z_, new IntTag(TAG_Z_, z));
        return TileEntity.fromNBT(tileEntityTag);
    }

    @SuppressWarnings("unchecked") // Guaranteed by standard
    private Entity getEntity(CompoundTag entityTag, int version) {
        String id = null;
        double x = 0, y = 0, z = 0;
        CompoundTag dataTag = null;
        for (Iterator<Map.Entry<String, Tag>> i = entityTag.getValue().entrySet().iterator(); i.hasNext(); ) {
            Tag extraTag = i.next().getValue();
            if (extraTag.getName().equals(TAG_ID)) {
                id = ((StringTag) extraTag).getValue();
                i.remove();
            } else if (extraTag.getName().equals(TAG_POS)) {
                final List<DoubleTag> posTags = ((ListTag<DoubleTag>) extraTag).getValue();
                x = posTags.get(0).getValue();
                y = posTags.get(1).getValue();
                z = posTags.get(2).getValue();
                i.remove();
            } else if (extraTag.getName().equals(TAG_DATA)) {
                dataTag = (CompoundTag) extraTag;
            }
        }
        if (version >= 3) {
            dataTag.setTag(TAG_ID_, new StringTag(TAG_ID_, id));
            return Entity.fromNBT(dataTag, new double[] {x, y, z});
        } else {
            entityTag.setTag(TAG_ID_, new StringTag(TAG_ID_, id));
            return Entity.fromNBT(entityTag, new double[] {x, y, z});
        }
    }

    private final int width, height, length;
    private final Material[] palette;
    private final int[] blocks;
    private final List<TileEntity> tileEntities;
    private final List<Entity> entities;
    private String name;
    private Map<String, Serializable> attributes;

    private static final String TAG_WIDTH = "Width";
    private static final String TAG_HEIGHT = "Height";
    private static final String TAG_LENGTH = "Length";
    private static final String TAG_OFFSET = "Offset";
    private static final String TAG_PALETTE = "Palette";
    private static final String TAG_METADATA = "Metadata";
    private static final String TAG_NAME = "Name";
    private static final String TAG_BLOCK_DATA = "BlockData";
    private static final String TAG_BLOCK_ENTITIES = "BlockEntities";

    private static final long serialVersionUID = 1L;
}