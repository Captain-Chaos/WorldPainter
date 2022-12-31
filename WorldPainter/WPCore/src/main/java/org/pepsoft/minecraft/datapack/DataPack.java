package org.pepsoft.minecraft.datapack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.util.Collections.unmodifiableMap;
import static org.pepsoft.util.FileUtils.visitFilesRecursively;

public class DataPack {
    public void write(OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            final ObjectWriter writer = OBJECT_MAPPER.writer().withoutFeatures(AUTO_CLOSE_TARGET);
            for (Map.Entry<String, Descriptor> entry: descriptors.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                writer.writeValue(zip, entry.getValue());
            }
        }
    }

    public void addDescriptor(String name, Descriptor descriptor) {
        descriptors.put(name, descriptor);
    }

    public Map<String, Descriptor> getDescriptors() {
        return unmodifiableMap(descriptors);
    }

    public static DataPack load(File dir, String name) throws IOException {
        final DataPack dataPack = new DataPack();
        final File packFile = new File(dir, "datapacks/" + name.substring(5));
        final ObjectReader reader = OBJECT_MAPPER.reader().withoutFeatures(AUTO_CLOSE_SOURCE).withoutFeatures(FAIL_ON_UNKNOWN_PROPERTIES);
        if (packFile.isFile()) {
            try (ZipFile in = new ZipFile(packFile)) {
                in.stream().filter(entry -> ! entry.isDirectory())
                        .forEach(entry -> {
                            final String descName = entry.getName();
                            try {
                                if (descName.equals("pack.mcmeta")) {
                                    dataPack.addDescriptor(descName, reader.readValue(in.getInputStream(entry), Meta.class));
                                } else if (descName.startsWith("data/minecraft/dimension_type")) {
                                    dataPack.addDescriptor(descName, reader.readValue(in.getInputStream(entry), Dimension.class));
                                } else {
                                    logger.trace("Ignoring descriptor {} from data pack {}", descName, name);
                                }
                            } catch (IOException e) {
                                logger.error("{} while reading descriptor {} from data pack {} in map {} (message: {})", e.getClass().getSimpleName(), descName, name, dir, e.getMessage());
                            }
                        });
            }
        } else if (packFile.isDirectory()) {
            visitFilesRecursively(packFile, file -> {
                final String descName = file.getName(), descPath = file.getParent();
                try {
                    if (descName.equals("pack.mcmeta")) {
                        try (InputStream in = new FileInputStream(file)) {
                            dataPack.addDescriptor(descName, reader.readValue(in, Meta.class));
                        }
                    } else if (descPath.contains("data" + File.separatorChar + "minecraft" + File.separatorChar + "dimension_type")) {
                        try (InputStream in = new FileInputStream(file)) {
                            dataPack.addDescriptor(descName, reader.readValue(in, Dimension.class));
                        }
                    } else {
                        logger.trace("Ignoring descriptor {} from data pack {}", descName, name);
                    }
                } catch (IOException e) {
                    logger.error("{} while reading descriptor {} from data pack {} in map {} (message: {})", e.getClass().getSimpleName(), descName, name, dir, e.getMessage());
                }
            });
        } else {
            logger.error("Data pack {} is not a file or a directory; returning empty data pack", packFile);
        }
        return dataPack;
    }

    public static boolean isDataPackFile(File packFile) {
        try (ZipFile in = new ZipFile(packFile)) {
            return in.getEntry("pack.mcmeta") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private final Map<String, Descriptor> descriptors = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DataPack.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(SNAKE_CASE)
            .setSerializationInclusion(NON_NULL);
}