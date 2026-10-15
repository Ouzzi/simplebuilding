package com.simplebeacons.client;

import com.simplebeacons.beacon.StrongBeaconLogic;
import com.simplebeacons.beacon.StrongBeaconMenu;
import com.simplebeacons.mixin.client.BeaconScreenAccessor;
import com.simplebeacons.mixin.client.ContainerScreenHeightAccessor;
import com.simplebeacons.mixin.client.SlotYAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Strong Beacon screen (concept §3.3, owner T12 15.10.): Vanilla's beacon screen with Vanilla's own layout and look,
 * 50 px taller - two more effect rows on the left (levels 5 and 6, three effects each) below Vanilla's three, the payment
 * slot, buttons and inventory moved down by that much. The background is Vanilla's beacon.png stretched by those rows
 * (tools/textures/strong_beacon_gui.py, built from the client jar, never committed); the pyramid counter, range and
 * duration sit in the right field under level 4. Locked rows are greyed out with a tooltip; the "II" upgrade button is
 * greyed out for the new effects.
 */
public class StrongBeaconScreen extends BeaconScreen {
    /** Vanilla's image height and the two extra rows (50 px). */
    public static final int VANILLA_HEIGHT = 219, EXTRA = 50, FULL_HEIGHT = VANILLA_HEIGHT + EXTRA, IMAGE_WIDTH = 230;
    /** Row of level 5 / 6 like Vanilla's rows: y = 22 + 25 * (level - 2); buttons are 22 px, 2 px apart, centered at x = 76. */
    public static final int ROW_Y = 22, ROW_STEP = 25, BUTTON_CENTER_X = 76, BUTTON_STEP = 24, BUTTON_SIZE = 22;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("simplebeacons", "textures/gui/container/strong_beacon.png");
    private static final int SHEET_WIDTH = 256, SHEET_HEIGHT = 288;
    private static final int LABEL = 0xFFE0E0E0;
    private static final int DIM = 0xFF8A8A8A;
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BUTTON_SELECTED = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("container/beacon/button");
    private final List<StrongButton> strongButtons = new ArrayList<>();

    public StrongBeaconScreen(StrongBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        reportHeight(FULL_HEIGHT);
        for (Slot slot : menu.slots) ((SlotYAccessor) slot).simplebeacons$setY(slot.y + EXTRA);
    }

    private void reportHeight(int imageHeight) {
        ((ContainerScreenHeightAccessor) this).simplebeacons$setImageHeight(imageHeight);
    }

