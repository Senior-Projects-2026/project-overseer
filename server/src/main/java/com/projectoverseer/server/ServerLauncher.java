package com.projectoverseer.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;

import java.io.IOException;

/** Launches the server application. */
public class ServerLauncher {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();

        Network.register(server);

        server.bind(Network.TCP_PORT, Network.UDP_PORT);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("SERVER: Received from client -> " + ping.message);

                    Ping response = new Ping();
                    response.message = "Pong from server, I hear you perfectly!";
                    connection.sendTCP(response);
                }
            }
        });

        System.out.println("SERVER: Successfully started and waiting for connections...");
    }
}
