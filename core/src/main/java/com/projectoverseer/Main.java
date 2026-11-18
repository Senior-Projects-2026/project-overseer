package com.projectoverseer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.projectoverseer.config.ClientConfig;
import com.projectoverseer.entities.Player;
import com.projectoverseer.environment.DayNightRenderer;
import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.AttackAccepted;
import com.projectoverseer.shared.Network.AttackRequest;
import com.projectoverseer.shared.Network.CraftRequest;
import com.projectoverseer.shared.Network.CraftResult;
import com.projectoverseer.shared.Network.GameEnd;
import com.projectoverseer.shared.Network.InventorySync;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.shared.Network.PlayerHealth;
import com.projectoverseer.shared.Network.PlayerHunger;
import com.projectoverseer.shared.Network.PlayerLeft;
import com.projectoverseer.shared.Network.PlayerPosition;
import com.projectoverseer.shared.Network.PositionUpdate;
import com.projectoverseer.shared.Network.PunchRock;
import com.projectoverseer.shared.Network.PunchTree;
import com.projectoverseer.shared.Network.RespawnRequest;
import com.projectoverseer.shared.Network.RockHit;
import com.projectoverseer.shared.Network.RockRemoved;
import com.projectoverseer.shared.Network.TimeSync;
import com.projectoverseer.shared.Network.TreeHit;
import com.projectoverseer.shared.Network.TreeRemoved;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ChunkManager;
import com.projectoverseer.shared.world.ResourceGatheringRules;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;
import com.projectoverseer.shared.world.generation.WorldGenerator;
import com.projectoverseer.ui.CraftingMenu;
import com.projectoverseer.ui.GameHUD;
import com.projectoverseer.ui.MainMenu;
import com.projectoverseer.ui.RespawnScreen;
import com.projectoverseer.world.TileRenderer;
import com.projectoverseer.world.WorldObjectRenderer;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private enum GameState {
        MENU,
        CONNECTING,
        PLAYING,
        DEAD,
        VICTORY
    }

    private Client client;
    private SpriteBatch batch;
    private MainMenu menu;
    private GameState gameState = GameState.MENU;
    private RespawnScreen respawnScreen;

    private volatile int currentDay = 1;
    private volatile boolean isNight = false;
    private DayNightRenderer dayNightRenderer;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private final Map<Integer, Player> otherPlayers = new HashMap<>();
    private final Map<Integer, Long> movementRevisions = new HashMap<>();
    private TileRenderer tileRenderer;
    private WorldObjectRenderer worldObjectRenderer;

    private ChunkManager chunkManager;
    private static final int CHUNK_LOAD_RADIUS = ClientConfig.getInt("world.chunk.load.radius", 2);
    private static final int CHUNK_UNLOAD_RADIUS = ClientConfig.getInt("world.chunk.unload.radius", 3);
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkY = Integer.MIN_VALUE;
    private List<Chunk> chunks;

    private GameHUD hud;
    private CraftingMenu craftingMenu;

    private volatile boolean isConnected;
    private volatile int playerId = -1;
    private float positionUpdateTimer;
    private Timer.Task welcomeTimeout;
    private String connectionError;

    private final Set<Long> destroyedResources = new HashSet<>();


    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        int camWidth = ClientConfig.getInt("camera.viewport.width", 640);
        int camHeight = ClientConfig.getInt("camera.viewport.height", 360);
        viewport = new FitViewport(camWidth, camHeight, camera);
        tileRenderer = new TileRenderer();
        worldObjectRenderer = new WorldObjectRenderer();
        chunks = List.of();
        dayNightRenderer = new DayNightRenderer();

        createClient();

        hud = new GameHUD(
            player.getStats(),
            player.getInventory()
        );
        craftingMenu = new CraftingMenu(
            hud.getStage(),
            player.getInventory(),
            itemId -> {
                if (isConnected && gameState == GameState.PLAYING && player.getStats().getHealth() > 0) {
                    client.sendTCP(new CraftRequest(itemId));
                }
            }
        );

        menu = new MainMenu(this::connectToServer);
        menu.show("Choose Play to connect to the server.");

        respawnScreen = new RespawnScreen(() -> {
            Gdx.app.log("Combat", "Respawn requested.");
            client.sendTCP(new RespawnRequest());
        });
    }

    private static long resourceKey(int tileX, int tileY) {
        return ((long) tileX << 32)
            | (tileY & 0xFFFFFFFFL);
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
                    Gdx.app.postRunnable(() -> {
                        long known = movementRevisions.getOrDefault(position.playerId, 0L);
                        if (position.movementRevision < known) return;
                        movementRevisions.put(position.playerId, position.movementRevision);
                        if (position.playerId == playerId) {
                            if (position.correction) {
                                player.setRemoteState(position.x, position.y, position.direction, false);
                            }
                        } else {
                            Player remote = otherPlayers.computeIfAbsent(
                                position.playerId, id -> new Player(false));
                            remote.setRemoteState(position.x, position.y, position.direction, position.moving);
                            remote.setHeldItem(position.heldItemId);
                        }
                    });
                } else if (object instanceof PlayerLeft) {
                    int leftId = ((PlayerLeft) object).playerId;
                    Gdx.app.postRunnable(() -> {
                        Player removed = otherPlayers.remove(leftId);
                        movementRevisions.remove(leftId);
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

                }
                else if (object instanceof PlayerHealth health) {
                    Gdx.app.postRunnable(() -> {
                        if (health.playerId != playerId || !isConnected) return;

                        player.getStats().setHealth((int) Math.ceil(health.health));
                        Gdx.app.log("Combat", "Health: " + health.health);

                        if (health.health <= 0 && gameState != GameState.DEAD) {
                            Gdx.app.log("Combat", "Death detected. Current state: " + gameState);
                            player.setRemoteState(health.x, health.y, player.getDirection(), false);
                            gameState = GameState.DEAD;
                            respawnScreen.show();
                            Gdx.app.log("Combat", "Player died. Waiting for respawn.");
                        } else if (health.health > 0 && gameState == GameState.DEAD) {
                            player.setRemoteState(health.x, health.y, player.getDirection(), false);
                            gameState = GameState.PLAYING;
                            respawnScreen.hide();
                            Gdx.input.setInputProcessor(hud.getStage());
                            Gdx.app.log("Combat", "Player respawned.");
                        }
                    });
                    }else if (object instanceof PlayerHunger hunger) {
                        Gdx.app.postRunnable(() -> {
                            if (hunger.playerId != playerId || !isConnected) return;

                            player.getStats().setHunger(Math.round(hunger.hunger / 5f));
                            Gdx.app.log("Survival", "Hunger depleted: " + hunger.hunger);
                        });
                    }else if (object instanceof AttackAccepted attack) {
                    Gdx.app.postRunnable(() -> {
                        Player attacker;
                        if (attack.playerId == playerId) {
                            attacker = player;
                            Gdx.app.log("Combat", "Attack accepted: "
                                + (attack.itemId.isEmpty() ? "unarmed" : attack.itemId));
                        } else {
                            attacker = otherPlayers.computeIfAbsent(
                                attack.playerId, id -> new Player(false));
                        }
                        attacker.triggerAttack(attack.itemId);
                    });
                }
                else if (object instanceof InventorySync sync) {

                    if (sync.itemIds == null
                        || sync.quantities == null
                        || sync.itemIds.length != Inventory.SLOT_COUNT
                        || sync.quantities.length != Inventory.SLOT_COUNT) {
                        return;
                    }

                    ItemStack[] slots = new ItemStack[Inventory.SLOT_COUNT];

                    try {
                        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
                            String itemId = sync.itemIds[i];
                            int quantity = sync.quantities[i];

                            if (itemId == null) {
                                if (quantity != 0) {
                                    return;
                                }
                            } else {
                                slots[i] = new ItemStack(itemId, quantity);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        return;
                    }

                    Gdx.app.postRunnable(() -> {
                        if (!connection.isConnected()) {
                            return;
                        }

                        Inventory inventory = player.getInventory();

                        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
                            inventory.setSlot(i, slots[i]);
                        }
                        craftingMenu.refresh();
                    });
                }

                else if (object instanceof CraftResult result) {
                    if (result.success) {
                        System.out.println(
                            "CLIENT: Crafted " + result.itemId
                        );
                    } else {
                        System.out.println(
                            "CLIENT: Craft failed for " + result.itemId
                        );
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
                        float victoryDisplaySec = ClientConfig.getFloat("ui.victory.display.seconds", 10f);
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                showMenu("Victory! Ready for another round?");
                            }
                        }, victoryDisplaySec);
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
        destroyedResources.clear();
        gameState = GameState.CONNECTING;
        connectionError = null;
        chunks = List.of();

        String host = ClientConfig.getString("server.host", "localhost");
        int tcpPort = ClientConfig.getInt("server.tcp.port", Network.TCP_PORT);
        int udpPort = ClientConfig.getInt("server.udp.port", Network.UDP_PORT);
        menu.show("Connecting to " + host + "...");

        // Keep connection status visible until Welcome has supplied the world seed.
        float welcomeTimeoutSec = ClientConfig.getFloat("network.welcome.timeout.seconds", 10f);
        welcomeTimeout = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (gameState == GameState.CONNECTING) {
                    connectionError = "Timed out waiting for world data. Please reconnect.";
                    client.close();
                    showMenu(connectionError);
                }
            }
        }, welcomeTimeoutSec);

        int connectTimeoutMs = ClientConfig.getInt("network.connect.timeout.ms", 5000);
        new Thread(() -> {
            try {
                client.connect(connectTimeoutMs, host, tcpPort, udpPort);

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
        if (craftingMenu != null) {
            welcomeTimeout.cancel();
            craftingMenu.hide();
        }
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



    private void handlePrimaryAction() {
        if (gameState != GameState.PLAYING
            || player.getStats().getHealth() == 0
            || !isConnected
            || craftingMenu.isOpen()
            || !Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        int bottomY = Gdx.graphics.getHeight() - 1 - screenY;

        if (screenX < viewport.getScreenX()
            || screenX >= viewport.getScreenX() + viewport.getScreenWidth()
            || bottomY < viewport.getScreenY()
            || bottomY >= viewport.getScreenY() + viewport.getScreenHeight()) {
            return;
        }

        Vector2 hudPoint = new Vector2(screenX, screenY);
        hud.getStage().screenToStageCoordinates(hudPoint);

        if (hudPoint.y >= 0 && hudPoint.y < 85f) {
            return;
        }

        Vector3 mouseWorld = new Vector3(screenX, screenY, 0);
        viewport.unproject(mouseWorld);

        WorldResource resource =
            worldObjectRenderer.findResourceAt(
                chunks,
                mouseWorld.x,
                mouseWorld.y
            );

        if (resource == null) {
            client.sendTCP(new AttackRequest(player.getInventory().getSelectedSlot(), mouseWorld.x, mouseWorld.y));
            return;
        }

        float playerX = player.getX() + 16f;
        float playerY = player.getY() + 16f;

        float tileSize = ResourceGatheringRules.TILE_SIZE;
        float punchRange = ResourceGatheringRules.PUNCH_RANGE;

        float resourceX = (resource.tileX() + 0.5f) * tileSize;
        float resourceY = (resource.tileY() + 0.5f) * tileSize;

        float dx = playerX - resourceX;
        float dy = playerY - resourceY;

        if (dx * dx + dy * dy > punchRange * punchRange) {
            client.sendTCP(new AttackRequest(player.getInventory().getSelectedSlot(), mouseWorld.x, mouseWorld.y));
            return;
        }

        if (resource.type() == ResourceType.TREE) {

            client.sendTCP(new PunchTree(
                resource.tileX(),
                resource.tileY(),
                player.getInventory().getSelectedSlot()
            ));

        } else if (resource.type() == ResourceType.ROCK) {

            client.sendTCP(new PunchRock(
                resource.tileX(),
                resource.tileY(),
                player.getInventory().getSelectedSlot()
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
            dayNightRenderer.renderVictoryScreen(camera, batch, currentDay);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            craftingMenu.toggle();
        }

        if (gameState == GameState.DEAD) {
            respawnScreen.render(Gdx.graphics.getDeltaTime());
            return;
        }

        player.update(chunks);
        updateLoadedChunks();

        if (isConnected) {
            positionUpdateTimer += Gdx.graphics.getDeltaTime() * 1000f;
            if (positionUpdateTimer >= Network.POSITION_UPDATE_INTERVAL_MS) {
                positionUpdateTimer = 0f;
                PositionUpdate position = new PositionUpdate(
                    player.getX(), player.getY(), player.getDirection(), player.isMoving());
                position.movementRevision = movementRevisions.getOrDefault(playerId, 0L);
                position.selectedSlot = player.getInventory().getSelectedSlot();
                client.sendUDP(position);
            }
        }

        for (Player other : otherPlayers.values()) other.update(chunks);

        float deadzoneWidth = ClientConfig.getFloat("camera.deadzone.width", 100f);
        float deadzoneHeight = ClientConfig.getFloat("camera.deadzone.height", 80f);

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
        handlePrimaryAction();

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

        if (respawnScreen != null) {
            respawnScreen.resize(width, height);
        }
    }

    private void removeTreeFromWorld(int tileX, int tileY) {
        destroyedResources.add(resourceKey(tileX, tileY));
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

    private void removeRockFromWorld(int tileX, int tileY) {
        destroyedResources.add(resourceKey(tileX, tileY));
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
        if (craftingMenu != null) craftingMenu.dispose();
        if (hud != null) hud.dispose();
        if (respawnScreen != null) respawnScreen.dispose();
    }

    private void disposeOtherPlayers() {
        for (Player other : otherPlayers.values()) other.dispose();
        otherPlayers.clear();
        movementRevisions.clear();

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
        System.out.println("CLIENT: Unloaded chunks outside radius " + CHUNK_UNLOAD_RADIUS + " from (" + chunkX + ", " + chunkY + ")");

        chunks = List.copyOf(chunkManager.getLoadedChunks());

        for (Chunk chunk : chunks) {
            for (WorldResource resource : List.copyOf(chunk.getResources())) {
                if (destroyedResources.contains(
                    resourceKey(resource.tileX(), resource.tileY())
                )) {
                    chunk.removeResource(
                        Chunk.localCoordinate(resource.tileX()),
                        Chunk.localCoordinate(resource.tileY())
                    );
                }
            }
        }

        lastChunkX = chunkX;
        lastChunkY = chunkY;

        System.out.println("CLIENT: Loaded " + chunks.size()
            + " chunks around (" + chunkX + ", " + chunkY + ")");
    }
}
