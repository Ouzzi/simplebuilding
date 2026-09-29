package com.simplebuilding.mixin;

import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Die Vanilla-Shulkerkisten (alle 17 Farben) sind die erste Stufe der Shulkerkisten-Aufwertung:
 * Vorschlaghammer plus zwei Rissige Diamanten in der Nebenhand ({@code TieredShulkerBoxes}). Wie bei
 * den Kupfertruhen ({@link ChestBlockMixin}) reicht die Kiste den Rechtsklick im Schmiedestand an den
 * Hammer weiter, statt ihr Menue zu oeffnen, wenn die Aufwertung beginnen kann. Die gestuften Kisten
 * erben die Methode nicht (sie ueberschreiben sie und fragen selbst).
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$passToTheHammer(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
