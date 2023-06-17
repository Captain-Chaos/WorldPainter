package org.pepsoft.worldpainter.dnd;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;

@SuppressWarnings("unchecked") // Guaranteed by java.awt.datatransfer package
public class WPTransferHandler extends TransferHandler {
    public WPTransferHandler(App app) {
        this.app = app;
    }

    public boolean canImport(TransferHandler.TransferSupport support) {
        if (! support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }
        if (! support.isDrop()) {
            return false;
        }
        if (! ((COPY & support.getSourceDropActions()) == COPY)) {
            return false;
        }
        support.setDropAction(COPY);
        return true;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if (! canImport(support)) {
            return false;
        }

        try {
            final List<File> list = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (list.size() > 1) {
                beepAndShowError(app, "Please drag only one file at a time.", "Multiple Files");
                return false;
            }
            final File file = list.get(0);
            final String name = file.getName();
            final int p = name.lastIndexOf('.');
            final String extension = (p != -1) ? name.substring(p + 1).toLowerCase() : null;
            if ((! "world".equals(extension)) && (! IMAGE_FILE_EXTENSIONS.contains(extension))) {
                beepAndShowError(app, "The file is not a WorldPainter .world file.", "No WorldPainter File");
                return false;
            }
            try {
                if (IMAGE_FILE_EXTENSIONS.contains(extension)) {
                    final int action = JOptionPane.showOptionDialog(app,
                            "An image file was dropped on WorldPainter.\nSelect how you would like to import it:",
                            "Select Image Import Action",
                            DEFAULT_OPTION,
                            QUESTION_MESSAGE,
                            null,
                            new String[] { "New World", "Height Map", "Mask", "Cancel" },
                            "Cancel");
                    switch (action) {
                        case 0:
                            app.importHeightMap(file);
                            break;
                        case 1:
                            app.importHeightMapIntoCurrentDimension(file);
                            break;
                        case 2:
                            app.importMask(file);
                            break;
                    }
                } else {
                    app.open(file, true);
                }
            } catch (RuntimeException e) {
                ExceptionHandler.handleException(e, app);
                return false;
            }
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            logger.error("{} while obtaining drag and drop transfer data", e.getClass().getSimpleName(), e);
            return false;
        }
    }

    private final App app;

    private static final Logger logger = LoggerFactory.getLogger(WPTransferHandler.class);

    private static final Set<String> IMAGE_FILE_EXTENSIONS = ImmutableSet.copyOf(ImageIO.getReaderFileSuffixes());
}