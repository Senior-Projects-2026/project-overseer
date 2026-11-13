package com.projectoverseer.server.telemetry;

public class TelemetryData {
    public String match_id = "dev-match-1";
    public int tick;
    public PlayerTelemetry players;
    public EnemyTelemetry enemies;
    public WorldTelemetry world;
    public PerformanceTelemetry performance;

    public static class PlayerTelemetry {
        public int alive;
        public float average_health;
        public int deaths_last_5_minutes;
        public int kills_last_5_minutes;
        public float average_equipment_level;
    }

    public static class EnemyTelemetry {
        public int alive;
        public int killed_last_5_minutes;
    }

    public static class WorldTelemetry {
        public int day;
        public float elapsed_seconds;
        public String phase; // "day" or "night"
    }

    public static class PerformanceTelemetry {
        public float server_tick_ms;
        public float server_load;
    }
}
