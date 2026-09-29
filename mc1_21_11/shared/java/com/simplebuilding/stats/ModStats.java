package com.simplebuilding.stats;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

/**
 * The mod's own player statistics. Registered as custom stats like vanilla's
 * {@code minecraft:jump}, so they show up in the vanilla statistics screen (General tab), in
 * {@code stats/<uuid>.json} and as scoreboard criteria
 * ({@code minecraft.custom:simplebuilding.wand_blocks_placed}). Names:
 * {@code stat.simplebuilding.<name>} in the language files.
 */
public final class ModStats {

    /** Blocks the building wand placed: plane, line, bridge and every block of a blueprint or octant build. */
    public static final Identifier WAND_BLOCKS_PLACED = id("wand_blocks_placed");
    /** Blocks the chisel or spatula moved one step along their chain. */
    public static final Identifier CHISEL_USES = id("chisel_uses");
    /** Teleports by the mod: spawn teleporter and echo sounder. */
    public static final Identifier TELEPORTS = id("teleports");

    public static final List<Identifier> ALL = List.of(WAND_BLOCKS_PLACED, CHISEL_USES, TELEPORTS);

    private ModStats() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("simplebuilding", path);
    }

    /** Registers the stats; called once per loader while the custom stat registry is open. */
    public static void register() {
        for (Identifier stat : ALL) {
            Registry.register(BuiltInRegistries.CUSTOM_STAT, stat, stat);
            Stats.CUSTOM.get(stat, StatFormatter.DEFAULT);
        }
    }

    /** Adds one to {@code stat} for {@code player}; a no-op on the client or for an unregistered stat. */
    public static void award(Player player, Identifier stat) {
        if (player instanceof ServerPlayer serverPlayer && BuiltInRegistries.CUSTOM_STAT.getKey(stat) != null) {
            serverPlayer.awardStat(stat);
        }
    }
}
