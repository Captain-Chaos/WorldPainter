package org.pepsoft.minecraft.mapexplorer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * Created by Pepijn on 15-6-2016.
 */
public abstract class FileSystemNode extends Node {
    public FileSystemNode(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return FILE_SYSTEM_VIEW.getSystemDisplayName(file);
    }

    @Override
    public Icon getIcon() {
        return FILE_SYSTEM_VIEW.getSystemIcon(file);
    }

    public static String getName(File file) {
        return FILE_SYSTEM_VIEW.getSystemDisplayName(file);
    }

    protected final File file;

    private static final FileSystemView FILE_SYSTEM_VIEW = FileSystemView.getFileSystemView();
}