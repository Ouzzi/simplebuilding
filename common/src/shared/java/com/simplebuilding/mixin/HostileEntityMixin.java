package com.simplebuilding.mixin;

import com.simplebuilding.util.ConstructionLightSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Das Baulicht verhindert keine Monster-Spawns: {@link ConstructionLightSpawning} rechnet die
 * Dunkelheitspruefung mit dem Blocklicht der uebrigen Lichtquellen. Frueher erlaubte dieser Mixin
 * Spawns nur direkt auf dem Baulicht, dort aber bedingungslos - auch im Tageslicht - und einen
 * Block daneben verhinderte sein Licht die Spawns wie eine Fackel.
 */
@Mixin(Monster.class)
public class HostileEntityMixin {
    @Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
    private static void ignoreConstructionLight(ServerLevelAccessor world, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        Boolean dark = ConstructionLightSpawning.isDarkEnoughIgnoringConstructionLight(world, pos, random);
        if (dark != null) {
            cir.setReturnValue(dark);
        }
    }
}
