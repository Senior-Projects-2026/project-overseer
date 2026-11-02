package com.projectoverseer.shared.world.generation;

/** Stateless coordinate hashing: generation never consumes a shared random sequence. */
final class SeedHash {
    private SeedHash() {}

    static long at(long seed, long x, long y) {
        return mix(seed ^ mix(x) ^ Long.rotateLeft(mix(y), 32));
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    static double unit(long seed, int x, int y) {
        return (at(seed, x, y) >>> 11) * 0x1.0p-53;
    }
}
