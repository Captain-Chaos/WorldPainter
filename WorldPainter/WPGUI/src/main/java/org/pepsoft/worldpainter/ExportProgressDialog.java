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

/**
 *
 * @author pepijn
 */
public class ExportProgressDialog extends MultiProgressDialog<Map<Integer, ChunkFactory.Stats>> implements WindowListener {
    /** Creates new form ExportWorldDialog */
    public ExportProgressDialog(Window parent, World2 world, File baseDir, String name) {
        super(parent, "Exporting");
        this.world = world;
        this.baseDir = baseDir;
        this.name = name;
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
        boolean nonStandardHeight = world.getMaxHeight() != world.getPlatform().standardMaxHeight;
        StringBuilder sb = new StringBuilder();
        sb.append("<html>World exported as ").append(new File(baseDir, FileUtils.sanitiseName(name)));
        int hours = (int) (duration / 3600);
        duration = duration - hours * 3600;
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration - minutes * 60);
        sb.append("<br>Export took ").append(hours).append(":").append((minutes < 10) ? "0" : "").append(minutes).append(":").append((seconds < 10) ? "0" : "").append(seconds);
        if (nonStandardHeight) {
            sb.append("<br><br><b>Please note:</b> this level has a non-standard height! You need to have<br>an appropriate height mod installed to play it!");
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
            sb.append("<br><br>Backup of existing map created in:<br>").append(backupDir);
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
        Configuration config = Configuration.getInstance();
        if (config != null) {
            config.setExportDirectory(baseDir);
        }
        
        return new ProgressTask<Map<Integer, ChunkFactory.Stats>>() {
            @Override
            public String getName() {
                return "Exporting world " + name;
            }

            @Override
            public Map<Integer, ChunkFactory.Stats> execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                progressReceiver = new TaskbarProgressReceiver(App.getInstance(), progressReceiver);
                progressReceiver.setMessage("Exporting world " + name);
                WorldExporter exporter = PlatformManager.getInstance().getExporter(world);
                try {
                    backupDir = exporter.selectBackupDir(new File(baseDir, FileUtils.sanitiseName(name)));
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
            sb.append("Land area: " + formatter.format(stats.landArea) + " blocks²<br>");
        }
        if (stats.waterArea > 0) {
            sb.append("Water or lava area: " + formatter.format(stats.waterArea) + " blocks²<br>");
            if (stats.landArea > 0) {
                sb.append("Total surface area: " + formatter.format(stats.landArea + stats.waterArea) + " blocks²<br>");
            }
        }
        final long totalBlocks = stats.surfaceArea * world.getMaxHeight();
        if (duration > 0) {
            sb.append("Generated " + formatter.format(totalBlocks) + " blocks, or " + formatter.format(totalBlocks / duration) + " blocks per second<br>");
            final long kbPerSecond = stats.size / duration / 1024;
            sb.append("Map size: " + formatter.format(stats.size / 1024 / 1024) + " MB, or " + ((kbPerSecond < 1024) ? (formatter.format(kbPerSecond) + " KB") : (formatter.format(kbPerSecond / 1024) + " MB")) + " per second");
        } else {
            sb.append("Generated " + formatter.format(totalBlocks) + " blocks<br>");
            sb.append("Map size: " + formatter.format(stats.size / 1024 / 1024) + " MB");
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        multiProgressComponent1 = new org.pepsoft.util.swing.MultiProgressComponent();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Exporting");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(multiProgressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(multiProgressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.util.swing.MultiProgressComponent multiProgressComponent1;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final String name;
    private final File baseDir;
    private volatile File backupDir;
    
    private static final long serialVersionUID = 1L;
}