package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.dragon.DragonEgg;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A dragon egg after every fight, not only the first (T28). */
@Mixin(EnderDragonFight.class)
public abstract class PlayDragonFightMixin {
    @Shadow private ServerLevel level;
    @Shadow private BlockPos origin;
    @Shadow private boolean hasPreviouslyKilledDragon;
    @Shadow private UUID dragonUUID;
    @Unique private boolean simpletweaks$repeat;

    @Inject(method = "setDragonKilled", at = @At("HEAD"))
    private void simpletweaks$playEggBefore(EnderDragon dragon, CallbackInfo ci) {
        simpletweaks$repeat = hasPreviouslyKilledDragon && dragon.getUUID().equals(dragonUUID);
    }
    @Inject(method = "setDragonKilled", at = @At("RETURN"))
    private void simpletweaks$playEgg(EnderDragon dragon, CallbackInfo ci) {
        if (simpletweaks$repeat && DragonEgg.active()) DragonEgg.give(level, EnderDragonFight.getPodiumLocation(origin));
        simpletweaks$repeat = false;
    }
}
