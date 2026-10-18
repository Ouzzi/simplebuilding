package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.AntiXray;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anti-X-Ray: remembers who the chunk packet built in {@code sendChunk} is for (the only server-side chunk packet). */
@Mixin(PlayerChunkSender.class)
public abstract class ProtectChunkSenderMixin {
    @Inject(method = "sendChunk", at = @At("HEAD"))
    private static void simpletweaks$receiver(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        AntiXray.RECEIVER.set(connection.player);
    }
    @Inject(method = "sendChunk", at = @At("RETURN"))
    private static void simpletweaks$done(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        AntiXray.RECEIVER.remove();
    }
}
