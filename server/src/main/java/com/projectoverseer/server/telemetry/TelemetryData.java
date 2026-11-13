package com.projectoverseer.server.telemetry;

import java.util.List;

/**
 * Data Transfer Object (DTO) used to serialize game state into JSON.
 * It contains the overall tension metrics (day, enemy counts) and a breakdown
 * of individual player conditions that the Python script will analyze.
 */
public class TelemetryData {
    public int dayNumber;
    public int activeEnemiesCount;
    public List<PlayerStat> players;

    public static class PlayerStat {
        public int playerId;
        public float health;
        public float maxHealth;
        public boolean hasWeapon;

        public PlayerStat(int playerId, float health, float maxHealth, boolean hasWeapon) {
            this.playerId = playerId;
            this.health = health;
            this.maxHealth = maxHealth;
            this.hasWeapon = hasWeapon;
        }
    }
}
