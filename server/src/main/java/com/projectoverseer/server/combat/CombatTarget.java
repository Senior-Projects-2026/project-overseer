package com.projectoverseer.server.combat;

/** Server-side target. Bounds use world units and a bottom-left origin, not tile coordinates. */
public interface CombatTarget {
    /** Stable, unique ID, e.g. player:12 or enemy:12. */
    String combatId();
    float x();
    float y();
    float width();
    float height();
    boolean isAlive();
    void takeDamage(float damage);
}
