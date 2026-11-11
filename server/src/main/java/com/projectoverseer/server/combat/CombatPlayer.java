package com.projectoverseer.server.combat;

import com.projectoverseer.shared.combat.CombatRules;

/** Health belongs to the server; a player becomes hittable after its first position update. */
public final class CombatPlayer implements CombatTarget {
    private final int playerId;
    private float x, y;
    private float health = CombatRules.PLAYER_MAX_HEALTH;
    private boolean positioned;

    public CombatPlayer(int playerId) { this.playerId = playerId; }
    public int playerId() { return playerId; }
    public String combatId() { return "player:" + playerId; }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return CombatRules.PLAYER_SIZE; }
    public float height() { return CombatRules.PLAYER_SIZE; }
    public float health() { return health; }
    public boolean isAlive() { return health > 0; }
    public boolean isPositioned() { return positioned; }

    public boolean updatePosition(float x, float y) {
        if (!isAlive() || !Float.isFinite(x) || !Float.isFinite(y)) return false;
        this.x = x;
        this.y = y;
        positioned = true;
        return true;
    }

    public void takeDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0) throw new IllegalArgumentException("Invalid damage");
        health = Math.max(0, health - damage);
    }
}
