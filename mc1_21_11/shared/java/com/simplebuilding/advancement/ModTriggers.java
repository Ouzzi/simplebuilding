package com.simplebuilding.advancement;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The mod's own advancement triggers ({@link FeatureTrigger}, {@code simplebuilding:feature_used}, and
 * {@link CounterTrigger}, {@code simplebuilding:counter}) and the names of the actions they report. The advancement tree (ModAdvancementProvider, datagen) waits
 * for these names; the game code calls {@link #feature} at the moment an action succeeds.
 *
 * <p>Loader-neutral: Fabric registers through {@code TweaksContent.init()}, NeoForge and Forge from
 * their {@code RegisterEvent} for {@code minecraft:trigger_type}. 1.21.11 keeps an identical copy.
 */
public final class ModTriggers {
    private ModTriggers() {
    }

    public static final Identifier FEATURE_USED_ID = Identifier.fromNamespaceAndPath("simplebuilding", "feature_used");
    public static final FeatureTrigger FEATURE_USED = new FeatureTrigger();

    /** Reinforced machine hammered into its netherite tier ({@code SledgehammerUpgrades#finish}). */
    public static final String HAMMER_UPGRADE_NETHERITE = "hammer_upgrade_netherite";
    /** Netherite machine hammered into its enderite tier. */
    public static final String HAMMER_UPGRADE_ENDERITE = "hammer_upgrade_enderite";
    /** Sledgehammer charge reshaped a block (full block - stairs - slab). */
    public static final String HAMMER_RESHAPE = "hammer_reshape";
    /** Sledgehammer charge crushed a Block of Diamond into pebbles. */
    public static final String DIAMOND_CRUSH = "diamond_crush";
    /** Sledgehammer hit turned a framed trim template into a glowing/emitting one. */
    public static final String TRIM_TEMPLATE_FORGED = "trim_template_forged";
    /** Chisel moved a block one step along its chain. */
    public static final String CHISEL = "chisel";
    /** Building wand started filling a plane. */
    public static final String WAND_BUILD = "wand_build";
    /** Octant marked a corner. */
    public static final String OCTANT_MARK = "octant_mark";
    /** Rotator turned a placed block. */
    public static final String ROTATE = "rotate";
    /** Scan result taken from a cartography table (Octant on top, blank Blueprint below). */
    public static final String BLUEPRINT_SCAN = "blueprint_scan";
    /** Copy taken from a cartography table (signed Blueprint on top, blank one below). */
    public static final String BLUEPRINT_COPY = "blueprint_copy";
    /** A signed Blueprint was built to the end with the building wand. */
    public static final String BLUEPRINT_BUILD = "blueprint_build";
    /** A launchpad flung the player up. */
    public static final String LAUNCHPAD = "launchpad";
    /** A flypad let the player fly. */
    public static final String FLYPAD = "flypad";
    /** An elytra pad lent the player its wings. */
    public static final String ELYTRA_PAD = "elytra_pad";
    /** A spawn teleporter carried the player to its target. */
    public static final String SPAWN_TELEPORT = "spawn_teleport";
    /** A potion pad gave the player its effect. */
    public static final String POTION_PAD = "potion_pad";
    /** The echo sounder jumped the player onto its lodestone. */
    public static final String ECHO_TELEPORT = "echo_teleport";
    /** The amethyst lens beam changed a block (melted, lit, dried, set alight). */
    public static final String LENS_BEAM = "lens_beam";
    /** The ore detector pinged an ore. */
    public static final String ORE_DETECTED = "ore_detected";
    /** Vein Miner broke at least one extra block of a vein ({@code VeinMinerUsageEvent}). */
    public static final String VEIN_MINE = "vein_mine";
    /** Strip Miner broke at least one extra block of its tunnel ({@code StripMinerUsageEvent}). */
    public static final String STRIP_MINE = "strip_mine";
    /** Versatility swapped a better tool into the hand ({@code VersatilityUsageEvent}). */
    public static final String VERSATILITY_SWAP = "versatility_swap";
    /** The server accepted an air jump ({@code ModMessageHandlers#handleDoubleJump}). */
    public static final String AIR_JUMP = "air_jump";
    /** Kinetic damage hit a player wearing Kinetic Protection ({@code AdvancementHooksMixin}). */
    public static final String KINETIC_PROTECTION = "kinetic_protection";
    /** Four armor pieces with the same bonus trim pattern worn together ({@link AdvancementChecks}). */
    public static final String FULL_TRIM_SET = "full_trim_set";
    /** A player set a backpack down as a block ({@code BackpackBlock#setPlacedBy}). */
    public static final String BACKPACK_PLACED = "backpack_placed";
    /** A damaged netherite or enderite piston repaired with its nugget ({@code NetheriteBreakerPistonBlock}). */
    public static final String PISTON_REPAIR = "piston_repair";
    /** A void-protected item the player dropped was caught below the world ({@code EnderiteItemMixin}). */
    public static final String VOID_RESCUE = "void_rescue";
    /** A not fully repaired echo sounder shattered after its jump ({@code EchoCompassItem#wear}). */
    public static final String ECHO_SHATTER = "echo_shatter";
    /** The amethyst lens beam primed TNT ({@code LaserBeam#beamAt}). */
    public static final String LASER_TNT = "laser_tnt";

    /** Every feature name, for the tests that fire each one. */
    public static final List<String> ALL = List.of(HAMMER_UPGRADE_NETHERITE, HAMMER_UPGRADE_ENDERITE, HAMMER_RESHAPE,
            DIAMOND_CRUSH, TRIM_TEMPLATE_FORGED, CHISEL, WAND_BUILD, OCTANT_MARK, ROTATE, BLUEPRINT_SCAN, BLUEPRINT_COPY,
            BLUEPRINT_BUILD, LAUNCHPAD, FLYPAD, ELYTRA_PAD, SPAWN_TELEPORT, POTION_PAD, ECHO_TELEPORT, LENS_BEAM,
            ORE_DETECTED, VEIN_MINE, STRIP_MINE, VERSATILITY_SWAP, AIR_JUMP, KINETIC_PROTECTION, FULL_TRIM_SET,
            BACKPACK_PLACED, PISTON_REPAIR, VOID_RESCUE, ECHO_SHATTER, LASER_TNT);

    /** {@code simplebuilding:counter} - waits until a per-player count ({@link ModCounters}) reaches a threshold. */
    public static final Identifier COUNTER_ID = Identifier.fromNamespaceAndPath("simplebuilding", "counter");
    public static final CounterTrigger COUNTER = new CounterTrigger();

    /** Registers the trigger type; called once per loader while the trigger registry is open. */
    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, FEATURE_USED_ID, FEATURE_USED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, COUNTER_ID, COUNTER);
    }

    /** Reports that {@code player} just used {@code feature}; a no-op for client or fake players. */
    public static void feature(Player player, String feature) {
        if (player instanceof ServerPlayer serverPlayer) {
            FEATURE_USED.trigger(serverPlayer, feature);
        }
    }
}
