package com.projectoverseer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;

import java.io.IOException;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    Client client;
    SpriteBatch batch;
    Texture smiley;
    boolean isConnected = false;

    @Override
    public void create() {
        client = new Client();
        client.start();

        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("CLIENT: Received from server -> " + ping.message);
                }
            }
        });

        try {
            client.connect(5000, "localhost", Network.TCP_PORT, Network.UDP_PORT);

            Ping myPing = new Ping();
            myPing.message = "Hello Server, I am a new client!";
            client.sendTCP(myPing);

            isConnected = true;

        } catch (IOException e) {
            System.err.println("CLIENT: Could not connect to server!");
            e.printStackTrace();
        }

        batch = new SpriteBatch();
        smiley = new Texture("smiley.png");
    }

    @Override
    public void render() {
        if (isConnected) {
            ScreenUtils.clear(0.2f, 0.6f, 1f, 1);
            batch.begin();
            batch.draw(smiley, 50, 0);
            batch.end();
        } else {
            ScreenUtils.clear(0.8f, 0, 0, 1);
        }
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.stop();
        }

        if (batch != null) batch.dispose();
        if (smiley != null) smiley.dispose();
    }
}
