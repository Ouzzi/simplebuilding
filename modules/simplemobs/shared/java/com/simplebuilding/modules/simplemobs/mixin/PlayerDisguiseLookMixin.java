package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.DisguiseLooks;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A disguised player is drawn as a stand-in of the mob or player he imitates (cloak of deception). */
@Mixin(EntityRenderDispatcher.class)
public abstract class PlayerDisguiseLookMixin {
    @ModifyVariable(method = "extractEntity", at = @At("HEAD"), argsOnly = true)
    private Entity simplemobs$playerDisguise(Entity entity) {
        return DisguiseLooks.look(entity);
    }
}
