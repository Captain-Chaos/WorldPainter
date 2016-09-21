package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 21-09-16.
 */
public class BiomeAlgorithmListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Integer) {
            switch ((Integer) value) {
                case Constants.BIOME_ALGORITHM_1_1:
                    setText("Minecraft 1.1");
                    break;
                case Constants.BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT:
                    setText("Minecraft 1.6 Default (or 1.2 - 1.5)");
                    break;
                case Constants.BIOME_ALGORITHM_1_3_LARGE:
                    setText("Minecraft 1.6 Large Biomes (or 1.3 - 1.5)");
                    break;
                case Constants.BIOME_ALGORITHM_1_7_DEFAULT:
                    setText("Minecraft 1.10 Default (or 1.7 - 1.9)");
                    break;
                case Constants.BIOME_ALGORITHM_1_7_LARGE:
                    setText("Minecraft 1.10 Large Biomes (or 1.7 - 1.9)");
                    break;
            }
        }
        return this;
    }
}
