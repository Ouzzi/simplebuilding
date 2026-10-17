package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.AntiXray;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anti-X-Ray: the section bytes of a chunk packet for a non-exempt player are replaced by the obfuscated ones. */
@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ProtectChunkDataMixin {
    @Shadow @Final @Mutable private byte[] buffer;

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("RETURN"))
    private void simpletweaks$obfuscate(LevelChunk chunk, CallbackInfo ci) {
        var player = AntiXray.RECEIVER.get();
        if (player != null && AntiXray.applies(player, chunk.getLevel())) buffer = AntiXray.obfuscate(chunk);
    }
}
