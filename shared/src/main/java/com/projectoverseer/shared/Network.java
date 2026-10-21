package com.projectoverseer.shared;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Network {
    public static final Properties CONFIG = loadConfig();
    public static final int TCP_PORT = Integer.parseInt(CONFIG.getProperty("server.tcpPort", "54555"));
    public static final int UDP_PORT = Integer.parseInt(CONFIG.getProperty("server.udpPort", "54777"));
    public static final String SERVER_HOST = CONFIG.getProperty("server.host", "localhost");
    public static final int POSITION_UPDATE_INTERVAL_MS = Integer.parseInt(
        CONFIG.getProperty("network.positionUpdateIntervalMs", "50"));

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
    }

    public static class Ping {
        public String message;
    }

    public static class Welcome {
        public int playerId;
    }

    /** Client to server: the sender's latest world position. */
    public static class PositionUpdate {
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
}
