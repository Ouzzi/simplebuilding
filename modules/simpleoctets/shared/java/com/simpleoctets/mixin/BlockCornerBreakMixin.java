package com.simpleoctets.mixin;

import com.simpleoctets.block.OctetCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Besitzer SP22-1: schleichend die Ecke eines Vollblocks mit Achteln abbauen ({@link OctetCellBlock#startCornerBreak}).
 * Die drei Schritte jedes Loaders: {@code playerWillDestroy} merkt sich das Achtel, {@code destroy} legt die 7 uebrigen
 * zurueck (Vanilla, Fabric und Forge rufen es am Vollblock, NeoForge an der Zelle), {@code playerDestroy} droppt ueber
 * die Zelle nur das eine Achtel. Achtelzellen selbst haben ihre eigene Logik und fallen hier durch.
 */
@Mixin(Block.class)
public abstract class BlockCornerBreakMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void simpleoctets$startCornerBreak(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        OctetCellBlock.startCornerBreak(level, pos, state, player);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void simpleoctets$restoreCorner(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!((Object) this instanceof OctetCellBlock)) {
            OctetCellBlock.restoreRest(level, pos);
        }
    }

    @ModifyVariable(method = "playerDestroy", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private BlockState simpleoctets$cornerDrops(BlockState state, ServerLevel level, ServerPlayer player, BlockPos pos) {
        return OctetCellBlock.cornerDropState(level, pos, state);
    }
}
