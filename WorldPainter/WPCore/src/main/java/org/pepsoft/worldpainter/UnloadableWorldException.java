package org.pepsoft.worldpainter;

import java.io.InputStream;
import java.util.Map;

/**
 * This exception is thrown by {@link WorldIO#load(InputStream)} if the world
 * cannot be loaded for some reason other than an I/O error.
 *
 * Created by Pepijn Schmitz on 02-07-15.
 */
public class UnloadableWorldException extends Exception {
    public UnloadableWorldException(Throwable cause, Map<String, Object> metadata) {
        super(cause);
        this.metadata = metadata;
    }

    public UnloadableWorldException(String message, Map<String, Object> metadata) {
        super(message);
        this.metadata = metadata;
    }

    public UnloadableWorldException(String message, Throwable cause, Map<String, Object> metadata) {
        super(message, cause);
        this.metadata = metadata;
    }

    /**
     * If the file contained metadata which <em>could</em> be loaded it is
     * stored in this property.
     *
     * @return The metadata from the file, if it was present and could be
     *     loaded; <code>null</code> otherwise.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    private final Map<String, Object> metadata;
}