package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SuspendPanel extends JPanel {

    public SuspendPanel(Runnable onSuspend, Runnable onResume) {
        setLayout(new GridLayout(1, 2, 10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Transaction Control"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JButton suspendBtn = createButton("Suspend", onSuspend, new Color(255, 140, 0));
        JButton resumeBtn = createButton("Resume", onResume, new Color(34, 139, 34));

        add(suspendBtn);
        add(resumeBtn);
    }

    private JButton createButton(String text, Runnable action, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusable(false);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.addActionListener(e -> action.run());
        return button;
    }
}