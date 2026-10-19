package com.simplemaps.web;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplemaps.MapsConfig;
import com.simplemaps.SimpleMaps;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Server side of the web map: switches (config, autoOffWith), the running export (one at a time, a bounded number of chunks
 * per tick), the optional periodic export, and which players show on the map (opt-in, saved with the world).
 */
public final class WebMap {
    public enum Start { STARTED, DISABLED, AUTO_OFF, RUNNING }

    /** Summary of the last finished export (this server session only). */
    public record Last(long finishedMillis, int tiles, int dimensions, Path dir) {}

    private static MinecraftServer server;
    private static WebExport job;
    private static Last last;
    private static int lastStartTick;

    private WebMap() {}

    /** First id of {@code ids} that is loaded, or null (the same rule as Simple Tweaks' autoOffWith, rebuilt here). */
    public static String autoOffBy(List<String> ids, Predicate<String> loaded) {
        if (ids != null) for (String id : ids) if (loaded.test(id)) return id;
        return null;
    }

    /** The loaded mod that switches the web export off, or null. */
    public static String autoOff() {
        return autoOffBy(MapsConfig.webAutoOffWith, SimpleMaps.modLoaded);
    }

    /** Whether exports may run: module on, {@code webEnabled}, no web map mod of {@code webAutoOffWith} loaded. */
    public static boolean active() {
        return MapsConfig.enabled && MapsConfig.webEnabled && autoOff() == null;
    }

    /** {@code webExportDir}, or {@code <world>/simplemaps-web}; a relative path is relative to the world folder. */
    public static Path exportDir(MinecraftServer server) {
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String configured = MapsConfig.webExportDir;
        return configured == null || configured.isBlank() ? world.resolve("simplemaps-web") : world.resolve(configured).normalize();
    }

    public static WebExport running(MinecraftServer s) {
        return s == server ? job : null;
    }

    public static Last last(MinecraftServer s) {
        return s == server ? last : null;
    }

    public static Start start(MinecraftServer s) {
        attach(s);
        if (!MapsConfig.enabled || !MapsConfig.webEnabled) return Start.DISABLED;
        if (autoOff() != null) return Start.AUTO_OFF;
        if (job != null) return Start.RUNNING;
        job = WebExport.ofAllMaps(s, exportDir(s), MapsConfig.webHideClaims);
        lastStartTick = s.getTickCount();
        return Start.STARTED;
    }

    /** Once per server tick (all loaders). */
    public static void tick(MinecraftServer s) {
        attach(s);
        if (job != null) {
            if (!active()) {
                job = null;
                return;
            }
            job.step(MapsConfig.webChunksPerTick);
            if (job.done()) {
                last = new Last(System.currentTimeMillis(), job.tilesWritten(), job.dimensions(), job.dir());
                SimpleMaps.LOG.info("Simple Maps: web map exported ({} tiles, {} dimensions) to {}", last.tiles(), last.dimensions(), last.dir());
                job = null;
            }
            return;
        }
        int interval = MapsConfig.webIntervalMinutes;
        if (interval > 0 && active() && s.getTickCount() - lastStartTick >= interval * 1200) start(s);
    }

    /** A new server (world) resets the session state and logs the autoOffWith hit once. */
    private static void attach(MinecraftServer s) {
        if (s == server) return;
        server = s;
        job = null;
        last = null;
        lastStartTick = s.getTickCount();
        String mod = autoOff();
        if (mod != null && MapsConfig.webEnabled) {
            SimpleMaps.LOG.info("Simple Maps: web map export is switched off because mod '{}' is loaded (autoOffWith). To keep it on, empty webAutoOffWith in {}.",
                    mod, MapsConfig.FILE);
        }
    }

    /** Whether this player chose to show on the web map. */
    public static boolean visible(MinecraftServer s, UUID player) {
        return Players.get(s).ids.contains(player);
    }

    public static void setVisible(MinecraftServer s, UUID player, boolean show) {
        Players data = Players.get(s);
        if (show ? data.ids.add(player) : data.ids.remove(player)) data.setDirty();
    }

    /** Players who switched themselves on ({@code /simplemaps web show}); default is off for everybody. */
    public static final class Players extends SavedData {
        public static final Codec<Players> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.listOf().fieldOf("players").forGetter(p -> new ArrayList<>(p.ids))).apply(i, Players::new));
        public static final SavedDataType<Players> TYPE = new SavedDataType<>(SimpleMaps.id("web_players"), Players::new, CODEC,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        final Set<UUID> ids = new LinkedHashSet<>();

        public Players() {}

        private Players(List<UUID> ids) {
            this.ids.addAll(ids);
        }

        static Players get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(TYPE);
        }
    }
}
