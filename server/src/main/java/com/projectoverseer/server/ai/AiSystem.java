package com.projectoverseer.server.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Owns all AI entities. Call {@link #update(float, Collection)} once per server tick
 * from the game loop, on the game-loop thread only.
 */
public class AiSystem {

    /**
     * Target scanning is relatively expensive, so it runs a few times per second, not every tick.
     */
    private static final float SCAN_INTERVAL = 0.3f;

    private final List<AiEntity> entities = new ArrayList<>();
    private float scanTimer;

    public AiEntity spawn(String id, float x, float y, AiConfig config) {
        AiEntity entity = new AiEntity(id, x, y, config);
        entities.add(entity);
        return entity;
    }

    public void update(float dt, Collection<? extends Targetable> players) {
        scanTimer -= dt;
        boolean scan = scanTimer <= 0.0f;
        if (scan) {
            scanTimer = SCAN_INTERVAL;
        }

        for (AiEntity entity : entities) {
            // A target that left the players collection (disconnect, removal) must be dropped,
            // even if its isAlive() still returns true. The FSM then falls back to IDLE / RETURN.
            Targetable current = entity.getTargetEntity();
            if (current != null && !players.contains(current)) {
                entity.setTargetEntity(null);
            }

            if (scan && !entity.hasValidTarget() && entity.canAcquireTarget()) {
                entity.setTargetEntity(findNearestInAggroRange(entity, players));
            }
            entity.update(dt);
        }
    }

    /**
     * Removes and returns dead entities so the caller can notify clients / schedule respawns.
     */
    public List<AiEntity> removeDead() {
        List<AiEntity> removed = new ArrayList<>();
        Iterator<AiEntity> it = entities.iterator();
        while (it.hasNext()) {
            AiEntity entity = it.next();
            if (!entity.isAlive()) {
                removed.add(entity);
                it.remove();
            }
        }
        return removed;
    }

    public List<AiEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    private static Targetable findNearestInAggroRange(AiEntity entity, Collection<? extends Targetable> candidates) {
        float aggro = entity.getConfig().aggroRadius();
        float bestDst2 = aggro * aggro;
        Targetable best = null;

        for (Targetable candidate : candidates) {
            if (!candidate.isAlive()) {
                continue;
            }
            float dst2 = entity.getPosition().dst2(candidate.getPosition());
            if (dst2 <= bestDst2) {
                bestDst2 = dst2;
                best = candidate;
            }
        }
        return best;
    }
}
