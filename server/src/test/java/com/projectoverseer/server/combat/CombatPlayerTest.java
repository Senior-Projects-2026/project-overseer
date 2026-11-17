package com.projectoverseer.server.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.projectoverseer.shared.combat.CombatRules;

class CombatPlayerTest {

    @Test
    void respawnRestoresHealth() {
        CombatPlayer player = new CombatPlayer(1);

        player.takeDamage(CombatRules.PLAYER_MAX_HEALTH);

        assertFalse(player.isAlive());

        player.respawn(200f, 200f);

        assertEquals(CombatRules.PLAYER_MAX_HEALTH, player.health());
        assertTrue(player.isAlive());
    }

    @Test
    void respawnRestoresPosition() {
        CombatPlayer player = new CombatPlayer(1);

        player.respawn(300f, 450f);

        assertEquals(300f, player.x(), 0.001f);
        assertEquals(450f, player.y(), 0.001f);
    }

    @Test
    void respawnIncrementsMovementRevision() {
        CombatPlayer player = new CombatPlayer(1);

        int previousRevision = (int)player.movementRevision();

        player.respawn(200f, 200f);

        assertTrue(player.movementRevision() > previousRevision);
    }
}
