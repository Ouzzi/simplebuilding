package com.simplebuilding.gametest;

import com.simplebuilding.version.McVersion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.tweaks.TweaksConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/**
 * The config options, and whether the mod still reads them.
 *
 * <p>Before this class the only covered options were the two trade switches, and those only
 * indirectly, through the datapack conditions that consume them. An option is a promise to the
 * player, and it is a promise that breaks quietly: the field stays in {@code simplebuilding.json},
 * the GUI keeps offering the toggle, and nothing anywhere reports that the last call site that read
 * it was deleted or renamed. The tests here flip an option and check that the behaviour behind it
 * actually changes.
 *
 * <p><b>Restoring the options.</b> Every test that writes an option captures the previous value
 * first and restores it in a {@code finally} block. A failing assertion throws, so the option is
 * put back on the way out of the very same call - which is what makes these tests independent of
 * each other no matter which one fails. The original value is re-applied a second time from
 * {@link GameTestHelper#runBeforeTestEnd}, purely as a belt and braces for a failure mode that
 * unwinds past the {@code finally}.
 *
 * <p>The suite runs as one batch in a single world and shares one config object, so a flipped
 * option is global while it lasts. It cannot leak into a neighbouring test all the same: none of
 * these bodies yields - no {@code succeedWhen}, no delayed callback - so each runs from its first
 * write to its restore inside one server tick on the server thread, and no other test body can be
 * running in that window.
 *
 * <p>Only two options gate behaviour a server side gametest can reach at all - every other read of
 * {@code Simplebuilding.getConfig()} in the mod sits in {@code client/} or in a client mixin. That
 * is written down per option in the javadoc below rather than papered over with a test that would
 * pass either way. The exception since 2026-09 is {@code pistonsBreachEndPortalFrames}, read
 * server side by {@code PistonBreach}; its behaviour is driven in
 * {@code PistonBreachTests#endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn}, and only its name
 * and default are pinned here. The same holds for {@code tools.buildingWandHungerCost} (read by
 * {@code WandHunger}); its behaviour is driven in {@code BuildingWandTests}.
 */
public final class ConfigOptionTests {

    private ConfigOptionTests() {
    }

    /** Resource folder the shipped trades live in: {@code data/simplebuilding/villager_trade/**}. */
    private static final String TRADE_DIRECTORY = "villager_trade";

    /** Sub folder of {@link #TRADE_DIRECTORY} that belongs to the wandering trader switch. */
    private static final String WANDERING_DIRECTORY = "wandering_trader";

    /** Id both loaders register their "read a config flag" datapack condition under. */
    private static final String CONFIG_CONDITION = SimpleBuildingGameTests.MOD_ID + ":config";

    /** Fully qualified name of Cloth Config's {@code @Config}; read reflectively, see below. */
    private static final String CONFIG_ANNOTATION = "me.shedaniel.autoconfig.annotation.Config";

    private static final String VILLAGER_FLAG = "enableVillagerTrades";
    private static final String WANDERING_FLAG = "enableWanderingTrades";

    /**
     * The classes that turn the flag string of a trade json into a config read, one per loader
     * (Fabric, NeoForge, Forge). Looked up by name so this class stays in the loader neutral tree;
     * only the running loader's copy is ever on the classpath, and the suite runs on all three.
     */
    private static final List<String> CONDITION_CLASSES = List.of(
            "com.simplebuilding.condition.ConfigResourceCondition",
            "com.simplebuilding.neoforge.ConfigLoadCondition",
            "com.simplebuilding.forge.ConfigLoadCondition");

    /**
     * Every vanilla loot table the mod hands pools to - all eighteen keys
     * {@code ModLootTableModifications.apply} names, not a sample of them.
     *
     * <p>The list has to be complete, because it is what the switched-off half of
     * {@link #lootTableChangesStopWhenTheOptionIsSwitchedOff} walks. A table missing from here is
     * a table whose block could be moved above the config guard - or deleted outright - without
     * anything going red.
     */
    private static final List<ResourceKey<LootTable>> MODIFIED_TABLES = List.of(
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.END_CITY_TREASURE,
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.BASTION_TREASURE,
            BuiltInLootTables.BASTION_OTHER,
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.BURIED_TREASURE,
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.SHIPWRECK_TREASURE,
            BuiltInLootTables.IGLOO_CHEST,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
            BuiltInLootTables.RUINED_PORTAL,
            BuiltInLootTables.FISHING_TREASURE);

    /** The bastion pool, which one condition hands to both {@code BASTION_*} keys. */
    private static final Set<String> BASTION_LOOT = Set.of(
            book(ModEnchantments.FUNNEL, 1),
            book(ModEnchantments.BREAK_THROUGH, 1),
            id(ModItems.GOLD_SLEDGEHAMMER),
            id(ModItems.GOLD_CORE),
            id(ModItems.NETHERITE_NUGGET),
            id(ModItems.NETHERITE_CARROT));

    /**
     * The second pool only the bastion treasure room gets on top of {@link #BASTION_LOOT}: the
     * netherite core and the apples sit there so that the many ordinary bastion chests do not
     * hand them out by the dozen.
     */
    private static final Set<String> BASTION_TREASURE_ONLY_LOOT = Set.of(
            book(ModEnchantments.BREAK_THROUGH, 2),
            id(ModItems.NETHERITE_CORE),
            id(ModItems.NETHERITE_APPLE),
            id(ModItems.ENCHANTED_NETHERITE_APPLE));

    /** The vault pool behind {@code TRIAL_CHAMBERS_REWARD_COMMON} - and the rare vault. */
    private static final Set<String> VAULT_COMMON_LOOT = Set.of(
            book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
            book(ModEnchantments.FAST_CHISELING, 2),
            id(ModItems.DIAMOND_PEBBLE));

    /** The vault pool behind {@code TRIAL_CHAMBERS_REWARD_OMINOUS} - and the rare vault. */
    private static final Set<String> VAULT_OMINOUS_LOOT = Set.of(
            book(ModEnchantments.MASTER_BUILDER, 1),
            book(ModEnchantments.DOUBLE_JUMP, 1),
            id(ModItems.DIAMOND_CORE),
            id(ModItems.NETHERITE_APPLE),
            id(ModItems.ENCHANTED_NETHERITE_APPLE));

