package com.simplemusic.mixin;

import com.simplemusic.disc.JukeboxDiscs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs a custom disc after the Vanilla jukebox tick and stops it when the jukebox goes (JukeboxDiscs). B23-M9: a jukebox
 * that was an Orchestrion (id moved, {@code SimpleMusic#registerIdMoves}) keeps the disc or sheet the Orchestrion held
 * (saved as {@code Item} instead of {@code RecordItem}).
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {
    @Shadow private ItemStack item;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplemusic$orchestrionItem(ValueInput input, CallbackInfo ci) {
        if (item.isEmpty()) input.read("Item", ItemStack.CODEC).ifPresent(old -> item = old);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void simplemusic$tickCustomDisc(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo ci) {
        if (level instanceof ServerLevel server) JukeboxDiscs.tick(server, pos, jukebox);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void simplemusic$stopCustomDisc(CallbackInfo ci) {
        JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
        if (self.getLevel() instanceof ServerLevel server) JukeboxDiscs.removed(server, self.getBlockPos());
    }
}
