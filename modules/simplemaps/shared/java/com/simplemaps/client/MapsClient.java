package com.simplemaps.client;

import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderMapItem;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.TileDataPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Client state of the module: tile cache, map pictures for hands and frames, locator bar marks. */
public final class MapsClient {
    public static final TileCache CACHE = new TileCache();
    private static final Map<InteractionHand, ViewTexture> HANDS = new HashMap<>();
    private static final Map<Integer, ViewTexture> FRAMES = new HashMap<>();
    private static final Identifier LOCATOR_DOT = Identifier.withDefaultNamespace("hud/locator_bar_dot/default_0");
    private static ClientLevel lastLevel;
    private static long ticks;

    private MapsClient() {}

    public static void init() {
        SimpleMaps.openHand = hand -> Minecraft.getInstance().gui.setScreen(WayfinderScreen.forHand(hand));
        SimpleMaps.openFrame = entityId -> Minecraft.getInstance().gui.setScreen(WayfinderScreen.forFrame(entityId));
    }

    public static void receive(TileDataPayload payload) {
        CACHE.receive(payload);
    }

    public static void receive(MapStatePayload payload) {
        CACHE.receive(payload);
    }

    /** Once per client tick (end of tick). */
    public static void tick(Minecraft mc) {
        if (mc.level != lastLevel) {
            CACHE.clear();
            HANDS.values().forEach(ViewTexture::release);
            HANDS.clear();
            FRAMES.values().forEach(ViewTexture::release);
            FRAMES.clear();
            lastLevel = mc.level;
        }
        if (mc.player == null || mc.level == null) return;
        ticks++;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
            if (!MapsItems.isWayfinder(stack) || ref == null) continue;
            ViewTexture view = HANDS.computeIfAbsent(hand, h -> new ViewTexture(h == InteractionHand.MAIN_HAND ? "hand_main" : "hand_off"));
            view.compose(CACHE, ref.id(), 1, Mth.floor(mc.player.getX()), Mth.floor(mc.player.getZ()),
                    stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY));
        }
        Iterator<Map.Entry<Integer, ViewTexture>> it = FRAMES.entrySet().iterator();
        while (it.hasNext()) {
            ViewTexture view = it.next().getValue();
            if (ticks - view.lastUsed > 200) {
                view.release();
                it.remove();
            }
        }
        CACHE.tick();
    }

    /** Whether the map is bound to the dimension the player is in (unknown or unbound counts as yes). */
    public static boolean inMapDimension(int mapId) {
        Minecraft mc = Minecraft.getInstance();
        String dim = CACHE.dimension(mapId);
        return dim == null || dim.isEmpty() || mc.level != null && mc.level.dimension().identifier().toString().equals(dim);
    }

    /** Feature 7: the last death point, when a recovery compass is in the other hand and it lies in this dimension. */
    public static Optional<GlobalPos> deathPoint(Player player, InteractionHand mapHand) {
        InteractionHand other = mapHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!player.getItemInHand(other).is(Items.RECOVERY_COMPASS)) return Optional.empty();
        return player.getLastDeathLocation().filter(p -> p.dimension().equals(player.level().dimension()));
    }

    /** First-person map in hand: our picture, the holder arrow in the middle and the death cross (Feature 7). */
    public static boolean fillHandState(ItemStack stack, boolean mainHand, MapRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ViewTexture view = HANDS.get(hand);
        if (view == null || mc.player == null || !MapsItems.isWayfinder(stack)) return false;
        state.texture = view.id;
        state.decorations.clear();
        var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        float yaw = mc.player.getYRot();
        MapRenderState.MapDecorationRenderState player = new MapRenderState.MapDecorationRenderState();
        player.atlasSprite = atlas.getSprite(Identifier.withDefaultNamespace("player"));
        player.rot = (byte) ((int) ((yaw + (yaw < 0 ? -8.0 : 8.0)) * 16.0 / 360.0) & 15);
        state.decorations.add(player);
        deathPoint(mc.player, hand).ifPresent(pos -> {
            MapRenderState.MapDecorationRenderState cross = new MapRenderState.MapDecorationRenderState();
            cross.atlasSprite = atlas.getSprite(Identifier.withDefaultNamespace("red_x"));
            cross.x = (byte) Mth.clamp((pos.pos().getX() - Mth.floor(mc.player.getX())) * 2, -128, 127);
            cross.y = (byte) Mth.clamp((pos.pos().getZ() - Mth.floor(mc.player.getZ())) * 2, -128, 127);
            state.decorations.add(cross);
        });
        return true;
    }

    /** Framed map (Feature 4): our picture of the stored view; false when the stack is not ready. */
    public static boolean fillFrameState(ItemFrame frame, ItemStack stack, MapRenderState state) {
        MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
        if (ref == null) return false;
        MapsComponents.View view = stack.get(MapsComponents.VIEW);
        int x = view != null ? view.x() : frame.blockPosition().getX(), z = view != null ? view.z() : frame.blockPosition().getZ();
        int zoom = view != null ? view.zoom() : 1;
        ViewTexture texture = FRAMES.computeIfAbsent(frame.getId(), id -> new ViewTexture("frame_" + id));
        texture.lastUsed = ticks;
        texture.compose(CACHE, ref.id(), zoom, x, z, stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY));
        state.texture = texture.id;
        state.decorations.clear();
        return true;
    }

    /** One locator bar mark: position and colour or head icon. */
    public record Mark(Vec3 pos, int color, ItemStack icon) {}

    /** F7: waypoints of the wayfinder maps in main and off hand, only in the map's dimension. */
    public static List<Mark> locatorMarks() {
        Minecraft mc = Minecraft.getInstance();
        List<Mark> out = new ArrayList<>();
        if (mc.player == null) return out;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
            if (!(stack.getItem() instanceof WayfinderMapItem item) || ref == null || !item.accepts(mc.player.level()) || !inMapDimension(ref.id())) continue;
            for (Waypoint w : stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).list()) {
                ItemStack icon = w.head().flatMap(BuiltInRegistries.ITEM::getOptional).map(ItemStack::new).orElse(ItemStack.EMPTY);
                out.add(new Mark(new Vec3(w.x() + 0.5, mc.player.getY(), w.z() + 0.5), w.color(), icon));
            }
        }
        return out;
    }

    public static boolean hasLocatorMarks() {
        return !locatorMarks().isEmpty();
    }

    /** Draws the marks like Vanilla's locator bar dots (same angle maths as TrackedWaypoint). */
    public static void drawLocator(GuiGraphicsExtractor g, DeltaTracker delta, int top) {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.mainCamera();
        int middle = Mth.ceil((g.guiWidth() - 9) / 2.0F);
        for (Mark mark : locatorMarks()) {
            Vec3 direction = camera.position().subtract(mark.pos()).rotateClockwise90();
            float angle = (float) Mth.atan2(direction.z(), direction.x()) * (180.0F / (float) Math.PI);
            double yaw = Mth.degreesDifference(camera.yaw(), angle);
            if (yaw <= -60.0 || yaw > 60.0) continue;
            int x = middle + Mth.floor(yaw * 173.0 / 2.0 / 60.0);
            if (mark.icon().isEmpty()) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_DOT, x, top - 2, 9, 9, mark.color());
            } else {
                g.pose().pushMatrix();
                g.pose().translate(x, top - 2);
                g.pose().scale(9 / 16.0F, 9 / 16.0F);
                g.item(mark.icon(), 0, 0);
                g.pose().popMatrix();
            }
        }
    }
}
