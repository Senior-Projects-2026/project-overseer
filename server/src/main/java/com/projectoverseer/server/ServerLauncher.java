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

import com.projectoverseer.shared.Network.PunchTree;
import com.projectoverseer.shared.Network.TreeRemoved;
import com.projectoverseer.shared.Network.WoodCountSync;

import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;

import java.util.ArrayList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.generation.WorldGenerator;

/** Launches the server application. */
public class ServerLauncher {
    private static List<Chunk> worldChunks;
    // Server configuration
    private static int serverSleepTime = 50;
    private static float serverSyncInterval = 1f;

    // Day-Night logic
    private static int day = 1;
    private static float timeTimer = 0;
    private static float dayLengthSeconds = 10;
    private static float nightThreshold = 0.7f;
    private static int winThreshold = 5;
    private static boolean gameStarted = false;

    // Internal timers
    private static float syncTimer = 0f;
    private static long lastTime;

    //Game logics, if u ask denis... it would be nice a config file, ngl, i don't like hardcoded shee in code
    //same with asset paths, what if i change the name of tree.png to oak.png? search the whole project for tree.png references
    private static final int TREE_HITS_REQUIRED = 6;
    private static final float TILE_SIZE = 32f;
    private static final float PUNCH_RANGE = 64f;

    private static final long PUNCH_COOLDOWN_NS = 250_000_000L;

    private static final int MAX_WOOD = 9 * 64;


    public static void main(String[] args) throws IOException {
        long worldSeed = Long.parseLong(Network.CONFIG.getProperty("world.seed", "123"));
        WorldGenerator generator = new WorldGenerator(worldSeed);
        worldChunks = generator.generateInitialChunks();
        System.out.println("SERVER: World seed " + worldSeed + ", generator version "
            + WorldGenerator.VERSION + ", chunks " + worldChunks.size());
        Server server = new Server();
        Network.register(server);
        server.bind(Network.TCP_PORT, Network.UDP_PORT);

        Map<Integer, PlayerPosition> players = new HashMap<>();

        Map<Long, Integer> treeHits = new HashMap<>();
        Map<Integer, Integer> playerWood = new HashMap<>();
        Map<Integer, Long> lastPunchTime = new HashMap<>();
        List<TreeRemoved> destroyedTrees = new ArrayList<>();


        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                if (!gameStarted) {
                    gameStarted = true;
                    lastTime = System.nanoTime();
                    System.out.println("SERVER: Game started!");
                }

                Welcome welcome = new Welcome();
                welcome.playerId = connection.getID();
                welcome.worldSeed = worldSeed;
                welcome.generatorVersion = WorldGenerator.VERSION;
                connection.sendTCP(welcome);

                for (PlayerPosition player : players.values()) {
                    connection.sendTCP(player);
                }

                System.out.println("SERVER: Player " + connection.getID() + " connected.");

                for (TreeRemoved removed : destroyedTrees) {
                    connection.sendTCP(removed);
                }
                WoodCountSync woodSync = new WoodCountSync();
                woodSync.totalWood = 0;
                connection.sendTCP(woodSync);
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
                else if (object instanceof PunchTree punch) {
                    int playerId = connection.getID();
                    PlayerPosition playerPosition = players.get(playerId);
                    if (playerPosition == null) {
                        return;
                    }
                    Chunk chunk = findChunk(punch.tileX, punch.tileY);
                    if (chunk == null) {
                        return;
                    }
                    int localX = Chunk.localCoordinate(punch.tileX);
                    int localY = Chunk.localCoordinate(punch.tileY);
                    WorldResource resource = chunk.getResource(localX, localY);
                    if (resource == null || resource.type() != ResourceType.TREE) {
                        return;
                    }
                    float treeX = (punch.tileX + 0.5f) * TILE_SIZE;
                    float treeY = (punch.tileY + 0.5f) * TILE_SIZE;
                    float dx = playerPosition.x + 16f - treeX;
                    float dy = playerPosition.y + 16f - treeY;
                    if (dx * dx + dy * dy > PUNCH_RANGE * PUNCH_RANGE) {
                        return;
                    }
                    long now = System.nanoTime();
                    Long lastPunch = lastPunchTime.get(playerId);
                    if (lastPunch != null
                        && now - lastPunch < PUNCH_COOLDOWN_NS) {
                        return;
                    }
                    int wood = playerWood.getOrDefault(playerId, 0);
                    if (wood >= MAX_WOOD) {
                        return;
                    }
                    lastPunchTime.put(playerId, now);
                    long treeId =
                        ((long) punch.tileX << 32)
                            | (punch.tileY & 0xFFFFFFFFL);
                    int hits = treeHits.getOrDefault(treeId, 0) + 1;
                    if (hits < TREE_HITS_REQUIRED) {
                        treeHits.put(treeId, hits);
                        System.out.println(
                            "SERVER: Tree hit " + hits + "/6"
                        );
                        return;
                    }
                    treeHits.remove(treeId);
                    chunk.removeResource(localX, localY);
                    wood++;
                    playerWood.put(playerId, wood);
                    WoodCountSync woodSync = new WoodCountSync();
                    woodSync.totalWood = wood;
                    connection.sendTCP(woodSync);
                    TreeRemoved removed = new TreeRemoved();
                    removed.tileX = punch.tileX;
                    removed.tileY = punch.tileY;
                    destroyedTrees.add(removed);
                    server.sendToAllTCP(removed);

                    System.out.println(
                        "SERVER: Player " + playerId
                            + " destroyed tree at "
                            + punch.tileX + ", " + punch.tileY
                    );
                }
            }

