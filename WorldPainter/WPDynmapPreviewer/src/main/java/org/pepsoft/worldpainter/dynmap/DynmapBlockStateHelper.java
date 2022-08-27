package org.pepsoft.worldpainter.dynmap;

import org.dynmap.renderer.DynmapBlockState;
import org.pepsoft.minecraft.Material;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.pepsoft.minecraft.Constants.MC_AIR;
import static org.pepsoft.minecraft.Constants.MC_CAVE_AIR;
import static org.pepsoft.minecraft.Material.MINECRAFT;
import static org.pepsoft.minecraft.Material.WATERLOGGED;

public class DynmapBlockStateHelper {
    public static void initialise() {
        // Do nothing (loading the class has done the initialisation)
    }

    static synchronized DynmapBlockState getDynmapBlockState(Material material) {
        return MATERIAL_TO_BLOCK_STATE.get(material);
    }

    private static final Map<Material, DynmapBlockState> MATERIAL_TO_BLOCK_STATE = new IdentityHashMap<>();
    private static final Map<String, Set<DynmapBlockState>> BLOCK_STATES_BY_NAME = new HashMap<>();
    private static final Map<String, DynmapBlockState> BLOCK_STATES_BASES_BY_NAME = new HashMap<>();

    static {
        // TODO this will take a long time and hugely expand the number of loaded materials; make this more efficient
        for (String simpleName: Material.getAllSimpleNamesForNamespace(MINECRAFT)) {
            final Material prototype = Material.getPrototype(MINECRAFT + ':' + simpleName);
            if ((prototype.propertyDescriptors == null) || prototype.propertyDescriptors.isEmpty()) {
                // Simple material
                DynmapBlockState blockState = new DynmapBlockState(null,
                        0,
                        prototype.name,
                        null,
                        prototype.simpleName,
                        prototype.blockType);
                setFlags(blockState, prototype);
                BLOCK_STATES_BASES_BY_NAME.put(prototype.name, blockState);
                BLOCK_STATES_BY_NAME.computeIfAbsent(prototype.name, k -> new HashSet<>()).add(blockState);
                MATERIAL_TO_BLOCK_STATE.put(prototype, blockState);
//                System.out.println(prototype + " -> " + blockState);
            } else {
                // Create all possible permutations of properties
                // TODO make this more efficient if possible
                List<Map<String, String>> permutations = new ArrayList<>();
                permutations.add(emptyMap());
                for (Map.Entry<String, Material.PropertyDescriptor> entry: prototype.propertyDescriptors.entrySet()) {
                    final String name = entry.getKey();
                    final Material.PropertyDescriptor descriptor = entry.getValue();
                    List<Map<String, String>> newPermutations = new ArrayList<>(permutations.size() * 5);
                    for (Map<String, String> partialPermutation: permutations) {
                        switch (descriptor.type) {
                            case BOOLEAN:
                                newPermutations.add(copyAndAdd(partialPermutation, name, "true"));
                                newPermutations.add(copyAndAdd(partialPermutation, name, "false"));
                                break;
                            case ENUM:
                                for (String value: descriptor.enumValues) {
                                    newPermutations.add(copyAndAdd(partialPermutation, name, value));
                                }
                                break;
                            case INTEGER:
                                for (int value = descriptor.minValue; value <= descriptor.maxValue; value++) {
                                    newPermutations.add(copyAndAdd(partialPermutation, name, Integer.toString(value)));
                                }
                                break;
                        }
                    }
                    permutations = newPermutations;
                }
                for (Map<String, String> properties: permutations) {
                    final Material material = Material.get(prototype.name, properties);
                    if (material.data >= 0) {
                        properties.put("meta", Integer.toString(material.data));
                    }
                    final String stateName = properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(joining(","));
                    final DynmapBlockState blockState;
                    if (BLOCK_STATES_BY_NAME.containsKey(material.name)) {
                        // Variation
                        blockState = new DynmapBlockState(BLOCK_STATES_BASES_BY_NAME.get(material.name),
                                BLOCK_STATES_BY_NAME.get(material.name).size(),
                                material.name,
                                stateName,
                                material.simpleName,
                                material.blockType);
                    } else {
                        // Base block
                        blockState = new DynmapBlockState(null,
                                0,
                                material.name,
                                stateName,
                                material.simpleName,
                                material.blockType);
                        BLOCK_STATES_BASES_BY_NAME.put(material.name, blockState);
                    }
                    setFlags(blockState, material);
                    BLOCK_STATES_BY_NAME.computeIfAbsent(material.name, k -> new HashSet<>()).add(blockState);
                    MATERIAL_TO_BLOCK_STATE.put(material, blockState);
//                    System.out.println(material + " -> " + blockState);
                }
            }
        }
        DynmapBlockState.finalizeBlockStates();
    }

    private static <K, V> Map<K, V> copyAndAdd(Map<K, V> map, K key, V value) {
        final Map<K, V> copy = new HashMap<>(map);
        copy.put(key, value);
        return copy;
    }

    private static void setFlags(DynmapBlockState blockState, Material material) {
        if (material.isNamedOneOf(MC_AIR, MC_CAVE_AIR)) {
            blockState.setAir();
        }
        if (material.simpleName.endsWith("_log")) {
            blockState.setLog();
        }
        if (material.simpleName.endsWith("_leaves")) {
            blockState.setLeaves();
        }
        if (material.solid) {
            blockState.setSolid();
        }
        if (material.is(WATERLOGGED)) {
            blockState.setWaterlogged();
        }
    }
}