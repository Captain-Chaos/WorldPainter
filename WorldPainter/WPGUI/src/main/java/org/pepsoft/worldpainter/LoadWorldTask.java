package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.swing.ProgressTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.pepsoft.worldpainter.World2.*;

public class LoadWorldTask implements ProgressTask<World2> {
    public LoadWorldTask(Component parent, File file) {
        this.parent = parent;
        this.file = file;
    }

    @Override
    public String getName() {
        return strings.getString("loading.world");
    }

    @Override
    public World2 execute(ProgressReceiver progressReceiver) {
        try {
            WorldIO worldIO = new WorldIO();
            worldIO.load(new FileInputStream(file));
            World2 world = worldIO.getWorld();
            if (logger.isDebugEnabled() && (world.getMetadata() != null)) {
                logMetadataAsDebug(world.getMetadata());
            }
            return world;
        } catch (UnloadableWorldException e) {
            logger.error("Could not load world from file " + file, e);
            if (e.getMetadata() != null) {
                logMetadataAsError(e.getMetadata());
            }
            reportUnloadableWorldException(e);
            return null;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while loading world", e);
        }
    }

    private void appendMetadata(StringBuilder sb, Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry: metadata.entrySet()) {
            switch (entry.getKey()) {
                case METADATA_KEY_WP_VERSION:
                    sb.append("Saved with WorldPainter ").append(entry.getValue());
                    String build = (String) metadata.get(METADATA_KEY_WP_BUILD);
                    if (build != null) {
                        sb.append(" (").append(build).append(')');
                    }
                    sb.append('\n');
                    break;
                case METADATA_KEY_TIMESTAMP:
                    sb.append("Saved on ").append(SimpleDateFormat.getDateTimeInstance().format((Date) entry.getValue())).append('\n');
                    break;
                case METADATA_KEY_PLUGINS:
                    String[][] plugins = (String[][]) entry.getValue();
                    for (String[] plugin: plugins) {
                        sb.append("Plugin: ").append(plugin[0]).append(" (").append(plugin[1]).append(")\n");
                    }
                    break;
            }
        }
    }

    private void logMetadataAsDebug(Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder("Metadata from world file:\n");
        appendMetadata(sb, metadata);
        logger.debug(sb.toString());
    }

    private void logMetadataAsError(Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder("Metadata from world file:\n");
        appendMetadata(sb, metadata);
        logger.error(sb.toString());
    }

    private void reportUnloadableWorldException(UnloadableWorldException e) {
        try {
            String text;
            if (e.getMetadata() != null) {
                StringBuilder sb = new StringBuilder("WorldPainter could not load the file. The cause may be one of:\n" +
                        "\n" +
                        "* The file is damaged or corrupted\n" +
                        "* The file was created with a newer version of WorldPainter\n" +
                        "* The file was created using WorldPainter plugins which you do not have\n" +
                        "\n");
                appendMetadata(sb, e.getMetadata());
                text = sb.toString();
            } else {
                text = "WorldPainter could not load the file. The cause may be one of:\n" +
                        "\n" +
                        "* The file is not a WorldPainter world\n" +
                        "* The file is damaged or corrupted\n" +
                        "* The file was created with a newer version of WorldPainter\n" +
                        "* The file was created using WorldPainter plugins which you do not have";
            }
            SwingUtilities.invokeAndWait(() -> showMessageDialog(parent, text, strings.getString("file.damaged"), ERROR_MESSAGE));
        } catch (InterruptedException e2) {
            throw new RuntimeException("Thread interrupted while reporting unloadable file " + file, e2);
        } catch (InvocationTargetException e2) {
            throw new RuntimeException("Invocation target exception while reporting unloadable file " + file, e2);
        }
    }

    private final Component parent;
    private final File file;

    private static final Logger logger = LoggerFactory.getLogger(LoadWorldTask.class);
    private static final ResourceBundle strings = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings"); // NOI18N
}