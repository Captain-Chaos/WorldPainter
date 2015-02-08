/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public class ThemeManager {
    private ThemeManager() {
        // Enforce singleton
    }
    
    public Theme[] getThemes() {
        return themes.clone();
    }
    
    public static ThemeManager getInstance() {
        return INSTANCE;
    }
    
    private final Theme[] themes = {
        //        Floors,           Beams,        Walls,            Roof,               Windows     Interior walls
        new Theme(WOODEN_PLANK_OAK, WOOD_OAK,     WOODEN_PLANK_OAK, WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_PINE,    SAND,             BRICK_STAIRS,       GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_PINE,    SANDSTONE,        BRICK_STAIRS,       GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_PINE,    SAND,             WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_PINE,    SANDSTONE,        WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_OAK,     BRICKS,           WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_OAK,     COBBLESTONE,      WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, STONE_BRICKS, STONE,            STONE_BRICK_STAIRS, GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, STONE,        COBBLESTONE,      BRICK_STAIRS,       GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, WOOD_OAK,     COBBLESTONE,      STONE_BRICK_STAIRS, GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, BRICKS,       BRICKS,           STONE_BRICK_STAIRS, GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, SANDSTONE,    SAND,             BRICK_STAIRS,       GLASS_PANE, WOODEN_PLANK_OAK),
        new Theme(WOODEN_PLANK_OAK, SANDSTONE,    SAND,             WOODEN_STAIRS,      GLASS_PANE, WOODEN_PLANK_OAK),
    };
    
    private static final ThemeManager INSTANCE = new ThemeManager();
}