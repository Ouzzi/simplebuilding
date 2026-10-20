package com.simplemaps.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContextMenu;
import com.simplelib.api.client.ui.UiPalette;
import com.simplemaps.CubeNet;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.WayfinderTabStyle;
import com.simplemaps.WaypointIcons;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderLayout;
import com.simplemaps.net.DisplayPayload;
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
 * The wayfinder map screen (N18 plan, part B): a map area whose 16 px grid always fits, as large as the window allows
 * ({@link WayfinderLayout}, owner N38), drag to pan, wheel or +/- to zoom (1, 2, 4, 8, 16, 32, 64 blocks per pixel; wheel steps accumulate, zoom centres on the cursor; arrows/WASD pan, Home/C = player, 1-8 = waypoints). Left
 * tool buttons (Vanilla button sprites): snap, grid, zoom, height lines, discovered structures (Feature 3).
 * Right bookmarks: player and waypoints 1-8 (click centres). Right-click on the map: "create waypoint" → every
 * bookmark except the eight waypoints is greyed out; a free one is taken at once, an occupied one turns red first
 * and is replaced by a second click. Right-click on a waypoint bookmark: configure (name, colour or mob head),
 * delete. Opened from an item frame (Feature 4) the waypoints are read-only and the view is stored in the frame.
 */
public final class WayfinderScreen extends Screen {
    public static final int CELL = WayfinderLayout.CELL, FRAME = WayfinderLayout.FRAME;
    public static final int LEFT_TABS = WayfinderLayout.LEFT_TABS, RIGHT_TABS = WayfinderLayout.RIGHT_TABS;
    private static final int TAB_HOVER = WayfinderLayout.TAB_HOVER;
    /** Icons on the grey Vanilla buttons: light with a dark drop shadow, like button text. */
    private static final int ICON_SHADOW = 0xFF2A2420;
    /** Matt bronze, like the Octant screen (owner N65, docs/ai/KONZEPT-OKTANT-UI-2026-10-15.md); derived like every other N12 box. */
    public static final UiPalette PALETTE = UiPalette.derived(0xFF6F5F4B);
    /** Size of a waypoint's icon square on the map. */
    public static final int MARK = 14;
    private static final Identifier PLAYER = Identifier.withDefaultNamespace("player");
    private static final Identifier DEATH = Identifier.withDefaultNamespace("red_x");

    /** Zoom stages: the first {@link #ZOOM_NEAR} magnify 1:1 data (8, 4, 2 screen pixels per block), then {@link WayfinderData#ZOOMS}. */
    private static final int[] MAGS = {8, 4, 2}, STAGE_BPP, STAGE_MAG;
    private static final int ZOOM_NEAR = MAGS.length;
    static {
        int n = ZOOM_NEAR + WayfinderData.ZOOMS.length;
        STAGE_BPP = new int[n];
        STAGE_MAG = new int[n];
        for (int i = 0; i < n; i++) {
            STAGE_BPP[i] = i < ZOOM_NEAR ? 1 : WayfinderData.ZOOMS[i - ZOOM_NEAR];
            STAGE_MAG[i] = i < ZOOM_NEAR ? MAGS[i] : 1;
        }
    }

    // Remembered for the session, like the recipe book's state.
    private static boolean snap = true, grid = true, frameContour, structures = true;
    private static int zoomIndex = ZOOM_NEAR;