    /** Screen factory for the menu type (the Vanilla screen is typed on {@code BeaconMenu}). */
    public static StrongBeaconScreen create(net.minecraft.world.inventory.BeaconMenu menu, Inventory inventory, Component title) {
        return new StrongBeaconScreen((StrongBeaconMenu) menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.strongButtons.clear();
        for (int tier = 5; tier <= 6; tier++) {
            List<Holder<MobEffect>> effects = StrongBeaconLogic.EFFECTS.get(tier - 1);
            int count = effects.size(), totalWidth = count * BUTTON_SIZE + (count - 1) * (BUTTON_STEP - BUTTON_SIZE);
            for (int c = 0; c < count; c++) {
                StrongButton button = new StrongButton(this.leftPos + BUTTON_CENTER_X + c * BUTTON_STEP - totalWidth / 2,
                        this.topPos + ROW_Y + (tier - 2) * ROW_STEP, effects.get(c), tier);
                this.addRenderableWidget(button);
                this.strongButtons.add(button);
            }
        }
        // Vanilla's confirm and cancel buttons sit at y = 107 below the fields: down with the payment slot.
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget && !(child instanceof StrongButton) && widget.getY() == this.topPos + 107) {
                widget.setY(widget.getY() + EXTRA);
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int levels = this.menu.getLevels();
        this.strongButtons.forEach(b -> b.updateStatus(levels));
        Holder<MobEffect> primary = ((BeaconScreenAccessor) this).simplebeacons$primary();
        if (StrongBeaconLogic.isStrongEffect(primary)) {
            // No "II" for the new effects (owner 13.10.: Absorption only level I); the class is private in Vanilla.
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget && widget.getClass().getName().endsWith("$BeaconUpgradePowerButton")) {
                    widget.active = false;
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Vanilla (and the Simple Interfaces styling) draw their 219 px image from "height": move that draw far above the
        // screen, dim the whole screen ourselves, then draw the stretched Vanilla image and the payment hints.
        int full = this.height;
        reportHeight(VANILLA_HEIGHT);
        this.height = 2 * this.topPos + VANILLA_HEIGHT - 4096;
        try {
            super.extractBackground(graphics, mouseX, mouseY, a);
        } finally {
            this.height = full;
            reportHeight(FULL_HEIGHT);
        }
        graphics.fillGradient(0, 0, this.width, full, -1072689136, -804253680);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, IMAGE_WIDTH, FULL_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        int y = this.topPos + 109 + EXTRA;
        graphics.item(new ItemStack(Items.NETHERITE_INGOT), this.leftPos + 20, y);
        graphics.item(new ItemStack(Items.EMERALD), this.leftPos + 41, y);
        graphics.item(new ItemStack(Items.DIAMOND), this.leftPos + 41 + 22, y);
        graphics.item(new ItemStack(Items.GOLD_INGOT), this.leftPos + 42 + 44, y);
        graphics.item(new ItemStack(Items.IRON_INGOT), this.leftPos + 42 + 66, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        int levels = this.menu.getLevels();
        int max = StrongBeaconLogic.maxLevels();
        int centre = 169; // the right field, like Vanilla's "Secondary Power" label
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.pyramid", levels, max), centre, 80, LABEL);
        int shown = Math.max(1, levels);
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.range", StrongBeaconLogic.range(shown)),
                centre, 104, levels > 0 ? LABEL : DIM);
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.duration", StrongBeaconLogic.durationTicks(shown) / 20),
                centre, 116, levels > 0 ? LABEL : DIM);
    }

    /** The extra buttons' state for tests and the layout check. */
    public List<? extends AbstractWidget> strongButtons() {
        return this.strongButtons;
    }

    private final class StrongButton extends AbstractButton {
        private final Holder<MobEffect> effect;
        private final int tier;
        private final Identifier sprite;
        private boolean selected;
        private Boolean unlocked;

        StrongButton(int x, int y, Holder<MobEffect> effect, int tier) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.translatable(effect.value().getDescriptionId()));
            this.effect = effect;
            this.tier = tier;
            this.sprite = Hud.getMobEffectSprite(effect);
            this.updateStatus(0);
        }

        void updateStatus(int levels) {
            boolean reachable = this.tier <= levels && this.tier <= StrongBeaconLogic.maxLevels();
            this.active = reachable;
            if (this.unlocked == null || reachable != this.unlocked) {
                this.unlocked = reachable;
                Component name = Component.translatable(this.effect.value().getDescriptionId());
                Component line = reachable
                        ? Component.translatable("simplebeacons.screen.effect." + StrongBeaconLogic.path(this.effect)).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("simplebeacons.screen.locked", this.tier, StrongBeaconLogic.pyramidBlocks(this.tier)).withStyle(ChatFormatting.GRAY);
                this.setTooltip(Tooltip.create(Component.empty().append(name).append("\n").append(line)));
            }
            this.selected = this.effect.equals(((BeaconScreenAccessor) StrongBeaconScreen.this).simplebeacons$primary());
        }

        @Override
        public void onPress(InputWithModifiers input) {
            if (this.selected) return;
            BeaconScreenAccessor screen = (BeaconScreenAccessor) StrongBeaconScreen.this;
            screen.simplebeacons$setPrimary(this.effect);
            if (!Objects.equals(screen.simplebeacons$secondary(), this.effect)) screen.simplebeacons$setSecondary(null);
            StrongBeaconScreen.this.containerTick();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier face = !this.active ? BUTTON_DISABLED : this.selected ? BUTTON_SELECTED
                    : this.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, face, this.getX(), this.getY(), this.width, this.height);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX() + 2, this.getY() + 2, 18, 18);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
