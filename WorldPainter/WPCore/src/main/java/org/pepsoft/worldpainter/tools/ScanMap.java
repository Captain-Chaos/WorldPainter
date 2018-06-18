package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ScanMap {
    public static void main(String[] args) throws IOException {
        MinecraftUtil.visitChunks(new File(args[0]), DIM_NORMAL, chunk -> {
            // Do nothing (we just want to load every chunk)
        });
    }
}