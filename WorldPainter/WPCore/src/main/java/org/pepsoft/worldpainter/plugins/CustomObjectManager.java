package org.pepsoft.worldpainter.plugins;

import com.google.common.collect.ImmutableMap;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class CustomObjectManager extends AbstractProviderManager<String, CustomObjectProvider> {
    public CustomObjectManager() {
        super(CustomObjectProvider.class);
        Map<String, CustomObjectProvider> tmpMap = new HashMap<>();
        getImplementations().forEach(provider -> {
            for (String extension: provider.getSupportedExtensions()) {
                tmpMap.put(extension.trim().toLowerCase(), provider);
            }
        });
        providersByExtension = ImmutableMap.copyOf(tmpMap);
    }

    public List<String> getAllSupportedExtensions() {
        return getImplementations().stream()
            .flatMap(provider -> provider.getSupportedExtensions().stream())
            .collect(toList());
    }

    public WPObject loadObject(File file) throws IOException {
        String name = file.getName().toLowerCase();
        int p = name.lastIndexOf('.');
        String extension = (p != -1) ? name.substring(p + 1).trim() : name.trim();
        return providersByExtension.get(extension).loadObject(file);
    }

    /**
     * Get a universal file filter (implementing {@link FileFilter},
     * {@link java.io.FileFilter} and {@link FilenameFilter}) which will select
     * all supported custom object extensions.
     *
     * @return A universal file filter which will elect all supported custom
     * object extensions.
     */
    public UniversalFileFilter getFileFilter() {
        List<String> extensions = getAllSupportedExtensions();
        Set<String> extensionSet = new HashSet<>(extensions);
        String description = "Custom Object Files(" + extensions.stream().map(extension -> "*." + extension).collect(joining(", ")) + ")";
        return new UniversalFileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String name = f.getName();
                    int p = name.lastIndexOf('.');
                    String extension = (p != -1) ? name.substring(p + 1).toLowerCase() : name.toLowerCase();
                    return extensionSet.contains(extension);
                }
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public boolean accept(File dir, String name) {
                int p = name.lastIndexOf('.');
                String extension = (p != -1) ? name.substring(p + 1).toLowerCase() : name.toLowerCase();
                return extensionSet.contains(extension);
            }
        };
    }

    public static CustomObjectManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, CustomObjectProvider> providersByExtension;

    private static final CustomObjectManager INSTANCE = new CustomObjectManager();

    public abstract class UniversalFileFilter extends FileFilter implements java.io.FileFilter, FilenameFilter {}
}