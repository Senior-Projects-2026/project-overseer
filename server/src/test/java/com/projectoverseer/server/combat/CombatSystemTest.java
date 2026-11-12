package com.projectoverseer.server.combat;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.combat.CombatConfig;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatSystemTest {
    private final CombatSystem combat = new CombatSystem();
    private final AttackValidator validator = new AttackValidator(CombatConfig.defaults());
    private final Inventory inventory = new Inventory();

    private CombatPlayer player(int id, float x, float y) {
        CombatPlayer player = new CombatPlayer(id);
        player.updatePosition(x, y);
        combat.register(player);
        return player;
    }

    private AttackValidator.Swing swing(long now) {
        return validator.accept(1, inventory, 16, 16, true, new Network.AttackRequest(0, 100, 16), now);
    }

    @Test void hitsMultipleTargetsOnceButNeverSelfOrTargetsOutsideHitbox() {
        CombatPlayer attacker = player(1, 0, 0);
        CombatPlayer first = player(2, 32, 0);
        CombatPlayer second = player(3, 40, 8);
        CombatPlayer behind = player(4, -40, 0);
        CombatPlayer far = player(5, 100, 0);
        combat.register(first); // Same object registered twice must not double damage.
        assertEquals(2, combat.resolve(attacker.combatId(), swing(0)).size());
        assertEquals(15f, first.health());
        assertEquals(15f, second.health());
        assertEquals(20f, attacker.health());
        assertEquals(20f, behind.health());
        assertEquals(20f, far.health());
    }

    @Test void rejectedCooldownCannotDealDamageAndNextSwingCan() {
        player(1, 0, 0);
        CombatPlayer victim = player(2, 32, 0);
        combat.resolve("player:1", swing(0));
        assertTrue(combat.resolve("player:1", swing(1)).isEmpty());
        assertEquals(15f, victim.health());
        combat.resolve("player:1", swing(600_000_000));
        assertEquals(10f, victim.health());
    }

    @Test void deadAndDisconnectedTargetsAreExcluded() {
        player(1, 0, 0);
        CombatPlayer dead = player(2, 32, 0);
        CombatPlayer disconnected = player(3, 32, 0);
        dead.takeDamage(100);
        combat.remove(disconnected.combatId());
        assertTrue(combat.resolve("player:1", swing(0)).isEmpty());
        assertEquals(0f, dead.health());
        assertEquals(20f, disconnected.health());
    }

    @Test void deadOrRemovedAttackerCannotDealDamage() {
        CombatPlayer attacker = player(1, 0, 0);
        CombatPlayer victim = player(2, 32, 0);
        var accepted = swing(0);
        attacker.takeDamage(100);
        assertTrue(combat.resolve(attacker.combatId(), accepted).isEmpty());
        combat.remove(attacker.combatId());
        assertTrue(combat.resolve(attacker.combatId(), accepted).isEmpty());
        assertEquals(20f, victim.health());
    }

    @Test void playerNeedsPositionAndCannotMoveAfterDeath() {
        player(1, 0, 0);
        CombatPlayer victim = new CombatPlayer(2);
        combat.register(victim);
        assertFalse(victim.updatePosition(Float.NaN, 0));
        assertTrue(combat.resolve("player:1", swing(0)).isEmpty());
        victim.updatePosition(32, 0);
        victim.takeDamage(100);
        assertFalse(victim.updatePosition(200, 100));
        assertEquals(32f, victim.x());
        assertEquals(0f, victim.y());
        assertThrows(IllegalArgumentException.class, () -> victim.takeDamage(-1));
    }

    @Test void enemyAdapterReceivesDamageThroughSameContract() {
        player(1, 0, 0);
        FakeEnemy enemy = new FakeEnemy();
        combat.register(enemy);
        assertEquals(1, combat.resolve("player:1", swing(0)).size());
        assertEquals(5f, enemy.damage);
        assertEquals(1, enemy.hits);
        combat.remove(enemy.combatId());
        assertTrue(combat.resolve("player:1", swing(600_000_000)).isEmpty());
    }

    @Test void duplicateIdsAreRejectedRatherThanReplacingTargets() {
        player(1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> combat.register(new CombatPlayer(1)));
    }

    @Test void healthPacketRoundTripsBetweenIndependentEndpoints() {
        Client sender = new Client();
        Client receiver = new Client();
        try {
            Network.register(sender);
            Network.register(receiver);
            Network.PlayerHealth packet = new Network.PlayerHealth();
            packet.playerId = 7;
            packet.health = 12.5f;
            packet.x = -32;
            packet.y = 64;
            Output output = new Output(128, -1);
            sender.getKryo().writeClassAndObject(output, packet);
            Network.PlayerHealth copy = (Network.PlayerHealth) receiver.getKryo()
                .readClassAndObject(new Input(output.toBytes()));
            assertEquals(packet.playerId, copy.playerId);
            assertEquals(packet.health, copy.health);
            assertEquals(packet.x, copy.x);
            assertEquals(packet.y, copy.y);
        } finally {
            sender.close();
            receiver.close();
        }
    }

    private static final class FakeEnemy implements CombatTarget {
        float damage;
        int hits;
        public String combatId() { return "enemy:test"; }
        public float x() { return 35; }
        public float y() { return 8; }
        public float width() { return 16; }
        public float height() { return 16; }
        public boolean isAlive() { return damage < 30; }
        public void takeDamage(float amount) { damage += amount; hits++; }
        public void moveTo(float x, float y) {}
    }
}
