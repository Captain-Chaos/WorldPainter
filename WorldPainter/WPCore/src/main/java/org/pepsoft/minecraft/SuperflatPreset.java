package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import org.jnbt.*;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_18Biomes.BIOMES_BY_MODERN_ID;
import static org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo.BIOME_PLAINS;
import static org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo.MODERN_IDS;

public class SuperflatPreset implements Serializable {
    public SuperflatPreset(int biome, List<Layer> layers, Map<Structure, Map<String, String>> structures) {
        this.biome = biome;
        this.layers = layers;
        // Deep copy of map to avoid unserializable or read only implementations
        structuresMap = structures.entrySet().stream().collect(toMap(Entry::getKey, entry -> new HashMap<>(entry.getValue())));
    }

    public SuperflatPreset(String biome, List<Layer> layers, Map<Structure, Map<String, String>> structures) {
        biomeName = biome;
        this.layers = layers;
        // Deep copy of map to avoid unserializable or read only implementations
        structuresMap = structures.entrySet().stream().collect(toMap(Entry::getKey, entry -> new HashMap<>(entry.getValue())));
    }

    public int getBiome() {
        return biome;
    }

    public void setBiome(int biome) {
        this.biome = biome;
    }

    public String getBiomeName() {
        return biomeName;
    }

    public void setBiomeName(String biomeName) {
        this.biomeName = biomeName;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
    }

    public Map<Structure, Map<String, String>> getStructures() {
        return structuresMap;
    }

    public void setStructures(Map<Structure, Map<String, String>> structures) {
        // Deep copy of map to avoid unserializable or read only implementations
        structuresMap = structures.entrySet().stream().collect(toMap(Entry::getKey, entry -> new HashMap<>(entry.getValue())));
    }

    public String toMinecraft1_12_2() {
        return "3;" + layers.stream().map(layer -> ((layer.thickness == 1) ? "" : (layer.thickness + "*")) + layer.materialName).collect(joining(",")) +
                ';' + biome +
                ';' + structuresMap.entrySet().stream().map(entry -> entry.getKey().name().toLowerCase()
                    + (entry.getValue().isEmpty()
                        ? ""
                        : "(" + entry.getValue().entrySet().stream().map(attr -> attr.getKey() + "=" + attr.getValue()).collect(joining(" ")) + ")"))
                    .collect(joining(","));
    }

    public CompoundTag toMinecraft1_15_2() {
        return new CompoundTag("generatorOptions", ImmutableMap.of(
                "biome", new StringTag("biome", "minecraft:" + Minecraft1_17Biomes.BIOME_NAMES[biome].toLowerCase().replace(' ', '_')), // TODOMC118 custom biome support
                "layers", new ListTag<>("layers", CompoundTag.class, layers.stream().map(layer -> new CompoundTag("", ImmutableMap.of(
                        "block", new StringTag("block", layer.materialName),
                        "height", new ShortTag("height", (short) layer.thickness)))).collect(toList())),
                "structures", new CompoundTag("structures", structuresMap.entrySet().stream().collect(toMap(
                        structure -> structure.getKey().name().toLowerCase(),
                        structure -> new CompoundTag(structure.getKey().name().toLowerCase(),
                                structure.getValue().entrySet().stream().collect(toMap(Entry::getKey, entry -> new StringTag(entry.getKey(), entry.getValue())))))))
        ));
    }

