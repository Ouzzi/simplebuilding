package com.simplebuilding.modules.simpletweaks.admin;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

/** Name or UUID to a profile: online players first, then the server's name cache; a bare UUID always resolves. */
public final class AdminPlayers {
    public static NameAndId resolve(MinecraftServer server, String input) {
        var online = server.getPlayerList().getPlayerByName(input);
        if (online != null) return online.nameAndId();
        try {
            var id = UUID.fromString(input);
            var p = server.getPlayerList().getPlayer(id);
            if (p != null) return p.nameAndId();
            return server.services().nameToIdCache().get(id).orElse(new NameAndId(id, input));
        } catch (IllegalArgumentException notUuid) {
            return server.services().nameToIdCache().get(input).orElse(null);
        }
    }
    private AdminPlayers() {}
}
