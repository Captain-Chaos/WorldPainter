package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.minecraft.Level;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.mapexplorer.Node;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

public class JavaMapRecognizer implements MapRecognizer {
    @Override
    public Platform identifyPlatform(File dir) {
        File file = new File(dir, "level.dat");
        if (file.isFile()
                // Distinguish from Bedrock Edition maps:
                && (! new File(dir, "db").isDirectory())
                && (! new File(dir, "levelname.txt").isFile())) {
            try {
                Level level = Level.load(file);
                int version = level.getVersion();
                if (version == VERSION_MCREGION) {
                    return JAVA_MCREGION;
                } else if (version == VERSION_ANVIL) {
                    if (level.getDataVersion() <= DATA_VERSION_MC_1_12_2) {
                        return JAVA_ANVIL;
                    } else {
                        return JAVA_ANVIL_1_13;
                    }
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error reading level.dat", e);
            }
        } else {
            return null;
        }
    }

    @Override
    public Node getMapNode(File mapDir) {
        return new JavaMapRootNode(mapDir);
    }
}