    public CompoundTag toMinecraft1_18_0() {
        String biome = biomeName;
        if ((biome == null) && (this.biome != -1)) {
            biome = MODERN_IDS[this.biome];
        }
        if (biome == null) {
            logger.warn("Biome ID {} not recognised while exporting Superflat preset; substituting minecraft:plains", biome);
            biome = MC_PLAINS;
        }
        return new CompoundTag(TAG_SETTINGS_, ImmutableMap.of(
                TAG_BIOME_, new StringTag(TAG_BIOME_, biome), // TODOMC118 custom biome support
                TAG_FEATURES_, new ByteTag(TAG_FEATURES_, (byte) 0), // TODOMC118 support
                TAG_LAKES_, new ByteTag(TAG_LAKES_, (byte) 0), // TODOMC118 support
                TAG_LAYERS_, new ListTag<>(TAG_LAYERS_, CompoundTag.class, layers.stream().map(layer -> new CompoundTag("", ImmutableMap.of(
                        TAG_BLOCK_, new StringTag(TAG_BLOCK_, layer.materialName),
                        TAG_HEIGHT_, new IntTag(TAG_HEIGHT_, layer.thickness)))).collect(toList())),
                TAG_STRUCTURES_, new CompoundTag(TAG_STRUCTURES_, ImmutableMap.of( // TODOMC118 support
                        TAG_STRONGHOLD_, new CompoundTag(TAG_STRONGHOLD_, emptyMap()),
                        TAG_STRUCTURES_, new CompoundTag(TAG_STRUCTURES_, emptyMap())
                ))
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuperflatPreset that = (SuperflatPreset) o;
        return biome == that.biome &&
                layers.equals(that.layers) &&
                structuresMap.equals(that.structuresMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(biome, layers, structuresMap);
    }

    @Override
    public String toString() {
        return "SuperflatPreset{" +
                "biome=" + biome +
                ", layers=" + layers +
                ", structures=" + structuresMap +
                '}';
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (structures != null) {
            structuresMap = structures.stream().collect(toMap(identity(), s -> emptyMap()));
            structures = null;
        }
    }

    public static SuperflatPreset fromMinecraft1_12_2(String str) {
        String[] tokens = str.split(";");
        if ((tokens.length < 3) || (! tokens[0].equals("3"))) {
            throw new IllegalArgumentException("Invalid Superflat preset for Minecraft 1.12.2: " + str + " (too few tokens, or invalid version)");
        }
        List<Layer> layers = stream(tokens[1].split(",")).map(layerDescriptor -> {
            int p = layerDescriptor.indexOf('*');
            if (p == -1) {
                return new Layer(layerDescriptor, 1);
            } else {
                return new Layer(layerDescriptor.substring(p + 1), Integer.parseInt(layerDescriptor.substring(0, p)));
            }
        }).collect(toList());
        int biome = Integer.parseInt(tokens[2]);
        Map<Structure, Map<String, String>> structures = new HashMap<>();
        if ((tokens.length >= 4) && (tokens[3] != null)) {
            stream(tokens[3].split(",")).forEach(structure -> {
                int p = structure.indexOf('(');
                if (p == -1) {
                    structures.put(Structure.valueOf(structure.toUpperCase()), emptyMap());
                } else {
                    Structure key = Structure.valueOf(structure.substring(0, p).toUpperCase());
                    Map<String, String> params = new HashMap<>();
                    for (String attr: structure.substring(p + 1, structure.length() - 1).split(" ")) {
                        p = attr.indexOf('=');
                        if (p == -1) {
                            throw new IllegalArgumentException("Invalid Superflat preset for Minecraft 1.12.2: " + str + " (parameter missing =)");
                        }
                        params.put(attr.substring(0, p), attr.substring(p + 1));
                    }
                    structures.put(key, params);
                }
            });
        }
        return new SuperflatPreset(biome, layers, structures);
    }

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    public static SuperflatPreset fromMinecraft1_15_2(CompoundTag tag) {
        return new SuperflatPreset(getBiomeByMinecraft117Name(((StringTag) tag.getTag("biome")).getValue()),
                ((ListTag<CompoundTag>) tag.getTag("layers")).getValue().stream()
                        .map(layerTag -> new Layer(((StringTag) layerTag.getTag("block")).getValue(), ((NumberTag) layerTag.getTag("height")).intValue()))
                        .collect(toList()),
                ((CompoundTag) tag.getTag("structures")).getValue().values().stream()
                        .collect(toMap(structureTag -> Structure.valueOf(structureTag.getName().toUpperCase()),
                                structureTag -> ((CompoundTag) structureTag).getValue().entrySet().stream().collect(toMap(Entry::getKey, paramEntry -> ((StringTag) paramEntry.getValue()).getValue()))))
        );
    }

    @SuppressWarnings("unchecked") // Guaranteed by Minecraft
    public static SuperflatPreset fromMinecraft1_18_0(CompoundTag tag) {
        // TODOMC118 add features, lakes and structures
        final String biomeName = ((StringTag) tag.getTag(TAG_BIOME_)).getValue();
        final SuperflatPreset preset = new SuperflatPreset(biomeName,
                ((ListTag<CompoundTag>) tag.getTag(TAG_LAYERS_)).getValue().stream()
                        .map(layerTag -> new Layer(((StringTag) layerTag.getTag(TAG_BLOCK_)).getValue(), ((NumberTag) layerTag.getTag(TAG_HEIGHT_)).intValue()))
                        .collect(toList()),
                emptyMap()
                );
        try {
            preset.setBiome(getBiomeByMinecraft118Name(biomeName));
        } catch (IllegalArgumentException e) {
            logger.warn("Biome {} not recognised while importing Superflat preset; substituting Plains biome for older Minecraft versions", biomeName);
            preset.setBiome(BIOME_PLAINS);
        }
        return preset;
    }

    public static SuperflatPreset defaultPreset(Platform platform) {
        return new SuperflatPreset(BIOME_PLAINS, asList(new Layer(MC_BEDROCK, 1), new Layer(MC_DIRT, 2), new Layer(((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18) /* TODOMC118 make dynamic */) ? MC_GRASS_BLOCK : "minecraft:grass", 1)), emptyMap());
    }

    public static Builder builder(int biome, Structure... structures) {
        return new Builder(biome, structures);
    }

    private static int getBiomeByMinecraft117Name(String name) {
        name = name.substring(name.indexOf(':') + 1).replace('_', ' ');
        for (int i = 0; i < Minecraft1_17Biomes.BIOME_NAMES.length; i++) {
            if (name.equalsIgnoreCase(Minecraft1_17Biomes.BIOME_NAMES[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown biome name: " + name);
    }

    private static int getBiomeByMinecraft118Name(String name) {
        if (BIOMES_BY_MODERN_ID.containsKey(name)) {
            return BIOMES_BY_MODERN_ID.get(name);
        }
        throw new IllegalArgumentException("Unknown biome name: " + name);
    }

    private int biome = -1;
    private List<Layer> layers;
    @Deprecated
    private Set<Structure> structures;
    private Map<Structure, Map<String, String>> structuresMap;
    private String biomeName;

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SuperflatPreset.class);

    public static class Layer implements Serializable {
        public Layer(String materialName, int thickness) {
            this.materialName = materialName;
            this.thickness = thickness;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public int getThickness() {
            return thickness;
        }

        public void setThickness(int thickness) {
            this.thickness = thickness;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Layer layer = (Layer) o;
            return thickness == layer.thickness &&
                    materialName.equals(layer.materialName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialName, thickness);
        }

        @Override
        public String toString() {
            return "Layer{" +
                    "materialName='" + materialName + '\'' +
                    ", thickness=" + thickness +
                    '}';
        }

        private String materialName;
        private int thickness;

        private static final long serialVersionUID = 1L;
    }

    public enum Structure {VILLAGE, BIOME_1, DECORATION, STRONGHOLD, MINESHAFT, LAKE, LAVA_LAKE, DUNGEON, OCEANMONUMENT, DESERT_PYRAMID, FORTRESS, MANSION, ENDCITY, PILLAGER_OUTPOST, RUINED_PORTAL, BASTION_REMNANT}

    public static class Builder {
        private final int biome;
        private final String biomeName;
        private final Map<Structure, Map<String, String>> structures;
        private final List<Layer> layers = new ArrayList<>();

        Builder(int biome, Structure... structures) {
            this.biome = biome;
            biomeName = null;
            this.structures = ((structures != null) && (structures.length > 0)) ? stream(structures).collect(toMap(identity(), s-> emptyMap())) : emptyMap();
        }

        Builder(String biome, Structure... structures) {
            biomeName = biome;
            this.biome = -1;
            this.structures = ((structures != null) && (structures.length > 0)) ? stream(structures).collect(toMap(identity(), s-> emptyMap())) : emptyMap();
        }

        public Builder addLayer(String materialName, int thickness) {
            layers.add(new Layer(materialName, thickness));
            return this;
        }

        public SuperflatPreset build() {
            return (biomeName != null) ? new SuperflatPreset(biomeName, layers, structures) : new SuperflatPreset(biome, layers, structures);
        }
    }
}