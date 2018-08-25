package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.MC113AnvilChunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.minecraft.MCRegionChunk;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ScanMap {
    public static void main(String[] args) throws IOException {
        MinecraftUtil.visitChunks(new File(args[0]), DIM_NORMAL, chunk -> {
            if (chunk instanceof MCRegionChunk) {

            } else if (chunk instanceof MC12AnvilChunk) {

            } else if (chunk instanceof MC113AnvilChunk) {

            }
        });
    }
}