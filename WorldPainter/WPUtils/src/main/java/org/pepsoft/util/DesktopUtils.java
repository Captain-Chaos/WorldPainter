/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pepijn
 */
public final class DesktopUtils {
    private DesktopUtils() {
        // Prevent instantiation
    }
    
    public static boolean open(File file) {
        try {
            try {
                Desktop.getDesktop().open(file);
                return true;
            } catch (IOException e) {
                if (SystemUtils.isLinux()) {
                    ProcessUtils.runInBackground("xdg-open", file.getAbsolutePath());
                    return true;
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

    public static File getDocumentsFolder() {
        File homeDir = new File(System.getProperty("user.home"));
        File potentialDocsDir = new File(homeDir, "Documents");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "My Documents");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Documenten");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Mijn Documenten");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Mes Documents");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Dokumente");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        potentialDocsDir = new File(homeDir, "Meine Dokumente");
        if (potentialDocsDir.isDirectory()) {
            return potentialDocsDir;
        }
        return homeDir;
    }

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

    public static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                // Do nothing
            }
        });
    }
    
    private static final Logger logger = Logger.getLogger(DesktopUtils.class.getName());
}