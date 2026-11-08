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
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.MapColor;

/**
 * The claim deed's map (WH26-9, owner 08.11.: variant A). Left two thirds: chunk management - the land of the chosen
 * dimension as a grid in its real order (touching chunks glue, separate claims 4 px apart, {@link DeedMapLayout#grid}),
 * "Claim here" and "Give up". Right third: the chosen chunk like a locator map - its picture, coordinates, the owner,
 * the picture's age, the credits badge. Tabs: Map / Buy / Rights on the left, one per dimension on the right. It only
 * shows what the server sent; every button is re-checked by the server ({@code DeedService.handleAction}).
 */
public final class DeedMapScreen extends DeedCardScreen {
    static final int UNKNOWN = 0xFFDCCFA8, UNKNOWN_LINE = 0xFFC9BA8E;
    private static final int RETRY_TICKS = 12, RETRIES = 3;

    private final Map<String,List<DeedChunksPayload.Tile>> tiles = new HashMap<>();
    private final Map<String,DeedChunksPayload.Tile[][]> pending = new HashMap<>();
    private Map<Chunk,DeedChunksPayload.Tile> byChunk = Map.of();
    private DeedMapLayout.Grid grid;
    private Chunk selected;
    private boolean confirming;
    private int zoom = -1;
    private double panX, panY;
    private double[] dragFrom;
    private boolean dragged;
    private int waited, retries;

    public DeedMapScreen(DeedOpenPayload data) {
        super(Component.translatable("simpletweaks.deed.title.owner", data.owner()), data);
    }

    @Override int page() { return 0; }

    @Override void pickDimension(String dimension) {
        if (dimension.equals(tab)) return;
        rememberTab(dimension);
        selected = null; confirming = false; retries = 0; zoom = -1; panX = panY = 0;
        rebuildWidgets();
    }

    private int chunksHere() { return map.chunksIn(tab); }
    private boolean hint() { return !map.mapsPresent() && DeedHint.shown(); }

