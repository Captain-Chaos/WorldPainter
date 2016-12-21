package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.World2;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Created by Pepijn on 11-12-2016.
 */
public abstract class AbstractWorldExporter {
    protected AbstractWorldExporter(World2 world) {
        if (world == null) {
            throw new NullPointerException();
        }
        this.world = world;
        this.selectedTiles = world.getTilesToExport();
        this.selectedDimensions = world.getDimensionsToExport();
        if ((selectedTiles != null) && (selectedDimensions.size() != 1)) {
            throw new IllegalArgumentException("When a tile selection is present exactly one dimension must be selected");
        }
    }

    public World2 getWorld() {
        return world;
    }

    public File selectBackupDir(File worldDir) throws IOException {
        File baseDir = worldDir.getParentFile();
        File minecraftDir = baseDir.getParentFile();
        File backupsDir = new File(minecraftDir, "backups");
        if ((! backupsDir.isDirectory()) &&  (! backupsDir.mkdirs())) {
            backupsDir = new File(System.getProperty("user.home"), "WorldPainter Backups");
            if ((! backupsDir.isDirectory()) && (! backupsDir.mkdirs())) {
                throw new IOException("Could not create " + backupsDir);
            }
        }
        return new File(backupsDir, worldDir.getName() + "." + DATE_FORMAT.format(new Date()));
    }

    public abstract  Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled;

    protected final World2 world;
    protected final Set<Integer> selectedDimensions;
    protected final Set<Point> selectedTiles;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
}