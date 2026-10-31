package com.simpleresonance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

/**
 * Takeover of the old SimpleBuilding tracker block (MOB27-3 stage 1: nothing is lost, nothing is deleted). SimpleBuilding's
 * {@code SurvivalTracerMixin} saved the resonance basis in the player's data as the compound {@code SimpleBuildingData}
 * (BaseDist, BaseTime, BaseHostile, BasePassive, BaseDamage, BaseXp, ActiveTicks, TotalHostile, TotalPassive). {@code ServerPlayerLegacyMixin}
 * parks the compound here when a player's data is read; the first Resonance tick of that player turns it into a {@link PlayerRecord}
 * once. The core keeps writing its block, so an older SimpleBuilding keeps working.
 */
public final class LegacyImport {
    public static final String KEY = "SimpleBuildingData";
    private static final Map<UUID, CompoundTag> PENDING = new ConcurrentHashMap<>();

    private LegacyImport() {}

    /** Parks the old block of a player until his first tick. */
    public static void park(UUID player, CompoundTag block) {
        PENDING.put(player, block);
    }

    /** The parked block of a player (and forgets it), {@code null} when there is none. */
    public static @Nullable CompoundTag take(UUID player) {
        return PENDING.remove(player);
    }

    public static void clear() {
        PENDING.clear();
    }

    /**
     * Fills {@code r} from the old block: the F baselines and totals are taken over, the long-term points start at the old active time
     * (one point per 30 active minutes), so nobody starts from nothing.
     * Blocks from before the active counter (no {@code ActiveTicks}) start at 0 active time like the core did.
     */
    public static void apply(PlayerRecord r, CompoundTag block) {
        boolean hasActive = block.contains("ActiveTicks");
        r.active = hasActive ? block.getIntOr("ActiveTicks", 0) : 0;
        r.baseActive = hasActive ? block.getIntOr("BaseTime", 0) : 0;
        r.baseDistance = block.getIntOr("BaseDist", 0);
        r.baseHostile = block.getIntOr("BaseHostile", 0);
        r.basePassive = block.getIntOr("BasePassive", 0);
        r.baseDamage = block.getIntOr("BaseDamage", 0);
        r.baseXp = block.getIntOr("BaseXp", 0);
        r.hostile = block.getIntOr("TotalHostile", 0);
        r.passive = block.getIntOr("TotalPassive", 0);
        r.points = r.active / ResonanceMath.TICKS_PER_POINT;
        r.legacyImported = true;
    }
}
