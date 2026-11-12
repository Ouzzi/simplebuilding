package com.simpleoctets.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simpleoctets.block.OctetCellBlock;
import com.simpleoctets.block.OctetCells;
import com.simpleoctets.registry.OctetRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PLAN40 simpleoctets-5: placing, sneak-breaking, mixing and the melon slice with the REAL operating-system mouse and keyboard (pattern
 * of Simple Building Tools' BtProbe): inert unless the game folder holds {@code simpleoctets-octets-probe.json}. Then it creates a flat
 * creative world and follows commands from {@code simpleoctets-octets-command.json}: {@code bench} (a fresh bench around the player:
 * the player at the centre of its block looking south, six cells at feet level on the floor - {@code corner} and {@code mixed} empty in
 * front, {@code sneak} (four oak octets, the bottom half) and {@code melon} (empty) behind, {@code cracks} (four oak octets) to the
 * east and {@code cracksMixed} (oak below, stone above) to the west), {@code aim} (turn the view to a world point from the CURRENT
 * eye, so a sneaking player aims from the lower eye), {@code cmd} (a chat command: items, game mode), {@code shot}, {@code quit}. Each tick it writes {@code simpleoctets-octets-status.json}: the
 * eye, sneaking, the crosshair (block, face, point, the octet index under it) and, from the server, each bench cell's block, octet mask
 * and the materials of its octets, and the held stack. The driver ({@code modules/simpleoctets/tools/octets_probe.py}) presses keys and
 * mouse buttons through XTest; the pointer never moves while the game holds it, so only {@code aim} turns the view.
 */
public final class OctetsProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simpleoctets-octets-probe.json", STATUS = "simpleoctets-octets-status.json",
            COMMAND = "simpleoctets-octets-command.json";
    private static Boolean active;
    private static boolean worldRequested, ready;
    private static int ticks;
    private static final Map<String, BlockPos> CELLS = new LinkedHashMap<>();

    private OctetsProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve(REQUEST));
        if (!active) return;
        ticks++;
        try {
            if (!ready) {
                if (!worldRequested) {
                    if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                    worldRequested = true;
                    String name = "sb-octets-probe-" + System.nanoTime();
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.gui.screen());
                    return;
                }
                var server = mc.getSingleplayerServer();
                if (mc.level == null || mc.player == null || mc.gameMode == null || server == null || !server.isReady() || mc.gui.screen() != null
                        || ticks % 20 != 0) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                ready = true;
                return;
            }
            Path cmd = dir.resolve(COMMAND);
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                switch (c.get("op").getAsString()) {
                    case "bench" -> bench(mc);
                    case "aim" -> aim(mc, new Vec3(c.get("x").getAsDouble(), c.get("y").getAsDouble(), c.get("z").getAsDouble()));
                    case "cmd" -> mc.player.connection.sendCommand(c.get("value").getAsString());
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simpleoctets").warn("octets probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleoctets").warn("octets probe: {}", e.toString());
        }
    }

    /** On the server: the player at its block centre looking south, a smooth stone floor, the five bench cells. */
    private static void bench(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) return;
            ServerLevel level = player.level();
            BlockPos p = player.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-6, -1, -6), p.offset(6, 3, 6))) {
                level.setBlock(q, (q.getY() < p.getY() ? Blocks.SMOOTH_STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
            }
            player.teleportTo(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, java.util.Set.of(), 0.0F, 20.0F, true);
            CELLS.clear();
            CELLS.put("corner", p.offset(-1, 0, 3));
            CELLS.put("mixed", p.offset(1, 0, 3));
            CELLS.put("sneak", p.offset(-1, 0, -3));
            CELLS.put("melon", p.offset(1, 0, -3));
            CELLS.put("cracks", p.offset(3, 0, 0));
            CELLS.put("cracksMixed", p.offset(-3, 0, 0));
            Block oak = OctetRegistry.WOOD_OCTETS.getFirst();
            for (String name : new String[]{"sneak", "cracks"}) {
                level.setBlock(CELLS.get(name), OctetCellBlock.withMask(oak.defaultBlockState(), OctetCellBlock.BOTTOM_HALF), Block.UPDATE_ALL);
            }
            // a mixed cell (oak below, stone above): its break cracks are drawn by the module (OctetBreakCracks), not by the world
            Block stone = com.simplelib.api.OctetCells.cellFor(Blocks.STONE);
            BlockState[] parts = new BlockState[8];
            for (int i = 0; i < 8; i++) parts[i] = OctetCells.material((OctetCellBlock.y(i) == 0 ? oak : stone).defaultBlockState());
            OctetCells.set(level, CELLS.get("cracksMixed"), parts, false, Block.UPDATE_ALL);
        });
    }

    /** Turns the view to {@code target} from the client's current eye (standing or sneaking). */
    private static void aim(Minecraft mc, Vec3 target) {
        Vec3 d = target.subtract(mc.player.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.setYHeadRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
    }

    private static void writeStatus(Minecraft mc, Path dir) throws java.io.IOException {
        JsonObject o = new JsonObject();
        o.addProperty("tick", ticks);
        o.addProperty("ready", mc.level != null && mc.player != null);
        o.addProperty("screen", mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
        var w = mc.getWindow();
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("windowW", w.getScreenWidth());
        o.addProperty("windowH", w.getScreenHeight());
        if (mc.player != null) {
            Vec3 eye = mc.player.getEyePosition();
            o.add("eye", vec(eye));
            o.addProperty("pos", mc.player.blockPosition().toShortString());
            o.addProperty("sneaking", mc.player.isShiftKeyDown());
            o.addProperty("yaw", mc.player.getYRot());
            o.addProperty("pitch", mc.player.getXRot());
        }
        if (mc.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK && mc.level != null) {
            JsonObject hit = new JsonObject();
            BlockState state = mc.level.getBlockState(h.getBlockPos());
            hit.addProperty("pos", h.getBlockPos().toShortString());
            hit.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            hit.addProperty("face", h.getDirection().getSerializedName());
            hit.add("at", vec(h.getLocation()));
            if (state.getBlock() instanceof OctetCellBlock) hit.addProperty("index", OctetCellBlock.hitIndex(h.getBlockPos(), h));
            o.add("hit", hit);
        }
        var server = mc.getSingleplayerServer();
        if (server != null && mc.player != null) {
            try {
                JsonObject s = server.submit(() -> {
                    JsonObject r = new JsonObject();
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = player.level();
                    r.addProperty("held", player.getMainHandItem().isEmpty() ? ""
                            : BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()) + " x" + player.getMainHandItem().getCount());
                    r.addProperty("gameMode", player.gameMode.getGameModeForPlayer().getName());
                    JsonObject cells = new JsonObject();
                    for (Map.Entry<String, BlockPos> e : CELLS.entrySet()) {
                        BlockPos pos = e.getValue();
                        BlockState state = level.getBlockState(pos);
                        JsonObject c = new JsonObject();
                        JsonArray at = new JsonArray();
                        at.add(pos.getX());
                        at.add(pos.getY());
                        at.add(pos.getZ());
                        c.add("pos", at);
                        c.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                        c.addProperty("mask", state.getBlock() instanceof OctetCellBlock ? OctetCellBlock.mask(state) : -1);
                        JsonArray parts = new JsonArray();
                        if (state.getBlock() instanceof OctetCellBlock) {
                            for (BlockState part : OctetCells.parts(level, pos, state)) {
                                parts.add(part == null ? "" : BuiltInRegistries.BLOCK.getKey(part.getBlock()).toString());
                            }
                        }
                        c.add("parts", parts);
                        cells.add(e.getKey(), c);
                    }
                    r.add("cells", cells);
                    return r;
                }).get(2, TimeUnit.SECONDS);
                o.add("server", s);
            } catch (Exception e) {
                o.addProperty("serverError", e.toString());
            }
        }
        Path tmp = dir.resolve("simpleoctets-octets-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static JsonArray vec(Vec3 v) {
        JsonArray a = new JsonArray();
        a.add(v.x);
        a.add(v.y);
        a.add(v.z);
        return a;
    }
}
