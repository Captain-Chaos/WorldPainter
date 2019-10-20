/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import static org.pepsoft.minecraft.Constants.LEGACY_ID_VILLAGER;
import static org.pepsoft.minecraft.Constants.TAG_PROFESSION;

/**
 *
 * @author pepijn
 */
public class Villager extends Mob {
    public Villager() {
        super(LEGACY_ID_VILLAGER); // TODO add MC 1.14 support
    }
    
    public Villager(CompoundTag tag) {
        super(tag);
    }
    
    public Villager(int profession) {
        super(LEGACY_ID_VILLAGER); // TODO add MC 1.14 support
        setProfession(profession);
    }
    
    public final int getProfession() {
        return getInt(TAG_PROFESSION);
    }
    
    public final void setProfession(int profession) {
        setInt(TAG_PROFESSION, profession);
    }

    private static final long serialVersionUID = 1L;
}