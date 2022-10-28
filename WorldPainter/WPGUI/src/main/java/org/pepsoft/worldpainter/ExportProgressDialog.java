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
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.TaskbarProgressReceiver;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 *
 * @author pepijn
 */
public class ExportProgressDialog extends MultiProgressDialog<Map<Integer, ChunkFactory.Stats>> implements WindowListener {
    /** Creates new form ExportWorldDialog */
    public ExportProgressDialog(Window parent, World2 world, WorldExportSettings exportSettings, File baseDir, String name) {
        super(parent, "Exporting");
        this.world = world;
        this.baseDir = baseDir;
        this.name = name;
        this.exportSettings = exportSettings;
        addWindowListener(this);

        JButton minimiseButton = new JButton("Minimize");
        minimiseButton.addActionListener(e -> App.getInstance().setState(Frame.ICONIFIED));
        addButton(minimiseButton);
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
        if ((world.getPlatform() == JAVA_MCREGION) && (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_MCREGION)) {
            sb.append("<br><br>Please note: this map has a <b>non-standard height!</b> You need to have<br>an appropriate height mod installed to play it!");
        }
        if (((world.getPlatform() == JAVA_ANVIL_1_17) || (world.getPlatform() == JAVA_ANVIL_1_18)) && (world.getMaxHeight() > 320)) {
            sb.append("<br><br>Please note: this map is <b>more than 320 blocks</b> high.<br>This may cause performance problems on lower end computers.");
        }
        if ((world.getPlatform() == JAVA_ANVIL_1_17) && (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_ANVIL)) {
            sb.append("<br><br>Please note: <b>this map uses a data pack</b> for a deviating build height.<br>This may not be forward compatible with newer versions of Minecraft.");
        }
        if ((world.getPlatform() == JAVA_ANVIL_1_18) && (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_1_18)) {
            sb.append("<br><br>Please note: <b>this map uses a data pack</b> for a deviating build height.<br>This data pack is only compatible with Minecraft 1.18.2.<br>It may not be forward compatible with newer versions of Minecraft.");
        }
        if (result.size() == 1) {
            ChunkFactory.Stats stats = result.get(result.keySet().iterator().next());
            sb.append("<br><br>Statistics:<br>");
            dumpStats(sb, stats);
        } else {
            for (Map.Entry<Integer, ChunkFactory.Stats> entry: result.entrySet()) {
                int dim = entry.getKey();
                ChunkFactory.Stats stats = entry.getValue();
                switch (dim) {
                    case Constants.DIM_NORMAL:
                        sb.append("<br><br>Statistics for surface:<br>");
                        break;
                    case Constants.DIM_NETHER:
                        sb.append("<br><br>Statistics for Nether:<br>");
                        break;
                    case Constants.DIM_END:
                        sb.append("<br><br>Statistics for End:<br>");
                        break;
                    default:
                        sb.append("<br><br>Statistics for dimension " + dim + ":<br>");
                        break;
                }
                dumpStats(sb, stats);
            }
        }
        if (backupDir.isDirectory()) {
            sb.append("<br>Backup of existing map created in:<br>").append(backupDir);
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
                    return exporter.export(baseDir, name, backupDir, progressReceiver);
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while exporting world", e);
                }
            }
        };
    }

    private void dumpStats(final StringBuilder sb, final ChunkFactory.Stats stats) {
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
        final long totalBlocks = stats.surfaceArea * world.getMaxHeight();
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
    
    private final World2 world;
    private final String name;
    private final File baseDir;
    private final WorldExportSettings exportSettings;
    private volatile File backupDir;
    
    private static final long serialVersionUID = 1L;
}