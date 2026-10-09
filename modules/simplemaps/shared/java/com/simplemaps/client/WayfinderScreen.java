package com.simplemaps.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContextMenu;
import com.simplelib.api.client.ui.UiPalette;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.net.FrameViewPayload;
import com.simplemaps.net.WaypointEditPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The wayfinder map screen (N18 plan, part B): a 4:3 map area whose 16 px grid always fits (12 x 9 cells), drag to
 * pan, wheel or +/- to zoom (1, 4, 8, 16, 64 blocks per pixel). Left bookmarks: snap, grid, zoom, height lines.
 * Right bookmarks: player and waypoints 1-8 (click centres). Right-click on the map: "create waypoint" → every
 * bookmark except the eight waypoints is greyed out; a free one is taken at once, an occupied one turns red first
 * and is replaced by a second click. Right-click on a waypoint bookmark: configure (name, colour or mob head),
 * delete. Opened from an item frame (Feature 4) the waypoints are read-only and the view is stored in the frame.
 */
public final class WayfinderScreen extends Screen {
    public static final int CELL = 16, COLS = 12, ROWS = 9, MAP_W = COLS * CELL, MAP_H = ROWS * CELL;
    public static final int FRAME = 8, TITLE_H = 12, FOOTER_H = 12;
    public static final int PANEL_W = MAP_W + 2 * FRAME, PANEL_H = TITLE_H + MAP_H + FOOTER_H + 2 * FRAME;
    public static final int TAB_W = 24, TAB_H = 16, TAB_OUT = 20;
    public static final int LEFT_TABS = 4, RIGHT_TABS = 1 + Waypoint.SLOTS;
    /** Parchment brown box (the map's colour); derived like every other N12 box. */
    public static final UiPalette PALETTE = UiPalette.derived(0xFFB08A57);
    private static final Identifier PLAYER = Identifier.withDefaultNamespace("player");
    private static final Identifier DEATH = Identifier.withDefaultNamespace("red_x");

    // Remembered for the session, like the recipe book's state.
    private static boolean snap = true, grid = true, contour;
    private static int zoomIndex;

    private final InteractionHand hand;
    private final int frameId;
    private final UiContextMenu menu = new UiContextMenu(PALETTE);
    private int left, top, mapX, mapY;
    /** Top-left of the map area in blocks. */
    private double originX, originZ;
    private boolean placed, dragging, moved;
    private double dragStartX, dragStartY;
    /** Waypoint creation: the target block, and the occupied slot that was clicked once (-1 = none). */
    private boolean picking;
    private int pickX, pickZ, confirmSlot = -1;
    /** Configure dialog. */
    private int editSlot = -1, editColor;
    private Optional<Identifier> editHead = Optional.empty();
    private EditBox nameBox;
    private final List<net.minecraft.client.gui.components.AbstractWidget> dialogWidgets = new ArrayList<>();

    private WayfinderScreen(InteractionHand hand, int frameId) {
        super(Component.translatable("screen.simplemaps.wayfinder"));
        this.hand = hand;
        this.frameId = frameId;
    }

    public static WayfinderScreen forHand(InteractionHand hand) {
        return new WayfinderScreen(hand, -1);
    }

    public static WayfinderScreen forFrame(int entityId) {
        return new WayfinderScreen(null, entityId);
    }

    private boolean frameMode() {
        return hand == null;
    }

