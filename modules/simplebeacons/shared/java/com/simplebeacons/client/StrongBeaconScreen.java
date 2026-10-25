package com.simplebeacons.client;

import com.simplebeacons.beacon.RangeFactor;
import com.simplebeacons.beacon.RangeFactorView;
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
import net.minecraft.client.resources.language.I18n;
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
 * greyed out for the new effects. SP22-10: every primary row has three effects - Vanilla's buttons of rows 1-3 move onto
 * the three-column grid and the new ones join them - and the right field shows the material factor ("Material x1.25")
 * with the range it gives.
 */
public class StrongBeaconScreen extends BeaconScreen {
    /** Vanilla's image height and the two extra rows (50 px). */
    public static final int VANILLA_HEIGHT = 219, EXTRA = 50, FULL_HEIGHT = VANILLA_HEIGHT + EXTRA, IMAGE_WIDTH = 230;
    /** Row of level 5 / 6 like Vanilla's rows: y = 22 + 25 * (level - 2); buttons are 22 px, 2 px apart, centered at x = 76. */
    public static final int ROW_Y = 22, ROW_STEP = 25, BUTTON_CENTER_X = 76, BUTTON_STEP = 24, BUTTON_SIZE = 22;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("simplebeacons", "textures/gui/container/strong_beacon.png");
    private static final int SHEET_WIDTH = 256, SHEET_HEIGHT = 288;
    /** Vanilla's label colour on Vanilla's field colour (#212121, measured in beacon.png): contrast 12.2:1. */
    public static final int LABEL = 0xFFE0E0E0, FIELD = 0xFF212121;
    private static final int DIM = 0xFF8A8A8A;
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BUTTON_SELECTED = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("container/beacon/button");
    private final List<StrongButton> strongButtons = new ArrayList<>();
    /** The menu with the server's settings (max pyramid, range, switched-off effects). */
    private final StrongBeaconMenu strongMenu;
    /** VIS24-2: how far the image is moved up when the window is lower than it (mouse wheel), 0 when it fits. */
    private int scroll;

    public StrongBeaconScreen(StrongBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        reportHeight(FULL_HEIGHT);
        for (Slot slot : menu.slots) ((SlotYAccessor) slot).simplebeacons$setY(slot.y + EXTRA);
        this.strongMenu = menu;
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
        // Vanilla's primary buttons of rows 1-3 (left half, in creation order) onto the three-column grid
        int[] vanillaInRow = new int[3];
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (!(child instanceof AbstractWidget widget) || !widget.getClass().getName().endsWith("$BeaconPowerButton")
                    || widget.getX() >= this.leftPos + 120) continue;
            int tier = (widget.getY() - this.topPos - ROW_Y) / ROW_STEP;
            if (tier < 0 || tier > 2) continue;
            widget.setX(columnX(vanillaInRow[tier]++, StrongBeaconLogic.EFFECTS.get(tier).size()));
        }
        for (int tier = 1; tier <= 6; tier++) {
            if (tier == 4) continue;
            List<Holder<MobEffect>> effects = StrongBeaconLogic.EFFECTS.get(tier - 1);
            int count = effects.size(), first = tier <= 3 ? vanillaInRow[tier - 1] : 0;
            for (int c = first; c < count; c++) {
                int row = tier <= 3 ? tier - 1 : tier - 2;
                StrongButton button = new StrongButton(columnX(c, count), this.topPos + ROW_Y + row * ROW_STEP, effects.get(c), tier);
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
        // VIS24-2: a window lower than the image (854x480 at GUI scale 2 gives 240 px) cut off the top and the hotbar; then
        // the image stands at the top and scrolls with the mouse wheel - layout and look stay Vanilla's (owner T12)
        int max = this.overflow();
        this.scroll = Math.min(this.scroll, max);
        if (max > 0) {
            int shift = MARGIN - this.scroll - this.topPos;
            this.topPos += shift;
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) widget.setY(widget.getY() + shift);
            }
        }
    }

    /** Gap kept above and below the image when it does not fit. */
    private static final int MARGIN = 2;

    /** Pixels of the image that do not fit into the window (0 when it fits). */
    private int overflow() {
        return Math.max(0, FULL_HEIGHT + 2 * MARGIN - this.height);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int max = this.overflow();
        if (max > 0 && scrollY != 0) {
            int next = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollY) * ROW_STEP));
            if (next != this.scroll) {
                this.scroll = next;
                this.rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    /** Left edge of column {@code c} of a centred row with {@code count} buttons (Vanilla's formula). */
    private int columnX(int c, int count) {
        int totalWidth = count * BUTTON_SIZE + (count - 1) * (BUTTON_STEP - BUTTON_SIZE);
        return this.leftPos + BUTTON_CENTER_X + c * BUTTON_STEP - totalWidth / 2;
    }

    /** "Material x1.25" with the language's decimal separator. */
    public static Component factorLine(int percent) {
        String value = RangeFactor.format(percent).replace(".", I18n.get("simplebeacons.screen.decimal_separator"));
        return Component.translatable("simplebeacons.screen.factor", value);
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
        int max = this.overflow();
        if (max > 0) { // where the window is on the image: a thin bar right of it (like the linked panel's)
            int track = full - 2 * MARGIN, thumb = Math.max(8, track * full / (FULL_HEIGHT + 2 * MARGIN));
            int x = this.leftPos + IMAGE_WIDTH + 3, top = MARGIN + (track - thumb) * this.scroll / max;
            graphics.fill(x, MARGIN, x + 2, MARGIN + track, FIELD);
            graphics.fill(x, top, x + 2, top + thumb, LABEL);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        int levels = this.menu.getLevels();
        int max = this.strongMenu.maxLevels();
        int centre = 169; // the right field, like Vanilla's "Secondary Power" label
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.pyramid", levels, max), centre, 80, LABEL);
        int shown = Math.max(1, levels);
        int percent = ((RangeFactorView) this.menu).simplebeacons$rangePercent();
        if (percent > 0) graphics.centeredText(this.font, factorLine(percent), centre, 92, levels > 0 ? LABEL : DIM);
        long range = Math.round(RangeFactor.apply(this.strongMenu.range(shown), percent > 0 && levels > 0 ? percent : RangeFactor.NONE));
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.range", range),
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
            boolean reachable = this.tier <= levels && this.tier <= StrongBeaconScreen.this.strongMenu.maxLevels()
                    && StrongBeaconScreen.this.strongMenu.effectEnabled(this.effect);
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
