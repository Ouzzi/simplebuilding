package com.simpleores.mixin;

import com.simpleores.block.HiddenOreBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * W24-4 (moved from SimpleBuilding's BrushItemMixin with the ores, N47-M2): every Vanilla brush stroke (the tick that plays the
 * brushing sound, every 10 ticks) on a hidden Astralit / Nihilit ore frees it one step further ({@link HiddenOreBlock#brush}).
 */
@Mixin(BrushItem.class)
public class HiddenOreBrushMixin {
    @Shadow
    private HitResult calculateHitResult(Player player) {
        throw new AssertionError();
    }

    @Inject(method = "onUseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;)V"))
    private void simpleores$brushHiddenOre(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (world instanceof ServerLevel level && user instanceof Player player && calculateHitResult(player) instanceof BlockHitResult hit) {
            HiddenOreBlock.brush(level, hit.getBlockPos(), user, stack);
        }
    }
}
