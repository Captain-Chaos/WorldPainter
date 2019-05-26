package org.pepsoft.minecraft.exception;

import org.pepsoft.minecraft.Material;

/**
 * Thrown by plugins to indicate they don't support a material that is being
 * passed to them.
 */
public class IncompatibleMaterialException extends IllegalArgumentException {
    private final Material material;

    public IncompatibleMaterialException(Material material) {
        super("Material " + material + " not supported");
        this.material = material;
    }

    public IncompatibleMaterialException(String message, Material material) {
        super(message);
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }
}