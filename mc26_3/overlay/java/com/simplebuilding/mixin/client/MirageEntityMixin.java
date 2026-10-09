package com.simplebuilding.mixin.client;

import com.simplebuilding.client.effect.PerceptionClient;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mirage / Reverse Mirage (queue N20): every entity the level hands to the renderer goes through
 * {@link PerceptionClient#look}, which swaps a mob for a stand-in of another type while the local player has the
 * effect. Only the picture changes; the real entity keeps its hit box, sounds and behaviour.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class MirageEntityMixin {
    @ModifyVariable(method = "extractEntity", at = @At("HEAD"), argsOnly = true)
    private Entity simplebuilding$mirage(Entity entity) {
        return PerceptionClient.look(entity);
    }
}
