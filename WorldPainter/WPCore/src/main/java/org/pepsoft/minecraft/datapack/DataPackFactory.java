package org.pepsoft.minecraft.datapack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.Platform;

import java.text.MessageFormat;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Platform.ATTRIBUTE_DATAPACK_DESCRIPTOR_OVERWORLD;

public class DataPackFactory {
    public static DataPack createWorldPainterDataPack(Platform platform, int minHeight, int maxHeight) {
        final DataPack datapack = new DataPack();
        Meta.Pack.PackBuilder packMetaBuilder = Meta.Pack.builder();
        packMetaBuilder.description("WorldPainter Settings");
        switch (platform.id) {
            case "org.pepsoft.anvil.1.17" -> packMetaBuilder.packFormat(7);
            case "org.pepsoft.anvil.1.21.11" -> {
                packMetaBuilder.minFormat(94.1f);
                packMetaBuilder.maxFormat(94.1f);
            }
            default -> packMetaBuilder.packFormat(9);
        };
        datapack.addDescriptor("pack.mcmeta", Meta.builder().pack(packMetaBuilder.build()).build());
        if (platform.attributes.containsKey(ATTRIBUTE_DATAPACK_DESCRIPTOR_OVERWORLD.key)) {
            final String processedValue = MessageFormat.format(platform.getAttribute(ATTRIBUTE_DATAPACK_DESCRIPTOR_OVERWORLD), maxHeight - minHeight, minHeight);
            try {
                datapack.addDescriptor("data/minecraft/dimension_type/overworld.json", OBJECT_MAPPER.readValue(processedValue, Dimension.class));
            } catch (JsonProcessingException e) {
                throw new MDCCapturingRuntimeException(e.getClass().getSimpleName() + " while unmarshalling dimension descriptor JSON: \"" + processedValue + "\" (message: " + e.getMessage() + ")", e);
            }
        } else {
            datapack.addDescriptor("data/minecraft/dimension_type/overworld.json", Dimension.createDefault(platform, DIM_NORMAL, minHeight, maxHeight));
        }
        return datapack;
    }

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(SNAKE_CASE)
            .setSerializationInclusion(NON_NULL);
}
