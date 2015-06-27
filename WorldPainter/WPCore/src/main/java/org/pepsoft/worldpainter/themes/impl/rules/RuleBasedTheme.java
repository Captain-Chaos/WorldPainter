/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.themes.Theme;

/**
 *
 * @author SchmitzP
 */
public class RuleBasedTheme implements Theme, Cloneable {
    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public void apply(Tile tile, int x, int y) {
        ruleSet.apply(new Context(tile, x, y, seed, maxHeight, waterHeight));
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void setMaxHeight(int maxHeight, HeightTransform transform) {
        this.maxHeight = maxHeight;
        waterHeight = transform.transformHeight(waterHeight);
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public int getWaterHeight() {
        return waterHeight;
    }

    @Override
    public void setWaterHeight(int waterHeight) {
        this.waterHeight = waterHeight;
    }

    @Override
    public RuleBasedTheme clone() {
        try {
            return (RuleBasedTheme) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private RuleSet ruleSet;
    private long seed;
    private int waterHeight = 62, maxHeight = Constants.DEFAULT_MAX_HEIGHT_2;

    private static final long serialVersionUID = 4743522528008344523L;
}