/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.pepsoft.util.MathUtils;

import java.util.HashMap;
import java.util.UUID;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.ArrayUtils.indexOf;

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
        if (containsTag(TAG_TILE_X)) {
            setInt(TAG_TILE_X, (int) Math.floor(pos[0]));
            setInt(TAG_TILE_Y, (int) Math.floor(pos[1]));
            setInt(TAG_TILE_Z, (int) Math.floor(pos[2]));
        }
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

    /**
     * Return a clone of this entity that has been rotated clockwise by {@code steps} times 90 degrees.
     */
    public Entity rotate(int steps) {
        final Entity rotEntity = clone();
        if (steps != 0) {
            final float[] rot = rotEntity.getRot();
            rot[0] = MathUtils.mod(rot[0] + steps * 90.0f, 360.0f);
            rotEntity.setRot(rot);
            if (containsTag(TAG_FACING_)) {
                int facing = getByte(TAG_FACING_);
                if ((facing >= 0) && (facing <= 3)) {
                    facing = (facing + steps) & 0x3;
                    rotEntity.setByte(TAG_FACING_, (byte) facing);
                }
            }
            if (containsTag(TAG_FACING)) {
                int facing = getByte(TAG_FACING);
                if ((facing >= 2) && (facing <= 5)) {
                    facing = FACING_VALUES[(indexOf(FACING_VALUES, facing) + steps) & 0x3];
                    rotEntity.setByte(TAG_FACING, (byte) facing);
                }
            }
            // TODO are there other properties to adjust?
            // TODO adjust velocity
        }
        return rotEntity;
    }

    /**
     * Return a clone of this entity that has been mirrored in the indicated axis.
     */
    public Entity mirror(boolean mirrorYAxis) {
        final Entity mirEntity = clone();
        final double[] vel = mirEntity.getVel();
        if (mirrorYAxis) {
            vel[2] = -vel[2];
        } else {
            vel[0] = -vel[0];
        }
        mirEntity.setVel(vel);
        final float[] rot = mirEntity.getRot();
        if (mirrorYAxis) {
            rot[0] = MathUtils.mod(180.0f - rot[0], 360.0f);
        } else {
            rot[0] = MathUtils.mod(-rot[0], 360.0f);
        }
        mirEntity.setRot(rot);
        if (containsTag(TAG_FACING_)) {
            int facing = getByte(TAG_FACING_);
            if (mirrorYAxis) {
                switch (facing) {
                    case 0: facing = 2; break;
                    case 2: facing = 0; break;
                }
            } else {
                switch (facing) {
                    case 1: facing = 3; break;
                    case 3: facing = 1; break;
                }
            }
            mirEntity.setByte(TAG_FACING_, (byte) facing);
        }
        if (containsTag(TAG_FACING)) {
            int facing = getByte(TAG_FACING);
            if (mirrorYAxis) {
                switch (facing) {
                    case 2: facing = 3; break;
                    case 3: facing = 2; break;
                }
            } else {
                switch (facing) {
                    case 4: facing = 5; break;
                    case 5: facing = 4; break;
                }
            }
            mirEntity.setByte(TAG_FACING, (byte) facing);
        }
        // TODO are there other properties to adjust?
        return mirEntity;
    }

    @Override
    public Entity clone() {
        final Entity clone = (Entity) super.clone();
        if (relPos != null) {
            clone.relPos = relPos.clone();
        }
        return clone;
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

    private static final int[] FACING_VALUES = { 3, 4, 2, 5 };
    private static final long serialVersionUID = 1L;
}