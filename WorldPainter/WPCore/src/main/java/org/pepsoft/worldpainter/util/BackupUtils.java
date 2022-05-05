package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.*;
import static org.pepsoft.util.DesktopUtils.beep;
import static org.pepsoft.util.FileUtils.deleteDir;
import static org.pepsoft.worldpainter.Constants.GB;
import static org.pepsoft.worldpainter.exporting.AbstractWorldExporter.DATE_FORMAT;

/**
 * Utility class for creating and managing backups.
 *
 * Created by pepijn on 27-4-15.
 */
public final class BackupUtils {
    private BackupUtils() {
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

    /**
     * Deletes backups, oldest first, until there is at least {@link Configuration}{@code .minimumFreeSpaceForMaps} GB
     * free, and only if enabled and/or the user gives permission. Because of that, even if this method returns
     * {@code true} it is not guaranteed that there is at least {@code minimumFreeSpaceForMaps} GB free.
     *
     * <p>If an {@code exportDir} is specified, this is assumed to be an automatic cleanup. Only the file store on which
     * the {@code worldDir} is located will be cleaned, and dialogs will only be shown to the user if a cleanup was
     * actually necessary.
     *
     * <p>If an {code exportDir} is <em>not</em> specified, this is assumed to be a manual request. The method will try
     * to find all backup directories on all file stores and clean them if necessary, and will always give feedback to
     * the user through dialogs, even if nothing was done.
     *
     * @param exportDir The directory to which the world is being exported. May be {@code null}.
     * @param parent The parent window for notifications. May be {@code null}.
     * @return {@code false} if the user indicated the operation should be cancelled, {@code true} otherwise.
     */
    public static synchronized boolean cleanUpBackups(File exportDir, Window parent) throws IOException {
        Configuration config = Configuration.getInstance();
        if ((config == null) || ((! config.isAutoDeleteBackups()) && (exportDir != null))) {
            // Either there is no configuration, or there is, but autoDeleteBackups is off and this is an automatic
            // cleanup
            return true;
        }
        final Map<FileStore, Set<File>> backupsDirs = (exportDir != null) ? selectDirsForAutomatic(exportDir) : selectDirsForManual();
        final int minimumFreeSpace = config.getMinimumFreeSpaceForMaps();
        final AtomicBoolean permissionGiven = new AtomicBoolean(exportDir == null); // For manual requests, permission is implicit
        final StringBuilder report = new StringBuilder();

        for (final Map.Entry<FileStore, Set<File>> entry: backupsDirs.entrySet()) {
            final FileStore store = entry.getKey();
            if (store.getUsableSpace() >= (minimumFreeSpace * GB)) {
                logger.debug("File store {} has more than {} GB usable space", store, minimumFreeSpace);
                report.append("File store ").append(store).append(" has more than ").append(minimumFreeSpace).append(" GB usable space; no action taken\n");
            } else {
                if (! permissionGiven.get()) {
                    beep();
                    final int rc = JOptionPane.showConfirmDialog(parent, "There is less than " + minimumFreeSpace + " GB free on file store " + store + "\nWould you like to make space by deleting old backups, if possible?", "Disk Space Low", YES_NO_CANCEL_OPTION);
                    switch (rc) {
                        case YES_OPTION:
                            // Clean up the backups
                            permissionGiven.set(true);
                            break;
                        case NO_OPTION:
                            // Don't clean up the backups, but continue the operation
                            return true;
                        case CANCEL_OPTION:
                            // Don't clean up the backups and cancel the operation
                            return false;
                    }
                }
                final int count = doDeleteBackups(entry.getValue(), minimumFreeSpace, store);
                if (count == 0) {
                    logger.debug("No backups to delete from {}", store);
                    report.append("No backups to delete on ").append(store).append('\n');
                } else {
                    logger.debug("{} backups deleted from {}", count, store);
                    report.append(count).append(" backups were deleted from ").append(store).append('\n');
                }
                if (store.getUsableSpace() >= (minimumFreeSpace * GB)) {
                    logger.debug("There is now more than {} GB free on {}", minimumFreeSpace, store);
                    report.append("There is now more than ").append(minimumFreeSpace).append(" GB free on ").append(store).append('\n');
                } else {
                    logger.warn("There is still less than {} GB free on {}", minimumFreeSpace, store);
                    report.append("There is still less than ").append(minimumFreeSpace).append(" GB free on ").append(store).append("!\n");
                }
            }
        }

        if (exportDir != null) {
            // Automatic cleanup; check that there is now enough space and if not, confirm that the user wants to
            // continue
            final FileStore store = Files.getFileStore(exportDir.toPath());
            if (store.getUsableSpace() < (minimumFreeSpace * GB)) {
                logger.warn("After deleting backups there is still less than {} GB free on {}", minimumFreeSpace, store);
                final int rc = JOptionPane.showConfirmDialog(parent, report + "\nDo you want to continue with the operation?", "Not Enough Space Cleared", OK_CANCEL_OPTION, WARNING_MESSAGE);
                return rc == OK_OPTION;
            }
        } else {
            // Manual cleanup; report the results
            JOptionPane.showMessageDialog(parent, report.toString(), "Clean Up Results", INFORMATION_MESSAGE);
        }

        return true;
    }

    private static Map<FileStore, Set<File>> selectDirsForManual() throws IOException {
        final Configuration config = Configuration.getInstance();
        final Map<FileStore, Set<File>> backupsDirs = new HashMap<>();
        final PlatformManager platformManager = PlatformManager.getInstance();
        platformManager.getAllPlatforms().forEach(platform -> {
            try {
                final File exportDir = config.getExportDirectory(platform);
                if (exportDir != null) {
                    final File backupsDir = platformManager.getPlatformProvider(platform).selectBackupDir(exportDir);
                    if ((backupsDir != null) && backupsDir.isDirectory()) {
                        FileStore backupsDirStore = Files.getFileStore(exportDir.toPath());
                        backupsDirs.computeIfAbsent(backupsDirStore, key -> new HashSet<>()).add(backupsDir);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error getting backups directory file store for platform " + platform, e);
            }
        });
        File backupsDir = new File(System.getProperty("user.home"), "WorldPainter Backups");
        if (backupsDir.isDirectory()) {
            FileStore backupsDirStore = Files.getFileStore(backupsDir.toPath());
            backupsDirs.computeIfAbsent(backupsDirStore, key -> new HashSet<>()).add(backupsDir);
        }
        final File minecraftDir = MinecraftUtil.findMinecraftDir();
        if ((minecraftDir != null) && minecraftDir.isDirectory()) {
            backupsDir = new File(minecraftDir, "backups");
            if (backupsDir.isDirectory()) {
                FileStore backupsDirStore = Files.getFileStore(backupsDir.toPath());
                backupsDirs.computeIfAbsent(backupsDirStore, key -> new HashSet<>()).add(backupsDir);
            }
        }
        return backupsDirs;
    }

    private static Map<FileStore, Set<File>> selectDirsForAutomatic(final File exportDir) throws IOException {
        final FileStore exportDirFileStore = Files.getFileStore(exportDir.toPath());
        final Map<FileStore, Set<File>> backupsDirs = selectDirsForManual();
        // Add the backup dirs specific to the current export dir, in case they are different
        final PlatformManager platformManager = PlatformManager.getInstance();
        platformManager.getAllPlatforms().forEach(platform -> {
            try {
                final File backupsDir = platformManager.getPlatformProvider(platform).selectBackupDir(exportDir);
                if ((backupsDir != null) && backupsDir.isDirectory()) {
                    FileStore backupsDirStore = Files.getFileStore(exportDir.toPath());
                    backupsDirs.computeIfAbsent(backupsDirStore, key -> new HashSet<>()).add(backupsDir);
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error getting backups directory file store for platform " + platform, e);
            }
        });
        // Remove the backup dirs on different file stores than the export dir
        backupsDirs.keySet().removeIf(fileStore -> ! fileStore.equals(exportDirFileStore));
        return backupsDirs;
    }

    /**
     * Delete backups from the specified directories until there is {@code minimumFreeSpace} GB free. If multiple
     * directories are specified, they must lie on the same file system.
     *
     * @return How many backups were deleted.
     */
    private static int doDeleteBackups(Collection<? extends File> backupsDirs, int minimumFreeSpace, FileStore fileStore) throws IOException {
        final java.util.List<File> allBackupDirs = backupsDirs.stream().map(backupsDir -> backupsDir.listFiles(file -> file.isDirectory() && BACKUP_DIR_PATTERN.matcher(file.getName()).matches()))
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .collect(toList());
        int count = 0;
        while ((fileStore.getUsableSpace() < (minimumFreeSpace * GB)) && (! allBackupDirs.isEmpty())) {
            // Try to postpone deleting the last backup for a map as long as possible by deleting the backups for maps
            // which still have multiple backups first

            // Sort the backup dirs by date and group by original name:
            final Map<String, java.util.List<File>> dirsByOriginalName = allBackupDirs.stream()
                    .collect(groupingBy(dir -> dir.getName().substring(0, dir.getName().length() - 15)));
            // Split into a list of backup directories for maps which only have one backup, and those for maps for which
            // there are still multiple backups, and sort both by date:
            final java.util.List<File> dirsWithOneBackup = dirsByOriginalName.values().stream().filter(list -> list.size() == 1).flatMap(Collection::stream).sorted(comparing(dir -> parseDate(dir.getName()))).collect(toList());
            final List<File> dirsWithMultipleBackups = dirsByOriginalName.values().stream().filter(list -> list.size() > 1).flatMap(Collection::stream).sorted(comparing(dir -> parseDate(dir.getName()))).collect(toList());
            final File backupToDelete = dirsWithMultipleBackups.isEmpty() ? dirsWithOneBackup.get(0) : dirsWithMultipleBackups.get(0);
            logger.info("Deleting map backup {} to make space on drive", backupToDelete);
            if (! deleteDir(backupToDelete)) {
                throw new IOException("Could not (fully) delete backup directory " + backupToDelete);
            }
            allBackupDirs.remove(backupToDelete);
            count++;
        }
        return count;
    }

    private static Date parseDate (String name){
        try {
            return DATE_FORMAT.parse(name.substring(name.length() - 14));
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse date in filename \"" + name + '"', e);
        }
    }

    private static final Pattern BACKUP_DIR_PATTERN = Pattern.compile("^.+\\.\\d{14}$");
    private static final Logger logger = LoggerFactory.getLogger(BackupUtils.class);
}