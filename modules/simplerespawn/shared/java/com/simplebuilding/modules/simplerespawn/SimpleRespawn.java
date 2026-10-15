package com.simplebuilding.modules.simplerespawn;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Respawn: lethal damage knocks a player down instead of killing them; a teammate can revive them before they
 * bleed out (plan docs/ai/PLAN-SIMPLE-RESPAWN-2026-10-12.md). Playable alone; with SimpleBuilding it is a bundled
 * optional sub-mod switched by "Enable Simple Respawn".
 */
public final class SimpleRespawn {
    public static final String ID = "simplerespawn";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    /** Server: sends a payload to one player when that client can receive it (set by the loader entry). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> send = (player, payload) -> {};
    /** Client side: the players the server reported as downed (pose and interaction prediction). */
    public static final Set<UUID> CLIENT_DOWNED = ConcurrentHashMap.newKeySet();
    /** Client side: bleed-out ticks left per downed player (last server value, counted down locally). */
    public static final java.util.Map<UUID, Integer> CLIENT_DOWNED_TICKS = new ConcurrentHashMap<>();
    /** Locator-bar color of a downed player (red); restored when the state ends. */
    public static final int DOWNED_COLOR = 0xFF4040;

    private SimpleRespawn() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    /** Super-mod switch "Enable Simple Respawn" (framework SubMods) and the module's own config switch. */
    public static boolean on() {
        return RespawnConfig.enabled && com.simplebuilding.framework.api.SubMods.enabled(ID);
    }

    /** Name tag above a downed player: the name and the bleed-out time left (m:ss), all red. */
    public static net.minecraft.network.chat.Component downedNameTag(net.minecraft.network.chat.Component name, int ticksLeft) {
        int seconds = (Math.max(0, ticksLeft) + 19) / 20;
        return net.minecraft.network.chat.Component.empty()
                .append(name.copy().withStyle(net.minecraft.ChatFormatting.RED))
                .append(net.minecraft.network.chat.Component.literal(" " + seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60)
                        .withStyle(net.minecraft.ChatFormatting.RED));
    }

    /** Downed on this side: the server's own state, on the client the last list the server sent. */
    public static boolean isDowned(Player player) {
        if (player == null) return false;
        return player.level().isClientSide() ? CLIENT_DOWNED.contains(player.getUUID()) : RespawnServer.isDowned(player);
    }
}
