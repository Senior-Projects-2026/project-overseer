package com.projectoverseer.shared.world;

/** Collectible world objects, separate from the terrain underneath them. */
public enum ResourceType {
    TREE,
    ROCK;

    /** Both current resource types occupy and block their tile. */
    public boolean blocksMovement() {
        return true;
    }
}
