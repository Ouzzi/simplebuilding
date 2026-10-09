package com.simplebuilding.mixin;

import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Der Vanilla-Verzauberungstisch wird mit Netherit-/Enderit-Vorschlaghammer und Enderit-Nugget in der Nebenhand zum
 * Astral-Verzauberungstisch (Queue N27, {@link SledgehammerUpgrades}). Vanilla oeffnet beim Rechtsklick sonst das
 * Verzauberungsmenue und der Hammer kaeme nie zum Zug - wie bei den Kupfertruhen ({@code ChestBlockMixin}) reicht der
 * Tisch den Klick im Schmiedestand an den Hammer weiter, wenn die Aufwertung beginnen kann. Auf beiden Seiten.
 */
@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$passToTheHammer(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
