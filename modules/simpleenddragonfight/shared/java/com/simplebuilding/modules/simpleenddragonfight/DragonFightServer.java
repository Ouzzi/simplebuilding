package com.simplebuilding.modules.simpleenddragonfight;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * Server hook (end of every server tick, all loaders): one {@link FightSession} for the End's main dragon fight. Custom
 * dragons of other mods are left alone: only the dragon whose UUID the End's {@link EnderDragonFight} holds is managed.
 * The flight-speed and damage factors per managed dragon are read by {@code EnderDragonMixin}.
 */
public final class DragonFightServer {
    private static final FightSession END = new FightSession();
    /** dragon UUID -> {damage-taken factor, speed factor, attack factor}; only managed dragons have an entry. */
    private static final Map<UUID, float[]> FACTORS = new ConcurrentHashMap<>();

    private DragonFightServer() {}

    public static FightSession endSession() { return END; }

    public static void tick(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        EnderDragonFight fight = end.getDragonFight();
        EnderDragon dragon = null;
        if (fight != null && fight.dragonUUID() != null) {
            Entity e = end.getEntity(fight.dragonUUID());
            if (e instanceof EnderDragon d && d.isAlive()) dragon = d;
        }
        if (dragon != null) END.setSummons(DragonSummons.get(server).record(dragon.getUUID(), fight.hasPreviouslyKilledDragon()));
        BlockPos center = dragon != null ? EnderDragonFight.getPodiumLocation(dragon.getFightOrigin()) : BlockPos.ZERO;
        END.tick(end, dragon, center, end.players(), end.getGameTime());
    }

    static void setFactors(UUID dragon, float damage, float speed, float attack) { FACTORS.put(dragon, new float[] {damage, speed, attack}); }

    static void clearFactors(UUID dragon) { FACTORS.remove(dragon); }

    /** Damage multiplier of a managed dragon (1 if unmanaged or the module is off). */
    public static float damageFactor(EnderDragon dragon) {
        float[] f = DragonFightRegistry.on() ? FACTORS.get(dragon.getUUID()) : null;
        return f == null ? 1.0f : f[0];
    }

    /** Flight-speed multiplier of a managed dragon; sitting phases keep Vanilla timing. */
    public static float speedFactor(EnderDragon dragon) {
        float[] f = DragonFightRegistry.on() ? FACTORS.get(dragon.getUUID()) : null;
        if (f == null || dragon.getPhaseManager().getCurrentPhase().isSitting()) return 1.0f;
        return f[1];
    }

    /** Multiplier on the damage a managed dragon deals (summonings; 1 if unmanaged or the module is off). */
    public static float attackFactor(EnderDragon dragon) {
        float[] f = DragonFightServer.factors(dragon);
        return f == null ? 1.0f : f[2];
    }

    private static float[] factors(EnderDragon dragon) {
        return DragonFightRegistry.on() ? FACTORS.get(dragon.getUUID()) : null;
    }
}
