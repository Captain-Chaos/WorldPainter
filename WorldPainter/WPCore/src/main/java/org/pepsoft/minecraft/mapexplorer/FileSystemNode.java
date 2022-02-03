package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.mapexplorer.AbstractNode;
import org.pepsoft.worldpainter.mapexplorer.Node;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * Created by Pepijn on 15-6-2016.
 */
public class FileSystemNode extends AbstractNode {
    public FileSystemNode(File file) {
        this(file, null);
    }

    public FileSystemNode(File file, String displayName) {
        this.file = file;
        this.displayName = (displayName == null) ? FILE_SYSTEM_VIEW.getSystemDisplayName(file) : displayName;
    }

    @Override
    public boolean isLeaf() {
        return ! file.isDirectory();
    }

    @Override
    protected Node[] loadChildren() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public Icon getIcon() {
        return FILE_SYSTEM_VIEW.getSystemIcon(file);
    }

    @Override
    public void doubleClicked() {
        DesktopUtils.open(file);
    }

    public static String getName(File file) {
        return FILE_SYSTEM_VIEW.getSystemDisplayName(file);
    }

    protected final File file;
    private final String displayName;

    private static final FileSystemView FILE_SYSTEM_VIEW = FileSystemView.getFileSystemView();
}