    /**
     * What each of those tables has to be able to hand out, written as
     * {@code <enchantment id>@<level>} for an enchanted book and as the plain registry id for
     * every other item.
     *
     * <p>Counting pools is only half a promise. A pool that quietly lost an entry is still a pool,
     * and outside a handful of items with a home of their own - the reinforced bundle
     * ({@code BundleWiringTests}), the mining books here - nothing in the suite ever looked inside
     * a mod loot pool. Every entry below is therefore <em>rolled</em> out of the table: deleting
     * one {@code .add(...)} line, swapping the item behind it or moving an enchanted book down a
     * level turns this red, while the pool count and the config switch stay exactly as they were.
     *
     * <p>For the plain items the sets are lower bounds: adding an item to a pool is not a
     * regression and does not have to be listed here. For the enchanted books they are exact in
     * both directions - a book that is rolled out of a mod pool without being written down here
     * fails just as a listed one that never appears does. Which enchantments a chest can hand out
     * is the mod's progression, and adding a book moves nothing else this test watches: not the
     * pool count, not the config switch, and not one of the listed entries.
     *
     * <p>For the two mining enchantments these tables are the entire supply in the game - neither
     * is in the enchanting table, and only Strip Miner appears in a trade at all.
     */
    private static final Map<ResourceKey<LootTable>, Set<String>> EXPECTED_LOOT = Map.ofEntries(
            Map.entry(BuiltInLootTables.STRONGHOLD_LIBRARY, Set.of(
                    book(ModEnchantments.RANGE, 2),
                    book(ModEnchantments.MASTER_BUILDER, 1),
                    book(ModEnchantments.VERSATILITY, 1),
                    book(ModEnchantments.VERSATILITY, 2))),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, Set.of(
                    // the two pre built pools - the only place addBuiltPool is used at all
                    id(ModItems.ENDERITE_SCRAP),
                    id(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                    // the End materials pool
                    id(ModItems.RAW_ENDERITE),
                    id(ModItems.ENDERITE_NUGGET),
                    id(ModItems.ASTRALIT_DUST),
                    id(ModItems.NIHILITH_SHARD),
                    book(ModEnchantments.RANGE, 3),
                    book(ModEnchantments.MASTER_BUILDER, 1),
                    book(ModEnchantments.OVERRIDE, 2),
                    book(ModEnchantments.DOUBLE_JUMP, 2),
                    book(ModEnchantments.VERSATILITY, 1),
                    book(ModEnchantments.VERSATILITY, 2),
                    book(ModEnchantments.BRIDGE, 1),
                    id(ModItems.DIAMOND_BUILDING_WAND),
                    id(ModItems.DIAMOND_SLEDGEHAMMER),
                    id(ModItems.ENDERITE_APPLE),
                    id(ModItems.ENCHANTED_ENDERITE_APPLE))),
            Map.entry(BuiltInLootTables.ANCIENT_CITY, Set.of(
                    book(ModEnchantments.DEEP_POCKETS, 2),
                    book(ModEnchantments.RADIUS, 1),
                    id(ModItems.OCTANT),
                    id(ModItems.DIAMOND_SLEDGEHAMMER),
                    id(ModItems.QUIVER),
                    id(ModItems.NETHERITE_APPLE),
                    id(ModItems.ENCHANTED_NETHERITE_APPLE),
                    id(ModItems.NETHERITE_NUGGET),
                    id(ModItems.DIAMOND_PEBBLE))),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, union(BASTION_LOOT, BASTION_TREASURE_ONLY_LOOT)),
            // The same pool, reached through the second half of the same condition. Rolling it is
            // what makes deleting "|| BASTION_OTHER.equals(key)" a red test.
            Map.entry(BuiltInLootTables.BASTION_OTHER, BASTION_LOOT),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, Set.of(
                    book(ModEnchantments.STRIP_MINER, 1),
                    book(ModEnchantments.STRIP_MINER, 2),
                    book(ModEnchantments.FUNNEL, 1),
                    book(ModEnchantments.BREAK_THROUGH, 1),
                    id(ModItems.GOLD_CORE),
                    id(ModItems.OCTANT),
                    id(ModItems.NETHERITE_NUGGET),
                    id(ModItems.NETHERITE_CARROT))),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, Set.of(
                    book(ModEnchantments.COLOR_PALETTE, 1),
                    book(ModEnchantments.COVER, 1),
                    book(ModEnchantments.LINEAR, 1),
                    id(ModItems.OCTANT),
                    id(ModItems.QUIVER),
                    id(ModItems.COPPER_CHISEL))),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, Set.of(
                    book(ModEnchantments.COLOR_PALETTE, 1),
                    book(ModEnchantments.COVER, 1),
                    book(ModEnchantments.LINEAR, 1),
                    book(ModEnchantments.VEIN_MINER, 4),
                    book(ModEnchantments.VEIN_MINER, 5),
                    book(ModEnchantments.DRAWER, 1),
                    id(ModItems.IRON_BUILDING_WAND),
                    id(ModItems.IRON_CORE),
                    id(ModItems.QUIVER))),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, Set.of(
                    book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
                    book(ModEnchantments.FAST_CHISELING, 2),
                    id(ModItems.GOLD_CHISEL),
                    id(ModItems.DIAMOND_CHISEL),
                    id(ModItems.DIAMOND_PEBBLE))),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    book(ModEnchantments.FUNNEL, 1),
                    book(ModEnchantments.BREAK_THROUGH, 1),
                    book(ModEnchantments.VEIN_MINER, 2),
                    book(ModEnchantments.VEIN_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 4),
                    id(ModItems.REINFORCED_BUNDLE),
                    id(ModItems.BASIC_UPGRADE_TEMPLATE),
                    id(ModItems.DIAMOND_PEBBLE))),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    id(ModItems.REINFORCED_BUNDLE),
                    id(ModItems.DIAMOND_PEBBLE))),
            Map.entry(BuiltInLootTables.IGLOO_CHEST, Set.of(
                    book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
                    book(ModEnchantments.FAST_CHISELING, 1),
                    id(ModItems.DIAMOND_CHISEL))),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    book(ModEnchantments.STRIP_MINER, 1),
                    book(ModEnchantments.STRIP_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 4),
                    id(ModItems.REINFORCED_BUNDLE),
                    id(ModItems.IRON_CORE),
                    id(ModItems.DIAMOND_PEBBLE))),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, VAULT_COMMON_LOOT),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, VAULT_OMINOUS_LOOT),
            // The rare vault is the one key both vault conditions match, so it gets both pools.
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, union(VAULT_COMMON_LOOT, VAULT_OMINOUS_LOOT)),
            Map.entry(BuiltInLootTables.RUINED_PORTAL, Set.of(
                    id(ModItems.NETHERITE_NUGGET),
                    id(ModItems.GOLD_CHISEL),
                    id(ModItems.NETHERITE_CARROT))),
            // Fishing: every treasure catch rolls this pool once on top of the vanilla item.
            Map.entry(BuiltInLootTables.FISHING_TREASURE, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
                    book(ModEnchantments.DEEP_POCKETS, 1),
                    book(ModEnchantments.LINEAR, 1),
                    id(ModItems.DIAMOND_PEBBLE))));

    /** Separator {@link #book} puts between an enchantment id and its level. */
    private static final String BOOK_MARKER = "@";

    /**
     * The enchantments the mod deliberately gives no chest at all: Kinetic Protection is built like
     * vanilla Protection and comes from the enchanting table (since 2026-09-28), so no mod loot pool
     * may ever hand out a book for it. (Drawer used to be listed here too; it now comes from the
     * woodland mansion and the librarian.)
     *
     * <p>This is the one thing about the loot tables that cannot be pinned by listing something,
     * because it is a statement about what is absent. Adding one {@code .add(enchantedBook(...))}
     * line for either of them turns them into chest enchantments and leaves the pool counts, the
     * config switch and every listed entry exactly where they were, so it is checked against what
     * the pools actually roll - and against {@link #EXPECTED_LOOT} itself, so that writing such a
     * book into both lists at once is not a way past it either.
     */
    private static final Set<String> ENCHANTMENTS_WITHOUT_A_CHEST = Set.of(
            ModEnchantments.KINETIC_PROTECTION.identifier().toString());

    /**
     * How often each recorded pool is rolled when looking for the entries above.
     *
     * <p>The thinnest wanted entry decides this number. Since 2026-09-28 that is the iron core in
     * the mineshaft: its own pool with a 0.5 % chance per roll, so this many rolls are worth about
     * 10 expected hits and missing it by chance is about one in 28000 (the seed is fixed, so it is
     * the same answer every run). The other cores (1.05 % to 1.65 %, netherite 6 %) and the
     * enchanted netherite apple of the ominous vault (1 of 55, drawn {@code between(0, 1)} times,
     * about 18 hits) come next; everything else sits far higher. The Enderite core (0.175 %, about
     * 4 hits) is too thin for this list and is watched by {@link #buildingCoresAreVeryRareInLootChests}
     * with {@link #CORE_CHESTS} chests instead.
     *
     * <p>Those margins are the reason a thin entry may be listed at all; they are computed from
     * the weights in {@code ModLootTableModifications}, so a balance change that makes an entry
     * much rarer has to be reflected here.
     */
    private static final int POOL_ROLLS = 2048;

    /**
     * How many mod stacks one chest of each table may bring on average, as {@code [min, max]}.
     *
     * <p>This is the balance sheet of {@code docs/LOOT-BALANCE.md} turned into numbers. Tables a
     * structure has many of - the mineshaft, the mansion, the ancient city, the ordinary bastion
     * chests, the dungeon - sit at about half a mod stack per chest; single chests such as the
     * bastion treasure room or an end city chest may give more. The bands are wide enough for the
     * dice ({@link #POOL_ROLLS} chests, fixed seed) and narrow enough that the old weights - the
     * mansion at more than two mod stacks per chest, the ancient city at almost one and a half -
     * are red.
     */
    private record Budget(double min, double max) {
    }

    private static final Map<ResourceKey<LootTable>, Budget> CHEST_BUDGETS = Map.ofEntries(
            Map.entry(BuiltInLootTables.STRONGHOLD_LIBRARY, new Budget(0.3, 1.0)),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, new Budget(1.3, 2.5)),
            Map.entry(BuiltInLootTables.ANCIENT_CITY, new Budget(0.35, 0.7)),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, new Budget(0.8, 1.6)),
            Map.entry(BuiltInLootTables.BASTION_OTHER, new Budget(0.35, 0.7)),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, new Budget(0.4, 0.8)),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, new Budget(0.4, 1.0)),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, new Budget(0.25, 0.7)),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, new Budget(0.3, 1.0)),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, new Budget(0.35, 0.7)),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, new Budget(0.15, 0.6)),
            Map.entry(BuiltInLootTables.IGLOO_CHEST, new Budget(0.15, 0.6)),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, new Budget(0.3, 0.7)),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, new Budget(0.1, 0.4)),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, new Budget(0.2, 0.6)),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, new Budget(0.1, 0.4)),
            Map.entry(BuiltInLootTables.RUINED_PORTAL, new Budget(0.1, 0.4)),
            Map.entry(BuiltInLootTables.FISHING_TREASURE, new Budget(0.2, 0.6)));

    /** How many chests {@link #buildingCoresAreVeryRareInLootChests} rolls per table. */
    private static final int CORE_CHESTS = 10000;

    /**
     * Cores per chest, as {@code [min, max]} around the chances in
     * {@code ModLootTableModifications} (docs/KERNE-SELTENHEIT.md, owner 2026-09-28 "Zeitalter B"):
     * iron 1.5 % in the mansion and 0.5 % in the mineshaft, gold 1.25 % in every bastion chest and
     * 1.65 % in the fortress, diamond 1.05 % in the ominous and rare vault, netherite 6 % in the
     * bastion treasure room, Enderite 0.175 % in the End City. Every pair not listed has to be
     * zero. The bands are about three standard deviations of {@link #CORE_CHESTS} chests wide and
     * leave out the chances of the previous balance (iron 0.8 %, gold 0.6 %, Enderite 0.25 %).
     */
    private static final Map<ResourceKey<LootTable>, Map<Item, Budget>> CORE_CHANCES = Map.ofEntries(
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, Map.of(ModItems.IRON_CORE, new Budget(0.0115, 0.0185))),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, Map.of(ModItems.IRON_CORE, new Budget(0.0028, 0.0072))),
            Map.entry(BuiltInLootTables.BASTION_OTHER, Map.of(ModItems.GOLD_CORE, new Budget(0.009, 0.016))),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, Map.of(ModItems.GOLD_CORE, new Budget(0.009, 0.016),
                    ModItems.NETHERITE_CORE, new Budget(0.052, 0.068))),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, Map.of(ModItems.GOLD_CORE, new Budget(0.0125, 0.0205))),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, Map.of(ModItems.DIAMOND_CORE, new Budget(0.0072, 0.0138))),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, Map.of(ModItems.DIAMOND_CORE, new Budget(0.0072, 0.0138))),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, Map.of(ModItems.ENDERITE_CORE, new Budget(0.0006, 0.0023))));

    /** Seed for the loot rolls, so a failure is reproducible instead of a coin flip. */
    private static final long POOL_ROLL_SEED = 20260904L;

    /** Vanilla loot tables the mod must never touch - the control group for the recorder. */
    private static final List<ResourceKey<LootTable>> UNTOUCHED_TABLES = List.of(
            BuiltInLootTables.SPAWN_BONUS_CHEST,
            BuiltInLootTables.DESERT_PYRAMID);

    /**
     * Every option the config persists, as {@code group.name type=default}. Statics carry no
     * default because they are not part of the saved file - see
     * {@link #everyConfigOptionKeepsItsPersistedNameAndDefault}.
     */
    private static final Set<String> EXPECTED_OPTIONS = Set.of(
            "root.tools group:Tools",
            "root.enableDoubleJump boolean=true",
            "root.airJumpCooldownTicks int=400",
            "root.enableArmorTrimBenefits boolean=true",
            "root.trimBenefitBaseMultiplier double=2.0",
            "root.maxMultiplierLimit double runtime-only(static)",
            "root.breakerPistonsLoseDurability boolean=true",
            "root.pistonsBreachEndPortalFrames boolean=false",
            "root.pistonsBreachModdedUnbreakables boolean=false",
            "root.tweaks group:TweaksConfig",
            "root.worldGen group:WorldGen",
            "root.giveGuideBookOnFirstJoin boolean=true",
            "root.vanillaEnchantedBookTextures boolean=true",
            "root.modEnchantedBookTextures boolean=true",
            "root.visibleTrimIconsVanillaArmor boolean=true",
            "root.visibleTrimIconsModArmor boolean=true",
            "root.showModHud boolean=true",
            "root.hudPositionX int=0",
            "root.hudPositionY int=50",
            "root.hudScale int=100",
            "root.pistonsFireBreakEvents boolean=true",
            "root.showDevEnchantedTab boolean=false",
            "tools.buildingWandHungerCost boolean=true",
            "tools.wandHungerMultiplier double=1.0",
            "tools.magnetRangeMultiplier double=1.0",
            "tools.rotatorChargePerTurn int=1",
            "tools.invertBundleInteractions boolean=false",
            "tools.invertOctantSneak boolean=false",
            "tools.buildingHighlightOpacity int=40",
            "tools.enableToolAnimations boolean=true",
            "tools.enableChiselAnimation boolean=true",
            "tweaks.pads group:Pads",
            "tweaks.padTuning group:PadTuning",
            "tweaks.laserPointer group:LaserPointer",
            "tweaks.balancing group:Balancing",
            "tweaks.spawn group:Spawn",
            "tweaks.dimensions group:Dimensions",
            "tweaks.commands group:Commands",
            "tweaks.optimization group:Optimization",
            "tweaks.pads.enableChunkLoaders boolean=true",
            "tweaks.pads.enableElytraPads boolean=true",
            "tweaks.pads.enableFlypads boolean=true",
            "tweaks.pads.enableSpawnTeleporters boolean=true",
            "tweaks.pads.enableLaunchpads boolean=true",
            "tweaks.pads.enableTimedCopperPlates boolean=true",
            "tweaks.pads.enableFilterPlates boolean=true",
            "tweaks.pads.enablePotionPads boolean=true",
            "tweaks.padTuning.teleporterTier1WarmupTicks int=1000",
            "tweaks.padTuning.teleporterTier2WarmupTicks int=400",
            "tweaks.padTuning.teleporterTier3WarmupTicks int=100",
            "tweaks.padTuning.launchpadStrengthMultiplier double=1.0",
            "tweaks.padTuning.potionPadChargeStepTicks int=20",
            "tweaks.padTuning.potionPadCooldownFactor double=2.0",
            "tweaks.laserPointer.enable boolean=true",
            "tweaks.laserPointer.color int=16711680",
            "tweaks.laserPointer.scale float=0.25",
            "tweaks.laserPointer.range int=512",
            "tweaks.laserPointer.beamCostPerSecond int=1",
            "tweaks.laserPointer.chargePerSecond int=4",
            "tweaks.laserPointer.effectCost int=5",
            "tweaks.laserPointer.showLine boolean=false",
            "tweaks.balancing.rocketStackSize int=64",
            "tweaks.balancing.echoSounderJumpCooldownTicks int=480",
            "tweaks.balancing.echoSounderAttemptLockTicks int=100",
            "tweaks.spawn.MAX_FLIGHT_SECONDS int runtime-only(static)",
            "tweaks.spawn.MAX_BOOSTS int runtime-only(static)",
            "tweaks.spawn.forceExactSpawn boolean=false",
            "tweaks.spawn.disableFallDamageInSpawn boolean=true",
            "tweaks.spawn.useCustomWorldSpawn boolean=false",
            "tweaks.spawn.xCoordSpawnPoint int=0",
            "tweaks.spawn.yCoordSpawnPoint int=-1",
            "tweaks.spawn.zCoordSpawnPoint int=0",
            "tweaks.spawn.firstJoinTeleporterCount int=0",
            "tweaks.spawn.firstJoinElytraPadCount int=0",
            "tweaks.spawn.spawnTeleporterCount Integer=null",
            "tweaks.spawn.giveElytraOnSpawn boolean=false",
            "tweaks.spawn.spawnElytraRadius int=25",
            "tweaks.spawn.useWorldSpawnAsCenter boolean=false",
            "tweaks.spawn.customSpawnElytraX int=0",
            "tweaks.spawn.customSpawnElytraZ int=0",
            "tweaks.spawn.flightTimeSeconds int=300",
            "tweaks.spawn.maxBoosts int=3",
            "tweaks.spawn.boostStrength float=0.6",
            "tweaks.spawn.spawn1X int=0",
            "tweaks.spawn.spawn1Y int=-1000",
            "tweaks.spawn.spawn1Z int=0",
            "tweaks.dimensions.allowNether boolean=true",
            "tweaks.dimensions.allowEnd boolean=true",
            "tweaks.commands.enableKillBoatsCommand boolean=true",
            "tweaks.commands.enableKillCartsCommand boolean=false",
            "tweaks.commands.killCommandRadius int=100",
            "tweaks.optimization.enableXpClumps boolean=true",
            "tweaks.optimization.xpClumpRadius double=2.0",
            "tweaks.optimization.scaleXpOrbs boolean=true",
            "worldGen.enableLootTableChanges boolean=true",
            "worldGen.buildingCoreLootChanceMultiplier double=1.0",
            "worldGen.enableVillagerTrades boolean=true",
            "worldGen.enableWanderingTrades boolean=true",
            // Reiter "Server & Modpack Tuning" (2026-09-28)
            "root.server group:ServerTuningConfig",
            "server.features group:Features",
            "server.chunkLoaders group:ChunkLoaders",
            "server.dimensionLocks group:DimensionLocks",
            "server.laser group:Laser",
            "server.oreGeneration group:OreGeneration",
            "server.pads group:Pads",
            "server.charges group:Charges",
            "server.tools group:Tools",
            "server.machines group:Machines",
            "server.oreDetector group:OreDetector",
            "server.loot group:Loot",
            "server.blueprint group:Blueprint",
            "server.trimStrengths group:TrimStrengths",
            "server.features.airJump boolean=true",
            "server.features.dynamicLight boolean=true",
            "server.features.backpack boolean=true",
            "server.features.attractor boolean=true",
            "server.features.echoSounder boolean=true",
            "server.features.blueprint boolean=true",
            "server.features.oreDetector boolean=true",
            "server.features.levitatingBlocks boolean=true",
            "server.chunkLoaders.requireOwnerOnline boolean=true",
            "server.dimensionLocks.chunkLoaderBlockedDimensions String=",
            "server.dimensionLocks.flypadBlockedDimensions String=",
            "server.dimensionLocks.echoSounderBlockedDimensions String=",
            "server.laser.igniteFlammables boolean=true",
            "server.laser.igniteTnt boolean=true",
            "server.laser.igniteEntities boolean=true",
            "server.oreGeneration.endOres boolean=true",
            "server.oreGeneration.astralitOre boolean=true",
            "server.oreGeneration.nihilitOre boolean=true",
            "server.pads.strangerPadBreakSeconds int=60",
            "server.pads.strangerPlateBreakSeconds int=10",
            "server.charges.lensMaxCharge int=640",
            "server.charges.rotatorMaxCharge int=1024",
            "server.charges.echoSounderMaxCharge int=1500",
            "server.tools.sledgehammerUpgradeSeconds int=5",
            "server.tools.reinforcedUpgradeDamagePerHit int=2",
            "server.tools.netheriteUpgradeDamagePerHit int=4",
            "server.tools.enderiteUpgradeDamagePerHit int=10",
            "server.tools.stoneChiselCooldownTicks int=30",
            "server.tools.copperChiselCooldownTicks int=25",
            "server.tools.ironChiselCooldownTicks int=25",
            "server.tools.goldChiselCooldownTicks int=20",
            "server.tools.diamondChiselCooldownTicks int=10",
            "server.tools.netheriteChiselCooldownTicks int=5",
            "server.tools.enderiteChiselCooldownTicks int=5",
            "server.machines.reinforcedHopperSpeed int=2",
            "server.machines.netheriteHopperSpeed int=4",
            "server.machines.enderiteHopperSpeed int=8",
            "server.machines.reinforcedFurnaceSpeed int=2",
            "server.machines.netheriteFurnaceSpeed int=4",
            "server.machines.enderiteFurnaceSpeed int=8",
            "server.oreDetector.rangeMultiplier double=1.0",
            "server.oreDetector.scanIntervalTicks int=20",
            "server.loot.globalLootMultiplier double=1.0",
            "server.loot.strongholdLoot boolean=true",
            "server.loot.endCityLoot boolean=true",
            "server.loot.ancientCityLoot boolean=true",
            "server.loot.bastionLoot boolean=true",
            "server.loot.netherFortressLoot boolean=true",
            "server.loot.pillagerOutpostLoot boolean=true",
            "server.loot.woodlandMansionLoot boolean=true",
            "server.loot.buriedTreasureLoot boolean=true",
            "server.loot.dungeonLoot boolean=true",
            "server.loot.shipwreckLoot boolean=true",
            "server.loot.iglooLoot boolean=true",
            "server.loot.mineshaftLoot boolean=true",
            "server.loot.trialChambersLoot boolean=true",
            "server.loot.ruinedPortalLoot boolean=true",
            "server.loot.fishingLoot boolean=true",
            "server.loot.tradePriceMultiplier double=1.0",
            "server.blueprint.maxBlocksPerTick int=32768",
            "server.trimStrengths.projectileProtection double=1.0",
            "server.trimStrengths.magicProtection double=1.0",
            "server.trimStrengths.thornProtection double=1.0",
            "server.trimStrengths.blastProtection double=1.0",
            "server.trimStrengths.drowningProtection double=1.0",
            "server.trimStrengths.breathSaving double=1.0",
            "server.trimStrengths.allProtection double=1.0",
            "server.trimStrengths.sonicProtection double=1.0",
            "server.trimStrengths.stealth double=1.0",
            "server.trimStrengths.fireProtection double=1.0",
            "server.trimStrengths.witherProtection double=1.0",
            "server.trimStrengths.witherShortening double=1.0",
            "server.trimStrengths.dragonBreathProtection double=1.0",
            "server.trimStrengths.fallProtection double=1.0",
            "server.trimStrengths.windChargeProtection double=1.0",
            "server.trimStrengths.lightningProtection double=1.0",
            "server.trimStrengths.walkingSpeed double=1.0",
            "server.trimStrengths.swimmingSpeed double=1.0",
            "server.trimStrengths.sprintHunger double=1.0",
            "server.trimStrengths.experience double=1.0",
            "server.trimStrengths.luck double=1.0",
            "server.trimStrengths.blockReach double=1.0",
            "server.trimStrengths.physicalProtection double=1.0",
            "server.trimStrengths.illagerProtection double=1.0",
            "server.trimStrengths.witherPiercingProtection double=1.0",
            "server.trimStrengths.healingChance double=1.0",
            "server.trimStrengths.knockbackResistance double=1.0");

    // =====================================================================================
    // tools.invertBundleInteractions
    // =====================================================================================

    /**
     * {@code tools.invertBundleInteractions} swaps the two mouse buttons of the reinforced bundle:
     * off, left click stuffs an item in and right click takes one out; on, it is the other way
     * round. The option is read by two helpers, {@code getInsertClick} and {@code getRemoveClick},
     * and those two are consulted from two independent places: {@code overrideStackedOnOther} (the
     * bundle is on the cursor, the item lies in a slot) and {@code overrideOtherStackedOnMe} (the
     * item is on the cursor, the bundle lies in a slot). Both interactions are driven here in both
     * settings and in both directions - eight combinations - because the player meets both of them
     * and losing either leaves a bundle that can only be filled or only be emptied.
     *
     * <p>What breaks it: dropping either config read, so that one of the two buttons keeps its
     * vanilla meaning while the other flips; flipping the branches in only one of the two
     * interaction methods, so the toggle works when the bundle is picked up but not when it lies
     * in the inventory; or a gate that stops seeing config changes at all, in which case the
     * inverted half fails exactly as the player's toggle would.
     *
     * <p><b>The insert click on a bundle that is nearly full</b> is driven once per binding on top
     * of that, through both interaction methods. Every other case here offers eight stone to an
     * empty bundle, where "the bundle took everything" and "the bundle took what fit" look the
     * same; that hid the one line in each method that decides what happens to the rest. Both count
     * the insert with {@code shrink(added)}, and {@code insertItemIntoBundle} returns less than the
     * offered amount as soon as the room runs out. With the whole offer swallowed instead, a click
     * on a 64 stack with room for 32 would delete the other 32 in front of the player - so the
     * partial case checks the leftover in the slot and on the cursor, both derived from a measured
     * capacity rather than a copied number.
     */
    public static void bundleClickInversionFollowsTheConfiguredOption(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        ServerPlayer player = mockPlayer(helper);
        boolean original = config.tools.invertBundleInteractions;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().tools.invertBundleInteractions = original);

        try {
            // --- switched off: left click fills, right click empties ---
            setBundleInversion(helper, false);
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, true, "off / left click");
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, false, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / left click");

            assertCursorFillClick(helper, player, ClickAction.PRIMARY, true, "off / cursor / left click");
            assertCursorFillClick(helper, player, ClickAction.SECONDARY, false, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / cursor / left click");

            assertPartialFillClick(helper, player, ClickAction.PRIMARY, "off");

            // --- switched on: exactly the other way round ---
            setBundleInversion(helper, true);
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / right click");
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / right click");

            assertCursorFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / cursor / right click");
            assertCursorFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / cursor / right click");

            assertPartialFillClick(helper, player, ClickAction.SECONDARY, "inverted");
        } finally {
            Simplebuilding.getConfig().tools.invertBundleInteractions = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableLootTableChanges
    // =====================================================================================

    /**
     * {@code worldGen.enableLootTableChanges} is the player's only way to keep the mod out of the
     * vanilla chests. It is the very first thing {@code ModLootTableModifications.apply} looks at,
     * and it guards both ways the mod hands a pool to the loader - the builder path
     * ({@code addPool}) and the pre built path ({@code addBuiltPool}).
     *
     * <p>Production calls {@code apply} once per loot table while the datapacks load, which has
     * long happened by the time a gametest runs, so the test drives that entry point directly with
     * a recording editor instead of reloading the world.
     *
     * <p>All sixteen tables are driven rather than a sample, and the end city is checked on both
     * editor paths. That is what separates "the guard is gone" from "the guard moved into one
     * branch": a gate that only still covers the four tables an earlier version of this test knew
     * about would let the nether fortress, the mineshaft, the igloo and the trial chambers through
     * and fail here. The two vanilla tables the mod never touches are recorded in the switched-on
     * state and have to come back empty - they prove the recorder reports zero when nothing is
     * added, so the switched-off half cannot pass merely because the recorder broke.
     *
     * <p>Counting pools is only half the promise, though. A pool that lost an entry is still a
     * pool, so every table is additionally <em>rolled</em>: {@link #EXPECTED_LOOT} names the books
     * and items each chest has to be able to hand out, and {@link #POOL_ROLLS} draws have to
     * produce every one of them. For most of those entries this is their only coverage - deleting
     * a single {@code .add(...)} line leaves the pool non-empty, the option still switches it off
     * and on, and nothing else in the suite ever looks inside.
     *
     * <p>The books are held to the list in both directions on top of that. An entry that is
     * <em>added</em> to a pool is normally nobody's business here, but an enchanted book is not an
     * item like any other: it decides whether an enchantment can be found in the world at all.
     * {@link #ENCHANTMENTS_WITHOUT_A_CHEST} names the two the mod documents as having no chest
     * anywhere, and the same reading catches every other undeclared book with it.
     *
     * <p>What breaks it: deleting the guard, so a player who switched the mod's loot off finds mod
     * books in a stronghold library anyway; moving the guard inside one of the branches, or
     * putting an ungated table block above it; a table quietly losing its pools while the option
     * is on; any listed entry losing its item, its level or its whole chest; or a pool growing a
     * book that is not written down - a Drawer book in the ancient city would make a creative-only
     * enchantment findable while every count and every switch above stayed as it is.
     */
    public static void lootTableChangesStopWhenTheOptionIsSwitchedOff(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = config.worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        try {
            // --- switched on: the mod puts its pools in ---
            setLootTableChanges(helper, true);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                PoolRecorder recorder = recordPools(key, registries);
                helper.assertTrue(recorder.total() > 0,
                        tableName(key) + " got no mod pool although the loot table option is on");
            }

            PoolRecorder endCity = recordPools(BuiltInLootTables.END_CITY_TREASURE, registries);
            helper.assertTrue(endCity.builders > 0,
                    "the end city treasure got no builder pool although the loot table option is on");
            helper.assertTrue(endCity.built > 0,
                    "the end city treasure got no pre built pool although the loot table option is on");

            for (ResourceKey<LootTable> key : UNTOUCHED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools added to " + tableName(key) + ", a table the mod does not touch");
            }

            // --- and what is in those pools, rolled out of them for real ---
            List<String> missing = new ArrayList<>();
            List<String> stray = new ArrayList<>();
            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                Set<String> wanted = EXPECTED_LOOT.get(key);
                helper.assertTrue(wanted != null,
                        tableName(key) + " is driven for its pool count but nothing is expected out of "
                                + "it; add its entries to EXPECTED_LOOT or the pool could be emptied");
                Set<String> rolled = rollContents(helper, recordPools(key, registries));
                for (String entry : wanted) {
                    if (!rolled.contains(entry)) {
                        missing.add(tableName(key) + " never handed out " + entry + " in " + POOL_ROLLS
                                + " rolls; what it did hand out was " + rolled);
                    }
                }
                stray.addAll(strayBooks(key, wanted, rolled));
            }
            helper.assertTrue(missing.isEmpty(),
                    "the mod loot pools no longer contain what they are documented to contain:\n"
                            + String.join("\n", missing));
            helper.assertTrue(stray.isEmpty(),
                    "the mod loot pools hand out enchanted books nobody wrote down for them. An added "
                            + "book is the one pool change that moves nothing else this test watches - "
                            + "the pool count, the config switch and every listed entry stay as they "
                            + "are - while it decides whether an enchantment can be found in the world "
                            + "at all:\n" + String.join("\n", stray));

            // Guards the list above against the obvious way around it: writing a banned enchantment
            // into EXPECTED_LOOT would make its book "expected" and silence the check.
            List<String> bannedButListed = new ArrayList<>();
            for (Map.Entry<ResourceKey<LootTable>, Set<String>> expected : EXPECTED_LOOT.entrySet()) {
                for (String entry : expected.getValue()) {
                    if (bannedEnchantment(entry) != null) {
                        bannedButListed.add(tableName(expected.getKey()) + " lists " + entry);
                    }
                }
            }
            helper.assertTrue(bannedButListed.isEmpty(),
                    "an enchantment out of ENCHANTMENTS_WITHOUT_A_CHEST was written into EXPECTED_LOOT, "
                            + "which would let a chest hand it out with every assertion above satisfied; "
                            + "the two lists have to keep disagreeing:\n"
                            + String.join("\n", bannedButListed));

            // --- switched off: nothing at all, on either path ---
            setLootTableChanges(helper, false);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools were added to " + tableName(key)
                                + " although the loot table option is switched off");
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableVillagerTrades / enableWanderingTrades
    // =====================================================================================

    /**
     * The two trade switches are not read from code at all - each shipped trade json names the flag
     * itself, once for Fabric ({@code fabric:load_conditions}), once for NeoForge
     * ({@code neoforge:conditions}) and once for Forge ({@code forge:condition}, a single object
     * rather than a list), and the loader hands that string to the mod's condition. All three
     * conditions resolve the string in a hard coded {@code switch} over exactly two cases and
     * answer anything else with {@code true} plus a log line, so a typo, a copy pasted folder or a
     * trade gated on a third flag does not fail anything: the switch simply stops working and the
     * trade ships regardless.
     *
     * <p>So this test reads the shipped files back out of the server's resource manager and checks
     * the wiring end to end: every mod trade carries both loaders' conditions, both name the same
     * flag, the flag matches the folder the trade lives in, and the set of flags in use is exactly
     * the two the conditions can actually resolve. The reflective look up on
     * {@code SimplebuildingConfig.WorldGen} that follows is the secondary check - it says whether
     * the flag still corresponds to a readable, non static boolean option, which is what the
     * conditions ultimately return.
     *
     * <p><b>The switch itself is driven, not described.</b> The two flag names above are literals
     * in this file, so comparing the jsons against them says nothing about the {@code case} labels
     * the loader's condition is written with. Rename the constant one of those cases uses - the
     * json and the config field keep their spelling, so everything else here still lines up - and
     * every trade in that folder falls into the {@code default} branch, ships regardless of the
     * player's toggle and leaves a log line as the only trace. Each flag is therefore handed to the
     * loader's real condition object twice: once with its own option switched off, where the
     * condition has to answer {@code false}, and once with the <em>other</em> flag's option
     * switched off, where it has to stay {@code true}. The second half is what separates a working
     * switch from one whose two cases end up reading the same field. Only the running loader's
     * class can be reached this way - the suite runs on both, and a run that finds neither class
     * says so instead of passing on an empty loop.
     *
     * <p>What breaks it: adding a trade with only the Fabric condition, which then ignores the
     * switch on NeoForge (and vice versa); putting a wandering trader offer behind the villager
     * flag, so the wrong toggle turns it off; gating a trade on a flag the two condition classes do
     * not have a {@code case} for - the file would keep shipping with the toggle off, and only a
     * log line would say so. A new trade that is meant to be unconditional fails here too -
     * deliberately, because "this one is not covered by the switch any more" is exactly the
     * decision that should not be made by accident.
     */
    public static void tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        boolean villagerOriginal = config.worldGen.enableVillagerTrades;
        boolean wanderingOriginal = config.worldGen.enableWanderingTrades;
        helper.runBeforeTestEnd(() -> {
            Simplebuilding.getConfig().worldGen.enableVillagerTrades = villagerOriginal;
            Simplebuilding.getConfig().worldGen.enableWanderingTrades = wanderingOriginal;
        });
        Map<Identifier, Resource> tradeFiles = helper.getLevel().getServer().getResourceManager()
                .listResources(TRADE_DIRECTORY, id -> id.getPath().endsWith(".json"));

        List<String> problems = new ArrayList<>();
        Set<String> flagsSeen = new TreeSet<>();
        int checked = 0;

        for (Map.Entry<Identifier, Resource> entry : tradeFiles.entrySet()) {
            Identifier id = entry.getKey();
            if (!SimpleBuildingGameTests.MOD_ID.equals(id.getNamespace())) {
                continue; // vanilla or another mod's trades, not ours to police
            }
            checked++;

            JsonObject json;
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                json = GsonHelper.parse(reader);
            } catch (IOException | RuntimeException e) {
                problems.add(id + ": could not be read as json (" + e + ")");
                continue;
            }

            String fabricFlag = configFlag(json, "fabric:load_conditions", "condition");
            String neoFlag = configFlag(json, "neoforge:conditions", "type");
            String forgeFlag = singleConfigFlag(json.get("forge:condition"), "type");
            String expected = id.getPath().startsWith(TRADE_DIRECTORY + "/" + WANDERING_DIRECTORY + "/")
                    ? WANDERING_FLAG
                    : VILLAGER_FLAG;

            if (fabricFlag == null) {
                problems.add(id + ": no \"fabric:load_conditions\" entry of type " + CONFIG_CONDITION
                        + " - on Fabric this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(fabricFlag);
            }
            if (neoFlag == null) {
                problems.add(id + ": no \"neoforge:conditions\" entry of type " + CONFIG_CONDITION
                        + " - on NeoForge this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(neoFlag);
            }
            if (forgeFlag == null) {
                problems.add(id + ": no \"forge:condition\" object of type " + CONFIG_CONDITION
                        + " - on Forge this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(forgeFlag);
            }
            if (fabricFlag != null && neoFlag != null && !fabricFlag.equals(neoFlag)) {
                problems.add(id + ": the two loaders are gated on different flags (Fabric " + fabricFlag
                        + ", NeoForge " + neoFlag + ")");
            }
            if (fabricFlag != null && forgeFlag != null && !fabricFlag.equals(forgeFlag)) {
                problems.add(id + ": Fabric and Forge are gated on different flags (Fabric " + fabricFlag
                        + ", Forge " + forgeFlag + ")");
            }
            if (fabricFlag != null && !expected.equals(fabricFlag)) {
                problems.add(id + ": gated on " + fabricFlag + ", but the folder it lives in belongs to "
                        + expected);
            }
        }

        helper.assertTrue(checked > 0,
                "no " + SimpleBuildingGameTests.MOD_ID + " trade json was found under data/"
                        + SimpleBuildingGameTests.MOD_ID + "/" + TRADE_DIRECTORY
                        + "; without files to read this test proves nothing, so the listing itself is broken");

        for (String flag : flagsSeen) {
            problems.addAll(worldGenFlagProblems(flag));
        }
        problems.addAll(conditionSwitchProblems(flagsSeen));

        helper.assertTrue(problems.isEmpty(),
                "the trade files and the config no longer line up:\n" + String.join("\n", problems));
        helper.assertValueEqual(flagsSeen, Set.of(VILLAGER_FLAG, WANDERING_FLAG),
                "the set of config flags the shipped trades are gated on - the two loader conditions "
                        + "resolve exactly these two strings and answer every other one with \"true\"");

        helper.succeed();
    }

    // =====================================================================================
    // THE OPTION SET ITSELF
    // =====================================================================================

    /**
     * Pins the saved config's identity: the file it is written to, and every option's name, group,
     * type and - for the persisted ones - default. Renaming or removing an option is silent by
     * construction. The code that reads it is renamed with it, so nothing fails to compile and
     * nothing fails at runtime; what happens instead is that the key in every player's
     * {@code config/simplebuilding.json} stops matching, their setting is dropped on the next load
     * and the option quietly reverts to its default. The file name behaves the same way, one level
     * up: change the {@code name} in the {@code @Config} annotation and every existing
     * {@code simplebuilding.json} is orphaned in place, with the mod happily writing a new file
     * beside it. This test is the place where both have to be a deliberate act.
     *
     * <p>The annotation is read by its fully qualified name instead of importing Cloth Config, so
     * this stays in the loader neutral tree and does not care which loader supplies
     * {@code me.shedaniel.autoconfig} (the Forge module ships a shim of its own).
     *
     * <p>The two multiplier fields are static, and the serializer both loaders use is Gson based,
     * which skips static fields. They are therefore runtime only: the {@code /simplebuilding}
     * command can change them for the session, but nothing writes them to disk and nothing reads
     * them back at startup. They are listed as such rather than with a default, so making one of
     * them an instance field (or making a persisted option static, which would silently stop saving
     * it) fails here. It also keeps this test independent of the trim tests, which write those two
     * statics while they run.
     *
     * <p>What breaks it: renaming, removing, retyping or re-grouping an option, changing a default,
     * adding a new one without listing it, or renaming the config file. All of those are fine
     * changes to make - they just have to be made on purpose, in one place, next to this
     * explanation.
     */
    public static void everyConfigOptionKeepsItsPersistedNameAndDefault(GameTestHelper helper) {
        SimplebuildingConfig defaults = new SimplebuildingConfig();

        List<String> problems = new ArrayList<>();
        Set<String> found = new TreeSet<>();
        collectOptions(found, problems, "root", SimplebuildingConfig.class, defaults);
        collectOptions(found, problems, "tools", SimplebuildingConfig.Tools.class, defaults.tools);
        collectOptions(found, problems, "worldGen", SimplebuildingConfig.WorldGen.class, defaults.worldGen);
        // Seit dem Config-Umbau 2026-09-28 auch der Tweaks-Abschnitt mit allen Gruppen (TweaksTests
        // pinnt ihn zusaetzlich in seiner eigenen Form).
        collectOptions(found, problems, "tweaks", TweaksConfig.class, defaults.tweaks);
        for (Field group : TweaksConfig.class.getFields()) {
            if (Modifier.isStatic(group.getModifiers())) {
                continue;
            }
            try {
                collectOptions(found, problems, "tweaks." + group.getName(), group.getType(), group.get(defaults.tweaks));
            } catch (IllegalAccessException e) {
                problems.add("tweaks." + group.getName() + " could not be read (" + e + ")");
            }
        }

        // Reiter "Server & Modpack Tuning" mit allen Gruppen (2026-09-28).
        collectOptions(found, problems, "server", com.simplebuilding.config.ServerTuningConfig.class, defaults.server);
        for (Field group : com.simplebuilding.config.ServerTuningConfig.class.getFields()) {
            if (Modifier.isStatic(group.getModifiers())) {
                continue;
            }
            try {
                collectOptions(found, problems, "server." + group.getName(), group.getType(), group.get(defaults.server));
            } catch (IllegalAccessException e) {
                problems.add("server." + group.getName() + " could not be read (" + e + ")");
            }
        }

        helper.assertTrue(problems.isEmpty(),
                "config options could not be read by reflection:\n" + String.join("\n", problems));
        helper.assertValueEqual(found, new TreeSet<>(EXPECTED_OPTIONS),
                "the set of config options (name, group, type, default)");

        String fileName = declaredConfigFileName();
        helper.assertTrue(fileName != null,
                "SimplebuildingConfig no longer carries a runtime visible " + CONFIG_ANNOTATION
                        + " annotation, so nothing here can tell which file the options are saved to");
        helper.assertValueEqual(fileName, SimpleBuildingGameTests.MOD_ID,
                "the name in @Config on SimplebuildingConfig - it is the base name of the file under "
                        + "config/, so changing it orphans every existing simplebuilding.json");

        // Every option test in this class writes through Simplebuilding.getConfig() and expects the
        // code under test to read the same object back. If that ever stopped being one live object,
        // those tests would fail somewhere far away with a confusing message; this says it plainly.
        SimplebuildingConfig live = liveConfig(helper);
        helper.assertTrue(Simplebuilding.getConfig() == live,
                "getConfig() handed out a different config object on the second call, so an option "
                        + "written through it would never reach the code that reads it");
        helper.assertTrue(Simplebuilding.getConfig().tools == live.tools
                        && Simplebuilding.getConfig().worldGen == live.worldGen,
                "the option groups behind getConfig() are rebuilt per call, so writing an option in a "
                        + "group has no effect on the code that reads it");

        helper.succeed();
    }

    // =====================================================================================
    // Config-Umbau 2026-09-28: screen layout, command, new options
    // =====================================================================================

    /** The tabs of the config screen, in the order Cloth shows them (first field of each wins). */
    private static final List<String> EXPECTED_TABS =
            List.of("building", "equipment", "pistons", "tweaks", "world", "visuals", "advanced", "server");

    /**
     * Fields that are persisted but deliberately not options: legacy keys read only for a
     * migration ({@code @ConfigEntry.Gui.Excluded}). Everything else the pin above lists as a value
     * has to be reachable through {@code ConfigOptions} - and so through the screen and the command.
     */
    private static final Set<String> EXCLUDED_KEYS =
            Set.of("tweaks.spawn.spawnTeleporterCount", "tweaks.laserPointer.showLine");

    /**
     * The config screen explains itself: every option a player can see has a name and a tooltip in
     * English and German, every tooltip states the default, every top level field sits on one of
     * the seven tabs (most important first), every tab and every group has a name, and the title
     * exists. On top of that the option list the command and the wiki read ({@code ConfigOptions})
     * is exactly the pinned list minus the excluded legacy keys, and its client-side and
     * on-reload marks name real options.
     *
     * <p>What breaks it: a new field without lang keys (Cloth shows the raw key), a tooltip
     * without its default, a top level field without {@code @ConfigEntry.Category} (Cloth opens a
     * stray "default" tab), a group without {@code TransitiveObject} on its tab, a reordered tab,
     * or {@code ConfigOptions} skipping or inventing an option.
     */
    public static void everyOptionHasNameTooltipAndTab(GameTestHelper helper) {
        JsonObject en = configLang(helper, "en_us");
        JsonObject de = configLang(helper, "de_de");
        String prefix = "text.autoconfig." + SimpleBuildingGameTests.MOD_ID + ".";
        List<String> problems = new ArrayList<>();
        requireLang(problems, en, de, prefix + "title");

        List<String> tabs = new ArrayList<>();
        for (Field field : SimplebuildingConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())
                    || field.isAnnotationPresent(me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded.class)) {
                continue;
            }
            me.shedaniel.autoconfig.annotation.ConfigEntry.Category category =
                    field.getAnnotation(me.shedaniel.autoconfig.annotation.ConfigEntry.Category.class);
            if (category == null) {
                problems.add(field.getName() + " carries no @ConfigEntry.Category, so Cloth puts it on a stray \"default\" tab");
                continue;
            }
            if (!tabs.contains(category.value())) {
                tabs.add(category.value());
            }
            if (!com.simplebuilding.config.ConfigOptions.isValueType(field.getType())
                    && !field.isAnnotationPresent(me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject.class)) {
                problems.add(field.getName() + " is a group on a tab without @ConfigEntry.Gui.TransitiveObject");
            }
        }
        helper.assertTrue(tabs.equals(EXPECTED_TABS), "config screen tabs in order: " + tabs + " instead of " + EXPECTED_TABS);
        for (String tab : EXPECTED_TABS) {
            requireLang(problems, en, de, prefix + "category." + tab);
        }

        Set<String> groups = new TreeSet<>();
        Set<String> paths = new TreeSet<>();
        for (com.simplebuilding.config.ConfigOptions.Option option : com.simplebuilding.config.ConfigOptions.all()) {
            paths.add(option.path());
            String key = prefix + "option." + option.path();
            requireLang(problems, en, de, key);
            if (!option.field().isAnnotationPresent(me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip.class)) {
                problems.add(option.path() + " has no @ConfigEntry.Gui.Tooltip, so the screen shows no explanation");
            } else {
                requireLang(problems, en, de, key + ".@Tooltip");
                if (en.has(key + ".@Tooltip") && !en.get(key + ".@Tooltip").getAsString().contains("Default:")) {
                    problems.add(key + ".@Tooltip (en_us) does not state the default");
                }
                if (de.has(key + ".@Tooltip") && !de.get(key + ".@Tooltip").getAsString().contains("Standard:")) {
                    problems.add(key + ".@Tooltip (de_de) does not state the default");
                }
            }
            int dot = option.path().lastIndexOf('.');
            while (dot > 0) {
                groups.add(option.path().substring(0, dot));
                dot = option.path().lastIndexOf('.', dot - 1);
            }
        }
        for (String group : groups) {
            requireLang(problems, en, de, prefix + "option." + group);
        }

        Set<String> pinned = new TreeSet<>();
        for (String entry : EXPECTED_OPTIONS) {
            if (entry.contains(" group:") || entry.contains("runtime-only")) {
                continue;
            }
            String qualified = entry.substring(0, entry.indexOf(' '));
            String path = qualified.startsWith("root.") ? qualified.substring("root.".length()) : qualified;
            if (!EXCLUDED_KEYS.contains(path)) {
                pinned.add(path);
            }
        }
        helper.assertTrue(paths.equals(pinned), "ConfigOptions lists " + paths + " but the pinned options are " + pinned);
        for (String path : com.simplebuilding.config.ConfigOptions.CLIENT_SIDE) {
            if (!paths.contains(path)) problems.add("ConfigOptions.CLIENT_SIDE names " + path + ", which is no option");
        }
        for (String path : com.simplebuilding.config.ConfigOptions.APPLY_ON_RELOAD) {
            if (!paths.contains(path)) problems.add("ConfigOptions.APPLY_ON_RELOAD names " + path + ", which is no option");
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " config screen problems:\n" + String.join("\n", problems));
        done(helper);
    }

    /**
     * {@code /simplebuilding config get|set|reset|list} reaches every option by its path: get
     * answers for all of them, set parses the value, clamps it like a loaded file, writes the live
     * config and saves once, reset brings back the default, and nonsense (an unknown option, a
     * value of the wrong type) changes nothing and reports failure.
     *
     * <p>What breaks it: an option the command cannot find, set not saving or not writing the live
     * object, no clamping (a negative rotator cost would reach the item), or a parse that accepts
     * garbage.
     */
    public static void theConfigCommandReachesEveryOption(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        ServerPlayer player = mockPlayer(helper);
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        Runnable installedSaver = com.simplebuilding.config.ConfigSaving.saver();
        double magnet = config.tools.magnetRangeMultiplier;
        int rotator = config.tools.rotatorChargePerTurn;
        boolean flypads = config.tweaks.pads.enableFlypads;
        Runnable restore = () -> {
            SimplebuildingConfig live = Simplebuilding.getConfig();
            live.tools.magnetRangeMultiplier = magnet;
            live.tools.rotatorChargePerTurn = rotator;
            live.tweaks.pads.enableFlypads = flypads;
            com.simplebuilding.config.ConfigSaving.setSaver(installedSaver);
        };
        restoreAtEnd(helper, restore);
        int[] saves = {0};
        List<String> problems = new ArrayList<>();
        try {
            com.simplebuilding.config.ConfigSaving.setSaver(() -> saves[0]++);
            players.op(player.nameAndId());
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            var source = player.createCommandSourceStack().withSuppressedOutput();
            for (com.simplebuilding.config.ConfigOptions.Option option : com.simplebuilding.config.ConfigOptions.all()) {
                if (dispatcher.execute("simplebuilding config get " + option.path(), source) != 1) {
                    problems.add("get " + option.path() + " did not answer");
                }
            }
            helper.assertTrue(problems.isEmpty(), "the config command cannot read: " + problems);

            helper.assertTrue(dispatcher.execute("simplebuilding config set tools.magnetRangeMultiplier 2.5", source) == 1,
                    "set tools.magnetRangeMultiplier 2.5 was refused");
            helper.assertTrue(config.tools.magnetRangeMultiplier == 2.5,
                    "set wrote " + config.tools.magnetRangeMultiplier + " instead of 2.5 into the live config");
            helper.assertTrue(saves[0] == 1, "set saved the config " + saves[0] + " times instead of once");

            dispatcher.execute("simplebuilding config set tweaks.pads.enableFlypads off", source);
            helper.assertFalse(config.tweaks.pads.enableFlypads, "set tweaks.pads.enableFlypads off left flypads on");

            dispatcher.execute("simplebuilding config set tools.rotatorChargePerTurn -5", source);
            helper.assertTrue(config.tools.rotatorChargePerTurn == 0,
                    "a negative rotator cost was stored as " + config.tools.rotatorChargePerTurn + " instead of clamped to 0");

            helper.assertTrue(dispatcher.execute("simplebuilding config set tools.magnetRangeMultiplier lots", source) == 0,
                    "set accepted \"lots\" as a double");
            helper.assertTrue(config.tools.magnetRangeMultiplier == 2.5, "a refused value still changed the option");
            helper.assertTrue(dispatcher.execute("simplebuilding config get tools.noSuchOption", source) == 0,
                    "get answered for an option that does not exist");

            dispatcher.execute("simplebuilding config reset tools.magnetRangeMultiplier", source);
            helper.assertTrue(config.tools.magnetRangeMultiplier == 1.0,
                    "reset left tools.magnetRangeMultiplier at " + config.tools.magnetRangeMultiplier + " instead of the default 1.0");

            long toolOptions = com.simplebuilding.config.ConfigOptions.all().stream().filter(o -> o.path().startsWith("tools.")).count();
            helper.assertTrue(dispatcher.execute("simplebuilding config list tools.", source) == toolOptions,
                    "list tools. did not list the " + toolOptions + " tool options");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("config command failed: " + e.getMessage());
        } finally {
            restore.run();
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        done(helper);
    }

    /**
     * The three new tool options change what the tools do: the hunger multiplier scales the
     * exhaustion of a paid wand block (0 = free), the magnet multiplier decides whether an item six
     * blocks away is pulled, and the rotator cost is what one real turn takes off the charge.
     */
    public static void newToolOptionsChangeWhatTheToolsDo(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        double hunger = config.tools.wandHungerMultiplier;
        double magnet = config.tools.magnetRangeMultiplier;
        int rotatorCost = config.tools.rotatorChargePerTurn;
        boolean hungerOn = config.tools.buildingWandHungerCost;
        Runnable restore = () -> {
            SimplebuildingConfig live = Simplebuilding.getConfig();
            live.tools.wandHungerMultiplier = hunger;
            live.tools.magnetRangeMultiplier = magnet;
            live.tools.rotatorChargePerTurn = rotatorCost;
            live.tools.buildingWandHungerCost = hungerOn;
        };
        restoreAtEnd(helper, restore);
        ServerPlayer player = mockPlayer(helper);
        player.getAbilities().instabuild = false;
        try {
            // --- tools.wandHungerMultiplier ---
            config.tools.buildingWandHungerCost = true;
            net.minecraft.world.item.Item wand = ModItems.COPPER_BUILDING_WAND;
            long paid = com.simplebuilding.util.WandHunger.ALLOWANCE[0] + 1;
            float base = (float) com.simplebuilding.util.WandHunger.PER_BLOCK[0];
            config.tools.wandHungerMultiplier = 1.0;
            float normal = com.simplebuilding.util.WandHunger.exhaust(player, wand, paid);
            config.tools.wandHungerMultiplier = 2.0;
            float doubled = com.simplebuilding.util.WandHunger.exhaust(player, wand, paid);
            config.tools.wandHungerMultiplier = 0.0;
            float free = com.simplebuilding.util.WandHunger.exhaust(player, wand, paid);
            helper.assertTrue(normal == base, "a paid copper wand block cost " + normal + " exhaustion at multiplier 1 instead of " + base);
            helper.assertTrue(Math.abs(doubled - 2 * base) < 1e-9, "multiplier 2 cost " + doubled + " instead of " + (2 * base));
            helper.assertTrue(free == 0.0F, "multiplier 0 still cost " + free + " exhaustion");

            // --- tools.magnetRangeMultiplier: an item 6 blocks away, outside the base range of 4 ---
            Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            ItemStack magnetStack = new ItemStack(ModItems.MAGNET);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, magnetStack);
            net.minecraft.world.entity.item.ItemEntity loose = new net.minecraft.world.entity.item.ItemEntity(
                    helper.getLevel(), at.x + 6.0, at.y + 0.5, at.z, new ItemStack(Items.STONE));
            loose.setNoGravity(true);
            helper.getLevel().addFreshEntity(loose);
            try {
                config.tools.magnetRangeMultiplier = 1.0;
                loose.setPickUpDelay(40);
                magnetStack.getItem().inventoryTick(magnetStack, helper.getLevel(), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                helper.assertTrue(loose.hasPickUpDelay(), "the magnet pulled an item 6 blocks away at its normal range of 4");
                config.tools.magnetRangeMultiplier = 2.0;
                magnetStack.getItem().inventoryTick(magnetStack, helper.getLevel(), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                helper.assertFalse(loose.hasPickUpDelay(), "at range multiplier 2 (8 blocks) the magnet did not pull an item 6 blocks away");
            } finally {
                loose.discard();
            }

            // --- tools.rotatorChargePerTurn: one real turn of a log ---
            BlockPos log = new BlockPos(2, 1, 2);
            int[] costs = {1, 3, 0};
            for (int cost : costs) {
                config.tools.rotatorChargePerTurn = cost;
                helper.setBlock(log, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Y));
                ItemStack rotator = new ItemStack(ModItems.ROTATOR);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rotator);
                BlockPos abs = helper.absolutePos(log);
                Vec3 hit = new Vec3(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);
                net.minecraft.world.InteractionResult result = rotator.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                        player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(hit, net.minecraft.core.Direction.UP, abs, false)));
                helper.assertTrue(result == net.minecraft.world.InteractionResult.SUCCESS, "the rotator refused to turn the log at cost " + cost + ": " + result);
                helper.assertTrue(rotator.getDamageValue() == cost,
                        "one turn at tools.rotatorChargePerTurn " + cost + " took " + rotator.getDamageValue() + " charge");
            }
        } finally {
            restore.run();
        }
        done(helper);
    }

    /**
     * The new pad options change what the pads do: the potion pad switch stops a filled pad, the
     * charge step decides when the first 25 % arrive, the cooldown factor sets the cooldown after
     * the full charge (0 = none), and the teleporter warm-ups and the launchpad multiplier feed the
     * countdown and the launch the block entities use.
     */
    public static void newPadOptionsChangeWhatThePadsDo(GameTestHelper helper) {
        TweaksConfig tweaks = liveConfig(helper).tweaks;
        boolean potionPads = tweaks.pads.enablePotionPads;
        int step = tweaks.padTuning.potionPadChargeStepTicks;
        double cooldownFactor = tweaks.padTuning.potionPadCooldownFactor;
        int warmup = tweaks.padTuning.teleporterTier1WarmupTicks;
        int netheriteWarmup = tweaks.padTuning.teleporterTier2WarmupTicks;
        int enderiteWarmup = tweaks.padTuning.teleporterTier3WarmupTicks;
        double launch = tweaks.padTuning.launchpadStrengthMultiplier;
        Runnable restore = () -> {
            TweaksConfig live = Simplebuilding.getConfig().tweaks;
            live.pads.enablePotionPads = potionPads;
            live.padTuning.potionPadChargeStepTicks = step;
            live.padTuning.potionPadCooldownFactor = cooldownFactor;
            live.padTuning.teleporterTier1WarmupTicks = warmup;
            live.padTuning.teleporterTier2WarmupTicks = netheriteWarmup;
            live.padTuning.teleporterTier3WarmupTicks = enderiteWarmup;
            live.padTuning.launchpadStrengthMultiplier = launch;
        };
        restoreAtEnd(helper, restore);
        try {
            // --- tweaks.pads.enablePotionPads ---
            BlockPos off = new BlockPos(1, 1, 1);
            ServerPlayer first = padPlayer(helper, off);
            fillPotionPad(helper, off);
            tweaks.pads.enablePotionPads = false;
            tickPotionPad(helper, off, 3 * com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity.RAMP_STEP_TICKS);
            helper.assertFalse(first.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "a switched off potion pad still gave swiftness");
            tweaks.pads.enablePotionPads = true;
            first.removeAllEffects();

            // --- tweaks.padTuning.potionPadChargeStepTicks + potionPadCooldownFactor ---
            BlockPos fast = new BlockPos(4, 1, 1);
            ServerPlayer second = padPlayer(helper, fast);
            com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity pad = fillPotionPad(helper, fast);
            int full = ((com.simplebuilding.tweaks.block.PotionPadBlock) com.simplebuilding.tweaks.block.TweaksBlocks.POTION_PAD)
                    .effectDurationAt(helper.getLevel(), helper.absolutePos(fast));
            tweaks.padTuning.potionPadChargeStepTicks = 5;
            tweaks.padTuning.potionPadCooldownFactor = 0.5;
            tickPotionPad(helper, fast, 4);
            helper.assertFalse(second.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "the pad gave swiftness before the first 5 tick step");
            tickPotionPad(helper, fast, 1);
            net.minecraft.world.effect.MobEffectInstance speed = second.getEffect(net.minecraft.world.effect.MobEffects.SPEED);
            helper.assertTrue(speed != null && speed.getDuration() == full / 4,
                    "after one 5 tick step the pad gave " + (speed == null ? "nothing" : speed.getDuration() + " ticks") + " instead of 25 % of " + full);
            tickPotionPad(helper, fast, 10);
            helper.assertTrue(pad.getCooldown() == full / 2,
                    "cooldown factor 0.5 set a cooldown of " + pad.getCooldown() + " instead of half the duration " + (full / 2));

            BlockPos none = new BlockPos(1, 1, 4);
            padPlayer(helper, none);
            com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity noCooldown = fillPotionPad(helper, none);
            tweaks.padTuning.potionPadCooldownFactor = 0.0;
            tickPotionPad(helper, none, 15);
            helper.assertFalse(noCooldown.isCoolingDown(), "cooldown factor 0 still put the pad on a cooldown of " + noCooldown.getCooldown());

            tweaks.padTuning.potionPadCooldownFactor = 2.0;
            helper.assertTrue(((com.simplebuilding.tweaks.block.PotionPadBlock) com.simplebuilding.tweaks.block.TweaksBlocks.POTION_PAD)
                            .cooldownAt(helper.getLevel(), helper.absolutePos(none)) == 2 * full,
                    "the default cooldown factor no longer gives twice the effect duration");

            // --- tweaks.padTuning.teleporterTier1/2/3WarmupTicks (drei Stufen seit 2026-09-28) ---
            int enderiteTier = com.simplebuilding.tweaks.block.SpawnTeleporterBlock.ENDERITE_TIER;
            helper.assertTrue(com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(1) == 1000
                            && com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(2) == 400
                            && com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(enderiteTier) == 100,
                    "the default teleporter warm-ups are no longer 1000, 400 and 100 ticks");
            tweaks.padTuning.teleporterTier1WarmupTicks = 40;
            tweaks.padTuning.teleporterTier2WarmupTicks = 20;
            tweaks.padTuning.teleporterTier3WarmupTicks = 10;
            helper.assertTrue(com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(1) == 40,
                    "teleporterTier1WarmupTicks 40 did not shorten the countdown of a tier I teleporter");
            helper.assertTrue(com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(2) == 20,
                    "teleporterTier2WarmupTicks 20 did not shorten the countdown of a tier II teleporter");
            helper.assertTrue(com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity.requiredTicks(enderiteTier) == 10,
                    "teleporterTier3WarmupTicks 10 did not shorten the countdown of the Enderite teleporter");

            // --- tweaks.padTuning.launchpadStrengthMultiplier ---
            double normal = com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity.strengthFor(4);
            tweaks.padTuning.launchpadStrengthMultiplier = 0.5;
            double halved = com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity.strengthFor(4);
            helper.assertTrue(Math.abs(normal - 4.7) < 1e-9, "a launchpad with 4 charges launches at " + normal + " instead of 1.5 + 4 x 0.8");
            helper.assertTrue(Math.abs(halved - normal / 2) < 1e-9, "launchpadStrengthMultiplier 0.5 gave " + halved + " instead of " + (normal / 2));
        } finally {
            restore.run();
        }
        done(helper);
    }

    /**
     * The new tweak options change what the tweaks do: the kill radius decides whether
     * {@code /killboats} reaches a boat, the XP merge radius whether a neighbour orb is swallowed,
     * both lens costs what a second of beaming and one effect take off the charge, and the echo
     * sounder cooldown whether a jump leaves the item on cooldown.
     */
    public static void newTweakOptionsChangeWhatTheTweaksDo(GameTestHelper helper) {
        TweaksConfig tweaks = liveConfig(helper).tweaks;
        int radius = tweaks.commands.killCommandRadius;
        boolean killBoats = tweaks.commands.enableKillBoatsCommand;
        double clump = tweaks.optimization.xpClumpRadius;
        int beam = tweaks.laserPointer.chargePerSecond;
        int effect = tweaks.laserPointer.effectCost;
        boolean lens = tweaks.laserPointer.enable;
        int echo = tweaks.balancing.echoSounderJumpCooldownTicks;
        Runnable restore = () -> {
            TweaksConfig live = Simplebuilding.getConfig().tweaks;
            live.commands.killCommandRadius = radius;
            live.commands.enableKillBoatsCommand = killBoats;
            live.optimization.xpClumpRadius = clump;
            live.laserPointer.chargePerSecond = beam;
            live.laserPointer.effectCost = effect;
            live.laserPointer.enable = lens;
            live.balancing.echoSounderJumpCooldownTicks = echo;
        };
        restoreAtEnd(helper, restore);
        ServerPlayer player = mockPlayer(helper);
        player.getAbilities().instabuild = false;
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        try {
            // --- tweaks.commands.killCommandRadius: a boat 5 blocks away ---
            Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            tweaks.commands.enableKillBoatsCommand = true;
            players.op(player.nameAndId());
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            var source = player.createCommandSourceStack().withSuppressedOutput();
            net.minecraft.world.entity.Entity boat = spawnBoat(helper, new BlockPos(6, 1, 1));
            tweaks.commands.killCommandRadius = 2;
            dispatcher.execute("killboats", source);
            helper.assertTrue(boat.isAlive(), "/killboats with radius 2 removed a boat about 5 blocks away");
            tweaks.commands.killCommandRadius = 100;
            dispatcher.execute("killboats", source);
            helper.assertFalse(boat.isAlive(), "/killboats with the default radius 100 left a boat 5 blocks away");

            // --- tweaks.optimization.xpClumpRadius: an orb 3 blocks away ---
            Vec3 spot = helper.absoluteVec(new Vec3(3.5, 2.0, 5.5));
            net.minecraft.world.entity.ExperienceOrb a = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), spot.x, spot.y, spot.z, 3);
            net.minecraft.world.entity.ExperienceOrb b = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), spot.x + 3.0, spot.y, spot.z, 4);
            a.setNoGravity(true);
            b.setNoGravity(true);
            helper.getLevel().addFreshEntity(a);
            helper.getLevel().addFreshEntity(b);
            try {
                tweaks.optimization.xpClumpRadius = 2.0;
                helper.assertTrue(com.simplebuilding.tweaks.xp.XpClumping.clump(a) == 0, "an orb 3 blocks away was merged at the default radius 2");
                tweaks.optimization.xpClumpRadius = 4.0;
                helper.assertTrue(com.simplebuilding.tweaks.xp.XpClumping.clump(a) == 1, "xpClumpRadius 4 did not merge an orb 3 blocks away");
                helper.assertTrue(a.getValue() == 7, "the merged orb holds " + a.getValue() + " experience instead of 3 + 4");
            } finally {
                a.discard();
                b.discard();
            }

            // --- tweaks.laserPointer.chargePerSecond: the first tick of beaming ---
            tweaks.laserPointer.enable = true;
            ItemStack beamLens = new ItemStack(com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, beamLens);
            tweaks.laserPointer.chargePerSecond = 3;
            beamLens.getItem().onUseTick(helper.getLevel(), player, beamLens, beamLens.getItem().getUseDuration(beamLens, player));
            helper.assertTrue(beamLens.getDamageValue() == 3, "the first second of beaming at cost 3 took " + beamLens.getDamageValue() + " charge");
            com.simplebuilding.tweaks.item.LaserBeam.reset(player);

            // --- tweaks.laserPointer.effectCost: lighting a candle ---
            int[] effectCosts = {5, 2};
            for (int cost : effectCosts) {
                tweaks.laserPointer.effectCost = cost;
                BlockPos candle = new BlockPos(3, 1, 1);
                helper.setBlock(candle, net.minecraft.world.level.block.Blocks.CANDLE);
                ItemStack effectLens = new ItemStack(com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER);
                BlockPos abs = helper.absolutePos(candle);
                net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                        Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
                com.simplebuilding.tweaks.item.LaserBeam.Effect fired = null;
                for (int tick = 0; tick < 200 && fired == null; tick++) {
                    fired = com.simplebuilding.tweaks.item.LaserBeam.beamAt(player, effectLens, hit);
                }
                helper.assertTrue(fired == com.simplebuilding.tweaks.item.LaserBeam.Effect.LIGHT, "the beam did not light the candle: " + fired);
                helper.assertTrue(effectLens.getDamageValue() == cost, "lighting a candle at effectCost " + cost + " took " + effectLens.getDamageValue() + " charge");
            }

            // --- tweaks.balancing.echoSounderJumpCooldownTicks ---
            BlockPos lodestone = new BlockPos(6, 1, 6);
            helper.setBlock(lodestone, net.minecraft.world.level.block.Blocks.LODESTONE);
            int[] cooldowns = {0, 480};
            for (int cooldown : cooldowns) {
                tweaks.balancing.echoSounderJumpCooldownTicks = cooldown;
                ServerPlayer jumper = mockPlayer(helper);
                jumper.getAbilities().instabuild = false;
                ItemStack compass = new ItemStack(com.simplebuilding.tweaks.item.TweaksItems.ECHO_COMPASS);
                compass.set(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
                        Optional.of(net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
                jumper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, compass);
                helper.assertTrue(com.simplebuilding.tweaks.item.EchoCompassItem.teleport(jumper, net.minecraft.world.InteractionHand.MAIN_HAND, compass),
                        "the echo sounder refused to jump (cooldown option " + cooldown + ")");
                helper.assertTrue(jumper.getCooldowns().isOnCooldown(compass) == (cooldown > 0),
                        "echoSounderJumpCooldownTicks " + cooldown + (cooldown > 0 ? " left the echo sounder without a cooldown" : " still put it on cooldown"));
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("/killboats failed: " + e.getMessage());
        } finally {
            restore.run();
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        done(helper);
    }

    /**
     * {@code worldGen.buildingCoreLootChanceMultiplier} scales the core pools: the End City chest
     * rolled 200 times gives an Enderite core every time at a huge multiplier (the chance is capped
     * at one per chest) and never at 0, where the core pool is left out entirely.
     */
    public static void coreLootChanceFollowsItsMultiplier(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean loot = config.worldGen.enableLootTableChanges;
        double multiplier = config.worldGen.buildingCoreLootChanceMultiplier;
        Runnable restore = () -> {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = loot;
            Simplebuilding.getConfig().worldGen.buildingCoreLootChanceMultiplier = multiplier;
        };
        restoreAtEnd(helper, restore);
        try {
            setLootTableChanges(helper, true);
            config.worldGen.buildingCoreLootChanceMultiplier = 1.0;
            helper.assertTrue(ModLootTableModifications.coreChance(ModLootTableModifications.ENDERITE_CORE_CHANCE)
                    == ModLootTableModifications.ENDERITE_CORE_CHANCE, "the default multiplier changed the Enderite core chance");
            int chests = 200;
            config.worldGen.buildingCoreLootChanceMultiplier = 1000.0;
            int always = countCores(helper, recordPools(BuiltInLootTables.END_CITY_TREASURE, registries), chests);
            config.worldGen.buildingCoreLootChanceMultiplier = 0.0;
            int never = countCores(helper, recordPools(BuiltInLootTables.END_CITY_TREASURE, registries), chests);
            helper.assertTrue(always == chests, "at multiplier 1000 the End City gave " + always + " Enderite cores in " + chests + " chests instead of one each");
            helper.assertTrue(never == 0, "at multiplier 0 the End City still gave " + never + " Enderite cores");
        } finally {
            restore.run();
        }
        done(helper);
    }

    /**
     * The air jump cooldown is the server's: it travels in the tweaks payload (encoded and decoded
     * here), and the level halving both sides share lives in one place ({@code AirJumpGuard}).
     */
    public static void theAirJumpCooldownTravelsFromServerToClient(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        int original = config.airJumpCooldownTicks;
        restoreAtEnd(helper, () -> Simplebuilding.getConfig().airJumpCooldownTicks = original);
        try {
            config.airJumpCooldownTicks = 40;
            com.simplebuilding.tweaks.SimpleTweaks.ServerValues local = com.simplebuilding.tweaks.SimpleTweaks.localValues();
            helper.assertTrue(local.airJumpCooldownTicks() == 40, "the server values carry an air jump cooldown of " + local.airJumpCooldownTicks() + " instead of 40");
            net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            com.simplebuilding.tweaks.network.TweaksConfigPayload.CODEC.encode(buf, com.simplebuilding.tweaks.network.TweaksConfigPayload.of(local));
            com.simplebuilding.tweaks.network.TweaksConfigPayload decoded = com.simplebuilding.tweaks.network.TweaksConfigPayload.CODEC.decode(buf);
            helper.assertTrue(decoded.values().equals(local), "the tweaks payload lost values on the way: " + decoded.values() + " instead of " + local);
            helper.assertTrue(com.simplebuilding.util.AirJumpGuard.cooldownTicks(1) == 40 && com.simplebuilding.util.AirJumpGuard.cooldownTicks(2) == 20,
                    "the server guard does not wait 40 / 20 ticks at airJumpCooldownTicks 40");
            helper.assertTrue(com.simplebuilding.util.AirJumpGuard.cooldownTicks(1, -5) == 0 && com.simplebuilding.util.AirJumpGuard.cooldownTicks(2, 1) == 1,
                    "a negative base must count as 0 and level II waits at least one tick");
        } finally {
            config.airJumpCooldownTicks = original;
        }
        done(helper);
    }

    // --- helpers of the Config-Umbau tests ---

    private static JsonObject configLang(GameTestHelper helper, String locale) {
        String path = "assets/" + SimpleBuildingGameTests.MOD_ID + "/lang/" + locale + ".json";
        try (java.io.InputStream in = ConfigOptionTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private static void requireLang(List<String> problems, JsonObject en, JsonObject de, String key) {
        if (!en.has(key) || en.get(key).getAsString().isBlank()) problems.add(key + " missing in en_us");
        if (!de.has(key) || de.get(key).getAsString().isBlank()) problems.add(key + " missing in de_de");
    }

    private static ServerPlayer padPlayer(GameTestHelper helper, BlockPos pad) {
        ServerPlayer player = mockPlayer(helper);
        Vec3 on = helper.absoluteVec(new Vec3(pad.getX() + 0.5, pad.getY() + 1.0 / 16.0, pad.getZ() + 0.5));
        player.snapTo(on.x, on.y, on.z, 0.0F, 0.0F);
        player.removeAllEffects();
        return player;
    }

    private static com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity fillPotionPad(GameTestHelper helper, BlockPos pad) {
        helper.setBlock(pad, com.simplebuilding.tweaks.block.TweaksBlocks.POTION_PAD);
        com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity be =
                helper.getBlockEntity(pad, com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity.class);
        com.simplebuilding.tweaks.block.PotionPadBlock.absorb(be, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        return be;
    }

    private static void tickPotionPad(GameTestHelper helper, BlockPos pad, int ticks) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pad);
        for (int i = 0; i < ticks; i++) {
            com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity be =
                    helper.getBlockEntity(pad, com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity.class);
            com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity.serverTick(level, abs, level.getBlockState(abs), be);
        }
    }

    private static int countCores(GameTestHelper helper, PoolRecorder recorder, int chests) {
        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(POOL_ROLL_SEED)
                .create(Optional.empty());
        int[] cores = {0};
        for (int chest = 0; chest < chests; chest++) {
            for (LootPool pool : recorder.pools) {
                pool.addRandomItems(stack -> {
                    if (stack.is(ModItems.ENDERITE_CORE)) {
                        cores[0] += stack.getCount();
                    }
                }, context);
            }
        }
        return cores[0];
    }

    private static void restoreAtEnd(GameTestHelper helper, Runnable restore) {
        helper.runBeforeTestEnd(restore);
    }

    private static void done(GameTestHelper helper) {
        helper.succeed();
    }

    private static net.minecraft.world.entity.Entity spawnBoat(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(net.minecraft.world.entity.EntityTypes.OAK_BOAT, pos);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** The config object the mod actually reads, with the two groups proven to exist. */
    private static SimplebuildingConfig liveConfig(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null,
                "Simplebuilding.getConfig() handed out no config at all; every option below would be untestable");
        helper.assertTrue(config.tools != null, "the config has no \"tools\" group");
        helper.assertTrue(config.worldGen != null, "the config has no \"worldGen\" group");
        return config;
    }

    /**
     * Creates a mock server player and makes sure it leaves the server again once the test is over.
     * A leaked mock player keeps the player list non-empty and the gametest server then stalls on
     * shutdown - a failing test would cost minutes of wall clock instead of seconds.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Writes an option and reads it straight back through the same accessor the mod uses. If
     * {@code getConfig()} ever started handing out copies instead of the live object, the tests
     * would otherwise fail somewhere far away with a confusing message; this fails right here.
     */
    private static void setBundleInversion(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().tools.invertBundleInteractions = value;
        helper.assertTrue(Simplebuilding.getConfig().tools.invertBundleInteractions == value,
                "tools.invertBundleInteractions did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    /** See {@link #setBundleInversion}. */
    private static void setLootTableChanges(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().worldGen.enableLootTableChanges = value;
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges == value,
                "worldGen.enableLootTableChanges did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    // --- interaction 1: bundle on the cursor, item in the slot (overrideStackedOnOther) ---

    /**
     * One click with the bundle on the cursor while the slot below holds eight stone. Off, that is
     * what fills the bundle; inverted, it is what does nothing at all.
     */
    private static void assertSlotFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                            boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 8));
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should be empty afterwards but holds " + slot.getItem());
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone left in the slot");
        }
    }

    /**
     * One click with a bundle that holds eight stone on the cursor, over an empty slot. Off, that
     * is what takes the stone back out; inverted, it is what does nothing at all.
     */
    private static void assertSlotEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                             boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone handed back to the slot");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should have stayed empty but holds " + slot.getItem());
        }
    }

    // --- interaction 2: item on the cursor, bundle in the slot (overrideOtherStackedOnMe) ---

    /**
     * The same fill, from the other side: eight stone on the cursor, clicked onto the bundle lying
     * in a slot. This is the second pair of config reads and the one the player uses most.
     */
    private static void assertCursorFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                              boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        ItemStack cursor = new ItemStack(Items.STONE, 8);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {cursor};

        boolean handled = item.overrideOtherStackedOnMe(bundle, cursor, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(cursor.isEmpty(),
                    what + ": the cursor should be empty afterwards but holds " + cursor);
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(cursor.getCount(), 8, what + ": stone left on the cursor");
        }
    }

    /**
     * The same emptying, from the other side: an empty cursor clicked onto a filled bundle lying in
     * a slot. The stack comes back onto the cursor through the {@code SlotAccess}, which is what
     * this records.
     */
    private static void assertCursorEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                               boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {ItemStack.EMPTY};

        boolean handled = item.overrideOtherStackedOnMe(bundle, ItemStack.EMPTY, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(cursorSlot[0].getCount(), 8,
                    what + ": stone handed back onto the cursor");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(cursorSlot[0].isEmpty(),
                    what + ": the cursor should have stayed empty but holds " + cursorSlot[0]);
        }
    }

    // --- the partial insert, on both interaction paths ---

    /**
     * One insert click on a bundle that has room for only part of the offered stack, driven
     * through both interaction methods. Everything else in this test offers eight stone to an
     * empty bundle and therefore never reaches the branch where the bundle takes less than it was
     * given.
     *
     * <p>Nothing is copied out of the item: the capacity is measured on a scratch bundle first,
     * the room left after one 64 stack and the expected leftover are derived from that
     * measurement, and the setup guard states the assumption that derivation rests on. A balance
     * change to the capacity table moves the numbers with it instead of turning this red.
     */
    private static void assertPartialFillClick(GameTestHelper helper, ServerPlayer player,
                                               ClickAction insertClick, String what) {
        int capacity = measureStoneCapacity(helper, player);
        helper.assertTrue(capacity > 64 && capacity < 128,
                what + " / partial: setup guard - a reinforced bundle takes " + capacity + " stone, so "
                        + "one vanilla stack no longer leaves a remainder between 1 and 63 and this case "
                        + "would stop being a partial insert at all");
        int room = capacity - 64;
        int leftOver = 64 - room;

        // --- bundle on the cursor, the offered stack lying in the slot ---
        ItemStack onCursor = nearlyFullBundle(helper, player, what);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 64));
        Slot slot = new Slot(container, 0, 0, 0);

        helper.assertTrue(((ReinforcedBundleItem) onCursor.getItem())
                        .overrideStackedOnOther(onCursor, slot, insertClick, player),
                what + " / partial / slot: the insert click was not handled although " + room
                        + " more stone still fit");
        helper.assertValueEqual(countInBundle(onCursor, Items.STONE), capacity,
                what + " / partial / slot: stone inside the bundle - it may take only the " + room
                        + " that fit");
        helper.assertValueEqual(slot.getItem().getCount(), leftOver,
                what + " / partial / slot: stone left in the slot - the bundle took " + room + " of 64, "
                        + "so the other " + leftOver + " have to stay where the player put them");

        // --- the offered stack on the cursor, the bundle lying in the slot ---
        ItemStack inSlot = nearlyFullBundle(helper, player, what);
        ItemStack cursor = new ItemStack(Items.STONE, 64);
        SimpleContainer holder = new SimpleContainer(1);
        holder.setItem(0, inSlot);
        Slot bundleSlot = new Slot(holder, 0, 0, 0);
        ItemStack[] cursorSlot = {cursor};

        helper.assertTrue(((ReinforcedBundleItem) inSlot.getItem()).overrideOtherStackedOnMe(
                        inSlot, cursor, bundleSlot, insertClick, player,
                        SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack)),
                what + " / partial / cursor: the insert click was not handled although " + room
                        + " more stone still fit");
        helper.assertValueEqual(countInBundle(inSlot, Items.STONE), capacity,
                what + " / partial / cursor: stone inside the bundle - it may take only the " + room
                        + " that fit");
        helper.assertValueEqual(cursor.getCount(), leftOver,
                what + " / partial / cursor: stone left on the cursor - the bundle took " + room
                        + " of 64, so the other " + leftOver + " have to stay on the cursor");
    }

    /**
     * How much stone a fresh reinforced bundle holds, measured by filling one until it refuses.
     * The world pickup path is used because it carries no config gate, so the measurement cannot
     * depend on the option under test.
     */
    private static int measureStoneCapacity(GameTestHelper helper, ServerPlayer player) {
        ItemStack scratch = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) scratch.getItem();
        for (int offer = 0; offer < 16; offer++) {
            if (!item.tryInsertStackFromWorld(scratch, new ItemStack(Items.STONE, 64), player)) {
                break;
            }
        }
        int capacity = countInBundle(scratch, Items.STONE);
        helper.assertTrue(capacity > 0,
                "a fresh reinforced bundle took no stone at all, so nothing below can be measured");
        return capacity;
    }

    /** A reinforced bundle holding exactly one vanilla stack of stone, so a little room is left. */
    private static ItemStack nearlyFullBundle(GameTestHelper helper, ServerPlayer player, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 64), player),
                what + " / partial: could not put the first 64 stone into the bundle to begin with");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 64,
                what + " / partial: stone in the bundle after the world pickup that sets this case up");
        return bundle;
    }

    /**
     * A reinforced bundle holding eight stone. Filled through the world pickup path on purpose:
     * that one carries no config gate, so the setup cannot quietly depend on the option under test.
     */
    private static ItemStack filledBundle(GameTestHelper helper, ServerPlayer player, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player),
                what + ": could not put stone into the bundle to begin with");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                what + ": stone in the bundle after the world pickup that set the test up");
        return bundle;
    }

    /** How many of {@code item} the bundle holds, across all of its stacks. */
    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : McVersion.bundleItemCopies(contents).toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Runs the mod's loot table hook for one table and counts what it handed over. */
    /**
     * The loot balance: every table the mod edits hands out, per chest, an amount of mod stacks
     * inside its {@link #CHEST_BUDGETS} band, and the ordinary bastion chests never hand out what
     * is meant for the treasure room.
     *
     * <p>{@link #lootTableChangesStopWhenTheOptionIsSwitchedOff} proves what can come out of a
     * chest, not how much of it: an empty weight dropped from 30 to 5 or a {@code between(0, 2)}
     * turned into {@code between(1, 4)} keeps every listed entry and every pool count. This test
     * rolls each table like {@link #POOL_ROLLS} chests - every mod pool once per chest, the way a
     * real chest is filled - and compares the mean number of stacks with the band. Both bounds
     * matter: the upper one is "not too much", the lower one catches a pool whose entries were
     * starved by an oversized empty weight.
     *
     * <p>The bastion half pins the split into a shared pool and a treasure-only pool. The treasure
     * items are lower bounds in {@link #EXPECTED_LOOT}, so moving the netherite core back into the
     * shared pool - every one of the dozen ordinary chests of a bastion - leaves that test green.
     *
     * <p>What breaks it: an empty weight or a roll count changed far enough to leave a band, a
     * table added to {@link #MODIFIED_TABLES} without a budget, or a treasure-room item in the
     * pool every bastion chest gets.
     */
    public static void lootBalanceKeepsEveryChestWithinItsBudget(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        helper.assertTrue(CHEST_BUDGETS.keySet().equals(Set.copyOf(MODIFIED_TABLES)),
                "CHEST_BUDGETS and MODIFIED_TABLES name different tables; every table the mod edits "
                        + "needs a budget, budgets " + CHEST_BUDGETS.keySet() + ", tables " + MODIFIED_TABLES);

        List<String> problems = new ArrayList<>();
        try {
            setLootTableChanges(helper, true);
            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                Budget budget = CHEST_BUDGETS.get(key);
                double mean = meanStacksPerChest(helper, recordPools(key, registries));
                if (mean < budget.min() || mean > budget.max()) {
                    problems.add(tableName(key) + " gives " + String.format(java.util.Locale.ROOT, "%.3f", mean)
                            + " mod stacks per chest, outside " + budget.min() + ".." + budget.max());
                }
            }

            Set<String> ordinary = rollContents(helper, recordPools(BuiltInLootTables.BASTION_OTHER, registries));
            Set<String> treasure = rollContents(helper, recordPools(BuiltInLootTables.BASTION_TREASURE, registries));
            for (String entry : BASTION_TREASURE_ONLY_LOOT) {
                if (ordinary.contains(entry)) {
                    problems.add("the ordinary bastion chests hand out " + entry
                            + ", which belongs to the treasure room only");
                }
                // Control: the same roll finds it in the treasure room, so "absent" above is not
                // just a roller that finds nothing.
                if (!treasure.contains(entry)) {
                    problems.add("the bastion treasure room never hands out " + entry);
                }
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.assertTrue(problems.isEmpty(), "loot balance problems: " + problems);
        helper.succeed();
    }

    /**
     * The building cores are very rare in chests (owner 2026-09-27), the Enderite core rarest of
     * all: every table the mod edits is rolled like {@link #CORE_CHESTS} chests - every mod pool
     * once per chest, fixed seed - and each core's share per chest has to land in its
     * {@link #CORE_CHANCES} band. A core in a table without a band is a failure as well, which
     * covers the copper core (no chest at all) and the Enderite core outside the End City.
     *
     * <p>{@link #lootTableChangesStopWhenTheOptionIsSwitchedOff} only proves a core <em>can</em>
     * come out and {@link #lootBalanceKeepsEveryChestWithinItsBudget} counts stacks of every kind,
     * so moving a core back into a multi roll pool at weight 2, or raising its chance to five per
     * cent, kept both green.
     *
     * <p>What breaks it: a core chance raised or lowered out of its band, a core in a new table, or
     * an Enderite core no longer rarer than every other core.
     */
    public static void buildingCoresAreVeryRareInLootChests(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        List<Item> cores = List.of(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                ModItems.DIAMOND_CORE, ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE);
        List<String> problems = new ArrayList<>();
        double rarestOther = Double.MAX_VALUE;
        double commonestEnderite = 0.0;
        try {
            setLootTableChanges(helper, true);
            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                PoolRecorder recorder = recordPools(key, registries);
                LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
                LootContext context = new LootContext.Builder(params)
                        .withOptionalRandomSeed(POOL_ROLL_SEED)
                        .create(Optional.empty());
                Map<Item, Integer> counts = new LinkedHashMap<>();
                for (int chest = 0; chest < CORE_CHESTS; chest++) {
                    for (LootPool pool : recorder.pools) {
                        pool.addRandomItems(stack -> {
                            if (cores.contains(stack.getItem())) {
                                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                            }
                        }, context);
                    }
                }
                Map<Item, Budget> bands = CORE_CHANCES.getOrDefault(key, Map.of());
                for (Item core : cores) {
                    double share = counts.getOrDefault(core, 0) / (double) CORE_CHESTS;
                    Budget band = bands.get(core);
                    String what = BuiltInRegistries.ITEM.getKey(core) + " in " + tableName(key);
                    if (band == null) {
                        if (share > 0) {
                            problems.add(what + ": " + share + " per chest, but that table is not meant to hold it at all");
                        }
                        continue;
                    }
                    if (share < band.min() || share > band.max()) {
                        problems.add(what + ": " + String.format(java.util.Locale.ROOT, "%.4f", share)
                                + " per chest, outside " + band.min() + ".." + band.max());
                    }
                    if (core == ModItems.ENDERITE_CORE) {
                        commonestEnderite = Math.max(commonestEnderite, share);
                    } else {
                        rarestOther = Math.min(rarestOther, share);
                    }
                }
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }
        helper.assertTrue(problems.isEmpty(), "core loot chances are off:\n" + String.join("\n", problems));
        helper.assertTrue(commonestEnderite > 0 && commonestEnderite < rarestOther,
                "the Enderite core is no longer the rarest core in chests: " + commonestEnderite
                        + " per chest against " + rarestOther + " for the rarest other core");
        helper.succeed();
    }

    /**
     * Mean number of stacks one chest gets from the mod: every recorded pool rolled once per
     * chest, {@link #POOL_ROLLS} chests, fixed seed.
     */
    private static double meanStacksPerChest(GameTestHelper helper, PoolRecorder recorder) {
        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(POOL_ROLL_SEED)
                .create(Optional.empty());
        int[] stacks = {0};
        for (int chest = 0; chest < POOL_ROLLS; chest++) {
            for (LootPool pool : recorder.pools) {
                pool.addRandomItems(stack -> stacks[0]++, context);
            }
        }
        return (double) stacks[0] / POOL_ROLLS;
    }

    private static PoolRecorder recordPools(ResourceKey<LootTable> key, HolderLookup.Provider registries) {
        PoolRecorder recorder = new PoolRecorder();
        ModLootTableModifications.apply(key, recorder, registries);
        return recorder;
    }

    /** Readable name of a loot table for assertion messages. */
    private static String tableName(ResourceKey<LootTable> key) {
        return "the loot table " + key.identifier();
    }

    /**
     * Rolls every pool the mod handed to one table and returns what came out: the registry id of
     * each item, plus {@code <enchantment id>@<level>} for every enchantment stored on an
     * enchanted book.
     *
     * <p>Rolling rather than reading: {@code LootPool} keeps its entries private, and going
     * through {@code addRandomItems} is what a chest does anyway - it covers the entry, its
     * {@code set_components} function and the level inside that component in one step. The seed
     * is fixed and the context is rebuilt per table, so the same pool always produces the same
     * answer here, on every machine.
     */
    private static Set<String> rollContents(GameTestHelper helper, PoolRecorder recorder) {
        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(POOL_ROLL_SEED)
                .create(Optional.empty());

        Set<String> seen = new TreeSet<>();
        for (LootPool pool : recorder.pools) {
            for (int roll = 0; roll < POOL_ROLLS; roll++) {
                pool.addRandomItems(stack -> {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null) {
                        seen.add(itemId.toString());
                    }
                    ItemEnchantments stored = stack.getOrDefault(
                            DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                    for (Holder<Enchantment> enchantment : stored.keySet()) {
                        seen.add(enchantment.getRegisteredName() + BOOK_MARKER + stored.getLevel(enchantment));
                    }
                }, context);
            }
        }
        return seen;
    }

    /** How {@link #EXPECTED_LOOT} spells a plain item: its registry id. */
    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** How {@link #EXPECTED_LOOT} spells an enchanted book: {@code <enchantment id>@<level>}. */
    private static String book(ResourceKey<Enchantment> enchantment, int level) {
        return enchantment.identifier() + BOOK_MARKER + level;
    }

    /**
     * The enchanted books one table rolled that {@link #EXPECTED_LOOT} does not name, as messages.
     *
     * <p>The books are singled out from the plain items because the two are not the same kind of
     * promise. An extra item in a chest is a balance decision; an extra book changes where an
     * enchantment can be obtained, and for
     * {@link #ENCHANTMENTS_WITHOUT_A_CHEST} it changes whether it can be obtained outside creative
     * mode at all. Only the {@code enchantedBook(...)} entries carry stored enchantments - the
     * randomly enchanted tools and quivers in these pools write the ordinary enchantment component
     * instead - so reading the stored ones reads exactly the book entries.
     */
    private static List<String> strayBooks(ResourceKey<LootTable> key, Set<String> wanted, Set<String> rolled) {
        List<String> problems = new ArrayList<>();
        for (String entry : rolled) {
            if (!entry.contains(BOOK_MARKER) || wanted.contains(entry)) {
                continue;
            }
            String banned = bannedEnchantment(entry);
            if (banned != null) {
                problems.add(tableName(key) + " handed out a " + banned + " book (" + entry
                        + "), an enchantment the mod documents as having no source outside the "
                        + "creative inventory");
            } else {
                problems.add(tableName(key) + " handed out the enchanted book " + entry
                        + ", which is not one of the books written down for it (" + wanted + ")");
            }
        }
        return problems;
    }

    /**
     * The banned enchantment behind an {@code <enchantment id>@<level>} entry, or {@code null} if
     * the entry is a plain item or an enchantment that is allowed in a chest.
     */
    private static String bannedEnchantment(String entry) {
        int marker = entry.indexOf(BOOK_MARKER);
        if (marker < 0) {
            return null;
        }
        String enchantment = entry.substring(0, marker);
        return ENCHANTMENTS_WITHOUT_A_CHEST.contains(enchantment) ? enchantment : null;
    }

    /** The two vault pools the rare vault receives together. */
    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> both = new TreeSet<>(first);
        both.addAll(second);
        return Set.copyOf(both);
    }

    /**
     * Counts the pools the mod offers, keeping the two editor paths apart, and keeps them so
     * their contents can be rolled afterwards.
     */
    private static final class PoolRecorder implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();
        private int builders;
        private int built;

        @Override
        public void addPool(LootPool.Builder pool) {
            this.builders++;
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.built++;
            this.pools.add(pool);
        }

        int total() {
            return this.builders + this.built;
        }
    }

    /**
     * The flag of this file's {@code simplebuilding:config} condition, or {@code null} if it has
     * none. Fabric spells the condition id {@code condition}, NeoForge spells it {@code type};
     * both keys sit in the same file and the foreign one is ignored by each loader.
     */
    private static String configFlag(JsonObject json, String arrayKey, String typeKey) {
        JsonElement array = json.get(arrayKey);
        if (array == null || !array.isJsonArray()) {
            return null;
        }
        for (JsonElement child : array.getAsJsonArray()) {
            String flag = singleConfigFlag(child, typeKey);
            if (flag != null) {
                return flag;
            }
        }
        return null;
    }

    /**
     * The flag of one condition object if it is a {@code simplebuilding:config} condition, else
     * {@code null}. Forge 65 takes exactly one such object under {@code "forge:condition"}.
     */
    private static String singleConfigFlag(JsonElement element, String typeKey) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject condition = element.getAsJsonObject();
        JsonElement type = condition.get(typeKey);
        if (type == null || !type.isJsonPrimitive() || !CONFIG_CONDITION.equals(type.getAsString())) {
            return null;
        }
        JsonElement flag = condition.get("flag");
        return flag != null && flag.isJsonPrimitive() ? flag.getAsString() : null;
    }

    /** Everything wrong with a flag name the trade files use, as messages; empty means it is fine. */
    private static List<String> worldGenFlagProblems(String flag) {
        List<String> problems = new ArrayList<>();
        Field field;
        try {
            field = SimplebuildingConfig.WorldGen.class.getField(flag);
        } catch (NoSuchFieldException e) {
            problems.add("the trades are gated on \"" + flag + "\", but SimplebuildingConfig.WorldGen has no "
                    + "such field - both loaders answer a flag they cannot resolve with \"true\", so that "
                    + "switch is dead and only a log line says so");
            return problems;
        }
        if (field.getType() != boolean.class) {
            problems.add("worldGen." + flag + " is a " + field.getType().getSimpleName()
                    + ", but the datapack conditions read it as a boolean");
            return problems;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            problems.add("worldGen." + flag + " became static, so it is no longer part of the saved config");
        }
        try {
            field.getBoolean(Simplebuilding.getConfig().worldGen);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            problems.add("worldGen." + flag + " cannot be read off the live config (" + e + ")");
        }
        return problems;
    }

    /**
     * Feeds every flag the trade files use to the running loader's own {@code simplebuilding:config}
     * condition, so the string in the json is followed all the way into the config field.
     *
     * <p>Everything else about the flags is a comparison against literals spelled out in this file.
     * That leaves the link that does the actual switching unobserved: both conditions resolve the
     * flag in a hard coded {@code switch} and answer an unknown string with {@code true}, so the
     * label of a {@code case} can drift away from the json without anything failing. Driving the
     * condition object is the only way to see that from a test.
     *
     * <p>The classes are looked up by name, and the one belonging to the other loader is simply
     * absent - that is expected and skipped. A run in which <em>neither</em> is present is not: it
     * would mean this whole check quietly did nothing, so it is reported as a problem of its own.
     */
    private static List<String> conditionSwitchProblems(Set<String> flags) {
        List<String> problems = new ArrayList<>();
        int driven = 0;

        for (String className : CONDITION_CLASSES) {
            Class<?> conditionClass;
            try {
                conditionClass = Class.forName(className);
            } catch (ClassNotFoundException | LinkageError e) {
                continue; // the other loader's condition; it is driven when the suite runs there
            }

            Constructor<?> constructor;
            Method test;
            try {
                constructor = conditionClass.getConstructor(String.class);
                test = singleArgumentTest(conditionClass);
            } catch (NoSuchMethodException | RuntimeException e) {
                problems.add(className + " no longer offers a (String flag) constructor plus a one or "
                        + "two argument test(...) method, so the switch behind the trade flags cannot be "
                        + "driven from here at all (" + e + ")");
                continue;
            }
            driven++;

            for (String flag : flags) {
                problems.addAll(switchProblems(className, constructor, test, flag, flags));
            }
        }

        if (driven == 0) {
            problems.add("none of " + CONDITION_CLASSES + " is on the classpath, so the switch that "
                    + "turns a flag into a config read was never driven; a trade could be gated on a "
                    + "string no case matches and every other assertion here would still hold");
        }
        return problems;
    }

    /**
     * Whether {@code flag} switches its own option and only its own, asked of the real condition.
     *
     * <p>Both directions matter. Its own option off has to shut the trade out, or the {@code case}
     * that resolves this string is gone and the toggle is dead; another flag's option off has to
     * leave it alone, or the two cases have ended up reading one field and one toggle silences the
     * other's trades as well.
     */
    private static List<String> switchProblems(String className, Constructor<?> constructor, Method test,
                                               String flag, Set<String> flags) {
        List<String> problems = new ArrayList<>();
        Object condition;
        try {
            condition = constructor.newInstance(flag);
        } catch (ReflectiveOperationException | RuntimeException e) {
            problems.add(className + " could not be built for the flag \"" + flag + "\" (" + e + ")");
            return problems;
        }

        for (String off : flags) {
            Boolean answer = answerWithOneFlagOff(problems, flags, off, test, condition);
            if (answer == null) {
                problems.add(className + " gave no boolean answer for the flag \"" + flag
                        + "\" while worldGen." + off + " was switched off");
            } else if (off.equals(flag) && answer) {
                problems.add(className + " lets a trade gated on \"" + flag + "\" load while "
                        + "worldGen." + flag + " is switched off - no case of its switch resolves "
                        + "that string any more, so the player's toggle does nothing and only a "
                        + "log line says so");
            } else if (!off.equals(flag) && !answer) {
                problems.add(className + " refuses a trade gated on \"" + flag + "\" although only "
                        + "worldGen." + off + " is switched off - the two flags no longer read "
                        + "separate options, so one toggle also turns off the other's trades");
            }
        }
        return problems;
    }

    /**
     * The condition's answer while exactly one of the trade flags is off and every other one is on.
     *
     * <p>All of them are written rather than just the one being switched off, so the answer says
     * something about the switch instead of about whatever the config file happened to hold when
     * the suite started. They are put back in a {@code finally}, and the caller collects messages
     * instead of throwing, precisely so that no failure can leave a trade switch flipped for the
     * rest of the batch.
     */
    private static Boolean answerWithOneFlagOff(List<String> problems, Set<String> flags, String off,
                                                Method test, Object condition) {
        SimplebuildingConfig.WorldGen worldGen = Simplebuilding.getConfig().worldGen;
        Map<Field, Boolean> restore = new LinkedHashMap<>();
        try {
            for (String flag : flags) {
                Field field;
                try {
                    field = SimplebuildingConfig.WorldGen.class.getField(flag);
                    restore.put(field, field.getBoolean(worldGen));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return null; // worldGenFlagProblems already reports a flag without such a field
                }
                try {
                    field.setBoolean(worldGen, !flag.equals(off));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add("worldGen." + flag + " could not be written (" + e + ")");
                    return null;
                }
            }
            return callTest(test, condition);
        } finally {
            for (Map.Entry<Field, Boolean> entry : restore.entrySet()) {
                try {
                    entry.getKey().setBoolean(worldGen, entry.getValue());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add("a trade switch could not be restored (" + e + ")");
                }
            }
        }
    }

    /**
     * The condition's own {@code test} method. Declared methods only, so the loader interface's
     * default overloads cannot be picked up by mistake. Fabric and NeoForge hand one argument (the
     * lookup / the condition context), Forge 65 two ({@code IContext} plus the {@code DynamicOps}).
     */
    private static Method singleArgumentTest(Class<?> conditionClass) throws NoSuchMethodException {
        for (Method method : conditionClass.getDeclaredMethods()) {
            if ("test".equals(method.getName())
                    && (method.getParameterCount() == 1 || method.getParameterCount() == 2)
                    && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers())
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                return method;
            }
        }
        throw new NoSuchMethodException("no one or two argument test(...) declared on " + conditionClass.getName());
    }

    /**
     * Calls the condition with {@code null} arguments. All conditions answer from the config
     * alone and never touch what they are handed, which is what makes this callable without
     * a datapack load in flight; {@code null} instead of a stand in is deliberate, so a condition
     * that started reading it fails loudly here rather than silently answering from nothing.
     */
    private static Boolean callTest(Method test, Object condition) {
        try {
            Object answer = test.invoke(condition, new Object[test.getParameterCount()]);
            return answer instanceof Boolean value ? value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * The base name of the file the config is saved under, taken from Cloth Config's
     * {@code @Config} annotation, or {@code null} if the annotation is gone or unreadable. Looked
     * up by name so this class needs no cloth-config import and stays loader neutral.
     */
    private static String declaredConfigFileName() {
        for (Annotation annotation : SimplebuildingConfig.class.getAnnotations()) {
            if (!CONFIG_ANNOTATION.equals(annotation.annotationType().getName())) {
                continue;
            }
            try {
                Object name = annotation.annotationType().getMethod("name").invoke(annotation);
                return name instanceof String text ? text : null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Describes every option declared by {@code owner} as {@code group.name type=default}, reading
     * the values off {@code instance}. Nested option groups - the classes declared inside
     * {@link SimplebuildingConfig} itself - are described by their type instead of a value, statics
     * by the fact that they are static: the serializer leaves those out of the saved file, so they
     * carry no persisted default to pin.
     */
    private static void collectOptions(Set<String> into, List<String> problems, String group,
                                       Class<?> owner, Object instance) {
        if (instance == null) {
            problems.add(group + ": the option group is null");
            return;
        }
        for (Field field : owner.getFields()) {
            if (field.getDeclaringClass() != owner || field.isSynthetic()) {
                continue;
            }
            String name = group + "." + field.getName();
            String type = field.getType().getSimpleName();
            if (Modifier.isStatic(field.getModifiers())) {
                into.add(name + " " + type + " runtime-only(static)");
            } else if (field.getType().getEnclosingClass() == SimplebuildingConfig.class
                    || field.getType() == TweaksConfig.class
                    || field.getType().getEnclosingClass() == TweaksConfig.class
                    || field.getType() == com.simplebuilding.config.ServerTuningConfig.class
                    || field.getType().getEnclosingClass() == com.simplebuilding.config.ServerTuningConfig.class) {
                into.add(name + " group:" + type);
            } else {
                try {
                    into.add(name + " " + type + "=" + field.get(instance));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    problems.add(name + " could not be read (" + e + ")");
                }
            }
        }
    }
}
