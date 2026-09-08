package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.util.ISpaceKeyTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The five features that only ever had their <em>declaration</em> looked at: what the End ores
 * hand out when they are mined, what the air jump enchantment costs, what enderite armour does to
 * void damage and to a fall, which vanilla chests the mod hands a pool to, and the two item frame
 * branches nobody had driven yet.
 *
 * <p>Every one of these is a promise that breaks quietly. A loot table entry can be swapped for a
 * neighbouring item and the block still drops <em>something</em>; an enchantment can lose half its
 * levels and still exist; a void protection interval can collapse to a single tick and the player
 * simply dies a little faster. The tests here run the real thing - vanilla's loot roll, vanilla's
 * damage pipeline, vanilla's own {@code Player#tick} - and compare against numbers that are read
 * off the mod's source, so a changed number has to be changed here as well, on purpose.
 *
 * <h2>Which mock player, and why</h2>
 * <p>Everything here uses {@code makeMockServerPlayerInLevel()}: none of the five features has a
 * survival-only branch, and three of them need a real {@code connection} - the void tests need
 * {@link ServerboundPlayerLoadedPacket} to stop being invulnerable, the fall test needs
 * {@code connection.tick()} to reach {@code Player#tick}, and the frame test needs a connection to
 * survive the mixin's {@code sendOverlayMessage} calls.
 *
 * <h2>Known defects</h2>
 * <ul>
 *   <li><b>The air jump reads every slot on the client and only the boots on the server.</b>
 *       {@code DoubleJumpController.getDoubleJumpLevel} walks {@code EquipmentSlot.values()} and
 *       takes the highest level it finds anywhere, while {@code ModMessageHandlers.handleDoubleJump}
 *       only ever looks at {@code EquipmentSlot.FEET}. A player carrying the enchantment in any
 *       other slot therefore gets the jump impulse and sends the payload, but the server refuses to
 *       clear the fall distance - the jump happens and the landing still hurts. Only reachable
 *       through commands or creative (the enchantment's supported items are
 *       {@code #minecraft:foot_armor}), so it is a latent inconsistency rather than an exploit.
 *       {@link #theAirJumpLevelReadsEverySlotWhileTheServerOnlyCreditsTheBoots} pins both halves as
 *       they are and will fail the day either side is aligned with the other.</li>
 *   <li><b>The void damage interval has an unreachable default.</b>
 *       {@code LivingEntityMixin#modifyVoidDamage} initialises {@code damageInterval} to 10 and then
 *       overwrites it for one, two, three and four pieces - and the whole block only runs when at
 *       least one piece is worn, so 10 can never be the value that is used. Not asserted (a test on
 *       dead code would only cement it); what is asserted is the four reachable intervals and the
 *       fact that no armour means no protection at all.</li>
 *   <li><b>{@code /simplebuilding} is unusable from the server console.</b> The {@code requires}
 *       gate resolves the operator through {@code getPlayerOrException()}, which throws for a
 *       non-player source and is caught into {@code false}. Deliberately <em>not</em> pinned here:
 *       writing "the console is refused" into a test would cement a defect. The permission gate and
 *       the argument range of that command are covered by
 *       {@code TrimWiringTests#theTrimMultiplierCommandGuardsItsRangeAndItsPermission}.</li>
 * </ul>
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>The air jump itself.</b> {@code DoubleJumpController.tick(Minecraft)} reads the jump key
 *       off {@code client.options} and writes the player's velocity on the client; there is no
 *       client in a gametest. Only {@code getDoubleJumpLevel(Player)}, the one piece of that class
 *       that takes a plain {@link Player}, is driven here.</li>
 *   <li><b>The HUD cooldown bar</b> ({@code DoubleJumpHudOverlay}) and the cooldown state machine
 *       behind it: both live in {@code DoubleJumpController.tick} and are client only.</li>
 *   <li><b>Whether an ore is reachable in a real End island.</b> Vein shape and spawn rates are the
 *       subject of {@code OreGenAndItemFrameTests}; this class only deals with what a mined ore
 *       hands over.</li>
 *   <li><b>The wrong-tool case ending in no drop.</b> The gate lives in
 *       {@code ServerPlayerGameMode#destroyBlock}, not in the loot table, so it cannot be reached
 *       through a creative mock player. Vanilla's own decision function,
 *       {@code Player#hasCorrectToolForDrops}, is driven instead - see
 *       {@link #endOreBlocksKeepTheirStrengthLightAndDiamondToolRequirement}.</li>
 *   <li><b>The slow fall's client prediction.</b> The mixin branch is explicitly server side
 *       ({@code !level().isClientSide()}); the matching client feel is not testable here.</li>
 * </ul>
 */
public final class WorldAndPlayerTests {

    private WorldAndPlayerTests() {
    }

    // =====================================================================================
    // CONSTANTS
    // =====================================================================================

    /** Where the loot rolls pretend the block stood; any position inside the room will do. */
    private static final BlockPos LOOT_ORIGIN = new BlockPos(3, 2, 3);

    /** The two positions the experience test breaks its ores at; far enough apart not to mix. */
    private static final BlockPos ASTRALIT_POS = new BlockPos(2, 3, 2);
    private static final BlockPos NIHILITH_POS = new BlockPos(5, 3, 5);

    /**
     * How often each loot table is rolled per tool. Large enough that the Fortune III case cannot
     * miss a multiplied drop by chance: one roll leaves the count at 1 with probability 2/5, so
     * this many rolls miss with probability 2e-13 - and the seeds below are fixed anyway.
     */
    private static final int LOOT_ROLLS = 32;

    /** Base seed for the loot rolls, so a failure is the same failure on every machine. */
    private static final long LOOT_SEED = 20260904L;

    /** Highest count {@code apply_bonus/ore_drops} can produce at Fortune III: {@code 3 + 1}. */
    private static final int FORTUNE_MAX_COUNT = 4;

    /** Fortune level used for the multiplied drops. */
    private static final int FORTUNE_LEVEL = 3;

    /**
     * How often the Fortune III table is rolled to pin the <em>shape</em> of the bonus, not just
     * its ceiling. The ceiling alone cannot tell {@code apply_bonus/ore_drops} from
     * {@code uniform_bonus_count} with a bonus multiplier of one: at Fortune III the first gives
     * {@code count * (max(0, nextInt(5) - 1) + 1)} and the second {@code count + nextInt(4)}, so
     * both cover exactly 1..{@value #FORTUNE_MAX_COUNT}. They part company in how often the drop
     * stays single - the ore formula folds {@code nextInt(5) == 0} and {@code == 1} onto the same
     * single item, two rolls in five, where the uniform bonus leaves one in four. That difference
     * is only visible in a sample, so it is measured over one wide enough that the two shares
     * cannot be mistaken for each other: at this many rolls the ore formula sits about seven
     * standard deviations inside the band below and the uniform one about seven outside it. The
     * seeds are fixed, so the number is the same on every machine, and all the rolls happen inside
     * one tick - a wide sample costs wall time, not tick budget.
     */
    private static final int FORTUNE_SHAPE_ROLLS = 2000;

    /**
     * The band the single drops have to fall into, 32 % to 48 % of {@link #FORTUNE_SHAPE_ROLLS}.
     * It brackets the 40 % of {@code ore_drops} on both sides and excludes the 25 % of a uniform
     * bonus of one as well as any formula that hands out singles more often than the ore one does.
     */
    private static final int FORTUNE_SINGLE_DROPS_MIN = FORTUNE_SHAPE_ROLLS * 32 / 100;

    /** Upper end of the band described on {@link #FORTUNE_SINGLE_DROPS_MIN}. */
    private static final int FORTUNE_SINGLE_DROPS_MAX = FORTUNE_SHAPE_ROLLS * 48 / 100;

    /**
     * How often each ore is broken while measuring its experience. The two ends of the range have
     * to be <em>seen</em> for the assertions below to bite, and each break is a uniform draw from
     * five values, so this is a question of probability: the chance that eighty draws never show a
     * seven is {@code 0.8^80 = 1.8e-8}, and the chance they never produce a sum of three is
     * {@code 0.795^80 = 1.1e-8} (three is drawn with probability 1/5, and a drawn six collapses
     * onto three whenever its two orbs merge, worth another 1/200). Over both ores and both ends
     * of their ranges that is a false alarm once in roughly twenty million runs, which is the price
     * of pinning the ends at all. Everything here happens inside a single tick, so a larger sample
     * count costs wall time, not tick budget.
     */
    private static final int EXPERIENCE_SAMPLES = 80;

    /** {@code UniformInt.of(3, 7)} in {@code ModBlocks} - the experience both ores drop. */
    private static final int EXPERIENCE_MIN = 3;
    private static final int EXPERIENCE_MAX = 7;

    /** Frames face south, so their supporting block sits one step north of them. */
    private static final Direction FRAME_FACING = Direction.SOUTH;

    private static final BlockPos FRAME_POS = new BlockPos(2, 3, 3);

    /** NBT key {@code ItemFrameEntityMixin} saves the lock under. */
    private static final String LOCK_TAG = "SimpleBuildingLocked";

    /** Custom-data key {@code ItemFrameEntityMixin} writes the magnet filter into. */
    private static final String MAGNET_FILTER_TAG = "MagnetFilter";

    /** The numbers {@code ModEnchantments} declares for {@code simplebuilding:double_jump}. */
    private static final int AIR_JUMP_MAX_LEVEL = 2;
    private static final int AIR_JUMP_WEIGHT = 2;
    private static final int AIR_JUMP_ANVIL_COST = 4;
    private static final int AIR_JUMP_MIN_COST_BASE = 20;
    private static final int AIR_JUMP_MAX_COST_BASE = 70;
    private static final int AIR_JUMP_COST_PER_LEVEL = 15;

    /**
     * Fall distance the player carries into an air jump, and the value the server has to leave
     * behind when it refuses one.
     *
     * <p>A {@code double}, and that is not cosmetic. {@code Entity#fallDistance} is declared
     * {@code public double} in this version, while {@code GameTestHelper#assertValueEqual} is
     * {@code <N> void assertValueEqual(N, N, String)} and compares its two arguments with
     * {@code Object#equals} - so a boxed {@code Double} measured off the field is compared against
     * a boxed {@code Float} literal and is never equal to it, whatever the mod does. That helper is
     * unusable on this field; the assertions below use {@code ==} instead, which is exact here
     * because both 0.0 and 7.5 are representable without rounding. The sibling suites do the same,
     * see {@code NetworkHandlerTests} and {@code ConsumptionAndDurabilityTests}.
     */
    private static final double FALL_DISTANCE_BEFORE_JUMP = 7.5;

    /**
     * Ticks the void protection is probed on. Chosen so that the five intervals the mixin can
     * possibly produce - the four reachable ones plus the dead default of 10 - each leave a
     * different set of hits behind: 20 hits {20,40,60,100,120}, 40 hits {40,120}, 60 hits
     * {60,120}, 100 hits {100} and 10 would hit {10,20,40,60,100,120}. "No protection" hits
     * everything and "always protected" hits nothing, so both of those are distinguishable too.
     */
    private static final int[] VOID_PROBE_TICKS =
            {1, 9, 10, 11, 19, 20, 21, 39, 40, 41, 59, 60, 61, 99, 100, 101, 120};

    /** Damage per void probe. Small enough that the player survives all of them. */
    private static final float VOID_HIT_DAMAGE = 1.0F;

    /** A tick that is a multiple of none of the intervals, used for the two counter samples. */
    private static final int VOID_UNPROTECTED_TICK = 101;

    /** Damage for the counter samples; big enough to get through armour absorption. */
    private static final float UNRELATED_HIT_DAMAGE = 15.0F;

    /** The four armour slots in the order {@code LivingEntityMixin} counts them. */
    private static final EquipmentSlot[] ARMOUR_SLOTS =
            {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    /** One enderite piece per armour slot, in the same order as {@link #ARMOUR_SLOTS}. */
    private static final Item[] ENDERITE_PIECES = {
            ModItems.ENDERITE_BOOTS, ModItems.ENDERITE_LEGGINGS,
            ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_HELMET};

    /** Void damage interval per worn enderite piece, index 0 = one piece. */
    private static final int[] VOID_INTERVALS = {20, 40, 60, 100};

    /** Where the falling player hangs; five blocks of air below it inside the room. */
    private static final Vec3 FALLING_POS = new Vec3(3.5, 5.0, 3.5);

    /** Block the grounded case stands on, and the spot on top of it. */
    private static final BlockPos GROUND_BLOCK = new BlockPos(5, 2, 5);
    private static final Vec3 GROUND_POS = new Vec3(5.5, 3.0, 5.5);

    /** Downward speed the mixin asks for; comfortably past its {@code < -0.1} threshold. */
    private static final double FALL_SPEED = -0.5;

    /**
     * The speed a player has the moment they walk off a block: none. It is the probe that pins the
     * mixin's {@code < -0.1} threshold from above, and it works because the tick that follows is
     * pure gravity.
     *
     * <p>The three speeds the slow fall test uses turn into these values by the time the mixin
     * reads them at the tail of {@code Player#tick}, all of them one application of
     * {@code LivingEntity#travelInAir}: it subtracts {@code getEffectiveGravity()}, which is the
     * {@code minecraft:gravity} attribute's default of 0.08 for a player without slow falling, and
     * then multiplies by the vertical air drag, {@code computeModifiedFriction(0.98, 1.0) = 0.98}.
     * So {@code -0.5} arrives as {@code -0.5684}, {@code +0.2} as {@code +0.1176} and this one as
     * {@code -0.0784}. The first has to be granted, the other two refused, which nails the
     * threshold into {@code (-0.5684, -0.0784]} - and {@code -0.1} sits inside it while "any
     * downward movement at all" ({@code < 0.0}) does not.
     */
    private static final double STEP_OFF_SPEED = 0.0;

    /** Duration {@code PlayerEntityMixin} hands the slow falling effect, in ticks. */
    private static final int SLOW_FALL_DURATION = 2;

    /**
     * Every vanilla loot table {@code ModLootTableModifications} touches, with the exact number of
     * pools it has to receive. The end city is a three because it takes both pre built pools plus
     * the builder one, and the trial chamber rare vault is a two because it is the single key that
     * both vault branches match. Everything else gets exactly one pool.
     */
    private static final Map<ResourceKey<LootTable>, Integer> POOLS_PER_TABLE = Map.ofEntries(
            Map.entry(BuiltInLootTables.STRONGHOLD_LIBRARY, 1),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, 3),
            Map.entry(BuiltInLootTables.ANCIENT_CITY, 1),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, 1),
            Map.entry(BuiltInLootTables.BASTION_OTHER, 1),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, 1),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, 1),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, 1),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, 1),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, 1),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, 1),
            Map.entry(BuiltInLootTables.IGLOO_CHEST, 1),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, 1),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, 1),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 2),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 1));

    /**
     * Control group: three vanilla tables that sit right next to a modified one and must stay
     * empty. They are the ones a widened {@code if} would catch first.
     */
    private static final List<ResourceKey<LootTable>> NEIGHBOURING_TABLES = List.of(
            BuiltInLootTables.BASTION_BRIDGE,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE);

    /** Registry id of the air jump enchantment, as it is spelt inside a loot table json. */
    private static final String AIR_JUMP_ID = SimpleBuildingGameTests.MOD_ID + ":double_jump";

    /** Loot entry the air jump books are: the vanilla enchanted book. */
    private static final String ENCHANTED_BOOK_ID = "minecraft:enchanted_book";

    // =====================================================================================
    // (a) THE TWO END ORES
    // =====================================================================================

    /**
     * What the two End ores actually hand over, pulled out of the shipped loot tables with three
     * different tools: a bare hand gives one dust (or shard), a silk touch pickaxe gives the ore
     * block itself, and Fortune III multiplies the dust without ever producing the block.
     *
     * <p>The table is rolled rather than read, because rolling is what a broken block does: it
     * covers the {@code alternatives} entry, the {@code match_tool} condition on the silk touch
     * branch and the {@code apply_bonus/ore_drops} function in one step. The seeds are fixed, so
     * the answer is the same on every machine, and the counts are checked against the formula's
     * own ceiling - {@code ore_drops} at level 3 multiplies by at most {@code 3 + 1}, so a drop of
     * five would mean the formula or the level changed.
     *
     * <p>The ceiling is not enough on its own, because {@code uniform_bonus_count} with a bonus
     * multiplier of one shares it exactly. Which of the two formulas is in the table is therefore
     * decided by how often the drop stays single over {@link #FORTUNE_SHAPE_ROLLS} rolls - see
     * that constant for the two shares and for why the sample is as wide as it is.
     *
     * <p>Note what this does <em>not</em> claim: the loot table carries no "correct tool" condition
     * at all, so these rolls succeed with any tool. That gate lives in
     * {@code ServerPlayerGameMode#destroyBlock} and is covered by the next test.
     *
     * <p>Breaks if a drop is swapped for a neighbouring item (astralit dust for nihilith shard is
     * exactly the kind of copy-paste this catches), if the silk touch branch is dropped so the ore
     * block becomes unobtainable, if the Fortune function is lost so the ore stops scaling, or if
     * the fortune formula is swapped for {@code uniform_bonus_count}, which reaches the same
     * ceiling but hands out a different mix of counts on the way there.
     */
    public static void endOresDropTheirDustAndFollowFortuneWhileSilkTouchKeepsTheOre(GameTestHelper helper) {
        assertOreDrops(helper, ModBlocks.ASTRALIT_ORE, ModItems.ASTRALIT_DUST, ModItems.ASTRALIT_ORE_ITEM);
        assertOreDrops(helper, ModBlocks.NIHILITH_ORE, ModItems.NIHILITH_SHARD, ModItems.NIHILITH_ORE_ITEM);
        TestCleanup.succeed(helper);
    }

    /**
     * The block properties both ores were given by hand in {@code ModBlocks}, and the tool they
     * demand: astralit is hardness 20 and glows at light level 5, nihilith is hardness 25 and does
     * not glow, both have blast resistance 1200, and both need a diamond pickaxe.
     *
     * <p>The tool half is driven through vanilla's own decision, {@code Player#hasCorrectToolForDrops},
     * which is the exact call {@code ServerPlayerGameMode#destroyBlock} makes before it hands the
     * block to {@code Block#dropResources}. That covers both halves of the requirement at once: the
     * {@code requiresCorrectToolForDrops()} flag on the block (without it every tool would be
     * correct) and the {@code #minecraft:needs_diamond_tool} tag entry (without it iron would be
     * enough). The tag membership itself is asserted as well, because that is what a datagen run
     * that was never re-executed loses first.
     *
     * <p>The light level is the one number here that is not a strength: it is what makes an
     * astralit vein visible in a dark End island, and it lives in a {@code lightLevel(state -> 5)}
     * lambda that is easy to lose in a refactor.
     *
     * <p>Breaks if a strength value is edited, if the astralit glow is dropped, if either ore
     * leaves {@code #minecraft:mineable/pickaxe} (it would become unbreakable by hand-tool rules)
     * or {@code #minecraft:needs_diamond_tool} (an iron pickaxe would suddenly be enough), or if
     * {@code requiresCorrectToolForDrops()} is dropped from the properties.
     */
    public static void endOreBlocksKeepTheirStrengthLightAndDiamondToolRequirement(GameTestHelper helper) {
        assertOreBlock(helper, ModBlocks.ASTRALIT_ORE, ASTRALIT_POS, 20.0F, 5);
        assertOreBlock(helper, ModBlocks.NIHILITH_ORE, NIHILITH_POS, 25.0F, 0);

        // The two ores are deliberately not equally hard; a single shared constant would pass every
        // assertion above and still be a change nobody decided on.
        helper.assertTrue(
                ModBlocks.ASTRALIT_ORE.defaultBlockState().getDestroySpeed(
                        helper.getLevel(), helper.absolutePos(ASTRALIT_POS))
                        < ModBlocks.NIHILITH_ORE.defaultBlockState().getDestroySpeed(
                        helper.getLevel(), helper.absolutePos(NIHILITH_POS)),
                "astralit ore is no longer the softer of the two End ores");

        TestCleanup.succeed(helper);
    }

    /**
     * Both ores are {@code DropExperienceBlock}s with {@code UniformInt.of(3, 7)}, so breaking one
     * has to award between three and seven experience - and not always the same amount.
     *
     * <p>The block is broken through {@code ServerLevel#destroyBlock(pos, true)} because
     * {@code GameTestHelper#destroyBlock} deliberately drops nothing, and the orbs are counted
     * straight afterwards in the same tick: that is safe because vanilla's own
     * {@code ExperienceOrb.award} relies on it - its merge lookup finds orbs it added itself one
     * loop iteration earlier.
     *
     * <p>Every sample has to land inside the declared range, and on top of that the two ends have
     * to be reached: the smallest sum seen over {@link #EXPERIENCE_SAMPLES} breaks must be exactly
     * three and the largest exactly seven. Without those two the test would be almost worthless -
     * "inside 3..7" plus "at least two different sums" is satisfied by {@code UniformInt.of(4, 7)},
     * {@code of(3, 5)} and {@code of(5, 7)} on every run, so the ore could hand out a permanently
     * different amount and stay green.
     *
     * <p>Pinning the ends is sound despite the one thing that makes the measurement lossy.
     * {@code ExperienceOrb.awardWithDirection} splits the amount into orbs of the denominations
     * {@code getExperienceValue} produces, and two orbs of equal value that land in the same block
     * merge into one whose private {@code count} nothing here can read - {@code tryMergeToExisting}
     * draws {@code random.nextInt(40)} and merges when {@code (orb.getId() - id) % 40 == 0}, so
     * that happens one time in forty. Summing {@code getValue()} can therefore under-report, never
     * over-report, which is what keeps the upper end honest. And the ends themselves cannot be
     * faked: three is a single orb of value three, seven is a single orb of value seven, and four
     * splits into 3+1, none of which has an equal pair to merge with. Only five (3+1+1) and six
     * (3+3) can lose to a merge, and they are not needed for either end - a merged six even lands
     * on three, which can only help the lower assertion.
     *
     * <p>Breaks if the experience range moves in either direction, if it narrows at either end, if
     * it collapses to a constant, or if either ore stops being a {@code DropExperienceBlock}
     * altogether - a plain {@code Block} would award nothing and fail on the very first sample.
     */
    public static void breakingAnEndOreAwardsThreeToSevenExperience(GameTestHelper helper) {
        assertOreExperience(helper, ModBlocks.ASTRALIT_ORE, ASTRALIT_POS);
        assertOreExperience(helper, ModBlocks.NIHILITH_ORE, NIHILITH_POS);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (b) ITEM FRAME: THE TWO BRANCHES THAT WERE NEVER DRIVEN
    // =====================================================================================

    /**
     * Two things about the order of the branches in {@code ItemFrameEntityMixin#interact}, both of
     * which only show up when a frame is used the way a player actually uses one.
     *
     * <p>First the fall-through: sneaking is not a mod gesture by itself. With something that is
     * neither a glass pane nor shears in hand, on a frame that is filled, visible and unlocked, all
     * four sneak branches miss and the click has to reach vanilla - which turns the framed item.
     * If any of those four conditions is ever loosened, sneaking near a frame starts doing
     * something the player did not ask for, and this is where that shows. {@code
     * OreGenAndItemFrameTests} drives the same fall-through from the other side, on an
     * <em>empty</em> frame where vanilla puts the held item in; the two cases fail for different
     * reasons, because an empty frame is what turns off the item conditions of the lock and shear
     * branches, while a filled one is what turns off the "not the right item" ones.
     *
     * <p>Then the priority: the magnet branch sits <em>before</em> the {@code FAIL} that a locked
     * frame answers every other click with, so a locked frame can still be used as a filter
     * template - that is the whole point of locking one up as a display. Sneaking with the same
     * magnet must take the unlock branch instead, because that one comes first.
     *
     * <p>The plain, unenchanted magnet on the locked frame is the assertion that keeps the rest
     * honest: it has to be refused with {@code FAIL}, which proves the lock really is in the way
     * and that the enchanted magnet got past it on its own merits.
     *
     * <p>Registered with an unrotated structure - a hanging entity needs its supporting block on a
     * fixed side, and {@link #FRAME_FACING} is an absolute direction on purpose.
     *
     * <p>Breaks if the magnet branch moves behind the lock check (a locked frame could no longer be
     * copied from), if the unlock branch moves behind the magnet branch (a locked frame could never
     * be opened while holding a magnet), or if any sneak branch loses a condition and starts
     * swallowing ordinary sneak clicks.
     */
    public static void lockedFramesStillAnswerTheMagnetWhileOtherSneakClicksFallThrough(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 5.5));
        ItemFrame frame = frameWithItem(helper, FRAME_POS);

        // --- sneaking with an unrelated item is not a mod gesture: vanilla turns the item ---
        int rotation = frame.getRotation();
        InteractionResult sneakStone = interact(frame, player, new ItemStack(Blocks.STONE), true);
        helper.assertTrue(frame.getRotation() != rotation,
                "sneaking with a stone on a plain frame did not reach vanilla; one of the four sneak "
                        + "branches has lost a condition and now swallows ordinary clicks");
        helper.assertTrue(sneakStone == InteractionResult.SUCCESS,
                "vanilla answered the fallen-through sneak click with " + sneakStone);

        // --- and the same with an empty hand, which is the gesture used to unlock ---
        rotation = frame.getRotation();
        interact(frame, player, ItemStack.EMPTY, true);
        helper.assertTrue(frame.getRotation() != rotation,
                "sneaking with an empty hand on an unlocked frame did something other than turn the "
                        + "item; the unlock branch now fires on frames that were never locked");

        // --- lock it, so everything below is about a locked frame ---
        interact(frame, player, new ItemStack(Items.GLASS_PANE), true);
        helper.assertTrue(isLocked(helper, frame),
                "the frame did not lock, so nothing below would prove anything about the lock");

        // --- a plain magnet is refused: the lock really is in the way ---
        ItemStack plainMagnet = new ItemStack(ModItems.MAGNET);
        rotation = frame.getRotation();
        InteractionResult refused = interact(frame, player, plainMagnet, false);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "a locked frame accepted a plain magnet, result was " + refused);
        Assertions.valueEqual(helper, filterOf(plainMagnet), "",
                "an unenchanted magnet wrote a filter, so the Constructor's Touch check is gone");
        Assertions.valueEqual(helper, frame.getRotation(), rotation, "rotation of the locked frame");

        // --- the enchanted magnet gets through anyway: its branch is checked before the lock ---
        ItemStack enchantedMagnet = magnetWithConstructorsTouch(helper);
        InteractionResult accepted = interact(frame, player, enchantedMagnet, false);
        helper.assertTrue(accepted == InteractionResult.SUCCESS,
                "an enchanted magnet on a locked frame was answered with " + accepted
                        + "; the magnet branch has moved behind the lock check and a locked display "
                        + "frame can no longer be used as a filter template");
        Assertions.valueEqual(helper, filterOf(enchantedMagnet), "minecraft:diamond",
                "magnet filter taken from the locked frame");
        helper.assertTrue(isLocked(helper, frame), "reading the filter unlocked the frame");

        // --- sneaking with that very magnet unlocks instead, because branch 2 comes first ---
        ItemStack sneakingMagnet = magnetWithConstructorsTouch(helper);
        InteractionResult unlocking = interact(frame, player, sneakingMagnet, true);
        helper.assertTrue(unlocking == InteractionResult.SUCCESS,
                "sneaking with a magnet on a locked frame was answered with " + unlocking);
        helper.assertFalse(isLocked(helper, frame),
                "sneaking with a magnet did not unlock the frame; the magnet branch now runs before "
                        + "the unlock branch and a locked frame can only be opened bare handed");
        Assertions.valueEqual(helper, filterOf(sneakingMagnet), "",
                "the sneaking magnet took a filter as well, so both branches ran for one click");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (c) DOUBLE JUMP
    // =====================================================================================

    /**
     * The numbers behind {@code simplebuilding:double_jump}: two levels, weight 2 (rare), an anvil
     * cost of 4, enchanting costs of {@code 20 + 15 per level} to {@code 70 + 15 per level}, and
     * the boots and nothing else.
     *
     * <p>These are read out of the datapack registry, not out of {@code ModEnchantments}, so this
     * also catches the case the whole suite exists for: a generated enchantment json that was never
     * regenerated after the bootstrap changed. Both cost curves are checked at both ends, which is
     * what separates the base value from the per-level step - a single level would let
     * {@code dynamicCost(20, 15)} and {@code constantCost(20)} pass as the same thing.
     *
     * <p>The slot is asserted twice on purpose: once as the declared {@code EquipmentSlotGroup}
     * list, and once through {@code Enchantment#matchingSlot}, which is the call vanilla itself
     * makes when it decides whether an enchantment in a slot counts. The first catches a rewritten
     * definition, the second catches a slot group that was renamed to something that no longer
     * covers the feet.
     *
     * <p>Breaks if any of the numbers moves, if the enchantment becomes obtainable on a helmet, or
     * if the maximum level shrinks - which is worth knowing because the end city loot pool hands
     * out a level II book and the trial vaults hand out level I, and a max level of 1 would make
     * one of them unobtainable nonsense.
     */
    public static void theDoubleJumpEnchantmentKeepsItsLevelsWeightCostsAndBootSlot(GameTestHelper helper) {
        Enchantment airJump = enchantment(helper, ModEnchantments.DOUBLE_JUMP).value();

        Assertions.valueEqual(helper, airJump.getMaxLevel(), AIR_JUMP_MAX_LEVEL, "max level of the air jump");
        Assertions.valueEqual(helper, airJump.getMinLevel(), 1, "min level of the air jump");
        Assertions.valueEqual(helper, airJump.getWeight(), AIR_JUMP_WEIGHT,
                "weight of the air jump - it decides how often the enchanting table offers it");
        Assertions.valueEqual(helper, airJump.getAnvilCost(), AIR_JUMP_ANVIL_COST, "anvil cost of the air jump");

        for (int level = 1; level <= AIR_JUMP_MAX_LEVEL; level++) {
            int step = (level - 1) * AIR_JUMP_COST_PER_LEVEL;
            Assertions.valueEqual(helper, airJump.getMinCost(level), AIR_JUMP_MIN_COST_BASE + step,
                    "minimum enchanting cost of air jump " + level);
            Assertions.valueEqual(helper, airJump.getMaxCost(level), AIR_JUMP_MAX_COST_BASE + step,
                    "maximum enchanting cost of air jump " + level);
        }

        Assertions.valueEqual(helper, airJump.definition().slots(), List.of(EquipmentSlotGroup.FEET),
                "the equipment slot groups the air jump is declared for");
        helper.assertTrue(airJump.matchingSlot(EquipmentSlot.FEET),
                "vanilla no longer counts the air jump in the feet slot, so the enchantment is inert");
        helper.assertFalse(airJump.matchingSlot(EquipmentSlot.HEAD),
                "the air jump now counts in the head slot as well");
        helper.assertFalse(airJump.matchingSlot(EquipmentSlot.MAINHAND),
                "the air jump now counts in the main hand as well");

        helper.assertTrue(airJump.isSupportedItem(new ItemStack(Items.DIAMOND_BOOTS)),
                "the air jump can no longer be put on boots at all");
        helper.assertFalse(airJump.isSupportedItem(new ItemStack(Items.DIAMOND_HELMET)),
                "the air jump became applicable to a helmet; the server side handler only ever reads "
                        + "the feet slot, so such a helmet would be a dead enchantment");

        TestCleanup.succeed(helper);
    }

    /**
     * The two halves of the air jump disagree about where the enchantment has to sit, and this
     * pins both of them as they are.
     *
     * <p>{@code DoubleJumpController.getDoubleJumpLevel} - the client's decision whether a second
     * jump is allowed - walks every {@link EquipmentSlot} and takes the highest level it finds, so
     * an enchanted helmet or even an enchanted stack in the off hand grants the jump.
     * {@code ModMessageHandlers.handleDoubleJump} - the server's answer to the resulting payload -
     * reads {@code EquipmentSlot.FEET} and nothing else, so it refuses to clear the fall distance
     * for that same player. The jump happens, the landing hurts. See the class javadoc.
     *
     * <p>The boots case is driven first and deliberately: without it, a handler that had stopped
     * clearing the fall distance for <em>everyone</em> would make the helmet assertion below pass
     * for entirely the wrong reason.
     *
     * <p><b>Not covered:</b> the client half. {@code DoubleJumpController.getDoubleJumpLevel}
     * takes a plain {@link Player} and touches nothing client side, but it sits in a class whose
     * {@code tick(Minecraft)} signature names {@code net.minecraft.client.Minecraft}. On Fabric's
     * dedicated game test server that class cannot be loaded at all - the run dies with
     * "Cannot load class net.minecraft.client.player.LocalPlayer in environment type SERVER"
     * before a single assertion runs. Moving that one static method into a loader neutral class
     * would make the client half testable; until then the asymmetry below is pinned from the
     * server side only.
     *
     * <p>Breaks if the server starts reading every slot (that would be the fix, and it needs its
     * own test) or if the handler stops clearing the fall distance for enchanted boots.
     */
    public static void theServerOnlyCreditsTheBootsForAnAirJump(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        Holder<Enchantment> airJump = enchantment(helper, ModEnchantments.DOUBLE_JUMP);

        // --- the server side, first on the boots so the helmet case below cannot pass emptily ---
        player.setItemSlot(EquipmentSlot.FEET, enchantedWith(airJump, Items.DIAMOND_BOOTS, 1));
        player.fallDistance = FALL_DISTANCE_BEFORE_JUMP;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(player.fallDistance == 0.0,
                "the handler did not clear the fall distance for enchanted boots - it is "
                        + player.fallDistance + " - so the assertion below would prove nothing");

        // --- ... and then on the helmet, which the server does not credit ---
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.HEAD, enchantedWith(airJump, Items.DIAMOND_HELMET, 2));
        player.fallDistance = FALL_DISTANCE_BEFORE_JUMP;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(player.fallDistance == FALL_DISTANCE_BEFORE_JUMP,
                "the server now clears the fall distance for an enchantment outside the feet slot. "
                        + "That aligns it with the client, which reads every slot - a good change, but "
                        + "it is a change: replace this pin with a test for the new behaviour");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (d) ENDERITE ARMOUR
    // =====================================================================================

    /**
     * Enderite armour does not stop void damage, it rations it: with pieces worn, a player standing
     * in the void is only hurt on ticks that are a multiple of an interval, and that interval grows
     * with the number of pieces - 20, 40, 60 and 100 ticks for one to four pieces.
     *
     * <p>The interval is <em>measured</em> rather than asserted one tick at a time: the player is
     * hurt on seventeen fixed ticks and the set of ticks that actually cost health is compared with
     * the set the expected interval predicts. Those seventeen ticks are picked so that every
     * interval the mixin can produce leaves a different set behind (see {@link #VOID_PROBE_TICKS}),
     * so this fails on a swapped pair of intervals, on an off-by-one, and on the two degenerate
     * cases - protection that never fires and protection that always fires.
     *
     * <p>Making the mock player damageable takes three switches and the game mode is none of them;
     * the one that is easy to miss is {@link ServerboundPlayerLoadedPacket}, without which
     * {@code ServerPlayer#isInvulnerableTo} keeps answering true for the first sixty ticks and
     * every hit here would be swallowed. The bare-headed run at the top is the guard against
     * exactly that: without armour every single probe has to hurt.
     *
     * <p>The two counter samples at the end are the conditions the branch is gated on. Ordinary
     * damage is not rationed even under a full set - the mixin is void protection, not general
     * protection - and a mob wearing the same four pieces is not protected at all, because the
     * branch starts with {@code instanceof Player}.
     *
     * <p>Breaks if an interval changes, if the piece count stops being counted (all four counts
     * would collapse onto one interval), if the damage type filter is dropped so enderite armour
     * starts rationing every kind of damage, or if the player condition is widened to every living
     * entity.
     */
    public static void enderiteArmourSwallowsVoidDamageExceptOnItsIntervalTick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerablePlayer(helper);
        DamageSource voidDamage = level.damageSources().fellOutOfWorld();

        // --- guard: without armour every probe has to land, or nothing below means anything ---
        wearEnderite(player, 0);
        Set<Integer> bare = damagingTicks(helper, player, voidDamage);
        Assertions.valueEqual(helper, bare, allProbeTicks(),
                "an unarmoured player did not take void damage on every probed tick; either the mock "
                        + "player is invulnerable again or the mixin now protects players who wear "
                        + "nothing at all");

        // --- one piece at a time, each with its own interval ---
        for (int pieces = 1; pieces <= ENDERITE_PIECES.length; pieces++) {
            wearEnderite(player, pieces);
            int expected = VOID_INTERVALS[pieces - 1];
            Assertions.valueEqual(helper, damagingTicks(helper, player, voidDamage), multiplesOf(expected),
                    "with " + pieces + " enderite piece(s) the void has to hurt every " + expected
                            + " ticks; the ticks that actually cost health are on the left");
        }

        // --- the damage type filter: an ordinary hit is not rationed, full set or not ---
        wearEnderite(player, ENDERITE_PIECES.length);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        player.tickCount = VOID_UNPROTECTED_TICK;
        player.hurtServer(level, level.damageSources().generic(), UNRELATED_HIT_DAMAGE);
        helper.assertTrue(player.getHealth() < player.getMaxHealth(),
                "a full enderite set swallowed ordinary damage as well; the FELL_OUT_OF_WORLD filter "
                        + "is gone and the armour is now general invulnerability on 99 of 100 ticks");

        // --- the player condition: a mob in the same armour gets nothing ---
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        TestCleanup.before(helper, zombie::discard);
        for (int slot = 0; slot < ENDERITE_PIECES.length; slot++) {
            zombie.setItemSlot(ARMOUR_SLOTS[slot], new ItemStack(ENDERITE_PIECES[slot]));
        }
        zombie.tickCount = VOID_UNPROTECTED_TICK;
        float zombieHealth = zombie.getHealth();
        zombie.hurtServer(level, voidDamage, UNRELATED_HIT_DAMAGE);
        helper.assertTrue(zombie.getHealth() < zombieHealth || !zombie.isAlive(),
                "a zombie in a full enderite set survived void damage on a tick that is no multiple "
                        + "of any interval; the protection is no longer limited to players");

        TestCleanup.succeed(helper);
    }

    /**
     * The other half of the enderite set: holding the jump key while falling gives a slow fall, as
     * long as at least two pieces are worn. All four conditions of
     * {@code PlayerEntityMixin#simplebuilding$tickLogic} are driven here, each against its own
     * opposite.
     *
     * <p>The effect is granted at the tail of {@code Player#tick}, which a mock player only reaches
     * through {@code connection.tick()} - the gametest server ticks {@code ServerPlayer#tick()},
     * and that one does not call {@code super.tick()}; {@code doTick()} does, and the connection is
     * what calls it. The connection also snaps the player back to where it was before the tick, so
     * the position is stable across the six runs below while the physics - and with it the
     * downward speed and the ground contact the mixin reads - really happened.
     *
     * <p>The effect is asked for with a duration of two ticks and re-granted every tick, so the
     * duration is asserted exactly: a longer one would mean the player keeps floating for a while
     * after letting go of the key, which is precisely what the two-tick refresh avoids.
     *
     * <p>The speed threshold is probed from both sides, and that is the point of the third and
     * fourth case. A single downward sample would leave {@code < -0.1} indistinguishable from
     * {@code < 0.0}, i.e. from "any downward movement at all" - and that reading is exactly the
     * fall damage immunity the feature avoids, because a player who has just walked off a block is
     * moving downwards too. See {@link #STEP_OFF_SPEED} for the three speeds and what one tick of
     * vanilla gravity turns them into; together they nail the threshold into
     * {@code (-0.5684, -0.0784]}.
     *
     * <p><b>What this test does not cover:</b> the {@code !onGround()} half of the condition. No
     * gametest can cover it, because vanilla will not produce the state that would tell the two
     * apart: {@code Entity#move} zeroes the vertical speed on the collision that sets
     * {@code onGround}, and the gravity applied afterwards in {@code LivingEntity#travelInAir}
     * only brings it back to {@code -0.0784} - above the speed threshold. A player on the ground
     * therefore always fails the speed check as well, so deleting {@code !onGround()} from the
     * mixin would not change a single assertion here. The "standing on solid ground" case below is
     * kept because it is the state a player is in most of the time, but it re-proves the speed
     * condition rather than the ground condition.
     *
     * <p>Breaks if the two-piece minimum changes, if the jump key is no longer consulted (the
     * effect would fire on every fall), if the speed threshold moves in either direction far enough
     * that a {@code -0.5684} fall stops counting or a {@code -0.0784} step off a block starts
     * counting, or if the effect stops being refreshed and turns into a lasting one.
     */
    public static void enderiteSlowFallNeedsTwoPiecesFallingSpeedAndTheJumpKey(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, FALLING_POS);
        helper.assertTrue(player instanceof ISpaceKeyTracker,
                "the player does not implement ISpaceKeyTracker, so PlayerEntityMixin did not apply "
                        + "on this loader and the whole feature is dead here");
        helper.setBlock(GROUND_BLOCK, Blocks.STONE);

        // --- everything in place: two pieces, falling, jump key held ---
        MobEffectInstance granted = slowFallAfterTick(helper, player, 2, FALLING_POS, FALL_SPEED, true);
        helper.assertTrue(granted != null,
                "two enderite pieces, a falling player and a held jump key produced no slow falling "
                        + "at all - the whole branch is gone or Player#tick was never reached");
        Assertions.valueEqual(helper, granted.getAmplifier(), 0, "amplifier of the granted slow falling");
        Assertions.valueEqual(helper, granted.getDuration(), SLOW_FALL_DURATION,
                "duration of the granted slow falling; it is re-granted every tick on purpose, so a "
                        + "longer one would keep the player floating after the key is released");

        // --- one piece is not enough ---
        helper.assertTrue(slowFallAfterTick(helper, player, 1, FALLING_POS, FALL_SPEED, true) == null,
                "a single enderite piece already gave slow falling; the two-piece minimum is gone");

        // --- rising instead of falling: the speed threshold, from far above ---
        helper.assertTrue(slowFallAfterTick(helper, player, 2, FALLING_POS, 0.2, true) == null,
                "a player who is not falling got slow falling; the downward speed condition is gone "
                        + "and the effect now fires while jumping upwards");

        // --- and from just above: one tick after walking off a block, which must NOT count ---
        helper.assertTrue(slowFallAfterTick(helper, player, 2, FALLING_POS, STEP_OFF_SPEED, true) == null,
                "a player who has merely stepped off a block got slow falling - one tick of gravity "
                        + "is about -0.078, which is above the mixin's -0.1 threshold. The threshold "
                        + "has been loosened towards zero, and enderite armour with the jump key held "
                        + "is now the fall damage immunity this feature is careful not to be");

        // --- jump key released ---
        helper.assertTrue(slowFallAfterTick(helper, player, 2, FALLING_POS, FALL_SPEED, false) == null,
                "slow falling was granted without the jump key held, so enderite armour has quietly "
                        + "become permanent fall damage immunity");

        // --- standing on solid ground (no fall, and on the ground) ---
        helper.assertTrue(slowFallAfterTick(helper, player, 4, GROUND_POS, FALL_SPEED, true) == null,
                "a player standing on a block got slow falling");

        // --- and a full set still works, so the check is a minimum and not an exact count ---
        helper.assertTrue(slowFallAfterTick(helper, player, 4, FALLING_POS, FALL_SPEED, true) != null,
                "a full enderite set got no slow falling although two pieces do; the piece count is "
                        + "compared for equality instead of as a minimum");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // (e) HOW MANY POOLS, AND WHAT THE AIR JUMP BOOKS ARE WORTH
    // =====================================================================================

    /**
     * How many pools each of the sixteen modified vanilla tables gets, and what the air jump books
     * inside three of them are worth.
     *
     * <p>Both halves are deliberately the halves {@code ConfigOptionTests} cannot see. That test
     * asserts "more than nothing" per table and then rolls the pools to prove the documented items
     * come out - so a table that quietly received a second copy of its pool, or lost one of two,
     * still passes there. Here the count is exact, and the number that matters is the trial chamber
     * rare vault: it is the single key that <em>both</em> vault {@code if}s in
     * {@code ModLootTableModifications} match, so it has to end up with two pools while its two
     * siblings get one each, and the end city has to end up with three because it takes two pre
     * built pools plus the builder one. An {@code ||} that lost an operand, or a copy pasted
     * {@code editor.addPool} left behind after a refactor, shows up here and nowhere else.
     *
     * <p>The three untouched vanilla tables are chosen to sit next to a modified one - the bastion
     * bridge next to the two modified bastion chests, the unique and the ominous rare vault next to
     * the three modified vaults. Those are the keys a widened condition catches first, and the ones
     * a player would notice least.
     *
     * <p>The books are read by encoding the pool through {@code LootPool.CODEC}, which is the shape
     * datagen writes to json. Rolling them, as the sibling test does, proves the book exists but
     * says nothing about how likely it is: a level II air jump book with a weight of 1 in an end
     * city is a different feature from one with a weight of 10, and both roll out eventually. The
     * roll count of the pool is compared the same way, against a freshly encoded
     * {@code UniformGenerator}, so a vault pool cannot quietly become a guaranteed drop.
     *
     * <p>{@code enableLootTableChanges} is switched on and restored again - in a {@code finally}
     * and from {@code runBeforeTestEnd}, exactly as the sibling class does it - because these
     * numbers only exist while the option is on. That the option really stops the branches is the
     * sibling's assertion, not this one's.
     *
     * <p>Breaks if a table gains or loses a pool, if the rare vault stops matching both vault
     * branches, if the mod starts writing into a neighbouring vanilla table, or if an air jump book
     * loses its level, its weight or the roll range of the pool it sits in.
     */
    public static void modLootPoolsKeepTheirExactCountAndTheAirJumpBookWeights(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        try {
            setLootTableChanges(helper, true);

            for (Map.Entry<ResourceKey<LootTable>, Integer> wanted : POOLS_PER_TABLE.entrySet()) {
                Assertions.valueEqual(helper, recordPools(helper, wanted.getKey()).size(), wanted.getValue(),
                        "pools the mod hands to " + wanted.getKey().identifier());
            }

            for (ResourceKey<LootTable> untouched : NEIGHBOURING_TABLES) {
                Assertions.valueEqual(helper, recordPools(helper, untouched).size(), 0,
                        "pools were added to " + untouched.identifier() + ", a vanilla table that sits "
                                + "next to a modified one and must stay untouched");
            }

            // --- the air jump books, with the level and the weight they were given ---
            assertAirJumpBook(helper, ops, BuiltInLootTables.END_CITY_TREASURE, 2, 10,
                    UniformGenerator.between(0.0F, 4.0F));
            assertAirJumpBook(helper, ops, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 1, 7,
                    UniformGenerator.between(0.0F, 1.0F));
            assertAirJumpBook(helper, ops, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 1, 7,
                    UniformGenerator.between(0.0F, 1.0F));
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS: ORES
    // =====================================================================================

    /** Rolls one ore's loot table with a bare hand, with silk touch and with Fortune III. */
    private static void assertOreDrops(GameTestHelper helper, Block ore, Item dust, Item oreItem) {
        BlockState state = ore.defaultBlockState();
        LootTable table = lootTableOf(helper, ore);
        String name = BuiltInRegistries.BLOCK.getKey(ore).toString();

        // --- bare hand: exactly one dust, on every seed ---
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            List<ItemStack> drops = rollDrops(helper, table, state, ItemStack.EMPTY, LOOT_SEED + roll);
            Assertions.valueEqual(helper, drops.size(), 1, name + " dropped more than one stack bare handed");
            helper.assertTrue(drops.get(0).is(dust),
                    name + " dropped " + drops.get(0) + " bare handed instead of "
                            + BuiltInRegistries.ITEM.getKey(dust));
            Assertions.valueEqual(helper, drops.get(0).getCount(), 1,
                    name + " dropped more than one " + BuiltInRegistries.ITEM.getKey(dust)
                            + " without any Fortune on the tool");
        }

        // --- silk touch: the ore block itself, never the dust ---
        ItemStack silkTouch = pickaxeWith(helper, Enchantments.SILK_TOUCH, 1);
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            List<ItemStack> drops = rollDrops(helper, table, state, silkTouch, LOOT_SEED + roll);
            Assertions.valueEqual(helper, drops.size(), 1, name + " dropped more than one stack under silk touch");
            helper.assertTrue(drops.get(0).is(oreItem),
                    name + " dropped " + drops.get(0) + " under silk touch instead of the ore block; "
                            + "the match_tool condition on the first alternative is gone");
        }

        // --- Fortune III: still the dust, but more of it ---
        ItemStack fortune = pickaxeWith(helper, Enchantments.FORTUNE, FORTUNE_LEVEL);
        int highest = 0;
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            List<ItemStack> drops = rollDrops(helper, table, state, fortune, LOOT_SEED + roll);
            Assertions.valueEqual(helper, drops.size(), 1, name + " dropped more than one stack under Fortune");
            ItemStack drop = drops.get(0);
            helper.assertTrue(drop.is(dust),
                    name + " dropped " + drop + " under Fortune instead of "
                            + BuiltInRegistries.ITEM.getKey(dust) + "; a Fortune pickaxe must not "
                            + "take the silk touch branch");
            helper.assertTrue(drop.getCount() >= 1 && drop.getCount() <= FORTUNE_MAX_COUNT,
                    name + " dropped " + drop.getCount() + " under Fortune " + FORTUNE_LEVEL
                            + "; apply_bonus/ore_drops can only multiply by 1 to "
                            + FORTUNE_MAX_COUNT + " at that level");
            highest = Math.max(highest, drop.getCount());
        }
        helper.assertTrue(highest > 1,
                name + " never dropped more than one item in " + LOOT_ROLLS + " rolls with Fortune "
                        + FORTUNE_LEVEL + "; the apply_bonus function is gone and the ore no longer "
                        + "rewards an enchanted pickaxe");

        // --- Fortune III, wide sample: which formula, not just which ceiling ---
        // Everything above holds just as well for uniform_bonus_count with a bonus multiplier of
        // one - it covers the same 1..FORTUNE_MAX_COUNT range at this level. Only the share of
        // drops that stay single separates the two, so that share is what decides here.
        int singles = 0;
        for (int roll = 0; roll < FORTUNE_SHAPE_ROLLS; roll++) {
            List<ItemStack> drops = rollDrops(helper, table, state, fortune, shapeSeed(roll));
            Assertions.valueEqual(helper, drops.size(), 1,
                    name + " dropped more than one stack under Fortune");
            ItemStack drop = drops.get(0);
            helper.assertTrue(drop.is(dust),
                    name + " dropped " + drop + " under Fortune instead of "
                            + BuiltInRegistries.ITEM.getKey(dust));
            helper.assertTrue(drop.getCount() >= 1 && drop.getCount() <= FORTUNE_MAX_COUNT,
                    name + " dropped " + drop.getCount() + " under Fortune " + FORTUNE_LEVEL
                            + "; apply_bonus/ore_drops can only multiply by 1 to "
                            + FORTUNE_MAX_COUNT + " at that level");
            if (drop.getCount() == 1) {
                singles++;
            }
        }
        helper.assertTrue(singles >= FORTUNE_SINGLE_DROPS_MIN && singles <= FORTUNE_SINGLE_DROPS_MAX,
                name + " left " + singles + " of " + FORTUNE_SHAPE_ROLLS + " Fortune " + FORTUNE_LEVEL
                        + " drops at a single item, outside the " + FORTUNE_SINGLE_DROPS_MIN + ".."
                        + FORTUNE_SINGLE_DROPS_MAX + " band that apply_bonus/ore_drops produces "
                        + "(two rolls in five); uniform_bonus_count with a bonus multiplier of one "
                        + "reaches the same " + FORTUNE_MAX_COUNT + " ceiling but leaves only one "
                        + "roll in four single, i.e. 2.5 instead of 2.2 items per ore");
    }

    /** The loot table the block points at, resolved through the server the way a break does. */
    private static LootTable lootTableOf(GameTestHelper helper, Block ore) {
        ResourceKey<LootTable> key = ore.getLootTable().orElse(null);
        helper.assertTrue(key != null,
                BuiltInRegistries.BLOCK.getKey(ore) + " has no loot table at all, so it drops nothing");
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
    }

    /**
     * One seeded roll of a block loot table. The tool is a real loot parameter, which is what makes
     * the {@code match_tool} condition and the {@code apply_bonus} function see it.
     */
    private static List<ItemStack> rollDrops(GameTestHelper helper, LootTable table, BlockState state,
                                             ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(LOOT_ORIGIN)))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .create(LootContextParamSets.BLOCK);
        return table.getRandomItems(params, seed);
    }

    /**
     * The seed for one roll of the distribution sample, mixed rather than counted up. Loot seeds
     * end up in a legacy {@code java.util.Random}, and consecutive seeds are not independent draws
     * there: their first outputs form an arithmetic progression, so a counted-up sample would
     * measure that progression instead of the formula behind it. The SplitMix64 finaliser below
     * scatters the seeds, which is what lets the band on the single drops be read as the binomial
     * spread it is. It stays a pure function of the roll index, so the sample is still identical
     * on every machine.
     */
    private static long shapeSeed(int roll) {
        long mixed = LOOT_SEED + roll * 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static ItemStack pickaxeWith(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(enchantment(helper, key), level);
        return pickaxe;
    }

    /** Hardness, blast resistance, light and the tool gate of one ore. */
    private static void assertOreBlock(GameTestHelper helper, Block ore, BlockPos relativePos,
                                       float hardness, int light) {
        String name = BuiltInRegistries.BLOCK.getKey(ore).toString();
        helper.setBlock(relativePos, ore);
        BlockState state = helper.getBlockState(relativePos);
        helper.assertTrue(state.is(ore), name + " could not be placed for this test");

        helper.assertTrue(Math.abs(state.getDestroySpeed(helper.getLevel(), helper.absolutePos(relativePos))
                        - hardness) < 1.0E-4F,
                name + " hardness should be " + hardness + " but is "
                        + state.getDestroySpeed(helper.getLevel(), helper.absolutePos(relativePos)));
        helper.assertTrue(Math.abs(ore.getExplosionResistance() - 1200.0F) < 1.0E-4F,
                name + " blast resistance should be 1200 but is " + ore.getExplosionResistance()
                        + "; the ore would stop surviving a creeper or a dragon");
        Assertions.valueEqual(helper, state.getLightEmission(), light, name + " light emission");

        helper.assertTrue(state.requiresCorrectToolForDrops(),
                name + " no longer requires the correct tool, so any tool drops it");
        helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                name + " left #minecraft:mineable/pickaxe, so no pickaxe is the right tool for it");
        helper.assertTrue(state.is(BlockTags.NEEDS_DIAMOND_TOOL),
                name + " left #minecraft:needs_diamond_tool, so a stone pickaxe is enough for it now");

        // Vanilla's own decision, i.e. the call ServerPlayerGameMode#destroyBlock makes.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        assertHarvest(helper, player, state, ItemStack.EMPTY, false, name + " with a bare hand");
        assertHarvest(helper, player, state, new ItemStack(Items.WOODEN_PICKAXE), false,
                name + " with a wooden pickaxe");
        assertHarvest(helper, player, state, new ItemStack(Items.IRON_PICKAXE), false,
                name + " with an iron pickaxe");
        assertHarvest(helper, player, state, new ItemStack(Items.DIAMOND_PICKAXE), true,
                name + " with a diamond pickaxe");
        assertHarvest(helper, player, state, new ItemStack(Items.NETHERITE_PICKAXE), true,
                name + " with a netherite pickaxe");
        assertHarvest(helper, player, state, new ItemStack(Items.DIAMOND_SHOVEL), false,
                name + " with a diamond shovel");
    }

    private static void assertHarvest(GameTestHelper helper, Player player, BlockState state,
                                      ItemStack tool, boolean expected, String what) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        helper.assertTrue(player.hasCorrectToolForDrops(state) == expected,
                what + (expected ? " must yield drops but does not" : " yields drops but must not"));
    }

    /** Breaks one ore repeatedly and checks the experience that comes out of it. */
    private static void assertOreExperience(GameTestHelper helper, Block ore, BlockPos relativePos) {
        BlockPos absolute = helper.absolutePos(relativePos);
        String name = BuiltInRegistries.BLOCK.getKey(ore).toString();
        TreeSet<Integer> amounts = new TreeSet<>();

        for (int sample = 0; sample < EXPERIENCE_SAMPLES; sample++) {
            clearDropsAround(helper, absolute);
            helper.setBlock(relativePos, ore);
            helper.assertTrue(helper.getLevel().destroyBlock(absolute, true),
                    "could not break " + name + " at " + relativePos);

            int orbs = 0;
            int total = 0;
            for (ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, aroundBlock(absolute))) {
                orbs++;
                total += orb.getValue();
            }

            helper.assertTrue(orbs > 0,
                    name + " dropped no experience at all; it is no longer a DropExperienceBlock");
            helper.assertTrue(total >= EXPERIENCE_MIN && total <= EXPERIENCE_MAX,
                    name + " awarded " + total + " experience, which is outside the declared range "
                            + EXPERIENCE_MIN + ".." + EXPERIENCE_MAX);
            amounts.add(total);
        }

        // Both ends, not just "more than one value". Without these a narrowed range - of(4, 7),
        // of(3, 5), of(5, 7) - would satisfy "inside 3..7" and "at least two different sums" on
        // every single run, and the ore would quietly hand out a different amount forever.
        helper.assertTrue(amounts.first() == EXPERIENCE_MIN,
                name + " never awarded as little as " + EXPERIENCE_MIN + " experience in "
                        + EXPERIENCE_SAMPLES + " breaks - the sums seen were " + amounts
                        + "; the low end of the range has moved up (or the range collapsed)");
        helper.assertTrue(amounts.last() == EXPERIENCE_MAX,
                name + " never awarded as much as " + EXPERIENCE_MAX + " experience in "
                        + EXPERIENCE_SAMPLES + " breaks - the sums seen were " + amounts
                        + "; the high end of the range has moved down (or the range collapsed)");
        clearDropsAround(helper, absolute);
    }

    /** A box just big enough to hold what one broken block leaves behind, and no neighbour's. */
    private static AABB aroundBlock(BlockPos absolute) {
        return AABB.ofSize(Vec3.atCenterOf(absolute), 3.0, 3.0, 3.0);
    }

    /** Removes the orbs and items of the previous sample so the next one measures only itself. */
    private static void clearDropsAround(GameTestHelper helper, BlockPos absolute) {
        AABB box = aroundBlock(absolute);
        helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).forEach(ExperienceOrb::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
    }

    // =====================================================================================
    // HELPERS: PLAYERS
    // =====================================================================================

    /**
     * The house standard mock player: in the level, with a connection, and creative whether we like
     * it or not. Handed back when the test ends, because a leaked mock player keeps the player list
     * non-empty and stalls the gametest server on shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * The same player, but able to lose health. Three switches, and the game mode is none of them:
     * {@code abilities.invulnerable} for {@code Player#hurtServer}, the entity flag for
     * {@code Entity#isInvulnerableTo}, and the packet a real client sends once it has loaded -
     * without that last one {@code ServerPlayer#isInvulnerableTo} answers true for sixty ticks.
     * Absorption is cleared as well, because it would be eaten before the health bar and make every
     * measurement below lie.
     */
    private static ServerPlayer vulnerablePlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().invulnerable = false;
        player.setInvulnerable(false);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setAbsorptionAmount(0.0F);
        return player;
    }

    /** Dresses the player in exactly {@code pieces} enderite items, clearing the other slots. */
    private static void wearEnderite(ServerPlayer player, int pieces) {
        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            player.setItemSlot(ARMOUR_SLOTS[slot],
                    slot < pieces ? new ItemStack(ENDERITE_PIECES[slot]) : ItemStack.EMPTY);
        }
    }

    /**
     * Hits the player once on every probe tick and returns the ticks that cost health. The
     * invulnerability window is reset before each hit, because otherwise every second hit would
     * arrive inside the damage cooldown and look exactly like perfect protection.
     */
    private static Set<Integer> damagingTicks(GameTestHelper helper, ServerPlayer player, DamageSource source) {
        Set<Integer> hit = new TreeSet<>();
        for (int tick : VOID_PROBE_TICKS) {
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;
            player.tickCount = tick;
            player.hurtServer(helper.getLevel(), source, VOID_HIT_DAMAGE);
            if (player.getHealth() < player.getMaxHealth()) {
                hit.add(tick);
            }
        }
        player.setHealth(player.getMaxHealth());
        return hit;
    }

    /** The probe ticks an interval of {@code interval} would let through. */
    private static Set<Integer> multiplesOf(int interval) {
        Set<Integer> expected = new TreeSet<>();
        for (int tick : VOID_PROBE_TICKS) {
            if (tick % interval == 0) {
                expected.add(tick);
            }
        }
        return expected;
    }

    /** Every probe tick, i.e. what an unprotected player has to answer with. */
    private static Set<Integer> allProbeTicks() {
        Set<Integer> all = new TreeSet<>();
        for (int tick : VOID_PROBE_TICKS) {
            all.add(tick);
        }
        return all;
    }

    /**
     * Sets one case up, runs a single {@code Player#tick} through the connection and hands back the
     * slow falling the mixin granted, or {@code null}. The effect is removed first, because it is
     * asked for with a duration of two ticks and would otherwise survive from the previous case and
     * make the next one look like a success.
     */
    private static MobEffectInstance slowFallAfterTick(GameTestHelper helper, ServerPlayer player,
                                                       int pieces, Vec3 relativePos, double speed,
                                                       boolean jumpKey) {
        player.removeEffect(MobEffects.SLOW_FALLING);
        wearEnderite(player, pieces);
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setDeltaMovement(0.0, speed, 0.0);
        ((ISpaceKeyTracker) player).simplebuilding$setSpacePressed(jumpKey);
        player.connection.tick();
        return player.getEffect(MobEffects.SLOW_FALLING);
    }

    private static ItemStack enchantedWith(Holder<Enchantment> enchantment, Item item, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    // =====================================================================================
    // HELPERS: ITEM FRAME
    // =====================================================================================

    /** Spawns a frame holding a diamond, plus the block it hangs on; both go at the end. */
    private static ItemFrame frameWithItem(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos.relative(FRAME_FACING.getOpposite()), Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relativePos), FRAME_FACING);
        helper.getLevel().addFreshEntity(frame);
        TestCleanup.before(helper, frame::discard);
        frame.setItem(new ItemStack(Items.DIAMOND), false);
        return frame;
    }

    /**
     * Right clicks the frame server side. The stack is handed in by reference on purpose - the
     * player's hand slot keeps that very object - so the caller can check afterwards whether the
     * interaction wrote a component into it.
     */
    private static InteractionResult interact(ItemFrame frame, Player player, ItemStack held, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return frame.interact(player, InteractionHand.MAIN_HAND);
    }

    /**
     * The lock lives in a {@code @Unique} mixin field with no accessor, so it is read the way the
     * game reads it: through the entity's save data.
     */
    private static boolean isLocked(GameTestHelper helper, ItemFrame frame) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        frame.saveWithoutId(output);
        return output.buildResult().getBooleanOr(LOCK_TAG, false);
    }

    private static ItemStack magnetWithConstructorsTouch(GameTestHelper helper) {
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        magnet.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        return magnet;
    }

    /** The filter the mixin wrote into the magnet, or {@code ""} when it wrote nothing. */
    private static String filterOf(ItemStack magnet) {
        return magnet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(MAGNET_FILTER_TAG, "");
    }

    // =====================================================================================
    // HELPERS: LOOT TABLES
    // =====================================================================================

    /** Writes the option and reads it back through the same accessor the mod uses. */
    private static void setLootTableChanges(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().worldGen.enableLootTableChanges = value;
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges == value,
                "worldGen.enableLootTableChanges did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    /** Runs the mod's loot table hook for one table and hands back the pools it offered. */
    private static List<LootPool> recordPools(GameTestHelper helper, ResourceKey<LootTable> key) {
        PoolRecorder recorder = new PoolRecorder();
        ModLootTableModifications.apply(key, recorder, helper.getLevel().registryAccess());
        return recorder.pools;
    }

    /**
     * Finds the air jump book in one table's pools and checks its level, its weight and the number
     * of rolls of the pool it sits in.
     */
    private static void assertAirJumpBook(GameTestHelper helper, RegistryOps<JsonElement> ops,
                                          ResourceKey<LootTable> key, int level, int weight,
                                          NumberProvider rolls) {
        String name = key.identifier().toString();
        JsonElement expectedRolls = NumberProviders.CODEC.encodeStart(ops, rolls).getOrThrow();
        int found = 0;

        for (LootPool pool : recordPools(helper, key)) {
            JsonElement encoded = LootPool.CODEC.encodeStart(ops, pool).getOrThrow();
            helper.assertTrue(encoded.isJsonObject(), name + ": a pool did not encode to a json object");
            JsonObject poolJson = encoded.getAsJsonObject();

            for (JsonObject book : bookEntries(poolJson, new ArrayList<>())) {
                int storedLevel = storedLevel(book, AIR_JUMP_ID);
                if (storedLevel == 0) {
                    continue;
                }
                found++;
                Assertions.valueEqual(helper, storedLevel, level, name + ": level of the air jump book");
                Assertions.valueEqual(helper, weightOf(book), weight, name + ": weight of the air jump book");
                Assertions.valueEqual(helper, poolJson.get("rolls"), expectedRolls,
                        name + ": rolls of the pool the air jump book sits in");
            }
        }

        Assertions.valueEqual(helper, found, 1,
                name + " offers " + found + " air jump books instead of exactly one; that chest is "
                        + "part of the only supply of " + AIR_JUMP_ID + " in the game");
    }

    /** Every {@code minecraft:enchanted_book} entry anywhere below {@code element}. */
    private static List<JsonObject> bookEntries(JsonElement element, List<JsonObject> into) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement name = object.get("name");
            if (name != null && name.isJsonPrimitive() && ENCHANTED_BOOK_ID.equals(name.getAsString())) {
                into.add(object);
            }
            for (Map.Entry<String, JsonElement> child : object.entrySet()) {
                bookEntries(child.getValue(), into);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                bookEntries(child, into);
            }
        }
        return into;
    }

    /**
     * The level {@code enchantmentId} is stored with anywhere below {@code element}, or {@code 0}.
     * Searched instead of walked down a fixed path on purpose: the exact nesting of
     * {@code set_components} around an {@code ItemEnchantments} component is vanilla's business and
     * has moved between versions, while the id and the level are the mod's promise.
     */
    private static int storedLevel(JsonElement element, String enchantmentId) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement direct = object.get(enchantmentId);
            if (direct != null && direct.isJsonPrimitive() && direct.getAsJsonPrimitive().isNumber()) {
                return direct.getAsInt();
            }
            for (Map.Entry<String, JsonElement> child : object.entrySet()) {
                int nested = storedLevel(child.getValue(), enchantmentId);
                if (nested > 0) {
                    return nested;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                int nested = storedLevel(child, enchantmentId);
                if (nested > 0) {
                    return nested;
                }
            }
        }
        return 0;
    }

    /** The weight of a loot entry; vanilla leaves the default of 1 out of the json. */
    private static int weightOf(JsonObject entry) {
        JsonElement weight = entry.get("weight");
        return weight != null && weight.isJsonPrimitive() ? weight.getAsInt() : 1;
    }

    /** Collects the pools the mod offers for one table, in the order it offers them. */
    private static final class PoolRecorder implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();

        @Override
        public void addPool(LootPool.Builder pool) {
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.pools.add(pool);
        }
    }
}
