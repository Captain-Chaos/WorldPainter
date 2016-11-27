/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.bridj.jawt.JAWTUtils;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

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

    public static void main(String[] args) throws MalformedURLException {
        copyToClipboard("Testing, testing, one two three testing.");
        open(new URL("http://www.telegraaf.nl/"));
        File documentsFolder = getDocumentsFolder();
        System.out.println("Documents folder: " + documentsFolder);
        open(documentsFolder);
    }

    public static void setProgress(Window window, int percentage) {
        doOnEventThread(() -> {
            ProgressHelper progressHelper = progressHelpers.get(window);
            if (progressHelper == null) {
                progressHelper = new ProgressHelper(window);
                progressHelpers.put(window, progressHelper);
                progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NORMAL);
            }
            if (! progressHelper.errorReported) {
                progressHelper.taskbarList.SetProgressValue(progressHelper.hwnd, percentage, 100);
            }
        });
    }

    public static void setProgressDone(Window window) {
        doOnEventThread(() -> {
            ProgressHelper progressHelper = progressHelpers.get(window);
            if (progressHelper != null) {
                progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
                progressHelpers.remove(window);
            }
        });
    }

    public static void setProgressError(Window window) {
        doOnEventThread(() -> {
            ProgressHelper progressHelper = progressHelpers.get(window);
            if (progressHelper != null) {
                progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_ERROR);
                progressHelper.errorReported = true;
            }
        });
    }

    private static final Map<Window, ProgressHelper> progressHelpers = new WeakHashMap<>();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DesktopUtils.class);

    static class ProgressHelper {
        @SuppressWarnings("unchecked") // Guaranteed by BridJ
        ProgressHelper(Window window) {
            try {
                taskbarList = COMRuntime.newInstance(ITaskbarList3.class);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            long hwndVal = JAWTUtils.getNativePeerHandle(window);
            hwnd = (Pointer<Integer>) Pointer.pointerToAddress(hwndVal);
        }

        final ITaskbarList3 taskbarList;
        final Pointer<Integer> hwnd;
        boolean errorReported;
    }
}