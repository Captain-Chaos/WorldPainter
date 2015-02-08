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
public class Villager extends Mob {
    public Villager() {
        super(Constants.ID_VILLAGER);
    }
    
    public Villager(CompoundTag tag) {
        super(tag);
    }
    
    public Villager(int profession) {
        super(Constants.ID_VILLAGER);
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