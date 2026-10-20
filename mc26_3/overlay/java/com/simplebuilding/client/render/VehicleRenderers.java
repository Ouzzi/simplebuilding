package com.simplebuilding.client.render;

import com.simplebuilding.entity.ModEntities;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Renderers of the tiered vehicles (Queue N19/N23): Vanilla's cart model; hopper carts (and, where SimpleBuilding still has
 * them, furnace carts) show their tier block as Vanilla shows its hopper and furnace. The chest carts and chest boats are
 * Simple Storage's since split stage 8b-2 (its own renderers).
 */
public final class VehicleRenderers {

    /** The loader's renderer registration (Fabric registry, NeoForge/Forge event). */
    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    private VehicleRenderers() {
    }

    public static void register(Registrar registrar) {
        if (!com.simplebuilding.version.McVersion.TIERED_VEHICLES) {
            return;
        }
        if (com.simplebuilding.version.McVersion.OVENS_IN_SB) { // split stage 4: Simple Ovens renders its furnace carts
            registrar.register(ModEntities.REINFORCED_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
            registrar.register(ModEntities.NETHERITE_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
            registrar.register(ModEntities.ENDERITE_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
        }
        registrar.register(ModEntities.REINFORCED_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
        registrar.register(ModEntities.NETHERITE_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
        registrar.register(ModEntities.ENDERITE_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
    }
}
