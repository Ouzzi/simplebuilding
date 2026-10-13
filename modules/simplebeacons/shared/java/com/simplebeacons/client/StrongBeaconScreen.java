package com.simplebeacons.client;

import com.simplebeacons.beacon.StrongBeaconLogic;
import com.simplebeacons.beacon.StrongBeaconMenu;
import com.simplebeacons.mixin.client.BeaconScreenAccessor;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
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

/**
 * The two-column Strong Beacon screen (concept §3.3): Vanilla's beacon screen unchanged on the left (levels 1-4,
 * payment, confirm - so the Simple Interfaces styling still applies), on the right an extra panel in SimpleLib's box
 * style with the pyramid counter, the level 5 and level 6 rows (three effects each) and the range/duration line.
 * Locked rows are greyed out with a tooltip; the "II" upgrade button is greyed out for the new effects.
 */
public class StrongBeaconScreen extends BeaconScreen {
    /** Panel size and offset to the right of Vanilla's 230 px image. */
    public static final int PANEL_X = 232, PANEL_WIDTH = 100, PANEL_HEIGHT = 132;
    public static final int ROW_Y = 30, ROW_STEP = 30, BUTTON_X = 22, BUTTON_STEP = 24, BUTTON_SIZE = 22;
    private static final int LABEL = 0xFFE0E0E0;
    private static final int DIM = 0xFF8A8A8A;
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BUTTON_SELECTED = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("container/beacon/button");
    private final List<StrongButton> strongButtons = new ArrayList<>();

    public StrongBeaconScreen(StrongBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.strongButtons.clear();
        for (int tier = 5; tier <= 6; tier++) {
            List<Holder<MobEffect>> effects = StrongBeaconLogic.EFFECTS.get(tier - 1);
            for (int c = 0; c < effects.size(); c++) {
                StrongButton button = new StrongButton(this.leftPos + PANEL_X + BUTTON_X + c * BUTTON_STEP,
                        this.topPos + ROW_Y + (tier - 5) * ROW_STEP, effects.get(c), tier);
                this.addRenderableWidget(button);
                this.strongButtons.add(button);
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
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos + PANEL_X, y = this.topPos;
        UiBoxes.box(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, UiPalette.INVENTORY);
        // The dark power field like Vanilla's (#212121), sunk into the box.
        UiBoxes.insetColored(graphics, x + 8, y + 20, PANEL_WIDTH - 16, 64, UiPalette.INVENTORY, 0xFF212121);
        UiBoxes.insetColored(graphics, x + 8, y + 90, PANEL_WIDTH - 16, 30, UiPalette.INVENTORY, 0xFF212121);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        int levels = this.menu.getLevels();
        int max = StrongBeaconLogic.maxLevels();
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.pyramid", levels, max),
                PANEL_X + PANEL_WIDTH / 2, 8, UiPalette.DARK_LABEL);
        for (int tier = 5; tier <= 6; tier++) {
            graphics.text(this.font, String.valueOf(tier), PANEL_X + 12, ROW_Y + (tier - 5) * ROW_STEP + 7, tier <= levels ? LABEL : DIM, false);
        }
        int shown = Math.max(1, levels);
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.range", StrongBeaconLogic.range(shown)),
                PANEL_X + PANEL_WIDTH / 2, 95, levels > 0 ? LABEL : DIM);
        graphics.centeredText(this.font, Component.translatable("simplebeacons.screen.duration", StrongBeaconLogic.durationTicks(shown) / 20),
                PANEL_X + PANEL_WIDTH / 2, 107, levels > 0 ? LABEL : DIM);
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
