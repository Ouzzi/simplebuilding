package com.simplebuilding.modules.simplecontainers.mixin;

import com.simplebuilding.modules.simplecontainers.client.ContainersClient;
import com.simplebuilding.modules.simplecontainers.client.StyleToggleButton;
import com.simplebuilding.modules.simplecontainers.client.StyleToggleClient;
import com.simplelib.api.client.ui.UiStyleToggle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the optional comparison control to every container screen, including SimpleBuilding mod screens. */
@Mixin(AbstractContainerScreen.class)
public abstract class StyleToggleMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    private StyleToggleMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void simplecontainers$toggle(CallbackInfo ci) {
        if ((StyleToggleClient.development || ContainersClient.config().showStyleToggle)
                && StyleToggleClient.KEY != null) {
            this.addRenderableWidget(new StyleToggleButton(this.leftPos + this.imageWidth + 4, this.topPos - 18,
                    button -> UiStyleToggle.toggle()));
        }
    }
}
