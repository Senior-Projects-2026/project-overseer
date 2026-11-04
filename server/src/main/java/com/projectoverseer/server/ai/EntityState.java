package com.projectoverseer.server.ai;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;

/**
 * Stateless, shared FSM states. All per-entity data lives in {@link AiEntity}.
 * Behavior that differs per mob type belongs in AiEntity hooks / AiConfig, not here.
 */
public enum EntityState implements State<AiEntity> {

    GLOBAL_STATE {
        @Override
        public void update(AiEntity entity) {
            // Global interrupt: flee from any state if health is low and a threat is close.
            // Skip FLEE (already fleeing) and RETURN (resetting, ignores threats).
            EntityState current = entity.getCurrentState();
            if (current != FLEE && current != RETURN && entity.shouldFlee()) {
                entity.getStateMachine().changeState(FLEE);
            }
        }
    },

    IDLE {
        @Override
        public void enter(AiEntity entity) {
            // Wait between 2 and 5 seconds before wandering
            entity.setStateTimer(MathUtils.random(2.0f, 5.0f));
        }

        @Override
        public void update(AiEntity entity) {
            // 1. If a valid target is inside aggro radius, start chasing
            if (entity.isTargetInRange(entity.getConfig().aggroRadius())) {
                entity.getStateMachine().changeState(CHASE);
                return;
            }

            // 2. Otherwise count down idle timer and transition to WANDER
            entity.decrementStateTimer();
            if (entity.getStateTimer() <= 0.0f) {
                entity.getStateMachine().changeState(WANDER);
            }
        }

        @Override
        public void exit(AiEntity entity) {
            entity.setStateTimer(0.0f);
        }
    },

    WANDER {
        @Override
        public void enter(AiEntity entity) {
            // Pick a random point around spawnPosition within wanderRadius
            float maxDistance = entity.getConfig().wanderRadius();
            float minDistance = Math.min(1.5f, maxDistance); // safe when wanderRadius < 1.5
            float angle = MathUtils.random(0.0f, MathUtils.PI2);
            float distance = MathUtils.random(minDistance, maxDistance);

            entity.getTargetPosition().set(
                entity.getSpawnPosition().x + MathUtils.cos(angle) * distance,
                entity.getSpawnPosition().y + MathUtils.sin(angle) * distance
            );
            // Failsafe timeout in case the entity gets blocked by an obstacle
            entity.setStateTimer(6.0f);
        }

        @Override
        public void update(AiEntity entity) {
            // 1. Check if a target entered aggro range
            if (entity.isTargetInRange(entity.getConfig().aggroRadius())) {
                entity.getStateMachine().changeState(CHASE);
                return;
            }

            // 2. Move towards the wander destination
            entity.moveTowards(entity.getTargetPosition(), entity.getConfig().wanderSpeed());
            entity.decrementStateTimer();

            // 3. Return to IDLE once destination is reached or wander timer expires
            if (entity.hasReachedPosition(entity.getTargetPosition(), 0.2f) || entity.getStateTimer() <= 0.0f) {
                entity.getStateMachine().changeState(IDLE);
            }
        }

        @Override
        public void exit(AiEntity entity) {
            entity.setStateTimer(0.0f);
        }
    },

    CHASE {
        @Override
        public void enter(AiEntity entity) {
            if (entity.hasValidTarget()) {
                entity.getTargetPosition().set(entity.getTargetEntity().getPosition());
            }
        }

        @Override
        public void update(AiEntity entity) {
            // 1. Target died or disconnected
            if (!entity.hasValidTarget()) {
                entity.setTargetEntity(null);
                entity.getStateMachine().changeState(IDLE);
                return;
            }

            // 2. Dragged too far from spawn: give up and walk home
            if (entity.isOutsideLeash()) {
                entity.getStateMachine().changeState(RETURN);
                return;
            }

            // 3. Transition to ATTACK once within attack range
            if (entity.isTargetInRange(entity.getConfig().attackRange())) {
                entity.getStateMachine().changeState(ATTACK);
                return;
            }

            // 4. Track target position and move towards it
            entity.getTargetPosition().set(entity.getTargetEntity().getPosition());
            entity.moveTowards(entity.getTargetPosition(), entity.getConfig().chaseSpeed());
        }
    },

    ATTACK {
        @Override
        public void enter(AiEntity entity) {
            if (entity.hasValidTarget()) {
                entity.getTargetPosition().set(entity.getTargetEntity().getPosition());
            }
        }

        @Override
        public void update(AiEntity entity) {
            // 1. Return to IDLE if target died or disconnected
            if (!entity.hasValidTarget()) {
                entity.setTargetEntity(null);
                entity.getStateMachine().changeState(IDLE);
                return;
            }

            // 2. Hysteresis buffer (1.2x attackRange) prevents flickering between ATTACK and CHASE
            //    (no leash check needed here: the entity doesn't move while attacking)
            if (!entity.isTargetInRange(entity.getConfig().attackRange() * 1.2f)) {
                entity.getStateMachine().changeState(CHASE);
                return;
            }

            // 3. Execute attack whenever cooldown finishes
            if (entity.getAttackCooldownTimer() <= 0.0f) {
                entity.performAttack();
                entity.resetAttackCooldownTimer();
            }
        }
    },

    FLEE {
        @Override
        public void enter(AiEntity entity) {
            // Flee for up to 4 seconds
            entity.setStateTimer(4.0f);
        }

        @Override
        public void update(AiEntity entity) {
            // 1. Threat gone: go home and recover
            if (!entity.hasValidTarget()) {
                entity.getStateMachine().changeState(RETURN);
                return;
            }

            // 2. Move directly away from threat's position
            entity.getTargetPosition().set(entity.getTargetEntity().getPosition());
            entity.moveAwayFrom(entity.getTargetPosition(), entity.getConfig().fleeSpeed());
            entity.decrementStateTimer();

            // 3. Safe distance reached or flee timer ran out: go home and recover
            if (!entity.isTargetInRange(entity.getConfig().safeDistance()) || entity.getStateTimer() <= 0.0f) {
                entity.getStateMachine().changeState(RETURN);
            }
        }

        @Override
        public void exit(AiEntity entity) {
            entity.setStateTimer(0.0f);
        }
    },

    /**
     * Walk back to spawn ignoring all targets, then heal to full.
     * Fixes two problems: being dragged across the map (leash) and the flee loop
     * (the entity comes back at full health, so it won't immediately flee again).
     */
    RETURN {
        @Override
        public void enter(AiEntity entity) {
            entity.setTargetEntity(null);
            entity.getTargetPosition().set(entity.getSpawnPosition());
        }

        @Override
        public void update(AiEntity entity) {
            entity.moveTowards(entity.getSpawnPosition(), entity.getConfig().chaseSpeed());

            if (entity.hasReachedPosition(entity.getSpawnPosition(), 0.2f)) {
                entity.setHealth(entity.getConfig().maxHealth());
                entity.getStateMachine().changeState(IDLE);
            }
        }
    };

    // Default no-op implementations; constants override only what they need.
    @Override
    public void enter(AiEntity entity) {
    }

    @Override
    public void exit(AiEntity entity) {
    }

    @Override
    public boolean onMessage(AiEntity entity, Telegram telegram) {
        return false;
    }
}
