/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.FilterImpl;

/**
 *
 * @author pepijn
 */
public class CreateFilterOp extends AbstractOperation<Filter> {
    public CreateFilterOp(ScriptingContext context) {
        super(context);
    }

    public CreateFilterOp aboveLevel(int aboveLevel) {
        this.aboveLevel = aboveLevel;
        return this;
    }
    
    public CreateFilterOp belowLevel(int belowLevel) {
        this.belowLevel = belowLevel;
        return this;
    }
    
    public CreateFilterOp feather() {
        feather = true;
        return this;
    }
    
    public CreateFilterOp onlyOnTerrain(int terrainIndex) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = Terrain.VALUES[terrainIndex];
        return this;
    }
    
    public CreateFilterOp onlyOnLayer(Layer layer) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = layer;
        return this;
    }
    
    public CreateFilterOp onlyOnBiome(int biomeIndex) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = biomeIndex;
        return this;
    }
    
    public CreateFilterOp onlyOnAutoBiome(int biomeIndex) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = -biomeIndex;
        return this;
    }
    
    public CreateFilterOp onlyOnAutoBiomes() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = FilterImpl.AUTO_BIOMES;
        return this;
    }
    
    public CreateFilterOp onlyOnWater() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = FilterImpl.WATER;
        return this;
    }
    
    public CreateFilterOp onlyOnLand() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = FilterImpl.LAND;
        return this;
    }
    
    public CreateFilterOp exceptOnTerrain(int terrainIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = Terrain.VALUES[terrainIndex];
        return this;
    }
    
    public CreateFilterOp exceptOnLayer(Layer layer) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = layer;
        return this;
    }
    
    public CreateFilterOp exceptOnBiome(int biomeIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = biomeIndex;
        return this;
    }
    
    public CreateFilterOp exceptOnAutoBiome(int biomeIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = -biomeIndex;
        return this;
    }
    
    public CreateFilterOp exceptOnAutoBiomes() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = FilterImpl.AUTO_BIOMES;
        return this;
    }
    
    public CreateFilterOp exceptOnWater() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = FilterImpl.WATER;
        return this;
    }
    
    public CreateFilterOp exceptOnLand() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = FilterImpl.LAND;
        return this;
    }
    
    public CreateFilterOp aboveDegrees(int aboveDegrees) throws ScriptException {
        if ((aboveDegrees < 0) || (aboveDegrees > 90)) {
            throw new ScriptException("Degrees must be between 0 and 90 (inclusive)");
        }
        if (degrees != -1) {
            throw new ScriptException("aboveDegrees and belowDegrees may not both be specified");
        }
        degrees = aboveDegrees;
        slopeIsAbove = true;
        return this;
    }
    
    public CreateFilterOp belowDegrees(int belowDegrees) throws ScriptException {
        if ((belowDegrees < 0) || (belowDegrees > 90)) {
            throw new ScriptException("Degrees must be between 0 and 90 (inclusive)");
        }
        if (degrees != -1) {
            throw new ScriptException("aboveDegrees and belowDegrees may not both be specified");
        }
        degrees = belowDegrees;
        return this;
    }
    
    @Override
    public Filter go() throws ScriptException {
        goCalled();

        return new FilterImpl(null, aboveLevel, belowLevel, feather, onlyOn, exceptOn, degrees, slopeIsAbove);
    }
    
    private int aboveLevel = -1, belowLevel = -1, degrees = -1;
    private boolean feather, slopeIsAbove;
    private Object onlyOn, exceptOn;
}