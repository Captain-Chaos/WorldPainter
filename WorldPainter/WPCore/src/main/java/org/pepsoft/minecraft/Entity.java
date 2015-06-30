/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.HashMap;
import org.jnbt.CompoundTag;
import org.jnbt.StringTag;

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
        setString(TAG_ID, id);
        setShort(TAG_FIRE, (short) -20);
        setShort(TAG_AIR, (short) 300);
        setBoolean(TAG_ON_GROUND, true);
        setPos(new double[] {0, 0, 0});
        setRot(new float[] {0, 0});
        setVel(new double[] {0, 0, 0});
    }

    protected Entity(CompoundTag tag) {
        super(tag);
    }

    public final String getId() {
        return getString(TAG_ID);
    }

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

    public static Entity fromNBT(CompoundTag entityTag) {
        String id = ((StringTag) entityTag.getTag(TAG_ID)).getValue();
        switch (id) {
            case ID_PLAYER:
                return new Player(entityTag);
            case ID_VILLAGER:
                return new Villager(entityTag);
            case ID_PAINTING:
                return new Painting(entityTag);
            default:
                return new Entity(entityTag);
        }
    }

    private static final long serialVersionUID = 1L;
}