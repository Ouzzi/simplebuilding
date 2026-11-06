package com.simplebuildingtools.version;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/** The one 26.3 client call the blueprint preview needs (SimpleBuilding's McClientVersion#quadShaded). */
public final class McClientVersion {
    private McClientVersion() {
    }

    public static boolean quadShaded(BakedQuad quad) {
        return quad.materialInfo().shadeDirectionOverride() != Direction.UP;
    }
}
