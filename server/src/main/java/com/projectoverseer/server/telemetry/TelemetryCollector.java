package com.projectoverseer.server.telemetry;

import com.google.gson.Gson;
import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.server.combat.CombatPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Periodically collects game state data
 * and sends an asynchronous HTTP POST request to the Python Overseer service.
 * This acts as the "eyes" of the AI Director, keeping it informed without blocking the KryoNet server loop.
 */
public class TelemetryCollector {
    private static final String PYTHON_API_URL = "http://localhost:8000/evaluate";
    private static final float COLLECTION_INTERVAL = 30f;
    // 5 minutes in nanoseconds (5 * 60 * 1 billion)
    private static final long FIVE_MINUTES_NS = 300_000_000_000L;

    private final HttpClient httpClient;
    private final Gson gson;
    private float timer = 0f;
    private int currentTick = 0;

    // --- Sliding Window Event Trackers ---
    private final List<Long> playerDeathTimestamps = new ArrayList<>();
    private final List<Long> enemyKillTimestamps = new ArrayList<>();

    public TelemetryCollector() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.gson = new Gson();
    }

    // Called from ServerLauncher when a player dies
    public void recordPlayerDeath() {
        playerDeathTimestamps.add(System.nanoTime());
    }

    // Called from ServerLauncher when an enemy is killed
    public void recordEnemyKill() {
        enemyKillTimestamps.add(System.nanoTime());
    }

    private void cleanOldEvents(long currentTimeNs) {
        playerDeathTimestamps.removeIf(timestamp -> (currentTimeNs - timestamp) > FIVE_MINUTES_NS);
        enemyKillTimestamps.removeIf(timestamp -> (currentTimeNs - timestamp) > FIVE_MINUTES_NS);
    }


    public void update(float delta, int currentDay, boolean isNight, int enemiesCount, Map<Integer, CombatPlayer> players, Map<Integer, Inventory> inventories) {
        timer += delta;
        if (timer >= COLLECTION_INTERVAL) {
            timer = 0f;
            currentTick++;

            long currentTimeNs = System.nanoTime();
            cleanOldEvents(currentTimeNs);

            TelemetryData data = new TelemetryData();
            data.tick = currentTick;

            // --- WORLD ---
            data.world = new TelemetryData.WorldTelemetry();
            data.world.day = currentDay;
            data.world.elapsed_seconds = currentTick * COLLECTION_INTERVAL;
            data.world.phase = isNight ? "night" : "day";

            // --- PLAYERS ---
            data.players = new TelemetryData.PlayerTelemetry();
            float totalHealth = 0;
            int aliveCount = 0;
            float equipmentScore = 0;

            for (Map.Entry<Integer, CombatPlayer> entry : players.entrySet()) {
                CombatPlayer cp = entry.getValue();
                if (cp.isAlive()) {
                    aliveCount++;
                    totalHealth += cp.health();
                    // Simplified
                    Inventory inv = inventories.get(entry.getKey());
                    if (inv != null && inv.getSlot(inv.getSelectedSlot()) != null) {
                        equipmentScore += 1.0f;
                    }
                }
            }

            data.players.alive = aliveCount;
            data.players.average_health = aliveCount > 0 ? (totalHealth / aliveCount) : 0;
            data.players.average_equipment_level = aliveCount > 0 ? (equipmentScore / aliveCount) : 0;

            // Fetch sliding window sizes
            data.players.deaths_last_5_minutes = playerDeathTimestamps.size();
            data.players.kills_last_5_minutes = enemyKillTimestamps.size();


            // --- ENEMIES & PERFORMANCE (Mock-uri pentru moment) ---
            data.enemies = new TelemetryData.EnemyTelemetry();
            data.enemies.alive = enemiesCount;
            data.enemies.killed_last_5_minutes = enemyKillTimestamps.size();

            data.performance = new TelemetryData.PerformanceTelemetry();
            data.performance.server_tick_ms = 50.0f; // 20 TPS
            data.performance.server_load = 0.5f;

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
