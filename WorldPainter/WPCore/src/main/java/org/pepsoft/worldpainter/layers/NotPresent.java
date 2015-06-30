package org.pepsoft.worldpainter.layers;

/**
 * Technical layer which indicates that a chunk is to be considered not present
 * and should not be exported, merged or displayed in an editor or viewer.
 *
 * Created by Pepijn Schmitz on 30-06-15.
 */
public class NotPresent extends Layer {
    private NotPresent() {
        super(NotPresent.class.getName(), "Not Present", "Mark chunks that were not present in the map from which the world was imported", DataSize.BIT_PER_CHUNK, 91);
    }

    public static final NotPresent INSTANCE = new NotPresent();

    private static final long serialVersionUID = 1L;
}