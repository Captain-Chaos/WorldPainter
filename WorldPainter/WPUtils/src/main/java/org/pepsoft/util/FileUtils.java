/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

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
     * @param dir The directory to copy.
     * @param destParent The parent directory to copy the directory into using
     *     the same name as the source directory.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyDir(File dir, File destParent) throws IOException {
        File destDir = new File(destParent, dir.getName());
        if (destDir.isDirectory()) {
            throw new IllegalStateException("Destination directory " + destDir + " already exists");
        }
        if (! destDir.mkdirs()) {
            throw new IOException("Could not create " + destDir);
        }
        File[] files = dir.listFiles();
        for (File file: files) {
            if (file.isDirectory()) {
                copyDir(file, destDir);
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
     * <code>false</code> and <code>destDir</code> already existed.
     */
    public static void copyFileToFile(File file, File destFile, boolean overwrite) throws IOException {
        if ((! overwrite) && destFile.isFile()) {
            throw new IllegalStateException("Destination file " + destFile + " already exists");
        }
        if (destFile.isDirectory()) {
            throw new IllegalStateException("Destination file is an existing directory");
        }
        try (FileInputStream in = new FileInputStream(file); FileOutputStream out = new FileOutputStream(destFile)) {
            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
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
        long fileSize = file.length();
        long bytesCopied = 0;
        try (FileInputStream in = new FileInputStream(file); FileOutputStream out = new FileOutputStream(destFile)) {
            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesCopied += bytesRead;
                if ((progressReceiver != null) && (fileSize > 0)) {
                    progressReceiver.setProgress((float) ((double) bytesCopied / fileSize));
                }
            }
        }
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
                } else if (fileOrDir.isFile()) {
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
                } else if (fileOrDir.isFile()) {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                } else {
                    fileChooser = new JFileChooser();
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
                } else if (fileOrDir.isFile()) {
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
                } else if (fileOrDir.isFile()) {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                } else {
                    fileChooser = new JFileChooser();
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
                } else if (fileOrDir.isFile()) {
                    fileDialog.setDirectory(fileOrDir.getParent());
                    fileDialog.setDirectory(fileOrDir.getName());
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
                } else if (fileOrDir.isFile()) {
                    fileChooser = new JFileChooser(fileOrDir.getParentFile());
                    fileChooser.setSelectedFile(fileOrDir);
                } else {
                    fileChooser = new JFileChooser();
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
    
    public static File absolutise(File file) {
        return ((file != null) && (file.getClass() != File.class)) ? file.getAbsoluteFile() : file;
    }
    
    public static <T extends Collection<?>> T absolutise(T collection) {
        if (collection == null) {
            return null;
        } else if (collection instanceof List) {
            for (ListIterator<Object> i = ((java.util.List) collection).listIterator(); i.hasNext(); ) {
                Object object = i.next();
                if (object instanceof File) {
                    i.set(absolutise((File) object));
                }
            }
            return collection;
        } else {
            Collection newCollection;
            if (collection instanceof SortedSet) {
                newCollection = new TreeSet();
            } else if (collection instanceof Set) {
                newCollection = new HashSet(collection.size());
            } else {
                newCollection = new ArrayList(collection.size());
            }
            for (Object object: collection) {
                if (object instanceof File) {
                    newCollection.add(absolutise((File) object));
                } else {
                    newCollection.add(object);
                }
            }
            return (T) newCollection;
        }
    }

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
                    newMap = new HashMap();
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