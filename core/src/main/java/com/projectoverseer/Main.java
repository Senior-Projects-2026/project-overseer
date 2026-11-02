package com.projectoverseer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import com.projectoverseer.entities.Player;
import com.projectoverseer.environment.DayNightRenderer;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.GameEnd;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.shared.Network.PlayerLeft;
import com.projectoverseer.shared.Network.PlayerPosition;
import com.projectoverseer.shared.Network.PositionUpdate;
import com.projectoverseer.shared.Network.TimeSync;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ChunkManager;
import com.projectoverseer.shared.world.generation.WorldGenerator;
import com.projectoverseer.ui.GameHUD;
import com.projectoverseer.ui.MainMenu;
import com.projectoverseer.world.TileRenderer;
import com.projectoverseer.world.WorldObjectRenderer;
import com.projectoverseer.shared.Network.PunchTree;
import com.projectoverseer.shared.Network.TreeRemoved;
import com.projectoverseer.shared.Network.WoodCountSync;
import com.projectoverseer.shared.world.WorldResource;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.shared.Network.TreeHit;
import com.projectoverseer.shared.Network.PunchRock;
import com.projectoverseer.shared.Network.RockHit;
import com.projectoverseer.shared.Network.RockRemoved;
import com.projectoverseer.shared.Network.StoneCountSync;


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
    private WorldObjectRenderer worldObjectRenderer;

    private ChunkManager chunkManager;
    private static final int CHUNK_LOAD_RADIUS = 2;
    private static final int CHUNK_UNLOAD_RADIUS = 3;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkY = Integer.MIN_VALUE;
    private List<Chunk> chunks;

    private GameHUD hud;

    private volatile boolean isConnected;
    private volatile int playerId = -1;
    private float positionUpdateTimer;
    private Timer.Task welcomeTimeout;
    private String connectionError;


    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 360, camera);
        tileRenderer = new TileRenderer();
        worldObjectRenderer = new WorldObjectRenderer();
        chunks = List.of();
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
                    Welcome welcome = (Welcome) object;
                    // Keep GL-facing state and the generated map on the render thread.
                    Gdx.app.postRunnable(() -> acceptWelcome(connection, welcome));
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
                else if (object instanceof TreeRemoved removed) {
                    int tileX = removed.tileX;
                    int tileY = removed.tileY;
                    Gdx.app.postRunnable(() -> {
                        removeTreeFromWorld(tileX, tileY);
                    });
                } else if (object instanceof WoodCountSync sync) {
                    int totalWood = sync.totalWood;
                    Gdx.app.postRunnable(() -> {
                        updateWoodInventory(totalWood);
                    });
                }
                else if (object instanceof TreeHit hit) {

                    Gdx.app.postRunnable(() -> {
                        worldObjectRenderer.triggerResourceShake(
                            hit.tileX,
                            hit.tileY
                        );
                    });
                }

                else if (object instanceof RockHit hit) {

                    Gdx.app.postRunnable(() -> {
                        worldObjectRenderer.triggerResourceShake(
                            hit.tileX,
                            hit.tileY
                        );
                    });

                } else if (object instanceof RockRemoved removed) {

                    Gdx.app.postRunnable(() -> {
                        removeRockFromWorld(
                            removed.tileX,
                            removed.tileY
                        );
                    });

                } else if (object instanceof StoneCountSync sync) {

                    int totalStone = sync.totalStone;

                    Gdx.app.postRunnable(() -> {
                        updateStoneInventory(totalStone);
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

                    if (gameState == GameState.VICTORY) {
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                showMenu("Victory! Ready for another round?");
                            }
                        }, 10f);
                    } else {
                        showMenu(connectionError != null ? connectionError
                            : "Connection lost. Choose Play to reconnect.");
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
        connectionError = null;
        chunks = List.of();
        menu.show("Connecting to " + Network.SERVER_HOST + "...");
        // Keep connection status visible until Welcome has supplied the world seed.
        welcomeTimeout = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (gameState == GameState.CONNECTING) {
                    connectionError = "Timed out waiting for world data. Please reconnect.";
                    client.close();
                    showMenu(connectionError);
                }
            }
        }, 10f);

        new Thread(() -> {
            try {
                client.connect(5000, Network.SERVER_HOST, Network.TCP_PORT, Network.UDP_PORT);

                Ping myPing = new Ping();
                myPing.message = "Hello Server, I am a new client!";
                client.sendTCP(myPing);

                // PLAYING begins only when acceptWelcome has generated the map.
            } catch (IOException e) {
                System.err.println("CLIENT: Could not connect to server!");
                e.printStackTrace();
                Gdx.app.postRunnable(() -> showMenu("Could not connect. Is the server running?"));
            }
        }, "kryonet-connect").start();
    }

    private void showMenu(String status) {
        if (welcomeTimeout != null) welcomeTimeout.cancel();
        isConnected = false;
        gameState = GameState.MENU;
        menu.show(status);
    }

    private void acceptWelcome(Connection connection, Welcome welcome) {
        if (gameState != GameState.CONNECTING || !connection.isConnected()) return;
        try {
            WorldGenerator generator = WorldGenerator.fromServer(
                welcome.worldSeed,
                welcome.generatorVersion
            );

            // Creat ChunkManager using world seed and gen vers + force initial chunk update
            chunkManager = new ChunkManager(generator);
            lastChunkX = Integer.MIN_VALUE;
            lastChunkY = Integer.MIN_VALUE;

            updateLoadedChunks();
        } catch (IllegalArgumentException e) {
            connectionError = "World generator version mismatch. Update client and server.";
            client.close();
            showMenu(connectionError);
            return;
        }
        if (welcomeTimeout != null) welcomeTimeout.cancel();
        playerId = welcome.playerId;
        isConnected = true;
        positionUpdateTimer = 0f;
        menu.hide();
        gameState = GameState.PLAYING;
        Gdx.input.setInputProcessor(hud.getStage());
        System.out.println("CLIENT: World seed " + welcome.worldSeed + ", generator version "
            + welcome.generatorVersion + ", chunks " + chunks.size());
    }



    private void handleResourcePunch() {

        if (gameState != GameState.PLAYING
            || !isConnected
            || !Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        int bottomY = Gdx.graphics.getHeight() - 1 - screenY;

        // Verificam daca mouse-ul este in viewport
        if (screenX < viewport.getScreenX()
            || screenX >= viewport.getScreenX() + viewport.getScreenWidth()
            || bottomY < viewport.getScreenY()
            || bottomY >= viewport.getScreenY() + viewport.getScreenHeight()) {
            return;
        }

        // Ignoram click-urile pe HUD
        Vector2 hudPoint = new Vector2(screenX, screenY);
        hud.getStage().screenToStageCoordinates(hudPoint);

        if (hudPoint.y >= 0 && hudPoint.y < 85f) {
            return;
        }

        // Transformam coordonatele mouse-ului
        // in coordonate ale lumii
        Vector3 mouseWorld = new Vector3(screenX, screenY, 0);
        viewport.unproject(mouseWorld);

        WorldResource resource =
            worldObjectRenderer.findResourceAt(
                chunks,
                mouseWorld.x,
                mouseWorld.y
            );

        if (resource == null) {
            return;
        }

        // Verificam distanta fata de resursa
        float playerX = player.getX() + 16f;
        float playerY = player.getY() + 16f;

        float resourceX = (resource.tileX() + 0.5f) * 32f;
        float resourceY = (resource.tileY() + 0.5f) * 32f;

        float dx = playerX - resourceX;
        float dy = playerY - resourceY;

        if (dx * dx + dy * dy > 64f * 64f) {
            return;
        }

        // Trimitem cererea potrivita catre server
        if (resource.type() == ResourceType.TREE) {

            client.sendTCP(new PunchTree(
                resource.tileX(),
                resource.tileY()
            ));

        } else if (resource.type() == ResourceType.ROCK) {

            client.sendTCP(new PunchRock(
                resource.tileX(),
                resource.tileY()
            ));
        }
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
        updateLoadedChunks();

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
        handleResourcePunch();

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
        worldObjectRenderer.render(batch, chunks, camera, player, otherPlayers.values());
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

    private void removeTreeFromWorld(int tileX, int tileY) {
        int chunkX = Chunk.chunkCoordinate(tileX);
        int chunkY = Chunk.chunkCoordinate(tileY);
        for (Chunk chunk : chunks) {
            if (chunk.getChunkX() != chunkX
                || chunk.getChunkY() != chunkY) {
                continue;
            }
            int localX = Chunk.localCoordinate(tileX);
            int localY = Chunk.localCoordinate(tileY);
            WorldResource resource =
                chunk.getResource(localX, localY);
            if (resource != null
                && resource.type() == ResourceType.TREE) {
                chunk.removeResource(localX, localY);
            }
            return;
        }
    }

    private void updateWoodInventory(int totalWood) {
        Inventory inventory = player.getInventory();
        inventory.removeItem(
            "wood",
            Inventory.SLOT_COUNT * ItemStack.MAX_STACK_SIZE
        );
        if (totalWood > 0) {
            int remaining = inventory.addItem(
                "wood",
                totalWood
            );
            if (remaining > 0) {
                System.err.println(
                    "CLIENT: Not enough inventory space for "
                        + remaining + " wood"
                );
            }
        }
    }


    private void removeRockFromWorld(int tileX, int tileY) {

        int chunkX = Chunk.chunkCoordinate(tileX);
        int chunkY = Chunk.chunkCoordinate(tileY);

        for (Chunk chunk : chunks) {

            if (chunk.getChunkX() != chunkX
                || chunk.getChunkY() != chunkY) {
                continue;
            }

            int localX = Chunk.localCoordinate(tileX);
            int localY = Chunk.localCoordinate(tileY);

            WorldResource resource =
                chunk.getResource(localX, localY);

            if (resource != null
                && resource.type() == ResourceType.ROCK) {

                chunk.removeResource(localX, localY);
            }

            return;
        }
    }


    private void updateStoneInventory(int totalStone) {

        Inventory inventory = player.getInventory();

        // Eliminam cantitatea locala veche
        inventory.removeItem(
            "stone",
            Inventory.SLOT_COUNT * ItemStack.MAX_STACK_SIZE
        );

        // Aplicam cantitatea confirmata de server
        if (totalStone > 0) {

            int remaining = inventory.addItem(
                "stone",
                totalStone
            );

            if (remaining > 0) {
                System.err.println(
                    "CLIENT: Not enough space for "
                        + remaining + " stone"
                );
            }
        }
    }


    @Override
    public void dispose() {
        if (welcomeTimeout != null) welcomeTimeout.cancel();
        if (client != null) {
            client.stop();
        }
        if (menu != null) menu.dispose();
        if (batch != null) batch.dispose();
        if (player != null) player.dispose();
        disposeOtherPlayers();
        if (tileRenderer != null) tileRenderer.dispose();
        if (worldObjectRenderer != null) worldObjectRenderer.dispose();
        if (dayNightRenderer != null) dayNightRenderer.dispose();
        if (hud != null) hud.dispose();
    }

    private void disposeOtherPlayers() {
        for (Player other : otherPlayers.values()) other.dispose();
        otherPlayers.clear();

    }

    /**
     * Updates the loaded chunks based on the player's current position.
     * Loads nearby chunks and unloads chunks that are too far away.
     */
    private void updateLoadedChunks() {
        if (chunkManager == null) return;

        // Convert the player's world position into tile coordinates,
        // then determine which chunk contains the player.
        int tileX = (int) Math.floor(player.getX() / TileRenderer.TILE_SIZE);
        int tileY = (int) Math.floor(player.getY() / TileRenderer.TILE_SIZE);

        int chunkX = Chunk.chunkCoordinate(tileX);
        int chunkY = Chunk.chunkCoordinate(tileY);

        // Only update chunk loading when the player enters a different chunk.
        if (chunkX == lastChunkX && chunkY == lastChunkY) return;

        chunkManager.loadAround(chunkX, chunkY, CHUNK_LOAD_RADIUS);

        chunkManager.unloadOutside(chunkX, chunkY, CHUNK_UNLOAD_RADIUS);

        chunks = List.copyOf(chunkManager.getLoadedChunks());

        lastChunkX = chunkX;
        lastChunkY = chunkY;

        System.out.println("CLIENT: Loaded " + chunks.size()
            + " chunks around (" + chunkX + ", " + chunkY + ")");
    }
}
