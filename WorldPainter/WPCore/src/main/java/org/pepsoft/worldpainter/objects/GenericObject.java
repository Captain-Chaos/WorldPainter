package org.pepsoft.worldpainter.objects;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.*;

/**
 * A generic {@link WPObject}, not tied to any particular platform or file
 * format.
 *
 * Created by Pepijn on 9-3-2017.
 */
public final class GenericObject extends NamedObjectWithAttributes {
    public GenericObject(String name, int dimX, int dimY, int dimZ, Material[] data) {
        this(name, dimX, dimY, dimZ, data, null, null, null);
    }

    public GenericObject(String name, int dimX, int dimY, int dimZ, Material[] data, List<Entity> entities, List<TileEntity> tileEntities, Map<String, Serializable> attributes) {
        super(name, ((attributes != null) && (! attributes.isEmpty())) ? new HashMap<>(attributes) : null);
        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;
        this.data = Arrays.copyOf(data, data.length);
        this.entities = ((entities != null) && (! entities.isEmpty())) ? new ArrayList<>(entities) : null;
        this.tileEntities = ((tileEntities != null) && (! tileEntities.isEmpty())) ? new ArrayList<>(tileEntities) : null;
        if ((dimX * dimY * dimZ) != data.length) {
            throw new IllegalArgumentException();
        }
    }

    public GenericObject(WPObject object) {
        super(object.getName(), ((object.getAttributes() != null) && (! object.getAttributes().isEmpty())) ? new HashMap<>(object.getAttributes()) : null);
        Point3i dims = object.getDimensions();
        this.dimX = dims.x;
        this.dimY = dims.y;
        this.dimZ = dims.z;
        data = new Material[dimX * dimY * dimY];
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    if (object.getMask(x, y, z)) {
                        data[x + y * dimX + z * dimX * dimY] = object.getMaterial(x, y, z);
                    }
                }
            }
        }
        this.entities = (object.getEntities() != null) ? new ArrayList<>(object.getEntities()) : null;
        this.tileEntities = (object.getTileEntities() != null) ? new ArrayList<>(object.getTileEntities()) : null;
    }

    @Override
    public Point3i getDimensions() {
        return new Point3i(dimX, dimY, dimZ);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return data[x + y * dimX + z * dimX * dimY];
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return data[x + y * dimX + z * dimX * dimY] != null;
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return tileEntities;
    }

    private final int dimX, dimY, dimZ;
    private final Material[] data;
    private final List<Entity> entities;
    private final List<TileEntity> tileEntities;

    private static final long serialVersionUID = 1L;
}