package org.pepsoft.worldpainter.importing;

import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Pepijn on 11-12-2016.
 */
public class Test {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
//        MCPELevel level = MCPELevel.load(levelDatFile);
//        System.out.println(level);
        MCPEMapImporter importer = new MCPEMapImporter(levelDatFile);
        World2 world = importer.doImport();
        WorldIO worldIO = new WorldIO(world);
        worldIO.save(new FileOutputStream("mcpe-map.world"));
    }
}