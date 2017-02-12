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
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.panels.DefaultFilter.LayerValue;

import static org.pepsoft.worldpainter.panels.DefaultFilter.Condition.*;

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
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp onlyOnLayer(Layer layer) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = layer;
        exceptOnLastSet = false;
        return this;
    }

    public CreateFilterOp withValue(int value) throws ScriptException {
        if (exceptOnLastSet) {
            if (! (exceptOn instanceof Layer)) {
                throw new ScriptException("No \"except on\" layer selected for \"with value\", or more than one \"with value\" specified");
            }
            if ((((Layer) exceptOn).getDataSize() == Layer.DataSize.BIT)
                    || (((Layer) exceptOn).getDataSize() == Layer.DataSize.BIT_PER_CHUNK)
                    || (((Layer) exceptOn).getDataSize() == Layer.DataSize.NONE)) {
                throw new ScriptException("A value may only be specified for continuous or discrete layer types");
            }
            exceptOn = new LayerValue((Layer) exceptOn, value);
        } else {
            if (! (onlyOn instanceof Layer)) {
                throw new ScriptException("No \"only on\" layer selected for \"with value\", or more than one \"with value\" specified");
            }
            if ((((Layer) onlyOn).getDataSize() == Layer.DataSize.BIT)
                    || (((Layer) onlyOn).getDataSize() == Layer.DataSize.BIT_PER_CHUNK)
                    || (((Layer) onlyOn).getDataSize() == Layer.DataSize.NONE)) {
                throw new ScriptException("A value may only be specified for continuous or discrete layer types");
            }
            onlyOn = new LayerValue((Layer) onlyOn, value);
        }
        return this;
    }
    
    public CreateFilterOp orHigher() throws ScriptException {
        if (exceptOnLastSet) {
            if (exceptOn instanceof Layer) {
                throw new ScriptException("No \"except on\" layer value specified for \"or higher\"");
            } else if (((LayerValue) exceptOn).condition != null) {
                throw new ScriptException("Only one of \"or lower\" and \"or higher\" may be specified for \"except on\" value");
            }
            exceptOn = new LayerValue(((LayerValue) exceptOn).layer, ((LayerValue) exceptOn).value, HIGHER_THAN_OR_EQUAL);
        } else {
            if (onlyOn == null) {
                throw new ScriptException("No \"only on\" layer specified for \"or higher\"");
            } else if (onlyOn instanceof Layer) {
                throw new ScriptException("No \"only on\" layer value specified for \"or higher\"");
            } else if (((LayerValue) onlyOn).condition != null) {
                throw new ScriptException("Only one of \"or lower\" and \"or higher\" may be specified for \"only on\" value");
            }
            onlyOn = new LayerValue(((LayerValue) onlyOn).layer, ((LayerValue) onlyOn).value, HIGHER_THAN_OR_EQUAL);
        }
        return this;
    }

    public CreateFilterOp orLower() throws ScriptException {
        if (exceptOnLastSet) {
            if (exceptOn instanceof Layer) {
                throw new ScriptException("No \"except on\" layer value specified for \"or lower\"");
            } else if (((LayerValue) exceptOn).condition != null) {
                throw new ScriptException("Only one of \"or lower\" and \"or higher\" may be specified for \"except on\" value");
            }
            exceptOn = new LayerValue(((LayerValue) exceptOn).layer, ((LayerValue) exceptOn).value, LOWER_THAN_OR_EQUAL);
        } else {
            if (onlyOn == null) {
                throw new ScriptException("No \"only on\" layer specified for \"or lower\"");
            } else if (onlyOn instanceof Layer) {
                throw new ScriptException("No \"only on\" layer value specified for \"or lower\"");
            } else if (((LayerValue) onlyOn).condition != null) {
                throw new ScriptException("Only one of \"or lower\" and \"or higher\" may be specified for \"only on\" value");
            }
            onlyOn = new LayerValue(((LayerValue) onlyOn).layer, ((LayerValue) onlyOn).value, LOWER_THAN_OR_EQUAL);
        }
        return this;
    }

    public CreateFilterOp onlyOnBiome(int biomeIndex) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = biomeIndex;
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp onlyOnAutoBiome(int biomeIndex) throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = -biomeIndex;
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp onlyOnAutoBiomes() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = DefaultFilter.AUTO_BIOMES;
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp onlyOnWater() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = DefaultFilter.WATER;
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp onlyOnLand() throws ScriptException {
        if (onlyOn != null) {
            throw new ScriptException("Only one \"only on\" or condition may be specified");
        }
        onlyOn = DefaultFilter.LAND;
        exceptOnLastSet = false;
        return this;
    }
    
    public CreateFilterOp exceptOnTerrain(int terrainIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = Terrain.VALUES[terrainIndex];
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnLayer(Layer layer) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = layer;
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnBiome(int biomeIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = biomeIndex;
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnAutoBiome(int biomeIndex) throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = -biomeIndex;
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnAutoBiomes() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = DefaultFilter.AUTO_BIOMES;
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnWater() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = DefaultFilter.WATER;
        exceptOnLastSet = true;
        return this;
    }
    
    public CreateFilterOp exceptOnLand() throws ScriptException {
        if (exceptOn != null) {
            throw new ScriptException("Only one or \"except on\" condition may be specified");
        }
        exceptOn = DefaultFilter.LAND;
        exceptOnLastSet = true;
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

    public CreateFilterOp inSelection() throws ScriptException {
        if (outsideSelection) {
            throw new ScriptException("inSelection and outsideSelection may not both be specified");
        }
        inSelection = true;
        return this;
    }

    public CreateFilterOp outsideSelection() throws ScriptException {
        if (inSelection) {
            throw new ScriptException("inSelection and outsideSelection may not both be specified");
        }
        outsideSelection = true;
        return this;
    }

    @Override
    public Filter go() throws ScriptException {
        goCalled();

        return new DefaultFilter(null, inSelection, outsideSelection, aboveLevel, belowLevel, feather, onlyOn, exceptOn, degrees, slopeIsAbove);
    }
    
    private int aboveLevel = -1, belowLevel = -1, degrees = -1;
    private boolean feather, slopeIsAbove, exceptOnLastSet, inSelection, outsideSelection;
    private Object onlyOn, exceptOn;
}