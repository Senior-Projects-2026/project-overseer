package com.projectoverseer.server.combat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Register, remove and resolve on the same server thread as attack validation. */
public final class CombatSystem {
    private final Map<String, CombatTarget> targets = new LinkedHashMap<>();

    public void register(CombatTarget target) {
        Objects.requireNonNull(target);
        String id = Objects.requireNonNull(target.combatId());
        CombatTarget existing = targets.putIfAbsent(id, target);
        if (existing != null && existing != target) throw new IllegalArgumentException("Duplicate target: " + id);
    }

    public void remove(String id) { targets.remove(id); }

    /** Call once per accepted swing. Each registered target can be damaged at most once. */
    public List<CombatTarget> resolve(String attackerId, AttackValidator.Swing swing) {
        CombatTarget attacker = targets.get(attackerId);
        if (swing == null || attacker == null || !attacker.isAlive()) return List.of();
        List<CombatTarget> hit = new ArrayList<>();
        for (CombatTarget target : List.copyOf(targets.values())) {
            if (target.combatId().equals(attackerId) || !target.isAlive()) continue;
            if (target instanceof CombatPlayer player && !player.isPositioned()) continue;
            if (swing.hitbox().intersects(target.x(), target.y(), target.width(), target.height())) {
                target.takeDamage(swing.stats().damage());
                hit.add(target);
            }
        }
        return List.copyOf(hit);
    }
}
