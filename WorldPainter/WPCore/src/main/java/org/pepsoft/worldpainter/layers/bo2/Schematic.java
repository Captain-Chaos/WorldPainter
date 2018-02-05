/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.AbstractNBTItem;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.pepsoft.minecraft.Constants.BLK_AIR;

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
        List<CompoundTag> entityTags = getList("Entities");
        if (entityTags.isEmpty()) {
            entities = null;
        } else {
            entities = new ArrayList<>(entityTags.size());
            entities.addAll(entityTags.stream().map(Entity::fromNBT).collect(Collectors.toList()));
        }
        List<CompoundTag> tileEntityTags = getList("TileEntities");
        if (tileEntityTags.isEmpty()) {
            tileEntities = null;
        } else {
            tileEntities = new ArrayList<>(tileEntityTags.size());
            tileEntities.addAll(tileEntityTags.stream().map(TileEntity::fromNBT).collect(Collectors.toList()));
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
        dimensions = new Point3i(width, length, height);
        if (offset == null) {
            offset = guestimateOffset();
        }
        if ((offset != null) && ((offset.x != 0) || (offset.y != 0) || (offset.z != 0))) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(ATTRIBUTE_OFFSET.key, offset);
        }
        if (containsTag("WEOriginX")) {
            weOriginX = getInt("WEOriginX");
            weOriginY = getInt("WEOriginY");
            weOriginZ = getInt("WEOriginZ");
        } else {
            weOriginX = weOriginY = weOriginZ = 0;
        }
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
    public void prepareForExport(Dimension dimension) {
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
    
    // Bo2ObjectProvider
    
    @Override
    public WPObject getObject() {
        return this;
    }
    
    @Override
    public List<WPObject> getAllObjects() {
        return Collections.singletonList(this);
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

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public Point3i getOffset() {
        return getAttribute(ATTRIBUTE_OFFSET);
    }

    @Override
    public Schematic clone() {
        Schematic clone = (Schematic) super.clone();
        clone.dimensions = (Point3i) dimensions.clone();
        if (origin != null) {
            clone.origin = (Point3i) origin.clone();
        }
        if (entities != null) {
            clone.entities = new ArrayList<>(entities.size());
            clone.entities.addAll(entities.stream().map(entity -> (Entity) entity.clone()).collect(Collectors.toList()));
        }
        if (tileEntities != null) {
            clone.tileEntities = new ArrayList<>(tileEntities.size());
            clone.tileEntities.addAll(tileEntities.stream().map(tileEntity -> (TileEntity) tileEntity.clone()).collect(Collectors.toList()));
        }
        if (attributes != null) {
            clone.attributes = new HashMap<>(attributes);
        }
        return clone;
    }

    /**
     * Load a custom object in schematic format from a file. The name of the
     * object will be the name of the file, minus the extension.
     *
     * @param file The file from which to load the object.
     * @return A new <code>Schematic</code> containing the contents of the
     *     specified file.
     * @throws IOException If an I/O error occurred while reading the file.
     */
    public static Schematic load(File file) throws IOException {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        return load(name, file);
    }
    
    /**
     * Load a custom object in schematic format from a file.
     *
     * @param name The name of the object.
     * @param file The file from which to load the object.
     * @return A new <code>Schematic</code> containing the contents of the
     *     specified file.
     * @throws IOException If an I/O error occurred while reading the file.
     */
    public static Schematic load(String name, File file) throws IOException {
        Schematic object = load(name, new FileInputStream(file));
        object.setAttribute(WPObject.ATTRIBUTE_FILE, file);
        return object;
    }

    /**
     * Load a custom object in schematic format from an input stream. The stream
     * is closed before exiting the method.
     *
     * @param name The name of the object.
     * @param stream The input stream from which to load the object.
     * @return A new <code>Schematic</code> containing the contents of the
     *     specified stream.
     * @throws IOException If an I/O error occurred while reading the stream.
     */
    public static Schematic load(String name, InputStream stream) throws IOException {
        InputStream in = new BufferedInputStream(stream);
        //noinspection TryFinallyCanBeTryWithResources // Not possible due to assignment of 'in' inside block
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
            return new Schematic(name, tag, null);
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
                attributes = new HashMap<>();
            }
            attributes.put(ATTRIBUTE_OFFSET.key, new Point3i(-origin.x, -origin.y, -origin.z));
            origin = null;
        }
        if (version == 0) {
            if (! attributes.containsKey(ATTRIBUTE_LEAF_DECAY_MODE.key)) {
                attributes.put(ATTRIBUTE_LEAF_DECAY_MODE.key, LEAF_DECAY_ON);
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