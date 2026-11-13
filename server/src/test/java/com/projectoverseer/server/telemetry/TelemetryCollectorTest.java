package com.projectoverseer.server.telemetry;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.server.combat.CombatPlayer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TelemetryCollectorTest {

    @Test
    void testUpdateDoesNotThrowExceptions() {
        // Arrange
        TelemetryCollector collector = new TelemetryCollector();
        Map<Integer, CombatPlayer> players = new HashMap<>();
        Map<Integer, Inventory> inventories = new HashMap<>();

        // Add a mock player to prevent sending empty collections
        players.put(1, new CombatPlayer(1));
        inventories.put(1, new Inventory());

        // Act & Assert
        assertDoesNotThrow(() -> {
            // Simulate 10 seconds passing (Should NOT trigger the HTTP request)
            collector.update(10f, 1, 0, players, inventories);

            // Simulate 25 more seconds (Total 35s > 30s threshold. SHOULD trigger the HTTP request)
            // We expect this to NOT throw an exception, even if the Python server is offline
            collector.update(25f, 1, 0, players, inventories);
        }, "The update method should gracefully handle HTTP errors without crashing the server");
    }
}
