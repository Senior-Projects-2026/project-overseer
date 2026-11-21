package com.projectoverseer.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.server.combat.AttackValidator;
import com.projectoverseer.server.combat.CombatPlayer;
import com.projectoverseer.server.combat.CombatSystem;
import com.projectoverseer.server.combat.CombatTarget;
import com.projectoverseer.server.combat.Knockback;
import com.projectoverseer.server.config.ServerConfig;
import com.projectoverseer.server.survival.SurvivalPlayer;
import com.projectoverseer.server.survival.SurvivalSystem;
import com.projectoverseer.server.telemetry.TelemetryCollector;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.AttackAccepted;
import com.projectoverseer.shared.Network.AttackRequest;
import com.projectoverseer.shared.Network.BerryBushUpdated;
import com.projectoverseer.shared.Network.CollectBerryBush;
import com.projectoverseer.shared.Network.ConsumeItemRequest;
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
import com.projectoverseer.shared.combat.CombatConfig;
import com.projectoverseer.shared.crafting.CraftingRecipe;
import com.projectoverseer.shared.items.FoodItem;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ResourceGatheringRules;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;
import com.projectoverseer.shared.world.generation.WorldGenerator;

/** Launches the server application. */
public class ServerLauncher {
    private static List<Chunk> worldChunks;
    private static WorldGenerator worldGenerator;
    private static TelemetryCollector overseerTelemetry;

    private static Map<Integer, PlayerPosition> players = new HashMap<>();
    private static Map<Integer, Inventory> playerInventories = new HashMap<>();
    private static Map<Integer, CombatPlayer> combatPlayers = new HashMap<>();

    private static SurvivalSystem survival;
    private static Map<Integer, SurvivalPlayer> survivalPlayers = new HashMap<>();

    // Server configuration
    private static final int SERVER_SLEEP_TIME = ServerConfig.getInt("server.tick.sleep.ms", 50);
    private static final  float SERVER_SYNC_INTERVAL = ServerConfig.getFloat("server.sync.interval", 1f);

    // Day-Night logic
    private static int day = 1;
    private static float timeTimer = 0;
    private static final float DAY_LENGTH_SECONDS = ServerConfig.getFloat("gameplay.day.length.seconds", 100f);
    private static final float NIGHT_THRESHOLD = ServerConfig.getFloat("gameplay.night.threshold", 0.7f);
    private static final int WIN_THRESHOLD = ServerConfig.getInt("gameplay.win.days", 30);
    private static boolean gameStarted = false;

    // Internal timers
    private static float syncTimer = 0f;
    private static long lastTime;

    // Resource gathering constants (unified from shared)
    private static final int TREE_HITS_REQUIRED = ResourceGatheringRules.damageRequired(ResourceType.TREE);
    private static final int ROCK_HITS_REQUIRED = ResourceGatheringRules.damageRequired(ResourceType.ROCK);
    private static final float TILE_SIZE = ResourceGatheringRules.TILE_SIZE;
    private static final float PUNCH_RANGE = ResourceGatheringRules.PUNCH_RANGE;
    private static final long PUNCH_COOLDOWN_NS =
        ServerConfig.getLong("gameplay.punch.cooldown.ms", 250L) * 1_000_000L;

    private static float hungerSyncTimer = 0f;
    private static final float HUNGER_SYNC_INTERVAL = ServerConfig.getFloat("gameplay.hunger.sync.interval", 5f);
    private static float starvationHealthSyncTimer = 0f;
    private static final float STARVATION_HEALTH_SYNC_INTERVAL = ServerConfig.getFloat("gameplay.starvation.health.sync.interval", 1f);



