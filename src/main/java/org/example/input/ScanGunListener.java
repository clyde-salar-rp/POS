package org.example.input;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;

public class ScanGunListener implements KeyEventDispatcher {
    private final BiConsumer<String, String> scanCallback;
    private final JTextField manualField;

    private StringBuilder buffer = new StringBuilder();
    private long lastKeyTime = 0;
    private long firstKeyTime = 0;
    private boolean scannerDetected = false;
    private static final long MAX_DELAY_MS = 25; // Fast typing threshold

    public ScanGunListener(BiConsumer<String, String> scanCallback, JTextField manualField) {
        this.scanCallback = scanCallback;
        this.manualField = manualField;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_TYPED) {
            return false;
        }

        char c = e.getKeyChar();
        long currentTime = System.currentTimeMillis();
        long timeSinceLastKey = currentTime - lastKeyTime;

        // Reset buffer if too much time has passed
        if (timeSinceLastKey > MAX_DELAY_MS * 3 && buffer.length() > 0) {
            reset();
        }

        // Track first key time
        if (buffer.length() == 0) {
            firstKeyTime = currentTime;
            scannerDetected = false;
        }

        lastKeyTime = currentTime;

        // Handle Enter - determine if scanner or manual
        if (c == '\n' || c == '\r') {
            if (buffer.length() >= 8) {
                // Calculate average time per keystroke
                long totalTime = currentTime - firstKeyTime;
                double avgTimePerKey = buffer.length() > 0 ?
                        (double) totalTime / buffer.length() : Double.MAX_VALUE;

                // Scanner detection: fast and consistent input
                boolean isScannerSpeed = avgTimePerKey < MAX_DELAY_MS;

                String barcode = buffer.toString();
                String source = isScannerSpeed ? "SCAN_GUN" : "MANUAL";

                if (isScannerSpeed) {
                    // Scanner detected - consume all events and clear field
                    SwingUtilities.invokeLater(() -> {
                        if (manualField != null) {
                            manualField.setText("");
                        }
                        scanCallback.accept(barcode, source);
                    });
                    reset();
                    return true; // Consume event
                } else {
                    // Manual typing - let the field's ActionListener handle it
                    reset();
                    return false; // Don't consume - let manual field process
                }
            }
            reset();
            return false;
        }

        // Accumulate characters
        if (Character.isLetterOrDigit(c) || c == '-') {
            buffer.append(c);

            // Early scanner detection after 2 characters based on timing
            if (buffer.length() >= 2 && !scannerDetected) {
                long totalTime = currentTime - firstKeyTime;
                double avgTimePerKey = (double) totalTime / buffer.length();

                // If typing is very fast, it's likely a scanner
                if (avgTimePerKey < MAX_DELAY_MS) {
                    scannerDetected = true;

                    // Clear any characters that made it into the field
                    SwingUtilities.invokeLater(() -> {
                        if (manualField != null) {
                            manualField.setText("");
                        }
                    });
                }
            }

            // If scanner detected, consume all subsequent key events
            if (scannerDetected && e.getComponent() == manualField) {
                return true; // Consume to prevent typing in field
            }

            // Prevent overflow
            if (buffer.length() > 20) {
                reset();
            }
        }

        return false;
    }

    public void reset() {
        buffer.setLength(0);
        lastKeyTime = 0;
        firstKeyTime = 0;
        scannerDetected = false;
    }
}