    private final InteractionHand hand;
    private final int frameId;
    private final UiContextMenu menu = new UiContextMenu(PALETTE);
    private WayfinderLayout layout;
    private int left, top, mapX, mapY, cols, rows, mapW, mapH, panelW, panelH;
    /** Top-left of the map area in blocks. */
    private double originX, originZ;
    private boolean placed, dragging, moved;
    private double dragStartX, dragStartY, wheel;
    /** Panning limit: the world border (blocks from the origin). */
    private static final double LIMIT = 30_000_000;
    /** Waypoint creation: the target block, and the occupied slot that was clicked once (-1 = none). */
    private boolean picking;
    private int pickX, pickZ, confirmSlot = -1;
    /** Configure dialog. */
    private int editSlot = -1;
    private WaypointDialog picker;

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
        frameContour = contourOn;
        zoomIndex = Mth.clamp(ZOOM_NEAR + zoomLevel, 0, STAGE_BPP.length - 1);
    }

    /** Client smoke: block X in the middle of the map. */
    public int blockAtMapCenterX() {
        return (int) Math.floor(originX + mapW / 2.0 / mag() * zoom());
    }

    /** Client smoke: screen position of the map area's centre. */
    public int mapCenterX() {
        return mapX + mapW / 2;
    }

    public int mapCenterY() {
        return mapY + mapH / 2;
    }

    /** Client smoke: screen position of a left bookmark's centre. */
    public int leftTabCenterX() {
        return layout.leftTabX() + layout.tab() / 2;
    }

    public int leftTabCenterY(int i) {
        return leftTabY(i) + layout.tab() / 2;
    }

    /** Client smoke: screen position of a right bookmark's centre. */
    public int rightTabCenterX() {
        return layout.rightTabX() + layout.tab() / 2;
    }

    public int rightTabCenterY(int i) {
        return rightTabY(i) + layout.tab() / 2;
    }

    /** Client smoke: opens the configure dialog of a waypoint. */
    public void openConfigure(int slot) {
        openDialog(slot);
    }

    /** Client smoke: the map size in grid cells. */
    public int mapCols() {
        return cols;
    }

    public int mapRows() {
        return rows;
    }

    /** Blocks per map pixel of the data shown (1 for the magnified stages). */
    public static int zoom() {
        return STAGE_BPP[zoomIndex];
    }

    /** Screen pixels per map pixel (1 except in the magnified stages). */
    public static int mag() {
        return STAGE_MAG[zoomIndex];
    }

    /** "8:1" for a magnified stage, else "1:4". */
    private static String ratio() {
        return mag() > 1 ? mag() + ":1" : "1:" + zoom();
    }

    @Override
    protected void init() {
        int oldW = mapW, oldH = mapH;
        layout = WayfinderLayout.of(width, height);
        cols = layout.cols();
        rows = layout.rows();
        mapW = layout.mapW();
        mapH = layout.mapH();
        panelW = layout.panelW();
        panelH = layout.panelH();
        left = layout.left();
        top = layout.top();
        mapX = layout.mapX();
        mapY = layout.mapY();
        if (placed && oldW > 0) {
            // Window resized: keep the centre where it was.
            originX += (oldW - mapW) / 2.0 / mag() * zoom();
            originZ += (oldH - mapH) / 2.0 / mag() * zoom();
            if (snap) snapOrigin();
        }
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

    /** Height lines: the saved mode of the map in hand; a framed map has no hand to save to, so it keeps a local switch. */
    private boolean contour() {
        return frameMode() ? frameContour : display().contour();
    }

    private MapsComponents.Display display() {
        return stack().getOrDefault(MapsComponents.DISPLAY, MapsComponents.Display.DEFAULT);
    }

    private static int indexOf(int zoom) {
        for (int i = ZOOM_NEAR; i < STAGE_BPP.length; i++) if (STAGE_BPP[i] == zoom) return i;
        return ZOOM_NEAR;
    }

    // ---------------------------------------------------------------- view maths

    private void centerOn(double x, double z, boolean snapToCell) {
        int bpp = zoom();
        originX = x - mapW / 2.0 / mag() * bpp;
        originZ = z - mapH / 2.0 / mag() * bpp;
        clampOrigin();
        if (snapToCell) snapOrigin();
    }

    /** "Player": with snap the player's grid cell is centred, not the player (owner N18). */
    private void centerOnPlayer() {
        if (minecraft == null || minecraft.player == null) return;
        int bpp = zoom(), cell = CELL * bpp;
        if (snap) {
            originX = (Math.floorDiv(minecraft.player.getBlockX(), cell) - cols / mag() / 2) * (double) cell;
            originZ = (Math.floorDiv(minecraft.player.getBlockZ(), cell) - rows / mag() / 2) * (double) cell;
        } else {
            centerOn(minecraft.player.getX(), minecraft.player.getZ(), false);
        }
    }

    /** Keeps the map centre inside the world border. */
    private void clampOrigin() {
        double halfW = mapW / 2.0 / mag() * zoom(), halfH = mapH / 2.0 / mag() * zoom();
        originX = Mth.clamp(originX, -LIMIT - halfW, LIMIT - halfW);
        originZ = Mth.clamp(originZ, -LIMIT - halfH, LIMIT - halfH);
    }

    private void snapOrigin() {
        double cell = CELL * zoom();
        originX = Math.round(originX / cell) * cell;
        originZ = Math.round(originZ / cell) * cell;
        clampOrigin();
    }

    /** Map pixel (at the current zoom) of the area's left/top edge. */
    private long originPx() {
        return (long) Math.floor(originX / zoom());
    }

    private long originPz() {
        return (long) Math.floor(originZ / zoom());
    }

    private int screenX(double blockX) {
        return mapX + (int) (Math.floor(blockX / zoom()) - originPx()) * mag() + mag() / 2;
    }

    private int screenY(double blockZ) {
        return mapY + (int) (Math.floor(blockZ / zoom()) - originPz()) * mag() + mag() / 2;
    }

    private int blockX(double sx) {
        return (int) Math.floor((originPx() + Math.floor((sx - mapX) / mag())) * zoom());
    }

    private int blockZ(double sy) {
        return (int) Math.floor((originPz() + Math.floor((sy - mapY) / mag())) * zoom());
    }

    private boolean overMap(double x, double y) {
        return x >= mapX && x < mapX + mapW && y >= mapY && y < mapY + mapH;
    }

    private void zoomBy(int steps, double anchorX, double anchorY) {
        int next = Mth.clamp(zoomIndex + steps, 0, STAGE_BPP.length - 1);
        if (next == zoomIndex) return;
        double ax = Mth.clamp(anchorX - mapX, 0, mapW), ay = Mth.clamp(anchorY - mapY, 0, mapH);
        double bx = originX + ax / mag() * zoom(), bz = originZ + ay / mag() * zoom();
        zoomIndex = next;
        originX = bx - ax / mag() * zoom();
        originZ = bz - ay / mag() * zoom();
        clampOrigin();
        if (snap) snapOrigin();
    }

    // ---------------------------------------------------------------- tabs

    private int leftTabY(int i) {
        return layout.leftTabY(i);
    }

    private int rightTabY(int i) {
        return layout.rightTabY(i);
    }

    private int tabAt(double x, double y, boolean leftSide) {
        int count = leftSide ? LEFT_TABS : RIGHT_TABS;
        int tab = layout.tab();
        int x0 = leftSide ? layout.leftTabX() - TAB_HOVER : layout.rightTabX();
        int x1 = leftSide ? layout.leftTabX() + tab : layout.rightTabX() + tab + TAB_HOVER;
        if (x < x0 || x >= x1) return -1;
        for (int i = 0; i < count; i++) {
            int ty = leftSide ? leftTabY(i) : rightTabY(i);
            if (y >= ty && y < ty + tab) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        ItemStack stack = stack();
        int mapId = mapId();
        Waypoints waypoints = waypoints();
        boolean dialog = picker != null;
        drawTabs(g, mouseX, mouseY, waypoints, dialog);
        UiBoxes.box(g, left, top, panelW, panelH, PALETTE);
        g.text(font, stack.isEmpty() ? title : stack.getHoverName(), left + FRAME, top + FRAME + 1, PALETTE.label(), false);
        // Map area: sunk dark field, then the explored tiles.
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, UiBoxes.RIM);
        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF2C2A25);
        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        if (mapId >= 0) {
            drawTiles(g, mapId);
            if (grid) drawGrid(g);
            if (!frameMode()) {
                MapLayers.request(mapId, originX, originZ, zoom(), mag(), mapW, mapH);
                MapLayers.draw(g, mapId, mapX, mapY, mapW, mapH, originX, originZ, zoom(), mag());
            }
            if (structures) drawStructures(g, mapId);
            drawMarks(g, waypoints);
        }
        g.disableScissor();
        Component status = status(mapId);
        // A new map says so in the middle; "another dimension" sits at the top so the explored map stays readable.
        if (status != null) g.centeredText(font, status, mapX + mapW / 2, mapId < 0 ? mapY + mapH / 2 - 4 : mapY + 6, 0xFFE8E2D0);
        drawFooter(g, mouseX, mouseY);
        if (picking) {
            UiBookmarks.greyOut(g, left, top, panelW, panelH);
            for (int i = 0; i < LEFT_TABS; i++) UiBookmarks.greyOut(g, layout.leftTabX(), leftTabY(i), layout.tab(), layout.tab());
            UiBookmarks.greyOut(g, layout.rightTabX(), rightTabY(0), layout.tab(), layout.tab());
            g.centeredText(font, Component.translatable("screen.simplemaps.pick_slot"), mapX + mapW / 2, mapY + mapH / 2 - 4, 0xFFFFFFFF);
        }
        if (dialog) picker.render(g, mouseX, mouseY, width, height);
        super.extractRenderState(g, mouseX, mouseY, a);
        if (dialog) picker.tooltips(g, mouseX, mouseY);
        menu.render(g, font, mouseX, mouseY);
        if (!dialog && !menu.isOpen()) {
            tabTooltips(g, mouseX, mouseY, waypoints);
            structureTooltip(g, mouseX, mouseY, mapId);
            if (!frameMode() && overMap(mouseX, mouseY)) MapLayers.tooltip(g, font, mapId, mouseX, mouseY, mapX, mapY, originX, originZ, zoom(), mag());
        }
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
        int mag = mag();
        int tx1 = (int) Math.floorDiv(px0 + (mapW - 1) / mag, WayfinderData.TILE), tz1 = (int) Math.floorDiv(pz0 + (mapH - 1) / mag, WayfinderData.TILE);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                TileCache.Entry e = MapsClient.CACHE.want(new TileCache.Key(mapId, bpp, tx, tz));
                DynamicTexture texture = contour() ? MapsClient.CACHE.contour(e) : MapsClient.CACHE.texture(e);
                if (texture == null) continue;
                int x = mapX + (int) ((long) tx * WayfinderData.TILE - px0) * mag, y = mapY + (int) ((long) tz * WayfinderData.TILE - pz0) * mag;
                g.blit(texture.getTextureView(), texture.getSampler(), x, y, x + WayfinderData.TILE * mag, y + WayfinderData.TILE * mag, 0, 1, 0, 1);
            }
        }
    }

    /** World-aligned lines every 16 map pixels (one chunk at 1:1). */
    private void drawGrid(GuiGraphicsExtractor g) {
        long px0 = originPx(), pz0 = originPz();
        int mag = mag();
        for (int x = (int) Math.floorMod(-px0, CELL) * mag; x < mapW; x += CELL * mag) g.fill(mapX + x, mapY, mapX + x + 1, mapY + mapH, 0x40000000);
        for (int y = (int) Math.floorMod(-pz0, CELL) * mag; y < mapH; y += CELL * mag) g.fill(mapX, mapY + y, mapX + mapW, mapY + y + 1, 0x40000000);
    }

    private void drawMarks(GuiGraphicsExtractor g, Waypoints waypoints) {
        for (Waypoint w : waypoints.list()) {
            int x = screenX(w.x()), y = screenY(w.z());
            WaypointArt.draw(g, w, x - MARK / 2, y - MARK / 2, MARK);
            // Owner N68: the name sits beside the icon, its middle on the icon's middle; the coordinates go under it.
            int textX = x + MARK / 2 + 3, lineH = font.lineHeight;
            g.text(font, w.name().isEmpty() ? String.valueOf(w.slot() + 1) : w.name(), textX, y - lineH / 2, 0xFFFFFFFF, true);
            if (MapLayers.coordsOn()) MapLayers.drawCoords(g, font, textX, y + lineH / 2 + 1, w.x(), w.z());
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
            if (MapLayers.coordsOn()) MapLayers.drawCoords(g, font, x + 6, y + 1, mc.player.getBlockX(), mc.player.getBlockZ());
        }
        // Feature 7: shown even over unexplored ground; only the point appears there.
        if (!frameMode()) {
            Optional<GlobalPos> death = MapsClient.deathPoint(mc.player, hand);
            death.ifPresent(pos -> g.blitSprite(RenderPipelines.GUI_TEXTURED, atlas.getSprite(DEATH),
                    screenX(pos.pos().getX()) - 4, screenY(pos.pos().getZ()) - 4, 8, 8));
        }
    }

    /** Feature 3: the structures the holder has seen or entered, as small diamonds in a colour derived from the id. */
    private void drawStructures(GuiGraphicsExtractor g, int mapId) {
        for (WayfinderData.Mark m : MapsClient.CACHE.marks(mapId)) {
            int x = screenX(m.x()), y = screenY(m.z());
            if (x < mapX - 4 || x > mapX + mapW + 4 || y < mapY - 4 || y > mapY + mapH + 4) continue;
            int color = 0xFF000000 | Mth.hsvToRgb((m.id().hashCode() & 0xFFFF) / 65536.0F, 0.6F, 0.95F);
            for (int r = 0; r <= 4; r++) g.fill(x - (4 - r), y - r, x + (4 - r) + 1, y - r + 1, r == 4 ? UiBoxes.RIM : color);
            for (int r = 1; r <= 4; r++) g.fill(x - (4 - r), y + r, x + (4 - r) + 1, y + r + 1, r == 4 ? UiBoxes.RIM : color);
        }
    }

    private void structureTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY, int mapId) {
        if (!structures || mapId < 0 || !overMap(mouseX, mouseY)) return;
        for (WayfinderData.Mark m : MapsClient.CACHE.marks(mapId)) {
            if (Math.abs(screenX(m.x()) - mouseX) <= 4 && Math.abs(screenY(m.z()) - mouseY) <= 4) {
                g.setTooltipForNextFrame(font, Component.literal(structureName(m.id()) + " (" + m.x() + ", " + m.z() + ")"), mouseX, mouseY);
                return;
            }
        }
    }

    /** "minecraft:ancient_city" → "Ancient City" (structures have no Vanilla names). */
    static String structureName(String id) {
        String path = id.substring(id.indexOf(':') + 1).replace('/', ' ').replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String word : path.split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** Layer switches (loaded chunks, then every source the server offers); a click flips one and the menu stays open. */
    private void openLayersMenu(int x, int y) {
        UiContextMenu m = menu.open(x, y);
        m.add(layerLabel(MapLayers.CHUNKS, Component.translatable("screen.simplemaps.layer.chunks")), true, () -> {
            MapLayers.toggle(MapLayers.CHUNKS);
            openLayersMenu(x, y);
        });
        for (var source : MapLayers.available()) {
            m.add(layerLabel(source.id(), Component.translatableWithFallback(source.titleKey(), source.id())), true, () -> {
                MapLayers.toggle(source.id());
                openLayersMenu(x, y);
            });
        }
    }

    private static Component layerLabel(String id, Component name) {
        return Component.literal(MapLayers.on(id) ? "[x] " : "[ ] ").append(name);
    }

    private void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = mapY + mapH + 3;
        g.text(font, Component.translatable("screen.simplemaps.zoom", ratio(), zoomIndex + 1, STAGE_BPP.length), mapX, y, PALETTE.label(), false);
        if (overMap(mouseX, mouseY)) {
            String pos = blockX(mouseX) + ", " + blockZ(mouseY);
            g.text(font, pos, mapX + mapW - font.width(pos), y, PALETTE.label(), false);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY, Waypoints waypoints, boolean dialog) {
        boolean[] states = {snap, grid, false, contour(), structures, MapLayers.coordsOn(), false, false};
        int tab = layout.tab();
        // Both sides are bookmarks in the box colour (owner N65): tools left, the player and the waypoints right.
        for (int i = 0; i < LEFT_TABS; i++) {
            int y = leftTabY(i);
            boolean hover = !dialog && tabAt(mouseX, mouseY, true) == i;
            int shift = hover ? TAB_HOVER : 0;
            WayfinderTabStyle style = WayfinderTabStyle.of(PALETTE, states[i], hover);
            drawTab(g, layout.leftTabX() - shift, y, layout.tabW(), tab, style);
            int x = layout.leftTabX() - shift + (tab - 10) / 2, iy = y + (tab - 10) / 2;
            // Shadow first, then the light icon one pixel up-left (Vanilla button text look).
            drawLeftIcon(g, i, x + 1, iy + 1, ICON_SHADOW, true);
            drawLeftIcon(g, i, x, iy, style.ink(), false);
            if (style.bar() != 0) g.fill(layout.leftTabX() - shift + 4, y + tab - 5, layout.leftTabX() - shift + tab - 4, y + tab - 4, style.bar());
        }
        for (int i = 0; i < RIGHT_TABS; i++) {
            int y = rightTabY(i);
            boolean hover = !dialog && tabAt(mouseX, mouseY, false) == i;
            Optional<Waypoint> w = i == 0 ? Optional.empty() : waypoints.get(i - 1);
            int shift = hover ? TAB_HOVER : 0, x = layout.rightTabX() - WayfinderLayout.TAB_IN + shift;
            WayfinderTabStyle style = WayfinderTabStyle.of(PALETTE, i == 0 || w.isPresent(), hover);
            drawTab(g, x, y, layout.tabW(), tab, style);
            int size = tab >= 26 ? 20 : 16, ix = layout.rightTabX() + shift + (tab - size) / 2, iy = y + (tab - size) / 2;
            if (i == 0) {
                WaypointArt.draw(g, Identifier.withDefaultNamespace("compass"), CubeNet.MODEL, ix, iy, size);
            } else if (w.isPresent()) {
                WaypointArt.draw(g, w.get(), ix, iy, size);
            } else {
                // Free slot: a dim plus.
                int cx = layout.rightTabX() + shift + tab / 2, cy = y + tab / 2;
                g.fill(cx - 3, cy, cx + 4, cy + 1, 0x80FFFFFF);
                g.fill(cx, cy - 3, cx + 1, cy + 4, 0x80FFFFFF);
            }
            if (picking && confirmSlot == i - 1 && i > 0) UiBookmarks.confirm(g, layout.rightTabX(), y, tab + shift, tab);
        }
    }

    /** A side button in the map frame's look: layered stair-cornered edge (rim, bevel, band) round the face; the inner end hides under the frame. */
    private static void drawTab(GuiGraphicsExtractor g, int x, int y, int w, int h, WayfinderTabStyle style) {
        UiBoxes.stairFrame(g, x, y, w, h, style.layers(), style.face());
    }

    /** 10x10 icons: magnet (snap), grid, zoom level, height lines, structure (house). */
    private void drawLeftIcon(GuiGraphicsExtractor g, int i, int x, int y, int c, boolean shadow) {
        switch (i) {
            case 0 -> {
                g.fill(x + 1, y + 1, x + 4, y + 8, c);
                g.fill(x + 7, y + 1, x + 10, y + 8, c);
                g.fill(x + 1, y + 7, x + 10, y + 10, c);
                if (!shadow) {
                    g.fill(x + 1, y + 1, x + 4, y + 3, 0xFFE04A3A);
                    g.fill(x + 7, y + 1, x + 10, y + 3, 0xFFE04A3A);
                }
            }
            case 1 -> {
                for (int k = 0; k <= 9; k += 3) {
                    g.fill(x + k, y, x + k + 1, y + 10, c);
                    g.fill(x, y + k, x + 10, y + k + 1, c);
                }
            }
            case 2 -> {
                String label = ratio();
                float scale = font.width(label) > 14 ? 0.75F : 1.0F;
                g.pose().pushMatrix();
                g.pose().translate(x + 5 - font.width(label) * scale / 2.0F, y + 1);
                g.pose().scale(scale, scale);
                g.text(font, label, 0, 0, c, false);
                g.pose().popMatrix();
            }
            case 7 -> {
                // Two arrows' worth of "sync": a small map frame with a centre dot.
                g.fill(x, y, x + 10, y + 1, c);
                g.fill(x, y + 9, x + 10, y + 10, c);
                g.fill(x, y + 1, x + 1, y + 9, c);
                g.fill(x + 9, y + 1, x + 10, y + 9, c);
                g.fill(x + 4, y + 4, x + 6, y + 6, c);
            }
            case 4 -> {
                g.fill(x + 4, y, x + 6, y + 1, c);
                g.fill(x + 2, y + 1, x + 8, y + 3, c);
                g.fill(x, y + 3, x + 10, y + 5, c);
                g.fill(x + 1, y + 5, x + 9, y + 10, c);
                if (!shadow) g.fill(x + 4, y + 7, x + 6, y + 10, 0xFF6B6B6B);
            }
            case 5 -> {
                // "x,z" as two small letters with a dot between.
                g.fill(x, y + 1, x + 3, y + 2, c);
                g.fill(x + 1, y + 2, x + 2, y + 5, c);
                g.fill(x, y + 5, x + 3, y + 6, c);
                g.fill(x + 4, y + 4, x + 6, y + 6, c);
                g.fill(x + 7, y + 5, x + 10, y + 6, c);
                g.fill(x + 8, y + 6, x + 9, y + 9, c);
                g.fill(x + 7, y + 9, x + 10, y + 10, c);
            }
            case 6 -> {
                // Stacked layers.
                g.fill(x + 3, y, x + 7, y + 1, c);
                g.fill(x + 1, y + 1, x + 9, y + 3, c);
                g.fill(x, y + 4, x + 10, y + 5, c);
                g.fill(x, y + 7, x + 10, y + 8, c);
                g.fill(x + 2, y + 9, x + 8, y + 10, c);
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
        String[] keys = {"snap", "grid", "zoom", "contour", "structures", "coords", "layers", "sync"};
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

    private void openDialog(int slot) {
        Optional<Waypoint> w = waypoints().get(slot);
        if (w.isEmpty()) return;
        if (picker != null && editSlot == slot) {
            picker.init(width, height);
            return;
        }
        closeDialog();
        editSlot = slot;
        picker = new WaypointDialog(font, PALETTE, w.get(), this::addRenderableWidget, this::removeWidget, this::saveDialog, this::closeDialog);
        picker.init(width, height);
        setFocused(picker.nameBox());
    }

    private void closeDialog() {
        if (picker != null) picker.close();
        picker = null;
        editSlot = -1;
    }

    private void saveDialog(Waypoint waypoint) {
        send(waypoint, false);
        closeDialog();
    }

    // ---------------------------------------------------------------- input

    private void sendDisplay(MapsComponents.Display display) {
        if (frameMode()) return;
        SimpleMaps.toServer.accept(new DisplayPayload(hand == InteractionHand.MAIN_HAND ? 0 : 1, display));
    }

    /** Height lines: saved on the map (so the map in hand shows the same); a framed map only switches locally. */
    private void toggleContour() {
        if (frameMode()) {
            frameContour = !frameContour;
            return;
        }
        MapsComponents.Display d = display();
        sendDisplay(new MapsComponents.Display(!d.contour(), d.handZoom()));
    }

    /**
     * "Synchronise zoom" (owner N65): the held map never follows the menu's zoom by itself. Click: the held map takes the menu's
     * scale; the menu centres on the player, so both show the same area. Shift+click: the menu takes the held map's scale.
     */
    private void syncZoom(boolean fromHand) {
        if (frameMode()) return;
        MapsComponents.Display d = display();
        if (fromHand) {
            zoomIndex = indexOf(d.handZoom());
        } else {
            sendDisplay(new MapsComponents.Display(d.contour(), zoom()));
        }
        centerOnPlayer();
    }

    private void send(Waypoint waypoint, boolean delete) {
        if (frameMode()) return;
        SimpleMaps.toServer.accept(new WaypointEditPayload(hand == InteractionHand.MAIN_HAND ? 0 : 1, delete, waypoint));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (menu.mouseClicked(mx, my)) return true;
        if (picker != null) {
            if (picker.click(mx, my)) return true;
            return super.mouseClicked(event, doubleClick);
        }
        int r = tabAt(mx, my, false);
        if (picking) {
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && r >= 1) {
                int slot = r - 1;
                if (waypoints().get(slot).isEmpty() || confirmSlot == slot) {
                    Waypoint old = waypoints().get(slot).orElse(null);
                    send(old == null ? Waypoint.fresh(slot, pickX, pickZ, WaypointIcons.random(new java.util.Random()))
                            : old.movedTo(pickX, pickZ), false);
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
                case 2 -> zoomBy(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT ? 1 : -1, mapX + mapW / 2.0, mapY + mapH / 2.0);
                case 3 -> toggleContour();
                case 4 -> structures = !structures;
                case 5 -> MapLayers.toggleCoords();
                case 7 -> syncZoom(event.hasShiftDown());
                default -> openLayersMenu((int) mx, (int) my);
            }
            return true;
        }
        if (r == 0) {
            centerOnPlayer();
            return true;
        }
        if (r > 0) {
            Optional<Waypoint> w = waypoints().get(r - 1);
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && !frameMode() && w.isPresent()) {
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
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {
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
            originX -= dx / mag() * zoom();
            originZ -= dy / mag() * zoom();
            clampOrigin();
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
        if (picker != null && picker.scrolled(mouseX, mouseY, scrollY)) return true;
        if (editSlot < 0 && overMap(mouseX, mouseY) && scrollY != 0) {
            // Fine wheels and touchpads send fractions: collect them until a whole step is reached.
            wheel += scrollY;
            int steps = (int) wheel;
            if (steps != 0) {
                wheel -= steps;
                zoomBy(-steps, mouseX, mouseY);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Numpad minus in the 26.3 SDL scancode table (KEY_ADD = 87 is numpad plus); InputConstants has no constant for it. */
    private static final int KEY_KP_MINUS = InputConstants.KEY_ADD - 1;

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
            if (picker != null && picker.enter(key)) return true;
            return super.keyPressed(event);
        }
        int dx = key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A ? -1 : key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D ? 1 : 0;
        int dz = key == InputConstants.KEY_UP || key == InputConstants.KEY_W ? -1 : key == InputConstants.KEY_DOWN || key == InputConstants.KEY_S ? 1 : 0;
        if (dx != 0 || dz != 0) {
            // Snap: one grid cell per press; free: half a cell. Held keys repeat.
            double step = CELL * zoom() * (snap ? 1 : 0.5);
            originX += dx * step;
            originZ += dz * step;
            clampOrigin();
            return true;
        }
        if (key == InputConstants.KEY_HOME || key == InputConstants.KEY_C) {
            centerOnPlayer();
            return true;
        }
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_8) {
            waypoints().get(key - InputConstants.KEY_1).ifPresent(w -> centerOn(w.x(), w.z(), snap));
            return true;
        }
        if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
            zoomBy(-1, mapX + mapW / 2.0, mapY + mapH / 2.0);
            return true;
        }
        if (key == InputConstants.KEY_MINUS || key == KEY_KP_MINUS) {
            zoomBy(1, mapX + mapW / 2.0, mapY + mapH / 2.0);
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
            double cx = originX + mapW / 2.0 * zoom(), cz = originZ + mapH / 2.0 * zoom();
            SimpleMaps.toServer.accept(new FrameViewPayload(frameId, new MapsComponents.View(Mth.floor(cx), Mth.floor(cz), zoom())));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
