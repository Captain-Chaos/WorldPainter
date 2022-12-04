package org.pepsoft.worldpainter.util;

import org.pepsoft.util.SystemUtils;
import org.pepsoft.worldpainter.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;

import static java.awt.FileDialog.LOAD;
import static java.lang.Boolean.TRUE;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static org.pepsoft.worldpainter.ExceptionHandler.doWithoutExceptionReporting;

public class FileUtils {
    /**
     * Select a single existing file for loading.
     *
     * @param parent The window relative to which the modal file dialog should be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir A file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be selected.
     * @return The selected file, or {@code null} if the user cancelled the dialog.
     */
    public static File selectFileForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        final Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            if (SystemUtils.isMac()) {
                // On Macs the AWT file dialog looks much closer to native than the Swing one, so use it
                return selectFileForOpenFallback(parent, title, fileOrDir, fileFilter);
            } else {
                try {
                    final JFileChooser fileChooser;
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
                    if (ExceptionHandler.doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(parent)) == APPROVE_OPTION) {
                        return fileChooser.getSelectedFile();
                    } else {
                        return null;
                    }
                } catch (RuntimeException e) {
                    logger.error("{} while using JFileChooser; falling back to FileDialog (message: \"{}\")", e.getClass().getSimpleName(), e.getMessage(), e);
                    return selectFileForOpenFallback(parent, title, fileOrDir, fileFilter);
                }
            }
        } finally {
            UIManager.put("FileChooser.readOnly", old);
        }
    }

    /**
     * Select a single existing directory for loading.
     *
     * @param parent The window relative to which the modal file dialog should be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param dir A directory to preselect.
     * @param description A description of the type of directory to select.
     * @return The selected directory, or {@code null} if the user cancelled the dialog.
     */
    public static File selectDirectoryForOpen(Window parent, String title, File dir, String description, FileView fileView) {
        final Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            // No fallback for Mac or on exceptions because we need the FileView support for Minecraft map selection,
            // and directory selection doesn't work in FileDialog on Windows
            final JFileChooser fileChooser;
            if (dir != null) {
                fileChooser = new JFileChooser(dir);
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setDialogTitle(title);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return description;
                }
            });
            fileChooser.setFileSelectionMode(FILES_AND_DIRECTORIES);
            if (fileView != null) {
                fileChooser.setFileView(fileView);
            }
            if (ExceptionHandler.doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(parent)) == APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            } else {
                return null;
            }
        } finally {
            UIManager.put("FileChooser.readOnly", old);
        }
    }

    /**
     * Select one or more existing files for loading.
     *
     * @param parent The window relative to which the modal file dialog should be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir A file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be selected.
     * @return The selected file(s), or {@code null} if the user cancelled the dialog.
     */
    public static File[] selectFilesForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        final Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            if (SystemUtils.isMac()) {
                // On Macs the AWT file dialog looks much closer to native than the Swing one, so use it
                return selectFilesForOpenFallback(parent, title, fileOrDir, fileFilter);
            } else {
                try {
                    final JFileChooser fileChooser;
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
                    if (ExceptionHandler.doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(parent)) == APPROVE_OPTION) {
                        return fileChooser.getSelectedFiles();
                    } else {
                        return null;
                    }
                } catch (RuntimeException e) {
                    logger.error("{} while using JFileChooser; falling back to FileDialog (message: \"{}\")", e.getClass().getSimpleName(), e.getMessage(), e);
                    return selectFilesForOpenFallback(parent, title, fileOrDir, fileFilter);
                }
            }
        } finally {
            UIManager.put("FileChooser.readOnly", old);
        }
    }

    /**
     * Select a single filename for saving. May be the name of an existing file, or a non-existent file.
     *
     * @param parent The window relative to which the modal file dialog should be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir An existing file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be selected.
     * @return The selected file, or {@code null} if the user cancelled the dialog.
     */
    public static File selectFileForSave(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        if (SystemUtils.isMac()) {
            // On Macs the AWT file dialog looks much closer to native than the Swing one, so use it
            return selectFileForSaveFallback(parent, title, fileOrDir, fileFilter);
        } else {
            try {
                final JFileChooser fileChooser;
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
                if (doWithoutExceptionReporting(() -> fileChooser.showSaveDialog(parent)) == APPROVE_OPTION) {
                    return fileChooser.getSelectedFile();
                } else {
                    return null;
                }
            } catch (RuntimeException e) {
                logger.error("{} while using JFileChooser; falling back to FileDialog (message: \"{}\")", e.getClass().getSimpleName(), e.getMessage(), e);
                return selectFileForSaveFallback(parent, title, fileOrDir, fileFilter);
            }
        }
    }

    private static File selectFileForOpenFallback(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        final FileDialog fileDialog;
        if (parent instanceof Frame) {
            fileDialog = new FileDialog((Frame) parent, title, LOAD);
        } else {
            fileDialog = new FileDialog((Dialog) parent, title, LOAD);
        }
        boolean fileSet = false;
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                fileDialog.setDirectory(fileOrDir.getPath());
            } else {
                fileDialog.setDirectory(fileOrDir.getParent());
                fileDialog.setFile(fileOrDir.getName());
                fileSet = true;
            }
        }
        if (fileFilter != null) {
            if (SystemUtils.isWindows() && (! fileSet)) {
                // Oracle Java does not implement setFilenameFilter() on Windows
                fileDialog.setFile(fileFilter.getExtensions());
            } else {
                fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
            }
        }
        fileDialog.setVisible(true);
        final File[] files = fileDialog.getFiles();
        if (files.length == 1) {
            return files[0];
        } else {
            return null;
        }
    }

    private static File[] selectFilesForOpenFallback(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        final FileDialog fileDialog;
        if (parent instanceof Frame) {
            fileDialog = new FileDialog((Frame) parent, title, LOAD);
        } else {
            fileDialog = new FileDialog((Dialog) parent, title, LOAD);
        }
        fileDialog.setMultipleMode(true);
        boolean fileSet = false;
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                fileDialog.setDirectory(fileOrDir.getPath());
            } else {
                fileDialog.setDirectory(fileOrDir.getParent());
                fileDialog.setFile(fileOrDir.getName());
                fileSet = true;
            }
        }
        if (SystemUtils.isWindows() && (! fileSet)) {
            // Oracle Java does not implement setFilenameFilter() on Windows
            fileDialog.setFile(fileFilter.getExtensions());
        } else {
            fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
        }
        fileDialog.setVisible(true);
        final File[] files = fileDialog.getFiles();
        if (files.length > 0) {
            return files;
        } else {
            return null;
        }
    }

    private static File selectFileForSaveFallback(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        final FileDialog fileDialog;
        if (parent instanceof Frame) {
            fileDialog = new FileDialog((Frame) parent, title, FileDialog.SAVE);
        } else {
            fileDialog = new FileDialog((Dialog) parent, title, FileDialog.SAVE);
        }
        boolean fileSet = false;
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                fileDialog.setDirectory(fileOrDir.getPath());
            } else {
                fileDialog.setDirectory(fileOrDir.getParent());
                fileDialog.setFile(fileOrDir.getName());
                fileSet = true;
            }
        }
        if (SystemUtils.isWindows() && (! fileSet)) {
            // Oracle Java does not implement setFilenameFilter() on Windows
            fileDialog.setFile(fileFilter.getExtensions());
        } else {
            fileDialog.setFilenameFilter((file, s) -> fileFilter.accept(new File(file, s)));
        }
        fileDialog.setVisible(true);
        final File[] files = fileDialog.getFiles();
        if (files.length == 1) {
            return files[0];
        } else {
            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
}