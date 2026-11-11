package com.projectoverseer.shared;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Network {
    // 1. Constants and configuration
    public static final Properties CONFIG = loadConfig();
    public static final int TCP_PORT = Integer.parseInt(CONFIG.getProperty("server.tcpPort", "54555"));
    public static final int UDP_PORT = Integer.parseInt(CONFIG.getProperty("server.udpPort", "54777"));
    public static final String SERVER_HOST = CONFIG.getProperty("server.host", "localhost");
    public static final int POSITION_UPDATE_INTERVAL_MS = Integer.parseInt(
        CONFIG.getProperty("network.positionUpdateIntervalMs", "50"));

    // 2. Utils (Config and Kryo register)
    private static Properties loadConfig() {
        Properties properties = new Properties();
        File localConfig = new File("config.properties");
        File parentConfig = new File("..", "config.properties");
        File externalConfig = localConfig.isFile() ? localConfig : parentConfig;
        if (externalConfig.isFile()) {
            try (FileInputStream input = new FileInputStream(externalConfig)) {
                properties.load(input);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        for (String key : properties.stringPropertyNames()) {
            String override = System.getProperty(key);
            if (override != null) properties.setProperty(key, override);
        }
        return properties;
    }

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(Ping.class);
        kryo.register(Welcome.class);
        kryo.register(PositionUpdate.class);
        kryo.register(PlayerPosition.class);
        kryo.register(PlayerLeft.class);
        kryo.register(TimeSync.class);
        kryo.register(GameEnd.class);

        kryo.register(PunchTree.class);
        kryo.register(TreeRemoved.class);
        kryo.register(TreeHit.class);

        kryo.register(PunchRock.class);
        kryo.register(RockHit.class);
        kryo.register(RockRemoved.class);

        kryo.register(String[].class);
        kryo.register(int[].class);
        kryo.register(InventorySync.class);

        kryo.register(CraftRequest.class);
        kryo.register(CraftResult.class);
        kryo.register(AttackRequest.class);
        kryo.register(AttackAccepted.class);
        kryo.register(PlayerHealth.class);

    }

    // 3. DTOs
    /** Server to player: authoritative health; position corrects movement on death. */
    public static class PlayerHealth {
        public int playerId;
        public float health, x, y;
        public PlayerHealth() {}
    }

    public static class AttackRequest {
        public int selectedSlot;
        public float aimX, aimY;
        public AttackRequest() {}
        public AttackRequest(int selectedSlot, float aimX, float aimY) {
            this.selectedSlot = selectedSlot;
            this.aimX = aimX;
            this.aimY = aimY;
        }
    }

    /** Server-approved swing, also usable by the future animation system. */
    public static class AttackAccepted {
        public int playerId;
        public String itemId;
        public float originX, originY, directionX, directionY;
        public AttackAccepted() {}
    }

    // Utils
    public static class Ping {
        public String message;
    }

    public static class Welcome {
        public int playerId;
        public long worldSeed;
        public int generatorVersion;
    }

    // Movement
    /** Client to server: the sender's latest world position. */
    public static class PositionUpdate {
        public long movementRevision;
        public float x;
        public float y;
        public int direction;
        public boolean moving;

        public PositionUpdate() {}
        public PositionUpdate(float x, float y, int direction, boolean moving) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.moving = moving;
        }
    }

    /** Server to clients: a player's latest world position. */
    public static class PlayerPosition {
        public long movementRevision;
        public boolean correction;
        public int playerId;
        public float x;
        public float y;
        public int direction;
        public boolean moving;

        public PlayerPosition() {}
        public PlayerPosition(int playerId, float x, float y, int direction, boolean moving) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.moving = moving;
        }
    }

    public static class PlayerLeft {
        public int playerId;
        public PlayerLeft() {}
        public PlayerLeft(int playerId) { this.playerId = playerId; }
    }

    // Game state

    public static class TimeSync {
        public int currentDay;
        public boolean isNight;
    }

    public static class GameEnd {
        public boolean isVictory;
    }

    public static class PunchTree {
        public int tileX;
        public int tileY;
        public int selectedSlot;

        public PunchTree() {}

        public PunchTree(int tileX, int tileY,int selectedSlot) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.selectedSlot = selectedSlot;
        }
    }

    public static class TreeRemoved {
        public int tileX;
        public int tileY;

        public TreeRemoved() {}

        public TreeRemoved(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }

    public static class TreeHit {
        public int tileX;
        public int tileY;
        public int hitCount;

        public TreeHit() {}

        public TreeHit(int tileX, int tileY, int hitCount) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.hitCount = hitCount;
        }
    }

    public static class PunchRock {
        public int tileX;
        public int tileY;
        public int selectedSlot;

        public PunchRock() {}

        public PunchRock(int tileX, int tileY, int selectedSlot) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.selectedSlot = selectedSlot;
        }
    }

    public static class RockHit {
        public int tileX;
        public int tileY;
        public int hitCount;

        public RockHit() {}

        public RockHit(int tileX, int tileY, int hitCount) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.hitCount = hitCount;
        }
    }

    public static class RockRemoved {                                                                                                                                                       //🐐
        public int tileX;
        public int tileY;

        public RockRemoved() {}

        public RockRemoved(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }

    public static class InventorySync {

        public String[] itemIds;
        public int[] quantities;

        public InventorySync() {
        }
    }


    public static class CraftRequest {
        public String itemId;

        public CraftRequest() {
        }

        public CraftRequest(String itemId) {
            this.itemId = itemId;
        }
    }

    public static class CraftResult {
        public String itemId;
        public boolean success;

        public CraftResult() {
        }

        public CraftResult(String itemId, boolean success) {
            this.itemId = itemId;
            this.success = success;
        }
    }



}
