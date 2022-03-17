package org.pepsoft.worldpainter.colourschemes;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

public class HardcodedColourScheme implements ColourScheme {
    @Override
    public int getColour(int blockType) {
        return Material.get(blockType).colour;
    }

    @Override
    public int getColour(int blockType, int dataValue) {
        return Material.get(blockType, dataValue).colour;
    }

    @Override
    public int getColour(Material material) {
        return material.colour;
    }

//    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        final ColourAndOrigin[] coloursByIndex = new ColourAndOrigin[4096];
//        final SortedMap<String, ColourAndOrigin> coloursByName = new TreeMap<>();
//        Configuration config = Configuration.load();
//        if (config == null) {
//            config = new Configuration();
//        }
//        Configuration.setInstance(config);
//        try (JarFile jarFile = new JarFile(BiomeSchemeManager.getLatestMinecraftJar(), true, OPEN_READ)) {
//            System.out.printf("Using jar: %s%n%n", jarFile.getName());
//
//            // First do all legacy materials
//            for (int i = 0; i < 4096; i++) {
//                Material material = Material.getByCombinedIndex(i);
//                if (! material.namespace.equals(LEGACY)) {
//                    ColourAndOrigin colourAndOrigin = findAverageColour(jarFile, material);
//                    if (colourAndOrigin != null) {
//                        coloursByIndex[material.index] = colourAndOrigin;
//                        if (!coloursByName.containsKey(material.name)) {
//                            coloursByName.put(material.name, colourAndOrigin);
//                        }
//                    }
//                }
//            }
//
//            // Then do all modern materials we know about and haven't encountered yet
//            Material.getAllMaterials().stream()
//                    .filter(material -> ! material.namespace.equals(LEGACY))
//                    .filter(material -> ! coloursByName.containsKey(material.name))
//                    .forEach(material -> {
//                        ColourAndOrigin colourAndOrigin = findAverageColour(jarFile, material);
//                        if (colourAndOrigin != null) {
//                            coloursByName.put(material.name, colourAndOrigin);
//                        }
//                    });
//        }
//
//        final Set<String> processedMaterials = new HashSet<>();
//        for (int i = 0; i < 4096; i++) {
//            final Material material = Material.getByCombinedIndex(i);
//            if (coloursByIndex[i] != null) {
//                processedMaterials.add(material.name);
//                System.out.printf("        COLOURS_BY_INDEX[%4d] = 0x%8x;     // %s (file: %s)%n", i, coloursByIndex[i].colour, material.name, coloursByIndex[i].origin);
//            } else if (! material.namespace.equals(LEGACY)) {
//                System.out.printf("        COLOURS_BY_INDEX[%4d] = UNKNOWN_COLOUR; // %s TODO: colour missing!%n", i, material.name);
//            }
//        }
//
//        System.out.println();
//        coloursByName.entrySet().stream()
//                .filter(entry -> ! processedMaterials.contains(entry.getKey()))
//                .forEach(entry -> System.out.printf("        coloursByName.put(\"%s\", 0x%8x); // file: %s%n", entry.getKey(), entry.getValue().colour, entry.getValue().origin));
//
//        System.out.println();
//        Material.getAllMaterials().stream()
//                .filter(material -> material.index == -1)
//                .map(material -> material.name)
//                .filter(name -> ! coloursByName.containsKey(name))
//                .distinct()
//                .sorted()
//                .forEach(name -> System.out.printf("        // TODO colour missing for modern material %s%n", name));
//    }

//    public static ColourAndOrigin determineColour(Material material, JarFile jarFile) {
//        material = PATCHES.getOrDefault(material.name, material);
//        // Preserve the colours people are used to. TODO: some of these were temporary and are probably wrong!
//        if (DYNMAP_COLOUR_SCHEME.getColour(material) != UNKNOWN_MATERIAL_COLOUR) {
//            return new ColourAndOrigin(DYNMAP_COLOUR_SCHEME.getColour(material), "old default");
//        } else if (HARDCODED_COLOURS.containsKey(material.name)) {
//            return new ColourAndOrigin(HARDCODED_COLOURS.get(material.name), "hardcoded");
//        } else {
//            return findAverageColour(jarFile, material);
//        }
//    }

//    private static ColourAndOrigin findAverageColour(JarFile jarFile, Material material) {
//        final JarEntry entry = findJarEntry(jarFile, material);
//        if (entry != null) {
//            int red = 0, green = 0, blue = 0;
//            int pixelCount = 0;
//            try (InputStream in = jarFile.getInputStream(entry)) {
//                BufferedImage image = ImageIO.read(in);
//                for (int x = 0; x < image.getWidth(); x++) {
//                    for (int y = 0; y < image.getHeight(); y++) {
//                        final int colour = image.getRGB(x, y);
//                        if ((colour & 0xff000000) == 0) {
//                            // Ignore transparent pixels
//                            continue;
//                        }
//                        red += (colour & 0xff0000) >> 16;
//                        green += (colour & 0x00ff00) >> 8;
//                        blue += (colour & 0x0000ff);
//                        pixelCount++;
//                    }
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            // TODO apply water, leaf, etc. colours!
//            return new ColourAndOrigin(0xff000000 | ((red / pixelCount) << 16) | ((green / pixelCount) << 8) | (blue / pixelCount), entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
//        } else {
//            return null;
//        }
//    }

//    private static JarEntry findJarEntry(JarFile jarFile, Material material) {
//        String name = material.simpleName;
//        do {
//            for (String prefix: PREFIXES) {
//                String subName = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
//                for (String extension: EXTENSIONS) {
//                    JarEntry entry = jarFile.getJarEntry("assets/minecraft/textures/block/" + subName + extension + ".png");
//                    if (entry != null) {
//                        return entry;
//                    }
//                }
//            }
//            name = name.substring(0, max(name.lastIndexOf('_'), 0));
//        } while (! name.isEmpty());
//        return null;
//    }

//    private static final String[] EXTENSIONS = {"", "s", "_block", "_top", "_block_top", "_front", "_planks", "_stage7", "_stage3", "_stage2", "_0"}; // Try adding these
//    private static final String[] PREFIXES = {"", "infested_", "smooth_"}; // Try removing these
//    private static final Map<String, Integer> HARDCODED_COLOURS = ImmutableMap.<String, Integer>builder()
//            .put(MC_WATER, 0xff3f76e4)
//            .put(MC_FLOWING_WATER, 0xff3f76e4)
//            .put(MC_LAVA, 0xfffe432a) // TODO: check
//            .put(MC_FLOWING_LAVA, 0xfffe432a) // TODO: check
//            .put(MC_BIRCH_LEAVES, 0xff80a755)
//            .put(MC_SPRUCE_LEAVES, 0xff619961)
//            .put(MC_LILY_PAD, 0xff208030)
//            .put(MC_GRASS_BLOCK, 0xff91bd59)
//            .put(MC_DIRT_PATH, 0xff91bd59)
//            .put(MC_GRASS_PATH, 0xff91bd59)
//            .put(MC_GRASS, 0xff91bd59)
//            .put(MC_TALL_GRASS, 0xff91bd59)
//            .put(MC_FERN, 0xff91bd59)
//            .put(MC_LARGE_FERN, 0xff91bd59)
//            .put(MC_POTTED_FERN, 0xff91bd59)
//            .put(MC_SUGAR_CANE, 0xff91bd59)
//            .put(MC_OAK_LEAVES, 0xff77ab2f)
//            .put(MC_JUNGLE_LEAVES, 0xff77ab2f)
//            .put(MC_ACACIA_LEAVES, 0xff77ab2f)
//            .put(MC_DARK_OAK_LEAVES, 0xff77ab2f)
//            .put(MC_VINE, 0xff77ab2f)
//            .put(MC_MELON_STEM, 0xff77ab2f) // TODO
//            .put(MC_PUMPKIN_STEM, 0xff77ab2f) // TODO
//            .build();
//    private static final Map<String, Material> PATCHES = ImmutableMap.of(MC_GRASS_PATH, DIRT_PATH);
//    private static final ColourScheme DYNMAP_COLOUR_SCHEME = new DynMapColourScheme("default", true);

//    public static class ColourAndOrigin {
//        ColourAndOrigin(int colour, String origin) {
//            this.colour = 0xff000000 | colour;
//            this.origin = origin;
//        }
//
//        public final int colour;
//        public final String origin;
//    }
}