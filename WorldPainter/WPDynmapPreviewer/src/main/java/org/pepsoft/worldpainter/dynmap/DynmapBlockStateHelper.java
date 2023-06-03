package org.pepsoft.worldpainter.dynmap;

import com.google.common.collect.ImmutableMap;
import org.dynmap.renderer.DynmapBlockState;
import org.pepsoft.minecraft.Material;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.pepsoft.minecraft.Constants.MC_AIR;
import static org.pepsoft.minecraft.Constants.MC_CAVE_AIR;
import static org.pepsoft.minecraft.Material.MINECRAFT;

public class DynmapBlockStateHelper {
    /**
     * Initialise the Dynmap {@link DynmapBlockState}s for all currently loaded {@link Material}s. This can be done
     * only once, after which Dynmap will only be able to render the materials known at that time.
     */
    public static void initialise() {
        // Do nothing (loading the class has done the initialisation)
    }

    static DynmapBlockState getDynmapBlockState(Material material) {
        return IDENTITY_TO_BLOCK_STATE.get(material.identity);
    }

    private static final Map<Material.Identity, DynmapBlockState> IDENTITY_TO_BLOCK_STATE;

    static {
        final Map<Material.Identity, DynmapBlockState> identityToBlockState = new HashMap<>();
        final Map<String, Set<DynmapBlockState>> blockStatesByName = new HashMap<>();
        final Map<String, DynmapBlockState> blockStatesBasesByName = new HashMap<>();
        for (String simpleName: Material.getAllSimpleNamesForNamespace(MINECRAFT)) {
            final Material prototype = Material.getPrototype(MINECRAFT + ':' + simpleName);
            if ((prototype.propertyDescriptors == null) || prototype.propertyDescriptors.isEmpty()) {
                // Simple material
                final DynmapBlockState blockState = new DynmapBlockState(null,
                        0,
                        prototype.name,
                        null,
                        prototype.simpleName,
                        prototype.blockType);
                setFlags(blockState, prototype);
                blockStatesBasesByName.put(prototype.name, blockState);
                blockStatesByName.computeIfAbsent(prototype.name, k -> new HashSet<>()).add(blockState);
                identityToBlockState.put(prototype.identity, blockState);
            } else {
                // Create all possible permutations of properties
                // Material.propertyDescriptors is sorted by property name, which is important to Dynmap as it expects
                // both the block variants and the properties in the statename to be in an exact order for the rendering
                // logic to work properly
                List<Map<String, String>> permutations = new ArrayList<>();
                permutations.add(new HashMap<>());
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
                        // Also include a permutation _without_ each property
                        newPermutations.add(partialPermutation);
                    }
                    permutations = newPermutations;
                }

                // Make sure the permutations with all properties set are at the start of the list, to ensure those are
                // at the indices that Dynmap expects in its rendering logic. We only add the variants with missing
                // properties to cover blocks that we might load from custom objects, etc., and accept that Dynmap might
                // not be able to render those
                final Set<Map<String, String>> permutationsWithMissingProperties = new HashSet<>(permutations.size());
                for (Iterator<Map<String, String>> i = permutations.iterator(); i.hasNext(); ) {
                    final Map<String, String> permutation = i.next();
                    if (permutation.size() < prototype.propertyDescriptors.size()) {
                        i.remove();
                        permutationsWithMissingProperties.add(permutation);
                    }
                }
                // Add them back at the end of the list
                permutations.addAll(permutationsWithMissingProperties);

                for (Map<String, String> properties: permutations) {
                    final Material material = Material.get(prototype.name, properties);
                    if (identityToBlockState.containsKey(material.identity)) {
                        continue;
                    }
                    final String stateName = properties.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(joining(","));
                    final DynmapBlockState blockState;
                    if (blockStatesByName.containsKey(material.name)) {
                        // Variation
                        blockState = new DynmapBlockState(blockStatesBasesByName.get(material.name),
                                blockStatesByName.get(material.name).size(),
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
                        blockStatesBasesByName.put(material.name, blockState);
                    }
                    setFlags(blockState, material);
                    blockStatesByName.computeIfAbsent(material.name, k -> new HashSet<>()).add(blockState);
                    identityToBlockState.put(material.identity, blockState);
                }
            }
        }
        IDENTITY_TO_BLOCK_STATE = ImmutableMap.copyOf(identityToBlockState);
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
        if (material.sustainsLeaves) {
            blockState.setLog();
        }
        if (material.leafBlock) {
            blockState.setLeaves();
        }
        if (material.solid) {
            blockState.setSolid();
        }
        if (material.containsWater()) {
            blockState.setWaterlogged();
        }
    }
}