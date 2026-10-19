package com.projectoverseer.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.shared.Network.PlayerLeft;
import com.projectoverseer.shared.Network.PlayerPosition;
import com.projectoverseer.shared.Network.PositionUpdate;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.shared.Network.TimeSync;
import com.projectoverseer.shared.Network.GameEnd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Launches the server application. */
public class ServerLauncher {
    //todo: server.properties for the ones below
    private static float timeTimer = 0;
    private static int day = 1;
    private static float dayLengthSeconds = 10;
    private static float nightThreshold = 0.7f;
    private static int winThreshold = 5;

    private static int serverSleepTime = 50;
    private static float serverSyncInterval = 1f;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        Network.register(server);
        server.bind(Network.TCP_PORT, Network.UDP_PORT);

        Map<Integer, PlayerPosition> players = new HashMap<>();

        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Welcome welcome = new Welcome();
                welcome.playerId = connection.getID();
                connection.sendTCP(welcome);
                for (PlayerPosition player : players.values()) {
                    connection.sendTCP(player);
                }
                System.out.println("SERVER: Player " + connection.getID() + " connected.");
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("SERVER: Received from client -> " + ping.message);

                    Ping response = new Ping();
                    response.message = "Pong from server, I hear you perfectly!";
                    connection.sendTCP(response);
                } else if (object instanceof PositionUpdate) {
                    PositionUpdate update = (PositionUpdate) object;
                    PlayerPosition position = new PlayerPosition(
                        connection.getID(), update.x, update.y, update.direction, update.moving);
                    players.put(connection.getID(), position);
                    for (Connection recipient : server.getConnections()) {
                        if (recipient.getID() != connection.getID()) recipient.sendUDP(position);
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                players.remove(connection.getID());
                PlayerLeft left = new PlayerLeft(connection.getID());
                for (Connection recipient : server.getConnections()) recipient.sendTCP(left);
                System.out.println("SERVER: Player " + connection.getID() + " disconnected.");
            }
        });

        server.start();

        System.out.println("SERVER: Listening on TCP " + Network.TCP_PORT + " and UDP " + Network.UDP_PORT + "...");

        float syncTimer = 0f;
        long lastTime = System.nanoTime();

        while (true) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;

            update(delta);
            syncTimer += delta;
            try {
                Thread.sleep(serverSleepTime); // Target ~20 TPS
            } catch (InterruptedException e) {
                System.err.println("SERVER: Thread interrupted!");
                Thread.currentThread().interrupt();
                break;
            }

            if (syncTimer >= serverSyncInterval) {
                syncTimer = 0f;

                boolean isNight = timeTimer > dayLengthSeconds * nightThreshold;
                Network.TimeSync syncPacket = new TimeSync();
                syncPacket.currentDay = day;
                syncPacket.isNight = isNight;
                server.sendToAllTCP(syncPacket);

                if (day > winThreshold) {
                    Network.GameEnd endPacket = new GameEnd();
                    endPacket.isVictory = true;
                    server.sendToAllTCP(endPacket);
                    System.out.println("SERVER: " + winThreshold + " days survived. Victory packet sent!");

                    break;
                }

            }
        }
    }

    public static void update(float delta) {
        timeTimer += delta;

        if (timeTimer >= dayLengthSeconds) {
            timeTimer = 0;
            day++;
            System.out.println("SERVER: A new day begins! Day " + day);
        }
    }
}
