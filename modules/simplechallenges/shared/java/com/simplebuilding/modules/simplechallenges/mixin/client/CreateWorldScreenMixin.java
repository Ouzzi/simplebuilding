package com.simplebuilding.modules.simplechallenges.mixin.client;

import com.simplebuilding.modules.simplechallenges.client.ChallengeScreenAccess;
import com.simplebuilding.modules.simplechallenges.core.PendingChallenge;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World creation (S3): leaving the screen drops the chosen challenge; the Select Challenge screen can return to the World tab. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin implements ChallengeScreenAccess {
    @Shadow
    private @Nullable MenuTabBar tabNavigationBar;

    @Inject(method = "popScreen", at = @At("HEAD"))
    private void simplechallenges$forgetChoice(CallbackInfo ci) {
        PendingChallenge.clear();
    }

    @Override
    public void simplechallenges$openWorldTab() {
        if (tabNavigationBar != null) tabNavigationBar.selectTab(1, false);
    }
}
