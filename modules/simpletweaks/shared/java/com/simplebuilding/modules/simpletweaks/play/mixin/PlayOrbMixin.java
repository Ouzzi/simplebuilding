package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.events.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Double XP event (T21): every awarded experience is multiplied. */
@Mixin(ExperienceOrb.class)
public abstract class PlayOrbMixin {
    @ModifyVariable(method = "awardWithDirection", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int simpletweaks$playDoubleXp(int amount) { return ServerEvents.xp(amount); }
}
