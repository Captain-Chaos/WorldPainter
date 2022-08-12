package org.pepsoft.worldpainter.layers;

/**
 * Created by Pepijn on 15-1-2017.
 */
public class Caves extends Layer {
    private Caves() {
        super("org.pepsoft.Caves", "Caves", "Generate underground tunnel-like caves of varying size", DataSize.NIBBLE, false, 23);
    }

    public static final Caves INSTANCE = new Caves();

    private static final long serialVersionUID = 1L;
}
