/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class Mob extends Entity {
    public Mob(String id) {
        super(id);
        setHealth((short) 20);
        setAttackTime((short) 0);
        setDeathTime((short) 0);
        setHurtTime((short) 0);
    }

    public Mob(CompoundTag tag) {
        super(tag);
    }

    public final short getAttackTime() {
        return getShort(TAG_ATTACK_TIME);
    }

    public final void setAttackTime(short attackTime) {
        setShort(TAG_ATTACK_TIME, attackTime);
    }

    public final short getDeathTime() {
        return getShort(TAG_DEATH_TIME);
    }

    public final void setDeathTime(short deathTime) {
        setShort(TAG_DEATH_TIME, deathTime);
    }

    public final short getHealth() {
        return getShort(TAG_HEALTH);
    }

    public final void setHealth(short health) {
        setShort(TAG_HEALTH, health);
    }

    public final short getHurtTime() {
        return getShort(TAG_HURT_TIME);
    }

    public final void setHurtTime(short hurtTime) {
        setShort(TAG_HURT_TIME, hurtTime);
    }
    
    private static final long serialVersionUID = 1L;
}