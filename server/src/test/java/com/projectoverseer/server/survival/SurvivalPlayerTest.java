package com.projectoverseer.server.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SurvivalPlayerTest {
    private SurvivalPlayer survivalPlayer;

    @BeforeEach
    void setUp() {
        survivalPlayer = new SurvivalPlayer();
    }

    @Test
    void shouldStartWithMaximumHunger() {
        assertEquals(SurvivalRules.PLAYER_MAX_HUNGER, survivalPlayer.hunger());
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldDepleteHungerOverTime() {
        survivalPlayer.update(10f);

        assertEquals(99f, survivalPlayer.hunger(), 0.001f);
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldStopHungerAtZero() {
        survivalPlayer.update(2000f);

        assertEquals(0f, survivalPlayer.hunger());
        assertTrue(survivalPlayer.isStarving());
    }

    @Test
    void shouldRemainAtZeroAfterFurtherUpdates() {
        survivalPlayer.update(2000f);
        survivalPlayer.update(100f);

        assertEquals(0f, survivalPlayer.hunger());
        assertTrue(survivalPlayer.isStarving());
    }

    @Test
    void shouldResetHunger() {
        survivalPlayer.update(500f);

        survivalPlayer.reset();

        assertEquals(SurvivalRules.PLAYER_MAX_HUNGER, survivalPlayer.hunger());
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldRejectNegativeDeltaTime() {
        assertThrows(
            IllegalArgumentException.class,
            () -> survivalPlayer.update(-1f)
        );
    }

    @Test
    void shouldRejectInvalidDeltaTime() {
        assertThrows(
            IllegalArgumentException.class,
            () -> survivalPlayer.update(Float.NaN)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> survivalPlayer.update(Float.POSITIVE_INFINITY)
        );
    }
}
