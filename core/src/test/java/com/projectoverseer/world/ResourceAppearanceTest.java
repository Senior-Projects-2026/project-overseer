package com.projectoverseer.world;

import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ResourceAppearanceTest {
    @Test
    public void treeIsAnchoredToGlobalTileCenterIncludingNegativeCoordinates() {
        var placement = ResourceAppearance.placement(new WorldResource(ResourceType.TREE, -1, -2));
        assertEquals(-16f, placement.x() + placement.width() / 2, 0f);
        assertEquals(-48f, placement.groundY(), 0f);
        assertEquals(placement.groundY() - 4, placement.y(), 0f);
    }

    @Test
    public void canopyRemainsVisibleWhenItsAnchorIsOutsideTheCamera() {
        var placement = ResourceAppearance.placement(new WorldResource(ResourceType.TREE, 0, 0));
        assertTrue(placement.intersects(new VisibleTileBounds(0, 70, 32, 90)));
        assertTrue(placement.intersects(new VisibleTileBounds(40, 20, 60, 40)));
        assertFalse(placement.intersects(new VisibleTileBounds(48, 20, 60, 40)));
        assertFalse(placement.intersects(new VisibleTileBounds(0, 92, 32, 100)));
    }

    @Test
    public void allRockVariantsAreUsedAndStableRegardlessOfCallOrder() {
        Set<Integer> variants = new HashSet<>();
        for (int y = -16; y < 16; y++) {
            for (int x = -16; x < 16; x++) {
                int before = ResourceAppearance.rockVariant(x, y);
                ResourceAppearance.rockVariant(y, x);
                assertEquals(before, ResourceAppearance.rockVariant(x, y));
                assertTrue(before >= 0 && before < 3);
                variants.add(before);
            }
        }
        assertEquals(Set.of(0, 1, 2), variants);
        int extreme = ResourceAppearance.rockVariant(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertTrue(extreme >= 0 && extreme < 3);
    }
}
