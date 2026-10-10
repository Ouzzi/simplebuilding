package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Styled screens draw their title in the box's label colour and leave out the inventory label ({@link StyledScreens#drawLabels}). */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerLabelsMixin extends Screen {
    @Shadow protected int titleLabelX;
    @Shadow protected int titleLabelY;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    private ContainerLabelsMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$labels(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (StyledScreens.drawLabels((AbstractContainerScreen<?>) (Object) this, graphics, this.font, this.title,
                this.titleLabelX, this.titleLabelY, this.imageWidth, this.imageHeight)) {
            ci.cancel();
        }
    }
}
