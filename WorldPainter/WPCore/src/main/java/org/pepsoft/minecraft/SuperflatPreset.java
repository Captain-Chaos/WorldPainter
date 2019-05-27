package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import org.jnbt.*;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes;

import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;

public class SuperflatPreset implements Serializable {
    public SuperflatPreset(int biome, List<Layer> layers, Set<Structure> structures) {
        this.biome = biome;
        this.layers = layers;
        this.structures = structures;
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

    public Set<Structure> getStructures() {
        return structures;
    }

    public void setStructures(Set<Structure> structures) {
        this.structures = structures;
    }

    public String toMinecraft1_12_2() {
        return "3;" + layers.stream().map(layer -> ((layer.thickness == 1) ? "" : (layer.thickness + "*")) + layer.materialName).collect(joining(",")) +
                ';' + biome +
                ';' + structures.stream().map(structure -> structure.name().toLowerCase()).collect(joining(","));
    }

    public CompoundTag toMinecraft1_13_2() {
        return new CompoundTag("generatorOptions", ImmutableMap.of(
                "biome", new StringTag("biome", "minecraft:" + Minecraft1_13Biomes.BIOME_NAMES[biome].toLowerCase().replace(' ', '_')),
                "layers", new ListTag<>("layers", CompoundTag.class, layers.stream().map(layer -> new CompoundTag("", ImmutableMap.of(
                        "block", new StringTag("block", layer.materialName),
                        "height", new ShortTag("height", (short) layer.thickness)))).collect(toList())),
                "structures", new CompoundTag("structures", structures.stream().collect(toMap(
                        structure -> structure.name().toLowerCase(),
                        structure -> new CompoundTag(structure.name().toLowerCase(), emptyMap()))))
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuperflatPreset that = (SuperflatPreset) o;
        return biome == that.biome &&
                layers.equals(that.layers) &&
                structures.equals(that.structures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(biome, layers, structures);
    }

    @Override
    public String toString() {
        return "SuperflatPreset{" +
                "biome=" + biome +
                ", layers=" + layers +
                ", structures=" + structures +
                '}';
    }

    public static SuperflatPreset fromMinecraft1_12_2(String str) {
        String[] tokens = str.split(";");
        if ((tokens.length < 3) || (! tokens[0].equals("3"))) {
            throw new IllegalArgumentException("Invalid Superflat preset for Minecraft 1.12.2");
        }
        List<Layer> layers = Arrays.stream(tokens[1].split(",")).map(layerDescriptor -> {
            int p = layerDescriptor.indexOf('*');
            if (p == -1) {
                return new Layer(layerDescriptor, 1);
            } else {
                return new Layer(layerDescriptor.substring(p + 1), Integer.valueOf(layerDescriptor.substring(0, p)));
            }
        }).collect(toList());
        int biome = Integer.valueOf(tokens[2]);
        Set<Structure> structures = EnumSet.noneOf(Structure.class);
        if (tokens[3] != null) {
            Arrays.stream(tokens[3].split(",")).forEach(structureName -> structures.add(Structure.valueOf(structureName.toUpperCase())));
        }
        return new SuperflatPreset(biome, layers, structures);
    }

    public static SuperflatPreset fromMinecraft1_13_2(String str) {
        String[] tokens = str.split(";");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Invalid Superflat preset for Minecraft 1.12.2");
        }
        List<Layer> layers = Arrays.stream(tokens[0].split(",")).map(layerDescriptor -> {
            int p = layerDescriptor.indexOf('*');
            if (p == -1) {
                return new Layer(layerDescriptor, 1);
            } else {
                return new Layer(layerDescriptor.substring(p + 1), Integer.valueOf(layerDescriptor.substring(0, p)));
            }
        }).collect(toList());
        Set<Structure> structures = EnumSet.noneOf(Structure.class);
        if (tokens[2] != null) {
            Arrays.stream(tokens[2].split(",")).forEach(structureName -> structures.add(Structure.valueOf(structureName.toUpperCase())));
        }
        return new SuperflatPreset(getBiomeByMinecraftName(tokens[1]), layers, structures);
    }

    public static SuperflatPreset fromMinecraft1_13_2(CompoundTag tag) {
        String biomeName = ((StringTag) tag.getTag("biome")).getValue();
        List<Layer> layers = ((ListTag<CompoundTag>) tag.getTag("layers")).getValue().stream()
                .map(layerTag -> new Layer(((StringTag) layerTag.getTag("block")).getValue(), (layerTag.getTag("height") instanceof ByteTag) ? ((ByteTag) layerTag.getTag("height")).getValue() : ((ShortTag) layerTag.getTag("height")).getValue()))
                .collect(toList());
        Set<Structure> structures = EnumSet.noneOf(Structure.class);
        ((CompoundTag) tag.getTag("structures")).getValue().forEach((name, structureTag) -> {
            structures.add(Structure.valueOf(structureTag.getName().toUpperCase()));
        });
        return new SuperflatPreset(getBiomeByMinecraftName(biomeName), layers, structures);
    }

    public static Builder builder(int biome, Structure... structures) {
        return new Builder(biome, structures);
    }

    private static int getBiomeByMinecraftName(String name) {
        name = name.substring(name.indexOf(':') + 1).replace('_', ' ');
        for (int i = 0; i < Minecraft1_13Biomes.BIOME_NAMES.length; i++) {
            if (name.equalsIgnoreCase(Minecraft1_13Biomes.BIOME_NAMES[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown biome name: " + name);
    }

    private int biome;
    private List<Layer> layers;
    private Set<Structure> structures;

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

    public enum Structure {VILLAGE, BIOME_1, DECORATION, STRONGHOLD, MINESHAFT, LAKE, LAVA_LAKE, DUNGEON, OCEANMONUMENT}

    public static class Builder {
        private final int biome;
        private final Set<Structure> structures;
        private final List<Layer> layers = new ArrayList<>();

        Builder(int biome, Structure... structures) {
            this.biome = biome;
            this.structures = ((structures != null) && (structures.length > 0)) ? EnumSet.copyOf(asList(structures)) : EnumSet.noneOf(Structure.class);
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