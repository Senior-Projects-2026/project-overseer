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
import com.projectoverseer.shared.Network.PlayerLeft;
import com.projectoverseer.shared.Network.PlayerPosition;
import com.projectoverseer.shared.Network.PositionUpdate;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.ui.MainMenu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<Integer, Player> otherPlayers = new HashMap<>();

    private volatile boolean isConnected;
    private volatile int playerId = -1;
    private float positionUpdateTimer;
    private GameState gameState = GameState.MENU;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 360, camera);

        createClient();
        menu = new MainMenu(this::connectToServer);
        menu.show("Choose Play to connect to the server.");
    }

    private void createClient() {
        client = new Client();
        client.start();
        Network.register(client);

        player = new Player(true);

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("CLIENT: Received from server -> " + ping.message);
                } else if (object instanceof Welcome) {
                    playerId = ((Welcome) object).playerId;
                    System.out.println("CLIENT: Assigned player ID " + playerId);
                } else if (object instanceof PlayerPosition) {
                    PlayerPosition position = (PlayerPosition) object;
                    if (position.playerId != playerId) {
                        Gdx.app.postRunnable(() -> {
                            Player remote = otherPlayers.computeIfAbsent(
                                position.playerId, id -> new Player(false));
                            remote.setRemoteState(position.x, position.y, position.direction, position.moving);
                        });
                    }
                } else if (object instanceof PlayerLeft) {
                    int leftId = ((PlayerLeft) object).playerId;
                    Gdx.app.postRunnable(() -> {
                        Player removed = otherPlayers.remove(leftId);
                        if (removed != null) removed.dispose();
                    });
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("CLIENT: Lost connection to server!");
                isConnected = false;
                playerId = -1;
                Gdx.app.postRunnable(() -> {
                    disposeOtherPlayers();
                    showMenu("Connection lost. Choose Play to reconnect.");
                });
            }
        });
    }

    private void connectToServer() {
        if (gameState == GameState.CONNECTING || isConnected) {
            return;
        }

        gameState = GameState.CONNECTING;
        menu.show("Connecting to " + Network.SERVER_HOST + "...");
        menu.hide();

        new Thread(() -> {
            try {
                client.connect(5000, Network.SERVER_HOST, Network.TCP_PORT, Network.UDP_PORT);

                Ping myPing = new Ping();
                myPing.message = "Hello Server, I am a new client!";
                client.sendTCP(myPing);

                isConnected = true;
                positionUpdateTimer = 0f;
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

        if (isConnected) {
            positionUpdateTimer += Gdx.graphics.getDeltaTime() * 1000f;
            if (positionUpdateTimer >= Network.POSITION_UPDATE_INTERVAL_MS) {
                positionUpdateTimer = 0f;
                client.sendUDP(new PositionUpdate(
                    player.getX(), player.getY(), player.getDirection(), player.isMoving()));
            }
        }

        for (Player other : otherPlayers.values()) other.update();

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

        ScreenUtils.clear(0f, 0.6f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        for (Player other : otherPlayers.values()) other.render(batch);
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
        disposeOtherPlayers();
    }

    private void disposeOtherPlayers() {
        for (Player other : otherPlayers.values()) other.dispose();
        otherPlayers.clear();
    }
}
