package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.DeedMapLayout;
import com.simplebuilding.modules.simpletweaks.deed.DeedMapLayout.Chunk;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedMap;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.MapColor;

/**
 * The claim deed's first page (SP22-8 §4): the owner's land per dimension, like a map. Several chunks: clickable tiles
 * (2x2 slots, 16x16 map pixels) in groups with squeezed, labelled gaps ({@link DeedMapLayout}); one chunk, or a click
 * on a tile: its detail map. Footer: rights (the code editor, second page), buy, claim here, done. It only shows what
 * the server sent; every button is re-checked by the server ({@code DeedService.handleAction}).
 */
public final class DeedMapScreen extends Screen {
    // Paper of the deed (panel) and of the sheet (map paper, lighter); inks >= 4.5:1 on both (A7, see the plan).
    static final int SHEET = DeedStyle.FIELD, INK = DeedStyle.INK, INK_SOFT = DeedStyle.INK_SOFT, INK_ERROR = DeedStyle.INK_ERROR;
    static final int UNKNOWN = 0xFFDCCFA8, UNKNOWN_LINE = 0xFFC9BA8E, HOVER = 0xFFFFFFFF, TAB_IDLE = DeedStyle.TAB_IDLE;
    private static final int RETRY_TICKS = 12, RETRIES = 3, RESULT_TICKS = 100;
    /** The last tab per deed owner, so a refresh after an action keeps the page. */
    private static final Map<java.util.UUID,String> LAST_TAB = new HashMap<>();

    private final DeedOpenPayload data;
    private final DeedMap map;
    private DeedMapLayout layout;
    private String tab;
    private int tabOffset;
    private final Map<String,List<DeedChunksPayload.Tile>> tiles = new HashMap<>();
    private final Map<String,DeedChunksPayload.Tile[][]> pending = new HashMap<>();
    private DeedMapLayout.Overview overview;
    private Map<Chunk,DeedChunksPayload.Tile> byChunk = Map.of();
    private Chunk detail;
    private boolean confirming;
    private double scrollX, scrollY;
    private double[] dragFrom;
    private boolean dragged;
    private int waited, retries, resultTicks;
    private List<Rect> tabRects = List.of();
    private List<DeedMap.Tab> tabList = List.of();

    public DeedMapScreen(DeedOpenPayload data) {
        super(Component.translatable("simpletweaks.deed.title.owner", data.owner()));
        this.data = data;
        this.map = data.map();
        this.resultTicks = map.result().isEmpty() ? 0 : RESULT_TICKS;
        String remembered = LAST_TAB.get(data.ownerId());
        tab = remembered != null && map.tabs().stream().anyMatch(t -> t.dimension().equals(remembered)) ? remembered : startTab();
    }

    /** The holder's dimension if it has a tab, else the first tab with land, else Overworld. */
    private String startTab() {
        String here = data.dimension();
        if (map.chunksIn(here) > 0) return here;
        for (var t : map.tabs()) if (t.chunks() > 0) return t.dimension();
        return map.tabs().isEmpty() ? here : map.tabs().get(0).dimension();
    }

    private int chunksHere() { return map.chunksIn(tab); }
    private UiPalette palette() { return DeedStyle.BOX; }
    private UiPalette sheetPalette() { return UiPalette.derived(SHEET); }
    private boolean hint() { return !map.mapsPresent() && DeedHint.shown(); }

