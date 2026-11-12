package com.projectoverseer.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.server.combat.AttackValidator;
import com.projectoverseer.server.combat.CombatPlayer;
import com.projectoverseer.server.combat.CombatSystem;
import com.projectoverseer.server.combat.CombatTarget;
import com.projectoverseer.server.combat.Knockback;
import com.projectoverseer.shared.combat.CombatConfig;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.*;
import com.projectoverseer.shared.crafting.CraftingRecipe;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;
import com.projectoverseer.shared.world.generation.WorldGenerator;
import com.projectoverseer.shared.world.ResourceGatheringRules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Launches the server application. */
public class ServerLauncher {
    private static List<Chunk> worldChunks;
    private static WorldGenerator worldGenerator;

    // Server configuration
    private static int serverSleepTime = 50;
    private static float serverSyncInterval = 1f;

    // Day-Night logic
    private static int day = 1;
    private static float timeTimer = 0;
    private static float dayLengthSeconds = 10;
    private static float nightThreshold = 0.7f;
    private static int winThreshold = 30;
    private static boolean gameStarted = false;

    // Internal timers
    private static float syncTimer = 0f;
    private static long lastTime;

    private static final int TREE_HITS_REQUIRED =
        ResourceGatheringRules.damageRequired(ResourceType.TREE);
    private static final int ROCK_HITS_REQUIRED =
        ResourceGatheringRules.damageRequired(ResourceType.ROCK);
    private static final float TILE_SIZE = 32f;
    private static final float PUNCH_RANGE = 64f;

    private static final long PUNCH_COOLDOWN_NS = 250_000_000L;



