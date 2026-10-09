package com.simplemaps;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants and the few loader hooks the shared code needs. Loader entry points call {@link #init} once and
 * set the senders; the client entry points set the screen openers (never touched on a dedicated server).
 */
public final class SimpleMaps {
    public static final String MOD_ID = "simplemaps";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Maps");

    /** Server to one client (set by each loader; no-op until then, e.g. a client without the mod). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> toClient = (player, payload) -> {};
    /** Client to server (set by each loader's client entry). */
    public static Consumer<CustomPacketPayload> toServer = payload -> {};
    /** Opens the map screen for the map in this hand (client entry). */
    public static Consumer<InteractionHand> openHand = hand -> {};
    /** Opens the map screen for the map in the item frame with this entity id (client entry). */
    public static IntConsumer openFrame = entityId -> {};

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(Path configDir) {
        MapsConfig.load(configDir);
    }

    private SimpleMaps() {}
}
