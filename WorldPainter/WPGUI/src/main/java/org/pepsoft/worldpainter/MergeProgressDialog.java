/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TaskbarProgressReceiver;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.merging.WorldMerger;

/**
 *
 * @author Pepijn Schmitz
 */
public class MergeProgressDialog extends MultiProgressDialog<Void> implements WindowListener {
    public MergeProgressDialog(Window parent, WorldMerger merger, File backupDir, boolean biomesOnly) {
        super(parent, "Merging");
        this.merger = merger;
        this.backupDir = backupDir;
        this.biomesOnly = biomesOnly;
        addWindowListener(this);
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
        return "Merge";
    }

    @Override
    protected String getResultsReport(Void results, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("World merged with ").append(merger.getLevelDatFile());
        int hours = (int) (duration / 3600);
        duration = duration - hours * 3600;
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration - minutes * 60);
        sb.append("\nMerge took ").append(hours).append(":").append((minutes < 10) ? "0" : "").append(minutes).append(":").append((seconds < 10) ? "0" : "").append(seconds);
        sb.append("\n\nBackup of existing map created in:\n").append(backupDir);
        return sb.toString();
    }

    @Override
    protected String getCancellationMessage() {
        return "Merge cancelled by user.\n\nThe partially merged map is now probably corrupted!\nYou should delete it, and restore it from the backup at:\n" + backupDir;
    }

    @Override
    protected ProgressTask<Void> getTask() {
        return new ProgressTask<Void>() {
            @Override
            public String getName() {
                return "Merging world " + merger.getWorld().getName();
            }

            @Override
            public Void execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
                progressReceiver = new TaskbarProgressReceiver(App.getInstance(), progressReceiver);
                try {
                    if (biomesOnly) {
                        merger.mergeBiomes(backupDir, progressReceiver);
                    } else {
                        merger.merge(backupDir, progressReceiver);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while merging world " + merger.getWorld().getName() + " with map " + merger.getLevelDatFile().getParent(), e);
                }
                return null;
            }
        };
    }

    private final File backupDir;
    private final WorldMerger merger;
    private final boolean biomesOnly;
}