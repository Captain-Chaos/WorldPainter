package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.World2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Pepijn on 12-2-2017.
 */
public interface WorldExporter {
    World2 getWorld();

    File selectBackupDir(File worldDir) throws IOException;

    Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled;
}
