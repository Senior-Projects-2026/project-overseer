package com.projectoverseer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
    private enum GameState {
        MENU,
        CONNECTING,
        PLAYING
    }

    private Client client;
    private SpriteBatch batch;
    private MainMenu menu;
    OrthographicCamera camera;

    private volatile boolean isConnected;
    private GameState gameState = GameState.MENU;

    Player player;

    Player player;

    @Override
    public void create() {
        batch = new SpriteBatch();

        createClient();
        menu = new MainMenu(this::connectToServer);
        menu.show("Choose Play to connect to the server.");
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        camera.position.set(w / 2f, h / 2f, 0);
        camera.update();
    }

    private void createClient() {
        client = new Client();
        client.start();

        Network.register(client);

        player = new Player();

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("CLIENT: Received from server -> " + ping.message);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("CLIENT: Lost connection to server!");
                isConnected = false;
            }
        });
    }

    private void connectToServer() {
        if (gameState == GameState.CONNECTING || isConnected) {
            return;
        }

        gameState = GameState.CONNECTING;
        menu.show("Connecting to localhost...");
        menu.hide();

        new Thread(() -> {
            try {
                client.connect(5000, "localhost", Network.TCP_PORT, Network.UDP_PORT);

                Ping myPing = new Ping();
                myPing.message = "Hello Server, I am a new client!";
                client.sendTCP(myPing);

                isConnected = true;
                Gdx.app.postRunnable(() -> gameState = GameState.PLAYING);
            } catch (IOException e) {
                System.err.println("CLIENT: Could not connect to server!");
                e.printStackTrace();
                Gdx.app.postRunnable(() -> showMenu("Could not connect. Is the server running?"));
            }
        }, "kryonet-connect").start();
    }

    private void showMenu(String status) {
        isConnected = false;
        gameState = GameState.MENU;
        menu.show(status);


    }

    @Override
    public void render() {
        camera.update();

        if (gameState == GameState.MENU || gameState == GameState.CONNECTING) {
            menu.render(Gdx.graphics.getDeltaTime());
            return;
        }

        player.update();

            ScreenUtils.clear(0f, 0.6f, 0f, 1f);
            batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        batch.end();
            player.update();

            ScreenUtils.clear(0f, 0.6f, 0f, 1);
            batch.begin();
            player.render(batch);
            batch.end();
        } else {
            ScreenUtils.clear(0.8f, 0, 0, 1);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.position.set(width / 2f, height / 2f, 0);
        camera.update();
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
