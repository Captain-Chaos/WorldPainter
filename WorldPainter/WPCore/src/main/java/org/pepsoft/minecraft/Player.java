/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.Collections;
import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class Player extends Mob {
    public Player() {
        super(ID_PLAYER);
        setList(TAG_INVENTORY, CompoundTag.class, Collections.<Tag>emptyList());
        setInt(TAG_SCORE, 0);
        setInt(TAG_DIMENSION, 0);
    }

    public Player(CompoundTag tag) {
        super(tag);
    }

    private static final long serialVersionUID = 1L;
}