package org.pepsoft.worldpainter.brushes;

import java.awt.*;

import static java.lang.Math.*;
import static org.pepsoft.util.MathUtils.distanceToLineSegment;
import static org.pepsoft.worldpainter.brushes.BrushShape.CUSTOM;

/**
 * An elongated version of another brush, stretched along a line. Only works well for circular brushes with rotational
 * symmetry.
 */
public class LineBrush extends AbstractBrush {
    private LineBrush(Brush brush, int dx, int dy) {
        super(brush.getName());
        this.brush = brush;
        radius = brush.getRadius();
        level = brush.getLevel();
        originalEffectiveRadius = brush.getEffectiveRadius();
        vx = -(dx / 2);
        vy = -(dy / 2);
        wx = -(dx / 2) + dx;
        wy = -(dy / 2) + dy;
        effectiveRadius = max(
                max(abs(dx / 2), abs(dy / 2)),
                max(abs(-(dx / 2) + dx), abs(-(dy / 2) + dy)));
        final Rectangle originalBoundingBox = brush.getBoundingBox();
        boundingBox = new Rectangle(originalBoundingBox.x - abs(dx / 2), originalBoundingBox.y - abs(dy / 2),
                originalBoundingBox.width + abs(dx), originalBoundingBox.height + abs(dy));
    }

    @Override
    public float getStrength(int dx, int dy) {
        final int distanceToLine = (int) round(distanceToLineSegment(dx, dy, vx, vy, wx, wy));
        if (distanceToLine <= originalEffectiveRadius) {
            return brush.getStrength(0, distanceToLine); // TODO support non circular and rotationally symmetric brushes
        } else {
            return 0.0f;
        }
    }

    @Override
    public float getFullStrength(int dx, int dy) {
        final int distanceToLine = (int) round(distanceToLineSegment(dx, dy, vx, vy, wx, wy));
        if (distanceToLine <= originalEffectiveRadius) {
            return brush.getFullStrength(0, distanceToLine); // TODO support non circular and rotationally symmetric brushes
        } else {
            return 0.0f;
        }
    }

    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public int getEffectiveRadius() {
        return effectiveRadius;
    }

    @Override
    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    @Override
    public float getLevel() {
        return level;
    }

    @Override
    public BrushShape getBrushShape() {
        return CUSTOM;
    }

    @Override public AbstractBrush clone() { throw new UnsupportedOperationException(); }
    @Override public void setLevel(float level) { throw new UnsupportedOperationException(); }
    @Override public void setRadius(int radius) { throw new UnsupportedOperationException(); }

    /**
     * Create a new brush that consists of a particular brush elongated into a line. The new brush will be centered on
     * the midpoint of the line.
     *
     * @param brush The brush of which to create an elongated version.
     * @param dx The delta x of the endpoint of the line, with the beginning being at 0.
     * @param dy The delta y of the endpoint of the line, with the beginning being at 0.
     */
    public static LineBrush of(Brush brush, int dx, int dy) {
        // TODO restrict to brushes for which this makes sense (rotiationally symmetric circular ones)?
        return new LineBrush(brush, dx, dy);
    }

    private final Brush brush;
    private final int radius, originalEffectiveRadius, effectiveRadius, vx, vy, wx, wy;
    private final float level;
    private final Rectangle boundingBox;
}
