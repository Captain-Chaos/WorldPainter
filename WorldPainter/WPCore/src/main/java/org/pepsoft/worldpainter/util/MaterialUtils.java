package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;
import org.pepsoft.worldpainter.layers.plants.Plant;
import org.pepsoft.worldpainter.layers.plants.PlantLayer;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Material.GOLD_ORE;
import static org.pepsoft.minecraft.Material.NETHER_GOLD_ORE;
import static org.pepsoft.worldpainter.Constants.DIM_NETHER;

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
        // Check layers
        for (Dimension dimension: world.getDimensions()) {
            Set<Layer> allLayers = dimension.getAllLayers(true);
            allLayers.addAll(dimension.getMinimumLayers());
            for (Layer layer: allLayers) {
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
                } else if (layer instanceof Resources) {
                    Set<String> nameOnlyMaterialsForLayer = new HashSet<>();
                    ResourcesExporterSettings settings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
                    for (Material material: settings.getMaterials()) {
                        if (settings.getChance(material) > 0) {
                            if (material.blockType == -1) {
                                nameOnlyMaterialsForLayer.add(material.name);
                            } else if ((material == GOLD_ORE) && (dimension.getAnchor().dim == DIM_NETHER)) {
                                nameOnlyMaterialsForLayer.add(NETHER_GOLD_ORE.name);
                            }
                        }
                    }
                    nameOnlyMaterialsForLayer.forEach(name -> nameOnlyMaterials.computeIfAbsent(name, m -> new HashSet<>()).add("Resources layer"));
                } else if (layer instanceof UndergroundPocketsLayer) {
                    checkMixedMaterial(((UndergroundPocketsLayer) layer).getMaterial(), nameOnlyMaterials, "Custom Underground Pockets layer " + layer.getName());
                } else if (layer instanceof GroundCoverLayer) {
                    checkMixedMaterial(((GroundCoverLayer) layer).getMaterial(), nameOnlyMaterials, "Custom Ground Cover layer " + layer.getName());
                } else if (layer instanceof TunnelLayer) {
                    final String name = "Custom Cave/Tunnel layer " + layer.getName();
                    checkMixedMaterial(((TunnelLayer) layer).getFloorMaterial(), nameOnlyMaterials, name);
                    checkMixedMaterial(((TunnelLayer) layer).getRoofMaterial(), nameOnlyMaterials, name);
                    checkMixedMaterial(((TunnelLayer) layer).getWallMaterial(), nameOnlyMaterials, name);
                }
            }
        }
        return nameOnlyMaterials;
    }

    private static void checkMixedMaterial(@Nullable MixedMaterial mixedMaterial, Map<String, Set<String>> nameOnlyMaterials, String name) {
        if (mixedMaterial == null) {
            return;
        }
        for (MixedMaterial.Row row: mixedMaterial.getRows()) {
            if (row.material.blockType == -1) {
                nameOnlyMaterials.computeIfAbsent(row.material.name, m -> new HashSet<>()).add(name);
            }
        }
    }

    public static Set<Material> gatherAllMaterials(World2 world, Platform platform) {
        Set<Material> allMaterials = new HashSet<>();
        // Check custom materials
        Arrays.stream(MixedMaterialManager.getInstance().getMaterials()).forEach(material -> {
            for (MixedMaterial.Row row: material.getRows()) {
                allMaterials.add(row.material);
            }
        });
        for (Dimension dimension: world.getDimensions()) {
            // Check custom object layers
            for (Layer layer: dimension.getAllLayers(true)) {
                if (layer instanceof Bo2Layer) {
                    for (WPObject object: ((Bo2Layer) layer).getObjectProvider().getAllObjects()) {
                        allMaterials.addAll(object.getAllMaterials());
                    }
                } else if (layer instanceof PlantLayer) {
                    for (Plant plant: ((PlantLayer) layer).getConfiguredPlants().keySet()) {
                        allMaterials.addAll(plant.realise(plant.getMaxGrowth(), platform).getAllMaterials());
                    }
                }
                // TODO other layer types
            }
            // Check all terrain types
            dimension.visitTiles().andDo(tile -> tile.getAllTerrains().forEach(terrain -> allMaterials.addAll(terrain.getAllMaterials())));
            // Check the underground material
            allMaterials.addAll(dimension.getSubsurfaceMaterial().getAllMaterials());
        }
        return allMaterials;
    }
}