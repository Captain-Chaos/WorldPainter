/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.util.*;

/**
 * A manager of {@link MixedMaterial}s.
 * 
 * @author Pepijn Schmitz
 */
public class MixedMaterialManager {
    public MixedMaterial[] getMaterials() {
        Set<MixedMaterial> materials = idsByMaterial.keySet();
        return materials.toArray(new MixedMaterial[materials.size()]);
    }
    
    /**
     * Forget all registered materials; used when unloading a world.
     */
    public synchronized void clear() {
        materialsById.clear();
        idsByMaterial.clear();
    }
    
    /**
     * Register a new material; used among others by the readObject() method
     * of MixedMaterial to automatically register materials from existing
     * worlds.
     * 
     * @param material The material to register.
     * @return The actual material to use instead of the specified one. This
     *     <em>may</em> be the same instance, but it may not be.
     */
    public synchronized MixedMaterial register(MixedMaterial material) {
        UUID id = material.getId();
        if (id == null) {
            // Old material without an ID. See if a material with the exact same
            // settings already exist. If so use that, if not, assign a new ID.
            id = idsByMaterial.get(material);
            if (id != null) {
                // A material with the same settings already exists
                return materialsById.get(id);
            } else {
                material = new MixedMaterial(material.getName(), material.getRows(), material.getBiome(), material.getMode(), material.getScale(), material.getColour(), material.getVariation(), material.getLayerXSlope(), material.getLayerYSlope(), material.isRepeat());
                materialsById.put(material.getId(), material);
                idsByMaterial.put(material, material.getId());
                return material;
            }
        } else if (materialsById.containsKey(id)) {
            return materialsById.get(id);
        } else {
            materialsById.put(id, material);
            idsByMaterial.put(material, id);
            return material;
        }
    }
    
    public static MixedMaterialManager getInstance() {
        return INSTANCE;
    }
    
    private final Map<UUID, MixedMaterial> materialsById = new HashMap<>();
    private final SortedMap<MixedMaterial, UUID> idsByMaterial = new TreeMap<>();
    
    private static final MixedMaterialManager INSTANCE = new MixedMaterialManager();
}