    public static void main(String[] args) throws IOException {
        long worldSeed = Long.parseLong(Network.CONFIG.getProperty("world.seed", "123"));
        worldGenerator = new WorldGenerator(worldSeed);
        worldChunks = new ArrayList<>(worldGenerator.generateInitialChunks());
        System.out.println("SERVER: World seed " + worldSeed + ", generator version "
            + WorldGenerator.VERSION + ", chunks " + worldChunks.size());
        Server server = new Server();
        Network.register(server);
        server.bind(Network.TCP_PORT, Network.UDP_PORT);

        Map<Integer, PlayerPosition> players = new HashMap<>();
        Map<Integer, Inventory> playerInventories = new HashMap<>();
        AttackValidator attacks = new AttackValidator(CombatConfig.defaults());
        CombatSystem combat = new CombatSystem();
        Map<Integer, CombatPlayer> combatPlayers = new HashMap<>();

        Map<Long, Integer> treeHits = new HashMap<>();
        Map<Integer, Long> lastPunchTime = new HashMap<>();
        List<TreeRemoved> destroyedTrees = new ArrayList<>();


        Map<Long, Integer> rockHits = new HashMap<>();
        List<RockRemoved> destroyedRocks = new ArrayList<>();



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

                int playerId = connection.getID();
                CombatPlayer combatPlayer = new CombatPlayer(playerId);
                combatPlayers.put(playerId, combatPlayer);
                combat.register(combatPlayer);
                connection.sendTCP(healthPacket(combatPlayer));
                playerInventories.put(
                    playerId,
                    new Inventory()
                );

                sendInventorySync(
                    connection,
                    playerInventories.get(playerId)
                );
                for (PlayerPosition player : players.values()) {
                    connection.sendTCP(player);
                }

                System.out.println("SERVER: Player " + connection.getID() + " connected.");

                for (TreeRemoved removed : destroyedTrees) {
                    connection.sendTCP(removed);
                }

                for (RockRemoved removed : destroyedRocks) {
                    connection.sendTCP(removed);
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                CombatPlayer combatPlayer = combatPlayers.get(connection.getID());
                if (combatPlayer == null || !combatPlayer.isAlive()) return;
                if (object instanceof Ping) {
                    Ping ping = (Ping) object;
                    System.out.println("SERVER: Received from client -> " + ping.message);

                    Ping response = new Ping();
                    response.message = "Pong from server, I hear you perfectly!";
                    connection.sendTCP(response);
                } else if (object instanceof PositionUpdate) {
                    PositionUpdate update = (PositionUpdate) object;
                    if (!combatPlayer.updatePosition(update.x, update.y, update.movementRevision)) return;
                    PlayerPosition position = new PlayerPosition(
                        connection.getID(), update.x, update.y, update.direction, update.moving);
                    players.put(connection.getID(), position);
                    position.movementRevision = combatPlayer.movementRevision();
                    Inventory inventory = playerInventories.get(connection.getID());
                    if (inventory != null && update.selectedSlot >= 0 && update.selectedSlot < Inventory.SLOT_COUNT) {
                        ItemStack held = inventory.getSlot(update.selectedSlot);
                        position.heldItemId = held == null ? "" : held.itemId();
                    }
                    for (Connection recipient : server.getConnections()) {
                        if (recipient.getID() != connection.getID()) recipient.sendUDP(position);
                    }
                }
                else if (object instanceof AttackRequest request) {
                    int id = connection.getID();
                    PlayerPosition position = players.get(id);
                    if (position == null) return;
                    AttackValidator.Swing swing = attacks.accept(id, playerInventories.get(id),
                        position.x + 16f, position.y + 16f, gameStarted && day <= winThreshold,
                        request, System.nanoTime());
                    if (swing == null) return;
                    AttackAccepted accepted = new AttackAccepted();
                    accepted.playerId = id;
                    accepted.itemId = swing.itemId();
                    accepted.originX = swing.originX();
                    accepted.originY = swing.originY();
                    accepted.directionX = swing.directionX();
                    accepted.directionY = swing.directionY();
                    server.sendToAllTCP(accepted);
                    for (CombatTarget target : combat.resolve(combatPlayer.combatId(), swing)) {
                        Knockback.apply(target, swing.directionX(), swing.directionY(), swing.stats().knockback(),
                            (tileX, tileY) -> {
                                Chunk chunk = findChunk(tileX, tileY);
                                return chunk != null && chunk.isWalkable(Chunk.localCoordinate(tileX), Chunk.localCoordinate(tileY));
                            });
                        System.out.println("SERVER: " + combatPlayer.combatId() + " hit "
                            + target.combatId() + " for " + swing.stats().damage());
                        if (target instanceof CombatPlayer victim) {
                            PlayerPosition previous = players.get(victim.playerId());
                            PlayerPosition correction = new PlayerPosition(victim.playerId(), victim.x(), victim.y(),
                                previous == null ? 0 : previous.direction, false);
                            correction.correction = true;
                            correction.heldItemId = victim.isAlive() && previous != null ? previous.heldItemId : "";
                            correction.movementRevision = victim.movementRevision();
                            players.put(victim.playerId(), correction);
                            server.sendToAllTCP(correction);
                            server.sendToTCP(victim.playerId(), healthPacket(victim));
                            if (!victim.isAlive()) {
                                PlayerPosition stopped = players.get(victim.playerId());
                                if (stopped != null) {
                                    stopped.moving = false;
                                    server.sendToAllTCP(stopped);
                                }
                            }
                        }
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
                    Inventory inventory = playerInventories.get(playerId);

                    if (inventory == null || !hasRoomFor(inventory, "wood")) {
                        return;
                    }
                    int damage = ResourceGatheringRules.hitDamage(
                        ResourceType.TREE,
                        inventory,
                        punch.selectedSlot
                    );

                    if (damage == 0) {
                        return;
                    }

                    lastPunchTime.put(playerId, now);
                    long treeId =
                        ((long) punch.tileX << 32)
                            | (punch.tileY & 0xFFFFFFFFL);

                    int hits = treeHits.getOrDefault(treeId, 0) + damage;

                    TreeHit hitMessage = new TreeHit(
                        punch.tileX,
                        punch.tileY,
                        hits
                    );

                    server.sendToAllTCP(hitMessage);
                    if (hits < TREE_HITS_REQUIRED) {
                        treeHits.put(treeId, hits);
                        System.out.println(
                            "SERVER: Tree hit " + hits + "/6"
                        );
                        return;
                    }
                    if (inventory.addItem("wood", 1) != 0) {
                        return;
                    }
                    treeHits.remove(treeId);
                    chunk.removeResource(localX, localY);
                    sendInventorySync(connection, inventory);
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

                else if (object instanceof PunchRock punch) {

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

                    WorldResource resource =
                        chunk.getResource(localX, localY);

                    if (resource == null
                        || resource.type() != ResourceType.ROCK) {
                        return;
                    }

                    float rockX = (punch.tileX + 0.5f) * TILE_SIZE;
                    float rockY = (punch.tileY + 0.5f) * TILE_SIZE;

                    float dx = playerPosition.x + 16f - rockX;
                    float dy = playerPosition.y + 16f - rockY;

                    if (dx * dx + dy * dy
                        > PUNCH_RANGE * PUNCH_RANGE) {
                        return;
                    }

                    long now = System.nanoTime();

                    Long lastPunch = lastPunchTime.get(playerId);

                    if (lastPunch != null
                        && now - lastPunch < PUNCH_COOLDOWN_NS) {
                        return;
                    }

                    Inventory inventory = playerInventories.get(playerId);

                    if (inventory == null || !hasRoomFor(inventory, "stone")) {
                        return;
                    }

                    int damage = ResourceGatheringRules.hitDamage(
                        ResourceType.ROCK,
                        inventory,
                        punch.selectedSlot
                    );

                    if (damage == 0) {
                        return;
                    }

                    lastPunchTime.put(playerId, now);
                    long rockId =
                        ((long) punch.tileX << 32)
                            | (punch.tileY & 0xFFFFFFFFL);

                    int hits = rockHits.getOrDefault(rockId, 0) + damage;

                    RockHit hitMessage = new RockHit(
                        punch.tileX,
                        punch.tileY,
                        hits
                    );

                    server.sendToAllTCP(hitMessage);
                    if (hits < ROCK_HITS_REQUIRED) {
                        rockHits.put(rockId, hits);

                        System.out.println(
                            "SERVER: Rock hit " + hits + "/7"
                        );
                        return;
                    }

                    if (inventory.addItem("stone", 1) != 0) {
                        return;
                    }
                    rockHits.remove(rockId);
                    chunk.removeResource(localX, localY);

                    sendInventorySync(connection, inventory);

                    RockRemoved removed = new RockRemoved();
                    removed.tileX = punch.tileX;
                    removed.tileY = punch.tileY;

                    destroyedRocks.add(removed);
                    server.sendToAllTCP(removed);

                    System.out.println(
                        "SERVER: Player " + playerId
                            + " destroyed rock at "
                            + punch.tileX + ", " + punch.tileY
                    );
                }
                else if (object instanceof CraftRequest request) {

                    int playerId = connection.getID();

                    Inventory inventory = playerInventories.get(playerId);

                    CraftingRecipe recipe =
                        CraftingRecipe.fromId(request.itemId);

                    boolean success = inventory != null
                        && recipe != null
                        && inventory.tryCraft(recipe);

                    CraftResult result = new CraftResult(
                        request.itemId,
                        success
                    );

                    connection.sendTCP(result);

                    if (success) {
                        sendInventorySync(connection, inventory);

                        System.out.println(
                            "SERVER: Player " + playerId
                                + " crafted " + request.itemId
                        );
                    }
                }

            }

            @Override
            public void disconnected(Connection connection) {
                CombatPlayer removed = combatPlayers.remove(connection.getID());
                if (removed != null) combat.remove(removed.combatId());
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

                lastPunchTime.remove(connection.getID());
                attacks.removePlayer(connection.getID());

                playerInventories.remove(connection.getID());

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

    private static PlayerHealth healthPacket(CombatPlayer player) {
        PlayerHealth packet = new PlayerHealth();
        packet.playerId = player.playerId();
        packet.health = player.health();
        packet.x = player.x();
        packet.y = player.y();
        return packet;
    }

    private static void sendInventorySync(
        Connection connection,
        Inventory inventory
    ) {
        InventorySync sync = new InventorySync();

        sync.itemIds = new String[Inventory.SLOT_COUNT];
        sync.quantities = new int[Inventory.SLOT_COUNT];

        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {

            ItemStack stack = inventory.getSlot(i);

            if (stack != null) {
                sync.itemIds[i] = stack.itemId();
                sync.quantities[i] = stack.quantity();
            }
        }

        connection.sendTCP(sync);
    }


    private static boolean hasRoomFor(
        Inventory inventory,
        String itemId
    ) {
        int maxStack = ItemStack.maxStackSize(itemId);

        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            ItemStack stack = inventory.getSlot(i);
            if (stack == null) {
                return true;
            }
            if (stack.itemId().equals(itemId)
                && stack.quantity() < maxStack) {
                return true;
            }
        }

        return false;
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

        Chunk newChunk = worldGenerator.generateChunk(chunkX, chunkY);
        worldChunks.add(newChunk);

        return newChunk;
    }

}
