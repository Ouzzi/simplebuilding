package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.DeedMapLayout;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedMap;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The deed's card (WH26-9, owner 08.11.: variant A): light parchment with the simplelib box frame (stepped corners),
 * folded in thirds; bookmarks stick out on the left (Map, Buy, Rights - like the Megaguide's) and on the right (one per
 * dimension of the server, the last one "next page" when they do not all fit). The tabs are real widgets (click,
 * keyboard, tooltip); they are drawn in the background pass so the card covers their inner end. Subclasses draw the
 * inside: {@link DeedMapScreen} (map, locator) and {@link DeedTradeScreen} (buy tab).
 */
abstract class DeedCardScreen extends Screen {
    static final int RESULT_TICKS = 100;
    /** The last dimension tab and tab page per deed owner, so a refresh or a page change keeps it. */
    private static final Map<UUID,String> LAST_TAB = new HashMap<>();
    private static final Map<UUID,Integer> LAST_PAGE = new HashMap<>();

    final DeedOpenPayload data;
    final DeedMap map;
    DeedMapLayout layout;
    String tab;
    int resultTicks;
    private final List<SideTab> sideTabs = new ArrayList<>();

    DeedCardScreen(Component title, DeedOpenPayload data) {
        super(title);
        this.data = data;
        this.map = data.map();
        this.resultTicks = map.result().isEmpty() ? 0 : RESULT_TICKS;
        String remembered = LAST_TAB.get(data.ownerId());
        tab = remembered != null && map.tabs().stream().anyMatch(t -> t.dimension().equals(remembered)) ? remembered : startTab();
    }

    /** The holder's dimension if it has land there, else the first tab with land, else the holder's. */
    private String startTab() {
        String here = data.dimension();
        if (map.chunksIn(here) > 0) return here;
        for (var t : map.tabs()) if (t.chunks() > 0) return t.dimension();
        return map.tabs().isEmpty() ? here : map.tabs().get(0).dimension();
    }

    /** 0 map, 1 buy, 2 rights: the left tab of this screen. */
    abstract int page();
    /** Called when a dimension tab is picked (the map shows it; the buy tab goes back to the map). */
    abstract void pickDimension(String dimension);

    @Override
    protected void init() {
        layout = DeedMapLayout.of(width, height);
        LAST_TAB.put(data.ownerId(), tab);
        sideTabs.clear();
        side(layout.leftTab(0), Component.translatable("simpletweaks.deed.tab.map"), page() == 0, () -> { if (page() != 0) minecraft.gui.setScreen(new DeedMapScreen(data)); },
                Component.translatable("simpletweaks.deed.tab.map.tip"), (g, r) -> g.item(new ItemStack(Items.FILLED_MAP), r.x() + 4, r.y() + 4));
        int next = 1;
        if (map.owns())
            side(layout.leftTab(next++), Component.translatable("simpletweaks.deed.buy"), page() == 1, () -> { if (page() != 1) minecraft.gui.setScreen(new DeedTradeScreen(data)); },
                    Component.translatable("simpletweaks.deed.buy.tip", map.credits()), (g, r) -> DeedIcons.coin(g, r.x() + 6, r.y() + 6));
        side(layout.leftTab(next), Component.translatable("simpletweaks.deed.rights"), false, () -> minecraft.gui.setScreen(new DeedScreen(data, this)),
                Component.translatable("simpletweaks.deed.rights.tip"), (g, r) -> DeedIcons.key(g, r.x() + 7, r.y() + 7));
        var tabs = map.tabs();
        int slots = layout.rightSlots(), pages = DeedMapLayout.tabPages(tabs.size(), slots);
        int page = Math.floorMod(LAST_PAGE.getOrDefault(data.ownerId(), 0), pages);
        int[] shown = DeedMapLayout.tabPage(tabs.size(), slots, page);
        for (int i = 0; i < shown.length; i++) {
            Rect r = layout.rightTab(i);
            if (shown[i] < 0) {
                side(r, Component.translatable("simpletweaks.deed.tab.next"), false, () -> { LAST_PAGE.put(data.ownerId(), page + 1); rebuildWidgets(); },
                        Component.translatable("simpletweaks.deed.tab.next.tip", page + 1, pages), (g, rr) -> DeedIcons.arrow(g, rr.x() + 8, rr.y() + 9, DeedStyle.P_INK)).right = true;
                continue;
            }
            var t = tabs.get(shown[i]);
            boolean on = t.dimension().equals(tab) && page() == 0;
            var icon = dimensionIcon(t.dimension());
            side(r, Component.literal(dimensionName(t.dimension())), on, () -> pickDimension(t.dimension()),
                    Component.translatable("simpletweaks.deed.tab.dimension", dimensionName(t.dimension()), t.chunks()), (g, rr) -> g.item(icon, rr.x() + 4, rr.y() + 4)).right = true;
        }
    }

    private SideTab side(Rect r, Component label, boolean active, Runnable press, Component tip, java.util.function.BiConsumer<GuiGraphicsExtractor,Rect> icon) {
        var tab = new SideTab(r, label, active, press, icon);
        tab.setTooltip(Tooltip.create(tip));
        sideTabs.add(tab);
        addRenderableWidget(tab);
        return tab;
    }

