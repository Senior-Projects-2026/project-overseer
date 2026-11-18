package com.projectoverseer.server.survival;

import com.projectoverseer.server.combat.CombatPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(initialHealth, combatPlayer.health());
    }

    @Test
    void shouldDamagePlayerWhenHungerReachesZero() {
        float initialHealth = combatPlayer.health();

        survivalSystem.update(999f, survivalPlayers, combatPlayers);
        assertEquals(initialHealth, combatPlayer.health());

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
        survivalSystem.update(999f, survivalPlayers, combatPlayers);

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
            survivalPlayer.hunger()
        );
    }
}
