package com.simplebuilding.modules.simpletweaks.table.client;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.table.ClaimGrid;
import com.simplebuilding.modules.simpletweaks.table.TableLayout;
import com.simplebuilding.modules.simpletweaks.table.TableService;
import com.simplebuilding.modules.simpletweaks.table.TableVisibility;
import com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableListPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

/**
 * The claim table: a chunk raster around the table (one color per owner, own land framed) next to the owner list. It
 * only shows what the server sent; every request is checked again by the server. No terrain (that is Simple Maps).
 */
public final class TableScreen extends Screen {
    private static final int PAPER = 0xFFE2D3AA, INK_SOFT = UiContrast.INK_SOFT, TEXT = 0xFF404040, FREE = 0xFFC9B98A, GRID = 0x22000000, TABLE = 0xFF202020, MINE = 0xFFFFFFFF, MINE_TEXT = UiContrast.INK_OK;

    private TableOpenPayload data;
    private TableLayout layout;
    private int zoom = 1, centerX, centerZ, page;
    private boolean onlyMine, dragged;
    private int[] dragStart;
    private TableAreaPayload area;
    private byte[] cells;
    private TableListPayload list;
    private Button prev, next, minus, plus, mineButton, expandButton;

    public TableScreen(TableOpenPayload data) {
        super(Component.translatable("simpletweaks.table.title"));
        this.data = data;
        this.centerX = data.chunkX(); this.centerZ = data.chunkZ();
    }

    private TableVisibility visibility() { return TableVisibility.byOrdinal(data.visibility()); }
    private boolean off() { return visibility() == TableVisibility.OFF; }
    private int cellsPerSide() { return TableLayout.ZOOM[zoom]; }
    private int minX() { return centerX - cellsPerSide() / 2; }
    private int minZ() { return centerZ - cellsPerSide() / 2; }

