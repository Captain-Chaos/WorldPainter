package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.util.IconUtils.createScaledColourIcon;
import static org.pepsoft.util.IconUtils.scaleIcon;
import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.util.BiomeUtils.getBiomeScheme;

public class LayerValueListCellRenderer extends DefaultListCellRenderer {
    public LayerValueListCellRenderer(Layer layer, Platform platform, ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        this(layer, platform, colourScheme, customBiomeManager, null);
    }

    public LayerValueListCellRenderer(Layer layer, Platform platform, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, String nullLabel) {
        this.nullLabel = nullLabel;
        if (layer instanceof Annotations) {
            for (int value = 1; value < 16; value++) {
                icons.put(value, createScaledColourIcon(Annotations.getColour(value, colourScheme)));
                texts.put(value, Annotations.getColourName(value));
            }
            showIcons = true;
        } else if (layer instanceof Biome) {
            final BiomeScheme biomeScheme = getBiomeScheme(platform);
            final boolean addIds = ! platform.capabilities.contains(NAMED_BIOMES);
            for (int value = 0; value < 256; value++) {
                if (biomeScheme.isBiomePresent(value)) {
                    icons.put(value, new ImageIcon(scaleIcon(BiomeSchemeManager.createImage(biomeScheme, value, colourScheme), 16)));
                    texts.put(value, biomeScheme.getBiomeName(value) + (addIds ? (" (" + value + ")") : ""));
                } else {
                    final int finalValue = value;
                    customBiomeManager.getCustomBiomes().stream()
                            .filter(customBiome -> customBiome.getId() == finalValue)
                            .findAny()
                            .ifPresent(customBiome -> {
                                icons.put(finalValue, createScaledColourIcon(customBiome.getColour()));
                                texts.put(finalValue, customBiome.getName() + (addIds ? (" (" + finalValue + ")") : ""));
                            });
                }
            }
            showIcons = true;
        } else {
            showIcons = false;
            final Layer.DataSize dataSize = layer.getDataSize();
            if (dataSize != null) {
                for (int value = 0; value <= dataSize.maxValue; value++) {
                    texts.put(value, dataSize.toString(value));
                }
            } else {
                throw new UnsupportedOperationException("Layer " + layer + " of type " + layer.getClass().getSimpleName() + " with data size " + dataSize + " not supported");
            }
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
            if (showIcons) {
                setIcon(icons.get(value));
            }
            setText(texts.get(value));
        } else if (nullLabel != null) {
            setText(nullLabel);
        }
        return this;
    }

    private final String nullLabel;
    private final boolean showIcons;
    private final Map<Integer, Icon> icons = new HashMap<>();
    private final Map<Integer, String> texts = new HashMap<>();
}