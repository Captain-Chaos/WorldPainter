package org.pepsoft.minecraft.datapack;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;

/**
 * Settings from official Caves and Cliffs Preview for Minecraft 1.17 from Mojang.
 */
@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Dimension extends Descriptor {
    @Builder.Default int logicalHeight = 384;
    @Builder.Default String infiniburn = "minecraft:infiniburn_overworld";
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
}