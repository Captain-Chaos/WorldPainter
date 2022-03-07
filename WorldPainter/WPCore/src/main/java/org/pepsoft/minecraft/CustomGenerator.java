package org.pepsoft.minecraft;

import static org.pepsoft.worldpainter.Generator.CUSTOM;

public class CustomGenerator extends MapGenerator {
    public CustomGenerator(String name) {
        super(CUSTOM);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getType().name() + "(name=" + name + ")";
    }

    private final String name;

    private static final long serialVersionUID = 1L;
}