    public static void main(String[] args) throws IOException {
        long defaultSeed = Long.parseLong(Network.CONFIG.getProperty("world.seed", "123"));
        long worldSeed = ServerConfig.getLong("world.seed", defaultSeed);
        int tcpPort = ServerConfig.getInt("server.tcp.port", Network.TCP_PORT);
        int udpPort = ServerConfig.getInt("server.udp.port", Network.UDP_PORT);
        worldGenerator = new WorldGenerator(worldSeed);
        worldChunks = new ArrayList<>(worldGenerator.generateInitialChunks());
        System.out.println("SERVER: World seed " + worldSeed + ", generator version "
            + WorldGenerator.VERSION + ", chunks " + worldChunks.size());
        Server server = new Server();
        Network.register(server);
        server.bind(tcpPort, udpPort);

        players = new HashMap<>();
        // TODO : on death clear player inventory
        playerInventories = new HashMap<>();
        AttackValidator attacks = new AttackValidator(CombatConfig.defaults());
        CombatSystem combat = new CombatSystem();
        combatPlayers = new HashMap<>();

        survival = new SurvivalSystem();
        survivalPlayers = new HashMap<>();

        overseerTelemetry = new TelemetryCollector();

        Map<Long, Integer> treeHits = new HashMap<>();
        Map<Integer, Long> lastPunchTime = new HashMap<>();
        List<TreeRemoved> destroyedTrees = new ArrayList<>();

        Map<Long, Integer> rockHits = new HashMap<>();
        List<RockRemoved> destroyedRocks = new ArrayList<>();

        List<BerryBushUpdated> depletedBerryBushes = new ArrayList<>();
        

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
                survivalPlayers.put(playerId, new SurvivalPlayer());

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

                for (BerryBushUpdated updated : depletedBerryBushes) {
                    connection.sendTCP(updated);
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                CombatPlayer combatPlayer = combatPlayers.get(connection.getID());
                if (combatPlayer == null) return;

                if (object instanceof RespawnRequest) {
                    int playerId = connection.getID();

                    float spawnX = ServerConfig.getFloat("player.spawn.x", 200f);
                    float spawnY = ServerConfig.getFloat("player.spawn.y", 200f);

                    combatPlayer.respawn(spawnX, spawnY);

                    SurvivalPlayer survivalPlayer = survivalPlayers.get(playerId);
                    if (survivalPlayer != null) {
                        survivalPlayer.reset();
                    }

                    PlayerPosition previous = players.get(playerId);
                    int direction = previous == null ? 0 : previous.direction;

                    PlayerPosition position = new PlayerPosition(
                        playerId, spawnX, spawnY, direction, false
                    );
                    position.correction = true;
                    position.movementRevision = combatPlayer.movementRevision();

                    players.put(playerId, position);

                    server.sendToAllTCP(position);
                    connection.sendTCP(healthPacket(combatPlayer));

                    System.out.println("SERVER: Player " + playerId + " respawned.");
                    return;
                }

                if (!combatPlayer.isAlive()) return;
                if (object instanceof Ping ping) {
                    System.out.println("SERVER: Received from client -> " + ping.message);

                    Ping response = new Ping();
                    response.message = "Pong from server, I hear you perfectly!";
                    connection.sendTCP(response);
                } else if (object instanceof PositionUpdate update) {
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
                        position.x + 16f, position.y + 16f, gameStarted && day <= WIN_THRESHOLD,
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
                                overseerTelemetry.recordPlayerDeath();

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
                else if (object instanceof CollectBerryBush collect) {
                    System.out.println("SERVER: Received berry collection request at "
                        + collect.tileX + ", " + collect.tileY);

                    int playerId = connection.getID();

                    PlayerPosition playerPosition = players.get(playerId);
                    if (playerPosition == null) {
                        System.out.println("SERVER: Player position not found");
                        return;
                    }

                    Chunk chunk = findChunk(collect.tileX, collect.tileY);
                    if (chunk == null) {
                        System.out.println("SERVER: Chunk not found");
                        return;
                    }

                    int localX = Chunk.localCoordinate(collect.tileX);
                    int localY = Chunk.localCoordinate(collect.tileY);

                    WorldResource resource = chunk.getResource(localX, localY);

                    if (resource == null) {
                        System.out.println("SERVER: Resource is null at local coordinates "
                            + localX + ", " + localY);
                        return;
                    }

                    if (resource.type() != ResourceType.BERRY_BUSH) {
                        System.out.println("SERVER: Wrong resource type: " + resource.type());
                        return;
                    }

                    if (resource.count() <= 0) {
                        System.out.println("SERVER: Bush has no berry: " + resource.count());
                        return;
                    }

                    float bushX = (collect.tileX + 0.5f) * TILE_SIZE;
                    float bushY = (collect.tileY + 0.5f) * TILE_SIZE;

                    float dx = playerPosition.x + 16f - bushX;
                    float dy = playerPosition.y + 16f - bushY;

                    float distanceSquared = dx * dx + dy * dy;

                    System.out.println("SERVER: Distance squared = " + distanceSquared
                        + ", allowed = " + (PUNCH_RANGE * PUNCH_RANGE));

                    if (distanceSquared > PUNCH_RANGE * PUNCH_RANGE) {
                        System.out.println("SERVER: Too far from bush");
                        return;
                    }

                    Inventory inventory = playerInventories.get(playerId);
                    int berryCount = resource.count();

                    if (inventory == null) {
                        System.out.println("SERVER: Inventory not found");
                        return;
                    }

                    if (!hasRoomFor(inventory, "berry", berryCount)) {
                        System.out.println("SERVER: Not enough inventory space");
                        return;
                    }

                    if (inventory.addItem("berry", berryCount) != 0) {
                        System.out.println("SERVER: Failed to add berry");
                        return;
                    }

                    chunk.setResource(localX, localY, ResourceType.BERRY_BUSH, 0);

                    sendInventorySync(connection, inventory);

                    BerryBushUpdated updated = new BerryBushUpdated(
                        collect.tileX,
                        collect.tileY,
                        0
                    );

                    depletedBerryBushes.add(updated);
                    server.sendToAllTCP(updated);

                    System.out.println("SERVER: Successfully collected "
                        + berryCount + " berry");
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
                else if (object instanceof ConsumeItemRequest request) {
                    int playerId = connection.getID();

                    Inventory inventory = playerInventories.get(playerId);
                    SurvivalPlayer survivalPlayer = survivalPlayers.get(playerId);

                    if (inventory == null || survivalPlayer == null) return;

                    int slot = request.selectedSlot;

                    if (slot < 0 || slot >= Inventory.SLOT_COUNT) return;

                    ItemStack stack = inventory.getSlot(slot);

                    if (stack == null) return;

                    FoodItem food = FoodItem.fromId(stack.itemId());

                    if (food == null) return;

                    inventory.removeItem(stack.itemId(), 1);
                    survivalPlayer.restoreHunger(food.hungerRestoration());

                    survivalPlayer.restoreHunger(food.hungerRestoration());
                    connection.sendTCP(new PlayerHunger(connection.getID(), survivalPlayer.hunger()));
                    sendInventorySync(connection, inventory);

                    System.out.println(
                        "SERVER: Player " + playerId
                            + " consumed " + stack.itemId()
                    );
                }
            }

            @Override
            public void disconnected(Connection connection) {
                CombatPlayer removed = combatPlayers.remove(connection.getID());
                survivalPlayers.remove(connection.getID());
                
                if (removed != null) combat.remove(removed.combatId());
                players.remove(connection.getID());

                PlayerLeft left = new PlayerLeft(connection.getID());
                for (Connection recipient : server.getConnections()) {
                    recipient.sendTCP(left);
                }

                if (server.getConnections().isEmpty()) {
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
        System.out.println("SERVER: Listening on TCP " + tcpPort + " and UDP " + udpPort + "...");

        lastTime = System.nanoTime();

        while (true) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;

            update(delta, server);

            try {
                Thread.sleep(SERVER_SLEEP_TIME); // Target ~20 TPS
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

    private static boolean hasRoomFor(
            Inventory inventory,
            String itemId,
            int quantity
        ) {
            int available = 0;

            for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
                ItemStack stack = inventory.getSlot(i);

                if (stack == null) {
                    available += 64;
                } else if (stack.itemId().equals(itemId)) {
                    available += 64 - stack.quantity();
                }

                if (available >= quantity) return true;
            }

            return false;
        }

    private static void handleDeadPlayers(List<Integer> deadPlayers, Server server) {
        for (int playerId : deadPlayers) {
            overseerTelemetry.recordPlayerDeath();

            PlayerPosition position = players.get(playerId);
            if (position != null) {
                position.moving = false;
                server.sendToAllTCP(position);
            }

            CombatPlayer combatPlayer = combatPlayers.get(playerId);
            if (combatPlayer != null) {
                server.sendToTCP(playerId, healthPacket(combatPlayer));
            }

            System.out.println("SERVER: Player " + playerId + " died from starvation.");
        }
    }

    private static void syncHunger(float delta, Server server) {
        hungerSyncTimer += delta;

        if (hungerSyncTimer >= HUNGER_SYNC_INTERVAL) {
            hungerSyncTimer = 0f;

            for (Map.Entry<Integer, SurvivalPlayer> entry : survivalPlayers.entrySet()) {
                int playerId = entry.getKey();
                float hunger = entry.getValue().hunger();

                server.sendToTCP(
                    playerId,
                    new Network.PlayerHunger(playerId, hunger)
                );
            }
        }
    }

    private static void syncStarvationHealth(float delta, Server server) {
        starvationHealthSyncTimer += delta;

        if (starvationHealthSyncTimer >= STARVATION_HEALTH_SYNC_INTERVAL) {
            starvationHealthSyncTimer = 0f;

            for (Map.Entry<Integer, SurvivalPlayer> entry : survivalPlayers.entrySet()) {
                int playerId = entry.getKey();
                SurvivalPlayer survivalPlayer = entry.getValue();
                CombatPlayer combatPlayer = combatPlayers.get(playerId);

                if (survivalPlayer.isStarving()
                        && combatPlayer != null
                        && combatPlayer.isAlive()) {
                    server.sendToTCP(playerId, healthPacket(combatPlayer));
                }
            }
        }
    }

    public static void update(float delta, Server server) {
        if (!gameStarted || server.getConnections().isEmpty()) return;

        handleDeadPlayers(survival.update(delta, survivalPlayers, combatPlayers), server);
        syncHunger(delta, server);
        syncStarvationHealth(delta, server);

        boolean isNight = timeTimer > DAY_LENGTH_SECONDS * NIGHT_THRESHOLD;

        // enemiesCount = 0 for now, because they are not implemented yet
        overseerTelemetry.update(delta, day, isNight, 0, combatPlayers, playerInventories);

        timeTimer += delta;
        syncTimer += delta;

        if (timeTimer >= DAY_LENGTH_SECONDS) {
            timeTimer = 0;
            day++;
            System.out.println("SERVER: A new day begins! Day " + day);
        }

        if (syncTimer >= SERVER_SYNC_INTERVAL) {
            syncTimer = 0f;

            TimeSync syncPacket = new TimeSync();
            syncPacket.currentDay = day;
            syncPacket.isNight = isNight;
            server.sendToAllTCP(syncPacket);

            if (day > WIN_THRESHOLD) {
                GameEnd endPacket = new GameEnd();
                endPacket.isVictory = true;
                server.sendToAllTCP(endPacket);
                System.out.println("SERVER: " + WIN_THRESHOLD + " days survived. Victory packet sent!");
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
