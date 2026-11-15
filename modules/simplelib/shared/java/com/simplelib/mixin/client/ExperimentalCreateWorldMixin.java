package com.simplelib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import com.simplelib.api.ExperimentalWarning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * EXP52 = A ({@link ExperimentalWarning}): creating a world whose only experimental registry entries are the Simple mods' own
 * dimensions asks no "experimental settings" question. Vanilla's rule stays, with those entries counted as stable: skipped
 * unless re-created, and a re-created world still asks for experimental feature flags. Only the arguments of the question
 * change - the world data (and its saved lifecycle) was built before this call and stays experimental.
 */
@Mixin(CreateWorldScreen.class)
public abstract class ExperimentalCreateWorldMixin {
    @Shadow
    private boolean recreated;

    @WrapOperation(method = "onCreate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"))
    private void simplelib$skipOwnDimensionsWarning(Minecraft minecraft, CreateWorldScreen screen, Lifecycle lifecycle, Runnable task,
            boolean skipWarning, Operation<Void> original, @Local LayeredRegistryAccess<RegistryLayer> finalLayers,
            @Local FeatureFlagSet enabledFeatures) {
        if (!skipWarning && lifecycle == Lifecycle.experimental()
                && ExperimentalWarning.ownOnly(finalLayers.compositeAccess(), "create")) {
            skipWarning = !this.recreated;
            lifecycle = FeatureFlags.isExperimental(enabledFeatures) ? Lifecycle.experimental() : Lifecycle.stable();
        }
        original.call(minecraft, screen, lifecycle, task, skipWarning);
    }
}
