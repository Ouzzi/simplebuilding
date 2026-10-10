package com.simplebuilding.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** 26.2: no tiered vehicles (McVersion.TIERED_VEHICLES is 26.3 only), so no renderers. */
public final class VehicleRenderers {

    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    private VehicleRenderers() {
    }

    public static void register(Registrar registrar) {
    }
}
