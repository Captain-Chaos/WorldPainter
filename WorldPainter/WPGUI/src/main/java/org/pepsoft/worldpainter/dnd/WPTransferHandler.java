package org.pepsoft.worldpainter.dnd;

import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
            if (! file.getName().toLowerCase().endsWith(".world")) {
                beepAndShowError(app, "The file is not a WorldPainter .world file.", "No WorldPainter File");
                return false;
            }
            try {
                app.open(file, true);
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
}