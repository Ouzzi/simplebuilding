package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simplebuilding.modules.simpletweaks.protect.SeedHash;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Seed protection: login, respawn and dimension change packets carry the random hashed seed when it is on. */
@Mixin(ServerPlayer.class)
public abstract class ProtectSpawnInfoMixin {
    @ModifyExpressionValue(method = "createCommonSpawnInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeManager;obfuscateSeed(J)J"))
    private long simpletweaks$seed(long hashed) { return SeedHash.sent(hashed); }
}
