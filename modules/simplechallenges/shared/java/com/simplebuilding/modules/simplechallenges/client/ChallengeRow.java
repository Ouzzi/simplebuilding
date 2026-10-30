package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.core.PendingChallenge;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.core.RecentChallenges;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;

/**
 * The block under the world type button when "Simple Challenges" is chosen (S3): one line with the chosen challenge, three buttons
 * with the challenges played last and "Select Challenge". It takes no room (height 0, widgets hidden) for every other world type, so
 * a normal world creation looks exactly as before.
 */
public final class ChallengeRow extends GridLayout {
    private static final int WIDTH = 310, BUTTON = 100;
    private final CreateWorldScreen screen;
    private final StringWidget status;
    private final List<Button> recents = new ArrayList<>();
    private final Button select;
    private List<String> recentIds = List.of();
    private boolean shown;

    private ChallengeRow(CreateWorldScreen screen) {
        this.screen = screen;
        spacing(4);
        var font = Minecraft.getInstance().font;
        status = new StringWidget(WIDTH, 9, Component.empty(), font);
        addChild(status, 0, 0, 1, 3);
        for (int i = 0; i < RecentChallenges.SLOTS; i++) {
            int slot = i;
            Button b = Button.builder(Component.empty(), btn -> pick(slot)).bounds(0, 0, BUTTON, 20).build();
            recents.add(addChild(b, 1, i));
        }
        select = addChild(Button.builder(Component.translatable("simplechallenges.ui.select"), b -> openSelect()).bounds(0, 0, WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("simplechallenges.ui.select.tip"))).build(), 2, 0, 1, 3);
        setWidgetsVisible(false);
    }

    /** Hooked into the World tab by the mixin: the row sits below the switches (rows 0-2 are type, seed, switches). */
    public static void attach(CreateWorldScreen screen, GridLayout tabLayout) {
        CreateChallenge.loadPresets();
        ChallengeRow row = new ChallengeRow(screen);
        tabLayout.addChild(row, 3, 0, 1, 2);
        screen.getUiState().addListener(row::update);
    }

    @Override
    public int getHeight() {
        return shown ? super.getHeight() : 0;
    }

    @Override
    public int getWidth() {
        return shown ? super.getWidth() : 0;
    }

    private void setWidgetsVisible(boolean visible) {
        visitWidgets((AbstractWidget w) -> {
            w.visible = visible;
            w.active = visible;
        });
    }

    private void update(WorldCreationUiState ui) {
        boolean ours = CreateChallenge.selected(ui);
        if (!ours) PendingChallenge.clear();
        else CreateChallenge.sync(ui);
        if (ours != shown) {
            shown = ours;
            setWidgetsVisible(ours);
            screen.repositionElements();
        }
        if (ours) refresh();
    }

    /** Labels and states of the line and the three buttons. */
    private void refresh() {
        recentIds = CreateChallenge.recent();
        String chosen = PendingChallenge.peek();
        Preset current = chosen == null ? null : PresetRegistry.get(chosen);
        status.setMessage(current == null ? Component.translatable("simplechallenges.ui.none_chosen").withStyle(ChatFormatting.GRAY)
                : Component.translatable("simplechallenges.ui.chosen", current.name()).withStyle(ChatFormatting.WHITE));
        for (int i = 0; i < recents.size(); i++) {
            Button b = recents.get(i);
            Preset p = i < recentIds.size() ? PresetRegistry.get(recentIds.get(i)) : null;
            boolean on = p != null && p.runnable();
            boolean isChosen = p != null && recentIds.get(i).equals(chosen);
            Component name = p == null ? Component.literal("-") : Component.literal(p.name());
            b.setMessage(isChosen ? name.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE) : name);
            b.active = on && shown;
            b.setTooltip(p == null ? null : Tooltip.create(Component.translatable("simplechallenges.ui.family." + p.family())));
        }
    }

    private void pick(int slot) {
        if (slot < recentIds.size() && CreateChallenge.choose(screen, recentIds.get(slot))) refresh();
    }

    private void openSelect() {
        Minecraft.getInstance().gui.setScreen(new SelectChallengeScreen(screen));
    }
}