    private Button button(Component label, Rect r, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build());
    }

    @Override
    protected void init() {
        layout = DeedMapLayout.of(width, height, hint());
        LAST_TAB.put(data.ownerId(), tab);
        tabList = map.tabs();
        int[] widths = tabList.stream().mapToInt(t -> font.width(dimensionName(t.dimension()))).toArray();
        if (tabOffset >= widths.length) tabOffset = 0;
        tabRects = layout.tabs(widths, tabOffset);
        if (tabRects.size() < widths.length || tabOffset > 0)
            button(Component.literal(">"), layout.nextTabs(), b -> { tabOffset += Math.max(1, tabRects.size()); if (tabOffset >= tabList.size()) tabOffset = 0; rebuildWidgets(); });
        // footer
        button(Component.translatable("simpletweaks.deed.rights"), layout.button(0), b -> minecraft.gui.setScreen(new DeedScreen(data, this)))
                .setTooltip(Tooltip.create(Component.translatable("simpletweaks.deed.rights.tip")));
        if (map.owns()) {
            var buy = button(Component.translatable("simpletweaks.deed.buy"), layout.button(1), b -> act(DeedActionPayload.BUY, 0));
            buy.active = map.buy() == DeedMap.BUY;
            buy.setTooltip(Tooltip.create(buyText()));
            boolean credit = map.credits() > 0;
            var claim = button(Component.translatable(credit ? "simpletweaks.deed.claim_here" : "simpletweaks.deed.buy_claim"), layout.button(2), b -> act(DeedActionPayload.CLAIM_HERE, 0));
            claim.active = map.here() == DeedMap.HERE_FREE && (credit || map.buy() == DeedMap.BUY);
            claim.setTooltip(Tooltip.create(map.here() == DeedMap.HERE_OWN ? Component.translatable("simpletweaks.deed.here.own")
                    : map.here() != DeedMap.HERE_FREE ? Component.translatable("simpletweaks.deed.here.no")
                    : credit ? Component.translatable("simpletweaks.deed.here.credit", map.credits()) : buyText()));
        }
        button(CommonComponents.GUI_DONE, layout.button(3), b -> onClose());
        // detail page
        Chunk shown = shownDetail();
        if (shown != null && !confirming) {
            if (map.owns()) button(Component.translatable("simpletweaks.deed.unclaim"), layout.detailButton(0), b -> { confirming = true; rebuildWidgets(); });
            if (chunksHere() > 1) button(Component.translatable("simpletweaks.deed.overview"), layout.detailButton(1), b -> { detail = null; rebuildWidgets(); });
        }
        if (confirming && shown != null) {
            button(Component.translatable("simpletweaks.deed.unclaim.yes"), layout.confirmButton(0), b -> {
                confirming = false;
                act(DeedActionPayload.UNCLAIM, ChunkPos.pack(shown.x(), shown.z()));
            });
            button(CommonComponents.GUI_CANCEL, layout.confirmButton(1), b -> { confirming = false; rebuildWidgets(); });
        }
        rebuildOverview();
        if (!tiles.containsKey(tab) && chunksHere() > 0 && !pending.containsKey(tab)) request();
    }

    private void act(int action, long chunk) {
        DeedNet.toServer.accept(new DeedActionPayload(data.hand(), action, tab, chunk));
    }

    private void request() {
        waited = 0;
        pending.put(tab, null);
        DeedNet.toServer.accept(new DeedChunksRequest(data.hand(), tab));
    }

    /** A page of tiles; when all pages of a dimension are there, the overview is laid out. */
    public void onChunks(DeedChunksPayload payload) {
        if (payload.pages() < 1 || payload.pages() > DeedChunksPayload.MAX_PAGES || payload.page() < 0 || payload.page() >= payload.pages()) return;
        if (!pending.containsKey(payload.dimension())) return; // not asked for (or a duplicate of a retry)
        var pages = pending.get(payload.dimension());
        if (pages == null || pages.length != payload.pages()) pages = new DeedChunksPayload.Tile[payload.pages()][];
        pages[payload.page()] = payload.tiles().toArray(DeedChunksPayload.Tile[]::new);
        pending.put(payload.dimension(), pages);
        for (var p : pages) if (p == null) return;
        var all = new ArrayList<DeedChunksPayload.Tile>();
        for (var p : pages) all.addAll(List.of(p));
        pending.remove(payload.dimension());
        tiles.put(payload.dimension(), all);
        if (payload.dimension().equals(tab)) { detail = null; rebuildWidgets(); centre(); }
    }

    private void rebuildOverview() {
        var list = tiles.getOrDefault(tab, List.of());
        var chunks = new ArrayList<Chunk>();
        var index = new HashMap<Chunk,DeedChunksPayload.Tile>();
        for (var t : list) { var c = new Chunk(t.x(), t.z()); chunks.add(c); index.put(c, t); }
        byChunk = index;
        overview = DeedMapLayout.overview(chunks, (blocks, dir) -> font.width(gapLabel(blocks, dir)));
    }

    private void centre() {
        if (overview == null) return;
        int[] f = overview.focus(hereChunk());
        Rect s = layout.sheet;
        scrollX = clamp(f[0] - s.w() / 2.0, overview.width(), s.w());
        scrollY = clamp(f[1] - s.h() / 2.0, overview.height(), s.h());
    }
    private static double clamp(double v, int content, int view) {
        if (content <= view) return -(view - content) / 2.0;
        return Math.max(0, Math.min(content - view, v));
    }

    /** The chunk of the holder when it is in this tab's dimension, else null. */
    private Chunk hereChunk() { return data.dimension().equals(tab) ? new Chunk(data.chunkX(), data.chunkZ()) : null; }

    /** The chunk whose detail map is shown: the clicked one, or the only one of the dimension. */
    private Chunk shownDetail() {
        if (detail != null) return detail;
        var list = tiles.get(tab);
        return list != null && list.size() == 1 ? new Chunk(list.get(0).x(), list.get(0).z()) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (resultTicks > 0) resultTicks--;
        if (pending.containsKey(tab) && ++waited > RETRY_TICKS && retries < RETRIES) { retries++; request(); }
    }

    @Override
    public boolean isPauseScreen() { return false; }

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

    static Component gapLabel(int blocks, String dir) {
        return Component.translatable("simpletweaks.deed.gap", String.format(java.util.Locale.ROOT, "%,d", blocks).replace(',', ' '),
                Component.translatable("simpletweaks.deed.dir." + dir));
    }

    static Component age(int ticks) {
        if (ticks == DeedChunksPayload.NO_PICTURE) return Component.translatable("simpletweaks.deed.age.none");
        int minutes = ticks / 1200;
        if (minutes < 1) return Component.translatable("simpletweaks.deed.age.now");
        if (minutes < 60) return Component.translatable("simpletweaks.deed.age.minutes", minutes);
        if (minutes < 60 * 48) return Component.translatable("simpletweaks.deed.age.hours", minutes / 60);
        return Component.translatable("simpletweaks.deed.age.days", minutes / (60 * 24));
    }

    private static Component itemName(String id) {
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key))
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key).getName(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key)));
        return Component.literal(id);
    }

    /** "+2 chunks: 2x Diamond, 15 levels" or why nothing can be bought. */
    private Component buyText() {
        return switch (map.buy()) {
            case DeedMap.BUY -> {
                var costs = Component.empty();
                for (var c : map.nextCosts()) costs.append(Component.literal(c.count() + "x ")).append(itemName(c.id())).append(Component.literal(", "));
                costs.append(Component.translatable("simpletweaks.deed.levels", map.nextLevels()));
                yield Component.translatable("simpletweaks.deed.next", map.nextChunks() - data.maxChunks(), costs);
            }
            case DeedMap.MAX -> Component.translatable("command.simpletweaks.claim.buy.max");
            case DeedMap.LIMIT -> Component.translatable("command.simpletweaks.claim.buy.server_limit");
            default -> Component.translatable("command.simpletweaks.claim.buy.disabled");
        };
    }

    private Component resultText() {
        String r = map.result();
        if (r.endsWith(".ok") || r.startsWith("unclaim.")) return Component.translatable("simpletweaks.deed.result." + r);
        int dot = r.indexOf('.');
        return Component.translatable("command.simpletweaks.claim.buy." + (dot < 0 ? r : r.substring(dot + 1)));
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect p = layout.panel, s = layout.sheet;
        UiBoxes.box(g, p.x(), p.y(), p.w(), p.h(), palette());
        for (int i = 0; i < tabRects.size(); i++) {
            Rect t = tabRects.get(i);
            boolean on = tabList.get(tabOffset + i).dimension().equals(tab);
            boolean hover = t.contains(mouseX, mouseY);
            if (on) g.fill(t.x(), t.y(), t.right(), t.bottom() + 1, SHEET);
            else UiBoxes.raised(g, t.x(), t.y() + 2, t.w(), t.h() - 2, hover ? UiPalette.mix(TAB_IDLE, 0xFFFFFFFF, 0.25) : TAB_IDLE);
        }
        UiBoxes.insetColored(g, s.x(), s.y(), s.w(), s.h(), palette(), SHEET);
        for (int i = 0; i < tabRects.size(); i++) {
            Rect t = tabRects.get(i);
            var d = tabList.get(tabOffset + i);
            boolean on = d.dimension().equals(tab);
            if (on) g.fill(t.x() + 1, t.bottom(), t.right() - 1, t.bottom() + 1, SHEET); // the active tab opens into the sheet
            String name = font.plainSubstrByWidth(dimensionName(d.dimension()), t.w() - 6);
            g.text(font, name, t.x() + (t.w() - font.width(name)) / 2, t.y() + (on ? 4 : 6), on ? INK : d.chunks() > 0 ? DeedStyle.TAB_TEXT : DeedStyle.TAB_SOFT, false);
        }
        g.enableScissor(s.x() + 1, s.y() + 1, s.right() - 1, s.bottom() - 1);
        drawSheet(g, mouseX, mouseY);
        g.disableScissor();
    }

    private void drawSheet(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect s = layout.sheet;
        if (chunksHere() == 0) {
            g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.no_land_here"), s.x() + 8, s.y() + 8, s.w() - 16, INK, false);
            return;
        }
        if (!tiles.containsKey(tab)) {
            g.text(font, Component.translatable("simpletweaks.deed.loading"), s.x() + 8, s.y() + 8, INK_SOFT, false);
            return;
        }
        Chunk shown = shownDetail();
        if (shown != null) drawDetail(g, shown);
        else drawOverview(g, mouseX, mouseY);
        if (confirming && shown != null) drawConfirm(g, shown);
    }

    private int originX() { return layout.sheet.x() + 1 - (int) Math.round(scrollX); }
    private int originY() { return layout.sheet.y() + 1 - (int) Math.round(scrollY); }

    private void drawOverview(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (overview == null) return;
        int ox = originX(), oy = originY();
        for (var l : overview.links()) {
            dotted(g, ox + l.x1(), oy + l.y1(), ox + l.x2(), oy + l.y2());
            g.text(font, gapLabel(l.blocks(), l.direction()), ox + l.label().x(), oy + l.label().y() + 1, INK_SOFT, false);
        }
        Chunk here = hereChunk();
        var hovered = hoveredTile(mouseX, mouseY);
        for (var t : overview.tiles()) {
            int x = ox + t.x(), y = oy + t.y();
            UiBoxes.insetColored(g, x, y, DeedMapLayout.TILE - 2, DeedMapLayout.TILE - 2, sheetPalette(), UNKNOWN);
            picture(g, byChunk.get(t.chunk()), x + 1, y + 1, 2);
            if (t.chunk().equals(here)) frame(g, x - 1, y - 1, DeedMapLayout.TILE, INK_ERROR);
            if (hovered == t) frame(g, x, y, DeedMapLayout.TILE - 2, HOVER);
        }
    }

    /** Dotted line, 1 px dots every 3 px (Bresenham), skipping nothing: the layout keeps it between the groups. */
    private static void dotted(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1), dy = -Math.abs(y2 - y1), sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, err = dx + dy, step = 0;
        for (int guard = 0; guard < 4096; guard++) {
            if (step++ % 3 == 0) g.fill(x1, y1, x1 + 1, y1 + 1, INK_SOFT);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + 1, color); g.fill(x, y + size - 1, x + size, y + size, color);
        g.fill(x, y, x + 1, y + size, color); g.fill(x + size - 1, y, x + size, y + size, color);
    }

    /** 16x16 map pixels at {@code scale}; equal neighbours in a row are one fill. No picture: paper with a faint grid. */
    static void picture(GuiGraphicsExtractor g, DeedChunksPayload.Tile tile, int x, int y, int scale) {
        int size = 16 * scale;
        if (tile == null || !tile.hasPicture()) {
            g.fill(x, y, x + size, y + size, UNKNOWN);
            for (int i = 4; i < 16; i += 4) {
                g.fill(x + i * scale, y, x + i * scale + 1, y + size, UNKNOWN_LINE);
                g.fill(x, y + i * scale, x + size, y + i * scale + 1, UNKNOWN_LINE);
            }
            return;
        }
        byte[] p = tile.pixels();
        for (int row = 0; row < 16; row++) {
            int col = 0;
            while (col < 16) {
                int id = p[row * 16 + col] & 0xFF, end = col + 1;
                while (end < 16 && (p[row * 16 + end] & 0xFF) == id) end++;
                int argb = id == 0 ? UNKNOWN : 0xFF000000 | MapColor.getColorFromPackedId(id);
                g.fill(x + col * scale, y + row * scale, x + end * scale, y + (row + 1) * scale, argb);
                col = end;
            }
        }
    }

    private void drawDetail(GuiGraphicsExtractor g, Chunk c) {
        Rect m = layout.detailMap(), info = layout.detailInfo();
        // paper rim like a Vanilla map
        g.fill(m.x() - 3, m.y() - 3, m.right() + 3, m.bottom() + 3, UiPalette.scale(SHEET, 0.78));
        g.fill(m.x() - 2, m.y() - 2, m.right() + 2, m.bottom() + 2, UNKNOWN);
        picture(g, byChunk.get(c), m.x(), m.y(), layout.detailScale());
        if (c.equals(hereChunk())) frame(g, m.x() - 1, m.y() - 1, m.w() + 2, INK_ERROR);
        var tile = byChunk.get(c);
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("simpletweaks.deed.chunk", dimensionName(tab), c.x(), c.z()));
        lines.add(Component.translatable("simpletweaks.deed.blocks", c.x() * 16, c.x() * 16 + 15, c.z() * 16, c.z() * 16 + 15));
        lines.add(Component.translatable("simpletweaks.deed.as_of", age(tile == null ? DeedChunksPayload.NO_PICTURE : tile.age())));
        lines.add(Component.translatable("simpletweaks.deed.neighbours", side(c.x(), c.z() - 1), side(c.x() + 1, c.z()), side(c.x(), c.z() + 1), side(c.x() - 1, c.z())));
        if (c.equals(hereChunk())) lines.add(Component.translatable("simpletweaks.deed.you_here"));
        int y = info.y();
        for (var line : lines) {
            for (var part : font.split(line, info.w())) {
                if (y + 9 > layout.detailButton(0).y() - 2) break;
                g.text(font, part, info.x(), y, INK, false);
                y += 10;
            }
        }
    }
    private Component side(int x, int z) {
        return Component.translatable(byChunk.containsKey(new Chunk(x, z)) ? "simpletweaks.deed.side.own" : "simpletweaks.deed.side.other");
    }

    private void drawConfirm(GuiGraphicsExtractor g, Chunk c) {
        Rect r = layout.confirm();
        g.fill(layout.sheet.x() + 1, layout.sheet.y() + 1, layout.sheet.right() - 1, layout.sheet.bottom() - 1, 0x80303030);
        UiBoxes.box(g, r.x(), r.y(), r.w(), r.h(), palette());
        g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.unclaim.ask", c.x(), c.z()), r.x() + 8, r.y() + 8, r.w() - 16, DeedStyle.LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int label = palette().label();
        Component count = map.owns() ? Component.translatable("simpletweaks.deed.count", data.landChunks(), map.credits())
                : Component.translatable("simpletweaks.deed.count.other", data.landChunks());
        int cw = font.width(count);
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), layout.title.w() - cw - 6), layout.title.x(), layout.title.y(), label, false);
        g.text(font, count, layout.title.right() - cw, layout.title.y(), label, false);
        Component info = resultTicks > 0 && !map.result().isEmpty() ? resultText()
                : map.owns() ? (map.buy() == DeedMap.BUY ? Component.translatable("simpletweaks.deed.info.next", buyText()) : buyText())
                : Component.translatable("simpletweaks.deed.read.rights.short", rightsLetters(data.rights()));
        int infoColor = resultTicks > 0 && !map.result().isEmpty() && !map.result().endsWith(".ok") ? DeedStyle.ERROR : DeedStyle.SOFT;
        g.text(font, font.plainSubstrByWidth(info.getString(), layout.info.w()), layout.info.x(), layout.info.y(), infoColor, false);
        if (layout.showHint) {
            Rect h = layout.hint, x = layout.hintClose();
            g.text(font, font.plainSubstrByWidth(Component.translatable("simpletweaks.deed.maps_hint").getString(), h.w() - x.w() - 4), h.x(), h.y() + 1, DeedStyle.SOFT, false);
            g.text(font, "x", x.x() + 2, x.y() + 1, x.contains(mouseX, mouseY) ? DeedStyle.ERROR : DeedStyle.LABEL, false);
        }
        var t = confirming || shownDetail() != null ? null : hoveredTile(mouseX, mouseY);
        if (t != null) {
            var tile = byChunk.get(t.chunk());
            Component tip = Component.translatable("simpletweaks.deed.tile", t.chunk().x(), t.chunk().z(), t.chunk().x() * 16, t.chunk().x() * 16 + 15,
                    t.chunk().z() * 16, t.chunk().z() * 16 + 15, age(tile == null ? DeedChunksPayload.NO_PICTURE : tile.age()));
            g.setTooltipForNextFrame(font.split(tip, 200), mouseX, mouseY);
        }
    }

    private static String rightsLetters(int rights) {
        var out = new StringBuilder();
        String letters = com.simplebuilding.modules.simpletweaks.claims.ClaimRights.LETTERS;
        for (int i = 0; i < letters.length(); i++) if ((rights & (1 << i)) != 0) out.append(out.isEmpty() ? "" : " ").append(letters.charAt(i));
        return out.isEmpty() ? "-" : out.toString();
    }

    private DeedMapLayout.Tile hoveredTile(double mouseX, double mouseY) {
        if (overview == null || !layout.sheet.contains(mouseX, mouseY)) return null;
        return overview.at(mouseX - originX(), mouseY - originY());
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < tabRects.size(); i++) {
                if (!tabRects.get(i).contains(event.x(), event.y())) continue;
                String d = tabList.get(tabOffset + i).dimension();
                if (!d.equals(tab)) { tab = d; detail = null; confirming = false; retries = 0; rebuildWidgets(); centre(); }
                return true;
            }
            if (layout.showHint && layout.hintClose().contains(event.x(), event.y())) { DeedHint.hide(); rebuildWidgets(); return true; }
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0 && !confirming && shownDetail() == null && layout.sheet.contains(event.x(), event.y())) {
            dragFrom = new double[]{event.x(), event.y(), scrollX, scrollY};
            dragged = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragFrom != null && overview != null) {
            double mx = dragFrom[0] - event.x(), my = dragFrom[1] - event.y();
            if (Math.abs(mx) + Math.abs(my) > 3) dragged = true;
            scrollX = clamp(dragFrom[2] + mx, overview.width(), layout.sheet.w());
            scrollY = clamp(dragFrom[3] + my, overview.height(), layout.sheet.h());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragFrom != null) {
            boolean click = !dragged;
            dragFrom = null;
            if (click) {
                var t = hoveredTile(event.x(), event.y());
                if (t != null) { detail = t.chunk(); rebuildWidgets(); }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        if (overview != null && shownDetail() == null && layout.sheet.contains(mouseX, mouseY)) {
            scrollY = clamp(scrollY - scrollYDelta * 18, overview.height(), layout.sheet.h());
            scrollX = clamp(scrollX - scrollXDelta * 18, overview.width(), layout.sheet.w());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }
}
