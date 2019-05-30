package org.pepsoft.util.plugins;

import com.google.common.collect.ImmutableList;
import org.pepsoft.util.Version;

import java.util.List;

/**
 * A plugin descriptor.
 */
public class Descriptor {
    public Descriptor(String name, List<String> classes, String descriptorUrl, String pluginUrl, Version version, Version minimumHostVersion) {
        this.name = name;
        this.classes = ImmutableList.copyOf(classes);
        this.descriptorUrl = descriptorUrl;
        this.pluginUrl = pluginUrl;
        this.version = version;
        this.minimumHostVersion = minimumHostVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (name != null) {
            sb.append("name: ").append(name);
        }
        if (version != null) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append("version: ").append(version);
        }
        sb.append('}');
        return sb.toString();
    }

    public final List<String> classes;
    public final String name, descriptorUrl, pluginUrl;
    public final Version version, minimumHostVersion;
}