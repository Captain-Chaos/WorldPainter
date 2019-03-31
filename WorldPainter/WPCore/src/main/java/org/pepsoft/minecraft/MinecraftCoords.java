package org.pepsoft.minecraft;

import java.io.Serializable;

/**
 * A set of two dimensional coordinates in the Minecraft coordinate system (X
 * axis increasing from west to east and Z axis increasing from north to south).
 */
public final class MinecraftCoords implements Serializable {
    public MinecraftCoords(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MinecraftCoords)
                && x == ((MinecraftCoords) o).x
                && z == ((MinecraftCoords) o).z;
    }

    @Override
    public int hashCode() {
        return x * 65537 + z;
    }

    @Override
    public String toString() {
        return x + "," + z;
    }

    public final int x, z;

    private static final long serialVersionUID = 1L;
}
