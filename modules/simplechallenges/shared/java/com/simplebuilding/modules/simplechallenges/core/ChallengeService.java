package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.PresetParser;
import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import com.simplebuilding.modules.simplechallenges.sky.IslandPlan;
import com.simplebuilding.modules.simplechallenges.sky.SkyService;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import com.google.gson.JsonParser;
import java.io.Reader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Life cycle of a challenge. Safety lock: a world has a challenge only if its state says so ({@link ChallengeState#peek} never
 * creates the file); the only other way in is {@code defaultChallenge} for a brand-new world (game time 0, no state yet) or an
 * explicit admin command that the server config must allow.
 */
public final class ChallengeService {
    private ChallengeService() {}

    public enum Result { OK, UNKNOWN_PRESET, NOT_RUNNABLE, DISABLED }

    /** Server start and /reload: presets and the block value table. */
    public static void loadData(ResourceManager resources) {
        PresetRegistry.load(resources, SimpleChallenges.configDir);
        BlockValues table = null;
        try {
            Identifier id = SimpleChallenges.id("simplechallenges/block_value.json");
            Resource r = resources.getResource(id).orElse(null);
            if (r != null) {
                try (Reader in = r.openAsReader()) {
                    table = BlockValues.parse(JsonParser.parseReader(in).getAsJsonObject());
                }
            }
        } catch (Exception e) {
            SimpleChallenges.LOG.warn("Broken block_value.json: {}", e.toString());
        }
        BlockValues.use(table);
    }

    /** Server started: resumes the saved challenge, or applies defaultChallenge to a brand-new world. */
    public static void started(MinecraftServer server) {
        loadData(server.getResourceManager());
        ChallengeRuntime.clear();
        Enforcement.clear();
        String chosen = PendingChallenge.take(); // the world creation screen's choice reaches only the world it created
        if (!SimpleChallenges.on()) return;
        ChallengeState state = ChallengeState.peek(server);
        if (state != null) {
            if (state.activated()) resume(server, state);
            return;
        }
        String wanted = chosen != null ? chosen : ChallengeServerConfig.defaultChallenge;
        if (!"none".equals(wanted) && server.overworld().getGameTime() == 0) {
            Result r = activate(server, wanted, true);
            SimpleChallenges.LOG.info("{} '{}' for the new world: {}", chosen != null ? "Chosen challenge" : "defaultChallenge", wanted, r);
        }
    }

    private static void resume(MinecraftServer server, ChallengeState state) {
        PresetParser.Result parsed = PresetParser.parse(state.challenge, state.presetJson);
        if (!parsed.ok() || !parsed.preset().runnable()) {
            SimpleChallenges.LOG.warn("The saved challenge '{}' cannot be read; the mod stays inactive in this world", state.challenge);
            return;
        }
        Active a = new Active(server, parsed.preset(), state);
        probeAll(a);
        ChallengeRuntime.install(a);
        SimpleChallenges.LOG.info("Challenge '{}' resumed: {} cells free in the Overworld", state.challenge, a.grid("minecraft:overworld") == null ? 0 : a.grid("minecraft:overworld").size());
    }

    /** Chooses a challenge for the running world: start area, probe, deadlock check, sync. */
    public static Result activate(MinecraftServer server, String presetId) {
        return activate(server, presetId, false);
    }

    /**
     * @param fresh the world is brand new: the world spawn moves to block 0,0 (scatter 0), above the island for a Skyblock preset.
     *              A world that was already played keeps its spawn.
     */
    public static Result activate(MinecraftServer server, String presetId, boolean fresh) {
        Preset preset = PresetRegistry.get(presetId);
        if (preset == null) return Result.UNKNOWN_PRESET;
        if (!preset.runnable()) return Result.NOT_RUNNABLE;
        ChallengeState state = ChallengeState.create(server);
        state.reset();
        state.challenge = presetId;
        state.presetJson = preset.json();
        state.initialized = true;
        state.seed = server.overworld().getSeed();
        state.lastUnlockTick = server.overworld().getGameTime();
        Active a = new Active(server, preset, state);
        for (String dim : a.dimensions()) {
            UnlockGrid g = a.grid(dim);
            for (int[] r : preset.unlock().start()) g.addRect(r[0], r[1], r[2], r[3]);
            g.unlock(0, 0); // spawn is fixed at 0,0, so its cell is always free
        }
        SkyService.start(a); // before the gate is installed: the start may stand in cells that are still locked
        probeAll(a);
        UnlockService.ensureSolvable(a);
        if (fresh) Spawn.setAtOrigin(server, preset.world() != null ? SkyService.ISLAND_Y + IslandPlan.standY() : Spawn.surfaceY(server.overworld()));
        state.setDirty();
        ChallengeRuntime.install(a);
        Enforcement.clear();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) ChallengeSync.sendFull(p, a);
        return Result.OK;
    }

    /** Back to no challenge: the state is reset, the hooks go silent at once, clients forget the borders. */
    public static void deactivate(MinecraftServer server) {
        ChallengeRuntime.clear();
        Enforcement.clear();
        LookInfo.clear();
        ChallengeState state = ChallengeState.peek(server);
        if (state != null) state.reset();
        ChallengeSync.sendEnd(server);
    }

    public static void stopped() {
        ChallengeRuntime.clear();
        Enforcement.clear();
    }

    /** The unlocked area's surface items (price pool of the easy tier). */
    private static void probeAll(Active a) {
        a.addAvailable(SkyService.reachable(a));
        for (String dim : a.dimensions()) {
            UnlockGrid g = a.grid(dim);
            var level = a.level(dim);
            if (g == null || level == null) continue;
            for (long k : g.keys()) a.addAvailable(LevelProbe.cell(level, a.mode.cell(), UnlockGrid.keyX(k), UnlockGrid.keyZ(k)).reachable());
        }
    }

    public static void join(ServerPlayer player) {
        Active a = ChallengeRuntime.server();
        if (a != null && SimpleChallenges.on()) ChallengeSync.sendFull(player, a);
    }

    public static void leave(ServerPlayer player) {
        Enforcement.forget(player.getUUID());
        LookInfo.forget(player.getUUID());
    }

    /** Server tick: borders every tick, pity every second, credits every two seconds. */
    public static void tick(MinecraftServer server) {
        Active a = ChallengeRuntime.server();
        if (a == null || !SimpleChallenges.on()) return;
        Enforcement.tick(server);
        SkyService.tick(a);
        int t = server.getTickCount();
        if (t % 10 == 3) LookInfo.tick(a);
        if (t % 20 == 10) UnlockService.pity(a);
        if (t % 40 == 5) Credits.poll(a);
    }
}
