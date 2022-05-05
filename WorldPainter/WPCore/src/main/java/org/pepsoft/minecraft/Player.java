/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import java.util.Collections;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class Player extends Mob {
    public Player() {
        super(LEGACY_ID_PLAYER); // TODO add MC 1.15 support
        setList(TAG_INVENTORY, CompoundTag.class, Collections.emptyList());
        setInt(TAG_SCORE, 0);
        setInt(TAG_DIMENSION, 0);
    }

    public Player(CompoundTag tag, double[] relPos) {
        super(tag, relPos);
    }

    private static final long serialVersionUID = 1L;
}