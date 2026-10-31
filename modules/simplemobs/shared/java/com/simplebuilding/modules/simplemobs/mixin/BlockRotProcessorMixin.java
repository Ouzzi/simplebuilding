package com.simplebuilding.modules.simplemobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** The decay of the Weathered Trials (block_rot with the tag simplemobs:weathered_rottable) follows the config weatheredDecayPercent; every other block_rot stays Vanilla. */
@Mixin(BlockRotProcessor.class)
public abstract class BlockRotProcessorMixin {
    @Shadow @Final private Optional<HolderSet<Block>> rottableBlocks;

    @ModifyExpressionValue(method = "processBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/BlockRotProcessor;integrity:F"))
    private float simplemobs$integrity(float original) {
        return rottableBlocks.isPresent() && rottableBlocks.get() instanceof HolderSet.Named<Block> named && TrialsExtras.isWeatheredRottable(named.key()) ? TrialsExtras.integrity() : original;
    }
}
