package com.simplebeacons.client;

import com.simplebeacons.beacon.BeaconKind;
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
 * The screen of the Strong, Mighty, Evil and Mighty Evil beacons (concept §3.3, owner T12 15.10.; A29 BS28b-4/5): Vanilla's beacon
 * screen with Vanilla's own layout and look, 25 px (Strong, Evil: four rows) or 50 px (Mighty kinds: five rows) taller - more
 * effect rows on the left below Vanilla's three, the payment slot, buttons and inventory moved down by that much. Every pyramid
 * level has its own marker icon (BS28b-4, {@code pyramid_icons.png}, drawn here; the background has none). The beacon kind arrives
 * with the first menu data sync, so the screen rebuilds itself once it is known. The background is Vanilla's beacon.png stretched by those rows
 * (tools/textures/strong_beacon_gui.py, built from the client jar, never committed); the pyramid counter, range and
 * duration sit in the right field under level 4. Locked rows are greyed out with a tooltip; the "II" upgrade button is
 * greyed out for the new effects. SP22-10: every primary row has three effects - Vanilla's buttons of rows 1-3 move onto
 * the three-column grid and the new ones join them - and the right field shows the material factor ("Material x1.25")
 * with the range it gives.
 */
public class StrongBeaconScreen extends BeaconScreen {
    /** Vanilla's image height, the most extra rows (two, 50 px; the sheet holds them) and the image width. */
    public static final int VANILLA_HEIGHT = 219, EXTRA = 50, FULL_HEIGHT = VANILLA_HEIGHT + EXTRA, IMAGE_WIDTH = 230;
    /** Row of level 5 / 6 like Vanilla's rows: y = 22 + 25 * (level - 2); buttons are 22 px, 2 px apart, centered at x = 76. */
    public static final int ROW_Y = 22, ROW_STEP = 25, BUTTON_CENTER_X = 76, BUTTON_STEP = 24, BUTTON_SIZE = 22;
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("simplebeacons", "textures/gui/container/strong_beacon.png");
    private static final int SHEET_WIDTH = 256, SHEET_HEIGHT = 288;
    /** Row of the sheet where the part below the fields starts (Vanilla's row 97 + the sheet's 50 extra rows). */
    private static final int SHEET_BOTTOM = 147, SPLIT = 97;
    /** The level marker icons: one 21 x 20 icon per level 1-6 (row 0 green, row 1 red for Evil), see tools/textures/beacon_pyramid_icons_2026_10_29.py. */
    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("simplebeacons", "textures/gui/container/pyramid_icons.png");
    private static final int ICON_W = 21, ICON_H = 20, ICON_SHEET_W = 126, ICON_SHEET_H = 40;
    /** Where the marker of the row {@code r} sits (Vanilla's string x = 27 centre, top y = 22 + 25 r) and the secondary one. */
    private static final int MARKER_X = 17, MARKER_Y = 22, SECONDARY_MARKER_X = 157;
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
    /** The kind this layout was built for, and the extra height already applied to the slots. */
    private BeaconKind shownKind;
    private int appliedExtra;
    /** Amplifier of the power the player clicked last (-1 = none: the beacon's own choice counts). */
    private int pickedAmp = -1;

    public StrongBeaconScreen(StrongBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.strongMenu = menu;
    }

    /** Extra image height of {@code kind}: one row more than Vanilla's three for four primary levels, two for five. */
    private static int extraFor(BeaconKind kind) {
        return (kind.primaryLevels() - 3) * ROW_STEP;
    }

    private int extra() {
        return this.appliedExtra;
    }

