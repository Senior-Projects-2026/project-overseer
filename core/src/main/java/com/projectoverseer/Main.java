package com.projectoverseer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.projectoverseer.entities.Player;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.ui.MainMenu;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.world.DemoWorld;
import com.projectoverseer.world.TileRenderer;

import java.io.IOException;
import java.util.List;

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
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Player player;
    private TileRenderer tileRenderer;
    private List<Chunk> chunks;

    private volatile boolean isConnected;
    private GameState gameState = GameState.MENU;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 360, camera);
        tileRenderer = new TileRenderer();
        chunks = DemoWorld.createChunks();

        createClient();
        menu = new MainMenu(this::connectToServer);
        menu.show("Choose Play to connect to the server.");
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
        if (gameState == GameState.MENU || gameState == GameState.CONNECTING) {
            menu.render(Gdx.graphics.getDeltaTime());
            return;
        }

        player.update();

        float deadzoneWidth = 100f;
        float deadzoneHeight = 80f;

        float leftBoundary = camera.position.x - deadzoneWidth;
        float rightBoundary = camera.position.x + deadzoneWidth;
        float bottomBoundary = camera.position.y - deadzoneHeight;
        float topBoundary = camera.position.y + deadzoneHeight;

        float playerX = player.getX()+20;
        float playerY = player.getY()+20;

        if (playerX < leftBoundary) {
            camera.position.x = playerX + deadzoneWidth;
        } else if (playerX > rightBoundary) {
            camera.position.x = playerX - deadzoneWidth;
        }

        if (playerY < bottomBoundary) {
            camera.position.y = playerY + deadzoneHeight;
        } else if (playerY > topBoundary) {
            camera.position.y = playerY - deadzoneHeight;
        }

        camera.update();

        ScreenUtils.clear(0.08f, 0.10f, 0.12f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileRenderer.render(batch, chunks, camera);
        player.render(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (menu != null) {
            menu.resize(width, height);
        }

        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.stop();
        }
        if (menu != null) menu.dispose();
        if (batch != null) batch.dispose();
        if (player != null) player.dispose();
        if (tileRenderer != null) tileRenderer.dispose();
    }
}
