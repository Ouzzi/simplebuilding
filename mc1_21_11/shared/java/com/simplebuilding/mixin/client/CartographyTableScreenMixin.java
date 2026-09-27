package com.simplebuilding.mixin.client;

import com.simplebuilding.blueprint.BlueprintCartography;
import com.simplebuilding.client.blueprint.BlueprintTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kartentisch: liegt oben eine signierte Blaupause, zeigt das grosse Kartenfeld statt der
 * (leeren) Karte dieselbe kreisende 3D-Miniatur wie ihr Tooltip ({@link BlueprintTooltip#renderPreview}).
 * Das Pergamentblatt von Vanilla bleibt als Rahmen stehen. Alles andere bleibt Vanilla.
 */
@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
    /** Innenflaeche des Kartenblatts (Vanilla: Karte ab 71/17, 128 px * 0.45). */
    private static final int SIMPLEBUILDING$PREVIEW_X = 71;
    private static final int SIMPLEBUILDING$PREVIEW_Y = 17;
    private static final int SIMPLEBUILDING$PREVIEW_SIZE = 58;

    private CartographyTableScreenMixin(CartographyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void simplebuilding$blueprintPreview(GuiGraphics graphics, float a, int mouseX, int mouseY, CallbackInfo ci) {
        String code = BlueprintCartography.previewCode(this.menu.getSlot(BlueprintCartography.MAP_SLOT).getItem(),
                this.menu.getSlot(BlueprintCartography.ADDITIONAL_SLOT).getItem());
        if (code == null) {
            return;
        }
        // Eigene Schicht: innerhalb einer Schicht sortiert der GUI-Renderer nach Textur, das Blatt
        // (GUI-Atlas) koennte sonst ueber der Miniatur (Block-Atlas) landen.
        graphics.nextStratum();
        BlueprintTooltip.renderPreview(graphics, code, this.leftPos + SIMPLEBUILDING$PREVIEW_X,
                this.topPos + SIMPLEBUILDING$PREVIEW_Y, SIMPLEBUILDING$PREVIEW_SIZE);
    }
}
