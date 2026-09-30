package com.simplebuilding.modules.simplemodels.mixin;
import com.simplebuilding.modules.simplemodels.client.ModelBrowser;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class ModelScreenMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener
            & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget);
    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void simplemodels$button(int width, int height, CallbackInfo ci) {
        var client = net.minecraft.client.Minecraft.getInstance();
        Screen self = (Screen)(Object)this;
        if (self instanceof AnvilScreen || self instanceof InventoryScreen) {
            var container = (ModelContainerAccessor)self;
            addRenderableWidget(Button.builder(Component.translatable("simplemodels.browser.models"), b -> client.setScreenAndShow(new ModelBrowser(self)))
                    .bounds(Math.max(6, container.simplemodels$left()), Math.max(6, container.simplemodels$top() - 22), 80, 20).build());
        }
    }
}
