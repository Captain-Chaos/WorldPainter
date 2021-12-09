package org.pepsoft.minecraft.datapack;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.util.Collections.unmodifiableMap;
import static org.pepsoft.util.ObjectMapperHolder.OBJECT_MAPPER;

public class DataPack {
    public void write(OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            final ObjectWriter writer = new ObjectWriter(OBJECT_MAPPER, OBJECT_MAPPER.getSerializationConfig()
                    .with(SNAKE_CASE)
                    .withoutFeatures(AUTO_CLOSE_TARGET)) { /* We have to make a subclass because there is no public way to change the propertyNamingStrategy */ };

            for (Map.Entry<String, Descriptor> entry: descriptors.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                writer.writeValue(zip, entry.getValue());
            }
        }
    }

    public static DataPack load(File dir, String name) throws IOException {
        DataPack dataPack = new DataPack();
        try (ZipFile in = new ZipFile(new File(dir, "datapacks/" + name.substring(5)))) {
            ObjectReader reader = new ObjectReader(OBJECT_MAPPER, OBJECT_MAPPER.getDeserializationConfig()
                    .with(SNAKE_CASE)
                    .withoutFeatures(AUTO_CLOSE_SOURCE)
                    .withoutFeatures(FAIL_ON_UNKNOWN_PROPERTIES)) { /* We have to make a subclass because there is no public way to change the propertyNamingStrategy. */ };
            in.stream().filter(entry -> ! entry.isDirectory())
                    .forEach(entry -> {
                        final String descName = entry.getName();
                        try {
                            if (descName.equals("pack.mcmeta")) {
                                dataPack.addDescriptor(descName, reader.readValue(in.getInputStream(entry), Meta.class));
                            } else if (descName.startsWith("data/minecraft/dimension_type")) {
                                dataPack.addDescriptor(descName, reader.readValue(in.getInputStream(entry), Dimension.class));
                            } else {
                                logger.debug("Ignoring descriptor {} from data pack {}", descName, name);
                            }
                        } catch (IOException e) {
                            logger.error("{} while reading descriptor {} from data pack {} in map {} (message: {})", e.getClass().getSimpleName(), descName, name, dir, e.getMessage());
                        }
                    });
        }
        return dataPack;
    }

    public void addDescriptor(String name, Descriptor descriptor) {
        descriptors.put(name, descriptor);
    }

    public Map<String, Descriptor> getDescriptors() {
        return unmodifiableMap(descriptors);
    }

    private final Map<String, Descriptor> descriptors = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DataPack.class);
}