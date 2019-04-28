package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import org.pepsoft.worldpainter.layers.bo2.*;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.CustomObjectProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

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
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public WPObject loadObject(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".bo2")) {
            return Bo2Object.load(file);
        } else if (name.endsWith(".bo3")) {
            return Bo3Object.load(file);
        } else if (name.endsWith(".nbt")) {
            return Structure.load(file);
        } else if (name.endsWith(".schematic")) {
            return Schematic.load(file);
        } else if (name.endsWith(".schem")) {
            return Schem.load(new FileInputStream(file), file.getName().substring(0, name.lastIndexOf('.')));
        } else {
            throw new IllegalArgumentException("Not a supported filename extension: \"" + file.getName() + "\"");
        }
    }

    private static final List<String> SUPPORTED_EXTENSIONS = ImmutableList.of("bo2", "bo3", "schematic", "nbt", "schem");
    private static final List<String> TYPES = ImmutableList.of(Bo2Object.class.getName(), Bo3Object.class.getName(), Structure.class.getName(), Schematic.class.getName(), Schem.class.getName());
}