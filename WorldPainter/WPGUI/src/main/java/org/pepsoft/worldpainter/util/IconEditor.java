package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.MouseAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.util.Objects.requireNonNull;
import static org.pepsoft.util.GUIUtils.getUIScale;

/**
 * A simple visual editor component for small images such as icons or patterns. The image set with
 * {@link #setIcon(BufferedImage)} is live-edited.
 */
public class IconEditor extends JComponent {
    public IconEditor() {
        setIcon(new BufferedImage(16, 16, TYPE_INT_ARGB));
        final MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IconEditor.this.mouseClicked(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                IconEditor.this.mouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                firePropertyChange("icon", null, icon);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public BufferedImage getIcon() {
        return icon;
    }

    public void setIcon(BufferedImage icon) {
        requireNonNull(icon);
        iconWidth = icon.getWidth();
        iconHeight = icon.getHeight();
        setMinimumSize(new Dimension(iconWidth * 2 + 1, iconHeight * 2 + 1));
        setPreferredSize(new Dimension(iconWidth * PREFERRED_CELL_SIZE + 1, iconHeight * PREFERRED_CELL_SIZE + 1));
        this.icon = icon;
        repaint();
    }

    public int getPaintColour() {
        return paintColour;
    }

    public void setPaintColour(int argb) {
        paintColour = argb;
    }

    public int getEraseColour() {
        return eraseColour;
    }

    public void setEraseColour(int eraseColour) {
        this.eraseColour = eraseColour;
    }

    public void fill(int argb) {
        for (int x = 0; x < iconWidth; x++) {
            for (int y = 0; y < iconHeight; y++) {
                icon.setRGB(x, y, argb);
            }
        }
        repaint();
        firePropertyChange("icon", null, icon);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        final int width = getWidth(), height = getHeight();
        final int xCellSize = (width - 1) / iconWidth, yCellSize = (height - 1) / iconHeight;
        g2.setColor(getForeground());
        for (int x = 0; x <= iconWidth; x++) {
            g2.drawLine(x * xCellSize, 0, x * xCellSize, height - 1);
        }
        for (int y = 0; y <= iconHeight; y++) {
            g2.drawLine(0, y * yCellSize, width - 1, y * yCellSize);
        }
        for (int x = 0; x < iconWidth; x++) {
            for (int y = 0; y < iconHeight; y++) {
                g2.setColor(new Color(icon.getRGB(x, y), true));
                g2.fillRect(x * xCellSize + 1, y * yCellSize + 1, xCellSize - 1, yCellSize - 1);
            }
        }
    }

    private void mouseClicked(MouseEvent e) {
        setPixel(e.getX(), e.getY(), e.getButton() == BUTTON1);
    }

    private void mouseDragged(MouseEvent e) {
        setPixel(e.getX(), e.getY(), (e.getModifiersEx() & BUTTON1_DOWN_MASK) != 0);
    }

    private void setPixel(int xOnComponent, int yOnComponent, boolean set) {
        final int xCellSize = (getWidth() - 1) / iconWidth, yCellSize = (getHeight() - 1) / iconHeight;
        final int x = xOnComponent / xCellSize;
        final int y = yOnComponent / yCellSize;
        if ((x >= 0) && (x < iconWidth) && (y >= 0) && (y < iconHeight)) {
            icon.setRGB(x, y, set ? paintColour : eraseColour);
            repaint(new Rectangle(x * xCellSize + 1, y * yCellSize + 1, xCellSize - 1, yCellSize - 1));
        }
    }

    private BufferedImage icon;
    private int iconWidth, iconHeight, paintColour = 0xff000000, eraseColour = 0x00ffffff;

    private static final int PREFERRED_CELL_SIZE = Math.round(16 * getUIScale()) + 1;
}