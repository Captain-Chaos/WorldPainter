/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 *
 * @author pepijn
 */
public class OffsetViewer extends JComponent {
    public OffsetViewer() {
        setMinimumSize(new Dimension(200, 200));
        setPreferredSize(new Dimension(300, 300));
    }
    
    public void rotateLeft() {
        direction = Direction.values()[(direction.ordinal() + 1) % 4];
        repaint();
    }
    
    public void rotateRight() {
        direction = Direction.values()[(direction.ordinal() + 3) % 4];
        repaint();
    }

    public WPObject getObject() {
        return object;
    }

    public void setObject(WPObject object) {
        this.object = object;
        repaint();
    }

    public Point3i getOffset() {
        return offset;
    }

    public void setOffset(Point3i offset) {
        this.offset = offset;
        repaint();
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    public BufferedImage getTexturePack() {
        return texturePack;
    }

    public void setTexturePack(BufferedImage texturePack) {
        this.texturePack = texturePack;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if ((object == null) || (offset == null)) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        int baseLine = getHeight() * 3 / 4;
        int middle = getWidth() / 2;
        Point3i dim = object.getDimensions();
        switch (direction) {
            case X_ASC:
                for (int y = 0; y <= Math.min(-offset.y, dim.y - 1); y++) {
                    for (int x = 0; x < dim.x; x++) {
                        for (int z = 0; z < dim.z; z++) {
                            if (object.getMask(x, y, z)) {
                                paintBlock(g2, baseLine, middle, x + offset.x, z + offset.z, -offset.y - y, object.getMaterial(x, y, z));
                            }
                        }
                    }
                }
                g2.drawImage(IMAGE_X_ASC, middle + 10, getHeight() - IMAGE_X_ASC.getHeight() - 2, null);
                break;
            case Y_ASC:
                for (int x = 0; x <= Math.min(-offset.x, dim.x - 1); x++) {
                    for (int y = 0; y < dim.y; y++) {
                        for (int z = 0; z < dim.z; z++) {
                            if (object.getMask(x, y, z)) {
                                paintBlock(g2, baseLine, middle, y + offset.y, z + offset.z, -offset.x - x, object.getMaterial(x, y, z));
                            }
                        }
                    }
                }
                g2.drawImage(IMAGE_Y_ASC, middle + 10, getHeight() - IMAGE_Y_ASC.getHeight() - 2, null);
                break;
            case X_DESC:
                for (int y = dim.y - 1; y >= Math.max(-offset.y, 0); y--) {
                    for (int x = 0; x < dim.x; x++) {
                        for (int z = 0; z < dim.z; z++) {
                            if (object.getMask(x, y, z)) {
                                paintBlock(g2, baseLine, middle, -x - offset.x, z + offset.z, y - -offset.y, object.getMaterial(x, y, z));
                            }
                        }
                    }
                }
                g2.drawImage(IMAGE_X_DESC, middle - IMAGE_X_DESC.getWidth() - 10, getHeight() - IMAGE_X_DESC.getHeight() - 2, null);
                break;
            case Y_DESC:
                for (int x = dim.x - 1; x >= Math.max(-offset.x, 0); x--) {
                    for (int y = 0; y < dim.y; y++) {
                        for (int z = 0; z < dim.z; z++) {
                            if (object.getMask(x, y, z)) {
                                paintBlock(g2, baseLine, middle, -y - offset.y, z + offset.z, x - -offset.x, object.getMaterial(x, y, z));
                            }
                        }
                    }
                }
                g2.drawImage(IMAGE_Y_DESC, middle - IMAGE_Y_DESC.getWidth() - 10, getHeight() - IMAGE_Y_DESC.getHeight() - 2, null);
                break;
        }
        g2.setColor(Color.BLACK);
        g2.setStroke(STROKE);
        g2.drawRect(middle - 8, baseLine - 8, 16, 16);
        g2.drawLine(middle - 8, baseLine + 8, 0, baseLine + 8);
        g2.drawLine(middle + 7, baseLine + 8, getWidth() - 1, baseLine + 8);
    }

    private void paintBlock(Graphics2D g2, int baseLine, int middle, int x, int y, int depth, Material material) {
        if (material.blockType == Constants.BLK_AIR) {
            return;
        }
        if (texturePack != null) {
            if (depth > 0) {
                RescaleOp rescaleOp = new RescaleOp(new float[] {1.0f, 1.0f, 1.0f, (float) Math.pow(2.0, -depth)}, new float[] {0.0f, 0.0f, 0.0f, 0.0f}, null);
                g2.drawImage(material.getImage(texturePack), rescaleOp, middle + x * 16 - 8, baseLine - y * 16 - 8);
            } else {
                g2.drawImage(material.getImage(texturePack), middle + x * 16 - 8, baseLine - y * 16 - 8, null);
            }
        } else {
            int colour = colourScheme.getColour(material);
            if (depth > 0) {
                colour = ColourUtils.mix(colour, WHITE, (int) (Math.pow(0.5, depth) * 256 + 0.5));
            }
            g2.setColor(new Color(colour));
            g2.fillRect(middle + x * 16 - 8, baseLine - y * 16 - 8, 16, 16);
        }
    }
    
    private BufferedImage texturePack;
    private WPObject object;
    private Point3i offset;
    private ColourScheme colourScheme;
    private Direction direction = Direction.X_ASC;
    
    private static final BufferedImage IMAGE_X_ASC  = loadImage("x_asc.png");
    private static final BufferedImage IMAGE_Y_ASC  = loadImage("z_asc.png");
    private static final BufferedImage IMAGE_X_DESC = loadImage("x_desc.png");
    private static final BufferedImage IMAGE_Y_DESC = loadImage("z_desc.png");
    private static final int WHITE = 0xffffff;
    private static final Stroke STROKE = new BasicStroke(2.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {3.0f, 4.0f}, 0.0f);
    private static final long serialVersionUID = 1L;
    
    private static BufferedImage loadImage(String name) {
        try {
            return ImageIO.read(OffsetViewer.class.getResourceAsStream("/org/pepsoft/worldpainter/resources/" + name));
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading image " + name, e);
        }
    }
    
    enum Direction {X_ASC, Y_ASC, X_DESC, Y_DESC}
}