package com.projectoverseer.shared.combat;

/** An oriented rectangle extending from the attack origin toward the aim point.
 * Targets use axis-aligned world bounds. Touching edges count as a hit.
 * This is geometry only: cooldowns, target IDs and damage belong to the server.
 */
public final class MeleeHitbox {
    private final double x, y, dx, dy, halfLength, halfWidth;

    private MeleeHitbox(double x, double y, double dx, double dy, WeaponStats stats) {
        this.halfLength = stats.reach() / 2.0;
        this.halfWidth = stats.width() / 2.0;
        this.x = x + dx * halfLength;
        this.y = y + dy * halfLength;
        this.dx = dx;
        this.dy = dy;
    }

    public static MeleeHitbox toward(double originX, double originY,
                                     double aimX, double aimY, WeaponStats stats) {
        requireFinite(originX, originY, aimX, aimY);
        double vx = aimX - originX, vy = aimY - originY;
        double length = Math.hypot(vx, vy);
        if (!Double.isFinite(length) || length == 0) {
            throw new IllegalArgumentException("Aim must define a finite, nonzero direction");
        }
        return new MeleeHitbox(originX, originY, vx / length, vy / length,
            java.util.Objects.requireNonNull(stats));
    }

    public boolean intersects(double minX, double minY, double width, double height) {
        requireFinite(minX, minY, width, height);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid target size");
        double hx = width / 2, hy = height / 2;
        double tx = minX + hx - x, ty = minY + hy - y;
        // Separating axis theorem: world X/Y and the attack's forward/side axes.
        return Math.abs(tx) <= hx + Math.abs(dx) * halfLength + Math.abs(dy) * halfWidth
            && Math.abs(ty) <= hy + Math.abs(dy) * halfLength + Math.abs(dx) * halfWidth
            && Math.abs(tx * dx + ty * dy) <= halfLength + hx * Math.abs(dx) + hy * Math.abs(dy)
            && Math.abs(-tx * dy + ty * dx) <= halfWidth + hx * Math.abs(dy) + hy * Math.abs(dx);
    }

    private static void requireFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite coordinate");
        }
    }
}
