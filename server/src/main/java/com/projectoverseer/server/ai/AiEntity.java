package com.projectoverseer.server.ai;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;

public class AiEntity implements Targetable {

    private final String id;
    private final AiConfig config;

    // Runtime vectors (initialized once per entity to avoid GC allocations per tick)
    private final Vector2 position = new Vector2();
    private final Vector2 spawnPosition = new Vector2();
    private final Vector2 targetPosition = new Vector2();
    private final Vector2 tmpDirection = new Vector2();

    private final StateMachine<AiEntity, EntityState> stateMachine;

    // Runtime state variables (change every tick)
    private float health;
    private float deltaTime;
    private float stateTimer;
    private float attackCooldownTimer;
    private Targetable targetEntity;

    public AiEntity(String id, float spawnX, float spawnY, AiConfig config) {
        this.id = id;
        this.config = config;
        this.health = config.maxHealth();

        this.spawnPosition.set(spawnX, spawnY);
        this.position.set(spawnX, spawnY);
        this.targetPosition.set(spawnX, spawnY);

        // DefaultStateMachine does NOT call enter() on the initial state passed to its constructor.
        // Start with no state and transition explicitly so IDLE.enter() runs (sets the idle timer).
        this.stateMachine = new DefaultStateMachine<>(this, null, EntityState.GLOBAL_STATE);
        this.stateMachine.changeState(EntityState.IDLE);
    }

    public void update(float dt) {
        if (!isAlive()) {
            return; // dead entities don't think, move or attack
        }
        this.deltaTime = dt;
        if (this.attackCooldownTimer > 0.0f) {
            this.attackCooldownTimer -= dt;
        }
        this.stateMachine.update();
    }

    // OVERRIDABLE BEHAVIOR HOOKS (Subclasses override these, NOT the FSM states)

    /**
     * Override in subclasses for ranged attacks, AoE spells, or custom packet broadcasts.
     */
    public void performAttack() {
        if (hasValidTarget()) {
            targetEntity.takeDamage(config.attackDamage());
        }
    }

    /**
     * Override in subclasses if a specific boss/mob has custom flee conditions.
     * Only flees if the threat is close enough to matter (within safeDistance), which also
     * guarantees FLEE does not exit on its very first tick.
     */
    public boolean shouldFlee() {
        return config.fleeHealthThreshold() > 0.0f
            && (health / config.maxHealth()) <= config.fleeHealthThreshold()
            && isTargetInRange(config.safeDistance());
    }

    /**
     * Override in subclasses if an entity is stationary or uses custom pathfinding (A*).
     */
    public void moveTowards(Vector2 destination, float speed) {
        tmpDirection.set(destination).sub(position);
        float dist = tmpDirection.len();
        if (dist > 0.001f) {
            tmpDirection.nor();
            float step = Math.min(dist, speed * deltaTime);
            position.mulAdd(tmpDirection, step);
        }
    }

    public void moveAwayFrom(Vector2 threatPosition, float speed) {
        tmpDirection.set(position).sub(threatPosition);
        if (tmpDirection.isZero(0.001f)) {
            tmpDirection.setToRandomDirection();
        } else {
            tmpDirection.nor();
        }
        position.mulAdd(tmpDirection, speed * deltaTime);
    }


    // GENERIC HELPER & STATE QUERIES

    public boolean hasReachedPosition(Vector2 destination, float stopRadius) {
        return position.dst2(destination) <= (stopRadius * stopRadius);
    }

    public boolean isTargetInRange(float range) {
        return hasValidTarget() && position.dst2(targetEntity.getPosition()) <= (range * range);
    }

    public boolean hasValidTarget() {
        return targetEntity != null && targetEntity.isAlive();
    }

    /** True when the entity has been pulled further from its spawn than leashRadius. */
    public boolean isOutsideLeash() {
        float r = config.leashRadius();
        return position.dst2(spawnPosition) > r * r;
    }

    /** Whether an external system (AiSystem) may assign this entity a new target right now. */
    public boolean canAcquireTarget() {
        EntityState state = getCurrentState();
        return isAlive() && (state == EntityState.IDLE || state == EntityState.WANDER);
    }

    @Override
    public boolean isAlive() {
        return health > 0.0f;
    }

    @Override
    public void takeDamage(float amount) {
        this.health = Math.max(0.0f, this.health - amount);
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public AiConfig getConfig() {
        return config;
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getSpawnPosition() {
        return spawnPosition;
    }

    public Vector2 getTargetPosition() {
        return targetPosition;
    }

    public StateMachine<AiEntity, EntityState> getStateMachine() {
        return stateMachine;
    }

    public EntityState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = Math.min(health, config.maxHealth());
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public float getStateTimer() {
        return stateTimer;
    }

    public void setStateTimer(float stateTimer) {
        this.stateTimer = stateTimer;
    }

    public void decrementStateTimer() {
        this.stateTimer -= this.deltaTime;
    }

    public float getAttackCooldownTimer() {
        return attackCooldownTimer;
    }

    public void resetAttackCooldownTimer() {
        this.attackCooldownTimer = config.attackCooldown();
    }

    public Targetable getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(Targetable targetEntity) {
        this.targetEntity = targetEntity;
    }
}
