package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.blocks.custom.ModPistonHeadBlock;
import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.ReinforcedPistonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    // Verhindert, dass Pistons sich gegenseitig kaputt machen oder falsch verschieben.
    // Gilt fuer alle Kolben der Mod: verstaerkt (normal und klebrig), Netherit und Enderit
    // (EnderitePistonBlock erbt von NetheriteBreakerPistonBlock).
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void isCustomPistonMovable(BlockState state, net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction, boolean canBreak, net.minecraft.core.Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof ReinforcedPistonBlock || state.getBlock() instanceof NetheriteBreakerPistonBlock) {
            // Wenn der Piston ausgefahren ist, darf er nicht bewegt werden
            if (state.getValue(PistonBaseBlock.EXTENDED)) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Der eigene Kopf jeder Kolbenstufe. {@code moveBlocks} nennt {@code Blocks.PISTON_HEAD} an drei
     * Stellen: beim Einfahren, um den Kopf vor dem Ziehen wegzuraeumen, beim Ausfahren als bewegten
     * Block der Kopfzelle (daraus wird der Kopf, sobald die Bewegung endet) und als Quelle der
     * Nachbar-Updates. Fuer die Kolben der Mod steht an allen drei Stellen ihr eigener Kopf
     * ({@link ModPistonHeadBlock#headFor}); Vanillas Kolben bekommen weiter {@code minecraft:piston_head}.
     * Den Kopftyp setzt Vanilla danach selbst aus dem klebrig-Flag des Kolbens.
     */
    @ModifyExpressionValue(method = "moveBlocks", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
    private Block simplebuilding$ownHead(Block original) {
        Block own = ModPistonHeadBlock.headFor((Block) (Object) this);
        return own != null ? own : original;
    }

    /**
     * Ein Kolben der Mod, der vor diesem Update ausgefahren wurde, traegt noch Vanillas Kopf (bis
     * 2026-09 setzten alle Kolben der Mod {@code minecraft:piston_head}). Weil die Aufraeumstelle oben
     * jetzt nach dem eigenen Kopf fragt, raeumt diese Zeile den alten Kopf so weg, wie Vanilla es tut,
     * bevor ein klebriger verstaerkter Kolben zieht.
     */
    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private void simplebuilding$clearLegacyHead(Level level, BlockPos pistonPos, Direction direction, boolean extending,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!extending && ModPistonHeadBlock.headFor((Block) (Object) this) != null) {
            BlockPos arm = pistonPos.relative(direction);
            if (level.getBlockState(arm).is(Blocks.PISTON_HEAD)) {
                level.setBlock(arm, Blocks.AIR.defaultBlockState(), 276);
            }
        }
    }

    /**
     * Der Verschleiss des Netherit-Brechers ({@link NetheriteBreakerPistonBlock#WEAR}) ueberlebt das
     * Einfahren: Vanilla legt dabei {@code this.defaultBlockState()} mit der Blickrichtung in den
     * bewegten Block an der Kolbenstelle, und daraus wird der eingefahrene Kolben - ohne diese Zeile
     * stuende danach immer Stufe 0 da. Server und Client spielen dieselbe Stelle, beide behalten die
     * Stufe. {@code Blocks.MOVING_PISTON.defaultBlockState()} hat einen anderen Eigentuemer
     * ({@code Block}) und wird hier nicht getroffen.
     */
    @ModifyExpressionValue(method = "triggerEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState simplebuilding$keepWear(BlockState original, @Local(argsOnly = true) BlockState state) {
        if (original.hasProperty(NetheriteBreakerPistonBlock.WEAR) && state.hasProperty(NetheriteBreakerPistonBlock.WEAR)) {
            return original.setValue(NetheriteBreakerPistonBlock.WEAR, state.getValue(NetheriteBreakerPistonBlock.WEAR));
        }
        return original;
    }
}
