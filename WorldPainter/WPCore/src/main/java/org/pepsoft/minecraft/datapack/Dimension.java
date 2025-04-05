package org.pepsoft.minecraft.datapack;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Value;
import org.pepsoft.worldpainter.Platform;

import java.util.Map;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_END;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_NETHER;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_17;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_20_5;

/**
 * Settings from official Caves and Cliffs Preview for Minecraft 1.17 from Mojang.
 */
@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Dimension extends Descriptor {
    @Builder.Default int logicalHeight = 384;
    @Builder.Default String infiniburn = "#minecraft:infiniburn_overworld";
    float ambientLight;
    boolean respawnAnchorWorks;
    @Builder.Default boolean hasRaids = true;
    @Builder.Default int minY = -64;
    @Builder.Default int height = 384;
    @Builder.Default boolean natural = true;
    @Builder.Default float coordinateScale = 1.0f;
    boolean piglinSafe;
    @Builder.Default boolean bedWorks = true;
    @Builder.Default boolean hasSkylight = true;
    boolean hasCeiling;
    boolean ultrawarm;
    Integer fixedTime;
    @Builder.Default String effects = "#minecraft:overworld";
    int monsterSpawnBlockLightLimit;
    Object monsterSpawnLightLevel;

    private static final IntProvider MONSTER_SPAWN_LIGHT_LEVEL_PRE_1_20_4 = IntProvider.builder().type("minecraft:uniform").value(ImmutableMap.of("min_inclusive", 0, "max_inclusive", 7)).build();
    private static final Map<String, Object> MONSTER_SPAWN_LIGHT_LEVEL_POST_1_20_5 = ImmutableMap.of("type", "minecraft:uniform", "min_inclusive", 0, "max_inclusive", 7);

    public static Dimension createDefault(Platform platform, int dim, int minHeight, int maxHeight) {
        switch (dim) {
            case DIM_NORMAL:
                return builder()
                        .infiniburn((platform == JAVA_ANVIL_1_17) ? "minecraft:infiniburn_overworld" : "#minecraft:infiniburn_overworld")
                        .logicalHeight(maxHeight - minHeight)
                        .minY(minHeight)
                        .height(maxHeight - minHeight)
                        .effects((platform == JAVA_ANVIL_1_17) ? "minecraft:overworld" : "#minecraft:overworld")
                        .monsterSpawnLightLevel((platform == JAVA_ANVIL_1_20_5) ? MONSTER_SPAWN_LIGHT_LEVEL_POST_1_20_5 : MONSTER_SPAWN_LIGHT_LEVEL_PRE_1_20_4)
                        .build();
            case DIM_NETHER:
                return builder()
                        .infiniburn((platform == JAVA_ANVIL_1_17) ? "minecraft:infiniburn_nether" : "#minecraft:infiniburn_nether")
                        .logicalHeight(128)
                        .minY(0)
                        .height(DEFAULT_MAX_HEIGHT_NETHER)
                        .ultrawarm(true)
                        .natural(false)
                        .coordinateScale(8.0f)
                        .piglinSafe(true)
                        .bedWorks(false)
                        .hasRaids(false)
                        .hasSkylight(false)
                        .hasCeiling(true)
                        .fixedTime(18000)
                        .ambientLight(0.1f)
                        .effects((platform == JAVA_ANVIL_1_17) ? "minecraft:the_nether" : "#minecraft:the_nether")
                        .monsterSpawnLightLevel((platform == JAVA_ANVIL_1_20_5) ? MONSTER_SPAWN_LIGHT_LEVEL_POST_1_20_5 : MONSTER_SPAWN_LIGHT_LEVEL_PRE_1_20_4)
                        .build();
            case DIM_END:
                return builder()
                        .infiniburn((platform == JAVA_ANVIL_1_17) ? "minecraft:infiniburn_end" : "#minecraft:infiniburn_end")
                        .logicalHeight(DEFAULT_MAX_HEIGHT_END)
                        .minY(0)
                        .height(DEFAULT_MAX_HEIGHT_END)
                        .natural(false)
                        .bedWorks(false)
                        .hasSkylight(false)
                        .fixedTime(6000)
                        .effects((platform == JAVA_ANVIL_1_17) ? "minecraft:the_end" : "#minecraft:the_end")
                        .monsterSpawnLightLevel((platform == JAVA_ANVIL_1_20_5) ? MONSTER_SPAWN_LIGHT_LEVEL_POST_1_20_5 : MONSTER_SPAWN_LIGHT_LEVEL_PRE_1_20_4)
                        .build();
            default:
                throw new IllegalArgumentException("Unsupported dimension: " + dim);
        }
    }

    @Value
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class IntProvider {
        String type;
        Object value;
    }
}