package com.simplemusic;

import com.simplemusic.config.MusicConfig;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Music module constants and loader hooks (plan docs/ai/PLAN-B-MUSIC-2026-10-20.md, concepts
 * KONZEPT-NOTENBLATT-2026-10-19.md and KONZEPT-MUSIKPLATTE-UPLOAD-2026-10-19.md). Loader entry points call {@link #init}
 * once with their config directory and fill the network hooks.
 */
public final class SimpleMusic {
    public static final String MOD_ID = "simplemusic";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Music");

    /** Server to one client (set by the loader; no-op until then). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> toClient = (player, payload) -> {};
    /** Client to server (set by the loader's client setup). */
    public static Consumer<CustomPacketPayload> toServer = payload -> {};
    /** Whether a mod is loaded (set by the loader). */
    public static Predicate<String> modLoaded = id -> false;
    /** Client screen hook: open the sheet editor for the stack in {@code hand} (0 main, 1 off). */
    public static Consumer<Integer> openSheet = hand -> {};

    /** The loader's config directory (sheet import and upload folders live below it). */
    public static Path configDir = Path.of("config");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Own config switch (and a super-mod switch, should a super-mod ever bundle this module). */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && MusicConfig.enabled;
    }

    public static void init(Path dir) {
        configDir = dir;
        MusicConfig.load(dir);
        registerIdMoves();
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "music", "simplemusic.testcentre.station",
                java.util.stream.Stream.concat(java.util.stream.Stream.of("simplemusic:sheet_music", "simplemusic:disc_press",
                        "simplemusic:blank_disc", "simplemusic:custom_disc", "simplemusic:guide_book", "simplemusic:pocket_jukebox"),
                        java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values()).map(c -> "simplemusic:" + c.getSerializedName() + "_pocket_jukebox"))
                        .toList());
    }

    /**
     * B23-M9 (owner 23.10.): the Orchestrion is gone - sheets only go into the disc press, discs into the jukebox and the
     * Pocket Jukebox. Old worlds get a Vanilla jukebox in its place (item, block and block entity); a disc or sheet that was
     * in it stays in the jukebox ({@code JukeboxBlockEntityMixin}).
     */
    public static void registerIdMoves() {
        Identifier jukebox = Identifier.withDefaultNamespace("jukebox");
        com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.BLOCK, id("orchestrion"), jukebox);
        com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ITEM, id("orchestrion"), jukebox);
        com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, id("orchestrion"), jukebox);
    }

    private SimpleMusic() {}
}
