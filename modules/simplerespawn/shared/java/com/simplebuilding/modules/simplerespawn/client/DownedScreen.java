package com.simplebuilding.modules.simplerespawn.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The downed screen, laid out like the Vanilla death screen (red gradient, large title, buttons in the same places):
 * remaining bleed-out time, the revive bar while someone helps, and the buttons Spectate, Inventory and Give up. Like
 * Vanilla the buttons wake up after one second, so a held click does not give up by accident.
 */
public final class DownedScreen extends Screen {
    /** Dark red box colours; the lines on it are white, light grey and light green (>= 7:1). */
    private static final UiPalette PANEL = UiPalette.derived(0xFF4A2424);

    private final List<Button> buttons = new ArrayList<>();
    private int delay;

    public DownedScreen() {
        super(Component.translatable("screen.simplerespawn.title"));
    }

    @Override
    protected void init() {
        buttons.clear();
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 72;
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("screen.simplerespawn.spectate"),
                b -> RespawnClient.spectate(this.minecraft)).bounds(x, y, 200, 20).build()));
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("screen.simplerespawn.inventory"),
                b -> { if (this.minecraft.player != null) this.minecraft.gui.setScreen(new ViewOnlyInventoryScreen(this.minecraft.player)); })
                .bounds(x, y + 24, 200, 20).build()));
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("screen.simplerespawn.give_up").withStyle(ChatFormatting.RED), b -> {
            b.active = false;
            RespawnClient.giveUp(this.minecraft);
        }).bounds(x, y + 48, 200, 20).build()));
        for (Button b : buttons) b.active = delay >= 20;
    }

    @Override
    public void tick() {
        super.tick();
        if (++delay == 20) for (Button b : buttons) b.active = true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 1615855616, -1602211792);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int mid = this.width / 2;
        // Text box (SimpleLib box style, dark red): the white/grey lines sit on a flat dark face instead of the gradient.
        UiBoxes.box(graphics, mid - 130, 78, 260, 48, PANEL, false);
        graphics.pose().pushMatrix();
        graphics.pose().scale(2f, 2f);
        graphics.centeredText(this.font, this.title, mid / 2, 30, 0xFFFFFFFF);
        graphics.pose().popMatrix();
        int seconds = (RespawnClient.ticksLeft + 19) / 20;
        graphics.centeredText(this.font, Component.translatable("screen.simplerespawn.bleeding", Component.literal(Integer.toString(seconds))
                .withStyle(ChatFormatting.YELLOW)), mid, 85, 0xFFFFFFFF);
        if (RespawnClient.progress > 0f) {
            graphics.centeredText(this.font, Component.translatable("screen.simplerespawn.helped", RespawnClient.other), mid, 100, 0xFF80FF80);
            RespawnHud.bar(graphics, mid - 50, 112, 100, RespawnClient.progress);
        } else {
            graphics.centeredText(this.font, Component.translatable("screen.simplerespawn.wait"), mid, 100, 0xFFC0C0C0);
        }
    }
}
