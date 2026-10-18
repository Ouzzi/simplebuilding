package com.simplebuilding.modules.simpletweaks.play;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registers the commands of every enabled player/world/event feature. {@code /tweaks} carries no requirement at its
 * root (Brigadier merges equal roots; the admin features add their own branches), each branch checks its own level.
 */
public final class PlayCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) { register(d, Play.config()); }
    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        com.simplebuilding.modules.simpletweaks.play.coords.Coords.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.seen.Playtime.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.pvp.Pvp.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.events.ServerEvents.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.top.Leaderboard.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.vote.Votes.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.afk.Afk.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.border.Borders.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.phantoms.Phantoms.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.cleanup.GroundCleanup.register(d, c);
        com.simplebuilding.modules.simpletweaks.play.pregen.Pregen.register(d, c);
        com.simplebuilding.modules.simpletweaks.TweakCommands.alias(d, "coords", "playtime", "seen", "pvp", "vote", "afk", "leaderboard", "phantoms", "pregen");
    }
    private PlayCommands() {}
}
