package org.pepsoft.util.swing;

import org.pepsoft.util.DesktopUtils;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JOptionPane.*;

public final class MessageUtils {
    private MessageUtils() {
        // Prevent instantiation
    }

    public static void beepAndShowError(Component parent, String message, String title) {
        DesktopUtils.beep();
        JOptionPane.showMessageDialog(parent, message, title, ERROR_MESSAGE);
    }

    public static void beepAndShowWarning(Component parent, String message, String title) {
        DesktopUtils.beep();
        showWarning(parent, message, title);
    }

    public static void showWarning(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, WARNING_MESSAGE);
    }

    public static void showInfo(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, INFORMATION_MESSAGE);
    }
}