package com.simplebuilding.modules.simpletweaks.play.shield;

import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

/**
 * T12: newcomer protection (plan docs/ai/PLAN-B-T12PVP-2026-10-22.md). While a player's online play time in this world
 * (Vanilla statistic {@code play_time}) is below {@code minutes}, no player hurts them and they hurt no player: melee,
 * arrows and tridents ({@code ServerPlayer.canHarmPlayer}) and harmful potion effects of players, their projectiles and
 * clouds. It ends once for good (world state {@code shieldEnded}): when the time is up or, with {@code attackEnds}, when
 * the protected player attacks a player. Only real players: fake players and GameTest mock players are subclasses of
 * {@link ServerPlayer} and never get it. Claims and T9 stay untouched; this only forbids more.
 */
public final class NewPlayerShield {
    /** Online players that were told their protection runs (end message pending). */
    static final Set<UUID> SHOWN = new HashSet<>();
    /** Tests: mock players treated as real players. */
    public static final Set<UUID> TEST_PLAYERS = new HashSet<>();

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().newPlayerShield().enabled(); }

    static boolean real(ServerPlayer p) { return p.getClass() == ServerPlayer.class || TEST_PLAYERS.contains(p.getUUID()); }

    /** Milliseconds of protection left (0 = none). */
    public static long leftMillis(Player player) {
        if (!active() || !(player instanceof ServerPlayer p) || !real(p) || Play.state().shieldEnded.contains(p.getUUID().toString())) return 0;
        long left = Play.config().newPlayerShield().minutes() * 1200L - p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        return left <= 0 ? 0 : left * 50;
    }
    public static boolean shielded(Player p) { return leftMillis(p) > 0; }

    /** False when this feature forbids {@code attacker} to hurt {@code target}. */
    public static boolean allows(Player attacker, Player target) {
        return attacker == target || !shielded(attacker) && !shielded(target);
    }

    /** {@code target} is about to be hurt ({@code ServerPlayer.hurtServer} head). */
    public static void hurt(ServerPlayer target, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer attacker) attempt(attacker, target);
    }
    /** True when a harmful effect from {@code source} (a player, its projectile or cloud) may not reach {@code target}. */
    public static boolean blocksEffect(LivingEntity target, MobEffectInstance effect, Entity source) {
        if (!(target instanceof ServerPlayer t) || effect.getEffect().value().isBeneficial() || !active()) return false;
        Entity owner = source instanceof TraceableEntity traced ? traced.getOwner() : source;
        if (!(owner instanceof ServerPlayer attacker) || attacker == t) return false;
        attempt(attacker, t);
        return !allows(attacker, t);
    }
    /** {@code attacker} tries to harm {@code target}: may end the attacker's protection, tells about the target's. */
    static void attempt(ServerPlayer attacker, ServerPlayer target) {
        if (attacker == target || !active()) return;
        if (shielded(attacker)) {
            if (!Play.config().newPlayerShield().attackEnds()) return;
            end(attacker, "shield.ended_attack");
        }
        long left = leftMillis(target);
        if (left > 0) attacker.sendSystemMessage(PlayText.t("shield.protected", PlayText.duration(left)), true);
    }

    public static void joined(ServerPlayer p) {
        long left = leftMillis(p);
        if (left <= 0) return;
        SHOWN.add(p.getUUID());
        var line = PlayText.t("shield.start", PlayText.duration(left).withStyle(ChatFormatting.GREEN));
        if (Play.config().newPlayerShield().attackEnds()) line.append(" ").append(PlayText.t("shield.attack_ends"));
        p.sendSystemMessage(line.withStyle(ChatFormatting.AQUA));
    }
    /** Once a second: protections that ran out end with a message. */
    public static void tick(MinecraftServer s) { if (s.getTickCount() % 20 == 0) check(s); }
    public static void check(MinecraftServer s) {
        if (SHOWN.isEmpty() || !active()) return;
        for (var id : List.copyOf(SHOWN)) {
            var p = s.getPlayerList().getPlayer(id);
            if (p == null) SHOWN.remove(id);
            else if (!shielded(p)) end(p, "shield.ended");
        }
    }
    static void end(ServerPlayer p, String key) {
        SHOWN.remove(p.getUUID());
        var s = Play.state();
        if (s.shieldEnded.add(p.getUUID().toString())) s.save();
        p.sendSystemMessage(PlayText.t(key).withStyle(ChatFormatting.YELLOW));
    }
    public static void reset() { SHOWN.clear(); }
    private NewPlayerShield() {}
}
