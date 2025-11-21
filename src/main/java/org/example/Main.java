package org.example;
import javax.swing.*;

import org.example.ui.RegisterWindow;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default
        }

        SwingUtilities.invokeLater(RegisterWindow::new);
    }
}