            @Override
            public void disconnected(Connection connection) {
                players.remove(connection.getID());

                PlayerLeft left = new PlayerLeft(connection.getID());
                for (Connection recipient : server.getConnections()) {
                    recipient.sendTCP(left);
                }

                if (server.getConnections().size() == 0) {
                    gameStarted = false;
                    timeTimer = 0;
                    syncTimer = 0;
                    day = 1;
                    System.out.println("SERVER: All players disconnected. Game stopped.");
                }

                for (Connection recipient : server.getConnections()) recipient.sendTCP(left);

                playerWood.remove(connection.getID());
                lastPunchTime.remove(connection.getID());

                System.out.println("SERVER: Player " + connection.getID() + " disconnected.");
            }
        });

        server.start();
        System.out.println("SERVER: Listening on TCP " + Network.TCP_PORT + " and UDP " + Network.UDP_PORT + "...");

        lastTime = System.nanoTime();

        while (true) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;

            update(delta, server);

            try {
                Thread.sleep(serverSleepTime); // Target ~20 TPS
            } catch (InterruptedException e) {
                System.err.println("SERVER: Thread interrupted!");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void update(float delta, Server server) {
        if (!gameStarted || server.getConnections().size() == 0) return;

        timeTimer += delta;
        syncTimer += delta;

        if (timeTimer >= dayLengthSeconds) {
            timeTimer = 0;
            day++;
            System.out.println("SERVER: A new day begins! Day " + day);
        }

        if (syncTimer >= serverSyncInterval) {
            syncTimer = 0f;

            boolean isNight = timeTimer > dayLengthSeconds * nightThreshold;
            TimeSync syncPacket = new TimeSync();
            syncPacket.currentDay = day;
            syncPacket.isNight = isNight;
            server.sendToAllTCP(syncPacket);

            if (day > winThreshold) {
                GameEnd endPacket = new GameEnd();
                endPacket.isVictory = true;
                server.sendToAllTCP(endPacket);
                System.out.println("SERVER: " + winThreshold + " days survived. Victory packet sent!");
                System.exit(0); // Close server when win is reached
            }
        }
    }

    private static Chunk findChunk(int tileX, int tileY) {

        int chunkX = Chunk.chunkCoordinate(tileX);
        int chunkY = Chunk.chunkCoordinate(tileY);

        for (Chunk chunk : worldChunks) {
            if (chunk.getChunkX() == chunkX
                && chunk.getChunkY() == chunkY) {
                return chunk;
            }
        }

        return null;
    }

}
