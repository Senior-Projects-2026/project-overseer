package com.projectoverseer.server.telemetry;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class TelemetryDataTest {

    @Test
    void testSerializationCreatesCorrectJson() {
        // Arrange: Prepare mock data
        TelemetryData data = new TelemetryData();
        data.dayNumber = 5;
        data.activeEnemiesCount = 12;
        data.players = new ArrayList<>();
        data.players.add(new TelemetryData.PlayerStat(1, 75.0f, 100.0f, true));

        // Act: Serialize to JSON
        Gson gson = new Gson();
        String json = gson.toJson(data);

        // Assert: Verify the JSON contains the correct keys and values
        assertNotNull(json, "The generated JSON should not be null");
        assertTrue(json.contains("\"dayNumber\":5"), "JSON should contain the correct day number");
        assertTrue(json.contains("\"activeEnemiesCount\":12"), "JSON should contain the active enemies count");
        assertTrue(json.contains("\"playerId\":1"), "JSON should contain the player ID");
        assertTrue(json.contains("\"hasWeapon\":true"), "JSON should contain the weapon status");
    }
}
