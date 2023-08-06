/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.pepsoft.worldpainter.layers.renderers;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Transparency.OPAQUE;

/**
 *
 * @author pepijn
 */
public class RendererPreviewer extends JComponent {
    public RendererPreviewer() {
        setOpaque(true);
    }

    public Color getColour() {
        return colour;
    }

    public void setColour(Color colour) {
        this.colour = colour;
        if (colour != null) {
            pattern = null;
        }
        repaint();
    }

    public BufferedImage getPattern() {
        return pattern;
    }

    public void setPattern(BufferedImage pattern) {
        this.pattern = pattern;
        if (pattern != null) {
            colour = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        if (colour != null) {
            g.setColor(colour);
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        } else if (pattern != null) {
            if (pattern.getTransparency() != OPAQUE) {
                paintBackground(g, clip);
            }
            final int w = pattern.getWidth(), h = pattern.getHeight();
            final int x1 = clip.x / w, x2 = (clip.x + clip.width) / w;
            final int y1 = clip.y / h, y2 = (clip.y + clip.height) / h;
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    g.drawImage(pattern, x * w, y * h, null);
                }
            }
        } else {
            g.clearRect(clip.x, clip.y, clip.width, clip.height);
        }
    }

    private void paintBackground(Graphics g, Rectangle clip) {
        g.clearRect(clip.x, clip.y, clip.width, clip.height);
        final int x1 = clip.x / GRID_SIZE, x2 = (clip.x + clip.width) / GRID_SIZE;
        final int y1 = clip.y / GRID_SIZE, y2 = (clip.y + clip.height) / GRID_SIZE;
        g.setColor(LIGHT_GRAY);
        for (int x = x1; x <= x2; x += 2) {
            for (int y = y1; y <= y2; y += 2) {
                g.fillRect(x * GRID_SIZE, y * GRID_SIZE, GRID_SIZE, GRID_SIZE);
                g.fillRect((x + 1) * GRID_SIZE, (y + 1) * GRID_SIZE, GRID_SIZE, GRID_SIZE);
            }
        }
    }

    private Color colour;
    private BufferedImage pattern;

    private static final int GRID_SIZE = 10;
}