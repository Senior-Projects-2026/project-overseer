package com.projectoverseer.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClientConfigTest {

    @Test
    public void testLoadsExistingPropertiesFromFile() {
        assertEquals("Should load server.host from client.properties",
            "localhost", ClientConfig.getString("server.host", "127.0.0.1"));
        assertEquals("Should load server.tcp.port from client.properties",
            54555, ClientConfig.getInt("server.tcp.port", 0));
        assertEquals("Should load player.movement.speed from client.properties",
            100.0f, ClientConfig.getFloat("player.movement.speed", 0f), 0.001f);
    }

    @Test
    public void testReturnsFallbackDefaultsForMissingKeys() {
        assertEquals("default-val", ClientConfig.getString("non.existent.string", "default-val"));
        assertEquals(42, ClientConfig.getInt("non.existent.int", 42));
        assertEquals(3.14f, ClientConfig.getFloat("non.existent.float", 3.14f), 0.001f);
        assertTrue(ClientConfig.getBoolean("non.existent.boolean", true));
    }

    @Test
    public void testHandlesInvalidNumberFormatsGracefully() {
        assertEquals("Should return default int when property value is not a valid integer",
            9999, ClientConfig.getInt("server.host", 9999));
        assertEquals("Should return default float when property value is not a valid float",
            1.5f, ClientConfig.getFloat("server.host", 1.5f), 0.001f);
    }
}
