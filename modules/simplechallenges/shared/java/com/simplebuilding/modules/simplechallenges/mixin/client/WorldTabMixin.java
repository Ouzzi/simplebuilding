package com.simplebuilding.modules.simplechallenges.mixin.client;

import com.simplebuilding.modules.simplechallenges.client.ChallengeRow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World creation (S3): adds the challenge row (recent challenges, Select Challenge) to the World tab. It shows only for the world type "Simple Challenges". */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplechallenges$addRow(CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof CreateWorldScreen screen && ((GridLayoutTab) (Object) this).getLayout() instanceof GridLayout grid) {
            ChallengeRow.attach(screen, grid);
        }
    }
}
