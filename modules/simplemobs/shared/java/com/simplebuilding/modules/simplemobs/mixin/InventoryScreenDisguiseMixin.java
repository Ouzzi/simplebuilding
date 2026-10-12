package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.DisguiseLooks;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Cloak of deception (owner 12.10.): the player model in the inventory shows the disguise too. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenDisguiseMixin {
    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, require = 0)
    private static LivingEntity simplemobs$disguisedModel(LivingEntity entity) {
        return DisguiseLooks.look(entity) instanceof LivingEntity look ? look : entity;
    }
}
