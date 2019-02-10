package org.pepsoft.worldpainter;

import javax.swing.*;

/**
 * A class that provides access to the main {@link JFrame} of WorldPainter. This class
 * exists to avoid having to use the {@link App} class, which has dependencies
 * on artefacts that are not in Maven Central.
 */
public final class MainFrame {
    private MainFrame() {
        // Prevent instantiation
    }

    /**
     * Get the main frame of WorldPainter, if it has been created.
     *
     * @return The main frame of WorldPainter, or {@code null} if it has not yet
     * been created.
     */
    public static JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Get the main frame of WorldPainter, if it has been created.
     *
     * @return The main frame of WorldPainter, or {@code null} if it has not yet
     * been created.
     */
    static void setMainFrame(JFrame mainFrame) {
        MainFrame.mainFrame = mainFrame;
    }

    private static JFrame mainFrame;
}