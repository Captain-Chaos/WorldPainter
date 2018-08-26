/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.pepsoft.util.AwtUtils.doOnEventThread;

/**
 * Utility methods for integrating into desktop environments. These methods do
 * very little error checking and assume they are running on a modern OS with a
 * supported desktop environment. If not, all manner of exceptions may be
 * thrown.
 *
 * @author pepijn
 */
public final class DesktopUtils {
    private DesktopUtils() {
        // Prevent instantiation
    }

    /**
     * Open a file in the associated application, or a directory in the default
     * file manager.
     *
     * @param file The file or directory to open.
     * @return <code>true</code> if the file or directory was successfully
     *     opened.
     */
    public static boolean open(File file) {
        try {
            try {
                Desktop.getDesktop().open(file);
                return true;
            } catch (IOException e) {
                if (SystemUtils.isLinux()) {
                    return ProcessUtils.runAndWait("xdg-open", file.getAbsolutePath()) == 0;
                } else if (SystemUtils.isMac()) {
                    return ProcessUtils.runAndWait("open", file.getAbsolutePath()) == 0;
                } else if (SystemUtils.isWindows()) {
                    return ProcessUtils.runAndWait("start", file.getAbsolutePath()) == 0;
                } else {
                    throw e;
                }
            }
        } catch (IOException e) {
            logger.error("I/O error while trying to open " + file, e);
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    /**
     * Get the default documents folder of the current user.
     *
     * @return The default documents folder of the current user, or the user's
     *     home directory if a documents folder could not be determined.
     */
    public static File getDocumentsFolder() {
        if (XDG.XDG_DOCUMENTS_DIR_FILE != null) {
            // Should cover most Linuxes
            return XDG.XDG_DOCUMENTS_DIR_FILE;
        }
        if (SystemUtils.isWindows()) {
            // Should cover Windows
            return FileSystemView.getFileSystemView().getDefaultDirectory();
        }
        File homeDir = new File(System.getProperty("user.home"));
        File potentialDocsDir = new File(homeDir, "Documents");
        if (potentialDocsDir.isDirectory()) {
            // Should cover Mac OS X, and possibly others we missed
            return potentialDocsDir;
        }
        return homeDir;
    }

    public static File getPicturesFolder() {
        if (XDG.XDG_PICTURES_DIR_FILE != null) {
            // Should cover most Linuxes
            return XDG.XDG_PICTURES_DIR_FILE;
        }
        File homeDir = new File(System.getProperty("user.home"));
        File potentialDocsDir = new File(homeDir, "Pictures");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Photos");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        return getDocumentsFolder();
    }

    /**
     * Get the default images folder of the current user.
     *
     * @return The default images folder of the current user, or the user's
     *     documents folder if an images folder could not be determined.
     */
    public static File getImagesFolder() {
        if (XDG.XDG_PICTURES_DIR_FILE != null) {
            // Should cover most Linuxes
            return XDG.XDG_PICTURES_DIR_FILE;
        }
        File docsDir = getDocumentsFolder();
        File candidate = new File(docsDir.getParentFile(), "Pictures");
        if (candidate.isDirectory() && candidate.canRead()) {
            return candidate;
        }
        candidate = new File(docsDir, "Pictures");
        if (candidate.isDirectory() && candidate.canRead()) {
            return candidate;
        }
        return docsDir;
    }

    /**
     * Open a URL in the default browser.
     *
     * @param url The URL to open.
     * @return <code>true</code> if the URL was successfully opened.
     */
    public static boolean open(URL url) {
        try {
            Desktop.getDesktop().browse(url.toURI());
            return true;
        } catch (URISyntaxException e) {
            logger.error("URI syntax exception while trying to open " + url, e);
            Toolkit.getDefaultToolkit().beep();
            return false;
        } catch (IOException e) {
            if (SystemUtils.isLinux()) {
                return ProcessUtils.runAndWait("xdg-open", url.toExternalForm()) == 0;
            } else if (SystemUtils.isMac()) {
                return ProcessUtils.runAndWait("open", url.toExternalForm()) == 0;
            } else if (SystemUtils.isWindows()) {
                return ProcessUtils.runAndWait("start", url.toExternalForm()) == 0;
            } else {
                logger.error("I/O error while trying to open " + url, e);
                Toolkit.getDefaultToolkit().beep();
                return false;
            }
        }
    }

    /**
     * Copy a string of text to the default system clipboard.
     *
     * @param text The text to copy to the clipboard.
     */
    public static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /**
     * Indicates the progress of an operation in the form of a progress bar on
     * the task bar for supported systems; does nothing on unsupported systems.
     *
     * <p>Once the operation has finished {@link #setProgressDone(Window)} must
     * always be invoked to clear the progress display from the task bar.
     *
     * @param window The window of which to use the task bar icon to display the
     *               progress.
     * @param percentage The progress to display out of 100.
     */
    public static void setProgress(Window window, int percentage) {
        doOnEventThread(() -> ProgressHelper.getInstance().setProgress(window, percentage));
    }

    /**
     * Indicates that progress should no longer be displayed on the task bar for
     * supported systems; does nothing on unsupported systems.
     *
     * @param window The window of which the task bar icon was being used to
     *               display progress.
     */
    public static void setProgressDone(Window window) {
        doOnEventThread(() -> ProgressHelper.getInstance().setProgressDone(window));
    }

    /**
     * Indicates that an operation for which progress was being displayed on the
     * task bar for supported systems has failed, for example by turning the
     * progress bar red; does nothing on unsupported systems.
     *
     * <p>Once the operation has finished {@link #setProgressDone(Window)} must
     * always be invoked to clear the progress error state from the task bar.
     *
     * @param window The window of which the task bar icon was being used to
     *               display progress.
     */
    public static void setProgressError(Window window) {
        doOnEventThread(() -> ProgressHelper.getInstance().setProgressError(window));
    }

    /**
     * Sound a beep, bell or other kind of audible alert, if supported by the
     * desktop system.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public static void main(String[] args) throws MalformedURLException {
        copyToClipboard("Testing, testing, one two three testing.");
        open(new URL("http://www.telegraaf.nl/"));
        File documentsFolder = getDocumentsFolder();
        System.out.println("Documents folder: " + documentsFolder);
        open(documentsFolder);
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DesktopUtils.class);
}