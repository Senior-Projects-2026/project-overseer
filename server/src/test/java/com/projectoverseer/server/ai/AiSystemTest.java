package com.projectoverseer.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiSystemTest {

    private static final float DT = 0.05f;

    private static final AiConfig CONFIG = new AiConfig(100f, 1f, 5f, 6f, 5f, 8f, 15f, 1.5f, 10f, 1f, 0.25f, 10f);

    private AiSystem system;
    private AiEntity entity;
    private FakeTarget target;
    private List<Targetable> players;

    @BeforeEach
    void setUp() {
        system = new AiSystem();
        entity = system.spawn("mob", 0f, 0f, CONFIG);
        target = new FakeTarget(1f, 0f);
        players = new ArrayList<>(List.of(target));

        run(3); // acquire target, IDLE -> CHASE -> ATTACK -> first hit
        assertEquals(EntityState.ATTACK, entity.getCurrentState());
        assertEquals(90f, target.getHealth(), 0.001f);
    }

    @Test
    void targetRemovedFromPlayersIsDroppedEvenIfStillAlive() {
        players.remove(target); // e.g. disconnect handler removed it; isAlive() is still true

        system.update(DT, players);

        assertNull(entity.getTargetEntity());
        assertEquals(EntityState.IDLE, entity.getCurrentState());

        run(40); // long enough for another attack if the mob still had the target
        assertEquals(90f, target.getHealth(), 0.001f);
    }

    @Test
    void disconnectedTargetIsDropped() {
        target.disconnect(); // still in the collection, but isAlive() is now false

        system.update(DT, players);

        assertNull(entity.getTargetEntity());
        assertEquals(EntityState.IDLE, entity.getCurrentState());

        run(40);
        assertEquals(90f, target.getHealth(), 0.001f);
    }

    private void run(int ticks) {
        for (int i = 0; i < ticks; i++) {
            system.update(DT, players);
        }
    }

    private static final class FakeTarget implements Targetable {
        private final Vector2 position = new Vector2();
        private float health = 100f;
        private boolean connected = true;

        FakeTarget(float x, float y) {
            position.set(x, y);
        }

        void disconnect() {
            connected = false;
        }

        @Override
        public Vector2 getPosition() {
            return position;
        }

        @Override
        public boolean isAlive() {
            return connected && health > 0f;
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
