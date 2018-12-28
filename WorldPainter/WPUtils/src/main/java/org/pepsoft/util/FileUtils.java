/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

/**
 *
 * @author pepijn
 */
public class FileUtils {
    private FileUtils() {
        // Prevent instantiation
    }
    
    public static Checksum getMD5(File file) throws IOException {
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            try (FileInputStream in = new FileInputStream(file)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    md5Digest.update(buffer, 0, read);
                }
            }
            return new Checksum(md5Digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // MD5 is among the minimally required algorithms to be supported by
            // a Java VM
            throw new InternalError("MD5 message digest not supported by Java runtime");
        }
    }
    
    public static String load(File file, Charset charset) throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("File too large (" + length + " bytes)");
        }
        StringBuilder sb = new StringBuilder((int) length);
        try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), charset)) {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }
    
    public static String load(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(new BufferedInputStream(inputStream), charset)) {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }

    /**
     * Recursively copy a directory including all contents.
     * 
     * @param dir The directory to copy. Must exist.
     * @param destDir The directory into which to copy the contents of
     *                <code>dir</code>. Must not exist yet and will be created.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyDir(File dir, File destDir) throws IOException {
        if (! dir.isDirectory()) {
            throw new IllegalArgumentException("Source directory " + dir + " does not exist or is not a directory");
        }
        if (destDir.isDirectory()) {
            throw new IllegalStateException("Destination directory " + destDir + " already exists");
        }
        if (! destDir.mkdirs()) {
            throw new IOException("Could not create " + destDir);
        }
        File[] files = dir.listFiles();
        //noinspection ConstantConditions // Guaranteed by precondition check at start
        for (File file: files) {
            if (file.isDirectory()) {
                copyDir(file, new File(destDir, file.getName()));
            } else if (file.isFile()) {
                copyFileToDir(file, destDir);
            } else {
                logger.warn("Not copying " + file + "; not a regular file or directory");
            }
        }
        destDir.setLastModified(dir.lastModified());
    }

    /**
     * Copy a file to another file.
     *
     * @param file The file to copy.
     * @param destFile The file to copy the file to. If <code>overwrite</code>
     *                 is <code>false</code> it must not exist yet. In either
     *                 case it may not be an existing directory.
     * @param overwrite Whether <code>destFile</code> should be overwritten if
     *                  it already exists. If this is false and the file does
     *                  already exist an {@link IllegalStateException} will be
     *                  thrown.
     * @throws IOException If there is an I/O error while performing the copy.
     * @throws IllegalStateException If <code>overwrite</code> was
     * <code>false</code> and <code>destFile</code> already existed.
     */
    public static void copyFileToFile(File file, File destFile, boolean overwrite) throws IOException {
        if ((! overwrite) && destFile.isFile()) {
            throw new IllegalStateException("Destination file " + destFile + " already exists");
        }
        if (destFile.isDirectory()) {
            throw new IllegalStateException("Destination file is an existing directory");
        }
        StreamUtils.copy(new FileInputStream(file), new FileOutputStream(destFile));
        destFile.setLastModified(file.lastModified());
    }

    /**
     * Copy a file to another directory.
     * 
     * @param file The file to copy.
     * @param destDir The directory to copy the file into, using the same name
     *     as the source file.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyFileToDir(File file, File destDir) throws IOException {
        try {
            copyFileToDir(file, destDir, null);
        } catch (ProgressReceiver.OperationCancelled e) {
            throw new InternalError();
        }
    }
    
    /**
     * Copy a file to another directory with optional progress reporting.
     * 
     * @param file The file to copy.
     * @param destDir The directory to copy the file into, using the same name
     *     as the source file.
     * @param progressReceiver The progress receiver to report copying progress
     *     to. May be <code>null</code>.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyFileToDir(File file, File destDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        File destFile = new File(destDir, file.getName());
        if (destFile.isFile()) {
            throw new IllegalStateException("Destination file " + destFile + " already exists");
        }
        StreamUtils.copy(new FileInputStream(file), new FileOutputStream(destFile), progressReceiver, file.length());
        destFile.setLastModified(file.lastModified());
    }

    /**
     * Recursively delete a directory and all its contents.
     * 
     * @param dir The directory to delete.
     * @return <code>true</code> if and only if the directory is successfully deleted; <code>false</code> otherwise
     */
    public static boolean deleteDir(File dir) {
        if (! dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " does not exist or is not a directory");
        }
        File[] contents = dir.listFiles();
        //noinspection ConstantConditions // Guaranteed by precondition check at start
        for (File file: contents) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        return dir.delete();
    }

    /**
     * Recursively delete all contents of a directory.
     *
     * @param dir The directory to empty.
     * @return <code>true</code> if and only if all contents of the directory
     * were successfully deleted; <code>false</code> otherwise
     */
    public static boolean emptyDir(File dir) {
        if (! dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " does not exist or is not a directory");
        }
        boolean success = true;
        File[] contents = dir.listFiles();
        //noinspection ConstantConditions // Guaranteed by precondition check at start
        for (File file: contents) {
            if (file.isDirectory()) {
                success &= deleteDir(file);
            } else {
                success &= file.delete();
            }
        }
        return success;
    }

    /**
     * Sanitises a filename by replacing characters which are illegal for
     * Windows, Linux or Mac OS filenames with underscores and enforcing other
     * rules.
     *
     * @param filename The filename to sanitise.
     * @return The sanitised filename.
     */
    public static String sanitiseName(@NonNls String filename) {
        StringBuilder sb = new StringBuilder(filename.length());

        // Replace illegal characters for Windows, Linux or Mac OS with
        // underscores
        for (char c: filename.toCharArray()) {
            if ((c < 32) || (ILLEGAL_CHARS.indexOf(c) != -1)) {
                sb.append(REPLACEMENT_CHAR);
            } else {
                sb.append(c);
            }
        }

        // Windows can't cope with filenames which end with spaces or periods
        if ((sb.charAt(sb.length() - 1) == '.') || (sb.charAt(sb.length() - 1) == ' ')) {
            sb.setCharAt(sb.length() - 1, REPLACEMENT_CHAR);
        }

        // Make sure the name doesn't start with a Windows reserved name, by
        // changing the third character to an underscore if necessary
        String uppercaseVersion = sb.toString().toUpperCase();
        for (String reservedName: RESERVED_NAMES) {
            if (uppercaseVersion.startsWith(reservedName)
                    && ((uppercaseVersion.length() == reservedName.length())
                        || (uppercaseVersion.charAt(reservedName.length()) == '.'))) {
                sb.setCharAt(2, REPLACEMENT_CHAR);
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Select a single existing file for loading.
     *
     * @param parent The window relative to which the modal file dialog should
     *               be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir A file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be
     *                   selected.
     * @return The selected file, or <code>null</code> if the user cancelled the
     * dialog.
     */
    public static File selectFileForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        if (SystemUtils.isMac()) {
            // On Macs the AWT file dialog looks much closer to native than the
            // Swing one, so use it
            FileDialog fileDialog;
            if (parent instanceof Frame) {
                fileDialog = new FileDialog((Frame) parent, title, FileDialog.LOAD);
            } else {
                fileDialog = new FileDialog((Dialog) parent, title, FileDialog.LOAD);
            }
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileDialog.setDirectory(fileOrDir.getPath());
                } else {
                    fileDialog.setDirectory(fileOrDir.getParent());
                    fileDialog.setDirectory(fileOrDir.getName());
                }
            }
            if (fileFilter != null) {
                fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
            }
            fileDialog.setVisible(true);
            File[] files = fileDialog.getFiles();
            if (files.length == 1) {
                return files[0];
            } else {
                return null;
            }
        } else {
            JFileChooser fileChooser;
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileChooser = new JFileChooser(fileOrDir);
                } else {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                }
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setDialogTitle(title);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            } else {
                return null;
            }
        }
    }

    /**
     * Select one or more existing files for loading.
     *
     * @param parent The window relative to which the modal file dialog should
     *               be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir A file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be
     *                   selected.
     * @return The selected file(s), or <code>null</code> if the user cancelled
     * the dialog.
     */
    public static File[] selectFilesForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        if (SystemUtils.isMac()) {
            // On Macs the AWT file dialog looks much closer to native than the
            // Swing one, so use it
            FileDialog fileDialog;
            if (parent instanceof Frame) {
                fileDialog = new FileDialog((Frame) parent, title, FileDialog.LOAD);
            } else {
                fileDialog = new FileDialog((Dialog) parent, title, FileDialog.LOAD);
            }
            fileDialog.setMultipleMode(true);
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileDialog.setDirectory(fileOrDir.getPath());
                } else {
                    fileDialog.setDirectory(fileOrDir.getParent());
                    fileDialog.setDirectory(fileOrDir.getName());
                }
            }
            fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
            fileDialog.setVisible(true);
            File[] files = fileDialog.getFiles();
            if (files.length > 0) {
                return files;
            } else {
                return null;
            }
        } else {
            JFileChooser fileChooser;
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileChooser = new JFileChooser(fileOrDir);
                } else {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                }
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setDialogTitle(title);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFiles();
            } else {
                return null;
            }
        }
    }

    /**
     * Select a single filename for saving. May be the name of an existing file,
     * or a non-existent file.
     *
     * @param parent The window relative to which the modal file dialog should
     *               be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir An existing file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be
     *                   selected.
     * @return The selected file, or <code>null</code> if the user cancelled the
     * dialog.
     */
    public static File selectFileForSave(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        if (SystemUtils.isMac()) {
            // On Macs the AWT file dialog looks much closer to native than the
            // Swing one, so use it
            FileDialog fileDialog;
            if (parent instanceof Frame) {
                fileDialog = new FileDialog((Frame) parent, title, FileDialog.SAVE);
            } else {
                fileDialog = new FileDialog((Dialog) parent, title, FileDialog.SAVE);
            }
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileDialog.setDirectory(fileOrDir.getPath());
                } else {
                    fileDialog.setDirectory(fileOrDir.getParent());
                    fileDialog.setFile(fileOrDir.getName());
                }
            }
            fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
            fileDialog.setVisible(true);
            File[] files = fileDialog.getFiles();
            if (files.length == 1) {
                return files[0];
            } else {
                return null;
            }
        } else {
            JFileChooser fileChooser;
            if (fileOrDir != null) {
                if (fileOrDir.isDirectory()) {
                    fileChooser = new JFileChooser(fileOrDir);
                } else {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                }
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setDialogTitle(title);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            } else {
                return null;
            }
        }
    }

    /**
     * Checks if a <code>File</code> is really a <code>java.io.File</code>, and
     * if not converts it to one using {@link File#getAbsolutePath()}.
     *
     * @param file The file to absolutise. May be <code>null</code>.
     * @return A file with the same absolute path as the input and guaranteed to
     *     be of class <code>java.io.File</code>, or <code>null</code> if the input was
     *     <code>null</code>.
     */
    public static File absolutise(File file) {
        return ((file != null) && (file.getClass() != File.class))
            ? new File(file.getAbsolutePath())
            : file;
    }

    /**
     * Ensures that a collection of {@link File}s only contains instances of
     * <code>java.io.File</code> and not subclasses, by converting subclasses
     * using {@link #absolutise(File)}. The collection is transformed in-place
     * if possible; otherwise a new collection with the same basic
     * characteristics is created.
     *
     * @param collection The collection to transform.
     * @param <T> The type of collection.
     * @return Either the same collection, transformed in-place, or a new
     *     collection of the same basic type as the input containing the
     *     transformed results.
     */
    @SuppressWarnings("unchecked") // Guaranteed by code
    public static <T extends Collection<File>> T absolutise(T collection) {
        if (collection == null) {
            return null;
        } else if (collection instanceof List) {
            for (ListIterator<File> i = ((List<File>) collection).listIterator(); i.hasNext(); ) {
                Object object = i.next();
                try {
                    i.set(absolutise((File) object));
                } catch (UnsupportedOperationException e) {
                    Collection<File> newCollection;
                    if (collection instanceof RandomAccess) {
                        newCollection = new ArrayList<>(collection.size());
                    } else {
                        newCollection = new LinkedList<>();
                    }
                    for (Object object2: collection) {
                        newCollection.add(absolutise((File) object2));
                    }
                    return (T) newCollection;
                }
            }
            return collection;
        } else {
            Collection<File> newCollection;
            if (collection instanceof SortedSet) {
                newCollection = new TreeSet<>();
            } else if (collection instanceof Set) {
                newCollection = new HashSet<>(collection.size());
            } else {
                newCollection = new ArrayList<>(collection.size());
            }
            for (Object object: collection) {
                newCollection.add(absolutise((File) object));
            }
            return (T) newCollection;
        }
    }

    /**
     * Ensures that a map with {@link File}s as keys and/or values only contains
     * instances of <code>java.io.File</code> and not subclasses, by converting
     * subclasses using {@link #absolutise(File)}. The map is transformed
     * in-place if possible; otherwise a new map with the same basic
     * characteristics is created.
     *
     * @param map The map to transform.
     * @param <T> The type of map.
     * @return Either the same map, transformed in-place, or a new map of the
     *     same basic type as the input containing the transformed results.
     */
    @SuppressWarnings("unchecked") // Guaranteed by code
    public static <T extends Map<?, ?>> T absolutise(T map) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<Object, Object> entry: ((Map<Object, Object>) map).entrySet()) {
            if ((entry.getKey() instanceof File) && (entry.getKey() != File.class)) {
                // There is a non-File File key in the map; start over and
                // create a new map, since we can't replace keys
                Map<Object, Object> newMap;
                if (map instanceof SortedMap) {
                    newMap = new TreeMap<>();
                } else {
                    newMap = new HashMap<>();
                }
                for (Map.Entry<?, ?> entry2: map.entrySet()) {
                    Object key = entry2.getKey();
                    Object value = entry2.getValue();
                    if (key instanceof File) {
                        key = absolutise((File) key);
                    }
                    if (value instanceof File) {
                        value = absolutise((File) value);
                    }
                    newMap.put(key, value);
                }
                return (T) newMap;
            }
            Object value = entry.getValue();
            if (value instanceof File) {
                entry.setValue(absolutise((File) value));
            }
        }
        return map;
    }

    /**
     * Recursively move a file out of the way by using increasing index numbers,
     * renaming each file to the next number and deleting the last one.
     *
     * @param file        The file to rotate or delete.
     * @param namePattern The pattern to use to create an indexed filename. Must
     *                    contain a <code>"{0}"</code> which will be replaced
     *                    with the index number.
     * @param index       The index of the current file.
     * @param maxIndex    The maximum index which should exist. Beyond this the
     *                    file will be deleted instead of renamed.
     * @throws IOException If a file could not be renamed or deleted.
     */
    public static void rotateFile(File file, String namePattern, int index, int maxIndex) throws IOException {
        if (file.isFile()) {
            // The file exists; move it out of the way by rotating it or
            // deleting it
            if (index >= maxIndex) {
                // We've used the max index, so delete the file
                if (! file.delete()) {
                    throw new IOException("Could not delete file " + file);
                }
            } else {
                // Rotate the file, but first make sure there is room by
                // rotating the next file
                File nextFile = new File(file.getParentFile(), MessageFormat.format(namePattern, index + 1));
                rotateFile(nextFile, namePattern, index + 1, maxIndex);
                // If the next file existed it has now either been renamed or
                // deleted
                if (! file.renameTo(nextFile)) {
                    throw new IOException("Could not rename file " + file + " to " + nextFile);
                }
            }
        }
    }

    /**
     * Asserts that the contents of two files or directories are equal.
     *
     * <p>Files have to have the same length and have the exact same contents.
     *
     * <p>Directories have to contain the same files and directories and are
     * compared recursively.
     *
     * @param expected The file or directory that is expected.
     * @param actual The actual file or directory.
     * @throws AssertionError If the specified files or directories are not
     * equal.
     * @throws IOException If an I/O error occurred while comparing the files or
     * directories.
     */
    public static void assertEquals(File expected, File actual) throws IOException {
        if (expected.isFile()) {
            if (! actual.isFile()) {
                throw new AssertionError(expected + " is a file but " + actual + " is not");
            }
            if (expected.length() != actual.length()) {
                throw new AssertionError("Size of " + expected + " is " + expected.length() + " bytes but size of " + actual + " is " + actual.length() + " bytes");
            }
            try (BufferedInputStream expIn = new BufferedInputStream(new FileInputStream(expected)); BufferedInputStream actIn = new BufferedInputStream(new FileInputStream(actual))) {
                long count = 0;
                int expByte;
                while ((expByte = expIn.read()) != -1) {
                    int actByte = actIn.read();
                    if (expByte != actByte) {
                        throw new AssertionError("Byte " + count + " is " + expByte + " in " + expected + " but " + actByte + " in " + actual);
                    }
                }
            }
        } else if (expected.isDirectory()) {
            File[] expFiles = expected.listFiles();
            File[] actFiles = actual.listFiles();
            if (expFiles.length != actFiles.length) {
                throw new AssertionError(expected + " has " + expFiles.length + " entries but " + actual + " has " + actFiles.length + " entries");
            }
            for (File expFile: expFiles) {
                File actFile = new File(actual, expFile.getName());
                if (! actFile.exists()) {
                    throw new AssertionError(expFile + " does not exist in " + actual);
                }
                assertEquals(expFile, actFile);
            }
        } else {
            throw new IllegalArgumentException("Don't know how to compare type of " + expected);
        }
    }

    /**
     * Calculate the total size of the files in the specified directory,
     * recursively. <em>Note</em> that this method does not take the size of
     * directories into account, nor the block or sector size of the filesystem.
     *
     * @param dir The directory of which to calculate the total size.
     * @return The total size of the files in the specified directory and all
     * its subdirectories, in bytes.
     */
    @SuppressWarnings("ConstantConditions") // Responsibility of caller
    public long getTreeSize(File dir) {
        long totalSize = 0;
        for (File entry: dir.listFiles()) {
            if (entry.isFile()) {
                totalSize += entry.length();
            } else if (entry.isDirectory()) {
                totalSize += getTreeSize(entry);
            }
        }
        return totalSize;
    }

    /**
     * Determine the free space on the file system containing a specific file or
     * directory.
     *
     * @param path The file or directory for which to determine its containing
     *             filesystem's free space.
     * @return The free space on the specified path's filesystem in bytes.
     * @throws IOException If an I/O error occurs while determining the free
     * space.
     */
    public long getFreeSpace(File path) throws IOException {
        return Files.getFileStore(path.toPath()).getUsableSpace();
    }

    public static void main(String[] args) throws IOException {
        Checksum md5 = getMD5(new File(args[0]));
        System.out.print('{');
        byte[] bytes = md5.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print("(byte) ");
            System.out.print(bytes[i]);
        }
        System.out.println('}');

//        Set<FileStore> fileStores = new HashSet<>();
//        for (File root: File.listRoots()) {
//            Path rootPath = root.toPath();
//            FileSystem fileSystem = rootPath.getFileSystem();
//            fileSystem.getFileStores().forEach(fileStores::add);
//        }
//        for (FileStore fileStore: fileStores) {
//            System.out.println(fileStore);
//            System.out.println("Name: " + fileStore.name());
//            System.out.println("Type: " + fileStore.type());
//            System.out.println("Total space: " + fileStore.getTotalSpace());
//            System.out.println("Usable space: " + fileStore.getUsableSpace());
//            System.out.println("Unallocated space: " + fileStore.getUnallocatedSpace());
//            System.out.println("Read only: " + fileStore.isReadOnly());
//        }
    }
    
    private static final int BUFFER_SIZE = 32768;
    private static final String ILLEGAL_CHARS = "<>:\"/\\|?*\t\r\n\b\f";
    private static final Set<String> RESERVED_NAMES = new HashSet<>(
            Arrays.asList("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
                    "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3",
                    "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"));
    private static final char REPLACEMENT_CHAR = '_';
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileUtils.class);
}