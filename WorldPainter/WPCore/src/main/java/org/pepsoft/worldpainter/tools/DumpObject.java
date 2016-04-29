package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.bo2.Bo2Object;
import org.pepsoft.worldpainter.layers.bo2.Bo3Object;
import org.pepsoft.worldpainter.layers.bo2.Schematic;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 * Created by Pepijn Schmitz on 02-09-15.
 */
public class DumpObject {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String filename = args[0];
        File file = new File(filename);
        int p = filename.lastIndexOf('.');
        Object object;
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
            case "layer":
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                    object = in.readObject();
                }
                break;
            default:
                throw new IllegalArgumentException("File not supported");
        }
        dump(object);
    }

    private static void dump(Object object) {
        if (object instanceof Bo2Layer) {
            ((Bo2Layer) object).getObjectProvider().getAllObjects().forEach(DumpObject::dump);
        } else if (object instanceof WPObject) {
            WPObject wpObject = (WPObject) object;
            System.out.println("Name: " + wpObject.getName());
            Point3i dim = wpObject.getDimensions();
            System.out.println("    Dimensions: " + dim);
            System.out.println("    Offset: " + wpObject.getOffset());
            if (wpObject.getAttributes() != null) {
                System.out.println("    Attributes:");
                wpObject.getAttributes().forEach((key, value) -> System.out.println("        " + key + ": " + value));
            }
            if (wpObject.getEntities() != null) {
                System.out.println("    Entities:");
                wpObject.getEntities().forEach(entity -> System.out.println("        " + entity.getId() + " @ " + entity.getPos()[0] + "," + entity.getPos()[2] + "," + entity.getPos()[1]));
            }
            if (wpObject.getTileEntities() != null) {
                System.out.println("    Tile entities:");
                wpObject.getTileEntities().forEach(entity -> System.out.println("        " + entity.getId() + " @ " + entity.getY() + "," + entity.getZ() + "," + entity.getY()));
            }
            System.out.println("    Blocks:");
            int blockCount = 0;
            for (int z = 0; z < dim.z; z++) {
                for (int x = 0; x < dim.x; x++) {
                    for (int y = 0; y < dim.y; y++) {
                        if (wpObject.getMask(x, y, z)) {
                            blockCount++;
                            if (blockCount <= 100) {
                                System.out.println("        " + x + "," + y + "," + z + ": " + wpObject.getMaterial(x, y, z));
                            }
                        }
                    }
                }
            }
            if (blockCount > 100) {
                System.out.println("        ... and " + (blockCount - 100) + " more");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized object type " + object.getClass());
        }
    }
}