    private Button button(Component label, Rect r, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build());
    }

    @Override
    protected void init() {
        super.init();
        rebuildGrid();
        if (map.owns() && !confirming) {
            boolean credit = map.credits() > 0;
            var claim = button(Component.translatable("simpletweaks.deed.claim_here"), layout.button(0), b -> act(DeedActionPayload.CLAIM_HERE, 0));
            claim.active = map.here() == DeedMap.HERE_FREE && credit;
            claim.setTooltip(Tooltip.create(map.here() == DeedMap.HERE_OWN ? Component.translatable("simpletweaks.deed.here.own")
                    : map.here() != DeedMap.HERE_FREE ? Component.translatable("simpletweaks.deed.here.no")
                    : credit ? Component.translatable("simpletweaks.deed.here.credit", map.credits()) : Component.translatable("simpletweaks.deed.here.no_credit")));
            var give = button(Component.translatable("simpletweaks.deed.unclaim"), layout.button(1), b -> { confirming = true; rebuildWidgets(); });
            give.active = selected != null;
            give.setTooltip(Tooltip.create(selected == null ? Component.translatable("simpletweaks.deed.unclaim.pick")
                    : Component.translatable("simpletweaks.deed.unclaim.tip", selected.x(), selected.z())));
        }
        if (confirming && selected != null) {
            Chunk c = selected;
            button(Component.translatable("simpletweaks.deed.unclaim.yes"), layout.confirmButton(0), b -> { confirming = false; act(DeedActionPayload.UNCLAIM, ChunkPos.pack(c.x(), c.z())); });
            button(CommonComponents.GUI_CANCEL, layout.confirmButton(1), b -> { confirming = false; rebuildWidgets(); });
        }
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

    /** A page of tiles; when all pages of a dimension are there, the grid is laid out. */
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
        if (payload.dimension().equals(tab)) { selected = null; rebuildWidgets(); }
    }

    /** Lays out the grid of the shown dimension; picks the holder's chunk (or the first) when nothing is chosen. */
    private void rebuildGrid() {
        var list = tiles.getOrDefault(tab, List.of());
        var chunks = new ArrayList<Chunk>();
        var index = new HashMap<Chunk,DeedChunksPayload.Tile>();
        for (var t : list) { var c = new Chunk(t.x(), t.z()); chunks.add(c); index.put(c, t); }
        byChunk = index;
        Rect a = layout.area;
        grid = DeedMapLayout.grid(chunks, a.w() - 2, a.h() - 2, zoom);
        if (selected != null && !index.containsKey(selected)) selected = null;
        if (selected == null && !chunks.isEmpty()) {
            Chunk here = hereChunk();
            selected = here != null && index.containsKey(here) ? here : grid.groups().get(0).chunks().get(0);
            centreOn(selected);
        }
        clampPan();
    }

    private void centreOn(Chunk c) {
        var t = grid.of(c);
        if (t == null) return;
        panX = t.x() + grid.cell() / 2.0 - layout.area.w() / 2.0;
        panY = t.y() + grid.cell() / 2.0 - layout.area.h() / 2.0;
        clampPan();
    }
    private void clampPan() {
        panX = clamp(panX, grid.width(), layout.area.w() - 2);
        panY = clamp(panY, grid.height(), layout.area.h() - 2);
    }
    private static double clamp(double v, int content, int view) {
        if (content <= view) return -(view - content) / 2.0;
        return Math.max(0, Math.min(content - view, v));
    }

    /** The chunk of the holder when it is in this tab's dimension, else null. */
    private Chunk hereChunk() { return data.dimension().equals(tab) ? new Chunk(data.chunkX(), data.chunkZ()) : null; }

    @Override
    public void tick() {
        super.tick();
        if (pending.containsKey(tab) && ++waited > RETRY_TICKS && retries < RETRIES) { retries++; request(); }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    void drawCard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect in = layout.inner, loc = layout.locator;
        // the locator third is a little darker; three fold lines: a third, two thirds, the edge of the locator
        g.fill(layout.fold2, in.y(), in.right(), in.bottom(), DeedStyle.PAPER_DARK);
        for (int x : new int[]{layout.fold1, layout.fold2}) {
            g.fill(x, in.y(), x + 1, in.bottom(), DeedStyle.CREASE);
            g.fill(x + 1, in.y(), x + 2, in.bottom(), UiPalette.mix(DeedStyle.PAPER, 0xFFFFFFFF, 0.35));
        }
        // management
        g.text(font, fit(getTitle().getString(), layout.title.w()), layout.title.x(), layout.title.y(), DeedStyle.P_INK, false);
        String sub = I18n.get("simpletweaks.deed.dimension_count", dimensionName(tab), chunksHere());
        g.text(font, fit(sub, layout.sub.w()), layout.sub.x(), layout.sub.y(), DeedStyle.P_SOFT, false);
        Rect a = layout.area;
        g.enableScissor(a.x(), a.y(), a.right(), a.bottom());
        drawGrid(g, mouseX, mouseY);
        g.disableScissor();
        if (confirming && selected != null) drawConfirm(g, selected);
        Component result = resultText();
        Component info = result != null ? result
                : !map.owns() ? Component.translatable("simpletweaks.deed.read.rights.short", rightsLetters(data.rights()))
                : chunksHere() > 0 ? Component.translatable("simpletweaks.deed.pick") : Component.translatable("simpletweaks.deed.no_land_here");
        int color = result == null ? DeedStyle.P_SOFT : resultOk() ? DeedStyle.P_OK : DeedStyle.P_ERROR;
        g.text(font, fit(info.getString(), layout.info.w()), layout.info.x(), layout.info.y(), color, false);
        // locator
        if (map.owns()) badge(g, layout.badge, map.credits());
        else {
            Rect b = layout.badge;
            UiBoxes.stairFrame(g, b.x(), b.y(), b.w(), b.h(), new int[]{DeedStyle.EDGE, DeedStyle.CREASE}, DeedStyle.CARD);
            g.text(font, fit(I18n.get("simpletweaks.deed.read.rights.short", rightsLetters(data.rights())), b.w() - 8), b.x() + 4, b.y() + 4, DeedStyle.P_INK, false);
        }
        drawLocator(g, mouseX, mouseY);
        if (hint()) {
            Rect x = layout.hintClose();
            var lines = font.split(Component.translatable("simpletweaks.deed.maps_hint"), loc.w() - 16);
            int y = x.y() - 10 * (Math.min(3, lines.size()) - 1);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.text(font, lines.get(i), loc.x() + 3, y + 10 * i, DeedStyle.P_SOFT, false);
            g.text(font, "x", x.x() + 2, x.y(), x.contains(mouseX, mouseY) ? DeedStyle.P_ERROR : DeedStyle.P_INK, false);
        }
    }

    private int originX() { return layout.area.x() + 1 - (int) Math.round(panX); }
    private int originY() { return layout.area.y() + 1 - (int) Math.round(panY); }

    private void drawGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect a = layout.area;
        if (chunksHere() == 0) return;
        if (!tiles.containsKey(tab)) { g.text(font, Component.translatable("simpletweaks.deed.loading"), a.x() + 4, a.y() + 4, DeedStyle.P_SOFT, false); return; }
        int ox = originX(), oy = originY(), c = grid.cell();
        var hovered = hoveredTile(mouseX, mouseY);
        for (var t : grid.tiles()) {
            int x = ox + t.x(), y = oy + t.y();
            var picture = byChunk.get(t.chunk());
            if (c == 16 && picture != null && picture.hasPicture()) picture(g, picture, x, y, 1);
            else g.fill(x, y, x + c, y + c, DeedStyle.GROUPS[t.group() % DeedStyle.GROUPS.length]);
            // edges: dark where the land ends, a light hairline between glued chunks
            Chunk k = t.chunk();
            boolean e = byChunk.containsKey(new Chunk(k.x() + 1, k.z())), s = byChunk.containsKey(new Chunk(k.x(), k.z() + 1));
            boolean w = byChunk.containsKey(new Chunk(k.x() - 1, k.z())), n = byChunk.containsKey(new Chunk(k.x(), k.z() - 1));
            int glue = UiPalette.mix(DeedStyle.PAPER, 0xFFFFFFFF, 0.3);
            g.fill(x + c - 1, y, x + c, y + c, e ? glue : DeedStyle.EDGE);
            g.fill(x, y + c - 1, x + c, y + c, s ? glue : DeedStyle.EDGE);
            if (!w) g.fill(x, y, x + 1, y + c, DeedStyle.EDGE);
            if (!n) g.fill(x, y, x + c, y + 1, DeedStyle.EDGE);
            if (k.equals(hereChunk())) { int d = Math.max(2, c / 4); g.fill(x + (c - d) / 2, y + (c - d) / 2, x + (c + d) / 2, y + (c + d) / 2, 0xFFFFFFFF); }
            if (k.equals(selected)) frame(g, x, y, c, 0xFFFFFFFF);
            else if (hovered == t) frame(g, x, y, c, UiPalette.mix(0xFFFFFFFF, DeedStyle.EDGE, 0.4));
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

    /** The right third: the chosen chunk like a locator map, then its facts. */
    private void drawLocator(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect card = layout.card, pic = layout.picture();
        UiBoxes.stairFrame(g, card.x(), card.y(), card.w(), card.h(), new int[]{DeedStyle.CREASE, UiPalette.mix(DeedStyle.CARD, DeedStyle.CREASE, 0.4)}, DeedStyle.CARD);
        Chunk c = selected;
        String head = c == null ? I18n.get("simpletweaks.deed.locator.none") : I18n.get("simpletweaks.deed.locator.chunk", c.x(), c.z());
        g.text(font, fit(head, card.w() - 8), card.x() + 4, card.y() + 4, DeedStyle.P_INK, false);
        g.fill(pic.x() - 1, pic.y() - 1, pic.right() + 1, pic.bottom() + 1, DeedStyle.CREASE);
        picture(g, c == null ? null : byChunk.get(c), pic.x(), pic.y(), layout.scale);
        if (c != null && c.equals(hereChunk())) pin(g, pic.x() + pic.w() / 2, pic.y() + pic.h() / 2);
        if (c == null) return;
        var tile = byChunk.get(c);
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("simpletweaks.deed.locator.owner", data.owner()));
        lines.add(Component.translatable("simpletweaks.deed.blocks", c.x() * 16, c.x() * 16 + 15, c.z() * 16, c.z() * 16 + 15));
        lines.add(Component.translatable("simpletweaks.deed.as_of", age(tile == null ? DeedChunksPayload.NO_PICTURE : tile.age())));
        if (c.equals(hereChunk())) lines.add(Component.translatable("simpletweaks.deed.you_here"));
        Rect d = layout.details;
        int y = d.y(), limit = d.bottom() - (hint() ? 30 : 0);
        for (var line : lines) for (var part : font.split(line, d.w())) {
            if (y + 9 > limit) return;
            g.text(font, part, d.x(), y, DeedStyle.P_INK, false);
            y += 10;
        }
    }

    /** Red map pin like the locator map's marker (8 x 11). */
    private static void pin(GuiGraphicsExtractor g, int cx, int cy) {
        int x = cx - 4, y = cy - 11;
        g.fill(x, y, x + 8, y + 8, 0xFFCC3B2B);
        g.fill(x + 2, y + 2, x + 6, y + 6, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + 5, y + 5, 0xFFCC3B2B);
        g.fill(x + 3, y + 8, x + 5, y + 11, 0xFFCC3B2B);
    }

    private void drawConfirm(GuiGraphicsExtractor g, Chunk c) {
        Rect r = layout.confirm();
        UiBoxes.stairFrame(g, r.x(), r.y(), r.w(), r.h(), new int[]{DeedStyle.EDGE, DeedStyle.CREASE}, DeedStyle.CARD);
        g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.unclaim.ask", c.x(), c.z()), r.x() + 6, r.y() + 5, r.w() - 12, DeedStyle.P_INK, false);
    }

    static Component age(int ticks) {
        if (ticks == DeedChunksPayload.NO_PICTURE) return Component.translatable("simpletweaks.deed.age.none");
        int minutes = ticks / 1200;
        if (minutes < 1) return Component.translatable("simpletweaks.deed.age.now");
        if (minutes < 60) return Component.translatable("simpletweaks.deed.age.minutes", minutes);
        if (minutes < 60 * 48) return Component.translatable("simpletweaks.deed.age.hours", minutes / 60);
        return Component.translatable("simpletweaks.deed.age.days", minutes / (60 * 24));
    }

    static String rightsLetters(int rights) {
        var out = new StringBuilder();
        String letters = com.simplebuilding.modules.simpletweaks.claims.ClaimRights.LETTERS;
        for (int i = 0; i < letters.length(); i++) if ((rights & (1 << i)) != 0) out.append(out.isEmpty() ? "" : " ").append(letters.charAt(i));
        return out.isEmpty() ? "-" : out.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        var t = confirming ? null : hoveredTile(mouseX, mouseY);
        if (t != null) {
            var tile = byChunk.get(t.chunk());
            Component tip = Component.translatable("simpletweaks.deed.tile", t.chunk().x(), t.chunk().z(), t.chunk().x() * 16, t.chunk().x() * 16 + 15,
                    t.chunk().z() * 16, t.chunk().z() * 16 + 15, age(tile == null ? DeedChunksPayload.NO_PICTURE : tile.age()));
            g.setTooltipForNextFrame(font.split(tip, 200), mouseX, mouseY);
        }
    }

    private DeedMapLayout.Tile hoveredTile(double mouseX, double mouseY) {
        if (grid == null || !tiles.containsKey(tab) || !layout.area.contains(mouseX, mouseY)) return null;
        return grid.at(mouseX - originX(), mouseY - originY());
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && hint() && layout.hintClose().contains(event.x(), event.y())) {
            DeedHint.hide(); rebuildWidgets(); return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && !confirming && layout.area.contains(event.x(), event.y())) {
            dragFrom = new double[]{event.x(), event.y(), panX, panY};
            dragged = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragFrom != null && grid != null) {
            double mx = dragFrom[0] - event.x(), my = dragFrom[1] - event.y();
            if (Math.abs(mx) + Math.abs(my) > 3) dragged = true;
            panX = dragFrom[2] + mx; panY = dragFrom[3] + my;
            clampPan();
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
                if (t != null && !t.chunk().equals(selected)) { selected = t.chunk(); rebuildWidgets(); }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    /** The wheel over the grid zooms (cell 16 / 12 / 9 / 6 / 4 px), keeping the chosen chunk in view. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        if (grid != null && !confirming && layout.area.contains(mouseX, mouseY) && scrollYDelta != 0) {
            int now = 0;
            for (int i = 0; i < DeedMapLayout.ZOOMS.length; i++) if (DeedMapLayout.ZOOMS[i] == grid.cell()) now = i;
            zoom = Math.max(0, Math.min(DeedMapLayout.ZOOMS.length - 1, now + (scrollYDelta > 0 ? -1 : 1)));
            rebuildGrid();
            if (selected != null) centreOn(selected);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }
}
