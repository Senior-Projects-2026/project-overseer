package com.projectoverseer.server.combat;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.shared.Network.AttackRequest;
import com.projectoverseer.shared.combat.*;
import java.util.HashMap;
import java.util.Map;

/** Called on the server network thread. Never accepts damage or origin from the client. */
public final class AttackValidator {
    private record Cooldown(long started, long duration) {}
    private final Map<Integer, Cooldown> cooldowns = new HashMap<>();
    private final CombatConfig config;

    public AttackValidator(CombatConfig config) { this.config = config; }

    public record Swing(WeaponStats stats, MeleeHitbox hitbox, String itemId,
                        float originX, float originY, float directionX, float directionY) {}

    public Swing accept(int playerId, Inventory inventory, float originX, float originY,
                        boolean canAttack, AttackRequest request, long now) {
        if (!canAttack || inventory == null || request == null
            || request.selectedSlot < 0 || request.selectedSlot >= Inventory.SLOT_COUNT
            || !Float.isFinite(originX) || !Float.isFinite(originY)
            || !Float.isFinite(request.aimX) || !Float.isFinite(request.aimY)) return null;
        double dx = (double) request.aimX - originX, dy = (double) request.aimY - originY;
        double length = Math.hypot(dx, dy);
        if (length == 0) return null;
        Cooldown previous = cooldowns.get(playerId);
        if (previous != null && now - previous.started() < previous.duration()) return null;
        ItemStack item = inventory.getSlot(request.selectedSlot);
        WeaponStats stats = config.forItem(item);
        MeleeHitbox hitbox = MeleeHitbox.toward(originX, originY, request.aimX, request.aimY, stats);
        inventory.selectSlot(request.selectedSlot);
        cooldowns.put(playerId, new Cooldown(now, (long) Math.ceil(stats.cooldown() * 1_000_000_000.0)));
        return new Swing(stats, hitbox, item == null ? "" : item.itemId(), originX, originY,
            (float) (dx / length), (float) (dy / length));
    }

    public void removePlayer(int playerId) { cooldowns.remove(playerId); }
}
