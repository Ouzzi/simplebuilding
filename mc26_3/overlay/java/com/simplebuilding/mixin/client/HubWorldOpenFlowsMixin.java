package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Lifecycle;
import com.simplebuilding.platform.ModEnvironment;
import com.simplebuilding.version.McVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Acknowledge experimental registries only in an explicitly opted-in hub development client. */
@Mixin(WorldOpenFlows.class)
public abstract class HubWorldOpenFlowsMixin {
    private static boolean simplebuilding$hubClient() {
        return McVersion.HUB_TEST_WORLD && Boolean.getBoolean("simplebuilding.hub")
                && ModEnvironment.isDevelopmentEnvironment();
    }

    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$confirmExperimentalCreation(Minecraft minecraft, CreateWorldScreen screen,
            Lifecycle lifecycle, Runnable create, boolean skipWarnings, CallbackInfo ci) {
        if (simplebuilding$hubClient() && lifecycle == Lifecycle.experimental()) {
            create.run();
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "openWorldCheckWorldStemCompatibility", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;"))
    private Lifecycle simplebuilding$acceptExperimentalRegistries(Lifecycle lifecycle) {
        // Change only this warning decision, never the world's persisted lifecycle or other checks.
        return simplebuilding$hubClient() && lifecycle == Lifecycle.experimental() ? Lifecycle.stable() : lifecycle;
    }
}
