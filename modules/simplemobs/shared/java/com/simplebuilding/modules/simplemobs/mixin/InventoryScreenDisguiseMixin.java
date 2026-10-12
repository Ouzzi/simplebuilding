package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.DisguiseLooks;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Cloak of deception (owner 12.10.): the player model in the inventory shows the disguise too (renderer and state of the stand-in). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenDisguiseMixin {
    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"),
            index = 0, require = 0)
    private static Entity simplemobs$disguisedRenderer(Entity entity) {
        return DisguiseLooks.look(entity);
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"),
            index = 0, require = 0)
    private static Entity simplemobs$disguisedState(Entity entity) {
        return DisguiseLooks.look(entity);
    }
}
