package com.simplequestionary;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Questionary: the Questionary Table (Fragentisch) shows lore, use and a secret for any item. Module constants
 * and the few loader hooks the shared code needs; loader entry points call {@link #init} once and set the hooks.
 */
public final class SimpleQuestionary {
    public static final String MOD_ID = "simplequestionary";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Questionary");

    /** Server to one client (set by each loader; no-op until then, e.g. a client without the mod). */
    public static BiConsumer<ServerPlayer, CustomPacketPayload> toClient = (player, payload) -> {};
    /** Display name of a mod by its namespace, for the {@code {mod}} placeholder (set by each loader). */
    public static Function<String, String> modName = namespace -> namespace;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(Path configDir) {
        QuestionaryConfig.load(configDir);
    }

    private SimpleQuestionary() {}
}
