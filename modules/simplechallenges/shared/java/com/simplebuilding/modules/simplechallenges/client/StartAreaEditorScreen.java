package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.preset.StartArea;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The start area editor (S3): the map of cells, painted with the mouse. Left drag adds a rectangle, right drag removes one, the middle
 * button moves the map, the wheel zooms. Cell (0,0) stays (the spawn is fixed at 0,0). Done turns the cells into the few rectangles
 * the preset file holds ({@link StartArea#cover}). The map logic lives in {@link StartArea}; this class is only mouse and drawing.
 */
public final class StartAreaEditorScreen extends Screen {
    private static final int PAD = 8, SIDE = 112;
    private final Screen parent;
    private final int cellSize;
    private final Consumer<List<int[]>> onDone;
    private final Set<Long> cells;
    private int panelX, panelY, panelW, panelH, mapX, mapY, mapW, mapH;
    private int px = 8;
    private double centerX, centerZ;
    private int squareR = 1;
    private boolean dragging, erasing, panning, tooMany;
    private int fromX, fromZ, toX, toZ;

    public StartAreaEditorScreen(Screen parent, int cellSize, List<int[]> start, Consumer<List<int[]>> onDone) {
        super(Component.translatable("simplechallenges.ui.area.title"));
        this.parent = parent;
        this.cellSize = cellSize;
        this.onDone = onDone;
        this.cells = StartArea.cells(start);
        int[] b = StartArea.bounds(cells);
        centerX = (b[0] + b[2] + 1) / 2.0;
        centerZ = (b[1] + b[3] + 1) / 2.0;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 12, 440);
        panelH = Math.min(height - 12, 290);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        mapX = panelX + PAD;
        mapY = panelY + 22;
        mapW = panelW - 3 * PAD - SIDE;
        mapH = panelH - 22 - PAD - 26;
        int sx = panelX + panelW - PAD - SIDE, by = panelY + panelH - PAD - 20;
        addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.area.grow"), b -> square(1)).bounds(sx, mapY + 60, SIDE / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.area.shrink"), b -> square(-1)).bounds(sx + SIDE / 2 + 2, mapY + 60, SIDE / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.area.clear"), b -> {
            cells.clear();
            cells.add(UnlockGrid.key(0, 0));
            squareR = 1;
        }).bounds(sx, mapY + 84, SIDE, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done()).bounds(sx, by - 24, SIDE, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(sx, by, SIDE, 20).build());
    }

    /** A centred square of 2r x 2r cells (the shape of the shipped presets), r grows or shrinks by one. */
    private void square(int delta) {
        squareR = Mth.clamp(squareR + delta, 1, 32);
        cells.clear();
        for (int x = -squareR; x < squareR; x++) for (int z = -squareR; z < squareR; z++) cells.add(UnlockGrid.key(x, z));
        centerX = 0;
        centerZ = 0;
    }

    private void done() {
        onDone.accept(StartArea.cover(cells));
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private int cols() {
        return mapW / px;
    }

    private int rows() {
        return mapH / px;
    }

    private int viewMinX() {
        return (int) Math.round(centerX) - cols() / 2;
    }

    private int viewMinZ() {
        return (int) Math.round(centerZ) - rows() / 2;
    }

    private boolean inMap(double x, double y) {
        return x >= mapX && y >= mapY && x < mapX + cols() * px && y < mapY + rows() * px;
    }

    private int cellX(double x) {
        return viewMinX() + (int) Math.floor((x - mapX) / px);
    }

    private int cellZ(double y) {
        return viewMinZ() + (int) Math.floor((y - mapY) / px);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inMap(event.x(), event.y())) {
            int b = event.button();
            if (b == 0 || b == 1) {
                dragging = true;
                erasing = b == 1;
                fromX = toX = cellX(event.x());
                fromZ = toZ = cellZ(event.y());
                tooMany = false;
                return true;
            }
            if (b == 2) {
                panning = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            toX = cellX(event.x());
            toZ = cellZ(event.y());
            return true;
        }
        if (panning) {
            centerX -= dx / px;
            centerZ -= dy / px;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            apply();
            return true;
        }
        panning = false;
        return super.mouseReleased(event);
    }

    private void apply() {
        Set<Long> next = new HashSet<>(cells);
        for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
            for (int z = Math.min(fromZ, toZ); z <= Math.max(fromZ, toZ); z++) {
                if (erasing) next.remove(UnlockGrid.key(x, z));
                else next.add(UnlockGrid.key(x, z));
            }
        }
        next.add(UnlockGrid.key(0, 0));
        tooMany = next.size() > UnlockMode.MAX_START_CELLS;
        if (!tooMany) {
            cells.clear();
            cells.addAll(next);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (inMap(x, y)) {
            px = Mth.clamp(px + (scrollY > 0 ? 1 : -1), 3, 20);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        UiBoxes.box(g, panelX, panelY, panelW, panelH, UiPalette.INVENTORY);
        g.text(font, title, panelX + PAD, panelY + 8, 0xFF404040, false);
        int cols = cols(), rows = rows(), vx = viewMinX(), vz = viewMinZ();
        g.fill(mapX - 1, mapY - 1, mapX + cols * px + 1, mapY + rows * px + 1, 0xFF1E1F23);
        for (int i = 0; i < cols; i++) {
            for (int k = 0; k < rows; k++) {
                int gap = px >= 5 ? 1 : 0;
                int x0 = mapX + i * px, y0 = mapY + k * px;
                g.fill(x0, y0, x0 + px - gap, y0 + px - gap, cells.contains(UnlockGrid.key(vx + i, vz + k)) ? PresetPreview.FREE : PresetPreview.LOCKED);
            }
        }
        // the origin cell (spawn) has a white frame; the rectangle being dragged a translucent one
        int ox = mapX + (0 - vx) * px, oz = mapY + (0 - vz) * px;
        if (ox >= mapX && oz >= mapY && ox < mapX + cols * px && oz < mapY + rows * px) g.outline(ox - 1, oz - 1, px + 1, px + 1, PresetPreview.ORIGIN);
        if (dragging) {
            int x0 = mapX + (Math.min(fromX, toX) - vx) * px, z0 = mapY + (Math.min(fromZ, toZ) - vz) * px;
            int x1 = mapX + (Math.max(fromX, toX) - vx + 1) * px, z1 = mapY + (Math.max(fromZ, toZ) - vz + 1) * px;
            g.fill(Math.max(x0, mapX), Math.max(z0, mapY), Math.min(x1, mapX + cols * px), Math.min(z1, mapY + rows * px), erasing ? 0x80E04040 : 0x80FFFFFF);
        }
        int sx = panelX + panelW - PAD - SIDE;
        int[] b = StartArea.bounds(cells);
        g.text(font, Component.translatable("simplechallenges.ui.area.count", cells.size(), UnlockMode.MAX_START_CELLS), sx, mapY, 0xFF404040, false);
        g.text(font, Component.translatable("simplechallenges.ui.area.size", (b[2] - b[0] + 1) * cellSize, (b[3] - b[1] + 1) * cellSize), sx, mapY + 11, 0xFF404040, false);
        g.text(font, Component.translatable("simplechallenges.ui.area.cell", cellSize), sx, mapY + 22, 0xFF404040, false);
        if (tooMany) g.text(font, Component.translatable("simplechallenges.ui.area.too_many"), sx, mapY + 44, 0xFF8B1A1A, false);
        g.textWithWordWrap(font, Component.translatable("simplechallenges.ui.area.help"), sx, mapY + 112, SIDE, 0xFF4A4A4A, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
}
