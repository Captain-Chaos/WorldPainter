package org.pepsoft.worldpainter.layers.bo2;

import org.jnbt.*;
import org.pepsoft.minecraft.AbstractNBTItem;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.DynamicList;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static java.util.stream.Collectors.toMap;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.minecraft.Material.MINECRAFT;

/**
 * A Sponge schematic according to the specification at
 * <a href="https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md">https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md</a>.
 */
public final class Schem extends AbstractNBTItem implements WPObject {
    public Schem(CompoundTag tag, String fallBackName) {
        super(tag);
        StringTag nameTag = (StringTag) getMap(TAG_METADATA).get(TAG_NAME);
        this.name = (nameTag != null) ? nameTag.getValue() : fallBackName;
        width = getShort(TAG_WIDTH);
        height = getShort(TAG_HEIGHT);
        length = getShort(TAG_LENGTH);
        Map<String, Tag> paletteMap = getMap(TAG_PALETTE);
        List<Material> paletteList = new DynamicList<>();
        paletteMap.forEach((key, value) -> {
            Material material = decodeMaterial(key);
            paletteList.set(((IntTag) value).getValue(), material);
        });
        palette = paletteList.toArray(new Material[paletteList.size()]);
        Tag blocksTag = getTag(TAG_BLOCK_DATA);
        if (blocksTag instanceof IntArrayTag) {
            blocks = ((IntArrayTag) blocksTag).getValue();
        } else if (blocksTag instanceof ByteArrayTag) {
            byte[] bytes = ((ByteArrayTag) blocksTag).getValue();
            blocks = new int[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                blocks[i] = bytes[i] & 0xff;
            }
        } else {
            throw new IllegalArgumentException("Unsupported tag type for BlockData: " + blocksTag.getClass().getSimpleName());
        }
        // Save memory
        removeTag(TAG_PALETTE);
        removeTag(TAG_BLOCK_DATA);

        Point3i offset;
        int[] schemOffset = getIntArray(TAG_OFFSET);
        if ((schemOffset[0] > -width) && (schemOffset[0] <= 0) && (schemOffset[2]> -length) && (schemOffset[2] <= 0) && (schemOffset[1] > -height) && (schemOffset[1] <= 0)) {
            // Schematic offset points inside the object; use it as the default
            offset = new Point3i(schemOffset[0], schemOffset[2], schemOffset[1]);
        } else {
            offset = guestimateOffset();
        }
        if ((offset.x != 0) || (offset.y != 0) || (offset.z != 0)) {
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
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        // TODO
        return null;
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

    private final int width, height, length;
    private final Material[] palette;
    private final int[] blocks;
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

    private static final long serialVersionUID = 1L;
}