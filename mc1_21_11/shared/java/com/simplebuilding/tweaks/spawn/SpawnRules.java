package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.tweaks.SimpleTweaks;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Regeln hinter den ServerPlayer-Mixins: gesperrte Dimensionen und exakter Spawn. */
public final class SpawnRules {
    /**
     * Abstand der Sperr-Meldung je Spieler: wer im Netherportal steht, versucht den Wechsel jeden
     * Tick erneut (Audit 2026-09-26 #51).
     */
    public static final int LOCK_MESSAGE_COOLDOWN_TICKS = 40;

    /** Tiefe der laufenden Befehls-Teleports ({@code /tp}, {@code /execute in ... run tp}); nur Server-Thread. */
    private static int commandTeleportDepth;
    /**
     * Letzte Sperr-Meldung je Spieler-Objekt (Spielzeit der Zielwelt); schwach gehalten, damit
     * abgemeldete Spieler ohne eigenen Logout-Haken verschwinden.
     */
    private static final Map<ServerPlayer, Long> LAST_LOCK_MESSAGE = new WeakHashMap<>();

    private SpawnRules() {
    }

    public static boolean forceExactSpawn() {
        return SimpleTweaks.config().spawn.forceExactSpawn;
    }

    /**
     * Fuehrt einen Befehls-Teleport aus, an dem die Dimensionssperre vorbeigeht (Audit #51: die Sperre
     * blockierte auch Operatoren per {@code /tp} und {@code /execute in}).
     */
    public static boolean asCommandTeleport(BooleanSupplier teleport) {
        commandTeleportDepth++;
        try {
            return teleport.getAsBoolean();
        } finally {
            commandTeleportDepth--;
        }
    }

    /** true = Wechsel in diese Welt abbrechen (Nether/End per Config gesperrt), mit Meldung. */
    public static boolean blocksDimensionChange(ServerPlayer player, @Nullable ServerLevel destination) {
        if (destination == null || destination == player.level() || commandTeleportDepth > 0) {
            return false;
        }
        if (destination.dimension() == Level.NETHER && !SimpleTweaks.config().dimensions.allowNether) {
            lockMessage(player, destination, "message.simplebuilding.dimension.nether_disabled");
            return true;
        }
        if (destination.dimension() == Level.END && !SimpleTweaks.config().dimensions.allowEnd) {
            lockMessage(player, destination, "message.simplebuilding.dimension.end_disabled");
            return true;
        }
        return false;
    }

    /**
     * Die Sperr-Meldung, hoechstens alle {@link #LOCK_MESSAGE_COOLDOWN_TICKS} Ticks je Spieler.
     *
     * @return ob sie geschickt wurde
     */
    public static boolean lockMessage(ServerPlayer player, ServerLevel destination, String key) {
        long now = destination.getGameTime();
        Long last = LAST_LOCK_MESSAGE.get(player);
        if (last != null && now >= last && now - last < LOCK_MESSAGE_COOLDOWN_TICKS) {
            return false;
        }
        LAST_LOCK_MESSAGE.put(player, now);
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** Fuer Tests: die Meldungssperre eines Spielers vergessen. */
    public static void forget(ServerPlayer player) {
        LAST_LOCK_MESSAGE.remove(player);
    }

    /**
     * Exakter Wiedereinstieg: im Bett genau auf der Bettmitte, ohne eigenen Wiedereinstiegspunkt genau
     * auf der Mitte des Weltspawn-Blocks. null = unveraendert lassen.
     *
     * <p>Die Bettposition kommt aus {@link ServerPlayer#getRespawnConfig()}: Vanilla liefert als Ziel
     * die Aufstehposition NEBEN dem Bett, der Block dort war also fast nie ein Bett (Audit #35).
     */
    public static @Nullable TeleportTransition exactRespawn(ServerPlayer player, @Nullable TeleportTransition original,
                                                            TeleportTransition.PostTeleportTransition post) {
        if (!forceExactSpawn() || original == null) {
            return null;
        }
        ServerLevel level = original.newLevel();
        ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
        if (respawn != null) {
            BlockPos bed = respawn.respawnData().pos();
            if (respawn.respawnData().dimension() == level.dimension() && level.getBlockState(bed).getBlock() instanceof BedBlock) {
                Vec3 exact = new Vec3(bed.getX() + 0.5, bed.getY() + 0.5625, bed.getZ() + 0.5);
                return new TeleportTransition(level, exact, Vec3.ZERO, original.yRot(), original.xRot(), post);
            }
            return null;
        }
        BlockPos spawn = level.getRespawnData().pos();
        Vec3 exact = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        return new TeleportTransition(level, exact, Vec3.ZERO, original.yRot(), original.xRot(), post);
    }
}
