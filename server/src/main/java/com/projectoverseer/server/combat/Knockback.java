package com.projectoverseer.server.combat;

/** Sweeps in steps of at most one world unit so displacement cannot skip a tile obstacle. */
public final class Knockback {
    @FunctionalInterface
    public interface Terrain { boolean isWalkable(int tileX, int tileY); }
    private Knockback() {}

    public static void apply(CombatTarget target, float dx, float dy, float distance, Terrain terrain) {
        if (distance == 0) return;
        if (!Float.isFinite(distance) || distance < 0 || !Float.isFinite(dx) || !Float.isFinite(dy))
            throw new IllegalArgumentException("Invalid knockback");
        double length = Math.hypot(dx, dy);
        if (length == 0) return;
        int steps = (int) Math.ceil(distance);
        float stepX = (float) (dx / length * distance / steps);
        float stepY = (float) (dy / length * distance / steps);
        float x = target.x(), y = target.y();
        for (int i = 0; i < steps; i++) {
            if (!fits(target, x + stepX, y + stepY, terrain)) break;
            x += stepX;
            y += stepY;
        }
        target.moveTo(x, y);
    }

    private static boolean fits(CombatTarget target, float x, float y, Terrain terrain) {
        // Match Player.canMoveTo's foot collision box; other entities use their supplied bounds.
        float width = target.width(), height = target.height();
        if (target instanceof CombatPlayer) { x += 10; width = 12; height = 6; }
        int minX = (int) Math.floor(x / 32f), minY = (int) Math.floor(y / 32f);
        int maxX = (int) Math.floor((x + width) / 32f), maxY = (int) Math.floor((y + height) / 32f);
        for (int ty = minY; ty <= maxY; ty++)
            for (int tx = minX; tx <= maxX; tx++)
                if (!terrain.isWalkable(tx, ty)) return false;
        return true;
    }
}
