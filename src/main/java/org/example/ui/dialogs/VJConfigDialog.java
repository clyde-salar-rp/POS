package org.example.ui.dialogs;

import org.example.config.VJConfig;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VJConfigDialog extends JDialog {
    private final VJConfig config;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JCheckBox retryCheckBox;
    private JSpinner maxAttemptsSpinner;
    private JSpinner delaySpinner;

    public VJConfigDialog(Frame owner, VJConfig config) {
        super(owner, "Virtual Journal Configuration", true);
        this.config = config;

        setSize(500, 400);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Host
        JPanel hostPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hostPanel.add(new JLabel("VJ Server Host:"));
        hostField = new JTextField(config.getServerHost(), 20);
        hostPanel.add(hostField);
        mainPanel.add(hostPanel);

        // Port
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.add(new JLabel("VJ Server Port:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(config.getServerPort(), 1, 65535, 1));
        portPanel.add(portSpinner);
        mainPanel.add(portPanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Retry settings
        retryCheckBox = new JCheckBox("Enable Auto-Retry", config.isRetryEnabled());
        mainPanel.add(retryCheckBox);

        JPanel attemptsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        attemptsPanel.add(new JLabel("Max Retry Attempts:"));
        maxAttemptsSpinner = new JSpinner(new SpinnerNumberModel(config.getMaxRetryAttempts(), 1, 20, 1));
        attemptsPanel.add(maxAttemptsSpinner);
        mainPanel.add(attemptsPanel);

        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delayPanel.add(new JLabel("Retry Delay (seconds):"));
        delaySpinner = new JSpinner(new SpinnerNumberModel(config.getRetryDelaySeconds(), 1, 60, 1));
        delayPanel.add(delaySpinner);
        mainPanel.add(delayPanel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            saveConfig();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    private void saveConfig() {
        config.setServerHost(hostField.getText().trim());
        config.setServerPort((Integer) portSpinner.getValue());
        config.saveConfig();

        JOptionPane.showMessageDialog(this,
                "Configuration saved!\nRestart the application for changes to take effect.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showDialog(Frame owner, VJConfig config) {
        VJConfigDialog dialog = new VJConfigDialog(owner, config);
        dialog.setVisible(true);
    }
}