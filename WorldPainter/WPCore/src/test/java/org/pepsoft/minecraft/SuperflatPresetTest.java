package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jnbt.CompoundTag;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.pepsoft.minecraft.SuperflatPreset.Layer;
import org.pepsoft.minecraft.SuperflatPreset.Structure;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.SuperflatPreset.Structure.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;

public class SuperflatPresetTest {
    @Test
    public void testSuperflatPreset() {
        SuperflatPreset superflatPreset = new SuperflatPreset(BIOME_PLAINS,
                ImmutableList.of(new Layer(MC_BEDROCK, 1), new Layer(MC_STONE, 10), new Layer(MC_DIRT, 4), new Layer(MC_GRASS_BLOCK, 1)),
                ImmutableMap.of(BIOME_1, emptyMap(), LAKE, emptyMap(), VILLAGE, emptyMap()));
        String mc112Representation = superflatPreset.toMinecraft1_12_2();
        System.out.println(mc112Representation);
        SuperflatPreset superflatPreset2 = SuperflatPreset.fromMinecraft1_12_2(mc112Representation);
        CompoundTag mc113Representation = superflatPreset2.toMinecraft1_15_2();
        System.out.println(mc113Representation);
        SuperflatPreset superflatPreset3 = SuperflatPreset.fromMinecraft1_15_2(mc113Representation);
        Assert.assertEquals(superflatPreset, superflatPreset2);
        Assert.assertEquals(superflatPreset2, superflatPreset3);
    }

    @Test
    public void testFromMinecraft1_12_2() {
        // TODO grass should actually map to grass_block, which is part of the larger problem of Minecraft 1.12 using different block names, which we should address somehow, someday
        SuperflatPreset superflatPreset = SuperflatPreset.fromMinecraft1_12_2("3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village,biome_1,decoration,stronghold,mineshaft,lake,lava_lake,dungeon");
        SuperflatPreset expected = new SuperflatPreset(BIOME_PLAINS,
                ImmutableList.of(new Layer(MC_BEDROCK, 1), new Layer(MC_STONE, 59), new Layer(MC_DIRT, 3), new Layer(MC_GRASS, 1)),
                ImmutableMap.<Structure, Map<String, String>>builder()
                        .put(VILLAGE, emptyMap())
                        .put(BIOME_1, emptyMap())
                        .put(DECORATION, emptyMap())
                        .put(STRONGHOLD, emptyMap())
                        .put(MINESHAFT, emptyMap())
                        .put(LAKE, emptyMap())
                        .put(LAVA_LAKE, emptyMap())
                        .put(DUNGEON, emptyMap())
                        .build());
        Assert.assertEquals(expected, superflatPreset);
    }

    @Ignore
    @Test
    public void testFromMinecraft1_15_2() {
        // TODO
    }
}