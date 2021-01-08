package org.pepsoft.worldpainter.layers;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.biomeschemes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static javax.swing.BoxLayout.PAGE_AXIS;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_15Biomes.*;
import static org.pepsoft.worldpainter.layers.BiomesPanel.BiomeOption.*;

/**
 * Created by pepijn on 27-05-15.
 */
public class BiomesPanel extends JPanel implements CustomBiomeManager.CustomBiomeListener {
    public BiomesPanel(ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Listener listener, ButtonGroup buttonGroup) {
        this.customBiomeManager = customBiomeManager;
        this.listener = listener;
        this.buttonGroup = buttonGroup;
        biomeHelper = new BiomeHelper(colourScheme, customBiomeManager);

        initComponents(colourScheme);

        customBiomeManager.addListener(this);
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
                button.setIcon(IconUtils.createScaledColourIcon(customBiome.getColour()));
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
                    notifyListener();
                }
                grid.remove(component);
                forceRepaint();
                return;
            }
        }
    }

    private void initComponents(ColourScheme colourScheme) {
        setLayout(new BoxLayout(this, PAGE_AXIS));

        label1.setHorizontalTextPosition(JLabel.LEADING);
        label1.setAlignmentX(0.0f);
        add(label1);
        label2.setAlignmentX(0.0f);
        add(label2);

        for (final int biome: BIOME_ORDER) {
            final JToggleButton button = new JToggleButton(new ImageIcon(BiomeSchemeManager.createImage(StaticBiomeInfo.INSTANCE, biome, colourScheme)));
            button.putClientProperty(KEY_BIOME, biome);
            button.setMargin(App.BUTTON_INSETS);
            StringBuilder tooltip = new StringBuilder();
            tooltip.append(StaticBiomeInfo.BIOME_NAMES[biome]);
            tooltip.append(" (");
            List<Integer> variantIds = findVariants(biome);
            boolean first = true;
            for (Integer variantId : variantIds) {
                if (first) {
                    first = false;
                } else {
                    tooltip.append(", ");
                }
                tooltip.append(variantId);
            }
            tooltip.append(')');
            button.setToolTipText(tooltip.toString());
            buttonGroup.add(button);
            button.addActionListener(e -> {
                if (button.isSelected()) {
                    selectBaseBiome(biome);
                }
            });
            grid.add(button);
        }

        JButton addCustomBiomeButton = new JButton(IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/plus.png"));
        addCustomBiomeButton.setMargin(App.BUTTON_INSETS);
        addCustomBiomeButton.setToolTipText("Add a custom biome");
        addCustomBiomeButton.addActionListener(e -> {
            final Window parent = SwingUtilities.getWindowAncestor(BiomesPanel.this);
            final int id = customBiomeManager.getNextId();
            if (id == -1) {
                JOptionPane.showMessageDialog(parent, "Maximum number of custom biomes reached", "Maximum Reached", JOptionPane.ERROR_MESSAGE);
                return;
            }
            CustomBiome customBiome = new CustomBiome("Custom", id, Color.ORANGE.getRGB());
            CustomBiomeDialog dialog = new CustomBiomeDialog(parent, customBiome, true);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                customBiomeManager.addCustomBiome(parent, customBiome);
            }
        });
        grid.add(addCustomBiomeButton);
        grid.setAlignmentX(0.0f);
        add(grid);

        optionsPanel.setLayout(new BoxLayout(optionsPanel, PAGE_AXIS));
        add(optionsPanel);
    }

    private void selectBaseBiome(int biome) {
        selectedBaseBiome = biome;
        selectedBiome = biome;
        notifyListener();
        resetOptions();
        updateLabels();
    }

    private void resetOptions() {
        Set<BiomeOption> availableOptions = findAvailableOptions(selectedBaseBiome);
        optionsPanel.removeAll();
        for (BiomeOption option: availableOptions) {
            JCheckBox checkBox = new JCheckBox(option.name().substring(0, 1) + option.name().substring(1).toLowerCase());
            checkBox.addActionListener(event -> updateOptions());
            checkBox.putClientProperty(PROPERTY_BIOME_OPTION, option);
            checkBox.setEnabled(findBiome(selectedBaseBiome, EnumSet.of(option)) != -1);
            optionsPanel.add(checkBox);
        }
    }

    private void updateOptions() {
        Set<BiomeOption> selectedOptions = getSelectedOptions();
        selectedBiome = findBiome(selectedBaseBiome, selectedOptions);
        notifyListener();
        for (Component component: optionsPanel.getComponents()) {
            JCheckBox checkBox = (JCheckBox) component;
            BiomeOption biomeOption = (BiomeOption) checkBox.getClientProperty(PROPERTY_BIOME_OPTION);
            if (selectedOptions.contains(biomeOption)) {
                checkBox.setEnabled(true);
            } else {
                EnumSet<BiomeOption> optionsCopy = EnumSet.copyOf(selectedOptions);
                optionsCopy.add(biomeOption);
                checkBox.setEnabled(findBiome(selectedBaseBiome, optionsCopy) != -1);
            }
        }
        updateLabels();
    }

    private Set<BiomeOption> getSelectedOptions() {
        Set<BiomeOption> selectedOptions = EnumSet.noneOf(BiomeOption.class);
        for (Component component: optionsPanel.getComponents()) {
            JCheckBox checkBox = (JCheckBox) component;
            if (checkBox.isSelected()) {
                selectedOptions.add((BiomeOption) checkBox.getClientProperty(PROPERTY_BIOME_OPTION));
            }
        }
        return selectedOptions;
    }

    /**
     * Find the actual biome ID for a specific base biome and a set of selected
     * options.
     *
     * @param baseId The base ID of the biome.
     * @param options The selected options.
     * @return The actual biome ID for the specified base biome and options, or
     * -1 if the specified base ID or options are invalid or don't specify an
     * existing actual biome.
     */
    private int findBiome(int baseId, Set<BiomeOption> options) {
        for (BiomeDescriptor descriptor: DESCRIPTORS) {
            if ((descriptor.getBaseId() == baseId) && descriptor.getOptions().equals(options)) {
                return descriptor.getId();
            }
        }
        return -1;
    }

    private void updateLabels() {
        label1.setText("Selected biome: " + selectedBiome);
        label1.setIcon(biomeHelper.getBiomeIcon(selectedBiome));
        label2.setText(biomeHelper.getBiomeName(selectedBiome));
    }

    private void addButton(CustomBiome customBiome) {
        final int biome = customBiome.getId();
        final JToggleButton button = new JToggleButton(IconUtils.createScaledColourIcon(customBiome.getColour()));
        button.putClientProperty(KEY_BIOME, biome);
        button.setMargin(App.BUTTON_INSETS);
        button.setToolTipText(customBiome.getName() + " (" + biome + "); right-click for options");
        button.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e);
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e);
                    }
                }

                private void showPopupMenu(MouseEvent e) {
                    JPopupMenu popup = new JPopupMenu();
                    
                    JMenuItem item = new JMenuItem("Edit...");
                    item.addActionListener(actionEvent -> {
                        CustomBiomeDialog dialog = new CustomBiomeDialog(SwingUtilities.getWindowAncestor(button), customBiome, false);
                        dialog.setVisible(true);
                        if (! dialog.isCancelled()) {
                            customBiomeManager.editCustomBiome(customBiome);
                        }
                    });
                    popup.add(item);
                    
                    item = new JMenuItem("Remove...");
                    item.addActionListener(actionEvent -> {
                        if (JOptionPane.showConfirmDialog(button, "Are you sure you want to remove custom biome \"" + customBiome.getName() + "\" (ID: " + customBiome.getId() + ")?\nAny occurrences will be replaced with Automatic Biomes", "Confirm Removal", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            customBiomeManager.removeCustomBiome(customBiome);
                        }
                    });
                    popup.add(item);
                    
                    popup.show(button, e.getX(), e.getY());
                }
            });
        buttonGroup.add(button);
        button.addActionListener(e -> {
            if (button.isSelected()) {
                selectBaseBiome(biome);
            }
        });
        grid.add(button, grid.getComponentCount() - 1);
        forceRepaint();
    }

    private void forceRepaint() {
        // Not sure why this is necessary. Swing bug?
        Window parent = SwingUtilities.getWindowAncestor(this);
        if (parent != null) {
            parent.validate();
        }
    }

    /**
     * Find the available biome options given a particular base biome.
     *
     * @param baseId The ID of the base biome.
     * @return The total available options for the specified base biome. May be
     * empty, but not {@code null}.
     */
    private static Set<BiomeOption> findAvailableOptions(int baseId) {
        if (StaticBiomeInfo.INSTANCE.isBiomePresent(baseId)) {
            Set<BiomeOption> availableOptions = EnumSet.noneOf(BiomeOption.class);
            for (BiomeDescriptor descriptor: DESCRIPTORS) {
                if (descriptor.getBaseId() == baseId) {
                    availableOptions.addAll(descriptor.getOptions());
                }
            }

            return availableOptions;
        } else {
            return Collections.emptySet();
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
        List<Integer> variants = new ArrayList<>();
        for (BiomeDescriptor descriptor: DESCRIPTORS) {
            if (descriptor.getBaseId() == baseId) {
                variants.add(descriptor.getId());
            }
        }
        return variants;
    }

    private void notifyListener() {
        listener.biomeSelected(selectedBiome);
    }

    private final JPanel grid = new JPanel(new GridLayout(0, 4)), optionsPanel = new JPanel();
    private final ButtonGroup buttonGroup;
    private final JLabel label1 = new JLabel("Selected biome: 1"), label2 = new JLabel("Plains");

    private final CustomBiomeManager customBiomeManager;
    private final BiomeHelper biomeHelper;
    private final Listener listener;
    private int selectedBiome = BIOME_PLAINS, selectedBaseBiome = BIOME_PLAINS;

    private static final int[] BIOME_ORDER = {
        BIOME_PLAINS, BIOME_FOREST, BIOME_SWAMPLAND, BIOME_JUNGLE, BIOME_BAMBOO_JUNGLE,
        BIOME_BIRCH_FOREST, BIOME_ROOFED_FOREST, BIOME_EXTREME_HILLS, BIOME_MUSHROOM_ISLAND,
        BIOME_TAIGA, BIOME_MEGA_TAIGA, BIOME_MEGA_SPRUCE_TAIGA, BIOME_ICE_PLAINS,
        BIOME_DESERT, BIOME_SAVANNA, BIOME_MESA, BIOME_ICE_PLAINS_SPIKES,
        BIOME_OCEAN, BIOME_RIVER, BIOME_BEACH, BIOME_STONE_BEACH,
        BIOME_HELL, BIOME_SKY, BIOME_VOID
    };
    private static final String KEY_BIOME = BiomesPanel.class.getName() + ".biome";
    private static final String PROPERTY_BIOME_OPTION = "org.pepsoft.worldpainter.layers.BiomesPanel.biomeOption";

    private static final BiomeDescriptor[] DESCRIPTORS = {
        new BiomeDescriptor("Ocean", 0, 0),
        new BiomeDescriptor("Plains", 1, 1),
        new BiomeDescriptor("Desert", 2, 2),
        new BiomeDescriptor("Mountains", 3, 3),
        new BiomeDescriptor("Forest", 4, 4),
        new BiomeDescriptor("Taiga", 5, 5),
        new BiomeDescriptor("Swamp", 6, 6),
        new BiomeDescriptor("River", 7, 7),
        new BiomeDescriptor("Nether", 8, 8),
        new BiomeDescriptor("End", 9, 9),

        new BiomeDescriptor("Frozen Ocean", 10, 0, FROZEN),
        new BiomeDescriptor("Frozen River", 11, 7, FROZEN),
        new BiomeDescriptor("Snowy Tundra", 12, 12),
        new BiomeDescriptor("Snowy Mountains", 13, 3, SNOWY),
        new BiomeDescriptor("Mushroom Fields", 14, 14),
        new BiomeDescriptor("Mushroom Fields Shore", 15, 14, SHORE),
        new BiomeDescriptor("Beach", 16, 16),
        new BiomeDescriptor("Desert Hills", 17, 2, HILLS),
        new BiomeDescriptor("Wooded Hills", 18, 4, HILLS),
        new BiomeDescriptor("Taiga Hills", 19, 5, HILLS),

        new BiomeDescriptor("Mountain Edge", 20, 3, EDGE),
        new BiomeDescriptor("Jungle", 21, 21),
        new BiomeDescriptor("Jungle Hills", 22, 21, HILLS),
        new BiomeDescriptor("Jungle Edge", 23, 21, EDGE),
        new BiomeDescriptor("Deep Ocean", 24, 0, DEEP),
        new BiomeDescriptor("Stone Shore", 25, 25),
        new BiomeDescriptor("Snowy Beach", 26, 16, SNOWY),
        new BiomeDescriptor("Birch Forest", 27, 27),
        new BiomeDescriptor("Birch Forest Hills", 28, 27, HILLS),
        new BiomeDescriptor("Dark Forest", 29, 29),

        new BiomeDescriptor("Snowy Taiga", 30, 5, SNOWY),
        new BiomeDescriptor("Snowy Taiga Hills", 31, 5, SNOWY, HILLS),
        new BiomeDescriptor("Giant Tree Taiga", 32, 32),
        new BiomeDescriptor("Giant Tree Taiga Hills", 33, 32, HILLS),
        new BiomeDescriptor("Wooded Mountains", 34, 3, WOODED),
        new BiomeDescriptor("Savanna", 35, 35),
        new BiomeDescriptor("Savanna Plateau", 36, 35, PLATEAU),
        new BiomeDescriptor("Badlands", 37, 37),
        new BiomeDescriptor("Wooded Badlands Plateau", 38, 37, WOODED, PLATEAU),
        new BiomeDescriptor("Badlands Plateau", 39, 37, PLATEAU),

        new BiomeDescriptor("Small End Islands", 40, 9, SMALL_ISLANDS),
        new BiomeDescriptor("End Midlands", 41, 9, MIDLANDS),
        new BiomeDescriptor("End Highlands", 42, 9, HIGHLANDS),
        new BiomeDescriptor("End Barrens", 43, 9, BARRENS),
        new BiomeDescriptor("Warm Ocean", 44, 0, WARM),
        new BiomeDescriptor("Lukewarm Ocean", 45, 0, LUKEWARM),
        new BiomeDescriptor("Cold Ocean", 46, 0, COLD),
        new BiomeDescriptor("Deep Warm Ocean", 47, 0, DEEP, WARM),
        new BiomeDescriptor("Deep Lukewarm Ocean", 48, 0, DEEP, LUKEWARM),
        new BiomeDescriptor("Deep Cold Ocean", 49, 0, DEEP, COLD),

        new BiomeDescriptor("Deep Frozen Ocean", 50, 0, DEEP, FROZEN),

        new BiomeDescriptor("Void", 127, 127),
        new BiomeDescriptor("Sunflower Plains", 129, 1, FLOWERS),

        new BiomeDescriptor("Desert Lakes", 130, 2, LAKES),
        new BiomeDescriptor("Gravelly Mountains", 131, 3, GRAVELLY),
        new BiomeDescriptor("Flower Forest", 132, 4, FLOWERS),
        new BiomeDescriptor("Taiga Mountains", 133, 5, MOUNTAINOUS),
        new BiomeDescriptor("Swamp Hills", 134, 6, HILLS),

        new BiomeDescriptor("Ice Spikes", 140, 140),
        new BiomeDescriptor("Modified Jungle", 149, 21, MODIFIED),

        new BiomeDescriptor("Modified Jungle Edge", 151, 21, MODIFIED, EDGE),
        new BiomeDescriptor("Tall Birch Forest", 155, 27, TALL),
        new BiomeDescriptor("Tall Birch Hills", 156, 27, HILLS, TALL),
        new BiomeDescriptor("Dark Forest Hills", 157, 29, HILLS),
        new BiomeDescriptor("Snowy Taiga Mountains", 158, 5, SNOWY, MOUNTAINOUS),

        new BiomeDescriptor("Giant Spruce Taiga", 160, 160),
        new BiomeDescriptor("Giant Spruce Taiga Hills", 161, 160, HILLS),
        new BiomeDescriptor("Gravelly Mountains+", 162, 3, GRAVELLY, VARIANT),
        new BiomeDescriptor("Shattered Savanna", 163, 35, SHATTERED),
        new BiomeDescriptor("Shattered Savanna Plateau", 164, 35, SHATTERED, PLATEAU),
        new BiomeDescriptor("Eroded Badlands", 165, 37, ERODED),
        new BiomeDescriptor("Modified Wooded Badlands Plateau", 166, 37, MODIFIED, WOODED, PLATEAU),
        new BiomeDescriptor("Modified Badlands Plateau", 167, 37, MODIFIED, PLATEAU),
        new BiomeDescriptor("Bamboo Jungle", 168, 168),
        new BiomeDescriptor("Bamboo Jungle Hills", 169, 168, HILLS),
    };

    public enum BiomeOption {HILLS, SHORE, EDGE, PLATEAU, MOUNTAINOUS, VARIANT, FROZEN, SNOWY, DEEP, WOODED, WARM,
        LUKEWARM, COLD, TALL, FLOWERS, LAKES, GRAVELLY, SHATTERED, SMALL_ISLANDS, MIDLANDS, HIGHLANDS, BARRENS,
        MODIFIED, ERODED}

    public static class BiomeDescriptor {
        public BiomeDescriptor(String name, int id, int baseId, BiomeOption... options) {
            this.name = name;
            this.id = id;
            this.baseId = baseId;
            this.options = ((options != null) && (options.length > 0)) ? EnumSet.copyOf(Arrays.asList(options)) : Collections.emptySet();
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

    public interface Listener {
        void biomeSelected(int biomeId);
    }
}