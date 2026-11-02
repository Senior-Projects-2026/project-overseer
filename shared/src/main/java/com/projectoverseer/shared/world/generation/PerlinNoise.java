package com.projectoverseer.shared.world.generation;

/** Seeded 2D gradient noise with quintic interpolation, independent of chunk borders. */
final class PerlinNoise {
    private static final double DIAGONAL = 0.7071067811865476;
    private final long seed;

    PerlinNoise(long seed) {
        this.seed = seed;
    }

    double sample(double x, double y) {
        long ix = (long) Math.floor(x);
        long iy = (long) Math.floor(y);
        double dx = x - ix;
        double dy = y - iy;
        double u = fade(dx);
        double v = fade(dy);
        double bottom = lerp(gradient(ix, iy, dx, dy), gradient(ix + 1, iy, dx - 1, dy), u);
        double top = lerp(gradient(ix, iy + 1, dx, dy - 1),
            gradient(ix + 1, iy + 1, dx - 1, dy - 1), u);
        return lerp(bottom, top, v);
    }

    private double gradient(long x, long y, double dx, double dy) {
        return switch ((int) (SeedHash.at(seed, x, y) & 7)) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dy;
            case 3 -> -dy;
            case 4 -> (dx + dy) * DIAGONAL;
            case 5 -> (dx - dy) * DIAGONAL;
            case 6 -> (-dx + dy) * DIAGONAL;
            default -> (-dx - dy) * DIAGONAL;
        };
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
}
