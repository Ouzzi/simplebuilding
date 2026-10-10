package com.simplebuilding.mixin.client;

import com.simplebuilding.api.ModuleScreenStyles;
import com.simplebuilding.client.gui.BackpackScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Astralgewoelbe: die drei Zusatzreihen (das Astrallager hinter den 27 Endertruhen-Slots) tragen dieselbe
 * violette Toenung wie die Zusatzspalten des Rucksacks (Besitzer 2026-10-01). Das Gewoelbe oeffnet ein
 * Vanilla-{@link ChestMenu} mit sechs Reihen und dem Titel {@code block.simplebuilding.astral_vault}.
 */
@Mixin(ContainerScreen.class)
public abstract class AstralVaultScreenMixin extends AbstractContainerScreen<ChestMenu> {
    private static final String SIMPLEBUILDING$VAULT_TITLE = "block.simplebuilding.astral_vault";
    private static final int SIMPLEBUILDING$ENDER_SLOTS = 27;

    private AstralVaultScreenMixin(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void simplebuilding$tintAstralRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!(this.title.getContents() instanceof TranslatableContents key) || !SIMPLEBUILDING$VAULT_TITLE.equals(key.getKey())
                || this.menu.getRowCount() != 6 || ModuleScreenStyles.isRestyled(this)) {
            return; // restyled (simpleinterfaces): the style draws the astral rows itself
        }
        for (int i = SIMPLEBUILDING$ENDER_SLOTS; i < 6 * 9 && i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            graphics.fill(sx, sy, sx + 16, sy + 16, BackpackScreen.TINT_EXTRA_COLUMN);
        }
    }
}
