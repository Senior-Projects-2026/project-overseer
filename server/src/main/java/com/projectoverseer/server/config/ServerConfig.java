package com.projectoverseer.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads server-side configuration values from server.properties with safe fallback defaults.
 */
public final class ServerConfig {
    private static final String CONFIG_FILE = "server.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ServerConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            } else {
                System.err.println("SERVER CONFIG: " + CONFIG_FILE + " not found, using fallback defaults.");
            }
        } catch (IOException e) {
            System.err.println("SERVER CONFIG: Failed to load " + CONFIG_FILE + ", using fallback defaults: " + e.getMessage());
        }
    }

    private ServerConfig() {}

    private static String resolve(String key) {
        String sysOverride = System.getProperty(key);
        return sysOverride != null ? sysOverride : PROPERTIES.getProperty(key);
    }

    public static String getString(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value.trim() : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = resolve(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("SERVER CONFIG: Invalid int for key '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = resolve(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("SERVER CONFIG: Invalid long for key '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static float getFloat(String key, float defaultValue) {
        String value = resolve(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("SERVER CONFIG: Invalid float for key '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = resolve(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }
}
