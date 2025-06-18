package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.exporters.AnnotationsExporter;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.painting.PaintFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created by pepijn on 14-5-15.
 */
public class Text extends AbstractBrushOperation implements PaintOperation {
    public Text(WorldPainterView view) {
        super("Text", "Draw text using any layer or terrain at different sizes, fonts and angles", view, "operation.text");
        hideBrush();
    }

    @Override
    public Paint getPaint() {
        return painter.getPaint();
    }

    @Override
    public void setPaint(Paint paint) {
        if (getBrush() != null) {
            paint.setBrush(getBrush());
        }
        painter.setPaint(paint);
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        if (painter.getPaint() != null) {
            painter.getPaint().setBrush(newBrush);
        }
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        if (painter.getPaint() instanceof PaintFactory.NullPaint) {
            // No paint set yet; do nothing
            return;
        }
        AnnotationsExporter.AnnotationsSettings settings = (AnnotationsExporter.AnnotationsSettings) getDimension().getLayerSettings(Annotations.INSTANCE);
        if (settings == null) {
            settings = new AnnotationsExporter.AnnotationsSettings();
        }
        TextDialog dialog = new TextDialog(SwingUtilities.getWindowAncestor(getView()), settings.getDefaultFont(), settings.getDefaultSize(), savedText);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            Font font = dialog.getSelectedFont();
            settings.setDefaultFont(font.getFamily());
            settings.setDefaultSize(font.getSize());
            dimension.setLayerSettings(Annotations.INSTANCE, settings);
            savedText = dialog.getText();
            if (! savedText.trim().isEmpty()) {
                painter.setFont(font);
                painter.setTextAngle(dialog.getSelectedAngle());
                dimension.setEventsInhibited(true);
                try {
                    painter.drawText(dimension, centreX, centreY, savedText);
                } finally {
                    dimension.setEventsInhibited(false);
                }
            }
        }
    }

    private final DimensionPainter painter = new DimensionPainter();
    private String savedText;

    private static final JPanel OPTIONS_PANEL = new StandardOptionsPanel("Text", "<p>Click to form text with the currently selected paint with its top left corner at the indicated location");
}