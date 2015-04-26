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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
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
            FileInputStream in = new FileInputStream(file);
            try {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    md5Digest.update(buffer, 0, read);
                }
            } finally {
                in.close();
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
        InputStreamReader in = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), charset);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }
    
    public static String load(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader in = new InputStreamReader(new BufferedInputStream(inputStream), charset);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }
    
    /**
     * Recursively copy a directory including all contents.
     * 
     * @param dir The directory to copy.
     * @param destParent The parent directory to copy the directory to.
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
                copyFile(file, destDir);
            } else {
                logger.warning("Not copying " + file + "; not a regular file or directory");
            }
        }
        destDir.setLastModified(dir.lastModified());
    }
    
    /**
     * Copy a file to another directory.
     * 
     * @param file The file to copy.
     * @param destDir The parent directory to copy the file to.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyFile(File file, File destDir) throws IOException {
        try {
            copyFile(file, destDir, null);
        } catch (ProgressReceiver.OperationCancelled e) {
            throw new InternalError();
        }
    }
    
    /**
     * Copy a file to another directory with optional progress reporting.
     * 
     * @param file The file to copy.
     * @param destDir The parent directory to copy the file to.
     * @param progressReceiver The progress receiver to report copying progress
     *     to. May be <code>null</code>.
     * @throws IOException If there is an I/O error while performing the copy.
     */
    public static void copyFile(File file, File destDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        File destFile = new File(destDir, file.getName());
        if (destFile.isFile()) {
            throw new IllegalStateException("Destination file " + destFile + " already exists");
        }
        long fileSize = file.length();
        long bytesCopied = 0;
        FileInputStream in = new FileInputStream(file);
        try {
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                int bytesRead;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;
                    if ((progressReceiver != null) && (fileSize > 0)) {
                        progressReceiver.setProgress((float) ((double) bytesCopied / fileSize));
                    }
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        destFile.setLastModified(file.lastModified());
    }

    /**
     * Recursively delete a directory and all its contents.
     * 
     * @param dir The directory to delete.
     */
    public static void deleteDir(File dir) {
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
        dir.delete();
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

    public static File openFile(Frame parent, String title, File dir, final FileFilter fileFilter) {
        if (SystemUtils.isMac()) {
            // On Macs the AWT file dialog looks much closer to native than the
            // Swing one, so use it
            FileDialog fileDialog = new FileDialog(parent, title, FileDialog.LOAD);
            fileDialog.setDirectory(dir.getPath());
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return fileFilter.accept(new File(file, s));
                }
            });
            fileDialog.setVisible(true);
            String selectedFileStr = fileDialog.getFile();
            return (selectedFileStr != null) ? new File(selectedFileStr) : null;
        } else {
            JFileChooser fileChooser = new JFileChooser(dir);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            } else {
                return null;
            }
        }
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
    private static final Set<String> RESERVED_NAMES = new HashSet<String>(
        Arrays.asList("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
        "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3",
        "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"));
    private static final char REPLACEMENT_CHAR = '_';
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());
}