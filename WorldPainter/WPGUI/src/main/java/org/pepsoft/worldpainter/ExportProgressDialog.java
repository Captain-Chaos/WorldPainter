/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExportWorldDialog.java
 *
 * Created on Mar 29, 2011, 5:09:50 PM
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.*;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.Version;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.FileInUseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Comparator.comparingLong;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.ExceptionUtils.chainContains;
import static org.pepsoft.worldpainter.Constants.V_1_17;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 *
 * @author pepijn
 */
public class ExportProgressDialog extends MultiProgressDialog<Map<Integer, ChunkFactory.Stats>> implements WindowListener {
    /** Creates new form ExportWorldDialog */
    public ExportProgressDialog(Window parent, World2 world, WorldExportSettings exportSettings, File baseDir, String name, String acknowledgedWarnings) {
        super(parent, "Exporting");
        this.world = world;
        this.baseDir = baseDir;
        this.name = name;
        this.exportSettings = exportSettings;
        this.acknowledgedWarnings = acknowledgedWarnings;
        addWindowListener(this);

        JButton minimiseButton = new JButton("Minimize");
        minimiseButton.addActionListener(e -> App.getInstance().setState(Frame.ICONIFIED));
        addButton(minimiseButton);
    }

    public boolean isAllowRetry() {
        return allowRetry;
    }

    // WindowListener

