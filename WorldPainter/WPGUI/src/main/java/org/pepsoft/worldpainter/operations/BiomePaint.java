/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.Biome;
import static org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme.*;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;
import static org.pepsoft.worldpainter.operations.BiomePaint.BiomeOption.*;

/**
 *
 * @author pepijn
 */
public class BiomePaint extends LayerPaint implements CustomBiomeListener {
    public BiomePaint(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        super(view, radiusControl, mapDragControl, Biome.INSTANCE);
        biomeHelper = new BiomeHelper(BIOME_SCHEME, colourScheme, customBiomeManager);
        optionsPanel = createOptionsPanel(colourScheme);
        this.customBiomeManager = customBiomeManager;
        customBiomeManager.addListener(this);
    }
    
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        addButton(customBiome);
    }

    @Override
    public void customBiomeChanged(CustomBiome customBiome) {
        for (Component component: grid.getComponents()) {
            if ((component instanceof JToggleButton) && (((Integer) ((JToggleButton) component).getClientProperty(KEY_BIOME)) == customBiome.getId())) {
                JToggleButton button = (JToggleButton) component;
                button.setIcon(new ImageIcon(createIcon(customBiome.getColour())));
                button.setToolTipText(customBiome.getName());
                return;
            }
        }
    }

    @Override
    public void customBiomeRemoved(CustomBiome customBiome) {
        for (Component component: grid.getComponents()) {
            if ((component instanceof JToggleButton) && (((Integer) ((JToggleButton) component).getClientProperty(KEY_BIOME)) == customBiome.getId())) {
                JToggleButton button = (JToggleButton) component;
                if (button.isSelected()) {
                    button.setSelected(false);
                    selectedBiome = BIOME_PLAINS;
                }
                grid.remove(component);
                forceRepaint();
                return;
            }
        }
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            int radius = getEffectiveRadius();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                    if ((strength > 0.95f) || (random.nextFloat() < strength)) {
                        if (inverse) {
                            dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
                        } else {
                            dimension.setLayerValueAt(Biome.INSTANCE, x, y, selectedBiome);
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private JPanel createOptionsPanel(ColourScheme colourScheme) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        label1.setHorizontalTextPosition(JLabel.LEADING);
        label1.setAlignmentX(0.0f);
        panel.add(label1);
        label2.setAlignmentX(0.0f);
        panel.add(label2);

        for (int i = 0; i < BIOME_ORDER.length; i++) {
            final int biome = BIOME_ORDER[i];
            if (biome != -1) {
                final JToggleButton button = new JToggleButton(new ImageIcon(BiomeSchemeManager.createImage(BIOME_SCHEME, biome, colourScheme)));
                button.putClientProperty(KEY_BIOME, biome);
                button.setMargin(new Insets(2, 2, 2, 2));
                StringBuilder tooltip = new StringBuilder();
                tooltip.append(AutoBiomeScheme.BIOME_NAMES[biome]);
                tooltip.append(" (");
                List<Integer> variantIds = findVariants(biome);
                boolean first = true;
                for (Integer variantId: variantIds) {
                    if (first) {
                        first = false;
                    } else {
                        tooltip.append(", ");
                    }
                    tooltip.append(variantId);
                }
                tooltip.append(')');
                button.setToolTipText(tooltip.toString());
                if (biome == selectedBaseBiome) {
                    button.setSelected(true);
                }
                buttonGroup.add(button);
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (button.isSelected()) {
                            selectBaseBiome(biome);
                        }
                    }
                });
                grid.add(button);
            } else {
                grid.add(new JLabel());
            }
        }
        
        JButton addCustomBiomeButton = new JButton(IconUtils.loadIcon("org/pepsoft/worldpainter/icons/plus.png"));
        addCustomBiomeButton.setMargin(new Insets(2, 2, 2, 2));
        addCustomBiomeButton.setToolTipText("Add a custom biome");
        addCustomBiomeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                customBiomeManager.addCustomBiome();
            }
        });
        grid.add(addCustomBiomeButton);
        grid.setAlignmentX(0.0f);
        panel.add(grid);
        
        checkBoxHillsShore.setEnabled(false);
        checkBoxEdgePlateau.setEnabled(false);
        checkBoxM.setEnabled(false);
        checkBoxF.setEnabled(false);
        checkBoxVariant.setEnabled(false);
        
        ActionListener optionActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateOptions();
            }
        };
        checkBoxHillsShore.addActionListener(optionActionListener);
        checkBoxEdgePlateau.addActionListener(optionActionListener);
        checkBoxM.addActionListener(optionActionListener);
        checkBoxF.addActionListener(optionActionListener);
        checkBoxVariant.addActionListener(optionActionListener);
        
        panel.add(checkBoxHillsShore);
        checkBoxEdgePlateau.setAlignmentX(0.0f);
        panel.add(checkBoxEdgePlateau);
        JPanel lowerRowPanel = new JPanel();
        lowerRowPanel.setLayout(new BoxLayout(lowerRowPanel, BoxLayout.LINE_AXIS));
        lowerRowPanel.add(checkBoxM);
        lowerRowPanel.add(checkBoxF);
        lowerRowPanel.add(checkBoxVariant);
        lowerRowPanel.setAlignmentX(0.0f);
        panel.add(lowerRowPanel);
        
        updateOptions();
        
        return panel;
    }
    
    private void selectBaseBiome(int biome) {
        selectedBaseBiome = biome;
        selectedBiome = biome;
        resetOptions();
        updateLabels();
    }
    
    private void resetOptions() {
        checkBoxHillsShore.setSelected(false);
        checkBoxEdgePlateau.setSelected(false);
        checkBoxM.setSelected(false);
        checkBoxF.setSelected(false);
        checkBoxVariant.setSelected(false);
        Set<BiomeOption> availableOptions = findAvailableOptions(selectedBaseBiome, null);
        checkBoxHillsShore.setEnabled(availableOptions.contains(HILLS_SHORE));
        checkBoxEdgePlateau.setEnabled(availableOptions.contains(EDGE_PLATEAU));
        checkBoxM.setEnabled(availableOptions.contains(M));
        checkBoxF.setEnabled(availableOptions.contains(F));
        checkBoxVariant.setEnabled(availableOptions.contains(VARIANT));
    }
    
    private void updateOptions() {
        Set<BiomeOption> selectedOptions = getSelectedOptions();
        selectedBiome = findBiome(selectedBaseBiome, selectedOptions);
        Set<BiomeOption> availableOptions = findAvailableOptions(selectedBaseBiome, selectedOptions);
        checkBoxHillsShore.setEnabled(availableOptions.contains(HILLS_SHORE));
        checkBoxEdgePlateau.setEnabled(availableOptions.contains(EDGE_PLATEAU));
        checkBoxM.setEnabled(availableOptions.contains(M));
        checkBoxF.setEnabled(availableOptions.contains(F));
        checkBoxVariant.setEnabled(availableOptions.contains(VARIANT));
        updateLabels();
    }
    
    private Set<BiomeOption> getSelectedOptions() {
        Set<BiomeOption> selectedOptions = EnumSet.noneOf(BiomeOption.class);
        if (checkBoxHillsShore.isSelected()) {
            selectedOptions.add(HILLS_SHORE);
        }
        if (checkBoxEdgePlateau.isSelected()) {
            selectedOptions.add(EDGE_PLATEAU);
        }
        if (checkBoxM.isSelected()) {
            selectedOptions.add(M);
        }
        if (checkBoxF.isSelected()) {
            selectedOptions.add(F);
        }
        if (checkBoxVariant.isSelected()) {
            selectedOptions.add(VARIANT);
        }
        return selectedOptions;
    }
    
    /**
     * Find the actual biome ID for a specific base biome and a set of selected
     * options.
     * 
     * @param baseId The base ID of the biome.
     * @param options The selected options.
     * @return The actual biome ID for the specified base biome and options.
     * @throws IllegalArgumentException If the specified base ID or options are
     *     invalid or don't specify an existing actual biome.
     */
    private int findBiome(int baseId, Set<BiomeOption> options) {
        for (BiomeDescriptor descriptor: DESCRIPTORS) {
            if ((descriptor.getBaseId() == baseId) && descriptor.getOptions().equals(options)) {
                return descriptor.getId();
            }
        }
        throw new IllegalArgumentException("There is no biome with base ID " + baseId + " and options " + options);
    }
    
    private void updateLabels() {
        label1.setText("Selected biome: " + selectedBiome);
        label1.setIcon(biomeHelper.getBiomeIcon(selectedBiome));
        label2.setText(biomeHelper.getBiomeName(selectedBiome));
    }
    
    private void addButton(CustomBiome customBiome) {
        final int biome = customBiome.getId();
        final JToggleButton button = new JToggleButton(new ImageIcon(createIcon(customBiome.getColour())));
        button.putClientProperty(KEY_BIOME, biome);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setToolTipText(customBiome.getName() + " (" + biome + ")");
        buttonGroup.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (button.isSelected()) {
                    selectBaseBiome(biome);
                }
            }
        });
        grid.add(button, grid.getComponentCount() - 1);
        forceRepaint();
    }
    
    private void forceRepaint() {
        // Not sure why this is necessary. Swing bug?
        Window parent = SwingUtilities.getWindowAncestor(optionsPanel);
        if (parent != null) {
            parent.validate();
        }
    }
    
    public static BufferedImage createIcon(int colour) {
        BufferedImage iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((x == 0) || (x == 15) || (y == 0) || (y == 15)) {
                    iconImage.setRGB(x, y, 0);
                } else {
                    iconImage.setRGB(x, y, colour);
                }
            }
        }
        return iconImage;
    }
    
    /**
     * Find the available biome options given a particular base biome and a set
     * of already selected options.
     * 
     * @param baseId The ID of the base biome.
     * @param options The already selected options. May be <code>null</code> or
     *     empty.
     * @return The total available options for the specified base biome and
     *     selected options. May be empty, but not <code>null</code>.
     */
    private static Set<BiomeOption> findAvailableOptions(int baseId, Set<BiomeOption> options) {
        if (BIOME_SCHEME.isBiomePresent(baseId)) {
            Set<BiomeOption> availableOptions = (options != null) ? EnumSet.copyOf(options) : EnumSet.noneOf(BiomeOption.class);
            for (BiomeDescriptor descriptor: DESCRIPTORS) {
                if ((descriptor.getBaseId() == baseId) && ((options == null) || descriptor.getOptions().containsAll(options))) {
                    availableOptions.addAll(descriptor.getOptions());
                }
            }
            
            // Special cases
            if (baseId == BIOME_MESA) {
                if ((options == null) || options.isEmpty()) {
                    // There is no Mesa M, Mesa F or Mesa M F, so if only Mesa is
                    // selected, M and F should not yet be available
                    availableOptions.remove(M);
                    availableOptions.remove(F);
                } else if (options.contains(M) || options.contains(F)) {
                    // On the other hand, once M or F are selected, it should
                    // no longer be possible to deselect "edge/plateau"
                    availableOptions.remove(EDGE_PLATEAU);
                }
            }
            
            return availableOptions;
        } else {
            return Collections.EMPTY_SET;
        }
    }
    
    /**
     * Find the IDs of all variants of the specified base biome.
     * 
     * @param baseId The ID of the base biome.
     * @return The IDs of all variants of the specified base biome (including
     *     the base biome itself).
     */
    private List<Integer> findVariants(int baseId) {
        List<Integer> variants = new ArrayList<Integer>();
        for (BiomeDescriptor descriptor: DESCRIPTORS) {
            if (descriptor.getBaseId() == baseId) {
                variants.add(descriptor.getId());
            }
        }
        return variants;
    }

    private final JPanel optionsPanel, grid = new JPanel(new GridLayout(0, 4));
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final JCheckBox checkBoxHillsShore = new JCheckBox("hills/shore");
    private final JCheckBox checkBoxEdgePlateau = new JCheckBox("edge/plateau");
    private final JCheckBox checkBoxM = new JCheckBox("M");
    private final JCheckBox checkBoxF = new JCheckBox("F");
    private final JCheckBox checkBoxVariant = new JCheckBox("variant");
    private final JLabel label1 = new JLabel("Selected biome: 1"), label2 = new JLabel("Plains");

    private final Random random = new Random();
    private final CustomBiomeManager customBiomeManager;
    private final BiomeHelper biomeHelper;
    private int selectedBiome = BIOME_PLAINS, selectedBaseBiome = BIOME_PLAINS;
    
    private static final AutoBiomeScheme BIOME_SCHEME = new AutoBiomeScheme(null);
    private static final int[] BIOME_ORDER = {
        BIOME_PLAINS, BIOME_FOREST, BIOME_SWAMPLAND, BIOME_JUNGLE,
        BIOME_BIRCH_FOREST, BIOME_ROOFED_FOREST, BIOME_EXTREME_HILLS, BIOME_MUSHROOM_ISLAND,
        BIOME_TAIGA, BIOME_MEGA_TAIGA, BIOME_MEGA_SPRUCE_TAIGA, -1,
        BIOME_DESERT, BIOME_SAVANNA, BIOME_MESA, -1,
        BIOME_OCEAN, BIOME_RIVER, BIOME_BEACH, BIOME_STONE_BEACH,
        BIOME_FROZEN_OCEAN, BIOME_FROZEN_RIVER, BIOME_COLD_BEACH, BIOME_ICE_PLAINS,
        BIOME_ICE_MOUNTAINS, BIOME_COLD_TAIGA, BIOME_HELL, BIOME_SKY
    };
    private static final String KEY_BIOME = BiomePaint.class.getName() + ".biome";
    
    private static final BiomeDescriptor[] DESCRIPTORS = {
        new BiomeDescriptor("Ocean", 0, 0),
        new BiomeDescriptor("Plains", 1, 1),
        new BiomeDescriptor("Desert", 2, 2),
        new BiomeDescriptor("Extreme Hills", 3, 3),
        new BiomeDescriptor("Forest", 4, 4),
        new BiomeDescriptor("Taiga", 5, 5),
        new BiomeDescriptor("Swampland", 6, 6),
        new BiomeDescriptor("River", 7, 7),
        new BiomeDescriptor("Nether", 8, 8),
        new BiomeDescriptor("End", 9, 9),
        new BiomeDescriptor("Frozen Ocean", 10, 10),
        new BiomeDescriptor("Frozen River", 11, 11),
        new BiomeDescriptor("Ice Plains", 12, 12),
        new BiomeDescriptor("Ice Mountains", 13, 13),
        new BiomeDescriptor("Mushroom Island", 14, 14),
        new BiomeDescriptor("Mushroom Island Shore", 15, 14, HILLS_SHORE),
        new BiomeDescriptor("Beach", 16, 16),
        new BiomeDescriptor("Desert Hills", 17, 2, HILLS_SHORE),
        new BiomeDescriptor("Forest Hills", 18, 4, HILLS_SHORE),
        new BiomeDescriptor("Taiga Hills", 19, 5, HILLS_SHORE),
        new BiomeDescriptor("Extreme Hills Edge", 20, 3, EDGE_PLATEAU),
        new BiomeDescriptor("Jungle", 21, 21),
        new BiomeDescriptor("Jungle Hills", 22, 21, HILLS_SHORE),
        new BiomeDescriptor("Jungle Edge", 23, 21, EDGE_PLATEAU),
        new BiomeDescriptor("Deep Ocean", 24, 0, VARIANT),
        new BiomeDescriptor("Stone Beach", 25, 25),
        new BiomeDescriptor("Cold Beach", 26, 26),
        new BiomeDescriptor("Birch Forest", 27, 27),
        new BiomeDescriptor("Birch Forest Hills", 28, 27, HILLS_SHORE),
        new BiomeDescriptor("Roofed Forest", 29, 29),
        new BiomeDescriptor("Cold Taiga", 30, 30),
        new BiomeDescriptor("Cold Taiga Hills", 31, 30, HILLS_SHORE),
        new BiomeDescriptor("Mega Taiga", 32, 32),
        new BiomeDescriptor("Mega Taiga Hills", 33, 32, HILLS_SHORE),
        new BiomeDescriptor("Extreme Hills+", 34, 3, VARIANT),
        new BiomeDescriptor("Savanna", 35, 35),
        new BiomeDescriptor("Savanna Plateau", 36, 35, EDGE_PLATEAU),
        new BiomeDescriptor("Mesa", 37, 37),
        new BiomeDescriptor("Mesa Plateau F", 38, 37, EDGE_PLATEAU, F),
        new BiomeDescriptor("Mesa Plateau", 39, 37, EDGE_PLATEAU),
        new BiomeDescriptor("Sunflower Plains", 129, 1, VARIANT),
        new BiomeDescriptor("Desert M", 130, 2, M),
        new BiomeDescriptor("Extreme Hills M", 131, 3, M),
        new BiomeDescriptor("Flower Forest", 132, 4, VARIANT),
        new BiomeDescriptor("Taiga M", 133, 5, M),
        new BiomeDescriptor("Swampland M", 134, 6, M),
        new BiomeDescriptor("Ice Plains Spikes", 140, 12, VARIANT),
        new BiomeDescriptor("Jungle M", 149, 21, M),
        new BiomeDescriptor("Jungle Edge M", 151, 21, EDGE_PLATEAU, M),
        new BiomeDescriptor("Birch Forest M", 155, 27, M),
        new BiomeDescriptor("Birch Forest Hills M", 156, 27, HILLS_SHORE, M),
        new BiomeDescriptor("Roofed Forest", 157, 157),
        new BiomeDescriptor("Cold Taiga M", 158, 30, M),
        new BiomeDescriptor("Mega Spruce Taiga", 160, 160),
        new BiomeDescriptor("Mega Spruce Taiga Hills", 161, 160, HILLS_SHORE),
        new BiomeDescriptor("Extreme Hills+ M", 162, 3, VARIANT, M),
        new BiomeDescriptor("Savanna M", 163, 35, M),
        new BiomeDescriptor("Savanna Plateau M", 164, 35, EDGE_PLATEAU, M),
        new BiomeDescriptor("Mesa (Bryce)", 165, 37, VARIANT),
        new BiomeDescriptor("Mesa Plateau F M", 166, 37, EDGE_PLATEAU, F, M),
        new BiomeDescriptor("Mesa Plateau M", 167, 37, EDGE_PLATEAU, M),
    };
    
    public enum BiomeOption {HILLS_SHORE, EDGE_PLATEAU, M, F, VARIANT}
    
    public static class BiomeDescriptor {
        public BiomeDescriptor(String name, int id, int baseId, BiomeOption... options) {
            this.name = name;
            this.id = id;
            this.baseId = baseId;
            this.options = ((options != null) && (options.length > 0)) ? EnumSet.copyOf(Arrays.asList(options)) : Collections.EMPTY_SET;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public int getBaseId() {
            return baseId;
        }

        public Set<BiomeOption> getOptions() {
            return options;
        }
        
        private final String name;
        private final int id, baseId;
        private final Set<BiomeOption> options;
    }
}