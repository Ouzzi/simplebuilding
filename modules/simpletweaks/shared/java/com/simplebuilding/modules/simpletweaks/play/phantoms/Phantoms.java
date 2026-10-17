package com.simplebuilding.modules.simpletweaks.play.phantoms;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import net.minecraft.commands.*;
import net.minecraft.server.level.ServerPlayer;

/**
 * T30: /phantoms off|on per player. Opted-out players count like spectators in the Vanilla {@code PhantomSpawner}, so
 * no phantoms spawn for them; everybody else keeps Vanilla phantoms (and the rule {@code spawn_phantoms} still rules all).
 */
public final class Phantoms {
    public static boolean active() { return Play.on() && Play.server() != null && Play.config().phantoms().enabled(); }
    public static boolean off(ServerPlayer p) { return active() && Play.state().phantomsOff.contains(p.getUUID().toString()); }

    public static void set(ServerPlayer p, boolean on) {
        var s = Play.state();
        if (on) s.phantomsOff.remove(p.getUUID().toString()); else s.phantomsOff.add(p.getUUID().toString());
        s.save();
    }
    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.phantoms().enabled()) return;
        d.register(Commands.literal("phantoms")
                .executes(x -> { var p = x.getSource().getPlayerOrException(); x.getSource().sendSuccess(() -> PlayText.t(off(p) ? "phantoms.is_off" : "phantoms.is_on"), false); return off(p) ? 0 : 1; })
                .then(Commands.literal("on").executes(x -> { var p = x.getSource().getPlayerOrException(); set(p, true); x.getSource().sendSuccess(() -> PlayText.t("phantoms.is_on"), false); return 1; }))
                .then(Commands.literal("off").executes(x -> { var p = x.getSource().getPlayerOrException(); set(p, false); x.getSource().sendSuccess(() -> PlayText.t("phantoms.is_off"), false); return 1; })));
    }
    private Phantoms() {}
}
