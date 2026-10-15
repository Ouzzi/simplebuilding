package com.simplebuilding.mixin;

import com.simplebuilding.util.JukeboxPlaylist;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla only ticks a jukebox that holds a disc. With a chest above, an empty jukebox must tick too to pick up the
 * first disc ({@link JukeboxPlaylist}, queue N62).
 */
@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockPlaylistTickerMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "getTicker", at = @At("RETURN"), cancellable = true)
    private <T extends BlockEntity> void simplebuilding$tickEmptyJukebox(Level level, BlockState state, BlockEntityType<T> type,
                                                                          CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if (cir.getReturnValue() == null && type == BlockEntityTypes.JUKEBOX && !level.isClientSide()) {
            cir.setReturnValue((BlockEntityTicker<T>) (BlockEntityTicker<JukeboxBlockEntity>) JukeboxBlockEntity::tick);
        }
    }
}
