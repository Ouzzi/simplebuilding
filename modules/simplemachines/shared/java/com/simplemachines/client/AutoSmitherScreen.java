package com.simplemachines.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplemachines.SimpleMachines;
import com.simplemachines.smither.AutoSmitherMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * The Auto Smither's menu (SimpleBuilding's {@code AutoSmitherScreen} with its 26.3 container style, MACH32-MOD stage 4; look
 * unchanged): Crafter layout with three smithing inputs and cycling silhouettes. Style on: the smither's grey box with the redstone
 * motif, the light inventory box, the big result slot and the redstone dust arrow (red while powered, the error colour when the
 * inputs make no recipe; owner 2026-10-20). Style off: the Vanilla-derived Crafter background and Vanilla's error arrow.
 */
public class AutoSmitherScreen extends AbstractContainerScreen<AutoSmitherMenu> {
    private static final Identifier BACKGROUND = SimpleMachines.id("textures/gui/container/auto_smither.png");
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/smithing/error");
    private static final List<Identifier> TEMPLATE_ICONS = List.of(
            Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim"),
            Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade"));
    /** SimpleBuilding's smither palette (ModScreenStyle.AUTO_SMITHER) and error red. */
    public static final UiPalette PALETTE = UiPalette.derived(0xFF4F5560);
    private static final int ERROR = 0xFFC9503E;

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionIcon = new CyclingSlotBackground(2);

    /** N62 boxes (style on: no shift, the box ends 3 px under the result slot, the image 169 high; style off: 166). */
    private final UiMargin.Boxes boxes;

    public AutoSmitherScreen(AutoSmitherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, UiStyleToggle.isEnabled() ? MachineMargins.autoSmither(menu).height() : 166);
        this.boxes = MachineMargins.autoSmither(menu);
        if (UiStyleToggle.isEnabled()) UiMargin.moveInventory(menu.slots, this.boxes.shift());
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = UiStyleToggle.isEnabled() ? 8 : (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (UiStyleToggle.isEnabled()) {
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, PALETTE.label(), false);
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
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
        boolean styled = styled(graphics, this.leftPos, this.topPos);
        if (!styled) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        }
        this.templateIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.baseIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.additionIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        if (!styled && this.menu.hasRecipeError()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, this.leftPos + 91, this.topPos + 33, 28, 21);
        }
    }

    /** The container style as SimpleBuilding drew it (ModScreenStyle.autoSmither): same boxes, motif seed, arrow and slots. */
    private boolean styled(GuiGraphicsExtractor g, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        // N62: 3 px fill round the inputs, arrow and result slot and round the inventory, both boxes with the shadow
        UiMargin.Rect c = this.boxes.container(), i = this.boxes.inventory();
        UiBoxes.box(g, left + c.x(), top + c.y(), c.width(), c.height(), PALETTE);
        UiBoxes.box(g, left + i.x(), top + i.y(), i.width(), i.height(), UiPalette.INVENTORY);
        Slot result = this.menu.getSlot(AutoSmitherMenu.RESULT_SLOT);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        avoid.add(new int[] {left + MachineMargins.ARROW_X, top + MachineMargins.ARROW_Y, left + MachineMargins.ARROW_X + MachineMargins.ARROW_WIDTH,
                top + MachineMargins.ARROW_Y + MachineMargins.ARROW_HEIGHT});
        avoid.add(new int[] {left + result.x - 4, top + result.y - 4, left + result.x + 21, top + result.y + 21});
        UiMotifs.draw(g, 0, 0, UiMotifs.Kind.REDSTONE, left + c.x() + UiBoxes.FRAME, top + c.y() + UiBoxes.FRAME, c.width() - 2 * UiBoxes.FRAME,
                c.height() - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, PALETTE, avoid, 4);
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == result) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, PALETTE);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : PALETTE);
        }
        if (this.menu.hasRecipeError()) UiSymbols.engrave(g, UiSymbols.ARROW_DUST, left + 99, top + 36, PALETTE, ERROR, true);
        else UiSymbols.redstoneArrow(g, left + 99, top + 36, PALETTE, this.menu.isPowered());
        return true;
    }
}
