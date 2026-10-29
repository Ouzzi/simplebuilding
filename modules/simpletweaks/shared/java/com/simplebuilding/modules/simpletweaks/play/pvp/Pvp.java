package com.simplebuilding.modules.simpletweaks.play.pvp;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * T9: personal PvP. /pvp on|off (no argument: status). A player can hurt another only when both have PvP on
 * ({@code ServerPlayer.canHarmPlayer}, which Vanilla also uses for arrows and tridents). After a switch the player waits
 * {@code cooldownSeconds} before switching again. Claims are checked first and keep precedence: this can only forbid more.
 */
public final class Pvp {
    static final Map<UUID, Long> TOGGLED = new HashMap<>();

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().pvp().enabled(); }

    public static boolean on(Player p) {
        var s = Play.state();
        String id = p.getUUID().toString();
        if (s.pvpOn.contains(id)) return true;
        if (s.pvpOff.contains(id)) return false;
        return Play.config().pvp().defaultOn();
    }
    /** False when this feature forbids {@code attacker} to hurt {@code target}. */
    public static boolean allows(Player attacker, Player target) {
        if (!active() || attacker == target) return true;
        return on(attacker) && on(target);
    }
    public static void set(ServerPlayer p, boolean on) {
        var s = Play.state();
        String id = p.getUUID().toString();
        s.pvpOn.remove(id); s.pvpOff.remove(id);
        (on ? s.pvpOn : s.pvpOff).add(id);
        TOGGLED.put(p.getUUID(), System.currentTimeMillis());
        s.save();
    }
    /** Seconds until the player may switch again (0 = now). */
    public static long waitSeconds(ServerPlayer p) {
        Long at = TOGGLED.get(p.getUUID());
        if (at == null) return 0;
        long left = Play.config().pvp().cooldownSeconds() * 1000L - (System.currentTimeMillis() - at);
        return left <= 0 ? 0 : left / 1000 + 1;
    }
    /** Leaving: drop the toggle time once its cooldown is over (a relog must not skip a running cooldown). */
    public static void forget(ServerPlayer p) { if (waitSeconds(p) == 0) TOGGLED.remove(p.getUUID()); }
    public static void resetCooldowns() { TOGGLED.clear(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.pvp().enabled()) return;
        d.register(Commands.literal("pvp")
                .executes(x -> { var p = x.getSource().getPlayerOrException(); x.getSource().sendSuccess(() -> PlayText.t(on(p) ? "pvp.is_on" : "pvp.is_off"), false); return on(p) ? 1 : 0; })
                .then(Commands.literal("on").executes(x -> toggle(x.getSource(), true)))
                .then(Commands.literal("off").executes(x -> toggle(x.getSource(), false))));
    }
    static int toggle(CommandSourceStack source, boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        if (!active()) return 0;
        if (on(p) == value) { source.sendSuccess(() -> PlayText.t(value ? "pvp.is_on" : "pvp.is_off"), false); return 1; }
        long wait = waitSeconds(p);
        if (wait > 0) { source.sendFailure(PlayText.t("pvp.cooldown", wait)); return 0; }
        set(p, value);
        source.sendSuccess(() -> PlayText.t(value ? "pvp.now_on" : "pvp.now_off").withStyle(value ? ChatFormatting.RED : ChatFormatting.GREEN), false);
        return 1;
    }
    private Pvp() {}
}
