package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.layers.bo2.Bo2Object;
import org.pepsoft.worldpainter.layers.bo2.Bo3Object;
import org.pepsoft.worldpainter.layers.bo2.Schematic;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.File;
import java.io.IOException;

/**
 * Created by Pepijn Schmitz on 02-09-15.
 */
public class DumpObject {
    public static void main(String[] args) throws IOException {
        String filename = args[0];
        File file = new File(filename);
        int p = filename.lastIndexOf('.');
        WPObject object;
        switch (filename.substring(p + 1).toLowerCase()) {
            case "bo2":
                object = Bo2Object.load(file);
                break;
            case "bo3":
                object = Bo3Object.load(file);
                break;
            case "schematic":
                object = Schematic.load(file);
                break;
            default:
                throw new IllegalArgumentException("File not supported");
        }
        System.out.println("Name: " + object.getName());
        Point3i dim = object.getDimensions();
        System.out.println("Dimensions: " + dim);
        System.out.println("Offset: " + object.getOffset());
        if (object.getAttributes() != null) {
            System.out.println("Attributes:");
            object.getAttributes().forEach((key, value) -> System.out.println("    " + key + ": " + value));
        }
        if (object.getEntities() != null) {
            System.out.println("Entities:");
            object.getEntities().forEach(entity -> System.out.println("    " + entity.getId() + " @ " + entity.getPos()[0] + "," + entity.getPos()[2] + "," + entity.getPos()[1]));
        }
        if (object.getTileEntities() != null) {
            System.out.println("Tile entities:");
            object.getTileEntities().forEach(entity -> System.out.println("    " + entity.getId() + " @ " + entity.getY() + "," + entity.getZ() + "," + entity.getY()));
        }
        System.out.println("Blocks:");
        int blockCount = 0;
        for (int z = 0; z < dim.z; z++) {
            for (int x = 0; x < dim.x; x++) {
                for (int y = 0; y < dim.y; y++) {
                    if (object.getMask(x, y, z)) {
                        blockCount++;
                        if (blockCount <= 100) {
                            System.out.println("    " + x + "," + y + "," + z + ": " + object.getMaterial(x, y, z));
                        }
                    }
                }
            }
        }
        if (blockCount > 100) {
            System.out.println("    ... and " + (blockCount - 100) + " more");
        }
    }
}