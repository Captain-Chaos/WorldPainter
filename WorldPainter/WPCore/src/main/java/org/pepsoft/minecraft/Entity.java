/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;

import java.util.HashMap;
import java.util.UUID;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class Entity extends AbstractNBTItem {
    public Entity(String id) {
        super(new CompoundTag("", new HashMap<>()));
        if (id == null) {
            throw new NullPointerException();
        }
        setString(TAG_ID_, id);
        setShort(TAG_FIRE, (short) -20);
        setShort(TAG_AIR, (short) 300);
        setBoolean(TAG_ON_GROUND, true);
        setPos(new double[] {0, 0, 0});
        setRot(new float[] {0, 0});
        setVel(new double[] {0, 0, 0});
    }

    protected Entity(CompoundTag tag, double[] relPos) {
        super(tag);
        if ((relPos != null) && (relPos.length != 3)) {
            throw new IllegalArgumentException("relPos.length != 3");
        }
        this.relPos = relPos;
    }

    public final String getId() {
        return getString(TAG_ID_);
    }

    /**
     * Get the position of this entity relative to the origin of the custom object in which it is contained.
     */
    public final double[] getRelPos() {
        return (relPos != null) ? relPos : new double[] {0, 0, 0};
    }

    public final void setRelPos(double[] relPos) {
        if (relPos.length != 3) {
            throw new IllegalArgumentException();
        }
        this.relPos = relPos;
    }

    /**
     * Get the absolute position of this entity in the world.
     */
    public final double[] getPos() {
        double[] list = getDoubleList(TAG_POS);
        return (list != null) ? list : new double[3];
    }

    public final void setPos(double[] pos) {
        if (pos.length != 3) {
            throw new IllegalArgumentException();
        }
        setDoubleList(TAG_POS, pos);
    }

    public final float[] getRot() {
        float[] list = getFloatList(TAG_ROTATION);
        return (list != null) ? list : new float[2];
    }

    public final void setRot(float[] rot) {
        if (rot.length != 2) {
            throw new IllegalArgumentException();
        }
        setFloatList(TAG_ROTATION, rot);
    }

    public final double[] getVel() {
        double[] list = getDoubleList(TAG_MOTION);
        return (list != null) ? list : new double[3];
    }

    public final void setVel(double[] vel) {
        if (vel.length != 3) {
            throw new IllegalArgumentException();
        }
        setDoubleList(TAG_MOTION, vel);
    }

    public final UUID getUUID() {
        String uuidStr = getString(TAG_UUID);
        if (uuidStr != null) {
            return UUID.fromString(uuidStr);
        } else {
            return new UUID(getLong(TAG_UUID_MOST), getLong(TAG_UUID_LEAST));
        }
    }

    public final void setUUID(UUID uuid) {
        setLong(TAG_UUID_MOST, uuid.getMostSignificantBits());
        setLong(TAG_UUID_LEAST, uuid.getLeastSignificantBits());
        setString(TAG_UUID, null);
    }

    public static Entity fromNBT(CompoundTag entityTag) {
        return fromNBT(entityTag, null);
    }

    public static Entity fromNBT(CompoundTag entityTag, double[] relPos) {
        String id = ((StringTag) entityTag.getTag(TAG_ID_)).getValue();
        switch (id) { // TODO add MC 1.15 support
            case LEGACY_ID_PLAYER:
                return new Player(entityTag, relPos);
            case LEGACY_ID_VILLAGER:
                return new Villager(entityTag, relPos);
            case LEGACY_ID_PAINTING:
                return new Painting(entityTag, relPos);
            default:
                return new Entity(entityTag, relPos);
        }
    }

    private double[] relPos;

    private static final long serialVersionUID = 1L;
}