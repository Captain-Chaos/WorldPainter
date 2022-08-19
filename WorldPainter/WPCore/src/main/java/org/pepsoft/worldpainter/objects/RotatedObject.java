/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Platform;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class RotatedObject extends AbstractObject {
    /**
     * A rotated version of another object.
     * 
     * @param object The object to rotate;
     * @param steps The number of 90 degree steps to rotate clockwise. Must be
     *     between 0 and 3 (inclusive).
     */
    public RotatedObject(WPObject object, int steps, Platform platform) {
        if ((steps < 0) || (steps > 3)) {
            throw new IllegalArgumentException(Integer.toString(steps));
        }
        this.object = object;
        this.steps = steps;
        this.platform = platform;
        Point3i origDim = object.getDimensions();
        Map<String, Serializable> attributes = (object.getAttributes() != null) ? new HashMap<>(object.getAttributes()) : new HashMap<>();
        Point3i offset = object.getOffset();
        switch (steps) {
            case 0:
                dimensions = origDim;
                break;
            case 1:
                dimensions = new Point3i(origDim.y, origDim.x, origDim.z);
                offset = new Point3i(-(origDim.y - (-offset.y) - 1), offset.x, offset.z);
                break;
            case 2:
                dimensions = origDim;
                offset = new Point3i(-(origDim.x - (-offset.x) - 1), -(origDim.y - (-offset.y) - 1), offset.z);
                break;
            case 3:
                dimensions = new Point3i(origDim.y, origDim.x, origDim.z);
                offset = new Point3i(offset.y, -(origDim.x - (-offset.x) - 1), offset.z);
                break;
            default:
                throw new InternalError();
        }
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
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        switch (steps) {
            case 0:
                return object.getMaterial(x, y, z);
            case 1:
                return object.getMaterial(y, dimensions.x - x - 1, z).rotate(1, platform);
            case 2:
                return object.getMaterial(dimensions.x - x - 1, dimensions.y - y - 1, z).rotate(2, platform);
            case 3:
                return object.getMaterial(dimensions.y - y - 1, x, z).rotate(3, platform);
            default:
                throw new InternalError();
        }
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        switch (steps) {
            case 0:
                return object.getMask(x, y, z);
            case 1:
                return object.getMask(y, dimensions.x - x - 1, z);
            case 2:
                return object.getMask(dimensions.x - x - 1, dimensions.y - y - 1, z);
            case 3:
                return object.getMask(dimensions.y - y - 1, x, z);
            default:
                throw new InternalError();
        }
    }
    
    @Override
    public List<Entity> getEntities() {
        if (steps == 0) {
            return object.getEntities();
        }
        List<Entity> objectEntities = object.getEntities();
        if (objectEntities != null) {
            List<Entity> entities = new ArrayList<>(objectEntities.size());
            for (Entity origEntity: objectEntities) {
                Entity rotEntity = origEntity.rotate(steps);
                double[] origRelPos = origEntity.getRelPos();
                double[] rotRelPos = rotEntity.getRelPos();
                switch (steps) {
                    case 1:
                        rotRelPos[0] = dimensions.x - origRelPos[2];
                        rotRelPos[2] = origRelPos[0];
                        break;
                    case 2:
                        rotRelPos[0] = dimensions.x - origRelPos[0];
                        rotRelPos[2] = dimensions.y - origRelPos[2];
                        break;
                    case 3:
                        rotRelPos[0] = origRelPos[2];
                        rotRelPos[2] = dimensions.y - origRelPos[0];
                        break;
                    default:
                        throw new InternalError();
                }
                rotEntity.setRelPos(rotRelPos);
                entities.add(rotEntity);
            }
            return entities;
        } else {
            return null;
        }
    }

    @Override
    public List<TileEntity> getTileEntities() {
        if (steps == 0) {
            return object.getTileEntities();
        } else {
            List<TileEntity> objectTileEntities = object.getTileEntities();
            if (objectTileEntities != null) {
                List<TileEntity> tileEntities = new ArrayList<>(objectTileEntities.size());
                for (TileEntity objectTileEntity: objectTileEntities) {
                    TileEntity tileEntity = (TileEntity) objectTileEntity.clone();
                    switch (steps) {
                        case 1:
                            tileEntity.setX(dimensions.x - objectTileEntity.getZ() - 1);
                            tileEntity.setZ(objectTileEntity.getX());
                            break;
                        case 2:
                            tileEntity.setX(dimensions.x - objectTileEntity.getX() - 1);
                            tileEntity.setZ(dimensions.y - objectTileEntity.getZ() - 1);
                            break;
                        case 3:
                            tileEntity.setX(objectTileEntity.getZ());
                            tileEntity.setZ(dimensions.y - objectTileEntity.getX() - 1);
                            break;
                        default:
                            throw new InternalError();
                    }
                    tileEntities.add(tileEntity);
                }
                return tileEntities;
            } else {
                return null;
            }
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
    private final int steps;
    private final Platform platform;
    private final Point3i dimensions;
    private final Map<String, Serializable> attributes;

    private static final long serialVersionUID = 1L;
}