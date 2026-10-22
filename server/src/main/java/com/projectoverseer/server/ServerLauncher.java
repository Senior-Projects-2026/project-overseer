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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Launches the server application. */
public class ServerLauncher {
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
    }
}
