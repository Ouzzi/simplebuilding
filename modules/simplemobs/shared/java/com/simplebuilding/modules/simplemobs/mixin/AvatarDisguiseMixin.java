package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player in a mob's shape takes that mob's hit box and eye height (owner 11.10.). */
@Mixin(Avatar.class)
public abstract class AvatarDisguiseMixin {
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void simplemobs$disguiseBox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof Player p) || pose == Pose.SLEEPING || pose == Pose.DYING) return;
        EntityType<?> look = Disguise.morphType(p);
        if (look != null) cir.setReturnValue(look.getDimensions());
    }
}
