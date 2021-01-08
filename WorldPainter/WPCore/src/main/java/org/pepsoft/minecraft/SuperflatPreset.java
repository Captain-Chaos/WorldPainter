package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import org.jnbt.*;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_15Biomes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class SuperflatPreset implements Serializable {
    public SuperflatPreset(int biome, List<Layer> layers, Map<Structure, Map<String, String>> structures) {
        this.biome = biome;
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
                "biome", new StringTag("biome", "minecraft:" + Minecraft1_15Biomes.BIOME_NAMES[biome].toLowerCase().replace(' ', '_')),
                "layers", new ListTag<>("layers", CompoundTag.class, layers.stream().map(layer -> new CompoundTag("", ImmutableMap.of(
                        "block", new StringTag("block", layer.materialName),
                        "height", new ShortTag("height", (short) layer.thickness)))).collect(toList())),
                "structures", new CompoundTag("structures", structuresMap.entrySet().stream().collect(toMap(
                        structure -> structure.getKey().name().toLowerCase(),
                        structure -> new CompoundTag(structure.getKey().name().toLowerCase(),
                                structure.getValue().entrySet().stream().collect(toMap(Entry::getKey, entry -> new StringTag(entry.getKey(), entry.getValue())))))))
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
        if (tokens[3] != null) {
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
        return new SuperflatPreset(getBiomeByMinecraftName(((StringTag) tag.getTag("biome")).getValue()),
                ((ListTag<CompoundTag>) tag.getTag("layers")).getValue().stream()
                        .map(layerTag -> new Layer(((StringTag) layerTag.getTag("block")).getValue(), ((NumberTag) layerTag.getTag("height")).intValue()))
                        .collect(toList()),
                ((CompoundTag) tag.getTag("structures")).getValue().values().stream()
                        .collect(toMap(structureTag -> Structure.valueOf(structureTag.getName().toUpperCase()),
                                structureTag -> ((CompoundTag) structureTag).getValue().entrySet().stream().collect(toMap(Entry::getKey, paramEntry -> ((StringTag) paramEntry.getValue()).getValue()))))
        );
    }

    public static Builder builder(int biome, Structure... structures) {
        return new Builder(biome, structures);
    }

    private static int getBiomeByMinecraftName(String name) {
        name = name.substring(name.indexOf(':') + 1).replace('_', ' ');
        for (int i = 0; i < Minecraft1_15Biomes.BIOME_NAMES.length; i++) {
            if (name.equalsIgnoreCase(Minecraft1_15Biomes.BIOME_NAMES[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown biome name: " + name);
    }

    private int biome;
    private List<Layer> layers;
    @Deprecated
    private Set<Structure> structures;
    private Map<Structure, Map<String, String>> structuresMap;

    private static final long serialVersionUID = 1L;

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

    public enum Structure {VILLAGE, BIOME_1, DECORATION, STRONGHOLD, MINESHAFT, LAKE, LAVA_LAKE, DUNGEON, OCEANMONUMENT, DESERT_PYRAMID, FORTRESS, MANSION, ENDCITY, PILLAGER_OUTPOST, RUINED_PORTAL‌, BASTION_REMNANT‌}

    public static class Builder {
        private final int biome;
        private final Map<Structure, Map<String, String>> structures;
        private final List<Layer> layers = new ArrayList<>();

        Builder(int biome, Structure... structures) {
            this.biome = biome;
            this.structures = ((structures != null) && (structures.length > 0)) ? stream(structures).collect(toMap(identity(), s-> emptyMap())) : emptyMap();
        }

        public Builder addLayer(String materialName, int thickness) {
            layers.add(new Layer(materialName, thickness));
            return this;
        }

        public SuperflatPreset build() {
            return new SuperflatPreset(biome, layers, structures);
        }
    }
}