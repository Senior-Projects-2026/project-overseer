
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
        assertEquals(20f, survivalPlayer.hunger());
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldDepleteHungerOverTime() {
        survivalPlayer.update(10f);

        assertEquals(19f, survivalPlayer.hunger(), 0.001f);
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
        survivalPlayer.update(50f);

        survivalPlayer.reset();

        assertEquals(20f, survivalPlayer.hunger());
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

    @Test
    void shouldRestoreHunger() {
        survivalPlayer.update(100f);

        survivalPlayer.restoreHunger(4f);

        assertEquals(14f, survivalPlayer.hunger(), 0.001f);
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldNotExceedMaximumHungerWhenRestoring() {
        survivalPlayer.update(10f);

        survivalPlayer.restoreHunger(10f);

        assertEquals(20f, survivalPlayer.hunger());
    }

    @Test
    void shouldRestoreHungerFromZero() {
        survivalPlayer.update(2000f);

        survivalPlayer.restoreHunger(4f);

        assertEquals(4f, survivalPlayer.hunger(), 0.001f);
        assertFalse(survivalPlayer.isStarving());
    }

    @Test
    void shouldRejectInvalidHungerRestoration() {
        assertThrows(IllegalArgumentException.class,
            () -> survivalPlayer.restoreHunger(0f));

        assertThrows(IllegalArgumentException.class,
            () -> survivalPlayer.restoreHunger(-1f));

        assertThrows(IllegalArgumentException.class,
            () -> survivalPlayer.restoreHunger(Float.NaN));

        assertThrows(IllegalArgumentException.class,
            () -> survivalPlayer.restoreHunger(Float.POSITIVE_INFINITY));
    }
}
