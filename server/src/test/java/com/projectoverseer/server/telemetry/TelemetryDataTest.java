package com.projectoverseer.server.telemetry;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TelemetryDataTest {

    @Test
    void testComplexSerializationMatchesPythonSchema() {
        // Arrange
        TelemetryData data = new TelemetryData();
        data.tick = 4;

        data.world = new TelemetryData.WorldTelemetry();
        data.world.day = 2;
        data.world.elapsed_seconds = 120.5f;
        data.world.phase = "night";

        data.players = new TelemetryData.PlayerTelemetry();
        data.players.alive = 3;
        data.players.average_health = 75.0f;
        data.players.average_equipment_level = 1.0f;
        data.players.deaths_last_5_minutes = 1;
        data.players.kills_last_5_minutes = 5;

        // Act
        Gson gson = new Gson();
        String json = gson.toJson(data);

        // Assert
        assertNotNull(json, "JSON should not be null");
        assertTrue(json.contains("\"match_id\":\"dev-match-1\""), "Should contain default match ID");
        assertTrue(json.contains("\"tick\":4"), "Should contain the correct tick");
        assertTrue(json.contains("\"phase\":\"night\""), "World telemetry should contain phase");
        assertTrue(json.contains("\"average_health\":75.0"), "Player telemetry should contain average health");
        assertTrue(json.contains("\"deaths_last_5_minutes\":1"), "Player telemetry should contain recent deaths");
    }
}
