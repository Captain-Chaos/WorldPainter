package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo;

public class GetBiomeIdOp extends AbstractOperation<Integer> {
    public GetBiomeIdOp(ScriptingContext context) {
        super(context);
    }

    public GetBiomeIdOp fromWorld(World2 world) {
        this.world = world;
        return this;
    }

    public GetBiomeIdOp withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Integer go() throws ScriptException {
        goCalled();

        if (name == null) {
            throw new ScriptException("name not set");
        }
        // First check vanilla biomes
        final BiomeScheme biomeScheme = StaticBiomeInfo.INSTANCE;
        for (int i = 0; i < biomeScheme.getBiomeCount(); i++) {
            if (biomeScheme.isBiomePresent(i) && (biomeScheme.getBiomeName(i).equalsIgnoreCase(name) || biomeScheme.getStringId(i).equalsIgnoreCase(name))) {
                return i;
            }
        }
        // It's not a vanilla biome (that we know of), so check all custom biomes (if a world was provided)
        if (world != null) {
            for (Dimension dimension: world.getDimensions()) {
                if (dimension.getCustomBiomes() == null) {
                    continue;
                }
                for (CustomBiome customBiome: dimension.getCustomBiomes()) {
                    if (customBiome.getName().equalsIgnoreCase(name)) {
                        return customBiome.getId();
                    }
                }
            }
        }

        throw new ScriptException("No biome found with name or ID \"" + name + "\"");
    }

    private World2 world;
    private String name;
}