    /** Remembers {@code dimension} as the shown tab of this deed. */
    void rememberTab(String dimension) { tab = dimension; LAST_TAB.put(data.ownerId(), dimension); }

    @Override
    public void tick() {
        super.tick();
        if (resultTicks > 0) resultTicks--;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        for (var t : sideTabs) {
            Rect r = t.rect;
            UiBookmarks.tab(g, r.x(), r.y(), r.w(), r.h(), !t.right, DeedStyle.PAPER_BOX, t.on, t.isHoveredOrFocused());
            t.icon.accept(g, t.right ? new Rect(r.x() + 6, r.y(), r.w() - 6, r.h()) : r);
        }
        Rect p = layout.panel;
        UiBoxes.box(g, p.x(), p.y(), p.w(), p.h(), DeedStyle.PAPER_BOX);
        Rect in = layout.inner;
        g.fill(in.x(), in.y(), in.right(), in.bottom(), DeedStyle.PAPER);
        // an active tab opens into the card
        for (var t : sideTabs) if (t.on) {
            Rect r = t.rect;
            if (t.right) g.fill(p.right() - UiBoxes.FRAME, r.y() + 2, p.right(), r.bottom() - 2, DeedStyle.PAPER_BOX.fill());
            else g.fill(p.x(), r.y() + 2, p.x() + UiBoxes.FRAME, r.bottom() - 2, DeedStyle.PAPER_BOX.fill());
        }
        drawCard(g, mouseX, mouseY);
    }

    /** The inside of the card (paper is already there). */
    abstract void drawCard(GuiGraphicsExtractor g, int mouseX, int mouseY);

    /** Text cut to {@code width} with "..." when it does not fit. */
    String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    /** The credits badge: coin + "3 credits" on a light card with a stepped two-layer rim. */
    void badge(GuiGraphicsExtractor g, Rect b, int credits) {
        UiBoxes.stairFrame(g, b.x(), b.y(), b.w(), b.h(), new int[]{DeedStyle.EDGE, DeedStyle.CREASE}, DeedStyle.CARD);
        DeedIcons.coin(g, b.x() + 3, b.y() + 2);
        String text = fit(I18n.get("simpletweaks.deed.credits", credits), b.w() - 20);
        g.text(font, text, b.x() + 18, b.y() + 4, DeedStyle.P_INK, false);
    }

    /** Result of the last action, or null when there is none (or it has faded). */
    Component resultText() {
        String r = map.result();
        if (resultTicks <= 0 || r.isEmpty()) return null;
        String own = "simpletweaks.deed.result." + r;
        if (net.minecraft.locale.Language.getInstance().has(own)) return Component.translatable(own);
        int dot = r.indexOf('.');
        return Component.translatable("command.simpletweaks.claim.buy." + (dot < 0 ? r : r.substring(dot + 1)));
    }
    boolean resultOk() { return map.result().endsWith(".ok"); }

    // ---------------------------------------------------------------- texts

    static String dimensionName(String id) {
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null) return id;
        if (key.getNamespace().equals("minecraft")) {
            String own = "simpletweaks.play.dimension." + key.getPath();
            if (net.minecraft.locale.Language.getInstance().has(own)) return I18n.get(own);
        }
        String vanilla = "dimension." + key.getNamespace() + "." + key.getPath();
        if (net.minecraft.locale.Language.getInstance().has(vanilla)) return I18n.get(vanilla);
        String path = key.getPath().replace('_', ' ');
        return path.isEmpty() ? id : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    /** Overworld grass, Nether netherrack, End end stone; any other dimension a compass. */
    static ItemStack dimensionIcon(String id) {
        return new ItemStack(switch (id) {
            case "minecraft:overworld" -> Items.GRASS_BLOCK;
            case "minecraft:the_nether" -> Items.NETHERRACK;
            case "minecraft:the_end" -> Items.END_STONE;
            default -> Items.COMPASS;
        });
    }

    static Component itemName(String id) {
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key))
            return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key)).getHoverName();
        return Component.literal(id);
    }

    static ItemStack stackOf(String id, int count) {
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)) return new ItemStack(Items.BARRIER);
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key), Math.max(1, count));
    }

    /**
     * A bookmark of the card: a real button (mouse, keyboard, narration, tooltip); its picture is drawn in the
     * background pass ({@link #extractBackground}) so the card covers the inner end.
     */
    static final class SideTab extends AbstractButton {
        final Rect rect;
        final boolean on;
        final Runnable press;
        final java.util.function.BiConsumer<GuiGraphicsExtractor,Rect> icon;
        boolean right;

        SideTab(Rect r, Component label, boolean on, Runnable press, java.util.function.BiConsumer<GuiGraphicsExtractor,Rect> icon) {
            super(r.x(), r.y(), r.w(), r.h(), label);
            this.rect = r; this.on = on; this.press = press; this.icon = icon;
        }
        @Override public void onPress(InputWithModifiers input) { press.run(); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) { }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
