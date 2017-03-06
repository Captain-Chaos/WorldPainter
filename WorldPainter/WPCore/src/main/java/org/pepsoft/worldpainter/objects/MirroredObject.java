/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.MathUtils;

/**
 *
 * @author pepijn
 */
public class MirroredObject extends AbstractObject {
    public MirroredObject(WPObject object, boolean mirrorYAxis) {
        this.object = object;
        this.mirrorYAxis = mirrorYAxis;
        dimensions = object.getDimensions();
        Map<String, Serializable> attributes = (object.getAttributes() != null) ? new HashMap<>(object.getAttributes()) : new HashMap<>();
        Point3i offset = object.getOffset();
        offset = mirrorYAxis
            ? new Point3i(offset.x, -(dimensions.y - (-offset.y) - 1), offset.z)
            : new Point3i(-(dimensions.x - (-offset.x) - 1), offset.y, offset.z);
        if ((offset.x != 0) || (offset.y != 0) || (offset.z != 0)) {
            attributes.put(ATTRIBUTE_OFFSET.key, offset);
        } else {
            attributes.remove(ATTRIBUTE_OFFSET.key);
        }
        if (! attributes.isEmpty()) {
            this.attributes = attributes;
        } else {
            this.attributes = null;
        }
    }

    @Override
    public Point3i getDimensions() {
        return object.getDimensions();
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return mirrorYAxis
            ? object.getMaterial(x, dimensions.y - y - 1, z).mirror(Direction.NORTH)
            : object.getMaterial(dimensions.x - x - 1, y, z).mirror(Direction.EAST);
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return mirrorYAxis
            ? object.getMask(x, dimensions.y - y - 1, z)
            : object.getMask(dimensions.x - x - 1, y, z);
    }

    @Override
    public List<Entity> getEntities() {
        List<Entity> objectEntities = object.getEntities();
        if (objectEntities != null) {
            List<Entity> entities = new ArrayList<>(objectEntities.size());
            for (Entity objectEntity: objectEntities) {
                Entity entity = (Entity) objectEntity.clone();
                double[] pos = entity.getPos();
                double[] vel = entity.getVel();
                if (mirrorYAxis) {
                    pos[2] = dimensions.y - pos[2];
                    vel[2] = -vel[2];
                } else {
                    pos[0] = dimensions.x - pos[0];
                    vel[0] = -vel[0];
                }
                entity.setPos(pos);
                entity.setVel(vel);
                float[] rot = entity.getRot();
                rot[0] = MathUtils.mod(rot[0] + 180.0f, 360.0f);
                entity.setRot(rot);
                entities.add(entity);
            }
            return entities;
        } else {
            return null;
        }
    }

    @Override
    public List<TileEntity> getTileEntities() {
        List<TileEntity> objectTileEntities = object.getTileEntities();
        if (objectTileEntities != null) {
            List<TileEntity> tileEntities = new ArrayList<>(objectTileEntities.size());
            for (TileEntity objectTileEntity: objectTileEntities) {
                TileEntity tileEntity = (TileEntity) objectTileEntity.clone();
                if (mirrorYAxis) {
                    tileEntity.setZ(dimensions.y - objectTileEntity.getZ() - 1);
                } else {
                    tileEntity.setX(dimensions.x - objectTileEntity.getX() - 1);
                }
                tileEntities.add(tileEntity);
            }
            return tileEntities;
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return object.getName();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    private final WPObject object;
    private final boolean mirrorYAxis;
    private final Point3i dimensions;
    private final Map<String, Serializable> attributes;

    private static final long serialVersionUID = 1L;
}