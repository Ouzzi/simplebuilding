package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitConfigPayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import com.simplebuilding.modules.simpleearth.view.EarthView;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client side of Simple Earth: the orbit state from the server (centre A fixed, the player flies freely - WG26-1), the
 * chosen version's view and the menu key (IG31-33, owner 31.10.: the menu opens only with the key, no pause-menu button).
 * Key N (IG31-33 follow-up, 31.10.): O was taken - 26.3 binds Vanilla's friends list (key.friends) to O, and that mapping
 * won the click, so O never opened the earth. N is free in Vanilla 26.3 (only F3+N, a debug combination) and in the mod
 * family (A, B, C, H, I, K, R, U taken); the big map mods use other keys (JourneyMap J, Xaero Y/U/B/M). The mapping has a
 * new name ({@code key.simpleearth.menu}) so options saved with the old O binding do not keep the clash. QOL34 (03.11.): the
 * standard is now "#" ({@link #HASH_KEY}); saved N bindings of earlier test builds stay as the player set them.
 */
public final class EarthClient {
    /**
     * "#" (QOL34, owner 03.11.): 26.3 numbers keys by SDL scancode; 50 = NONUSHASH, the ISO key left of Enter that prints "#" on a
     * German layout (Minecraft name {@code key.keyboard.world.2}). Free in Vanilla and in the mod family.
     */
    public static final int HASH_KEY = 50;
    public static final KeyMapping KEY = new KeyMapping("key.simpleearth.menu", HASH_KEY,
            KeyMapping.Category.register(SimpleEarth.id("general")));

    static boolean enabled, active, parked;
    static double ax, az, ox, oz;
    static int activationY = 1024, switchY = 1536, edge = 2048, seaLevel = 63, minGrid = 5;
    static boolean exploredOnly;
    static List<float[]> others = List.of();
    /** Predicted orbit position (absolute) and the one of the previous tick, for smooth frames. */
    private static EarthView view;
    private static int viewVersion;

    private EarthClient() {}

    public static void init(Path configDir, boolean dev) {
        EarthClientConfig.load(configDir);
        // CMD24: /simpleclient simpleearth (version, the value sets per version).
        com.simplelib.api.config.ConfigCommands.client(SimpleEarth.MOD_ID).json(EarthClientConfig::toJson, EarthClientConfig::apply).file(configDir, EarthClientConfig.FILE).register();
    }

    /** True above the render switch while the earth is shown (nothing is held any more, WG26-1). */
    public static boolean parked() {
        return enabled && parked;
    }

    public static void receive(OrbitStatePayload p) {
        enabled = p.enabled();
        active = p.active();
        parked = p.parked();
        ax = p.ax();
        az = p.az();
        activationY = p.activationY();
        switchY = p.switchY();
        edge = p.edge();
        seaLevel = p.seaLevel();
        exploredOnly = p.exploredOnly();
        minGrid = p.minGrid();
        others = p.others();
        ox = p.ox();
        oz = p.oz();
    }

    public static void receive(EarthTilePayload p) {
        ClientTiles.receive(p);
    }

    public static void joined() {
        sendConfig();
    }

    public static void left() {
        enabled = active = parked = false;
        com.simplebuilding.modules.simpleearth.orbit.OrbitView.active = false;
        ClientTiles.clear();
        FaceTextures.clear();
        view = null;
    }

    /** Tells the server the orbit values of the chosen version (it clamps them). */
    public static void sendConfig() {
        SimpleEarth.toServer.accept(new OrbitConfigPayload((int) EarthClientConfig.get("switchY"), (int) EarthClientConfig.get("switchY"),
                (int) EarthClientConfig.get("edge")));
    }

    public static void endTick(Minecraft mc) {
        while (KEY.consumeClick()) {
            if (mc.gui.screen() == null && mc.player != null) mc.gui.setScreen(new EarthScreen());
        }
        LocalPlayer player = mc.player;
        com.simplebuilding.modules.simpleearth.orbit.OrbitView.active = player != null && fade(player.getEyeY()) > 0;
        ClientTiles.flush();
    }

    /**
     * The player's position relative to the centre A, interpolated between ticks (WG26-1: the real position - the player
     * flies freely and his place over the earth is his place relative to A, like on a map).
     */
    public static double relX(float partial) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? ox - ax : p.xo + (p.getX() - p.xo) * partial - ax;
    }

    public static double relZ(float partial) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? oz - az : p.zo + (p.getZ() - p.zo) * partial - az;
    }

    /** The chosen version's view (fresh after a version switch). */
    public static EarthView view() {
        if (view == null || viewVersion != EarthClientConfig.version) {
            view = EarthView.create(EarthClientConfig.version);
            viewVersion = EarthClientConfig.version;
        }
        return view;
    }

    public static void resetView() {
        view = null;
    }

    /** 0 below the render switch, 1 once {@code fadeBand} blocks above it (the earth blends in). */
    public static double fade(double cameraY) {
        if (!enabled || !active) return 0;
        double band = Math.max(1, EarthClientConfig.get("fadeBand"));
        // E30-3 phase A: ease in and out over the blend band
        return com.simplebuilding.modules.simpleearth.view.Meshes.smooth(0, 1, (cameraY - switchY) / band);
    }

    /** 0 at the render switch, 1 at the ceiling height (V12 turns its corner up with it). */
    public static double heightFraction(double cameraY) {
        double ceiling = Math.max(switchY + 1, EarthClientConfig.get("ceilingY"));
        return Math.max(0, Math.min(1, (cameraY - switchY) / (ceiling - switchY)));
    }

    /**
     * Eye height above the contact point in cube units. WG26-2 ({@code realDistance}, standard): the real height above sea
     * level, so the earth lies exactly as far below as the world would (with an edge of 2048 the whole cube shows from about
     * y 1500 on); off: the earlier zoom - real height at the switch, exponential to the orbit distance at the ceiling.
     */
    public static double eyeHeight(double cameraY) {
        if (EarthClientConfig.renderOn("realDistance")) return Math.max(0.002, (cameraY - seaLevel) / (edge / 2.0));
        double half = edge / 2.0, ceiling = Math.max(switchY + 1, EarthClientConfig.get("ceilingY"));
        double near = Math.max(0.02, (switchY - seaLevel) / half), far = Math.max(near, EarthClientConfig.get("orbitDistance"));
        double t = Math.max(0, Math.min(1, (cameraY - switchY) / (ceiling - switchY)));
        return near * Math.pow(far / near, t);
    }

    public static int edge() {
        return edge;
    }

    public static double ax() {
        return ax;
    }

    public static double az() {
        return az;
    }

    public static int seaLevel() {
        return seaLevel;
    }

    /** E9: other players in orbit (empty when the client option showOthers is off). */
    public static List<float[]> others() {
        return EarthClientConfig.renderOn("showOthers") ? others : List.of();
    }

    /** Finest sample grid the server serves. */
    public static int minGrid() {
        return minGrid;
    }
}
