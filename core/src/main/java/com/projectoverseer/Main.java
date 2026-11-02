package com.projectoverseer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.projectoverseer.entities.Player;
import com.projectoverseer.environment.DayNightRenderer;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.ui.GameHUD;
import com.projectoverseer.shared.Network.TimeSync;
import com.projectoverseer.shared.Network.GameEnd;
import com.projectoverseer.shared.Network.PlayerLeft;
import com.projectoverseer.shared.Network.PlayerPosition;
import com.projectoverseer.shared.Network.PositionUpdate;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.ui.MainMenu;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.world.DemoWorld;
import com.projectoverseer.world.TileRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private enum GameState {
        MENU,
        CONNECTING,
        PLAYING,
        VICTORY
    }

    private Client client;
    private SpriteBatch batch;
    private MainMenu menu;
    private GameState gameState = GameState.MENU;

    private volatile int currentDay = 1;
    private volatile boolean isNight = false;
    private DayNightRenderer dayNightRenderer;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private final Map<Integer, Player> otherPlayers = new HashMap<>();
    private TileRenderer tileRenderer;
    private List<Chunk> chunks;

    private GameHUD hud;

    private volatile boolean isConnected;
    private volatile int playerId = -1;
    private float positionUpdateTimer;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 360, camera);
        tileRenderer = new TileRenderer();
        chunks = DemoWorld.createChunks();
        dayNightRenderer = new DayNightRenderer();

        createClient();

        hud = new GameHUD(
            player.getStats(),
            player.getInventory()
        );

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
                } else if (object instanceof TimeSync) {
                    TimeSync sync = (TimeSync) object;
                    currentDay = sync.currentDay;
                    isNight = sync.isNight;
                } else if (object instanceof GameEnd) {
                    GameEnd end = (GameEnd) object;
                    if (end.isVictory) {
                        Gdx.app.postRunnable(() -> {
                            gameState = GameState.VICTORY;
                        });
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("CLIENT: Lost connection to server!");
                isConnected = false;
                playerId = -1;

                Gdx.app.postRunnable(() -> {
                    disposeOtherPlayers();

                    if (gameState == GameState.VICTORY) {
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                showMenu("Victory! Ready for another round?");
                            }
                        }, 10f);
                    } else {
                        showMenu("Connection lost. Choose Play to reconnect.");
                    }
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
                Gdx.app.postRunnable(() -> {
                    gameState = GameState.PLAYING;
                    Gdx.input.setInputProcessor(hud.getStage());
                });
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

        if (gameState == GameState.VICTORY) {
            ScreenUtils.clear(0.1f, 0.4f, 0.1f, 1f);
            dayNightRenderer.renderVictoryScreen(camera, batch);
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

        float playerX = player.getX() + 20;
        float playerY = player.getY() + 20;

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

        camera.position.x = Math.round(camera.position.x);
        camera.position.y = Math.round(camera.position.y);

        camera.update();

        // Making the entire screen black for black screen borders
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        // Scissor test for game background
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());

        // Grey background color
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Disable Scissor test for subsequent rendering
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileRenderer.render(batch, chunks, camera);
        player.render(batch);
        for (Player other : otherPlayers.values()) other.render(batch);
        batch.end();

        hud.render(Gdx.graphics.getDeltaTime());

        dayNightRenderer.render(camera, batch, currentDay, isNight);
    }

    @Override
    public void resize(int width, int height) {
        if (menu != null) {
            menu.resize(width, height);
        }

        viewport.update(width, height, false);

        if (hud != null) {
            hud.resize(width, height);
        }

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
        if (tileRenderer != null) tileRenderer.dispose();
        if (dayNightRenderer != null) dayNightRenderer.dispose();
    }

    private void disposeOtherPlayers() {
        for (Player other : otherPlayers.values()) other.dispose();
        otherPlayers.clear();

        if (hud != null) {
            hud.dispose();
        }

    }
}
