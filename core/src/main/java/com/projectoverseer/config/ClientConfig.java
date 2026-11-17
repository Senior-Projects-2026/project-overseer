package com.projectoverseer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads client-side configuration values from client.properties with safe fallback defaults.
 */
public final class ClientConfig {
    private static final String CONFIG_FILE = "client.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ClientConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            } else {
                System.err.println("CLIENT CONFIG: " + CONFIG_FILE + " not found, using fallback defaults.");
            }
        } catch (IOException e) {
            System.err.println("CLIENT CONFIG: Failed to load " + CONFIG_FILE + ", using fallback defaults: " + e.getMessage());
        }
    }

    private ClientConfig() {}

    public static String getString(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("CLIENT CONFIG: Invalid int for key '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static float getFloat(String key, float defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("CLIENT CONFIG: Invalid float for key '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = PROPERTIES.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }
}
