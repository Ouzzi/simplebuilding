package com.simplebuilding.mixin;

import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Die Vanilla-Kupfertruhen (alle acht Varianten) sind die erste Stufe der Truhen-Aufwertung:
 * Vorschlaghammer plus Rissiger Diamant in der Nebenhand. Vanilla oeffnet beim Rechtsklick
 * sonst das Truhenmenue und der Hammer kaeme nie zum Zug - wie die Mod-Maschinen in ihrem
 * {@code useItemOn} reicht die Truhe den Klick deshalb im Schmiedestand an den Hammer weiter,
 * wenn die Aufwertung beginnen kann (oder der Hammer nach einer fertigen noch abkuehlt).
 * Auf beiden Seiten, damit Client und Server dieselbe Benutzung starten.
 */
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$passToTheHammer(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
