package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.config.EarthServerConfig;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Earth (plan docs/ai/PLAN-B-EARTH24-2026-10-24.md, concept KONZEPT-SIMPLE-EARTH-2026-10-23.md): far above the
 * world the Overworld becomes a slightly rounded cube whose faces show Vanilla map colours sampled from the world generator.
 * Below the activation height nothing changes. The Fabric entry fills the network hooks.
 */
public final class SimpleEarth {
    public static final String MOD_ID = "simpleearth";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Earth");

    /** Server to one client (set by the loader; no-op until then). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> toClient = (player, payload) -> {};
    /** Whether a client can receive a payload type (has the mod). */
    public static BiPredicate<ServerPlayer, CustomPacketPayload.Type<?>> canSend = (player, type) -> false;
    /** Client to server (set by the loader's client setup). */
    public static Consumer<CustomPacketPayload> toServer = payload -> {};

    public static Path configDir = Path.of("config");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Own config switch and the super-mod switch ("Enable Simple Earth" in SimpleBuilding). */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && EarthServerConfig.enabled;
    }

    public static void init(Path dir) {
        configDir = dir;
        EarthServerConfig.load(dir);
        // CMD24: /simpleearth config (server options; every value is clamped by EarthServerConfig.apply).
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(EarthServerConfig::toJson, json -> {
            EarthServerConfig.apply(json);
            com.simplebuilding.modules.simpleearth.terrain.TileService.invalidate(); // biome votes / surface detail change the colours
        }).file(dir, EarthServerConfig.FILE).register();
    }

    /** Server start and /reload: the biome colour tables of all data packs (N7/N8), fresh tile caches. */
    public static void dataLoaded(net.minecraft.server.packs.resources.ResourceManager resources) {
        com.simplebuilding.modules.simpleearth.terrain.EarthColors.load(resources);
        com.simplebuilding.modules.simpleearth.terrain.TileService.invalidate();
    }

    private SimpleEarth() {}
}
