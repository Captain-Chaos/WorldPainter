/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DonationDialog.java
 *
 * Created on 5-nov-2011, 17:24:59
 */
package org.pepsoft.worldpainter;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.vo.EventVO;

/**
 *
 * @author pepijn
 */
public final class DonationDialog extends javax.swing.JDialog {
    /** Creates new form DonationDialog */
    public DonationDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        // Fix JTextArea font, which uses a butt ugly non-proportional font by
        // default on Windows
        jTextArea1.setFont(UIManager.getFont("TextField.font"));
        
        try {
            URL buttonURL = new URL("http://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif");
            try (InputStream in = buttonURL.openStream()) {
                BufferedImage buttonImage = ImageIO.read(in);
                // buttonImage is null if the image format is not supported:
                if (buttonImage != null) {
                    jButton1.setIcon(new ImageIcon(buttonImage));
                    jButton1.setText(null);
                    jButton1.setBorder(new EmptyBorder(0, 0, 0, 0));
                    jButton1.setContentAreaFilled(false);
                    pack();
                }
            }
        } catch (IOException e) {
            // Do nothing
        }
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                askLater();
            }

            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        rootPane.setDefaultButton(jButton1);
        pack();
    }

    public boolean isCancelled() {
        return cancelled;
    }
    
    public static void maybeShowDonationDialog(Frame parent) {
        Configuration config = Configuration.getInstance();
        if ((config.getLaunchCount() >= 5) && (config.getDonationStatus() == null)) {
            DonationDialog dialog = new DonationDialog(parent, true);
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            if (dialog.isCancelled()) {
                config.logEvent(new EventVO(Constants.EVENT_KEY_DONATION_CLOSED).addTimestamp());
            }
        }
    }
    
    private void donate() {
        try {
            DesktopUtils.open(new URL("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=VZ7WNQVPXDZHY"));
            Configuration.getInstance().setDonationStatus(Configuration.DonationStatus.DONATED);
            JOptionPane.showMessageDialog(this, "The donation PayPal page has been opened in your browser.\n\nThank you very much for donating!", "Thank You", JOptionPane.INFORMATION_MESSAGE);
            cancelled = false;
            Configuration.getInstance().logEvent(new EventVO(Constants.EVENT_KEY_DONATION_DONATE).addTimestamp());
            dispose();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void alreadyDonated() {
        Configuration.getInstance().setDonationStatus(Configuration.DonationStatus.DONATED);
        JOptionPane.showMessageDialog(this, "Thank you very much for donating!", "Thank You", JOptionPane.INFORMATION_MESSAGE);
        cancelled = false;
        Configuration.getInstance().logEvent(new EventVO(Constants.EVENT_KEY_DONATION_ALREADY_DONATED).addTimestamp());
        dispose();
    }
    
    private void askLater() {
        cancelled = false;
        Configuration.getInstance().logEvent(new EventVO(Constants.EVENT_KEY_DONATION_ASK_LATER).addTimestamp());
        dispose();
    }
    
    private void noThanks() {
        Configuration.getInstance().setDonationStatus(Configuration.DonationStatus.NO_THANK_YOU);
        JOptionPane.showMessageDialog(this, "Alright, no problem. We will not ask you again.\nIf you ever change your mind, you can donate from the About screen!", "No Problem", JOptionPane.INFORMATION_MESSAGE);
        cancelled = false;
        Configuration.getInstance().logEvent(new EventVO(Constants.EVENT_KEY_DONATION_NO_THANKS).addTimestamp());
        dispose();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Please Donate");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/resources/banner.png"))); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("Thank you for using WorldPainter!\n\nWorldPainter takes a lot of effort to create and maintain. You are free to keep using it without paying, but please consider helping out with a small donation of € 4.95.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setOpaque(false);

        jButton1.setMnemonic('d');
        jButton1.setText("Donate");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setMnemonic('a');
        jButton2.setText("I have already donated");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jButton3.setMnemonic('l');
        jButton3.setText("Ask me later");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        jButton4.setMnemonic('n');
        jButton4.setText("No thank you");
        jButton4.addActionListener(this::jButton4ActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4))
                    .addComponent(jTextArea1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jTextArea1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        donate();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        alreadyDonated();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        askLater();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        noThanks();
    }//GEN-LAST:event_jButton4ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables

    private boolean cancelled = true;
    
    private static final long serialVersionUID = 1L;
}