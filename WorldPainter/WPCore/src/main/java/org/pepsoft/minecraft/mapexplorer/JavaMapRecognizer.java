package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.mapexplorer.Node;

import java.io.File;

public class JavaMapRecognizer implements MapRecognizer {
    @Override
    public boolean isMap(File dir) {
        File levelDatFile = new File(dir, "level.dat");
        return levelDatFile.isFile();
    }

    @Override
    public Node getMapNode(File mapDir) {
        return new JavaMapRootNode(mapDir);
    }
}
