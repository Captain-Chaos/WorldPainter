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
import java.util.logging.Level;
import java.util.logging.Logger;

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
            logger.log(Level.SEVERE, "I/O error while trying to open " + file, e);
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
            // Should covers most Linuxes
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
            logger.log(Level.SEVERE, "URI syntax exception while trying to open " + url, e);
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
                logger.log(Level.SEVERE, "I/O error while trying to open " + url, e);
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

    public static void main(String[] args) throws MalformedURLException {
        copyToClipboard("Testing, testing, one two three testing.");
        open(new URL("http://www.telegraaf.nl/"));
        open(new File("/"));
        System.out.println("Documents folder: " + getDocumentsFolder());
    }

    private static final Logger logger = Logger.getLogger(DesktopUtils.class.getName());
}