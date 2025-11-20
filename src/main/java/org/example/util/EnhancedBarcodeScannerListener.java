package org.example.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Enhanced Barcode Scanner Listener with Multiple Detection Strategies
 *
 * Uses multiple heuristics to reliably differentiate scanner input from manual typing:
 * 1. Prefix/Suffix Detection (configurable scanner codes)
 * 2. Timing Consistency Analysis (variance in keystroke intervals)
 * 3. Auto-Enter Detection (scanners typically send Enter automatically)
 * 4. Input Length Patterns (barcodes are typically 8-14 digits)
 * 5. No-Modifier Detection (scanners don't send Shift/Ctrl/Alt)
 */
public class EnhancedBarcodeScannerListener implements KeyEventDispatcher {
    private final BiConsumer<String, String> barcodeCallback;
    private final JTextField manualInputField;

    private StringBuilder buffer = new StringBuilder();
    private List<Long> keystrokeTimings = new ArrayList<>();
    private long firstKeyTime = 0;
    private long lastKeyTime = 0;
    private boolean scannerDetected = false;
    private int modifierKeyCount = 0;

    // Configuration
    private ScannerConfig config;

    /**
     * Scanner detection configuration
     */
    public static class ScannerConfig {
        // Timing thresholds
        public long maxInterKeystrokeTime = 50;      // Max ms between scanner keystrokes
        public long minTotalInputTime = 100;          // Min time for complete barcode
        public long maxTimingVariance = 20;           // Max variance in keystroke timing

        // Pattern detection
        public int minBarcodeLength = 8;
        public int maxBarcodeLength = 20;
        public boolean requireAutoEnter = true;       // Scanner must send Enter
        public boolean requireNoModifiers = true;     // Scanner shouldn't send Shift/Ctrl/Alt

        // Prefix/Suffix detection (optional)
        public String scannerPrefix = null;           // e.g., "STX" or custom code
        public String scannerSuffix = null;           // e.g., "ETX" or custom code

        // Statistical analysis
        public boolean useTimingConsistency = true;   // Check for uniform timing
        public double minConfidenceScore = 0.7;       // 0.0 to 1.0

        public static ScannerConfig standard() {
            return new ScannerConfig();
        }

        public static ScannerConfig lenient() {
            ScannerConfig config = new ScannerConfig();
            config.maxInterKeystrokeTime = 100;
            config.minConfidenceScore = 0.5;
            config.requireNoModifiers = false;
            return config;
        }

        public static ScannerConfig strict() {
            ScannerConfig config = new ScannerConfig();
            config.maxInterKeystrokeTime = 30;
            config.maxTimingVariance = 10;
            config.minConfidenceScore = 0.9;
            return config;
        }

        public ScannerConfig withPrefix(String prefix) {
            this.scannerPrefix = prefix;
            return this;
        }

        public ScannerConfig withSuffix(String suffix) {
            this.scannerSuffix = suffix;
            return this;
        }
    }

    public EnhancedBarcodeScannerListener(BiConsumer<String, String> barcodeCallback,
                                          JTextField manualInputField) {
        this(barcodeCallback, manualInputField, ScannerConfig.standard());
    }

    public EnhancedBarcodeScannerListener(BiConsumer<String, String> barcodeCallback,
                                          JTextField manualInputField,
                                          ScannerConfig config) {
        this.barcodeCallback = barcodeCallback;
        this.manualInputField = manualInputField;
        this.config = config;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // Only process KEY_PRESSED for timing, KEY_TYPED for characters
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            handleKeyPressed(e);
            return false;
        }

        if (e.getID() != KeyEvent.KEY_TYPED) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        char c = e.getKeyChar();

        // Check for prefix (if configured)
        if (config.scannerPrefix != null && buffer.length() == 0) {
            if (matchesPrefix(c)) {
                buffer.append(c);
                firstKeyTime = currentTime;
                lastKeyTime = currentTime;
                scannerDetected = true;
                logEvent("SCANNER", "Prefix detected: " + config.scannerPrefix);
                return true; // Consume prefix
            }
        }

        // Reset if too much time has passed
        long timeSinceLastKey = currentTime - lastKeyTime;
        if (timeSinceLastKey > config.maxInterKeystrokeTime * 3 && buffer.length() > 0) {
            if (scannerDetected) {
                logEvent("SCANNER", "Timeout - resetting");
            }
            reset();
        }

        // Record timing
        if (buffer.length() == 0) {
            firstKeyTime = currentTime;
        }
        keystrokeTimings.add(timeSinceLastKey);
        lastKeyTime = currentTime;

        // Handle Enter/Return - evaluate if this was a scanner input
        if (c == '\n' || c == '\r') {
            return handleEnterKey();
        }

        // Accumulate characters
        if (Character.isLetterOrDigit(c) || c == '-') {
            buffer.append(c);

            // Detect scanner based on consistent rapid input
            if (buffer.length() >= 3 && !scannerDetected) {
                scannerDetected = evaluateScannerLikelihood();

                if (scannerDetected) {
                    logEvent("SCANNER", "Scanner detected - clearing manual field");
                    SwingUtilities.invokeLater(() -> {
                        if (manualInputField != null) {
                            manualInputField.setText("");
                        }
                    });
                }
            }

            // Prevent buffer overflow
            if (buffer.length() > config.maxBarcodeLength + 10) {
                logEvent("SCANNER", "Buffer overflow - resetting");
                reset();
            }

            // Consume event if scanner detected and in manual field
            if (scannerDetected && e.getComponent() == manualInputField) {
                return true;
            }
        }

        return false;
    }

    private void handleKeyPressed(KeyEvent e) {
        // Track modifier keys (Shift, Ctrl, Alt)
        if (config.requireNoModifiers) {
            if (e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                modifierKeyCount++;
            }
        }
    }

    private boolean handleEnterKey() {
        if (buffer.length() < config.minBarcodeLength) {
            reset();
            return false;
        }

        // Check for suffix (if configured)
        String barcode = buffer.toString();
        if (config.scannerSuffix != null && barcode.endsWith(config.scannerSuffix)) {
            barcode = barcode.substring(0, barcode.length() - config.scannerSuffix.length());
        }

        // Calculate confidence score
        double confidence = calculateConfidence();

        logEvent("SCANNER", String.format("Input complete - Confidence: %.2f, Length: %d, Time: %dms",
                confidence, buffer.length(), (lastKeyTime - firstKeyTime)));

        if (confidence >= config.minConfidenceScore) {
            // High confidence this is a scanner
            String finalBarcode = barcode;
            SwingUtilities.invokeLater(() -> {
                if (manualInputField != null) {
                    manualInputField.setText("");
                }
                barcodeCallback.accept(finalBarcode, "BARCODE_SCANNER");
            });

            reset();
            return true; // Consume the event
        } else {
            // Low confidence - likely manual input
            logEvent("SCANNER", "Low confidence - treating as manual input");
            reset();
            return false; // Allow manual entry to process
        }
    }

    /**
     * Evaluate if the current input pattern looks like a scanner
     */
    private boolean evaluateScannerLikelihood() {
        if (keystrokeTimings.size() < 3) {
            return false;
        }

        // Check timing consistency
        double avgTiming = keystrokeTimings.stream()
                .skip(1) // Skip first (always 0 or large)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        double variance = keystrokeTimings.stream()
                .skip(1)
                .mapToLong(Long::longValue)
                .mapToDouble(t -> Math.pow(t - avgTiming, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);

        boolean consistentTiming = avgTiming <= config.maxInterKeystrokeTime
                && stdDev <= config.maxTimingVariance;

        if (config.useTimingConsistency) {
            return consistentTiming;
        }

        return avgTiming <= config.maxInterKeystrokeTime;
    }

    /**
     * Calculate confidence score (0.0 to 1.0) that this is scanner input
     */
    private double calculateConfidence() {
        double score = 0.0;
        int factors = 0;

        // Factor 1: Timing consistency (40% weight)
        if (keystrokeTimings.size() > 1) {
            double avgTiming = keystrokeTimings.stream()
                    .skip(1)
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(Double.MAX_VALUE);

            if (avgTiming <= config.maxInterKeystrokeTime) {
                score += 0.4 * (1.0 - (avgTiming / config.maxInterKeystrokeTime));
                factors++;
            }
        }

        // Factor 2: Length pattern (20% weight)
        if (buffer.length() >= config.minBarcodeLength
                && buffer.length() <= config.maxBarcodeLength) {
            score += 0.2;
            factors++;
        }

        // Factor 3: No modifiers (15% weight)
        if (config.requireNoModifiers && modifierKeyCount == 0) {
            score += 0.15;
            factors++;
        }

        // Factor 4: Total input speed (15% weight)
        long totalTime = lastKeyTime - firstKeyTime;
        if (totalTime > config.minTotalInputTime && totalTime < 1000) {
            score += 0.15;
            factors++;
        }

        // Factor 5: Prefix/suffix detection (10% weight)
        if (config.scannerPrefix != null || config.scannerSuffix != null) {
            if (scannerDetected) { // Prefix was detected
                score += 0.1;
                factors++;
            }
        } else {
            // If no prefix/suffix configured, distribute weight to other factors
            score += 0.1 * (factors > 0 ? 1.0 : 0.0);
            factors++;
        }

        return score;
    }

    private boolean matchesPrefix(char c) {
        if (config.scannerPrefix == null) return false;
        return config.scannerPrefix.indexOf(c) == 0;
    }

    public boolean isScannerInputActive() {
        return scannerDetected;
    }

    public void reset() {
        buffer.setLength(0);
        keystrokeTimings.clear();
        scannerDetected = false;
        modifierKeyCount = 0;
        firstKeyTime = 0;
        lastKeyTime = 0;
    }

    public String getBufferContents() {
        return buffer.toString();
    }

    public void updateConfig(ScannerConfig newConfig) {
        this.config = newConfig;
        logEvent("CONFIG", "Scanner configuration updated");
    }

    private void logEvent(String source, String message) {
        System.out.println("[" + source + "] " + message);
    }
}