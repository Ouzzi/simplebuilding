package com.simplebuilding.client.gui;

import com.simplebuilding.screen.AutoSmitherMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * Bildschirm des Auto-Schmieds: Vanillas Schmiedetisch-Hintergrund mit denselben wechselnden Slot-Silhouetten, rechts
 * die Redstone-Anzeige des Crafters (leuchtet, solange ein Signal anliegt). Keine Knoepfe: geschmiedet wird per Redstone.
 */
public class AutoSmitherScreen extends AbstractContainerScreen<AutoSmitherMenu> {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/smithing.png");
    private static final Identifier POWERED = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
    private static final Identifier UNPOWERED = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final List<Identifier> TEMPLATE_ICONS = List.of(
            Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim"),
            Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade"));
    private static final int REDSTONE_X = 133;
    private static final int REDSTONE_Y = 48;

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionIcon = new CyclingSlotBackground(2);

    public AutoSmitherScreen(AutoSmitherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelX = 44;
        this.titleLabelY = 15;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ItemStack template = this.menu.getSlot(0).getItem();
        SmithingTemplateItem item = template.getItem() instanceof SmithingTemplateItem t ? t : null;
        this.templateIcon.tick(TEMPLATE_ICONS);
        this.baseIcon.tick(item == null ? List.of() : item.getBaseSlotEmptyIcons());
        this.additionIcon.tick(item == null ? List.of() : item.getAdditionalSlotEmptyIcons());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.templateIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.baseIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.additionIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.menu.isPowered() ? POWERED : UNPOWERED,
                this.leftPos + REDSTONE_X, this.topPos + REDSTONE_Y, 16, 16);
    }
}
