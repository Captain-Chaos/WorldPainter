package org.pepsoft.worldpainter.util;

import org.pepsoft.util.SystemUtils;
import org.pepsoft.worldpainter.ExceptionHandler;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
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
     * @param parent The window relative to which the modal file dialog should
     *               be displayed.
     * @param title The text for the title bar of the file dialog.
     * @param fileOrDir A file or directory to preselect.
     * @param fileFilter A filter limiting which files and/or directories can be
     *                   selected.
     * @return The selected file, or {@code null} if the user cancelled the
     * dialog.
     */
    public static File selectFileForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            if (SystemUtils.isMac()) {
                // On Macs the AWT file dialog looks much closer to native than the
                // Swing one, so use it
                FileDialog fileDialog;
                if (parent instanceof Frame) {
                    fileDialog = new FileDialog((Frame) parent, title, LOAD);
                } else {
                    fileDialog = new FileDialog((Dialog) parent, title, LOAD);
                }
                if (fileOrDir != null) {
                    if (fileOrDir.isDirectory()) {
                        fileDialog.setDirectory(fileOrDir.getPath());
                    } else {
                        fileDialog.setDirectory(fileOrDir.getParent());
                        fileDialog.setFile(fileOrDir.getName());
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
                if (ExceptionHandler.doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(parent)) == APPROVE_OPTION) {
                    return fileChooser.getSelectedFile();
                } else {
                    return null;
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
        Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            if ((SystemUtils.isMac()) && (fileView == null)) { // TODO support FileFiew or similar functionality on Mac OS
                // On Macs the AWT file dialog looks much closer to native than the
                // Swing one, so use it
                FileDialog fileDialog;
                if (parent instanceof Frame) {
                    fileDialog = new FileDialog((Frame) parent, title, LOAD);
                } else {
                    fileDialog = new FileDialog((Dialog) parent, title, LOAD);
                }
                if (dir != null) {
                    fileDialog.setDirectory(dir.getPath());
                }
                fileDialog.setFilenameFilter((directory, name) -> new File(directory, name).isDirectory());
                fileDialog.setVisible(true);
                File[] files = fileDialog.getFiles();
                if (files.length == 1) {
                    return files[0];
                } else {
                    return null;
                }
            } else {
                JFileChooser fileChooser;
                if (dir != null) {
                    fileChooser = new JFileChooser(dir);
                } else {
                    fileChooser = new JFileChooser();
                }
                fileChooser.setDialogTitle(title);
                fileChooser.setFileFilter(new FileFilter() {
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
            }
        } finally {
            UIManager.put("FileChooser.readOnly", old);
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
     * @return The selected file(s), or {@code null} if the user cancelled
     * the dialog.
     */
    public static File[] selectFilesForOpen(Window parent, String title, File fileOrDir, final FileFilter fileFilter) {
        Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", TRUE);
        try {
            if (SystemUtils.isMac()) {
                // On Macs the AWT file dialog looks much closer to native than the
                // Swing one, so use it
                FileDialog fileDialog;
                if (parent instanceof Frame) {
                    fileDialog = new FileDialog((Frame) parent, title, LOAD);
                } else {
                    fileDialog = new FileDialog((Dialog) parent, title, LOAD);
                }
                fileDialog.setMultipleMode(true);
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
                if (ExceptionHandler.doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(parent)) == APPROVE_OPTION) {
                    return fileChooser.getSelectedFiles();
                } else {
                    return null;
                }
            }
        } finally {
            UIManager.put("FileChooser.readOnly", old);
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
     * @return The selected file, or {@code null} if the user cancelled the
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
            if (doWithoutExceptionReporting(() -> fileChooser.showSaveDialog(parent)) == APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            } else {
                return null;
            }
        }
    }
}