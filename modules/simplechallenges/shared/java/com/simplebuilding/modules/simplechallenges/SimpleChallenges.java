package com.simplebuilding.modules.simplechallenges;

import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Challenges (concept docs/ai/KONZEPT-A-WP24-2026-10-29.md, plan PLAN-A-CHALLENGES-2026-10-29.md): game modes chosen for
 * a world. S0-S2 contain the safety lock, the preset files and the Unlock mode (grid, enforcement, prices, credits).
 * <b>Safety lock:</b> without a chosen challenge in the world state ({@code core.Gate}) the mod changes nothing.
 */
public final class SimpleChallenges {
    public static final String MOD_ID = "simplechallenges";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Challenges");

    /** Server to one client (set by the loader; no-op until then). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> toClient = (player, payload) -> {};
    /** Whether a client can receive a payload type (has the mod). */
    public static BiPredicate<ServerPlayer, CustomPacketPayload.Type<?>> canSend = (player, type) -> false;

    public static Path configDir = Path.of("config");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Own config switch and the super-mod switch ("Enable Simple Challenges" in SimpleBuilding). */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && ChallengeServerConfig.enabled;
    }

    public static void init(Path dir) {
        configDir = dir;
        ChallengeServerConfig.load(dir);
        // CMD24: /simplechallenges config (server options; every value is clamped by ChallengeServerConfig.apply).
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(ChallengeServerConfig::toJson, ChallengeServerConfig::apply)
                .file(dir, ChallengeServerConfig.FILE).register();
    }

    private SimpleChallenges() {}
}
