
package com.projectoverseer.server.survival;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.projectoverseer.server.combat.CombatPlayer;
import com.projectoverseer.shared.combat.CombatRules;

class SurvivalSystemTest {

    private SurvivalSystem survivalSystem;
    private SurvivalPlayer survivalPlayer;
    private CombatPlayer combatPlayer;
    private Map<Integer, SurvivalPlayer> survivalPlayers;
    private Map<Integer, CombatPlayer> combatPlayers;

    @BeforeEach
    void setUp() {
        survivalSystem = new SurvivalSystem();
        survivalPlayer = new SurvivalPlayer();
        combatPlayer = new CombatPlayer(1);

        survivalPlayers = new HashMap<>();
        combatPlayers = new HashMap<>();

        survivalPlayers.put(1, survivalPlayer);
        combatPlayers.put(1, combatPlayer);
    }

    @Test
    void shouldDepleteHungerWithoutDamagingHealth() {
        float initialHealth = combatPlayer.health();

        survivalSystem.update(10f, survivalPlayers, combatPlayers);

        assertEquals(
            SurvivalRules.PLAYER_MAX_HUNGER - 1f,
            survivalPlayer.hunger(),
            0.001f
        );
        assertEquals(initialHealth, combatPlayer.health(), 0.001f);
    }

    @Test
    void shouldDamagePlayerWhenHungerReachesZero() {
        survivalSystem.update(21f, survivalPlayers, combatPlayers);

        combatPlayer.takeDamage(5f);
        float initialHealth = combatPlayer.health();

        survivalSystem.update(178f, survivalPlayers, combatPlayers);

        assertEquals(0.1f, survivalPlayer.hunger(), 0.001f);
        assertEquals(initialHealth, combatPlayer.health(), 0.001f);

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(0f, survivalPlayer.hunger(), 0.001f);
        assertEquals(
            initialHealth - SurvivalRules.STARVATION_DAMAGE_RATE,
            combatPlayer.health(),
            0.001f
        );
    }

    @Test
    void shouldReportPlayerDeathFromStarvation() {
        survivalSystem.update(199f, survivalPlayers, combatPlayers);

        List<Integer> deadPlayers = List.of();

        while (combatPlayer.isAlive()) {
            deadPlayers = survivalSystem.update(1f, survivalPlayers, combatPlayers);
        }

        assertEquals(List.of(1), deadPlayers);
        assertFalse(combatPlayer.isAlive());
    }

    @Test
    void shouldNotReportAlreadyDeadPlayerAgain() {
        combatPlayer.takeDamage(combatPlayer.health());

        List<Integer> deadPlayers =
            survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertTrue(deadPlayers.isEmpty());
    }

    @Test
    void shouldSkipPlayersWithoutCombatPlayer() {
        combatPlayers.clear();

        survivalSystem.update(10f, survivalPlayers, combatPlayers);

        assertEquals(
            SurvivalRules.PLAYER_MAX_HUNGER,
            survivalPlayer.hunger(),
            0.001f
        );
    }

    @Test
    void shouldRegenerateHealthWhenHungerIsFull() {
        combatPlayer.takeDamage(10f);
        float damagedHealth = combatPlayer.health();

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(
            damagedHealth + SurvivalRules.HEALTH_REGENERATION_RATE,
            combatPlayer.health(),
            0.001f
        );
    }

    @Test
    void shouldRegenerateHealthWhenHungerIsAboveThreshold() {
        survivalPlayer.update(10f); // Hunger: 19
        combatPlayer.takeDamage(10f);
        float damagedHealth = combatPlayer.health();

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(18.9f, survivalPlayer.hunger(), 0.001f);
        assertEquals(
            damagedHealth + SurvivalRules.HEALTH_REGENERATION_RATE,
            combatPlayer.health(),
            0.001f
        );
    }

    @Test
    void shouldNotRegenerateHealthWhenHungerIsAtThreshold() {
        survivalPlayer.update(20f); // Hunger: 18
        combatPlayer.takeDamage(10f);
        float damagedHealth = combatPlayer.health();

        survivalSystem.update(0f, survivalPlayers, combatPlayers);

        assertEquals(18f, survivalPlayer.hunger(), 0.001f);
        assertEquals(damagedHealth, combatPlayer.health(), 0.001f);
    }

    @Test
    void shouldNotRegenerateHealthWhenHungerIsBelowThreshold() {
        survivalPlayer.update(110f); // Hunger: 9
        combatPlayer.takeDamage(10f);
        float damagedHealth = combatPlayer.health();

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(8.9f, survivalPlayer.hunger(), 0.001f);
        assertEquals(damagedHealth, combatPlayer.health(), 0.001f);
    }

    @Test
    void shouldNotRegenerateHealthBeyondMaximum() {
        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(CombatRules.PLAYER_MAX_HEALTH, combatPlayer.health(), 0.001f);
    }

    @Test
    void shouldNotRegenerateDeadPlayers() {
        combatPlayer.takeDamage(combatPlayer.health());

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(0f, combatPlayer.health(), 0.001f);
    }
}
