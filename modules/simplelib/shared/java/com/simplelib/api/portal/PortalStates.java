package com.simplelib.api.portal;

import com.mojang.serialization.Codec;
import com.simplelib.SimpleLib;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The state of every portal link that differs from its rule's start state (BACK22 core §5.1), saved once with the Overworld so
 * both ends of a link read the same entry - also across dimensions and while one end is unloaded. Link ids are longs chosen by
 * the owning mod (hash of its own salt and position); server thread only.
 */
public final class PortalStates extends SavedData {
    public static final Codec<PortalStates> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(PortalStates::new, PortalStates::save)
            .fieldOf("links").codec();
    public static final SavedDataType<PortalStates> TYPE = new SavedDataType<>(SimpleLib.id("portal_states"), PortalStates::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<Long, PortalState> states = new HashMap<>();

    public PortalStates() {}

    private PortalStates(Map<String, String> saved) {
        saved.forEach((k, v) -> {
            try {
                states.put(Long.parseLong(k), PortalState.valueOf(v));
            } catch (IllegalArgumentException ignored) {
                // an unknown entry of a newer version: dropped, the link falls back to its start state
            }
        });
    }

    private Map<String, String> save() {
        Map<String, String> out = new HashMap<>();
        states.forEach((k, v) -> out.put(Long.toString(k), v.name()));
        return out;
    }

    public static PortalStates of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PortalState get(MinecraftServer server, long link, PortalRule rule) {
        return of(server).states.getOrDefault(link, rule.initial());
    }

    public static void set(MinecraftServer server, long link, PortalRule rule, PortalState state) {
        PortalStates data = of(server);
        PortalState old = state == rule.initial() ? data.states.remove(link) : data.states.put(link, state);
        if (old != state) data.setDirty();
    }

    /** Test hook: number of stored deviations. */
    public int size() {
        return states.size();
    }
}
