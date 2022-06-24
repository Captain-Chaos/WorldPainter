package org.pepsoft.minecraft;

import static org.pepsoft.worldpainter.Generator.FLAT;

public class SuperflatGenerator extends MapGenerator {
    public SuperflatGenerator(SuperflatPreset settings) {
        super(FLAT);
        this.settings = settings;
    }

    public SuperflatPreset getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return getType().name() + "(settings=" + settings + ")";
    }

    private final SuperflatPreset settings;

    private static final long serialVersionUID = 1L;
}