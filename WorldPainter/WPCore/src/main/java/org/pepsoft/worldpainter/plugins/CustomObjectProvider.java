package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.objects.WPObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A plugin which can import and export custom objects of some specific type(s).
 *
 * <p>Created by Pepijn on 9-3-2017.
 */
public interface CustomObjectProvider extends Provider<String> {
    /**
     * Get the list of filename extensions of the custom object formats
     * supported by this plugin.
     *
     * @return The list of filename extensions of the custom object formats
     * supported by this plugin.
     */
    List<String> getSupportedExtensions();

    /**
     * Load a custom object from a specific file.
     *
     * @param file The file from which to load the custom object.
     * @return The custom object contained in the specified file.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    WPObject loadObject(File file) throws IOException;
}