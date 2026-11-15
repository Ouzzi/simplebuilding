package com.simplelib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import com.simplelib.api.ExperimentalWarning;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * EXP52 = A ({@link ExperimentalWarning}): opening a world whose only experimental registry entries are the Simple mods' own
 * dimensions skips the backup question. Changes only the value this check compares (pattern of SimpleBuilding's hub-only
 * {@code HubWorldOpenFlowsMixin}), never the world's saved lifecycle or the other checks (old customized worlds still ask).
 */
@Mixin(WorldOpenFlows.class)
public abstract class ExperimentalOpenWorldMixin {
    @ModifyExpressionValue(method = "openWorldCheckWorldStemCompatibility", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle simplelib$skipOwnDimensionsWarning(Lifecycle lifecycle, @Local(argsOnly = true) WorldStem worldStem) {
        if (lifecycle == Lifecycle.experimental() && ExperimentalWarning.ownOnly(worldStem.registries().compositeAccess(), "open")) {
            return Lifecycle.stable();
        }
        return lifecycle;
    }
}
