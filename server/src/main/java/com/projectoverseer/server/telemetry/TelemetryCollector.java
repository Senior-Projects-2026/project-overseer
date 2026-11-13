package com.projectoverseer.server.telemetry;

import com.google.gson.Gson;
import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.server.combat.CombatPlayer;
import com.projectoverseer.shared.combat.CombatRules;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;

/**
 * Periodically collects game state data (team health, weapons, enemy presence)
 * and sends an asynchronous HTTP POST request to the Python Overseer service.
 * This acts as the "eyes" of the AI Director, keeping it informed without blocking the KryoNet server loop.
 */
public class TelemetryCollector {
    private static final String PYTHON_API_URL = "http://localhost:8000/overseer/analyze";
    private final HttpClient httpClient;
    private final Gson gson;
    private float timer = 0f;
    private static final float COLLECTION_INTERVAL = 30f;

    public TelemetryCollector() {
        httpClient = HttpClient.newBuilder().build();
        this.gson = new Gson();
    }

    public void update(float delta, int currentDay, int enemiesCount, Map<Integer, CombatPlayer> players, Map<Integer, Inventory> inventories) {
        timer += delta;
        if (timer >= COLLECTION_INTERVAL) {
            timer = 0f;

            TelemetryData data = new TelemetryData();
            data.dayNumber = currentDay;
            data.activeEnemiesCount = enemiesCount;
            data.players = new ArrayList<>();

            for (Map.Entry<Integer, CombatPlayer> entry : players.entrySet()) {
                int id = entry.getKey();
                CombatPlayer cp = entry.getValue();
                Inventory inv = inventories.get(id);

                boolean hasGoodWeapon = false;
                if (inv != null) {
                    // Simplification
                    hasGoodWeapon = inv.getSlot(inv.getSelectedSlot()) != null;
                }

                data.players.add(new TelemetryData.PlayerStat(id, cp.health(), CombatRules.PLAYER_MAX_HEALTH, hasGoodWeapon));
            }

            String jsonPayload = gson.toJson(data);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("OVERSEER: Received response -> " + response.body());
                        // Ticket #57: process Python response
                    } else {
                        System.err.println("OVERSEER: HTTP error: " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    // If server is down (Connection refused)
                    System.out.println("OVERSEER: Could not connect to Python (" + e.getMessage() + ")");
                    return null;
                });
        }
    }
}
