package com.simplebuilding.mixin;

import com.simplebuilding.util.JukeboxPlaylist;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Jukebox with a chest above it (queue N62, {@link JukeboxPlaylist}): runs the playlist step after vanilla's tick and
 * remembers the slot of the last disc taken from the chest in the block entity data.
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityPlaylistMixin implements JukeboxPlaylist.Cursor {
    @Unique
    private int simplebuilding$playlistSlot = -1;

    @Override
    public int simplebuilding$getPlaylistSlot() {
        return simplebuilding$playlistSlot;
    }

    @Override
    public void simplebuilding$setPlaylistSlot(int slot) {
        simplebuilding$playlistSlot = slot;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void simplebuilding$playlist(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            JukeboxPlaylist.tick(server, pos, jukebox);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void simplebuilding$savePlaylistSlot(ValueOutput output, CallbackInfo ci) {
        if (simplebuilding$playlistSlot >= 0) {
            output.putInt("simplebuilding_playlist_slot", simplebuilding$playlistSlot);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplebuilding$loadPlaylistSlot(ValueInput input, CallbackInfo ci) {
        simplebuilding$playlistSlot = input.getIntOr("simplebuilding_playlist_slot", -1);
    }
}
