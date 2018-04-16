package org.pepsoft.worldpainter.util;

import java.io.File;

/**
 * Utility class for creating backups.
 *
 * Created by pepijn on 27-4-15.
 */
public final class BackupUtil {
    private BackupUtil() {
        // Prevent instantiation
    }

    /**
     * Create a backup file by appending a number to the name part of the
     * filename.
     *
     * @param file The file for which to create a backup file.
     * @param backup The backup number to append.
     * @return The file to use for backing up the specified file.
     */
    public static File getBackupFile(File file, int backup) {
        String filename = file.getName();
        int p = filename.lastIndexOf('.');
        if (p != -1) {
            filename = filename.substring(0, p) + "." + backup + filename.substring(p);
        } else {
            filename = filename + "." + backup;
        }
        return new File(file.getParentFile(), filename);
    }
}