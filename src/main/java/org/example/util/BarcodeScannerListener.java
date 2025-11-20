package org.example.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;

/**
 * Barcode Scanner Listener with Priority Input Detection
 *
 * Detects rapid keyboard input characteristic of barcode scanners and gives
 * it priority over manual typing. When rapid input is detected, any manual
 * input is cleared and overridden.
 *
 * Usage:
 * <pre>
 * BarcodeScannerListener listener = new BarcodeScannerListener(
 *     (barcode, source) -> processBarcode(barcode),
 *     manualInputField
 * );
 * KeyboardFocusManager.getCurrentKeyboardFocusManager()
 *     .addKeyEventDispatcher(listener);
 * </pre>
 */
public class BarcodeScannerListener implements KeyEventDispatcher {
    private final BiConsumer<String, String> barcodeCallback;
    private final JTextField manualInputField;

    private StringBuilder buffer = new StringBuilder();
    private long lastKeyTime = 0;
    private long rapidInputStartTime = 0;
    private boolean rapidInputActive = false;

    // Scanner detection thresholds
    private static final long SCANNER_SPEED_THRESHOLD = 50;  // Max ms between scanner keystrokes
    private static final long RAPID_INPUT_WINDOW = 200;      // Window to consider input "rapid"
    private static final int MIN_BARCODE_LENGTH = 8;
    private static final int MIN_RAPID_CHARS = 3;            // Min chars to trigger rapid detection

    /**
     * Creates a new barcode scanner listener
     *
     * @param barcodeCallback Callback invoked when a barcode is scanned (barcode, source)
     * @param manualInputField The text field used for manual entry (will be cleared on scan)
     */
    public BarcodeScannerListener(BiConsumer<String, String> barcodeCallback, JTextField manualInputField) {
        this.barcodeCallback = barcodeCallback;
        this.manualInputField = manualInputField;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_TYPED) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastKey = currentTime - lastKeyTime;

        // Detect start of rapid input (scanner)
        if (timeSinceLastKey <= SCANNER_SPEED_THRESHOLD) {
            if (!rapidInputActive) {
                // Rapid input detected - clear any manual input and take priority
                rapidInputActive = true;
                rapidInputStartTime = currentTime;

                // Clear the manual input field if we're taking over
                SwingUtilities.invokeLater(() -> {
                    if (manualInputField != null && e.getComponent() == manualInputField) {
                        manualInputField.setText("");
                    }
                });

                logEvent("SCANNER", "Rapid input detected - scanner priority active");
            }
        } else if (timeSinceLastKey > SCANNER_SPEED_THRESHOLD * 3) {
            // Slow typing detected - reset buffer if it contains scanner remnants
            if (rapidInputActive) {
                logEvent("SCANNER", "Rapid input ended");
            }
            rapidInputActive = false;
            buffer.setLength(0);
        }

        lastKeyTime = currentTime;
        char c = e.getKeyChar();

        // Accumulate characters BEFORE checking for Enter
        // This ensures we don't lose the first character before rapid input is detected
        if (Character.isLetterOrDigit(c) || c == '-') {
            buffer.append(c);

            // Prevent buffer overflow
            if (buffer.length() > 50) {
                buffer.setLength(0);
                rapidInputActive = false;
            }

            // Consume event if we're in scanner mode and in the scan field
            // and we've detected enough rapid characters
            if (rapidInputActive && buffer.length() >= MIN_RAPID_CHARS && e.getComponent() == manualInputField) {
                return true; // Prevent the character from appearing in the field
            }
        }

        // Handle Enter/Return - complete the barcode scan
        if (c == '\n' || c == '\r') {
            if (buffer.length() >= MIN_BARCODE_LENGTH && rapidInputActive) {
                String scannedCode = buffer.toString();
                buffer.setLength(0);
                rapidInputActive = false;

                SwingUtilities.invokeLater(() -> {
                    // Clear manual field and process scanner input
                    if (manualInputField != null) {
                        manualInputField.setText("");
                    }
                    barcodeCallback.accept(scannedCode, "BARCODE_SCANNER");
                });

                // Consume the event to prevent it from triggering manual entry
                return true;
            } else {
                buffer.setLength(0);
                rapidInputActive = false;
            }
            return false;
        }

        return false;
    }

    /**
     * Check if rapid input (scanner) was recently detected
     * Used to prevent manual entry from processing during scanner input
     *
     * @return true if scanner input is active or was recently active
     */
    public boolean isRapidInputDetected() {
        return rapidInputActive ||
                (System.currentTimeMillis() - rapidInputStartTime < RAPID_INPUT_WINDOW);
    }

    /**
     * Reset the scanner state
     * Useful when clearing transactions or resetting the UI
     */
    public void reset() {
        buffer.setLength(0);
        rapidInputActive = false;
        lastKeyTime = 0;
        rapidInputStartTime = 0;
    }

    /**
     * Get current buffer contents (for debugging)
     */
    public String getBufferContents() {
        return buffer.toString();
    }

    /**
     * Check if scanner is currently active
     */
    public boolean isActive() {
        return rapidInputActive;
    }

    private void logEvent(String source, String message) {
        System.out.println("[" + source + "] " + message);
    }
}