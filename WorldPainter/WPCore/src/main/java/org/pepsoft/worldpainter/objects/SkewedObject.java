/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;

/**
 *
 * @author pepijn
 */
public class SkewedObject extends AbstractObject {
    public SkewedObject(WPObject object, float xSkew, float ySkew) {
        this.object = object;
        this.xSkew = xSkew;
        this.ySkew = ySkew;
        objectDimensions = object.getDimensions();
        dx = (int) (objectDimensions.z * xSkew);
        dy = (int) (objectDimensions.z * ySkew);
//        System.out.println("xSkew: " + xSkew + ", ySkew: " + ySkew + ", dx: " + dx + ", dy: " + dy);
        dimensions = new Point3i(objectDimensions.x + dx, objectDimensions.y + dy, objectDimensions.z);
//        Point3i orig = object.getOrigin();
//        int originX = orig.x + (int) (orig.z * xSkew);
//        if (dx < 0) {
//            originX -= dx;
//        }
//        int originY = orig.y + (int) (orig.z * ySkew);
//        if (dy < 0) {
//            originY -= dy;
//        }
//        origin = new Point3i(originX, originY, orig.z);
        attributes = (object.getAttributes() != null) ? new HashMap<>(object.getAttributes()) : new HashMap<>();
        // TODO: skew offset
    }

    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        Point3i coords = translate(x, y, z);
        return object.getMaterial(coords.x, coords.y, coords.z);
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        Point3i coords = translate(x, y, z);
        if ((coords.x < 0) || (coords.x >= objectDimensions.x) || (coords.y < 0) || (coords.y >= objectDimensions.y)) {
            // Outside original object's bounding box
            return false;
        } else {
            return object.getMask(coords.x, coords.y, coords.z);
        }
    }

    @Override
    public List<Entity> getEntities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TileEntity> getTileEntities() {
        throw new UnsupportedOperationException();
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

    private Point3i translate(int x, int y, int z) {
        return new Point3i(
            x - (int) (z * xSkew) + ((dx < 0) ? dx : 0),
            y - (int) (z * ySkew) + ((dy < 0) ? dy : 0),
            z);
    }

    private final WPObject object;
    private final int dx, dy;
    private final Point3i objectDimensions, dimensions;
    private final float xSkew, ySkew;
    private final Map<String, Serializable> attributes;

    private static final long serialVersionUID = 1L;
}