    private int fullHeight() {
        return VANILLA_HEIGHT + this.appliedExtra;
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
        this.shownKind = this.strongMenu.kind();
        int want = extraFor(this.shownKind);
        for (Slot slot : this.strongMenu.slots) ((SlotYAccessor) slot).simplebeacons$setY(slot.y + want - this.appliedExtra);
        this.appliedExtra = want;
        reportHeight(fullHeight());
        super.init();
        this.strongButtons.clear();
        BeaconKind kind = this.shownKind;
        // Vanilla's own primary buttons (left half) give way to ours: every power of every kind is one of our buttons, which
        // also tell the server the amplifier of the placeholder powers. Vanilla's secondary buttons stay unless the kind is evil.
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (!(child instanceof AbstractWidget widget) || child instanceof StrongButton) continue;
            String name = widget.getClass().getName();
            boolean power = name.endsWith("$BeaconPowerButton") || name.endsWith("$BeaconUpgradePowerButton");
            if (!power) continue;
            if (widget.getX() < this.leftPos + 120 || kind.evil()) {
                widget.visible = false;
                widget.active = false;
            }
        }
        for (int tier = 1; tier <= kind.primaryLevels(); tier++) {
            List<BeaconKind.Power> powers = kind.powers(tier);
            for (int c = 0; c < powers.size(); c++) {
                StrongButton button = new StrongButton(columnX(c, powers.size()), this.topPos + ROW_Y + (tier - 1) * ROW_STEP, powers.get(c).effect(),
                        powers.get(c).amplifier(), tier, false);
                this.addRenderableWidget(button);
                this.strongButtons.add(button);
            }
        }
        if (kind.evil()) {
            // Vanilla's secondary spot (x 156, y 47 of the image): the evil secondary power
            for (var effect : kind.secondaries()) {
                StrongButton button = new StrongButton(this.leftPos + 156, this.topPos + 47, effect, 0, kind.secondaryLevel(), true);
                this.addRenderableWidget(button);
                this.strongButtons.add(button);
            }
        }
        // Vanilla's confirm and cancel buttons sit at y = 107 below the fields: down with the payment slot.
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget && !(child instanceof StrongButton) && widget.getY() == this.topPos + 107) {
                widget.setY(widget.getY() + extra());
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
        return Math.max(0, fullHeight() + 2 * MARGIN - this.height);
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
        if (this.strongMenu.kind() != this.shownKind) {
            this.rebuildWidgets(); // the kind arrived with the data sync
            return;
        }
        BeaconKind kind = this.shownKind;
        int levels = this.menu.getLevels();
        this.strongButtons.forEach(b -> b.updateStatus(levels));
        Holder<MobEffect> primary = ((BeaconScreenAccessor) this).simplebeacons$primary();
        for (GuiEventListener child : this.children()) {
            if (!(child instanceof AbstractWidget widget) || child instanceof StrongButton || !widget.visible) continue;
            String name = widget.getClass().getName();
            // Vanilla's secondary buttons: from the kind's top level (not Vanilla's 4); "II" only for powers that can be raised
            if (name.endsWith("$BeaconPowerButton") && widget.getX() >= this.leftPos + 120 && levels < kind.secondaryLevel()) {
                widget.active = false;
            } else if (name.endsWith("$BeaconUpgradePowerButton") && (levels < kind.secondaryLevel() || !StrongBeaconLogic.canRaise(kind, primary))) {
                widget.active = false;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Vanilla (and the Simple Interfaces styling) draw their 219 px image from "height": move that draw far above the
        // screen, dim the whole screen ourselves, then draw the stretched Vanilla image and the payment hints.
        int full = this.height;
        int fullImage = fullHeight();
        reportHeight(VANILLA_HEIGHT);
        this.height = 2 * this.topPos + VANILLA_HEIGHT - 4096;
        try {
            super.extractBackground(graphics, mouseX, mouseY, a);
        } finally {
            this.height = full;
            reportHeight(fullImage);
        }
        graphics.fillGradient(0, 0, this.width, full, -1072689136, -804253680);
        // The sheet holds Vanilla's top, two extra row periods and the bottom: take the top with this kind's rows, then the bottom
        int extra = extra();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, IMAGE_WIDTH, SPLIT + extra, SHEET_WIDTH, SHEET_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos + SPLIT + extra, 0.0F, (float) SHEET_BOTTOM, IMAGE_WIDTH,
                VANILLA_HEIGHT - SPLIT, SHEET_WIDTH, SHEET_HEIGHT);
        drawMarkers(graphics);
        int y = this.topPos + 109 + extra;
        graphics.item(new ItemStack(Items.NETHERITE_INGOT), this.leftPos + 20, y);
        graphics.item(new ItemStack(Items.EMERALD), this.leftPos + 41, y);
        graphics.item(new ItemStack(Items.DIAMOND), this.leftPos + 41 + 22, y);
        graphics.item(new ItemStack(Items.GOLD_INGOT), this.leftPos + 42 + 44, y);
        graphics.item(new ItemStack(Items.IRON_INGOT), this.leftPos + 42 + 66, y);
        int max = this.overflow();
        if (max > 0) { // where the window is on the image: a thin bar right of it (like the linked panel's)
            int track = full - 2 * MARGIN, thumb = Math.max(8, track * full / (fullImage + 2 * MARGIN));
            int x = this.leftPos + IMAGE_WIDTH + 3, top = MARGIN + (track - thumb) * this.scroll / max;
            graphics.fill(x, MARGIN, x + 2, MARGIN + track, FIELD);
            graphics.fill(x, top, x + 2, top + thumb, LABEL);
        }
    }

    /** The level markers: one icon per row (levels 1 to the primary count) and the secondary one, lit like the pyramid level they need. */
    private void drawMarkers(GuiGraphicsExtractor graphics) {
        BeaconKind kind = this.shownKind;
        float v = kind.evil() ? ICON_H : 0;
        for (int tier = 1; tier <= kind.primaryLevels(); tier++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, this.leftPos + MARKER_X, this.topPos + MARKER_Y + (tier - 1) * ROW_STEP,
                    (float) ((tier - 1) * ICON_W), v, ICON_W, ICON_H, ICON_SHEET_W, ICON_SHEET_H);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, this.leftPos + SECONDARY_MARKER_X, this.topPos + MARKER_Y,
                (float) ((kind.secondaryLevel() - 1) * ICON_W), v, ICON_W, ICON_H, ICON_SHEET_W, ICON_SHEET_H);
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
        long range = Math.round(Math.min(StrongBeaconLogic.RANGE_CAP, RangeFactor.apply(this.strongMenu.range(shown), percent > 0 && levels > 0 ? percent : RangeFactor.NONE)));
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
        private final int amplifier, tier;
        private final boolean secondary;
        private final Identifier sprite;
        private boolean selected;
        private Boolean unlocked;

        StrongButton(int x, int y, Holder<MobEffect> effect, int amplifier, int tier, boolean secondary) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, name(effect, amplifier));
            this.effect = effect;
            this.amplifier = amplifier;
            this.tier = tier;
            this.secondary = secondary;
            this.sprite = Hud.getMobEffectSprite(effect);
            this.updateStatus(0);
        }

        private static Component name(Holder<MobEffect> effect, int amplifier) {
            Component name = Component.translatable(effect.value().getDescriptionId());
            return amplifier > 0 ? Component.empty().append(name).append(" ").append(Component.translatable("enchantment.level." + (amplifier + 1))) : name;
        }

        void updateStatus(int levels) {
            boolean reachable = this.tier <= levels && this.tier <= StrongBeaconScreen.this.strongMenu.maxLevels()
                    && StrongBeaconScreen.this.strongMenu.effectEnabled(this.effect);
            this.active = reachable;
            if (this.unlocked == null || reachable != this.unlocked) {
                this.unlocked = reachable;
                Component line = reachable
                        ? Component.translatable("simplebeacons.screen.effect." + StrongBeaconLogic.path(this.effect)).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("simplebeacons.screen.locked", this.tier, StrongBeaconLogic.pyramidBlocks(this.tier)).withStyle(ChatFormatting.GRAY);
                this.setTooltip(Tooltip.create(Component.empty().append(getMessage()).append("\n").append(line)));
            }
            BeaconScreenAccessor screen = (BeaconScreenAccessor) StrongBeaconScreen.this;
            if (this.secondary) {
                this.selected = this.effect.equals(screen.simplebeacons$secondary());
            } else {
                int chosen = StrongBeaconScreen.this.pickedAmp >= 0 ? StrongBeaconScreen.this.pickedAmp : StrongBeaconScreen.this.strongMenu.primaryAmp();
                this.selected = this.effect.equals(screen.simplebeacons$primary()) && this.amplifier == chosen;
            }
        }

        @Override
        public void onPress(InputWithModifiers input) {
            if (this.selected) return;
            BeaconScreenAccessor screen = (BeaconScreenAccessor) StrongBeaconScreen.this;
            if (this.secondary) {
                screen.simplebeacons$setSecondary(this.effect);
            } else {
                screen.simplebeacons$setPrimary(this.effect);
                if (!Objects.equals(screen.simplebeacons$secondary(), this.effect)) screen.simplebeacons$setSecondary(null);
                // The Vanilla confirm packet carries the effect only: tell the server which button (amplifier) it was
                StrongBeaconScreen.this.pickedAmp = this.amplifier;
                var minecraft = StrongBeaconScreen.this.minecraft;
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(StrongBeaconScreen.this.menu.containerId, StrongBeaconMenu.POWER_BUTTON + this.amplifier);
                }
            }
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
