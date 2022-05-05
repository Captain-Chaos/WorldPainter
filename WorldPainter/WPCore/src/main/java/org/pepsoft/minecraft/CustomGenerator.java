package org.pepsoft.minecraft;

import org.jnbt.Tag;

import static org.pepsoft.worldpainter.Generator.CUSTOM;

public class CustomGenerator extends MapGenerator {
    public CustomGenerator(String name, Tag settings) {
        super(CUSTOM);
        this.name = name;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public Tag getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return getType().name() + "(name=" + name + ((settings != null) ? (", " + settings) : "") + ")";
    }

    private final String name;
    private final Tag settings;

    private static final long serialVersionUID = 1L;
}