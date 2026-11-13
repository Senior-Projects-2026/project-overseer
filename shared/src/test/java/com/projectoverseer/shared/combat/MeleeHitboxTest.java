package com.projectoverseer.shared.combat;

import org.junit.Test;
import static org.junit.Assert.*;

public class MeleeHitboxTest {
    private static final WeaponStats STATS = new WeaponStats(10, 40, 20, 1, 10);

    @Test public void forwardReachWidthAndTouchingEdges() {
        var hit = MeleeHitbox.toward(0, 0, 1, 0, STATS);
        assertTrue(hit.intersects(10, -5, 10, 10));
        assertTrue(hit.intersects(40, -5, 2, 10));
        assertFalse(hit.intersects(40.01, -5, 2, 10));
        assertFalse(hit.intersects(-10, -5, 2, 10));
        assertFalse(hit.intersects(10, 10.01, 2, 2));
        assertTrue(hit.intersects(39, -30, 5, 60)); // center outside, bounds overlap
    }

    @Test public void diagonalRejectsTargetsInsideBoundingBoxButOutsideAttack() {
        var hit = MeleeHitbox.toward(0, 0, 10, 10, STATS);
        assertTrue(hit.intersects(20, 20, 2, 2));
        assertFalse(hit.intersects(0, 25, 2, 2));
        assertFalse(hit.intersects(29, 29, 2, 2));
    }

    @Test public void aimDistanceDoesNotExtendReach() {
        var near = MeleeHitbox.toward(0, 0, 1, 0, STATS);
        var far = MeleeHitbox.toward(0, 0, 10000, 0, STATS);
        for (int x = -10; x < 60; x++) {
            assertEquals(near.intersects(x, 0, 2, 2), far.intersects(x, 0, 2, 2));
        }
    }

    @Test public void worksFacingLeftAndDownAtNegativeCoordinates() {
        assertTrue(MeleeHitbox.toward(-50, -50, -100, -50, STATS).intersects(-80, -55, 5, 5));
        assertTrue(MeleeHitbox.toward(-50, -50, -50, -100, STATS).intersects(-55, -80, 5, 5));
    }

    @Test public void rejectsUndefinedAimAndInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> MeleeHitbox.toward(0, 0, 0, 0, STATS));
        assertThrows(IllegalArgumentException.class, () -> MeleeHitbox.toward(0, 0, Double.NaN, 0, STATS));
        var hit = MeleeHitbox.toward(0, 0, 1, 0, STATS);
        assertThrows(IllegalArgumentException.class, () -> hit.intersects(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> hit.intersects(Double.POSITIVE_INFINITY, 0, 1, 1));
    }
}
