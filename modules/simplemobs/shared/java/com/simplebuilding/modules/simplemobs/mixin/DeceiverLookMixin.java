package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.DeceiverLooks;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A disguised Deceiver is drawn as a stand-in of the mob it turned into (owner N38); its hit box already matches. */
@Mixin(EntityRenderDispatcher.class)
public abstract class DeceiverLookMixin {
    @ModifyVariable(method = "extractEntity", at = @At("HEAD"), argsOnly = true)
    private Entity simplemobs$deceiverLook(Entity entity) {
        return DeceiverLooks.look(entity);
    }
}
