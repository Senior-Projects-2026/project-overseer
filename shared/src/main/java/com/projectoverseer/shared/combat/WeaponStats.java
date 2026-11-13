package com.projectoverseer.shared.combat;

/** Distances are world units, cooldown is seconds, knockback is displacement distance. */
public record WeaponStats(float damage, float reach, float width, float cooldown, float knockback) {
    public WeaponStats {
        if (!Float.isFinite(damage) || damage <= 0
            || !Float.isFinite(reach) || reach <= 0
            || !Float.isFinite(width) || width <= 0
            || !Float.isFinite(cooldown) || cooldown <= 0
            || !Float.isFinite(knockback) || knockback < 0) {
            throw new IllegalArgumentException("Invalid weapon stats");
        }
    }
}
