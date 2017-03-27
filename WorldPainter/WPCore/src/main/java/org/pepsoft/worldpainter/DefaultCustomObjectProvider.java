package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.bo2.Bo2Object;
import org.pepsoft.worldpainter.layers.bo2.Bo3Object;
import org.pepsoft.worldpainter.layers.bo2.Schematic;
import org.pepsoft.worldpainter.layers.bo2.Structure;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.CustomObjectProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.pepsoft.worldpainter.Constants.BIOME_ALGORITHM_1_7_DEFAULT;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class DefaultCustomObjectProvider extends AbstractPlugin implements CustomObjectProvider {
    public DefaultCustomObjectProvider() {
        super("DefaultCustomObjects", Version.VERSION);
    }

    @Override
    public List<String> getKeys() {
        return TYPES;
    }

    @Override
    public List<String> getSupportedExtensions() {
        // Lazy initialisation to avoid a pause at startup while the biome
        // scheme manager inventories the available biome schemes:
        if (supportedExtensions == null) {
            BiomeScheme biomeScheme = BiomeSchemeManager.getSharedBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT);
            if (biomeScheme instanceof MCInterface) {
                // We can support nbt files
                supportedExtensions = ImmutableList.of("bo2", "bo3", "schematic", "nbt");
            } else {
                // We cannot support nbt files
                supportedExtensions = ImmutableList.of("bo2", "bo3", "schematic");
            }
        }
        return supportedExtensions;
    }

    @Override
    public WPObject loadObject(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".bo2")) {
            return Bo2Object.load(file);
        } else if (name.endsWith(".bo3")) {
            return Bo3Object.load(file);
        } else if (name.endsWith(".nbt")) {
            return Structure.load(file, (MCInterface) BiomeSchemeManager.getSharedBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT));
        } else if (name.endsWith(".schematic")) {
            return Schematic.load(file);
        } else {
            throw new IllegalArgumentException("Not a supported filename extension: \"" + file.getName() + "\"");
        }
    }

    private List<String> supportedExtensions;

    private static final List<String> TYPES = ImmutableList.of(Bo2Object.class.getName(), Bo3Object.class.getName(), Structure.class.getName(), Schematic.class.getName());
}