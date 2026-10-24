package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitConfigPayload;
import com.simplebuilding.modules.simpleearth.net.OrbitMovePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import com.simplebuilding.modules.simpleearth.view.EarthView;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Client side of Simple Earth: the orbit state from the server, the predicted orbit position while parked (glide input from
 * the movement keys, figure held over the park point), the chosen version's view, the menu key and the dev button.
 * Key O ("orbit"): free in Vanilla (L/P are the neighbouring menu keys) and in the mod family (H, K, B, I, R taken); none of
 * the big map mods binds it by default (JourneyMap J, Xaero Y/U/B).
 */
public final class EarthClient {
    public static final KeyMapping KEY = new KeyMapping("key.simpleearth.open", InputConstants.KEY_O,
            KeyMapping.Category.register(SimpleEarth.id("general")));

    static boolean development;
    static boolean enabled, active, parked;
    static double ax, az, ox, oz;
    static int activationY = 1024, switchY = 1536, edge = 65536, seaLevel = 63;
    static float speed = 256;
    static boolean exploredOnly;
    static List<float[]> others = List.of();
    /** Predicted orbit position (absolute) and the one of the previous tick, for smooth frames. */
    static double predX, predZ, prevX, prevZ;
    static double parkX, parkZ;
    static boolean holding;
    /** Movement input captured by {@code EarthKeyboardInputMixin} while parked (strafe, forward). */
    public static float glideStrafe, glideForward;
    private static EarthView view;
    private static int viewVersion;

    private EarthClient() {}

    public static void init(Path configDir, boolean dev) {
        development = dev;
        EarthClientConfig.load(configDir);
    }

    /** True while the player is parked in orbit: movement keys glide the orbit position instead of walking. */
    public static boolean parked() {
        return enabled && parked;
    }

    public static void receive(OrbitStatePayload p) {
        boolean wasParked = parked;
        enabled = p.enabled();
        active = p.active();
        parked = p.parked();
        ax = p.ax();
        az = p.az();
        activationY = p.activationY();
        switchY = p.switchY();
        edge = p.edge();
        speed = p.speed();
        seaLevel = p.seaLevel();
        exploredOnly = p.exploredOnly();
        others = p.others();
        ox = p.ox();
        oz = p.oz();
        LocalPlayer player = Minecraft.getInstance().player;
        if (parked && !wasParked && player != null) {
            parkX = player.getX();
            parkZ = player.getZ();
            holding = true;
        }
        if (!parked) holding = false;
        // the server is authoritative; small differences are prediction lag, large ones a correction (border, teleport)
        if (!parked || Math.hypot(predX - ox, predZ - oz) > speed * 2) {
            predX = prevX = ox;
            predZ = prevZ = oz;
        }
    }

    public static void receive(EarthTilePayload p) {
        ClientTiles.receive(p);
    }

    public static void joined() {
        sendConfig();
    }

    public static void left() {
        enabled = active = parked = holding = false;
        ClientTiles.clear();
        FaceTextures.clear();
        view = null;
    }

    /** Tells the server the orbit values of the chosen version (it clamps them). */
    public static void sendConfig() {
        SimpleEarth.toServer.accept(new OrbitConfigPayload((int) EarthClientConfig.get("activationY"), (int) EarthClientConfig.get("switchY"),
                (int) EarthClientConfig.get("edge"), (float) EarthClientConfig.get("orbitSpeed")));
    }

    public static void startTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || !holding) return;
        // parked: the figure stays over the park point, only the height changes (Vanilla flight up/down keeps working)
        var motion = player.getDeltaMovement();
        player.setDeltaMovement(0, motion.y, 0);
    }

    public static void endTick(Minecraft mc) {
        while (KEY.consumeClick()) {
            if (mc.gui.screen() == null && mc.player != null) mc.gui.setScreen(new EarthScreen());
        }
        LocalPlayer player = mc.player;
        prevX = predX;
        prevZ = predZ;
        if (player == null) return;
        if (holding) {
            if (Math.hypot(player.getX() - parkX, player.getZ() - parkZ) > 0.25) player.setPos(parkX, player.getY(), parkZ);
            double len = Math.hypot(glideStrafe, glideForward);
            if (len > 1e-3) {
                double yaw = Math.toRadians(player.getYRot()), step = speed / 20.0;
                double fx = -Math.sin(yaw), fz = Math.cos(yaw), sx = Math.cos(yaw), sz = Math.sin(yaw);
                double k = Math.min(1, len) / len;
                double dx = (fx * glideForward + sx * glideStrafe) * k * step, dz = (fz * glideForward + sz * glideStrafe) * k * step;
                predX += dx;
                predZ += dz;
                SimpleEarth.toServer.accept(new OrbitMovePayload((float) dx, (float) dz));
            }
        } else {
            predX = prevX = player.getX();
            predZ = prevZ = player.getZ();
        }
        ClientTiles.flush();
    }

    /** Orbit position relative to the centre A, interpolated between ticks. */
    public static double relX(float partial) {
        return prevX + (predX - prevX) * partial - ax;
    }

    public static double relZ(float partial) {
        return prevZ + (predZ - prevZ) * partial - az;
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
        return Math.max(0, Math.min(1, (cameraY - switchY) / band));
    }

    /** Eye height above the contact point in cube units: real height at the switch, exponential to the orbit distance at the ceiling. */
    public static double eyeHeight(double cameraY) {
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

    public static List<float[]> others() {
        return others;
    }

    /** Dev button: AUTO = only in a development environment. */
    public static boolean devButton() {
        return switch (EarthClientConfig.devButton) {
            case ON -> true;
            case OFF -> false;
            case AUTO -> development;
        };
    }

    /** Adds the dev button (top left of the pause menu) that opens the earth menu. */
    public static void decorate(Screen screen, int width, int height) {
        if (!(screen instanceof PauseScreen) || !devButton()) return;
        Button button = Button.builder(Component.translatable("simpleearth.screen.dev_button"), b -> Minecraft.getInstance().gui.setScreen(new EarthScreen()))
                .bounds(4, 4, 52, 20).tooltip(Tooltip.create(Component.translatable("simpleearth.screen.dev_button.tip"))).build();
        net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(button);
    }
}