    private Button button(Component label, Rect r, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build());
    }

    @Override
    protected void init() {
        layout = TableLayout.of(width, height);
        minus = button(Component.literal("-"), layout.zoomOut(), b -> zoom(-1));
        plus = button(Component.literal("+"), layout.zoomIn(), b -> zoom(1));
        mineButton = button(Component.translatable("simpletweaks.table.mine"), layout.mine(), b -> { onlyMine = !onlyMine; page = 0; requestList(); });
        prev = button(Component.literal("<"), layout.prev(), b -> { if (page > 0) { page--; requestList(); } });
        next = button(Component.literal(">"), layout.next(), b -> { if (list != null && (page + 1) * TableListPayload.PAGE < list.total()) { page++; requestList(); } });
        button(CommonComponents.GUI_DONE, layout.close(), b -> onClose());
        expandButton = button(Component.translatable("simpletweaks.table.expand"), layout.expand(), b ->
                DeedNet.toServer.accept(new TableRequestPayload(data.pos(), TableRequestPayload.EXPAND, 0, 0, 0, 0)));
        showExpand();
        minus.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simpletweaks.table.zoom_out")));
        plus.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simpletweaks.table.zoom_in")));
        if (!off()) { requestArea(); requestList(); }
        else { for (var b : List.of(minus, plus, mineButton, prev, next)) b.active = false; }
    }

    /** Refreshed table data after a purchase: budget, next step; the map and the list stay as they are. */
    public boolean refresh(TableOpenPayload fresh) {
        if (!fresh.pos().equals(data.pos())) return false;
        data = fresh; showExpand();
        return true;
    }

    private static Component itemName(String id) {
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key))
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key).getName();
        return Component.literal(id);
    }

    /** The next step as text: "6 chunks: 8 Copper Ingot, 1 level". */
    private Component nextStep() {
        var costs = Component.empty();
        for (var c : data.nextCosts()) costs.append(Component.literal(c.count() + "x ")).append(itemName(c.id())).append(Component.literal(", "));
        costs.append(Component.translatable("simpletweaks.table.levels", data.nextLevels()));
        return Component.translatable("simpletweaks.table.next", data.nextChunks(), costs);
    }

    private void showExpand() {
        expandButton.active = data.state() == TableOpenPayload.BUY;
        expandButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(switch (data.state()) {
            case TableOpenPayload.BUY -> nextStep();
            case TableOpenPayload.MAX -> Component.translatable("simpletweaks.table.expand.max");
            case TableOpenPayload.LIMIT -> Component.translatable("simpletweaks.table.expand.limit");
            default -> Component.translatable("simpletweaks.table.expand.off");
        }));
    }

    private void zoom(int by) {
        int z = Math.max(0, Math.min(TableLayout.ZOOM.length - 1, zoom + by));
        if (z == zoom) return;
        zoom = z; clampCenter(); requestArea();
    }

    private void clampCenter() {
        int reach = TableService.RADIUS - cellsPerSide() / 2;
        centerX = Math.max(data.chunkX() - reach, Math.min(data.chunkX() + reach, centerX));
        centerZ = Math.max(data.chunkZ() - reach, Math.min(data.chunkZ() + reach, centerZ));
    }

    private void requestArea() {
        int n = cellsPerSide();
        DeedNet.toServer.accept(new TableRequestPayload(data.pos(), TableRequestPayload.AREA, minX(), minZ(), n, n));
    }
    private void requestList() { DeedNet.toServer.accept(new TableRequestPayload(data.pos(), TableRequestPayload.LIST, page, onlyMine ? 1 : 0, 0, 0)); }

    public void onArea(TableAreaPayload payload) {
        // Defensive: a hostile or stale answer must not crash the screen.
        byte[] decoded = ClaimGrid.decode(payload.cells(), payload.w() * payload.h());
        if (decoded == null || payload.w() < 1 || payload.h() < 1 || payload.w() > ClaimGrid.MAX_SIDE || payload.h() > ClaimGrid.MAX_SIDE) return;
        area = payload; cells = decoded;
    }
    public void onList(TableListPayload payload) { list = payload; page = payload.page(); }

    /** Palette index (1-based) of the chunk in the last answer, 0 = free or unknown. */
    private int index(int chunkX, int chunkZ) {
        if (area == null) return 0;
        int cx = chunkX - area.minX(), cz = chunkZ - area.minZ();
        if (cx < 0 || cz < 0 || cx >= area.w() || cz >= area.h()) return 0;
        return cells[cz * area.w() + cx] & 0xFF;
    }
    private TableAreaPayload.Entry entry(int index) { return index >= 1 && index <= area.palette().size() ? area.palette().get(index - 1) : null; }
    private Component nameOf(String name) { return name.isEmpty() ? Component.translatable("simpletweaks.table.foreign") : Component.literal(name); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect p = layout.panel;
        var palette = UiPalette.derived(PAPER);
        UiBoxes.box(g, p.x(), p.y(), p.w(), p.h(), palette);
        Rect m = layout.map, l = layout.list;
        UiBoxes.insetColored(g, m.x(), m.y(), m.w(), m.h(), palette, UiPalette.INVENTORY.fill());
        UiBoxes.insetColored(g, l.x(), l.y(), l.w(), l.h(), palette, UiPalette.INVENTORY.fill());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int label = UiPalette.derived(PAPER).label();
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), layout.title.w()), layout.title.x(), layout.title.y(), label, false);
        Component land = Component.translatable("simpletweaks.table.land", data.ownChunks(), data.maxChunks());
        if (data.state() == TableOpenPayload.BUY) land = Component.empty().append(land).append(Component.literal("  -  ")).append(nextStep());
        g.text(font, font.plainSubstrByWidth(land.getString(), layout.info.w()), layout.info.x(), layout.info.y(), INK_SOFT, false);

        if (off()) {
            g.textWithWordWrap(font, Component.translatable("simpletweaks.table.off"), layout.map.x() + 4, layout.map.y() + 4, layout.map.w() + layout.list.w(), TEXT, false);
            return;
        }
        drawMap(g, mouseX, mouseY);
        drawList(g, mouseX, mouseY);
    }

    private void drawMap(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int n = cellsPerSide(), size = layout.cell(n);
        Rect grid = layout.grid(n);
        g.fill(grid.x(), grid.y(), grid.right(), grid.bottom(), FREE);
        for (int cz = 0; cz < n; cz++) {
            for (int cx = 0; cx < n; cx++) {
                int x = grid.x() + cx * size, y = grid.y() + cz * size;
                int idx = index(minX() + cx, minZ() + cz);
                var e = idx == 0 || area == null ? null : entry(idx);
                if (e != null) {
                    g.fill(x, y, x + size, y + size, 0xFF000000 | e.color());
                    if (e.mine() && size >= 3) frame(g, x, y, size, MINE);
                } else if (size >= 5) frame(g, x, y, size, GRID);
            }
        }
        int tx = data.chunkX() - minX(), tz = data.chunkZ() - minZ();
        if (tx >= 0 && tz >= 0 && tx < n && tz < n) frame(g, grid.x() + tx * size, grid.y() + tz * size, size, TABLE);
        int[] at = layout.cellAt(mouseX, mouseY, n);
        Rect s = layout.status;
        if (at == null) return;
        int chunkX = minX() + at[0], chunkZ = minZ() + at[1];
        var e = entry(index(chunkX, chunkZ));
        Component line = e == null ? Component.translatable("simpletweaks.table.hover.free", chunkX, chunkZ)
                : Component.translatable("simpletweaks.table.hover.owner", chunkX, chunkZ, nameOf(e.name()));
        g.text(font, font.plainSubstrByWidth(line.getString(), s.w()), s.x(), s.y() + 1, TEXT, false);
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + 1, color); g.fill(x, y + size - 1, x + size, y + size, color);
        g.fill(x, y, x + 1, y + size, color); g.fill(x + size - 1, y, x + size, y + size, color);
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (list == null) return;
        int rows = Math.min(layout.rows(), list.rows().size());
        for (int i = 0; i < rows; i++) {
            var row = list.rows().get(i);
            Rect r = layout.row(i);
            if (r.contains(mouseX, mouseY) && row.chunkX() != TableListPayload.NONE) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x30FFFFFF);
            g.fill(r.x() + 1, r.y() + 2, r.x() + 6, r.y() + 7, 0xFF000000 | row.color());
            String count = String.valueOf(row.chunks());
            int cw = font.width(count);
            g.text(font, font.plainSubstrByWidth(nameOf(row.name()).getString(), r.w() - 12 - cw - 4), r.x() + 9, r.y() + 1, row.mine() ? MINE_TEXT : TEXT, false);
            g.text(font, count, r.right() - cw - 1, r.y() + 1, INK_SOFT, false);
        }
        int pages = Math.max(1, (list.total() + TableListPayload.PAGE - 1) / TableListPayload.PAGE);
        prev.active = page > 0; next.active = page + 1 < pages;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!off() && event.button() == 0) {
            if (layout.map.contains(event.x(), event.y())) { dragStart = new int[]{centerX, centerZ}; dragAnchor = new double[]{event.x(), event.y()}; dragged = false; return true; }
            if (list != null) {
                int rows = Math.min(layout.rows(), list.rows().size());
                for (int i = 0; i < rows; i++) {
                    var row = list.rows().get(i);
                    if (layout.row(i).contains(event.x(), event.y()) && row.chunkX() != TableListPayload.NONE) {
                        centerX = row.chunkX(); centerZ = row.chunkZ(); clampCenter(); requestArea();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragStart != null && dragAnchor != null) {
            int size = layout.cell(cellsPerSide());
            // Cumulative drag from the press point, in whole cells.
            double mx = event.x(), my = event.y();
            int shiftX = (int) Math.round((dragAnchor[0] - mx) / size), shiftZ = (int) Math.round((dragAnchor[1] - my) / size);
            centerX = dragStart[0] + shiftX; centerZ = dragStart[1] + shiftZ; clampCenter(); dragged = true;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    private double[] dragAnchor;

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragStart != null) {
            boolean moved = dragged; dragStart = null; dragAnchor = null; dragged = false;
            if (moved) requestArea();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!off() && layout.map.contains(mouseX, mouseY) && scrollY != 0) { zoom(scrollY > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
