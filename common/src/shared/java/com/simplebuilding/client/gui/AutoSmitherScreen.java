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
 * Crafter layout with three smithing inputs and cycling silhouettes.
 * Only invalid recipes show the smithing error arrow; redstone controls crafting.
 */
public class AutoSmitherScreen extends AbstractContainerScreen<AutoSmitherMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("simplebuilding", "textures/gui/container/auto_smither.png");
    private static final Identifier ERROR = Identifier.withDefaultNamespace("container/smithing/error");
    private static final List<Identifier> TEMPLATE_ICONS = List.of(
            Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim"),
            Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade"));

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionIcon = new CyclingSlotBackground(2);

    public AutoSmitherScreen(AutoSmitherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
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
        if (this.menu.hasRecipeError()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR,
                    this.leftPos + 91, this.topPos + 33, 28, 21);
        }
    }
}
