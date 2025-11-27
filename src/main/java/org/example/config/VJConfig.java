package org.example.config;

import java.io.*;
import java.util.Properties;

public class VJConfig {
    private static final String CONFIG_FILE = "src/main/resources/vj-config.properties";
    private final Properties properties;

    public VJConfig() {
        properties = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            System.out.println("VJ Configuration loaded from: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Failed to load VJ config, using defaults: " + e.getMessage());
            setDefaults();
        }
    }

    private void setDefaults() {
        properties.setProperty("vj.server.host", "localhost");
        properties.setProperty("vj.server.port", "9999");
        properties.setProperty("vj.connection.retry.enabled", "true");
        properties.setProperty("vj.connection.retry.max-attempts", "5");
        properties.setProperty("vj.connection.retry.delay-seconds", "5");
    }

    public String getServerHost() {
        return properties.getProperty("vj.server.host", "localhost");
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("vj.server.port", "9999"));
    }

    public boolean isRetryEnabled() {
        return Boolean.parseBoolean(properties.getProperty("vj.connection.retry.enabled", "true"));
    }

    public int getMaxRetryAttempts() {
        return Integer.parseInt(properties.getProperty("vj.connection.retry.max-attempts", "5"));
    }

    public int getRetryDelaySeconds() {
        return Integer.parseInt(properties.getProperty("vj.connection.retry.delay-seconds", "5"));
    }

    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Virtual Journal Configuration");
            System.out.println("Configuration saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void setServerHost(String host) {
        properties.setProperty("vj.server.host", host);
    }

    public void setServerPort(int port) {
        properties.setProperty("vj.server.port", String.valueOf(port));
    }

    public void displayConfig() {
        System.out.println("=".repeat(60));
        System.out.println("Virtual Journal Configuration:");
        System.out.println("  Host: " + getServerHost());
        System.out.println("  Port: " + getServerPort());
        System.out.println("  Retry Enabled: " + isRetryEnabled());
        System.out.println("  Max Retry Attempts: " + getMaxRetryAttempts());
        System.out.println("  Retry Delay: " + getRetryDelaySeconds() + "s");
        System.out.println("=".repeat(60));
    }
}
