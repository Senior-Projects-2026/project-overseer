package com.projectoverseer.server.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    @Test
    void testLoadsExistingPropertiesFromFile() {
        assertEquals("http://localhost:8000/evaluate",
            ServerConfig.getString("overseer.api.url", "fallback"),
            "Should load overseer.api.url from server.properties");
        assertEquals(54555, ServerConfig.getInt("server.tcp.port", 0),
            "Should load server.tcp.port from server.properties");
        assertEquals(123L, ServerConfig.getLong("world.seed", 0L),
            "Should load world.seed from server.properties");
        assertEquals(30.0f, ServerConfig.getFloat("overseer.collection.interval", 0f), 0.001f,
            "Should load overseer.collection.interval from server.properties");
    }

    @Test
    void testReturnsFallbackDefaultsForMissingKeys() {
        assertEquals("fallback", ServerConfig.getString("non.existent.string", "fallback"));
        assertEquals(8080, ServerConfig.getInt("non.existent.int", 8080));
        assertEquals(999999L, ServerConfig.getLong("non.existent.long", 999999L));
        assertEquals(2.5f, ServerConfig.getFloat("non.existent.float", 2.5f), 0.001f);
        assertFalse(ServerConfig.getBoolean("non.existent.boolean", false));
    }

    @Test
    void testHandlesInvalidNumberFormatsGracefully() {
        // "overseer.api.url" is a URL string, so numeric parsers should catch NumberFormatException and return defaults
        assertEquals(1234, ServerConfig.getInt("overseer.api.url", 1234),
            "Should return default int when property is not a valid integer");
        assertEquals(5678L, ServerConfig.getLong("overseer.api.url", 5678L),
            "Should return default long when property is not a valid long");
        assertEquals(0.75f, ServerConfig.getFloat("overseer.api.url", 0.75f), 0.001f,
            "Should return default float when property is not a valid float");
    }

    @Test
    void testSystemPropertyOverridesFileConfiguration() {
        String portKey = "server.tcp.port";
        String boolKey = "test.server.feature.enabled";
        try {
            System.setProperty(portKey, "60000");
            System.setProperty(boolKey, "true");

            assertEquals(60000, ServerConfig.getInt(portKey, 54555),
                "System property should override server.properties value");
            assertTrue(ServerConfig.getBoolean(boolKey, false),
                "System property should resolve boolean flag");
        } finally {
            System.clearProperty(portKey);
            System.clearProperty(boolKey);
        }
    }
}
