package com.simplequalityoflife.mixin.client;

import com.simplequalityoflife.client.LinkedPanel;
import com.simplequalityoflife.client.LinkedScreen;
import com.simplequalityoflife.container.LinkedMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Linked GUIs: lay out, draw and scroll the marked container's panel; clicks on it are not "outside". */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenLinkedMixin extends Screen implements LinkedScreen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow protected abstract boolean hasClickedOutside(double x, double y, int left, int top);

    protected ContainerScreenLinkedMixin(Component title) {
        super(title);
    }

    @Unique
    private @Nullable LinkedPanel qol$panel() {
        return ((LinkedMenu) this.menu).qol$panel() instanceof LinkedPanel panel ? panel : null;
    }

    @Override
    public void qol$layout() {
        LinkedPanel panel = this.qol$panel();
        if (panel != null) panel.layout(this.menu, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, this.width, this.height);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void qol$init(CallbackInfo ci) {
        this.qol$layout();
    }

    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void qol$drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null) panel.render(graphics, this.font);
    }

    @Redirect(method = {"mouseClicked", "mouseReleased"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"))
    private boolean qol$outside(AbstractContainerScreen<?> self, double x, double y, int left, int top) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null && panel.contains(x, y, this.leftPos, this.topPos)) return false;
        return this.hasClickedOutside(x, y, left, top);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void qol$scroll(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null && panel.contains(x, y, this.leftPos, this.topPos) && panel.scroll(this.menu, scrollY)) cir.setReturnValue(true);
    }
}