    @Override
    public void windowClosed(WindowEvent e) {
        // Make sure to clean up any progress that is still showing
        DesktopUtils.setProgressDone(App.getInstance());
    }

    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowOpened(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    // MultiProgressDialog

    @Override
    protected String getVerb() {
        return "Export";
    }

    @Override
    protected String getResultsReport(Map<Integer, ChunkFactory.Stats> result, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>World exported as ").append(new File(baseDir, FileUtils.sanitiseName(name)));
        int hours = (int) (duration / 3600);
        duration = duration - hours * 3600L;
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration - minutes * 60);
        sb.append("<br>Export took ").append(hours).append(":").append((minutes < 10) ? "0" : "").append(minutes).append(":").append((seconds < 10) ? "0" : "").append(seconds);
        final Platform platform = world.getPlatform();
        final Version mcVersion = platform.getAttribute(ATTRIBUTE_MC_VERSION);
        if ((platform == JAVA_MCREGION) && (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_MCREGION)) {
            sb.append("<br><br>Please note: this map has a <b>non-standard height!</b> You need to have<br>an appropriate height mod installed to play it!");
        } else if ((mcVersion.isAtLeast(V_1_17)) && ((world.getMaxHeight() - world.getMinHeight()) > 384)) {
            sb.append("<br><br>Please note: this map is <b>more than 384 blocks</b> high.<br>This may cause performance problems on lower end computers.");
        }
        if ((platform == JAVA_ANVIL_1_17) && ((world.getMinHeight() != DEFAULT_MIN_HEIGHT) || (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_ANVIL))) {
            sb.append("<br><br>Please note: <b>this map uses a data pack</b> for a deviating build height.<br>This data pack has only been tested with Minecraft 1.17.<br>It is not forward compatible with newer versions of Minecraft.");
        } else if ((world.getMinHeight() != DEFAULT_MIN_HEIGHT_1_18) || (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_1_18)) {
            if ((platform == JAVA_ANVIL_1_18) || (platform == JAVA_ANVIL_1_19)) {
                sb.append("<br><br>Please note: <b>this map uses a data pack</b> for a deviating build height.<br>This data pack has only been tested with Minecraft 1.18.2 - 1.20.4.<br>It may not be forward compatible with newer versions of Minecraft.");
            } else if (platform == JAVA_ANVIL_1_20_5) {
                sb.append("<br><br>Please note: <b>this map uses a data pack</b> for a deviating build height.<br>This data pack has only been tested with Minecraft 1.20.5 - 1.21.8.<br>It may not be forward compatible with newer versions of Minecraft.");
            }
        }
        if (result.size() == 1) {
            ChunkFactory.Stats stats = result.get(result.keySet().iterator().next());
            sb.append("<br><br>Statistics:<br>");
            dumpStats(sb, stats, world.getMaxHeight() - world.getMinHeight());
            if ((stats.timings != null) && (! stats.timings.isEmpty())) {
                sb.append("<br>Three longest stages (preview of timings feature):");
                sb.append("<table><tr><th>Stage</th><th>Description</th><th>Duration</th></tr>");
                stats.timings.entrySet().stream()
                        .sorted(comparingLong((Map.Entry<Object, AtomicLong> entry) -> entry.getValue().longValue()).reversed())
                        .limit(3)
                        .forEach(entry -> sb.append("<tr><td>")
                                .append(formatTimingLabel(entry.getKey()))
                                .append("</td><td>")
                                .append(formatTimingDescription(entry.getKey()))
                                .append("</td><td>")
                                .append(formatTimingValue(entry.getValue().longValue()))
                                .append("</td></tr>"));
                sb.append("</table><br>");
            }
        } else {
            for (Map.Entry<Integer, ChunkFactory.Stats> entry: result.entrySet()) {
                final int dim = entry.getKey();
                final int height;
                ChunkFactory.Stats stats = entry.getValue();
                switch (dim) {
                    case Constants.DIM_NORMAL:
                        sb.append("<br><br>Statistics for surface:<br>");
                        height = world.getMaxHeight() - world.getMinHeight();
                        break;
                    case Constants.DIM_NETHER:
                        sb.append("<br><br>Statistics for Nether:<br>");
                        height = DEFAULT_MAX_HEIGHT_NETHER;
                        break;
                    case Constants.DIM_END:
                        sb.append("<br><br>Statistics for End:<br>");
                        height = DEFAULT_MAX_HEIGHT_END;
                        break;
                    default:
                        sb.append("<br><br>Statistics for dimension " + dim + ":<br>");
                        height = world.getMaxHeight() - world.getMinHeight();
                        break;
                }
                dumpStats(sb, stats, height);
            }
        }
        if (backupDir.isDirectory()) {
            sb.append("<br>Backup of existing map created in:<br>").append(backupDir);
        }
        if ((acknowledgedWarnings != null) && (! acknowledgedWarnings.trim().isEmpty())) {
            sb.append("<br><br><em>Previously acknowledged warnings:</em>");
            sb.append(acknowledgedWarnings);
        }
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    protected String getCancellationMessage() {
        return "Export cancelled by user.\n\nThe partially exported map is now probably corrupted!\nYou should delete it, or export the map again." + (backupDir.isDirectory() ? ("\n\nBackup of existing map created in:\n" + backupDir) : "");
    }

    @Override
    protected ProgressTask<Map<Integer, ChunkFactory.Stats>> getTask() {
        return new ProgressTask<Map<Integer, ChunkFactory.Stats>>() {
            @Override
            public String getName() {
                return "Exporting world " + name;
            }

            @Override
            public Map<Integer, ChunkFactory.Stats> execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                progressReceiver = new TaskbarProgressReceiver(App.getInstance(), progressReceiver);
                progressReceiver.setMessage("Exporting world " + name);
                WorldExporter exporter = PlatformManager.getInstance().getExporter(world, exportSettings);
                try {
                    backupDir = exporter.selectBackupDir(baseDir, name);

                    ProgressReceiver finalProgressReceiver = progressReceiver;
                    return exporter.export(baseDir, name, backupDir, new ProgressReceiver() {
                        @Override
                        public void setProgress(float progress) throws OperationCancelled {

                        }

                        @Override
                        public void exceptionThrown(Throwable exception) {
                            finalProgressReceiver.exceptionThrown(exception);
                        }

                        @Override
                        public void done() {
                            finalProgressReceiver.done();
                        }

                        @Override
                        public void setMessage(String message) throws OperationCancelled {

                        }

                        @Override
                        public void checkForCancellation() throws OperationCancelled {
                            finalProgressReceiver.checkForCancellation();
                        }

                        @Override
                        public void reset() throws OperationCancelled {
                            finalProgressReceiver.reset();
                        }

                        @Override
                        public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
                            finalProgressReceiver.subProgressStarted(subProgressReceiver);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while exporting world", e);
                } catch (RuntimeException e) {
                    if (chainContains(e, FileInUseException.class)) {
                        allowRetry = true;
                    }
                    throw e;
                }
            }
        };
    }

    private void dumpStats(final StringBuilder sb, final ChunkFactory.Stats stats, final int height) {
        final NumberFormat formatter = NumberFormat.getIntegerInstance();
        final long duration = stats.time / 1000;
        if (stats.landArea > 0) {
            sb.append("Land area: " + formatter.format(stats.landArea) + " blocks<br>");
        }
        if (stats.waterArea > 0) {
            sb.append("Water or lava area: " + formatter.format(stats.waterArea) + " blocks<br>");
            if (stats.landArea > 0) {
                sb.append("Total surface area: " + formatter.format(stats.landArea + stats.waterArea) + " blocks<br>");
            }
        }
        final long totalBlocks = stats.surfaceArea * height;
        if (duration > 0) {
            sb.append("Generated " + formatter.format(totalBlocks) + " blocks, or " + formatter.format(totalBlocks / duration) + " blocks per second<br>");
            if (stats.size > 0) {
                final long kbPerSecond = stats.size / duration / 1024;
                sb.append("Map size: " + formatter.format(stats.size / 1024 / 1024) + " MB, or " + ((kbPerSecond < 1024) ? (formatter.format(kbPerSecond) + " KB") : (formatter.format(kbPerSecond / 1024) + " MB")) + " per second<br>");
            }
        } else {
            sb.append("Generated " + formatter.format(totalBlocks) + " blocks<br>");
            if (stats.size > 0) {
                sb.append("Map size: " + formatter.format(stats.size / 1024 / 1024) + " MB<br>");
            }
        }
    }

    private String formatTimingLabel(Object label) {
        if (label instanceof Layer) {
            return ((Layer) label).getName();
        } else if (label instanceof ChunkFactory.Stage) {
            return ((ChunkFactory.Stage) label).getName();
        } else {
            return label.toString();
        }
    }

    private String formatTimingDescription(Object label) {
        if (label instanceof Layer) {
            return ((Layer) label).getDescription();
        } else if (label instanceof ChunkFactory.Stage) {
            return ((ChunkFactory.Stage) label).getDescription();
        } else {
            return "";
        }
    }

    private String formatTimingValue(long value) {
        return formatter.format(value / 1000000000L) + " s";
    }

    private final World2 world;
    private final String name, acknowledgedWarnings;
    private final File baseDir;
    private final WorldExportSettings exportSettings;
    private final NumberFormat formatter = NumberFormat.getIntegerInstance();
    private volatile File backupDir;
    private volatile boolean allowRetry = false;
    
    private static final long serialVersionUID = 1L;
}