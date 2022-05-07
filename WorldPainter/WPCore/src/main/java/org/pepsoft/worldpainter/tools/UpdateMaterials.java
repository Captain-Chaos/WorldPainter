package org.pepsoft.worldpainter.tools;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.CSVDataSource;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.zip.ZipFile.OPEN_READ;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.UNKNOWN_MATERIAL_COLOUR;

public class UpdateMaterials {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        enrichMaterialsFromPluginDump();
    }

    private static void enrichMaterialsWithColours() throws IOException, ClassNotFoundException {
        Configuration config = Configuration.load();
        if (config == null) {
            config = new Configuration();
        }
        Configuration.setInstance(config);
        try (JarFile jarFile = new JarFile(BiomeSchemeManager.getLatestMinecraftJar(), true, OPEN_READ); Reader in = new InputStreamReader(Material.class.getResourceAsStream("mc-materials.csv"), UTF_8); Writer out = new OutputStreamWriter(System.out, UTF_8)) {
            System.out.printf("Using jar: %s%n%n", jarFile.getName());
            CSVDataSource csvIn = new CSVDataSource();
            csvIn.openForReading(in);
            CSVDataSource csvOut = new CSVDataSource();
            csvOut.openForWriting(out, "name", "discriminator", "properties", "opacity", "terrain", "insubstantial", "veryInsubstantial", "resource", "tileEntity", "treeRelated", "vegetation", "blockLight", "natural", "watery", "colour", "colourOrigin");
            do {
                String name = csvIn.getString("name");
                csvOut.setString("name", name);
                String str = csvIn.getString("discriminator");
                if (! isNullOrEmpty(str)) {
                    csvOut.setString("discriminator", str);
                }
                str = csvIn.getString("properties");
                if (! isNullOrEmpty(str)) {
                    csvOut.setString("properties", str);
                }
                csvOut.setInt("opacity", csvIn.getInt("opacity"));
                csvOut.setBoolean("terrain", csvIn.getBoolean("terrain"));
                csvOut.setBoolean("insubstantial", csvIn.getBoolean("insubstantial"));
                csvOut.setBoolean("veryInsubstantial", csvIn.getBoolean("veryInsubstantial"));
                csvOut.setBoolean("resource", csvIn.getBoolean("resource"));
                csvOut.setBoolean("tileEntity", csvIn.getBoolean("tileEntity"));
                csvOut.setBoolean("treeRelated", csvIn.getBoolean("treeRelated"));
                csvOut.setBoolean("vegetation", csvIn.getBoolean("vegetation"));
                csvOut.setInt("blockLight", csvIn.getInt("blockLight"));
                csvOut.setBoolean("natural", csvIn.getBoolean("natural"));
                csvOut.setBoolean("watery", csvIn.getBoolean("watery"));
                if (! isNullOrEmpty(csvIn.getString("colour"))) {
                    csvOut.setString("colour", csvIn.getString("colour"));
                    if (csvIn.getString("colourOrigin") != null) {
                        csvOut.setString("colourOrigin", csvIn.getString("colourOrigin"));
                    }
                } else {
                    Material material = Material.getPrototype(name);
                    if (material == null) {
                        material = Material.get(name);
                    }
                    final ColourAndOrigin colourAndOrigin = determineColour(material, jarFile);
                    if (colourAndOrigin != null) {
                        csvOut.setString("colour", String.format("%8x", colourAndOrigin.colour));
                        csvOut.setString("colourOrigin", colourAndOrigin.origin);
                    }
                }
                csvIn.next();
                csvOut.next();
            } while (! csvIn.isEndOfFile());
        }
    }

    private static void enrichMaterialsFromPluginDump() {
        try (Reader in = new InputStreamReader(Material.class.getResourceAsStream("materials.csv"), UTF_8)) {
            final CSVDataSource csvIn = new CSVDataSource();
            csvIn.openForReading(in);
            do {
                final String name = csvIn.getString("name");
                final String properties = csvIn.getString("properties");
                final boolean tileEntity = csvIn.getBoolean("tileEntity");
                final String tileEntityId = csvIn.getString("tileEntityId");
                final int blockLight = csvIn.getInt("blockLight");

                if (MATERIAL_SPECS.containsKey(name)) {
                    final Set<Map<String, Object>> specs = MATERIAL_SPECS.get(name);
                    specs.forEach(spec -> {
                        if (! isNullOrEmpty(properties)) {
                            spec.put("properties", ImmutableSet.copyOf(properties.split(",")));
                        } else {
                            spec.remove("properties");
                        }
                        spec.put("tileEntity", tileEntity);
                        if (! isNullOrEmpty(tileEntityId)) {
                            spec.put("tileEntityId", tileEntityId);
                        } else {
                            spec.remove("tileEntityId");
                        }
                        spec.put("blockLight", blockLight);
                    });
                } else {
                    final Map<String, Object> spec = new HashMap<>();
                    if (! isNullOrEmpty(properties)) {
                        spec.put("properties", ImmutableSet.copyOf(properties.split(",")));
                    }
                    spec.put("tileEntity", tileEntity);
                    if (! isNullOrEmpty(tileEntityId)) {
                        spec.put("tileEntityId", tileEntityId);
                    }
                    spec.put("blockLight", blockLight);
                    MATERIAL_SPECS.put(name, singleton(spec));
                }

                csvIn.next();
            } while (! csvIn.isEndOfFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Writer out = new OutputStreamWriter(System.out, UTF_8)) {
            final CSVDataSource csvOut = new CSVDataSource();
            csvOut.openForWriting(out, "name", "discriminator", "properties", "opacity", "terrain", "insubstantial", "veryInsubstantial", "resource", "tileEntity", "tileEntityId", "treeRelated", "vegetation", "blockLight", "natural", "watery", "colour", "colourOrigin");
            for (Map.Entry<String, Set<Map<String, Object>>> entry: MATERIAL_SPECS.entrySet()) {
                final String name = entry.getKey();
                for (Map<String, Object> spec: entry.getValue()) {
                    csvOut.setString("name", name);
                    if (spec.containsKey("discriminator")) {
                        csvOut.setString("discriminator", String.join(",", (Set<String>) spec.get("discriminator")));
                    }
                    if (spec.containsKey("properties")) {
                        csvOut.setString("properties", String.join(",", (Set<String>) spec.get("properties")));
                    }
                    csvOut.setInt("opacity", (Integer) spec.getOrDefault("opacity", guessOpacity(name)));
                    csvOut.setBoolean("terrain", (Boolean) spec.getOrDefault("terrain", false));
                    csvOut.setBoolean("insubstantial", (Boolean) spec.getOrDefault("insubstantial", guessInsubstantial(name)));
                    csvOut.setBoolean("veryInsubstantial", (Boolean) spec.getOrDefault("veryInsubstantial", guessInsubstantial(name)));
                    csvOut.setBoolean("resource", (Boolean) spec.getOrDefault("resource", guessResource(name)));
                    csvOut.setBoolean("tileEntity", (Boolean) spec.get("tileEntity"));
                    if (spec.containsKey("tileEntityId")) {
                        csvOut.setString("tileEntityId", (String) spec.get("tileEntityId"));
                    }
                    csvOut.setBoolean("treeRelated", (Boolean) spec.getOrDefault("treeRelated", guessTreeRelated(name)));
                    csvOut.setBoolean("vegetation", (Boolean) spec.getOrDefault("vegetation", guessVegetation(name)));
                    csvOut.setInt("blockLight", (Integer) spec.get("blockLight"));
                    csvOut.setBoolean("natural", (Boolean) spec.getOrDefault("natural", guessNatural(name)));
                    csvOut.setBoolean("watery", (Boolean) spec.getOrDefault("watery", false));
                    if (spec.containsKey("colour")) {
                        csvOut.setString("colour", String.format("%8x", (Integer) spec.get("colour")));
                    }
                    if (spec.containsKey("colourOrigin")) {
                        csvOut.setString("colourOrigin", (String) spec.get("colourOrigin"));
                    }
                    csvOut.next();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean guessInsubstantial(String name) {
        return guessVegetation(name);
    }

    private static boolean guessVegetation(String name) {
        return (! name.endsWith("_block"))
                && (! guessTreeRelated(name))
                && (name.contains("leaf")
                || name.contains("vine")
                || name.contains("fungus")
                || name.contains("roots")
                || name.contains("azalea")
                || name.contains("flowering")
                || name.contains("lichen")
                || name.contains("moss")
                || name.contains("stem")
                || name.contains("blossom"));
    }

    private static boolean guessNatural(String material) {
        return guessVegetation(material) || guessTreeRelated(material);
    }

    public static ColourAndOrigin determineColour(Material material, JarFile jarFile) {
        material = PATCHES.getOrDefault(material.name, material);
        // Preserve the colours people are used to. TODO: some of these were temporary and are probably wrong!
        if (DYNMAP_COLOUR_SCHEME.getColour(material) != UNKNOWN_MATERIAL_COLOUR) {
            return new ColourAndOrigin(DYNMAP_COLOUR_SCHEME.getColour(material), "old default");
        } else if (HARDCODED_COLOURS.containsKey(material.name)) {
            return new ColourAndOrigin(HARDCODED_COLOURS.get(material.name), "hardcoded");
        } else {
            return findAverageColour(jarFile, material);
        }
    }

    private static ColourAndOrigin findAverageColour(JarFile jarFile, Material material) {
        final JarEntry entry = findJarEntry(jarFile, material);
        if (entry != null) {
            int red = 0, green = 0, blue = 0;
            int pixelCount = 0;
            try (InputStream in = jarFile.getInputStream(entry)) {
                BufferedImage image = ImageIO.read(in);
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        final int colour = image.getRGB(x, y);
                        if ((colour & 0xff000000) == 0) {
                            // Ignore transparent pixels
                            continue;
                        }
                        red += (colour & 0xff0000) >> 16;
                        green += (colour & 0x00ff00) >> 8;
                        blue += (colour & 0x0000ff);
                        pixelCount++;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // TODO apply water, leaf, etc. colours!
            return new ColourAndOrigin(0xff000000 | ((red / pixelCount) << 16) | ((green / pixelCount) << 8) | (blue / pixelCount), entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
        } else {
            return null;
        }
    }

    private static JarEntry findJarEntry(JarFile jarFile, Material material) {
        String name = material.simpleName;
        do {
            for (String prefix: PREFIXES) {
                String subName = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
                for (String extension: EXTENSIONS) {
                    JarEntry entry = jarFile.getJarEntry("assets/minecraft/textures/block/" + subName + extension + ".png");
                    if (entry != null) {
                        return entry;
                    }
                }
            }
            name = name.substring(0, max(name.lastIndexOf('_'), 0));
        } while (! name.isEmpty());
        return null;
    }

    private static final String[] EXTENSIONS = {"", "s", "_block", "_top", "_block_top", "_front", "_planks", "_stage7", "_stage3", "_stage2", "_0"}; // Try adding these
    private static final String[] PREFIXES = {"", "infested_", "smooth_"}; // Try removing these
    private static final Map<String, Integer> HARDCODED_COLOURS = ImmutableMap.<String, Integer>builder()
            .put(MC_WATER, 0xff3f76e4)
            .put(MC_FLOWING_WATER, 0xff3f76e4)
            .put(MC_LAVA, 0xfffe432a) // TODO: check
            .put(MC_FLOWING_LAVA, 0xfffe432a) // TODO: check
            .put(MC_BIRCH_LEAVES, 0xff80a755)
            .put(MC_SPRUCE_LEAVES, 0xff619961)
            .put(MC_LILY_PAD, 0xff208030)
            .put(MC_GRASS_BLOCK, 0xff91bd59)
            .put(MC_DIRT_PATH, 0xff91bd59)
            .put(MC_GRASS_PATH, 0xff91bd59)
            .put(MC_GRASS, 0xff91bd59)
            .put(MC_TALL_GRASS, 0xff91bd59)
            .put(MC_FERN, 0xff91bd59)
            .put(MC_LARGE_FERN, 0xff91bd59)
            .put(MC_POTTED_FERN, 0xff91bd59)
            .put(MC_SUGAR_CANE, 0xff91bd59)
            .put(MC_OAK_LEAVES, 0xff77ab2f)
            .put(MC_JUNGLE_LEAVES, 0xff77ab2f)
            .put(MC_ACACIA_LEAVES, 0xff77ab2f)
            .put(MC_DARK_OAK_LEAVES, 0xff77ab2f)
            .put(MC_VINE, 0xff77ab2f)
            .put(MC_MELON_STEM, 0xff77ab2f) // TODO
            .put(MC_PUMPKIN_STEM, 0xff77ab2f) // TODO
            .build();
    private static final Map<String, Material> PATCHES = ImmutableMap.of(MC_GRASS_PATH, DIRT_PATH);
    private static final ColourScheme DYNMAP_COLOUR_SCHEME = new DynMapColourScheme("default", true);

    private static final Map<String, Set<Map<String, Object>>> MATERIAL_SPECS = new HashMap<>();

    static {
        // Read MC materials database
        try (Reader in = new InputStreamReader(requireNonNull(Material.class.getResourceAsStream("mc-materials.csv")), UTF_8)) {
            CSVDataSource csvDataSource = new CSVDataSource();
            csvDataSource.openForReading(in);
            do {
                Map<String, Object> materialSpecs = new HashMap<>();
                String name = csvDataSource.getString("name");
                materialSpecs.put("name", name);
                String str = csvDataSource.getString("discriminator");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("discriminator", ImmutableSet.copyOf(str.split(",")));
                }
                str = csvDataSource.getString("properties");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("properties", ImmutableSet.copyOf(str.split(",")));
                }
                materialSpecs.put("opacity", csvDataSource.getInt("opacity"));
                materialSpecs.put("terrain", csvDataSource.getBoolean("terrain"));
                materialSpecs.put("insubstantial", csvDataSource.getBoolean("insubstantial"));
                materialSpecs.put("veryInsubstantial", csvDataSource.getBoolean("veryInsubstantial"));
                materialSpecs.put("resource", csvDataSource.getBoolean("resource"));
                materialSpecs.put("tileEntity", csvDataSource.getBoolean("tileEntity"));
                materialSpecs.put("treeRelated", csvDataSource.getBoolean("treeRelated"));
                materialSpecs.put("vegetation", csvDataSource.getBoolean("vegetation"));
                materialSpecs.put("blockLight", csvDataSource.getInt("blockLight"));
                materialSpecs.put("natural", csvDataSource.getBoolean("natural"));
                materialSpecs.put("watery", csvDataSource.getBoolean("watery"));
                str = csvDataSource.getString("colour");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("colour", Integer.parseUnsignedInt(str, 16));
                }
                str = csvDataSource.getString("colourOrigin");
                if (! isNullOrEmpty(str)) {
                    materialSpecs.put("colourOrigin", str);
                }
                MATERIAL_SPECS.computeIfAbsent(name, s -> new HashSet<>()).add(materialSpecs);
                csvDataSource.next();
            } while (! csvDataSource.isEndOfFile());
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minecraft materials database materials.csv from classpath", e);
        }
    }

    public static class ColourAndOrigin {
        ColourAndOrigin(int colour, String origin) {
            this.colour = 0xff000000 | colour;
            this.origin = origin;
        }

        public final int colour;
        public final String origin;
    }
}