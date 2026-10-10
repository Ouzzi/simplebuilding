package com.simplebuilding.modules.simplemodels.mixin;
import com.simplebuilding.modules.simplemodels.client.ModelsTab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Adds the Models bookmark to the inventory and the anvil. Hooks both init(II) and rebuildWidgets(): a window
 * resize only rebuilds, and would otherwise drop the tab.
 */
@Mixin(Screen.class)
public abstract class ModelScreenMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener
            & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget);
    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void simplemodels$tab(int width, int height, CallbackInfo ci) { simplemodels$addTab(); }
    @Inject(method = "rebuildWidgets()V", at = @At("TAIL"))
    private void simplemodels$tabAfterRebuild(CallbackInfo ci) { simplemodels$addTab(); }
    private void simplemodels$addTab() {
        Screen self = (Screen)(Object)this;
        if (!(self instanceof AnvilScreen || self instanceof InventoryScreen)
                || self.children().stream().anyMatch(ModelsTab.class::isInstance)) return;
        var container = (ModelContainerAccessor)self;
        addRenderableWidget(new ModelsTab(self, container::simplemodels$left, container::simplemodels$top));
    }
}
