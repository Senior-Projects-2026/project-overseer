package com.projectoverseer.server.telemetry;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.server.combat.CombatPlayer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TelemetryCollectorTest {

    @Test
    void testUpdateAndEventTrackingDoesNotThrowExceptions() {
        // Arrange
        TelemetryCollector collector = new TelemetryCollector();
        Map<Integer, CombatPlayer> players = new HashMap<>();
        Map<Integer, Inventory> inventories = new HashMap<>();

        // Add a mock player
        players.put(1, new CombatPlayer(1));
        inventories.put(1, new Inventory());

        // Act & Assert
        assertDoesNotThrow(() -> {
            // Simulate events happening in the game loop
            collector.recordPlayerDeath();
            collector.recordEnemyKill();
            collector.recordEnemyKill();

            // Simulate 10 seconds passing (Daytime)
            collector.update(10f, 1, false, 0, players, inventories);

            // Simulate 25 more seconds (Nighttime, triggers HTTP request)
            collector.update(25f, 1, true, 0, players, inventories);
        }, "The collector should gracefully handle updates and event tracking without crashing");
    }
}
