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

    @Test
    void shouldRegenerateHealthWhenHungerIsFull() {
        combatPlayer.takeDamage(10f);

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(
            CombatRules.PLAYER_MAX_HEALTH - 10f
                + SurvivalRules.HEALTH_REGENERATION_RATE,
            combatPlayer.health(),
            0.001f
        );
    }

    @Test
    public void shouldRegenerateHealthWhenHungerIsAboveThreshold() {
        SurvivalPlayer player = new SurvivalPlayer();
        player.update(50f);

        CombatPlayer combat = new CombatPlayer(1);
        combat.takeDamage(CombatRules.PLAYER_MAX_HEALTH / 2f);

        SurvivalSystem system = new SurvivalSystem();
        system.update(1f, Map.of(1, player), Map.of(1, combat));

        assertEquals(
            CombatRules.PLAYER_MAX_HEALTH / 2f + SurvivalRules.HEALTH_REGENERATION_RATE,
            combat.health(),
            0.001f
        );
    }

    @Test
    public void shouldNotRegenerateHealthWhenHungerIsAtThreshold() {
        SurvivalPlayer survivalPlayer1 = new SurvivalPlayer();
        survivalPlayer1.update(100f); // Hunger: 90

        CombatPlayer combatPlayer1 = new CombatPlayer(1);
        combatPlayer1.takeDamage(10f);

        SurvivalSystem system = new SurvivalSystem();

        system.update(1f, Map.of(1, survivalPlayer1), Map.of(1, combatPlayer1));

        assertEquals(CombatRules.PLAYER_MAX_HEALTH - 10f,
                combatPlayer1.health(), 0.001f);
    }

    @Test
    public void shouldNotRegenerateHealthWhenHungerIsBelowThreshold() {
        SurvivalPlayer survivalPlayer1 = new SurvivalPlayer();
        survivalPlayer1.update(110f); // Hunger: 89

        CombatPlayer combatPlayer1 = new CombatPlayer(1);
        combatPlayer1.takeDamage(10f);

        SurvivalSystem system = new SurvivalSystem();

        system.update(1f, Map.of(1, survivalPlayer1), Map.of(1, combatPlayer1));

        assertEquals(CombatRules.PLAYER_MAX_HEALTH - 10f,
                combatPlayer1.health(), 0.001f);
    }

    @Test
    void shouldNotRegenerateHealthBeyondMaximum() {
        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(CombatRules.PLAYER_MAX_HEALTH, combatPlayer.health());
    }

    @Test
    void shouldNotRegenerateDeadPlayers() {
        combatPlayer.takeDamage(combatPlayer.health());

        survivalSystem.update(1f, survivalPlayers, combatPlayers);

        assertEquals(0f, combatPlayer.health());
    }
}
