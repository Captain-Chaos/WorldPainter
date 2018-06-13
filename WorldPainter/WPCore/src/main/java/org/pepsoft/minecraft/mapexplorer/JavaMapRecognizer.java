package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.mapexplorer.Node;

import java.io.File;

public class JavaMapRecognizer implements MapRecognizer {
    @Override
    public boolean isMap(File dir) {
        return new File(dir, "level.dat").isFile()
            // Distinguish from Bedrock Edition maps:
            && (! new File(dir, "db").isDirectory())
            && (! new File(dir, "levelname.txt").isFile());
    }

    @Override
    public Node getMapNode(File mapDir) {
        return new JavaMapRootNode(mapDir);
    }
}
