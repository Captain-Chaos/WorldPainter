/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.vecmath.Point3i;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.AbstractNBTItem;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 *
 * @author pepijn
 */
public final class Schematic extends AbstractNBTItem implements WPObject, Bo2ObjectProvider {
    public Schematic(String name, CompoundTag tag, Map<String, Serializable> attributes) {
        super(tag);
        this.name = name;
        materials = getString("Materials");
        if (! materials.equalsIgnoreCase("Alpha")) {
            throw new IllegalArgumentException("Unsupported materials type " + materials);
        }
        blocks = getByteArray("Blocks");
        addBlocks = getByteArray("AddBlocks");
        data = getByteArray("Data");
        List<Tag> entityTags = getList("Entities");
        if (entityTags.isEmpty()) {
            entities = null;
        } else {
            entities = new ArrayList<Entity>(entityTags.size());
            for (Tag entityTag: entityTags) {
                entities.add(new Entity((CompoundTag) entityTag));
            }
        }
        List<Tag> tileEntityTags = getList("TileEntities");
        if (tileEntityTags.isEmpty()) {
            tileEntities = null;
        } else {
            tileEntities = new ArrayList<TileEntity>(tileEntityTags.size());
            for (Tag tileEntityTag: tileEntityTags) {
                tileEntities.add(TileEntity.fromNBT(tileEntityTag));
            }
        }
        width = getShort("Width");
        length = getShort("Length");
        height = getShort("Height");
        Point3i offset = null;
        if (containsTag("WEOffsetX")) {
            weOffsetX = getInt("WEOffsetX");
            weOffsetY = getInt("WEOffsetY");
            weOffsetZ = getInt("WEOffsetZ");
//            System.out.println("Schematic has offset tag: " + weOffsetX + ", " + weOffsetY + ", " + weOffsetZ);
            if ((weOffsetX > -width) && (weOffsetX <= 0) && (weOffsetZ> -length) && (weOffsetZ <= 0) && (weOffsetY > -height) && (weOffsetY <= 0)) {
                // Schematic offset points inside the object; use it as the default
//                System.out.println("That's inside");
                offset = new Point3i(weOffsetX, weOffsetZ, weOffsetY);
            }
        } else {
            weOffsetX = weOffsetY = weOffsetZ = 0;
        }
        if (offset == null) {
            int offsetZ = Integer.MIN_VALUE, lowestX = 0, highestX = 0, lowestY = 0, highestY = 0;
            for (int z = 0; (z < height) && (offsetZ == Integer.MIN_VALUE); z++) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < length; y++) {
                        if (getMask(x, y, z)) {
                            if (offsetZ == Integer.MIN_VALUE) {
                                offsetZ = z;
                                lowestX = highestX = x;
                                lowestY = highestY = y;
                            } else {
                                if (x < lowestX) {
                                    lowestX = x;
                                } else if (x > highestX) {
                                    highestX = x;
                                }
                                if (y < lowestY) {
                                    lowestY = y;
                                } else if (y > highestY) {
                                    highestY = y;
                                }
                            }
                        }
                    }
                }
            }
            if (offsetZ > Integer.MIN_VALUE) {
                offset = new Point3i(-(lowestX + highestX) / 2, -(lowestY + highestY) / 2, -offsetZ);
            }
        }
        if ((offset != null) && ((offset.x != 0) || (offset.y != 0) || (offset.z != 0))) {
            if (attributes == null) {
                attributes = new HashMap<String, Serializable>();
            }
            attributes.put(ATTRIBUTE_OFFSET, offset);
        }
        if (containsTag("WEOriginX")) {
            weOriginX = getInt("WEOriginX");
            weOriginY = getInt("WEOriginY");
            weOriginZ = getInt("WEOriginZ");
        } else {
            weOriginX = weOriginY = weOriginZ = 0;
        }
        dimensions = new Point3i(width, length, height);
        this.attributes = attributes;
    }

    // WPObject
    
    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        final int offset = blockOffset(x, y, z);
        int blockId = blocks[offset] & 0xFF;
        if (addBlocks != null) {
            if ((offset & 1) == 0) {
                // Even offset; first nibble
                blockId |= (addBlocks[offset >> 1] & 0x0f) << 8;
            } else {
                // Odd offset; second nibble
                blockId |= (addBlocks[offset >> 1] & 0xf0) << 4;
            }
        }
        return Material.get(blockId, data[offset] & 0xF);
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return blocks[blockOffset(x, y, z)] != BLK_AIR;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    // Bo2ObjectProvider
    
    @Override
    public WPObject getObject() {
        return this;
    }
    
    @Override
    public List<WPObject> getAllObjects() {
        return Collections.singletonList((WPObject) this);
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    @SuppressWarnings("unchecked") // Responsibility of caller
    public <T extends Serializable> T getAttribute(String key, T _default) {
        if ((attributes != null) && attributes.containsKey(key)) {
            return (T) attributes.get(key);
        } else {
            return _default;
        }
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public void setAttribute(String key, Serializable value) {
        if (value != null) {
            if (attributes == null) {
                attributes = new HashMap<String, Serializable>();
            }
            attributes.put(key, value);
        } else if (attributes != null) {
            attributes.remove(key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        }
    }

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public Schematic clone() {
        Schematic clone = (Schematic) super.clone();
        clone.dimensions = (Point3i) dimensions.clone();
        if (origin != null) {
            clone.origin = (Point3i) origin.clone();
        }
        if (entities != null) {
            clone.entities = new ArrayList<Entity>(entities.size());
            for (Entity entity: entities) {
                clone.entities.add((Entity) entity.clone());
            }
        }
        if (tileEntities != null) {
            clone.tileEntities = new ArrayList<TileEntity>(tileEntities.size());
            for (TileEntity tileEntity: tileEntities) {
                clone.tileEntities.add((TileEntity) tileEntity.clone());
            }
        }
        if (attributes != null) {
            clone.attributes = new HashMap<String, Serializable>(attributes);
        }
        return clone;
    }

    public static Schematic load(File file) throws IOException {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        return load(name, file);
    }
    
    public static Schematic load(String name, File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            byte[] magicNumber = new byte[2];
            in.mark(2);
            in.read(magicNumber);
            in.reset();
            if ((magicNumber[0] == (byte) 0x1f) && (magicNumber[1] == (byte) 0x8b)) {
                in = new GZIPInputStream(in);
            }
            NBTInputStream nbtIn = new NBTInputStream(in);
            CompoundTag tag = (CompoundTag) nbtIn.readTag();
            Map<String, Serializable> attributes = new HashMap<String, Serializable>();
            attributes.put(WPObject.ATTRIBUTE_FILE, file);
            return new Schematic(name, tag, attributes);
        } finally {
            in.close();
        }
    }
    
    private int blockOffset(int x, int y, int z) {
        return x + width * y + width * length * z;
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy
        if (origin != null) {
            if (attributes == null) {
                attributes = new HashMap<String, Serializable>();
            }
            attributes.put(WPObject.ATTRIBUTE_OFFSET, new Point3i(-origin.x, -origin.y, -origin.z));
            origin = null;
        }
        if (version == 0) {
            if (! attributes.containsKey(ATTRIBUTE_LEAF_DECAY_MODE)) {
                attributes.put(ATTRIBUTE_LEAF_DECAY_MODE, LEAF_DECAY_ON);
            }
            version = 1;
        }
    }
    
    private String name;
    private final String materials;
    private final byte[] data, blocks, addBlocks;
    private final short width, length, height;
    private final int weOffsetX, weOffsetY, weOffsetZ;
    private final int weOriginX, weOriginY, weOriginZ;
    private List<Entity> entities;
    private List<TileEntity> tileEntities;
    private Point3i dimensions;
    @Deprecated
    private Point3i origin;
    private Map<String, Serializable> attributes;
    private int version = 1;
    
    private static final long serialVersionUID = 1L;
}