package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.DisguiseLooks;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A disguised player is drawn as a stand-in of the mob or player he imitates (cloak of deception). Only the world
 * entity pass is swapped: the local player's own state (first-person hands, overlays) must stay an avatar.
 */
@Mixin(LevelExtractor.class)
public abstract class PlayerDisguiseLookMixin {
    @ModifyArg(method = "extractVisibleEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"),
            index = 0)
    private Entity simplemobs$playerDisguise(Entity entity) {
        return DisguiseLooks.look(entity);
    }
}