    private ItemFrame frame() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(frameId) instanceof ItemFrame f ? f : null;
    }

    /** The live stack (waypoints change when the server confirms an edit). */
    private ItemStack stack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        if (!frameMode()) return mc.player.getItemInHand(hand);
        ItemFrame f = frame();
        return f == null ? ItemStack.EMPTY : f.getItem();
    }

    private int mapId() {
        MapsComponents.MapRef ref = stack().get(MapsComponents.MAP_ID);
        return ref == null ? -1 : ref.id();
    }

    private Waypoints waypoints() {
        return stack().getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY);
    }

    /** Client smoke: set the remembered bookmark states before opening the screen. */
    public static void setModes(boolean snapOn, boolean gridOn, boolean contourOn, int zoomLevel) {
        snap = snapOn;
        grid = gridOn;
        contour = contourOn;
        zoomIndex = Mth.clamp(zoomLevel, 0, WayfinderData.ZOOMS.length - 1);
    }

    /** Client smoke: screen position of the map area's centre. */
    public int mapCenterX() {
        return mapX + MAP_W / 2;
    }

    public int mapCenterY() {
        return mapY + MAP_H / 2;
    }

    public static int zoom() {
        return WayfinderData.ZOOMS[zoomIndex];
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        mapX = left + FRAME;
        mapY = top + FRAME + TITLE_H;
        if (!placed) {
            placed = true;
            MapsComponents.View view = frameMode() ? stack().get(MapsComponents.VIEW) : null;
            if (view != null) {
                zoomIndex = indexOf(view.zoom());
                centerOn(view.x(), view.z(), false);
            } else if (frameMode() && frame() != null) {
                centerOn(frame().getBlockX(), frame().getBlockZ(), false);
            } else if (minecraft != null && minecraft.player != null) {
                centerOnPlayer();
            }
        }
        if (editSlot >= 0) openDialog(editSlot);
    }

    private static int indexOf(int zoom) {
        for (int i = 0; i < WayfinderData.ZOOMS.length; i++) if (WayfinderData.ZOOMS[i] == zoom) return i;
        return 0;
    }

    // ---------------------------------------------------------------- view maths

    private void centerOn(double x, double z, boolean snapToCell) {
        int bpp = zoom();
        originX = x - MAP_W / 2.0 * bpp;
        originZ = z - MAP_H / 2.0 * bpp;
        if (snapToCell) snapOrigin();
    }

    /** "Player": with snap the player's grid cell is centred, not the player (owner N18). */
    private void centerOnPlayer() {
        if (minecraft == null || minecraft.player == null) return;
        int bpp = zoom(), cell = CELL * bpp;
        if (snap) {
            originX = (Math.floorDiv(minecraft.player.getBlockX(), cell) - COLS / 2) * (double) cell;
            originZ = (Math.floorDiv(minecraft.player.getBlockZ(), cell) - ROWS / 2) * (double) cell;
        } else {
            centerOn(minecraft.player.getX(), minecraft.player.getZ(), false);
        }
    }

    private void snapOrigin() {
        double cell = CELL * zoom();
        originX = Math.round(originX / cell) * cell;
        originZ = Math.round(originZ / cell) * cell;
    }

    /** Map pixel (at the current zoom) of the area's left/top edge. */
    private long originPx() {
        return (long) Math.floor(originX / zoom());
    }

    private long originPz() {
        return (long) Math.floor(originZ / zoom());
    }

    private int screenX(double blockX) {
        return mapX + (int) (Math.floor(blockX / zoom()) - originPx());
    }

    private int screenY(double blockZ) {
        return mapY + (int) (Math.floor(blockZ / zoom()) - originPz());
    }

    private int blockX(double sx) {
        return (int) Math.floor((originPx() + Math.floor(sx - mapX)) * zoom());
    }

    private int blockZ(double sy) {
        return (int) Math.floor((originPz() + Math.floor(sy - mapY)) * zoom());
    }

    private boolean overMap(double x, double y) {
        return x >= mapX && x < mapX + MAP_W && y >= mapY && y < mapY + MAP_H;
    }

    private void zoomBy(int steps, double anchorX, double anchorY) {
        int next = Mth.clamp(zoomIndex + steps, 0, WayfinderData.ZOOMS.length - 1);
        if (next == zoomIndex) return;
        double ax = Mth.clamp(anchorX - mapX, 0, MAP_W), ay = Mth.clamp(anchorY - mapY, 0, MAP_H);
        double bx = originX + ax * zoom(), bz = originZ + ay * zoom();
        zoomIndex = next;
        originX = bx - ax * zoom();
        originZ = bz - ay * zoom();
        if (snap) snapOrigin();
    }

    // ---------------------------------------------------------------- tabs

    private int leftTabY(int i) {
        return mapY + i * (TAB_H + 2);
    }

    private int rightTabY(int i) {
        return mapY + i * TAB_H;
    }

    private int tabAt(double x, double y, boolean leftSide) {
        int count = leftSide ? LEFT_TABS : RIGHT_TABS;
        int tx = leftSide ? left - TAB_OUT : left + PANEL_W - (TAB_W - TAB_OUT);
        if (x < tx || x >= tx + TAB_W) return -1;
        for (int i = 0; i < count; i++) {
            int ty = leftSide ? leftTabY(i) : rightTabY(i);
            if (y >= ty && y < ty + TAB_H) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        ItemStack stack = stack();
        int mapId = mapId();
        Waypoints waypoints = waypoints();
        boolean dialog = editSlot >= 0;
        drawTabs(g, mouseX, mouseY, waypoints, dialog);
        UiBoxes.box(g, left, top, PANEL_W, PANEL_H, PALETTE);
        g.text(font, stack.isEmpty() ? title : stack.getHoverName(), left + FRAME, top + FRAME + 1, PALETTE.label(), false);
        // Map area: sunk dark field, then the explored tiles.
        g.fill(mapX - 1, mapY - 1, mapX + MAP_W + 1, mapY + MAP_H + 1, UiBoxes.RIM);
        g.fill(mapX, mapY, mapX + MAP_W, mapY + MAP_H, 0xFF2C2A25);
        g.enableScissor(mapX, mapY, mapX + MAP_W, mapY + MAP_H);
        if (mapId >= 0) {
            drawTiles(g, mapId);
            if (grid) drawGrid(g);
            drawMarks(g, waypoints);
        }
        g.disableScissor();
        Component status = status(mapId);
        if (status != null) g.centeredText(font, status, mapX + MAP_W / 2, mapY + MAP_H / 2 - 4, 0xFFE8E2D0);
        drawFooter(g, mouseX, mouseY);
        if (picking) {
            UiBookmarks.greyOut(g, left, top, PANEL_W, PANEL_H);
            for (int i = 0; i < LEFT_TABS; i++) UiBookmarks.greyOut(g, left - TAB_OUT, leftTabY(i), TAB_OUT, TAB_H);
            UiBookmarks.greyOut(g, left + PANEL_W, rightTabY(0), TAB_OUT, TAB_H);
            g.centeredText(font, Component.translatable("screen.simplemaps.pick_slot"), mapX + MAP_W / 2, mapY + MAP_H / 2 - 4, 0xFFFFFFFF);
        }
        if (dialog) drawDialog(g);
        super.extractRenderState(g, mouseX, mouseY, a);
        menu.render(g, font, mouseX, mouseY);
        if (!dialog && !menu.isOpen()) tabTooltips(g, mouseX, mouseY, waypoints);
    }

    private Component status(int mapId) {
        if (mapId < 0) return Component.translatable("screen.simplemaps.status.new");
        if (!frameMode() && !MapsClient.inMapDimension(mapId)) return Component.translatable("screen.simplemaps.status.other_dimension");
        return null;
    }

    private void drawTiles(GuiGraphicsExtractor g, int mapId) {
        int bpp = zoom();
        long px0 = originPx(), pz0 = originPz();
        int tx0 = (int) Math.floorDiv(px0, WayfinderData.TILE), tz0 = (int) Math.floorDiv(pz0, WayfinderData.TILE);
        int tx1 = (int) Math.floorDiv(px0 + MAP_W - 1, WayfinderData.TILE), tz1 = (int) Math.floorDiv(pz0 + MAP_H - 1, WayfinderData.TILE);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                TileCache.Entry e = MapsClient.CACHE.want(new TileCache.Key(mapId, bpp, tx, tz));
                DynamicTexture texture = contour ? MapsClient.CACHE.contour(e) : MapsClient.CACHE.texture(e);
                if (texture == null) continue;
                int x = mapX + (int) ((long) tx * WayfinderData.TILE - px0), y = mapY + (int) ((long) tz * WayfinderData.TILE - pz0);
                g.blit(texture.getTextureView(), texture.getSampler(), x, y, x + WayfinderData.TILE, y + WayfinderData.TILE, 0, 1, 0, 1);
            }
        }
    }

    /** World-aligned lines every 16 map pixels (one chunk at 1:1). */
    private void drawGrid(GuiGraphicsExtractor g) {
        long px0 = originPx(), pz0 = originPz();
        for (int x = (int) Math.floorMod(-px0, CELL); x < MAP_W; x += CELL) g.fill(mapX + x, mapY, mapX + x + 1, mapY + MAP_H, 0x40000000);
        for (int y = (int) Math.floorMod(-pz0, CELL); y < MAP_H; y += CELL) g.fill(mapX, mapY + y, mapX + MAP_W, mapY + y + 1, 0x40000000);
    }

    private void drawMarks(GuiGraphicsExtractor g, Waypoints waypoints) {
        for (Waypoint w : waypoints.list()) {
            int x = screenX(w.x()), y = screenY(w.z());
            Optional<Item> head = w.head().flatMap(BuiltInRegistries.ITEM::getOptional);
            if (head.isPresent()) {
                g.pose().pushMatrix();
                g.pose().translate(x - 5, y - 5);
                g.pose().scale(10 / 16.0F, 10 / 16.0F);
                g.item(new ItemStack(head.get()), 0, 0);
                g.pose().popMatrix();
            } else {
                g.fill(x - 3, y - 3, x + 4, y + 4, UiBoxes.RIM);
                g.fill(x - 2, y - 2, x + 3, y + 3, w.color());
            }
            g.text(font, String.valueOf(w.slot() + 1), x + 4, y - 9, 0xFFFFFFFF, true);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || frameMode() && !MapsClient.inMapDimension(mapId())) return;
        var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        if (MapsClient.inMapDimension(mapId())) {
            int x = screenX(mc.player.getX()), y = screenY(mc.player.getZ());
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().rotate((float) Math.toRadians(mc.player.getYRot() + 180.0F));
            g.blitSprite(RenderPipelines.GUI_TEXTURED, atlas.getSprite(PLAYER), -4, -4, 8, 8);
            g.pose().popMatrix();
        }
        // Feature 7: shown even over unexplored ground; only the point appears there.
        if (!frameMode()) {
            Optional<GlobalPos> death = MapsClient.deathPoint(mc.player, hand);
            death.ifPresent(pos -> g.blitSprite(RenderPipelines.GUI_TEXTURED, atlas.getSprite(DEATH),
                    screenX(pos.pos().getX()) - 4, screenY(pos.pos().getZ()) - 4, 8, 8));
        }
    }

    private void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = mapY + MAP_H + 3;
        g.text(font, Component.translatable("screen.simplemaps.zoom", zoom()), mapX, y, PALETTE.label(), false);
        if (overMap(mouseX, mouseY)) {
            String pos = blockX(mouseX) + ", " + blockZ(mouseY);
            g.text(font, pos, mapX + MAP_W - font.width(pos), y, PALETTE.label(), false);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY, Waypoints waypoints, boolean dialog) {
        boolean[] states = {snap, grid, false, contour};
        for (int i = 0; i < LEFT_TABS; i++) {
            int x = left - TAB_OUT, y = leftTabY(i);
            boolean hover = !dialog && tabAt(mouseX, mouseY, true) == i;
            UiBookmarks.tab(g, x, y, TAB_W, TAB_H, true, PALETTE, states[i], hover);
            drawLeftIcon(g, i, x + 3, y + 3);
        }
        for (int i = 0; i < RIGHT_TABS; i++) {
            int x = left + PANEL_W - (TAB_W - TAB_OUT), y = rightTabY(i);
            boolean hover = !dialog && tabAt(mouseX, mouseY, false) == i;
            Optional<Waypoint> w = i == 0 ? Optional.empty() : waypoints.get(i - 1);
            UiBookmarks.tab(g, x, y, TAB_W, TAB_H, false, PALETTE, w.isPresent(), hover);
            int ix = x + TAB_W - 15;
            if (i == 0) {
                g.item(new ItemStack(net.minecraft.world.item.Items.COMPASS), ix - 2, y);
            } else if (w.isPresent() && w.get().head().isPresent()) {
                g.pose().pushMatrix();
                g.pose().translate(ix, y + 3);
                g.pose().scale(10 / 16.0F, 10 / 16.0F);
                BuiltInRegistries.ITEM.getOptional(w.get().head().get()).ifPresent(item -> g.item(new ItemStack(item), 0, 0));
                g.pose().popMatrix();
            } else {
                g.fill(ix, y + 4, ix + 8, y + 12, UiBoxes.RIM);
                g.fill(ix + 1, y + 5, ix + 7, y + 11, w.map(Waypoint::color).orElse(0xFF8A7656));
            }
            g.text(font, String.valueOf(i == 0 ? "" : i), ix + 9, y + 4, PALETTE.label(), false);
            if (picking && confirmSlot == i - 1 && i > 0) UiBookmarks.confirm(g, x, y, TAB_W, TAB_H);
        }
    }

    /** Engraved 10x10 icons: magnet (snap), grid, magnifier (zoom), height lines. */
    private void drawLeftIcon(GuiGraphicsExtractor g, int i, int x, int y) {
        int c = PALETTE.label();
        switch (i) {
            case 0 -> {
                g.fill(x + 1, y + 1, x + 4, y + 8, c);
                g.fill(x + 7, y + 1, x + 10, y + 8, c);
                g.fill(x + 1, y + 7, x + 10, y + 10, c);
                g.fill(x + 1, y + 1, x + 4, y + 3, 0xFFC0392B);
                g.fill(x + 7, y + 1, x + 10, y + 3, 0xFFC0392B);
            }
            case 1 -> {
                for (int k = 0; k <= 9; k += 3) {
                    g.fill(x + k, y, x + k + 1, y + 10, c);
                    g.fill(x, y + k, x + 10, y + k + 1, c);
                }
            }
            case 2 -> {
                String label = "1:" + zoom();
                g.pose().pushMatrix();
                g.pose().translate(x - 1, y + 1);
                g.pose().scale(0.75F, 0.75F);
                g.text(font, label, 0, 0, c, false);
                g.pose().popMatrix();
            }
            default -> {
                g.fill(x, y + 2, x + 10, y + 3, c);
                g.fill(x + 1, y + 5, x + 9, y + 6, c);
                g.fill(x + 3, y + 8, x + 7, y + 9, c);
            }
        }
    }

    private void tabTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY, Waypoints waypoints) {
        int l = tabAt(mouseX, mouseY, true), r = tabAt(mouseX, mouseY, false);
        String[] keys = {"snap", "grid", "zoom", "contour"};
        if (l >= 0) g.setTooltipForNextFrame(font, Component.translatable("screen.simplemaps.tab." + keys[l]), mouseX, mouseY);
        if (r == 0) g.setTooltipForNextFrame(font, Component.translatable("screen.simplemaps.tab.player"), mouseX, mouseY);
        if (r > 0) {
            Optional<Waypoint> w = waypoints.get(r - 1);
            Component text = w.map(p -> p.name().isEmpty()
                            ? Component.translatable("screen.simplemaps.waypoint", p.slot() + 1)
                            : Component.literal(p.name()))
                    .map(c -> (Component) Component.empty().append(c).append(" (" + w.get().x() + ", " + w.get().z() + ")"))
                    .orElse(Component.translatable("screen.simplemaps.waypoint.empty", r));
            g.setTooltipForNextFrame(font, text, mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- configure dialog

    private static final int DIALOG_W = 180, DIALOG_H = 104;

    private int dialogX() {
        return (width - DIALOG_W) / 2;
    }

    private int dialogY() {
        return (height - DIALOG_H) / 2;
    }

    private void openDialog(int slot) {
        Optional<Waypoint> w = waypoints().get(slot);
        if (w.isEmpty()) return;
        boolean reopen = editSlot == slot && nameBox != null;
        String name = reopen ? nameBox.getValue() : w.get().name();
        if (!reopen) {
            editColor = w.get().color();
            editHead = w.get().head();
        }
        closeDialogWidgets();
        editSlot = slot;
        int x = dialogX(), y = dialogY();
        nameBox = new EditBox(font, x + 8, y + 18, DIALOG_W - 16, 16, Component.translatable("screen.simplemaps.name"));
        nameBox.setMaxLength(Waypoint.MAX_NAME);
        nameBox.setValue(name);
        add(nameBox);
        setFocused(nameBox);
        add(Button.builder(Component.translatable("gui.done"), b -> saveDialog()).bounds(x + 8, y + DIALOG_H - 26, 78, 18).build());
        add(Button.builder(Component.translatable("gui.cancel"), b -> closeDialog()).bounds(x + DIALOG_W - 86, y + DIALOG_H - 26, 78, 18).build());
    }

    private void add(net.minecraft.client.gui.components.AbstractWidget widget) {
        dialogWidgets.add(widget);
        addRenderableWidget(widget);
    }

    private void closeDialogWidgets() {
        dialogWidgets.forEach(this::removeWidget);
        dialogWidgets.clear();
    }

    private void closeDialog() {
        closeDialogWidgets();
        editSlot = -1;
        nameBox = null;
    }

    private void saveDialog() {
        Optional<Waypoint> w = waypoints().get(editSlot);
        if (w.isPresent() && nameBox != null) {
            send(new Waypoint(editSlot, w.get().x(), w.get().z(), nameBox.getValue().trim(), editColor, editHead), false);
        }
        closeDialog();
    }

    private List<Item> heads() {
        List<Item> out = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(MapsItems.WAYPOINT_HEADS).forEach(h -> out.add(h.value()));
        return out;
    }

    private void drawDialog(GuiGraphicsExtractor g) {
        int x = dialogX(), y = dialogY();
        g.fill(0, 0, width, height, 0x60000000);
        UiBoxes.box(g, x, y, DIALOG_W, DIALOG_H, PALETTE);
        g.text(font, Component.translatable("screen.simplemaps.configure", editSlot + 1), x + 8, y + 7, PALETTE.label(), false);
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            int sx = x + 8 + i * 10, sy = y + 40;
            int c = 0xFF000000 | colors[i].getTextColor();
            boolean selected = editHead.isEmpty() && (editColor & 0xFFFFFF) == (c & 0xFFFFFF);
            g.fill(sx, sy, sx + 9, sy + 9, selected ? 0xFFFFFFFF : UiBoxes.RIM);
            g.fill(sx + 1, sy + 1, sx + 8, sy + 8, c);
        }
        List<Item> heads = heads();
        for (int i = 0; i < heads.size(); i++) {
            int sx = x + 8 + i * 18, sy = y + 54;
            boolean selected = editHead.isPresent() && editHead.get().equals(BuiltInRegistries.ITEM.getKey(heads.get(i)));
            UiBoxes.slot(g, sx, sy, PALETTE);
            if (selected) g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            g.item(new ItemStack(heads.get(i)), sx, sy);
        }
    }

    private boolean dialogClick(double mx, double my) {
        int x = dialogX(), y = dialogY();
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            int sx = x + 8 + i * 10, sy = y + 40;
            if (mx >= sx && mx < sx + 9 && my >= sy && my < sy + 9) {
                editColor = 0xFF000000 | colors[i].getTextColor();
                editHead = Optional.empty();
                return true;
            }
        }
        List<Item> heads = heads();
        for (int i = 0; i < heads.size(); i++) {
            int sx = x + 8 + i * 18, sy = y + 54;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                Identifier id = BuiltInRegistries.ITEM.getKey(heads.get(i));
                editHead = editHead.isPresent() && editHead.get().equals(id) ? Optional.empty() : Optional.of(id);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- input

    private void send(Waypoint waypoint, boolean delete) {
        if (frameMode()) return;
        SimpleMaps.toServer.accept(new WaypointEditPayload(hand == InteractionHand.MAIN_HAND ? 0 : 1, delete, waypoint));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (menu.mouseClicked(mx, my)) return true;
        if (editSlot >= 0) {
            if (dialogClick(mx, my)) return true;
            return super.mouseClicked(event, doubleClick);
        }
        int r = tabAt(mx, my, false);
        if (picking) {
            if (button == 0 && r >= 1) {
                int slot = r - 1;
                if (waypoints().get(slot).isEmpty() || confirmSlot == slot) {
                    Waypoint old = waypoints().get(slot).orElse(null);
                    send(old == null ? Waypoint.fresh(slot, pickX, pickZ)
                            : new Waypoint(slot, pickX, pickZ, old.name(), old.color(), old.head()), false);
                    picking = false;
                    confirmSlot = -1;
                } else {
                    confirmSlot = slot;
                }
                return true;
            }
            picking = false;
            confirmSlot = -1;
            return true;
        }
        int l = tabAt(mx, my, true);
        if (l >= 0) {
            switch (l) {
                case 0 -> {
                    snap = !snap;
                    if (snap) snapOrigin();
                }
                case 1 -> grid = !grid;
                case 2 -> zoomBy(button == 1 ? -1 : 1, mapX + MAP_W / 2.0, mapY + MAP_H / 2.0);
                default -> contour = !contour;
            }
            return true;
        }
        if (r == 0) {
            centerOnPlayer();
            return true;
        }
        if (r > 0) {
            Optional<Waypoint> w = waypoints().get(r - 1);
            if (button == 1 && !frameMode() && w.isPresent()) {
                int slot = r - 1;
                menu.open((int) mx, (int) my)
                        .add(Component.translatable("screen.simplemaps.menu.configure"), true, () -> openDialog(slot))
                        .add(Component.translatable("screen.simplemaps.menu.delete"), true, () -> send(Waypoint.fresh(slot, 0, 0), true));
            } else if (w.isPresent()) {
                centerOn(w.get().x(), w.get().z(), snap);
            }
            return true;
        }
        if (overMap(mx, my)) {
            if (button == 1) {
                int bx = blockX(mx), bz = blockZ(my);
                menu.open((int) mx, (int) my)
                        .add(Component.translatable("screen.simplemaps.menu.create"), !frameMode() && mapId() >= 0, () -> {
                            picking = true;
                            pickX = bx;
                            pickZ = bz;
                            confirmSlot = -1;
                        })
                        .add(Component.translatable("screen.simplemaps.menu.center"), true, () -> centerOn(bx, bz, snap));
                return true;
            }
            dragging = true;
            moved = false;
            dragStartX = mx;
            dragStartY = my;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            originX -= dx * zoom();
            originZ -= dy * zoom();
            moved = moved || Math.abs(event.x() - dragStartX) + Math.abs(event.y() - dragStartY) > 2;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            if (snap) snapOrigin();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editSlot < 0 && overMap(mouseX, mouseY) && scrollY != 0) {
            zoomBy(scrollY > 0 ? -1 : 1, mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE && (menu.isOpen() || picking || editSlot >= 0)) {
            menu.close();
            picking = false;
            confirmSlot = -1;
            if (editSlot >= 0) closeDialog();
            return true;
        }
        if (editSlot >= 0) {
            if (key == InputConstants.KEY_RETURN) {
                saveDialog();
                return true;
            }
            return super.keyPressed(event);
        }
        if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
            zoomBy(-1, mapX + MAP_W / 2.0, mapY + MAP_H / 2.0);
            return true;
        }
        if (key == InputConstants.KEY_MINUS || key == 333 /* GLFW_KEY_KP_SUBTRACT */) {
            zoomBy(1, mapX + MAP_W / 2.0, mapY + MAP_H / 2.0);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !MapsItems.isWayfinder(stack())) {
            onClose();
            return;
        }
        if (frameMode() && (frame() == null || mc.player.distanceToSqr(frame()) > 64)) onClose();
    }

    @Override
    public void removed() {
        if (frameMode() && frame() != null && MapsItems.isWayfinder(stack())) {
            double cx = originX + MAP_W / 2.0 * zoom(), cz = originZ + MAP_H / 2.0 * zoom();
            SimpleMaps.toServer.accept(new FrameViewPayload(frameId, new MapsComponents.View(Mth.floor(cx), Mth.floor(cz), zoom())));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
