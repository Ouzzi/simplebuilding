package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleinterfaces.client.LinkedPanel;
import com.simplebuilding.modules.simpleinterfaces.client.LinkedScreen;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Linked GUIs (N65): lay out, draw, scroll and switch the marked containers' panel ({@link LinkedPanel}, {@link LinkedLayout});
 * clicks on it are not "outside". The layout runs after the screen's whole {@code init} ({@code ScreenLinkedMixin}) and when
 * the payload arrives; C/E move {@code leftPos/topPos} together with every widget (undone before the next layout). When
 * something else moves the screen (the recipe book button) the widgets are rebuilt and laid out again.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenLinkedMixin extends Screen implements LinkedScreen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int inventoryLabelY;
    @Shadow @Final protected Component playerInventoryTitle;

    @Unique private int qol$dx, qol$dy;
    @Unique private int qol$expectedLeft = Integer.MIN_VALUE, qol$expectedTop = Integer.MIN_VALUE;
    /** The inventory label's y while A hides it (MIN_VALUE: not hidden, the screen's own value stands). */
    @Unique private int qol$inventoryLabelY = Integer.MIN_VALUE;

    protected ContainerScreenLinkedMixin(Component title) {
        super(title);
    }

    @Unique
    private @Nullable LinkedPanel qol$panel() {
        return ((LinkedMenu) this.menu).qol$panel() instanceof LinkedPanel panel ? panel : null;
    }

    @Unique
    private void qol$shift(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        this.leftPos += dx;
        this.topPos += dy;
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget) widget.setPosition(widget.getX() + dx, widget.getY() + dy);
        }
    }

    @Override
    public void qol$layout() {
        // undo the last shift unless something else has moved the screen since (then init starts from scratch anyway)
        if (this.leftPos == this.qol$expectedLeft && this.topPos == this.qol$expectedTop) this.qol$shift(-this.qol$dx, -this.qol$dy);
        this.qol$dx = 0;
        this.qol$dy = 0;
        LinkedPanel panel = this.qol$panel();
        if (panel == null) {
            this.qol$hideInventoryLabel(false);
            this.qol$expectedLeft = this.qol$expectedTop = Integer.MIN_VALUE;
            return;
        }
        LinkedLayout.Layout layout = panel.layout(this.menu, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, this.width, this.height,
                LinkedPanel.clean((AbstractContainerScreen<?>) (Object) this), (Object) this instanceof InventoryScreen);
        // A: the inventory label would stick out above the marked box
        this.qol$hideInventoryLabel(layout.hotbar() != null);
        this.qol$dx = layout.dx();
        this.qol$dy = layout.dy();
        this.qol$shift(this.qol$dx, this.qol$dy);
        this.qol$expectedLeft = this.leftPos;
        this.qol$expectedTop = this.topPos;
    }

    @Unique
    private void qol$hideInventoryLabel(boolean hide) {
        if (hide && this.qol$inventoryLabelY == Integer.MIN_VALUE) {
            this.qol$inventoryLabelY = this.inventoryLabelY;
            this.inventoryLabelY = -1000;
        } else if (!hide && this.qol$inventoryLabelY != Integer.MIN_VALUE) {
            this.inventoryLabelY = this.qol$inventoryLabelY;
            this.qol$inventoryLabelY = Integer.MIN_VALUE;
        }
    }

    @Override
    public int @Nullable [] qol$background() {
        LinkedPanel panel = this.qol$panel();
        LinkedLayout.Layout layout = panel == null ? null : panel.current();
        if (layout == null || layout.bgTop() == Integer.MIN_VALUE && layout.bgBottom() == Integer.MAX_VALUE) return null;
        int top = layout.bgTop() == Integer.MIN_VALUE ? 0 : Math.clamp(this.topPos + layout.bgTop(), 0, this.height);
        int bottom = layout.bgBottom() == Integer.MAX_VALUE ? this.height : Math.clamp(this.topPos + layout.bgBottom(), top, this.height);
        return new int[] {0, top, this.width, bottom};
    }

    /** {@code init} computes {@code leftPos/topPos} afresh: the old shift is gone with it. */
    @Inject(method = "init()V", at = @At("HEAD"))
    private void qol$init(CallbackInfo ci) {
        this.qol$dx = 0;
        this.qol$dy = 0;
        this.qol$expectedLeft = this.qol$expectedTop = Integer.MIN_VALUE;
    }

    /** The recipe book button (or another mod) moved the screen: rebuild, so the layout starts from the new place. */
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void qol$tick(CallbackInfo ci) {
        if (this.qol$panel() != null && this.qol$expectedLeft != Integer.MIN_VALUE
                && (this.leftPos != this.qol$expectedLeft || this.topPos != this.qol$expectedTop)) {
            this.rebuildWidgets();
        }
    }

    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void qol$drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null) panel.render(graphics, this.font, this.playerInventoryTitle);
    }

    @WrapOperation(method = {"mouseClicked", "mouseReleased"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"))
    private boolean qol$outside(AbstractContainerScreen<?> self, double x, double y, int left, int top, Operation<Boolean> original) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null && panel.contains(x, y, this.leftPos, this.topPos)) return false;
        return original.call(self, x, y, left, top);
    }

    /** A click on a tab switches the panel and goes no further (also with an item on the cursor: carry it to another tab). */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void qol$tab(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null && panel.clickTab(this.menu, event.x(), event.y(), this.leftPos, this.topPos)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void qol$scroll(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        LinkedPanel panel = this.qol$panel();
        if (panel != null && panel.contains(x, y, this.leftPos, this.topPos) && panel.scroll(this.menu, scrollY)) cir.setReturnValue(true);
    }
}
