package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.CSVDataSource;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.StartupMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static org.pepsoft.minecraft.Constants.MC_DIRT_PATH;
import static org.pepsoft.minecraft.Constants.MC_GRASS_PATH;

public class MaterialImporter {
    static void importCustomMaterials(Map<String, Set<Map<String, Object>>> materialSpecs, Map<String, Set<String>> simpleNamesByNamespace) {
        final File customMaterialsDir = new File(Configuration.getConfigDir(), "materials");
        if (customMaterialsDir.isDirectory()) {
            final File[] customSpecFiles = customMaterialsDir.listFiles(pathname -> pathname.isFile() && pathname.getName().toLowerCase().endsWith(".csv"));
            if (customSpecFiles != null) {
                for (File customSpecFile: customSpecFiles) {
                    int count = 0;
                    final Set<String> namespaces = new HashSet<>();
                    try (Reader in = new InputStreamReader(new FileInputStream(customSpecFile), UTF_8)) {
                        CSVDataSource csvDataSource = new CSVDataSource();
                        csvDataSource.openForReading(in);
                        do {
                            Map<String, Object> myMaterialSpecs = new HashMap<>();
                            String name = csvDataSource.getString("name");
                            myMaterialSpecs.put("name", name);
                            String str = csvDataSource.getString("discriminator", null);
                            if (! isNullOrEmpty(str)) {
                                myMaterialSpecs.put("discriminator", ImmutableSet.copyOf(str.split(",")));
                            }
                            str = csvDataSource.getString("properties", null);
                            if (! isNullOrEmpty(str)) {
                                myMaterialSpecs.put("properties", stream(str.split(","))
                                        .map(Material.PropertyDescriptor::fromString)
                                        .collect(toImmutableSortedMap(String::compareTo, d -> d.name, identity())));
                            }
                            myMaterialSpecs.put("opacity", csvDataSource.getInt("opacity", guessOpacity(name)));
                            myMaterialSpecs.put("receivesLight", csvDataSource.getBoolean("receivesLight", guessReceivesLight(name)));
                            myMaterialSpecs.put("terrain", false);
                            final boolean insubstantial = csvDataSource.getBoolean("insubstantial", guessInsubstantial(name));
                            myMaterialSpecs.put("insubstantial", insubstantial);
                            myMaterialSpecs.put("veryInsubstantial", csvDataSource.getBoolean("insubstantial", insubstantial || guessVeryInsubstantial(name)));
                            myMaterialSpecs.put("resource", csvDataSource.getBoolean("resource", guessResource(name)));
                            final boolean tileEntity = csvDataSource.getBoolean("tileEntity", false);
                            myMaterialSpecs.put("tileEntity", tileEntity);
                            if (tileEntity) {
                                myMaterialSpecs.put("tileEntityId", csvDataSource.getString("tileEntityId"));
                            }
                            final boolean treeRelated = csvDataSource.getBoolean("treeRelated", guessTreeRelated(name));
                            myMaterialSpecs.put("treeRelated", treeRelated);
                            final boolean vegetation = csvDataSource.getBoolean("vegetation", (! treeRelated) && guessVegetation(name));
                            myMaterialSpecs.put("vegetation", vegetation);
                            myMaterialSpecs.put("blockLight", csvDataSource.getInt("blockLight", 0));
                            myMaterialSpecs.put("natural", csvDataSource.getBoolean("natural", vegetation || treeRelated || guessNatural(name)));
                            myMaterialSpecs.put("watery", csvDataSource.getBoolean("watery", false));
                            str = csvDataSource.getString("colour", null);
                            if (! isNullOrEmpty(str)) {
                                try {
                                    myMaterialSpecs.put("colour", Integer.parseUnsignedInt(str, 16));
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Not a valid hexadecimal integer value for column colour: \"" + str + "\"", e);
                                }
                            }
                            myMaterialSpecs.put("horizontal_orientation_schemes", csvDataSource.getString("horizontal_orientation_schemes", null));
                            myMaterialSpecs.put("vertical_orientation_scheme", csvDataSource.getString("vertical_orientation_scheme", null));

                            materialSpecs.computeIfAbsent(name, s -> new HashSet<>()).add(myMaterialSpecs);
                            final int p = name.indexOf(':');
                            final String namespace = name.substring(0, p);
                            namespaces.add(namespace);
                            simpleNamesByNamespace.computeIfAbsent(namespace, s -> new HashSet<>()).add(name.substring(p + 1));
                            csvDataSource.next();
                            count++;
                        } while (! csvDataSource.isEndOfFile());
                        logger.info("Loaded {} custom block(s) with namespace(s) {} from {}", count, namespaces, customSpecFile.getName());
                    } catch (RuntimeException | IOException e) {
                        final String message = String.format("%s while reading custom block definition(s) from %s\nMessage: %s", e.getClass().getSimpleName(), customSpecFile.getName(), e.getMessage());
                        logger.error(message, e);
                        StartupMessages.addError(message);
                    }
                }
            }
        }
    }

    public static int guessOpacity(String name) {
        if (name.endsWith("_slab") || name.endsWith("_stairs") || name.contains("block") || name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphea") || name.contains("bricks")) {
            return 15;
        } else if (name.contains("leaves")) {
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean guessResource(String name) {
        return name.contains("ore");
    }

    public static boolean guessTreeRelated(String name) {
        return name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphea") || name.endsWith("_leaves") || name.endsWith("_sapling");
    }

    public static boolean guessVeryInsubstantial(String name) {
        return guessVegetation(name) || name.contains("leaves");
    }

    public static boolean guessInsubstantial(String name) {
        return guessVegetation(name);
    }

    public static boolean guessVegetation(String name) {
        return (! name.endsWith("_block"))
                && (! guessTreeRelated(name))
                && (name.contains("leaf")
                || name.contains("vine")
                || name.contains("fungus")
                || name.contains("roots")
                || name.contains("azalea")
                || name.contains("flowering")
                || name.contains("lichen")
                || name.contains("stem")
                || name.contains("blossom"));
    }

    public static boolean guessNatural(String material) {
        return (guessVegetation(material) || guessTreeRelated(material))
                && (! material.contains("stripped"));
    }

    /**
     * Guess whether a material receives light unto itself, despite being opaque to surrounding blocks.
     */
    public static boolean guessReceivesLight(String name) {
        return NON_TRANSMITTING_TRANSPARENT_BLOCKS.contains(name)
                || name.endsWith("_slab")
                || name.endsWith("_stairs");
    }

    private static final Set<String> NON_TRANSMITTING_TRANSPARENT_BLOCKS = ImmutableSet.of(MC_DIRT_PATH, MC_GRASS_PATH);
    private static final Logger logger = LoggerFactory.getLogger(MaterialImporter.class);
}