package com.projectoverseer.server.combat;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.combat.CombatConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnockbackTest {
    private CombatPlayer player(float x, float y) {
        CombatPlayer player = new CombatPlayer(1);
        player.updatePosition(x, y);
        return player;
    }

    @Test void normalizesDirectionAndMovesConfiguredDistance() {
        CombatPlayer player = player(0, 0);
        Knockback.apply(player, 3, 4, 10, (x, y) -> true);
        assertEquals(6, player.x(), 0.001);
        assertEquals(8, player.y(), 0.001);
        assertEquals(1, player.movementRevision());
    }

    @Test void stopsBeforeWallEvenWithLargeDisplacement() {
        CombatPlayer player = player(0, 0);
        Knockback.apply(player, 1, 0, 100, (x, y) -> x != 1);
        assertEquals(9, player.x()); // Feet end at x=31, before blocked tile at x=32.
        assertEquals(0, player.y());
    }

    @Test void stopsAtNegativeCoordinateObstacle() {
        CombatPlayer player = player(0, 0);
        Knockback.apply(player, -1, 0, 40, (x, y) -> x >= 0);
        assertEquals(-10, player.x());
    }

    @Test void blockedAndZeroKnockbackDoNotMovePlayer() {
        CombatPlayer player = player(0, 0);
        Knockback.apply(player, 1, 0, 0, (x, y) -> true);
        assertEquals(0, player.movementRevision());
        Knockback.apply(player, 1, 0, 8, (x, y) -> false);
        assertEquals(0, player.x());
    }

    @Test void oldMovementCannotUndoCorrectionIncludingRepeatedHits() {
        CombatPlayer player = player(0, 0);
        Knockback.apply(player, 1, 0, 8, (x, y) -> true);
        assertFalse(player.updatePosition(0, 0, 0));
        assertEquals(8, player.x());
        assertTrue(player.updatePosition(9, 0, 1));
        Knockback.apply(player, 1, 0, 8, (x, y) -> true);
        assertFalse(player.updatePosition(9, 0, 1));
        assertTrue(player.updatePosition(18, 0, 2));
    }

    @Test void validatedAttackAppliesDamageAndKnockbackThenEnforcesCooldown() {
        CombatSystem combat = new CombatSystem();
        CombatPlayer attacker = player(0, 0);
        CombatPlayer victim = new CombatPlayer(2);
        victim.updatePosition(32, 0);
        combat.register(attacker);
        combat.register(victim);
        AttackValidator validator = new AttackValidator(CombatConfig.defaults());
        Inventory inventory = new Inventory();
        Network.AttackRequest request = new Network.AttackRequest(0, 100, 16);
        var swing = validator.accept(1, inventory, 16, 16, true, request, 0);
        for (CombatTarget hit : combat.resolve(attacker.combatId(), swing)) {
            Knockback.apply(hit, swing.directionX(), swing.directionY(), swing.stats().knockback(), (x, y) -> true);
        }
        assertEquals(15, victim.health());
        assertEquals(40, victim.x());
        assertNull(validator.accept(1, inventory, 16, 16, true, request, 1));
        assertFalse(victim.updatePosition(32, 0, 0));
    }
}
