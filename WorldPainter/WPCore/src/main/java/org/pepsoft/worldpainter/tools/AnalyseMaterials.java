package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.AbstractMain;
import org.pepsoft.worldpainter.UnloadableWorldException;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import static org.pepsoft.worldpainter.util.MaterialUtils.gatherAllMaterials;

public class AnalyseMaterials extends AbstractMain {
    public static void main(String[] args) throws IOException, UnloadableWorldException {
        initialisePlatform();

        WorldIO worldIO = new WorldIO();
        worldIO.load(new FileInputStream(args[0]));
        World2 world = worldIO.getWorld();
        Set<Material> materials = gatherAllMaterials(world, world.getPlatform());
        materials.forEach(System.out::println);
    }
}
