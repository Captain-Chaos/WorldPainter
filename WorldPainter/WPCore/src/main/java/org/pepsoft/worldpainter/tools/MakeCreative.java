package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.JavaLevel;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.GAME_TYPE_CREATIVE;

public class MakeCreative {
    public static void main(String[] args) throws IOException {
        final File worldDir = new File(args[0]);
        JavaLevel level = JavaLevel.load(new File(worldDir, "level.dat"));
        level.setGameType(GAME_TYPE_CREATIVE);
        level.setAllowCommands(true);
        level.save(worldDir);
    }
}