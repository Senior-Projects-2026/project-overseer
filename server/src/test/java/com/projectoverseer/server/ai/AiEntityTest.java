package com.projectoverseer.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.projectoverseer.server.ai.AiConfig;
import com.projectoverseer.server.ai.AiEntity;
import com.projectoverseer.server.ai.EntityState;
import com.projectoverseer.server.ai.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiEntityTest {

    private static final float DT = 0.05f;

    //                                         hp   wSpd cSpd fSpd wRad aggro leash aRng dmg  cd   flee  safe
    private static final AiConfig CONFIG = new AiConfig(100f, 1f, 5f, 6f, 5f, 8f, 15f, 1.5f, 10f, 1f, 0.25f, 10f);

    private AiEntity entity;

    @BeforeEach
    void setUp() {
        entity = new AiEntity("test", 0f, 0f, CONFIG);
    }

    @Test
    void startsInIdleWithTimerSet() {
        assertEquals(EntityState.IDLE, entity.getCurrentState());
        // proves IDLE.enter() ran for the initial state
        assertTrue(entity.getStateTimer() >= 2.0f && entity.getStateTimer() <= 5.0f);
    }

    @Test
    void idleBecomesWanderAfterTimer() {
        // idle timer is at most 5s; allow 10s of simulated time
        assertTrue(tickUntil(entity, EntityState.WANDER, 200));
    }

    @Test
    void targetInsideAggroRadiusStartsChase() {
        entity.setTargetEntity(new FakeTarget(7f, 0f));
        entity.update(DT);
        assertEquals(EntityState.CHASE, entity.getCurrentState());
    }

    @Test
    void targetOutsideAggroRadiusIsIgnored() {
        entity.setTargetEntity(new FakeTarget(20f, 0f));
        entity.update(DT);
        assertEquals(EntityState.IDLE, entity.getCurrentState());
    }

    @Test
    void chasesThenAttacksOnCooldown() {
        FakeTarget target = new FakeTarget(1f, 0f);
        entity.setTargetEntity(target);

        run(entity, 3); // IDLE -> CHASE -> ATTACK -> first hit
        assertEquals(EntityState.ATTACK, entity.getCurrentState());
        assertEquals(90f, target.getHealth(), 0.001f);

        run(entity, 7); // still on cooldown
        assertEquals(90f, target.getHealth(), 0.001f);

        run(entity, 23); // ~1.65s after first hit: exactly one more hit
        assertEquals(80f, target.getHealth(), 0.001f);
    }

    @Test
    void leashBreakSendsEntityHomeAndHeals() {
        entity.setHealth(50f);
        FakeTarget target = new FakeTarget(7f, 0f);
        entity.setTargetEntity(target);

        entity.update(DT);
        assertEquals(EntityState.CHASE, entity.getCurrentState());

        target.getPosition().set(30f, 0f); // player runs far away
        assertTrue(tickUntil(entity, EntityState.RETURN, 200));
        assertNull(entity.getTargetEntity());

        assertTrue(tickUntil(entity, EntityState.IDLE, 200));
        assertTrue(entity.hasReachedPosition(entity.getSpawnPosition(), 0.25f));
        assertEquals(100f, entity.getHealth(), 0.001f);
    }

    @Test
    void lowHealthFleesThenReturnsAndHealsWithoutLooping() {
        entity.setHealth(20f); // 20% <= 25% threshold
        entity.setTargetEntity(new FakeTarget(1f, 0f));

        entity.update(DT);
        assertEquals(EntityState.FLEE, entity.getCurrentState());

        assertTrue(tickUntil(entity, EntityState.RETURN, 200));
        assertNull(entity.getTargetEntity());

        assertTrue(tickUntil(entity, EntityState.IDLE, 200));
        assertEquals(100f, entity.getHealth(), 0.001f);
    }

    @Test
    void doesNotFleeWithoutThreat() {
        entity.setHealth(20f);
        entity.update(DT);
        assertEquals(EntityState.IDLE, entity.getCurrentState());
    }

    @Test
    void deadEntityDoesNothing() {
        FakeTarget target = new FakeTarget(1f, 0f);
        entity.setTargetEntity(target);
        entity.takeDamage(1000f);

        run(entity, 100);

        assertEquals(100f, target.getHealth(), 0.001f);
        assertEquals(EntityState.IDLE, entity.getCurrentState());
    }

    // ---------------------------------------------------------------------

    private static void run(AiEntity entity, int ticks) {
        for (int i = 0; i < ticks; i++) {
            entity.update(DT);
        }
    }

    private static boolean tickUntil(AiEntity entity, EntityState expected, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (entity.getCurrentState() == expected) {
                return true;
            }
            entity.update(DT);
        }
        return entity.getCurrentState() == expected;
    }

    private static final class FakeTarget implements Targetable {
        private final Vector2 position = new Vector2();
        private float health = 100f;

        FakeTarget(float x, float y) {
            position.set(x, y);
        }

        @Override
        public Vector2 getPosition() {
            return position;
        }

        @Override
        public boolean isAlive() {
            return health > 0f;
        }

        @Override
        public void takeDamage(float amount) {
            health -= amount;
        }

        float getHealth() {
            return health;
        }
    }
}
