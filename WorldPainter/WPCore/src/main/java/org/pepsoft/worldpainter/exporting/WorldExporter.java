package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.World2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.pepsoft.util.FileUtils.sanitiseName;

/**
 * A class which knows how to export a WorldPainter world in the map format of a
 * particular {@link org.pepsoft.worldpainter.Platform}.
 *
 * Created by Pepijn on 12-2-2017.
 */
public interface WorldExporter {
    /**
     * Get the world that is being or has been exported.
     *
     * @return The world that is being or has been exported.
     */
    World2 getWorld();

    /**
     * Indicate into which directory the existing map, if any, would be backed up if the specified world would be
     * exported into the specified base directory.
     *
     * @param baseDir The base directory in which the map directory would be created into which the world would be
     *                exported.
     * @param name    The map name to which the world is being exported.
     * @return The directory to which the existing map at that location, if any, should be backed up.
     * @throws IOException If an I/O error occurs while determining the backup directory.
     */
    default File selectBackupDir(File baseDir, String name) throws IOException {
        return selectBackupDir(new File(baseDir, sanitiseName(name)));
    }

    /**
     * Indicate into which directory the existing map, if any, will be backed up that is going to be exported into
     * {@code worldDir}.
     *
     * @param worldDir The map directory into which the world would be exported.
     * @return The directory to which the existing map at that location, if any, should be backed up.
     * @throws IOException If an I/O error occurs while determining the backup directory.
     */
    File selectBackupDir(File worldDir) throws IOException;

    /**
     * Export the world previously configured for export by the call to
     * {@link org.pepsoft.worldpainter.plugins.PlatformProvider#getExporter(World2)}
     * to a directory with the specified name in the specified directory.
     *
     * @param baseDir The directory in which to create the map directory.
     * @param name The name to give the map directory.
     * @param backupDir The directory to which to backup the existing map
     *                  directory, if any.
     * @param progressReceiver An optional progress receiver to which to report
     *                         the progress of the export. May be {@code null}.
     * @return Per-dimension statistics about the export process and result.
     * @throws IOException If an I/O error occurs while exporting the world.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver has
     * thrown an {@link ProgressReceiver.OperationCancelled} exception, or the
     * user has in some other manner indicated that the export should be
     * aborted.
     */
    Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled;
}
