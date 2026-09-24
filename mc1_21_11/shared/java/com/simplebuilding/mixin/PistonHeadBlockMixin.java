package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.ReinforcedPistonBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHeadBlock.class)
public class PistonHeadBlockMixin {

    // Vanillas isFittingBase kennt nur Blocks.PISTON (normaler Kopf) und Blocks.STICKY_PISTON
    // (klebriger Kopf). Wir erkennen zusaetzlich die Kolben der Mod als Basis an, mit denselben
    // Bedingungen wie Vanilla: passender Kopftyp, Basis ausgefahren, gleiche Blickrichtung.
    // Das deckt alle drei Aufrufer auf einmal ab: canSurvive (der Kopf bleibt stehen),
    // affectNeighborsAfterRemoval (Kopf abgebaut -> Basis bricht mit Drop) und playerWillDestroy
    // (im Kreativmodus ohne Drop). Frueher hing hier nur canSurvive, ohne EXTENDED- und
    // Richtungspruefung; ein abgebauter Kopf liess dann eine ausgefahrene Basis ohne Kopf zurueck,
    // deren Einfahren den Block in der Kopfzelle loeschte.
    @Inject(method = "isFittingBase", at = @At("HEAD"), cancellable = true)
    private void allowCustomPistons(BlockState armState, BlockState potentialBase, CallbackInfoReturnable<Boolean> cir) {
        Block base = potentialBase.getBlock();
        boolean sticky;
        if (base instanceof ReinforcedPistonBlock reinforced) {
            sticky = reinforced.isStickyPiston();
        } else if (base instanceof NetheriteBreakerPistonBlock) {
            sticky = false;
        } else {
            return;
        }
        PistonType wanted = sticky ? PistonType.STICKY : PistonType.DEFAULT;
        cir.setReturnValue(armState.getValue(PistonHeadBlock.TYPE) == wanted
                && potentialBase.getValue(PistonBaseBlock.EXTENDED)
                && potentialBase.getValue(DirectionalBlock.FACING) == armState.getValue(DirectionalBlock.FACING));
    }
}
