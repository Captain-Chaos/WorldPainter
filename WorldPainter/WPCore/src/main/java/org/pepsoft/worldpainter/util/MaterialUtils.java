package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.plants.Plant;
import org.pepsoft.worldpainter.layers.plants.PlantLayer;
import org.pepsoft.worldpainter.objects.WPObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for working with materials.
 */
public class MaterialUtils {
    /**
     * Gather descriptions of all materials that don't have a block ID (and are
     * therefore not compatible with Minecraft 1.13 or later) from:
     *
     * <ul><li>The currently loaded custom materials in the
     * {@link MixedMaterialManager}
     * <li>The specified {@code World}
     * </ul>
     *
     * @param world    The world from which to obtain all materials without
     *                 block IDs.
     * @param platform The platform for which the materials are to be
     *                 determined.
     * @return A map of object descriptions to sets of material names describing
     * one or more materials uses by that object.
     */
    public static Map<String, Set<String>> gatherBlocksWithoutIds(World2 world, Platform platform) {
        // Check for materials that have no block IDs
        // TODO: anything else?
        // Check custom materials
        final Map<String, Set<String>> nameOnlyMaterials = new HashMap<>();
        Arrays.stream(MixedMaterialManager.getInstance().getMaterials()).forEach(material -> {
            for (MixedMaterial.Row row: material.getRows()) {
                if (row.material.blockType == -1) {
                    nameOnlyMaterials.computeIfAbsent(row.material.name, m -> new HashSet<>()).add("custom material " + material.getName());
                }
            }
        });
        // Check custom object layers
        for (Dimension dimension: world.getDimensions()) {
            for (Layer layer: dimension.getAllLayers(true)) {
                if (layer instanceof Bo2Layer) {
                    Map<String, List<WPObject>> nameOnlyMaterialsForLayer = new HashMap<>();
                    for (WPObject object: ((Bo2Layer) layer).getObjectProvider().getAllObjects()) {
                        object.getAllMaterials().forEach(material -> {
                            if (material.blockType == -1) {
                                nameOnlyMaterialsForLayer.computeIfAbsent(material.name, m -> new ArrayList<>()).add(object);
                            }
                        });
                    }
                    nameOnlyMaterialsForLayer.forEach((name, objects) -> {
                        if (objects.size() == 1) {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("object " + objects.iterator().next().getName() + " in layer " + layer.getName());
                        } else if (objects.size() <= 3) {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("objects " + objects.stream().map(WPObject::getName).collect(Collectors.joining(", ")) + " in layer " + layer.getName());
                        } else {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("objects " + objects.subList(0, 2).stream().map(WPObject::getName).collect(Collectors.joining(", ")) + " and " + (objects.size() - 2) + " more in layer " + layer.getName());
                        }
                    });
                } else if (layer instanceof PlantLayer) {
                    Map<String, List<Plant>> nameOnlyMaterialsForLayer = new HashMap<>();
                    for (Plant plant: ((PlantLayer) layer).getConfiguredPlants().keySet()) {
                        plant.realise(plant.getMaxGrowth(), platform).getAllMaterials().forEach(material -> {
                            if (material.blockType == -1) {
                                nameOnlyMaterialsForLayer.computeIfAbsent(material.name, m -> new ArrayList<>()).add(plant);
                            }
                        });
                    }
                    nameOnlyMaterialsForLayer.forEach((name, plants) -> {
                        if (plants.size() == 1) {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("plant " + plants.iterator().next().getName() + " in layer " + layer.getName());
                        } else if (plants.size() <= 3) {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("plants " + plants.stream().map(WPObject::getName).collect(Collectors.joining(", ")) + " in layer " + layer.getName());
                        } else {
                            nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("plants " + plants.subList(0, 2).stream().map(WPObject::getName).collect(Collectors.joining(", ")) + " and " + (plants.size() - 2) + " more in layer " + layer.getName());
                        }
                    });
                }
            }
        }
        return nameOnlyMaterials;
    }
}