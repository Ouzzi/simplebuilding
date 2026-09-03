package com.simplebuilding.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Lifecycle;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.trim.ModTrimMaterials;
import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimBenefitUser;
import com.simplebuilding.util.TrimEffectUtil;
import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The <em>wiring</em> of the armour trim benefits: the inputs the multiplier is built from, the
 * mixins that carry a bonus to the player, the tracker that survives a save and a death, the two
 * item tags that make the trims craftable at all, and the operator command that scales everything.
 *
 * <p>{@link TrimEffectTests} pins what {@code TrimEffectUtil} <em>computes</em> - every count,
 * every rate, every clamp - and {@link HopperAndTrimTests} pins the experience curve and the
 * configured base. None of that is repeated here. What none of it can notice is a mixin dropping
 * out of {@code simplebuilding.mixins.json}, a renamed NBT key silently resetting every save, a
 * respawn that forgets to rebase, or a data tag that stops reaching the smithing table: the
 * numbers would all still be right and no player would ever see them. That gap is what this class
 * is for, so nearly every assertion below goes through vanilla's own call - {@code Player.getSpeed},
 * {@code hurtServer}, {@code baseTick}, {@code restoreFrom}, the smithing recipe, the command
 * dispatcher - rather than through the mod's utility class.
 *
 * <h2>How the numbers are obtained</h2>
 *
 * <p>Balance constants are <em>measured</em>, not copied. The two curve scales (distance and play
 * time) and the combat scale are recovered by inverting the curve from a measurement, using the
 * curve's own measured floor and ceiling; the three combat weights are pinned against each other
 * (one hostile kill has to be worth exactly five passive ones and twenty points of damage taken);
 * and every mixin delivery is asserted against what {@code TrimEffectUtil} reports for that same
 * player at that same moment. A rebalance therefore moves these tests only when it changes a
 * <em>relationship</em>, which is what they are about.
 *
 * <h2>Known defects</h2>
 * <ul>
 *   <li><b>The Tide swim bonus never reaches a player.</b>
 *       {@code LivingEntityMixin#simplebuilding$modifySwimSpeed} injects into
 *       {@code LivingEntity#getSpeed}, but {@code Player#getSpeed} <em>overrides</em> that method
 *       with {@code (float) getAttributeValue(MOVEMENT_SPEED)} and never calls {@code super}. The
 *       injection is therefore dead for players and only ever runs for other living entities.
 *       {@link #thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards} measures the swim
 *       bonus on an armour stand, where it does work, and deliberately makes its player-side
 *       assertions with a bolt (land) trim only - so they stay correct whichever way this is
 *       fixed, instead of freezing the defect in place.</li>
 *   <li><b>{@code MobCategory.WATER_AMBIENT} is in neither kill list.</b>
 *       {@code SurvivalTracerMixin} counts {@code CREATURE}, {@code AMBIENT},
 *       {@code WATER_CREATURE}, {@code UNDERGROUND_WATER_CREATURE} and {@code AXOLOTLS} as
 *       peaceful, so cod, salmon, tropical fish and pufferfish are the only mobs in the game whose
 *       death counts for nothing at all. Not asserted either way: pinning it would cement it.</li>
 *   <li><b>{@code ModTrimMaterials} keeps a dead second copy of every colour.</b> Next to the
 *       {@code bootstrap} datagen runs sit three {@code *_HOLDER} constants carrying the same three
 *       literals, and nothing reads them - not the registration, not datagen, not the item
 *       components. {@link #theThreeTrimMaterialsKeepTheirColoursAndTheirTags} therefore takes its
 *       expected colours from a recorded {@code bootstrap} run; comparing the registry against the
 *       copies instead only ever compared two literals that are edited, if at all, together.</li>
 *   <li><b>{@code ModCommands} exists four times</b> (Fabric, NeoForge, Forge and the 1.21.11
 *       tree) with no shared source. A gametest can only ever see the copy the running loader
 *       registered, so {@link #theTrimMultiplierCommandGuardsItsRangeAndItsPermission} covers one
 *       loader per run and the other copies stay unguarded.</li>
 * </ul>
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>The client half of {@code TrimMultiplierLogic}.</b> Both factor methods branch on
 *       {@code level().isClientSide()} and then read the live values out of the accessor instead of
 *       out of the statistics. A gametest server has no client side, so that branch is
 *       unreachable - and with it {@code simplebuilding$setCurrentValues}, which only the client
 *       mixin implements for real.</li>
 *   <li><b>{@code simplebuilding$syncTrimData} and the 20-tick sync in {@code SurvivalTracerMixin}.</b>
 *       Both do nothing but hand a payload to {@code PlatformServices.sendToPlayer}; the mock
 *       player's embedded connection swallows it and nothing on the far end can be inspected.</li>
 *   <li><b>The chat feedback of the command.</b> {@code sendSuccess} ends up in
 *       {@code ServerPlayer#sendSystemMessage}, i.e. in a packet. The command's return code and its
 *       effect on the config are checked instead.</li>
 *   <li><b>How a trim material's colour is rendered.</b> Only the {@code Style} on the description
 *       component is server side; the palette texture and the tooltip are not.</li>
 * </ul>
 */
public final class TrimWiringTests {

    private TrimWiringTests() {
    }

    /** The four slots {@code TrimEffectUtil} walks, and a carrier item for each. */
    private static final EquipmentSlot[] ARMOUR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Chainmail on purpose, exactly as in {@link TrimEffectTests}: the mod reads the trim
     * <em>material</em>, never the armour item, and "chainmail" collides with none of the material
     * names {@code TrimEffectUtil} looks for.
     */
    private static final Item[] ARMOUR_ITEMS = {
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
            Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
    };

    /** Raw damage every hit in {@link #everyServerSideHitRunsThroughTheTrimDamageModifier} deals. */
    private static final float HIT_DAMAGE = 10.0F;

    /** Exhaustion handed to {@code causeFoodExhaustion}; well under the 40.0 cap {@code FoodData} applies. */
    private static final float EXHAUSTION_UNIT = 2.0F;

    /** A wither the rib trim is strong enough to clear outright. Not a multiple of 40, see the test. */
    private static final int SHORT_WITHER = 101;

    /** A wither the rib trim can only heal against. Also not a multiple of 40. */
    private static final int LONG_WITHER = 401;

    /** Air supply every submerged case starts from, comfortably above the drowning threshold. */
    private static final int START_AIR = 200;

    /** The metres {@link #stampProgress} adds up to: 1 + 2 + 3 + 4 + 5, one per distance counter. */
    private static final int STAMPED_DISTANCE = 15;

    // =====================================================================================
    // (a) THE TWO PROGRESS FACTORS
    // =====================================================================================

    /**
     * The survival half of the trim multiplier: how far the player has walked and how long they
     * have been alive, both counted from the last death.
     *
     * <p>Four separate claims, none of which any existing test makes.
     * {@code HopperAndTrimTests#trimMultiplierFollowsTheExperienceCurveAndTheConfiguredBase} only
     * requires this factor to stay inside {@code 0.1..1.0}, which it does with the whole method
     * body deleted and replaced by {@code return 0.1}.
     *
     * <ol>
     *   <li><b>The floor.</b> A player who has done nothing gets the same starting factor from all
     *       three curves - survival, combat and experience-at-level-zero. Asserting them against
     *       each other rather than against a literal means a deliberate rebalance of the floor
     *       moves all three together and stays green, while one of them drifting alone goes red.</li>
     *   <li><b>The distance unit.</b> The five {@code *_ONE_CM} counters are summed after each is
     *       divided by 100, so 500 cm of walking and 100 cm in each of the five counters have to
     *       come out the same, and 99 cm has to come out as nothing at all.</li>
     *   <li><b>The two scales.</b> Recovered by inverting the curve at a single measurement, using
     *       the floor and the ceiling this same test measures. The pair is what makes an hour of
     *       play worth as much as four kilometres of walking; if either scale moves, one half of
     *       the mechanic quietly stops mattering.</li>
     *   <li><b>{@code max}, not {@code min} and not a sum.</b> Checked in both directions, so that
     *       neither "distance always wins" nor "time always wins" can pass.</li>
     * </ol>
     *
     * <p>The last block is the baseline arithmetic: with the base set to the current reading the
     * factor drops back to the floor, and with the base set <em>above</em> the current reading -
     * which is what a rolled-back statistics file looks like - it stays at the floor instead of
     * going negative and dragging the whole multiplier below zero.
     *
     * <p>What breaks this: a changed scale, a changed floor in one curve only, the {@code /100}
     * disappearing, one of the five distance counters being dropped, {@code max} turning into
     * {@code min}, or the {@code Math.max(0, ...)} clamps going away.
     */
    public static void theSurvivalFactorTracksDistanceAndTimeSinceTheLastDeath(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        SurvivalTracerAccessor tracker = tracker(helper, player);
        clearProgress(helper, player);

        // --- the floor, cross-checked against the other two curves ---
        double floor = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        helper.assertTrue(floor > 0.0 && floor < 1.0,
                "the survival factor of a player who has never moved is " + floor
                        + ", which is outside the 0..1 band the whole multiplier is built on");
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), floor,
                "the combat curve starts from a different floor than the survival curve");
        player.experienceLevel = 0;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), floor,
                "the experience curve starts from a different floor than the survival curve");

        // --- the ceiling, needed below to invert the curve ---
        setStat(player, Stats.WALK_ONE_CM, 100_000_000);
        double ceiling = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        helper.assertTrue(ceiling > floor && ceiling <= 1.0 + 1.0e-6,
                "the survival factor saturates at " + ceiling + "; it has to converge on a value "
                        + "above its floor and never above 1.0, otherwise the configured base is no "
                        + "longer the only thing that can push the multiplier past 1");

        // --- centimetres into metres, and all five counters into one sum ---
        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 99);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), floor,
                "99 cm of walking already counted as distance; the /100 conversion is gone");

        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 500);
        double fiveMetresWalked = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        helper.assertTrue(fiveMetresWalked > floor,
                "five metres of walking moved the survival factor by nothing at all");
        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 100);
        setStat(player, Stats.SPRINT_ONE_CM, 100);
        setStat(player, Stats.CROUCH_ONE_CM, 100);
        setStat(player, Stats.FLY_ONE_CM, 100);
        setStat(player, Stats.CLIMB_ONE_CM, 100);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), fiveMetresWalked,
                "one metre in each of the five distance counters is not worth five metres of "
                        + "walking, so one of them is no longer being added up");

        // --- the distance scale ---
        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 400_000);
        double atFourThousandMetres = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        assertRelative(helper, curveScale(4000.0, atFourThousandMetres, floor, ceiling), 4000.0,
                "the distance scale of the survival curve");

        // --- the play time scale ---
        clearProgress(helper, player);
        setStat(player, Stats.PLAY_TIME, 72_000);
        double atOneHour = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        assertRelative(helper, curveScale(72_000.0, atOneHour, floor, ceiling), 72_000.0,
                "the play time scale of the survival curve");

        // --- max(), in both directions ---
        // 400 m against an hour, then 4000 m against six minutes: whichever half is the weaker one
        // has to be ignored completely, not averaged in and not added on.
        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 40_000);
        double atFourHundredMetres = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        helper.assertTrue(atFourHundredMetres < atOneHour,
                "test setup broken: 400 m is no longer the weaker half against an hour of play");
        setStat(player, Stats.PLAY_TIME, 72_000);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), atOneHour,
                "an hour of play plus 400 m walked did not come out as the better of the two");

        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 400_000);
        setStat(player, Stats.PLAY_TIME, 7_200);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), atFourThousandMetres,
                "4000 m walked plus six minutes of play did not come out as the better of the two");

        // --- the baselines, which is what "since the last death" means ---
        tracker.simplebuilding$setBaseValues(4000, 7_200, 0, 0, 0);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), floor,
                "the survival factor ignores its baseline, so a death no longer costs anything");

        tracker.simplebuilding$setBaseValues(9_999, 999_999, 0, 0, 0);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), floor,
                "a baseline above the current statistics produced a factor of "
                        + TrimMultiplierLogic.calculateSurvivalMultiplier(player)
                        + " instead of the floor; the Math.max(0, ...) clamps are gone and the "
                        + "whole multiplier can now go negative");

        clearProgress(helper, player);
        helper.succeed();
    }

    /**
     * The combat half of the trim multiplier, and the mob-category sorting that feeds it.
     *
     * <p>The three weights are pinned against each other instead of against literals: one hostile
     * kill has to be worth exactly five peaceful ones and exactly twenty points of the raw
     * {@code DAMAGE_TAKEN} statistic. That is the shape of the design - killing things is what
     * moves this factor, farming animals is worth a fifth of it, and getting hurt counts a little -
     * and it stays true through a rebalance of the curve as a whole while catching any one of the
     * three weights drifting on its own.
     *
     * <p>The kill sorting is exercised through the same weights rather than through a separate
     * counter read, because that is how it reaches the player: a zombie has to land in the hostile
     * bucket and a cow in the peaceful one, or the numbers above cannot come out. A bat is included
     * because {@code AMBIENT} is the least obvious of the five peaceful categories, and an armour
     * stand because {@code MISC} has to fall through both branches - without that test the whole
     * category check could be replaced by "everything counts as hostile".
     *
     * <p>Kills are handed over by calling {@code awardKillScore} repeatedly with the same mob. The
     * mixin counts calls, not corpses, so that is the same thing to it and it saves spawning five
     * cows into an 8x8x8 room.
     *
     * <p>What breaks this: a changed weight, a changed scale, a mob category moving between the two
     * buckets or into neither, and the baseline subtraction disappearing - the last one is checked
     * with a baseline above the current tally, the state a statistics rollback leaves behind.
     */
    public static void theCombatFactorWeighsKillsAndDamageByMobCategory(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        SurvivalTracerAccessor tracker = tracker(helper, player);
        DamageSource anyKill = helper.getLevel().damageSources().generic();
        Mob hostile = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob peaceful = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(6, 2, 1));
        Mob ambient = helper.spawnWithNoFreeWill(EntityTypes.BAT, new BlockPos(1, 2, 6));
        ArmorStand neither = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(6, 2, 6));

        clearProgress(helper, player);
        double floor = TrimMultiplierLogic.calculateCombatMultiplier(player);

        // --- one hostile kill sets the yardstick ---
        rebaseKills(player, tracker, 0);
        player.awardKillScore(hostile, anyKill);
        double oneHostileKill = TrimMultiplierLogic.calculateCombatMultiplier(player);
        helper.assertTrue(oneHostileKill > floor,
                "killing a zombie left the combat factor at its floor; either the kill is not being "
                        + "counted or MONSTER has fallen out of the hostile branch");

        // --- five peaceful kills have to be worth exactly the same ---
        rebaseKills(player, tracker, 0);
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), floor,
                "test setup broken: rebasing the kill tally did not put the factor back on its floor");
        for (int i = 0; i < 5; i++) {
            player.awardKillScore(peaceful, anyKill);
        }
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), oneHostileKill,
                "five cows are not worth one zombie; either the 1.0 / 0.2 weighting moved or "
                        + "CREATURE has left the peaceful branch");

        // --- and so has twenty points of damage taken ---
        rebaseKills(player, tracker, 0);
        setStat(player, Stats.DAMAGE_TAKEN, 20);
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), oneHostileKill,
                "twenty points of damage taken are not worth one zombie; the 0.05 weighting moved");

        // --- AMBIENT counts as peaceful, MISC as nothing at all ---
        setStat(player, Stats.DAMAGE_TAKEN, 0);
        rebaseKills(player, tracker, 0);
        for (int i = 0; i < 5; i++) {
            player.awardKillScore(ambient, anyKill);
        }
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), oneHostileKill,
                "five bats are not worth one zombie, so AMBIENT is no longer a peaceful category");

        rebaseKills(player, tracker, 0);
        for (int i = 0; i < 20; i++) {
            player.awardKillScore(neither, anyKill);
        }
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), floor,
                "twenty armour stands moved the combat factor; MISC entities are being counted and "
                        + "every falling block, boat and item frame in the world now feeds the trim "
                        + "multiplier");

        // --- the scale, recovered the same way as the two survival scales ---
        double ceiling = ceilingOf(helper, player, tracker);
        rebaseKills(player, tracker, 0);
        setStat(player, Stats.DAMAGE_TAKEN, 2_000);
        double atScore100 = TrimMultiplierLogic.calculateCombatMultiplier(player);
        assertRelative(helper, curveScale(100.0, atScore100, floor, ceiling), 100.0,
                "the scale of the combat curve");

        // --- the baseline, including the rolled-back case ---
        setStat(player, Stats.DAMAGE_TAKEN, 0);
        rebaseKills(player, tracker, 999_999);
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), floor,
                "a kill baseline above the current tally produced "
                        + TrimMultiplierLogic.calculateCombatMultiplier(player)
                        + " instead of the floor; the clamps are gone");

        clearProgress(helper, player);
        helper.succeed();
    }

    // =====================================================================================
    // (b) THE TRACKER'S PERSISTENCE
    // =====================================================================================

    /**
     * Everything {@code SurvivalTracerMixin} has to remember: across a save and reload, and across
     * a respawn with and without a death in front of it.
     *
     * <p>The save half pins the seven field names <em>literally</em> on purpose. A round trip alone
     * proves nothing about them - renaming a field in the writer and the reader together keeps a
     * round trip green while quietly resetting every existing world to zero, which is the exact
     * failure this is here to catch. So the written tag is inspected key by key, and the read side
     * is then fed a tag assembled by hand from those same names.
     *
     * <p>The respawn half is the core of the survival factor: after a death the baselines have to
     * jump forward to the dead player's own statistics, so that "distance since the last death"
     * really starts at zero, while the kill tally itself is carried over from the old player and
     * immediately becomes the new baseline. After an end portal - {@code alive == true}, the same
     * method with one flag flipped - none of that may happen, or every trip out of the End would
     * cost the player their progress.
     *
     * <p>The distance baseline is the one number here that is not simply copied from somewhere:
     * {@code SurvivalTracerMixin} adds it up itself, in a private copy of
     * {@code getStatTotalDistance} that has nothing to do with the one
     * {@link #theSurvivalFactorTracksDistanceAndTimeSinceTheLastDeath} measures inside
     * {@code TrimMultiplierLogic}. {@link #stampProgress} therefore puts a different value in each
     * of the five distance counters, so that the expected baseline can only come out if that copy
     * sums all five of them and divides each by 100. A counter missing from it leaves the baseline
     * below the statistics it is later subtracted from, and the dead player's progress survives the
     * death - the exact opposite of what this test is about.
     *
     * <p>All players here are the detached kind: not in the level, not in the player list, no
     * connection. {@code restoreFrom} and {@code Entity#load} both rewrite large parts of a player,
     * and doing that to a player the server is tracking is a bad trade for a test. The mixin's own
     * {@code syncTrimData} tail call is null-safe, so nothing is skipped by it.
     *
     * <p>What breaks this: a renamed or dropped NBT field, the save or load injection falling out
     * of the mixin config, the two {@code restoreFrom} branches being swapped, the death branch
     * reading the <em>old</em> player's statistics instead of the new one's, the kill tally being
     * reset rather than carried over, or one of the five distance counters - or the {@code /100} -
     * disappearing from the mixin's own distance sum.
     */
    public static void theTrackerSurvivesTheSaveAndRebasesOnlyOnDeath(GameTestHelper helper) {
        DamageSource anyKill = helper.getLevel().damageSources().generic();
        Mob hostile = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Mob peaceful = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(6, 2, 1));

        // ---------- the save ----------
        ServerPlayer saver = detachedPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        SurvivalTracerAccessor saverTracker = tracker(helper, saver);
        saverTracker.simplebuilding$setBaseValues(11, 22, 33, 44, 55);
        for (int i = 0; i < 3; i++) {
            saver.awardKillScore(hostile, anyKill);
        }
        for (int i = 0; i < 2; i++) {
            saver.awardKillScore(peaceful, anyKill);
        }

        CompoundTag saved = saveOf(helper, saver);
        Optional<CompoundTag> block = saved.getCompound("SimpleBuildingData");
        helper.assertTrue(block.isPresent(),
                "a saved player carries no SimpleBuildingData compound; either the key was renamed - "
                        + "which resets every existing world to zero - or writeSurvivalData is no "
                        + "longer injected");
        CompoundTag data = block.orElse(new CompoundTag());
        assertField(helper, data, "BaseDist", 11);
        assertField(helper, data, "BaseTime", 22);
        assertField(helper, data, "BaseHostile", 33);
        assertField(helper, data, "BasePassive", 44);
        assertField(helper, data, "BaseDamage", 55);
        assertField(helper, data, "TotalHostile", 3);
        assertField(helper, data, "TotalPassive", 2);

        // ---------- the load ----------
        // A second, fresh player, so that every number arriving on the other side really came out
        // of the tag. Its own counters all start at zero, and the tag says otherwise for each one.
        ServerPlayer loader = detachedPlayer(helper, new Vec3(4.5, 2.0, 2.5));
        SurvivalTracerAccessor loaderTracker = tracker(helper, loader);
        CompoundTag handWritten = saved.copy();
        CompoundTag replacement = new CompoundTag();
        replacement.putInt("BaseDist", 101);
        replacement.putInt("BaseTime", 102);
        replacement.putInt("BaseHostile", 103);
        replacement.putInt("BasePassive", 104);
        replacement.putInt("BaseDamage", 105);
        replacement.putInt("TotalHostile", 106);
        replacement.putInt("TotalPassive", 107);
        handWritten.put("SimpleBuildingData", replacement);
        loader.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), handWritten));

        helper.assertValueEqual(loaderTracker.simplebuilding$getBaseDistance(), 101, "BaseDist after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getBaseTime(), 102, "BaseTime after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getBaseHostileKills(), 103, "BaseHostile after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getBasePassiveKills(), 104, "BasePassive after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getBaseDamageTaken(), 105, "BaseDamage after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getCurrentHostileKills(), 106, "TotalHostile after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getCurrentPassiveKills(), 107, "TotalPassive after loading");

        // ---------- the respawn ----------
        ServerPlayer dead = detachedPlayer(helper, new Vec3(2.5, 2.0, 4.5));
        SurvivalTracerAccessor deadTracker = tracker(helper, dead);
        deadTracker.simplebuilding$setBaseValues(11, 22, 33, 44, 55);
        for (int i = 0; i < 7; i++) {
            dead.awardKillScore(hostile, anyKill);
        }
        for (int i = 0; i < 4; i++) {
            dead.awardKillScore(peaceful, anyKill);
        }

        // An end portal is not a death: every baseline has to survive it untouched.
        ServerPlayer travelled = detachedPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        SurvivalTracerAccessor travelledTracker = tracker(helper, travelled);
        stampProgress(travelled);
        travelled.restoreFrom(dead, true);
        helper.assertValueEqual(travelledTracker.simplebuilding$getBaseDistance(), 11,
                "the distance baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getBaseTime(), 22,
                "the time baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getBaseHostileKills(), 33,
                "the hostile kill baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getBasePassiveKills(), 44,
                "the peaceful kill baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getBaseDamageTaken(), 55,
                "the damage baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getCurrentHostileKills(), 7,
                "the hostile kill tally did not follow the player through the portal");
        helper.assertValueEqual(travelledTracker.simplebuilding$getCurrentPassiveKills(), 4,
                "the peaceful kill tally did not follow the player through the portal");

        // A death: the baselines move to the respawning player's own statistics, and the kill
        // tally is carried over and becomes its own baseline in the same step.
        ServerPlayer respawned = detachedPlayer(helper, new Vec3(2.5, 2.0, 6.5));
        SurvivalTracerAccessor respawnedTracker = tracker(helper, respawned);
        stampProgress(respawned);
        respawned.restoreFrom(dead, false);
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBaseDistance(), STAMPED_DISTANCE,
                "after a death the distance baseline has to be the respawning player's own distance "
                        + "in metres, summed over all five counters and each divided by 100");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBaseTime(), 5678,
                "after a death the time baseline has to be the respawning player's own play time");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBaseDamageTaken(), 91,
                "after a death the damage baseline has to be the respawning player's own damage taken");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getCurrentHostileKills(), 7,
                "the hostile kill tally was not carried over into the new life");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getCurrentPassiveKills(), 4,
                "the peaceful kill tally was not carried over into the new life");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBaseHostileKills(), 7,
                "the carried-over hostile kills were not made the new baseline, so they keep paying "
                        + "out after the death that should have cost them");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBasePassiveKills(), 4,
                "the carried-over peaceful kills were not made the new baseline");

        // ...which is the whole point: both progress factors are back at their floor. The floors
        // are read off the save player, put back into the "nothing since the baseline" state, so
        // that a rebalanced floor moves both sides of this comparison together.
        clearProgress(helper, saver);
        rebaseKills(saver, saverTracker, 0);
        double survivalFloor = TrimMultiplierLogic.calculateSurvivalMultiplier(saver);
        double combatFloor = TrimMultiplierLogic.calculateCombatMultiplier(saver);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(respawned), survivalFloor,
                "the survival factor did not fall back to its floor after a death");
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(respawned), combatFloor,
                "the combat factor did not fall back to its floor after a death");

        helper.succeed();
    }

    // =====================================================================================
    // (c) WHAT THE MIXINS ACTUALLY DELIVER
    // =====================================================================================

    /**
     * The three deliveries of {@code PlayerEntityMixin} that no test could reach before - walking
     * speed, hunger and experience - each together with the guard that decides when it applies.
     *
     * <p>{@code TrimEffectTests#trimBonusesReachThePlayerThroughTheMixins} already shows that
     * {@code getSpeed} picks the bolt bonus up while standing. What it cannot show is the condition
     * around it: the bonus is meant to be a <em>land</em> bonus and has to stand down while the
     * wearer is swimming or gliding. Both states are checked with a bolt trim and nothing else on,
     * so the expected reading is the player's own untrimmed speed no matter how the swim bonus is
     * wired - see the Tide defect in the class javadoc.
     *
     * <p>That swim bonus is measured where it does work, on an armour stand, and there both halves
     * are real: the multiplier only applies while {@code isSwimming()}, and only to a wearer that
     * has a tide trim on.
     *
     * <p>Hunger and experience both ride on {@code @ModifyVariable} at the head of a vanilla
     * method, which is the kind of injection that fails silently - the method still runs, the
     * argument is simply never bent. Exhaustion is read back out of {@code FoodData}'s own save
     * data because it has no getter, and experience through {@code totalExperience}, which is what
     * {@code giveExperiencePoints} writes the modified amount into.
     *
     * <p>The negative experience case is the one worth spelling out. {@code ServerPlayer} already
     * short-circuits an amount of exactly zero, so passing zero proves nothing; a negative amount
     * reaches the mixin, and without its {@code experience <= 0} guard a reduction of four points
     * would be scaled up into a reduction of seven.
     *
     * <p>The player has to be made non-invulnerable first, because {@code Player#causeFoodExhaustion}
     * returns immediately for an invulnerable player and the creative mock is one.
     *
     * <p>What breaks this: any of the three injections leaving the mixin config, the swim/glide
     * condition on the walking bonus, the sprint condition on the exhaustion reduction, the
     * rounding of the experience gain, or the guard against non-positive gains.
     */
    public static void thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            // causeFoodExhaustion is a no-op for an invulnerable player, and the mock is creative.
            player.getAbilities().invulnerable = false;
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimPattern> bolt = pattern(helper, TrimPatterns.BOLT);
            Holder<TrimPattern> tide = pattern(helper, TrimPatterns.TIDE);
            Holder<TrimPattern> wayfinder = pattern(helper, TrimPatterns.WAYFINDER);
            Holder<TrimPattern> raiser = pattern(helper, TrimPatterns.RAISER);
            pinProgressMultiplier(helper, player, 1.0);

            // --- walking speed, and the two states it has to stand down in ---
            bare(player);
            float bareSpeed = player.getSpeed();
            helper.assertTrue(bareSpeed > 0.0F,
                    "the mock player's movement speed is " + bareSpeed + ", so multiplying it could "
                            + "not be detected either way");
            wear(player, copper, bolt, 4);
            float landMultiplier = TrimEffectUtil.getLandSpeedMultiplier(player);
            helper.assertTrue(landMultiplier > 1.0F,
                    "test setup broken: a full bolt set is worth a land speed multiplier of "
                            + landMultiplier);
            assertClose(helper, player.getSpeed(), bareSpeed * landMultiplier,
                    "Player.getSpeed did not pick up the bolt trim bonus");

            player.setSwimming(true);
            assertClose(helper, player.getSpeed(), bareSpeed,
                    "the land speed bonus of a bolt trim kept running while the player was swimming");
            player.setSwimming(false);
            assertClose(helper, player.getSpeed(), bareSpeed * landMultiplier,
                    "the land speed bonus did not come back once the player stopped swimming");

            player.startFallFlying();
            assertClose(helper, player.getSpeed(), bareSpeed,
                    "the land speed bonus of a bolt trim kept running under an elytra");
            player.stopFallFlying();
            assertClose(helper, player.getSpeed(), bareSpeed * landMultiplier,
                    "the land speed bonus did not come back once the player stopped gliding");

            // --- the swim bonus, on the only kind of wearer it can reach (see Known defects) ---
            ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
            bare(stand);
            stand.setSpeed(0.25F);
            assertClose(helper, stand.getSpeed(), 0.25,
                    "test setup broken: LivingEntity.getSpeed no longer reports what setSpeed was given");
            wear(stand, copper, tide, 4);
            assertClose(helper, stand.getSpeed(), 0.25,
                    "the tide bonus applied to a wearer that was not swimming");
            stand.setSwimming(true);
            float swimMultiplier = TrimEffectUtil.getSwimSpeedMultiplier(stand);
            helper.assertTrue(swimMultiplier > 1.0F,
                    "test setup broken: a full tide set is worth a swim multiplier of " + swimMultiplier);
            assertClose(helper, stand.getSpeed(), 0.25 * swimMultiplier,
                    "LivingEntity.getSpeed did not pick up the tide trim bonus while swimming");
            bare(stand);
            assertClose(helper, stand.getSpeed(), 0.25,
                    "an untrimmed swimmer was handed a speed bonus");
            stand.setSwimming(false);

            // --- exhaustion, and the sprint condition on it ---
            wear(player, copper, wayfinder, 4);
            float reduction = TrimEffectUtil.getExhaustionReduction(player);
            helper.assertTrue(reduction > 0.0F && reduction < 1.0F,
                    "test setup broken: a full wayfinder set is worth an exhaustion reduction of "
                            + reduction + ", which cannot be told apart from no reduction at all");

            resetFood(helper, player);
            player.setSprinting(false);
            player.causeFoodExhaustion(EXHAUSTION_UNIT);
            assertClose(helper, exhaustionOf(helper, player), EXHAUSTION_UNIT,
                    "a standing player's exhaustion was reduced; the wayfinder bonus is meant to pay "
                            + "for sprinting only");

            resetFood(helper, player);
            player.setSprinting(true);
            player.causeFoodExhaustion(EXHAUSTION_UNIT);
            assertClose(helper, exhaustionOf(helper, player), EXHAUSTION_UNIT * (1.0F - reduction),
                    "a sprinting player in a full wayfinder set paid the undiscounted exhaustion, so "
                            + "the reduction is computed but never delivered");

            bare(player);
            resetFood(helper, player);
            player.causeFoodExhaustion(EXHAUSTION_UNIT);
            assertClose(helper, exhaustionOf(helper, player), EXHAUSTION_UNIT,
                    "an untrimmed sprinter's exhaustion was reduced");
            player.setSprinting(false);

            // --- experience: the guard first, while the multiplier is provably above 1 ---
            wear(player, copper, raiser, 4);
            player.experienceLevel = 10;
            player.experienceProgress = 0.9F;
            player.totalExperience = 100;
            helper.assertTrue(TrimEffectUtil.getXPMultiplier(player) > 1.0F,
                    "test setup broken: the experience multiplier is not above 1, so the guard below "
                            + "would hold with or without the mixin");
            int beforeLoss = player.totalExperience;
            player.giveExperiencePoints(-4);
            helper.assertValueEqual(player.totalExperience - beforeLoss, -4,
                    "a negative experience change was run through the trim multiplier; without the "
                            + "experience <= 0 guard every penalty grows with the player's trims");

            // --- and the delivery, rounded, in both directions ---
            assertExperienceGain(helper, player, 3);
            assertExperienceGain(helper, player, 5);

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    /**
     * A real hit on a real player, measured on the health bar rather than by calling
     * {@code TrimEffectUtil.modifyDamage} directly.
     *
     * <p>Every damage assertion in {@link TrimEffectTests} calls that method itself, which means
     * all of them would still pass with {@code LivingEntityMixin} deleted from the mixin config:
     * the reductions would be computed correctly and nothing in the game would ever ask for them.
     * This is the test that notices.
     *
     * <p>{@code minecraft:fly_into_wall} is used for the same reason
     * {@link ProtectionAndRangeTests#kineticProtectionActuallyReducesTheDamageThePlayerTakes} uses
     * it: it is in {@code minecraft:bypasses_armor} and in none of {@code bypasses_effects},
     * {@code bypasses_enchantments} or {@code bypasses_invulnerability}, so vanilla's own armour
     * arithmetic - which is not linear in the incoming amount - stays out of the measurement and
     * the raw hit arrives at the mixin unchanged. It is also in none of the damage type tags
     * {@code modifyDamage} branches on, so what is left is the unconditional ward reduction plus
     * the enderite material, i.e. exactly what the worn set is for.
     *
     * <p>The expected loss is read out of {@code modifyDamage} for the same player at the same
     * moment rather than written down, so this test says "the mixin delivers what the utility
     * computes" and leaves the question of what it should compute to {@link TrimEffectTests}.
     *
     * <p>Making the mock player damageable takes the same three switches as in
     * {@link ProtectionAndRangeTests}, and its javadoc explains why the third one - the player
     * loaded packet - is not optional.
     *
     * <p>What breaks this: the {@code hurtServer} injection leaving the mixin config, its
     * {@code ordinal = 0} slipping onto the wrong argument, or the trim benefit switch no longer
     * reaching the damage path.
     */
    public static void everyServerSideHitRunsThroughTheTrimDamageModifier(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            player.getAbilities().invulnerable = false;
            player.setInvulnerable(false);
            // Without this ServerPlayer#isInvulnerableTo keeps returning true for 60 ticks.
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            player.setAbsorptionAmount(0.0F);

            Holder<TrimMaterial> enderite = material(helper, ModTrimMaterials.ENDERITE);
            Holder<TrimPattern> ward = pattern(helper, TrimPatterns.WARD);
            DamageSource armourPiercing = helper.getLevel().damageSources().flyIntoWall();
            pinProgressMultiplier(helper, player, 1.0);

            // --- the control: an untrimmed set loses the full hit ---
            wear(player, enderite, ward, 4);
            TrimBenefitUser gate = (TrimBenefitUser) player;
            gate.simplebuilding$setTrimBenefitsEnabled(false);
            float unprotected = damageTaken(helper, player, armourPiercing);
            helper.assertTrue(unprotected > 0.0F,
                    "the mock player took no damage at all, so this test proves nothing about the "
                            + "mixin - the creative mock is invulnerable again");
            assertClose(helper, unprotected, HIT_DAMAGE,
                    "fly_into_wall bypasses armour, so a player whose trim benefits are switched off "
                            + "has to lose the full hit");

            // --- and with the benefits on, exactly what modifyDamage says ---
            gate.simplebuilding$setTrimBenefitsEnabled(true);
            float expected = TrimEffectUtil.modifyDamage(player, HIT_DAMAGE, armourPiercing);
            helper.assertTrue(expected < HIT_DAMAGE - 0.5F,
                    "test setup broken: a full enderite ward set only takes " + (HIT_DAMAGE - expected)
                            + " points off a " + HIT_DAMAGE + " point hit, which is too little to "
                            + "measure on the health bar");
            float protectedLoss = damageTaken(helper, player, armourPiercing);
            assertClose(helper, protectedLoss, expected,
                    "the player lost " + protectedLoss + " health where TrimEffectUtil.modifyDamage "
                            + "says " + expected + "; the reduction is being computed but hurtServer "
                            + "is not going through it");

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    /**
     * The two remaining {@code LivingEntityMixin} deliveries that are not about damage: the coast
     * trim holding the air supply under water, and the silence trim lowering how visible the wearer
     * is to the mobs looking for them.
     *
     * <p>Air is the interesting one because the mixin rolls a die for it. With a full enderite
     * coast set and the progress factor pinned to 1.0 the chance comes out above 1.0, so
     * {@code nextFloat() < chance} is always true and the case is deterministic - the setup asserts
     * that before it measures anything. The whole air path is then driven for real: water is placed
     * around the player's eyes and {@code baseTick} is called, which is where vanilla decides to
     * spend a point of air. The control run without the trim has to lose that point, otherwise the
     * measurement would be about the player not being submerged rather than about the trim.
     *
     * <p>{@code baseTick} is used rather than a full tick because the air logic sits in
     * {@code LivingEntity#baseTick} directly, and because it does not move the player - the water
     * column only has to be right for the one call.
     *
     * <p>The visibility half is measured against the same player's own untrimmed reading and
     * against {@code getStealthMultiplier}, so a vanilla change to the base visibility cannot turn
     * it into a false failure. The inert shaper pattern is worn for the control run so that the
     * armour itself is identical in both readings.
     *
     * <p>What breaks this: either injection leaving the mixin config, the coast chance no longer
     * keeping the old air value, the {@code mult < 1.0} guard on the visibility injection widening
     * to make untrimmed players more visible, or the air saving becoming unconditional.
     */
    public static void coastHoldsTheAirSupplyAndSilenceLowersTheVisibility(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            // The drowning branch is skipped entirely for an invulnerable player.
            player.getAbilities().invulnerable = false;
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimMaterial> enderite = material(helper, ModTrimMaterials.ENDERITE);
            Holder<TrimPattern> coast = pattern(helper, TrimPatterns.COAST);
            Holder<TrimPattern> silence = pattern(helper, TrimPatterns.SILENCE);
            Holder<TrimPattern> blank = pattern(helper, TrimPatterns.SHAPER);
            pinProgressMultiplier(helper, player, 1.0);

            // --- a water column around the standing player's eyes ---
            helper.setBlock(new BlockPos(3, 2, 3), Blocks.WATER);
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.WATER);
            helper.setBlock(new BlockPos(3, 4, 3), Blocks.WATER);

            bare(player);
            player.setAirSupply(START_AIR);
            player.baseTick();
            helper.assertTrue(player.isEyeInFluid(FluidTags.WATER),
                    "the mock player's eyes are not in water, so nothing below is about the coast trim");
            helper.assertValueEqual(player.getAirSupply(), START_AIR - 1,
                    "an untrimmed submerged player did not spend a point of air, so the measurement "
                            + "below would pass with the mixin deleted");

            wear(player, enderite, coast, 4);
            float chance = TrimEffectUtil.getAirSaveChance(player);
            helper.assertTrue(chance >= 1.0F,
                    "test setup broken: the coast air chance is " + chance + ", so the random draw is "
                            + "not switched off and this test would be flaky");
            player.setAirSupply(START_AIR);
            player.baseTick();
            helper.assertValueEqual(player.getAirSupply(), START_AIR,
                    "a full coast set at a guaranteed chance still lost air; decreaseAirSupply is no "
                            + "longer going through the mixin");

            // --- visibility: same armour in both readings, only the pattern differs ---
            wear(player, copper, blank, 4);
            double plainVisibility = player.getVisibilityPercent(null);
            helper.assertTrue(plainVisibility > 0.0,
                    "the wearer is already invisible to everything at " + plainVisibility
                            + ", so multiplying it could not be detected");
            wear(player, copper, silence, 4);
            float stealth = TrimEffectUtil.getStealthMultiplier(player);
            helper.assertTrue(stealth < 1.0F && stealth > 0.0F,
                    "test setup broken: a full silence set is worth a stealth factor of " + stealth);
            assertClose(helper, player.getVisibilityPercent(null), plainVisibility * stealth,
                    "LivingEntity.getVisibilityPercent did not pick the silence trim up; mobs still "
                            + "spot the wearer from just as far away");
            wear(player, copper, blank, 4);
            assertClose(helper, player.getVisibilityPercent(null), plainVisibility,
                    "an untrimmed wearer's visibility was modified as well");

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    /**
     * The two effects {@code LivingEntityMixin} runs on a clock: the rib trim working against a
     * wither every 20 ticks, and the amethyst material healing every 200.
     *
     * <p>Two ticks cannot pin a period. Tick 200 alone is a multiple of 4, 5, 8, 10, 20, 25, 40,
     * 50, 100 and 200, so a rib branch slowed down to every 40th or every 200th tick would still
     * fire on it and a test that only knew 200 and one neighbour would stay green. Each cadence is
     * therefore driven on two ticks it has to fire on and two or three it has to stay quiet on,
     * picked so that exactly one period survives all of them - 20 for the rib branch, 200 for the
     * amethyst one. The arithmetic is spelled out at both sites.
     *
     * <p>The cadence is reached by setting {@code tickCount} directly and then pushing exactly one
     * tick through {@code connection.tick()} - the same route
     * {@link ProtectionAndRangeTests#rangeAddsBlockInteractionReachInTheMainHandOnly} uses, because
     * the gametest server does not pump a mock player's connection and
     * {@code ServerPlayer#doTick} is what leads into {@code LivingEntity#tick}. A test that instead
     * waited 200 real ticks would cost ten seconds and still not prove the cadence.
     *
     * <p>The rib trim has two branches and they are told apart by the remaining duration of the
     * effect. The setup pins the reduction between the two durations used here, so "short" really
     * is short enough to be cleared and "long" really is not; both durations are chosen so that
     * they are not multiples of 40 and the wither therefore deals no damage of its own during the
     * measured tick, which would otherwise land on the same health bar the healing branch is read
     * from.
     *
     * <p>Healing is always measured as a difference against the same tick run without the trim.
     * A gametest world on peaceful heals a hurt player once every 20 ticks on its own, and that
     * would sit right on top of both cadences; taking the difference cancels it whatever the
     * server's difficulty happens to be.
     *
     * <p>The amethyst chance is pushed above 1.0 by pinning the progress factor, so the die is
     * switched off and the test is deterministic - asserted before it is relied on.
     *
     * <p>Deliberately not asserted: the {@code getHealth() < getMaxHealth()} guard on the amethyst
     * branch. Removing it changes nothing observable, because {@code heal} on a wearer at full
     * health does nothing either way.
     *
     * <p>What breaks this: either injection leaving the mixin config, a period changed in either
     * direction - stretched to 40, 100 or 200 ticks, or shortened to 2, 4, 5 or 10 - the two wither
     * branches swapping, or the healing amounts changing.
     */
    public static void theTickDrivenTrimEffectsFireOnTheirOwnCadence(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            // The same packet a real client sends; it also stops the connection's load timeout from
            // counting down while this test pushes ticks through it by hand.
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            // 17 food and no saturation: one point below the slow regeneration branch of
            // FoodData#tick and far above the starvation one, so the food bar cannot move the
            // health bar during a measured tick. The peaceful auto heal in Player#tick sits on the
            // same 20 tick boundary as the rib branch and is cancelled by measuring differences.
            player.getFoodData().setFoodLevel(17);
            player.getFoodData().setSaturation(0.0F);

            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimMaterial> amethyst = material(helper, TrimMaterials.AMETHYST);
            Holder<TrimPattern> rib = pattern(helper, TrimPatterns.RIB);
            Holder<TrimPattern> blank = pattern(helper, TrimPatterns.SHAPER);
            pinProgressMultiplier(helper, player, 1.5);

            // --- the rib trim, at a strength that can clear the short wither but not the long one ---
            wear(player, copper, rib, 4);
            int reduction = TrimEffectUtil.getWitherReductionAmount(player);
            helper.assertTrue(reduction >= SHORT_WITHER && reduction < LONG_WITHER,
                    "test setup broken: the rib trim is worth " + reduction + " ticks of wither, which "
                            + "does not sit between the " + SHORT_WITHER + " and " + LONG_WITHER
                            + " tick effects the two branches are told apart with");

            // Two ticks it has to fire on, three it has to stay quiet on. A period that still
            // fires on 200 and on 220 has to divide both, i.e. divide 20; 204 then rules out 4,
            // 206 rules out 2 and 210 rules out 5 and 10. Only 20 itself is left.
            for (int firesAt : new int[]{200, 220}) {
                helper.assertTrue(ribClearsWitherAt(player, firesAt),
                        "a wither shorter than the rib trim's reduction survived tick " + firesAt
                                + ", which is on the 20 tick cadence; either the cleansing branch is "
                                + "gone or its period has been stretched");
            }
            for (int quietAt : new int[]{204, 206, 210}) {
                helper.assertFalse(ribClearsWitherAt(player, quietAt),
                        "the rib trim cleansed a wither on tick " + quietAt + ", which is not a "
                                + "multiple of 20, so it now runs on a shorter period than it is "
                                + "meant to");
            }

            // --- the other branch: a wither too long to clear is healed against instead ---
            float healedWithoutRib = witherTickHealing(helper, player, copper, blank);
            float healedWithRib = witherTickHealing(helper, player, copper, rib);
            assertClose(helper, healedWithRib - healedWithoutRib, 0.5,
                    "a rib trim facing a wither it cannot clear healed " + healedWithRib
                            + " where an untrimmed wearer healed " + healedWithoutRib
                            + "; the half heart of compensation is gone");

            // --- the amethyst material, on its own much slower clock ---
            wear(player, amethyst, blank, 4);
            float healChance = TrimEffectUtil.getAmethystHealChance(player);
            helper.assertTrue(healChance >= 1.0F,
                    "test setup broken: the amethyst heal chance is " + healChance + ", so the random "
                            + "draw is not switched off and this test would be flaky");

            // Same idea as above. A period that fires on 200 and 400 divides 200; not firing on
            // 100 leaves 8, 40 and 200, and not firing on 120 - a multiple of both 8 and 40 - leaves
            // 200 alone. Both quiet ticks are multiples of 20 so that the rib branch and the
            // peaceful auto heal run in every one of the four measurements and cancel out.
            for (int firesAt : new int[]{200, 400}) {
                assertClose(helper, amethystHealBonus(helper, player, amethyst, copper, blank, firesAt), 1.0,
                        "the extra healing a full amethyst set gives on tick " + firesAt
                                + ", which is on its 200 tick cadence - nothing here means either the "
                                + "heart is gone or the period has been stretched");
            }
            for (int quietAt : new int[]{100, 120}) {
                assertClose(helper, amethystHealBonus(helper, player, amethyst, copper, blank, quietAt), 0.0,
                        "the extra healing a full amethyst set gives on tick " + quietAt
                                + ", a multiple of 20 but not of 200 - anything here means the heal now "
                                + "pays out several times as often as it is meant to");
            }

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // (d) THE DATA THE TRIMS ARE MADE OF
    // =====================================================================================

    /**
     * The three mod trim materials as data: their colour, the two vanilla item tags that decide
     * whether the smithing table will even look at them, and the material each ingredient actually
     * hands over once it does.
     *
     * <p>The tags are checked the way a player meets them - by assembling a trim at a real smithing
     * table. Vanilla's {@code smithing_trim} recipe takes its base from
     * {@code #minecraft:trimmable_armor} and its addition from {@code #minecraft:trim_materials},
     * and its {@code applyTrim} then reads the material straight off the addition's
     * {@code provides_trim_material} component, returning an empty stack when that component is
     * missing (checked in the 26.2 jar). Smithing every ingredient and reading the material back out
     * of the result therefore covers the tag entry, the component and the registration in one go -
     * and catches the two failures that a bare "something came out" check cannot: an ingredient
     * losing its {@code trimMaterial(...)}, which makes it unusable as a trim for good, and two of
     * them being swapped, which hands the player the wrong colour <em>and</em> the wrong set of
     * {@code TrimEffectUtil} bonuses, since those are looked up by the material's registry path. All
     * four armour pieces are smithed for the same reason: the tag entry that could go missing is per
     * item.
     *
     * <p>The two refusals next to that prove neither slot is being waved through, and the vanilla
     * control run is the guard against {@code "replace": true}. Both generated tag files add to a
     * vanilla tag; if either ever started replacing it, every vanilla armour piece and every vanilla
     * trim material would quietly stop being trimmable, and nothing in the mod's own data would look
     * wrong.
     *
     * <p>The colours are compared against a recorded run of {@code ModTrimMaterials.bootstrap} -
     * the method datagen calls to produce the JSON the registry is loaded from - so the comparison
     * is a real staleness check: edit a colour in {@code bootstrap} and forget datagen, and the
     * registry still carries the old one. The dead {@code *_HOLDER} copies next to it are
     * deliberately not consulted (see the class javadoc). The distinctness check is what catches a
     * copy-paste between the three.
     *
     * <p>What breaks this: a dropped tag entry, a tag file gaining {@code "replace": true}, an
     * ingredient losing or swapping its {@code trimMaterial(...)}, a material losing the
     * {@code setStyle} on its description - which renders it in plain white, indistinguishable from
     * a vanilla trim - two materials ending up the same colour, or datagen not having been re-run
     * after a colour change.
     */
    public static void theThreeTrimMaterialsKeepTheirColoursAndTheirTags(GameTestHelper helper) {
        // --- the colours, against the bootstrap datagen turns into the JSON ---
        Map<ResourceKey<TrimMaterial>, TrimMaterial> declared = bootstrapped();
        TextColor astralit = assertColourIsCurrent(helper, declared, ModTrimMaterials.ASTRALIT, "astralit");
        TextColor nihilith = assertColourIsCurrent(helper, declared, ModTrimMaterials.NIHILITH, "nihilith");
        TextColor enderite = assertColourIsCurrent(helper, declared, ModTrimMaterials.ENDERITE, "enderite");
        helper.assertTrue(astralit.getValue() != nihilith.getValue()
                        && astralit.getValue() != enderite.getValue()
                        && nihilith.getValue() != enderite.getValue(),
                "two of the three trim materials share a colour (" + astralit + ", " + nihilith + ", "
                        + enderite + "), so they cannot be told apart on a piece of armour");

        // --- the tags and the material components, through the recipe that reads them ---
        SmithingMenu table = smithingTable(helper);
        for (Item armour : new Item[]{ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE,
                ModItems.ENDERITE_LEGGINGS, ModItems.ENDERITE_BOOTS}) {
            assertTrims(helper, table, armour, ModItems.ASTRALIT_DUST, ModTrimMaterials.ASTRALIT);
        }
        assertTrims(helper, table, ModItems.ENDERITE_CHESTPLATE, ModItems.NIHILITH_SHARD,
                ModTrimMaterials.NIHILITH);
        assertTrims(helper, table, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_INGOT,
                ModTrimMaterials.ENDERITE);

        // --- neither ingredient is simply being waved through ---
        ItemStack notArmour = smith(table, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                new ItemStack(ModItems.ENDERITE_INGOT), new ItemStack(ModItems.ASTRALIT_DUST));
        helper.assertTrue(notArmour.isEmpty(),
                "an enderite ingot was accepted as the base of a trim, so the base slot is no longer "
                        + "filtered and the four armour pieces smithed above prove nothing");
        ItemStack notMaterial = smith(table, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                new ItemStack(ModItems.ENDERITE_CHESTPLATE), new ItemStack(Items.STICK));
        helper.assertTrue(notMaterial.isEmpty(),
                "a stick was accepted as a trim material, so the addition slot is no longer filtered");

        // --- and the vanilla entries survived, i.e. neither tag file replaces its vanilla one ---
        ItemStack vanillaTrim = smith(table, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND));
        helper.assertTrue(vanillaTrim.get(DataComponents.TRIM) != null,
                "a diamond chestplate can no longer be trimmed with a diamond; one of the two mod tag "
                        + "files has gained \"replace\": true and dropped the whole vanilla list");

        helper.succeed();
    }

    /**
     * {@code /simplebuilding config setTrimMultiplier} - the one switch that scales every trim
     * bonus in the game at runtime - driven through the server's own command dispatcher.
     *
     * <p>Three separate things are checked, and each fails differently. The permission gate: a
     * player who is not an operator must not find the command at all, and the value must not move.
     * The range: the argument is declared as {@code 0.0 .. maxMultiplierLimit}, so the limit itself
     * has to be accepted while anything past it is a parse error that leaves the config untouched -
     * without that a single command could multiply every trim bonus by an arbitrary number. And the
     * effect: an accepted value has to actually land in {@code trimBenefitBaseMultiplier}, which is
     * what everything else in this file reads.
     *
     * <p>The bound is taken from {@code SimplebuildingConfig.maxMultiplierLimit} rather than written
     * out, because the command builds its argument type from that same field; a test that spelled
     * the number out would only be checking that two copies of it agree.
     *
     * <p>The command is registered per loader - see the class javadoc - so a run only ever covers
     * the copy the current loader installed. If a loader stops registering it at all, the first
     * command that is meant to succeed fails with the message below rather than silently passing as
     * "correctly refused", which is why the permitted cases come after the refused one.
     *
     * <p>What breaks this: the {@code requires} gate disappearing or inverting, the argument bounds
     * widening, the executor no longer writing the config, or the whole registration being dropped
     * on one loader.
     */
    public static void theTrimMultiplierCommandGuardsItsRangeAndItsPermission(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        ServerPlayer player = mockPlayer(helper);
        PlayerList players = helper.getLevel().getServer().getPlayerList();
        CommandDispatcher<CommandSourceStack> dispatcher =
                helper.getLevel().getServer().getCommands().getDispatcher();
        boolean wasOperator = players.isOp(player.nameAndId());
        try {
            helper.assertFalse(wasOperator,
                    "the mock player is already an operator, so the permission check below would pass "
                            + "with the requires() gate deleted");

            // --- without operator rights the subtree is not even reachable ---
            SimplebuildingConfig.trimBenefitBaseMultiplier = 1.0;
            assertRefused(helper, dispatcher, player, "simplebuilding config setTrimMultiplier 3.5",
                    "a player who is not an operator");
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, 1.0,
                    "a player who is not an operator changed the trim multiplier anyway");

            players.op(player.nameAndId());
            helper.assertTrue(players.isOp(player.nameAndId()),
                    "the mock player could not be made an operator, so the rest of this test would "
                            + "only be re-checking the refusal above");

            // --- an accepted value reaches the config ---
            run(helper, dispatcher, player, "simplebuilding config setTrimMultiplier 3.5");
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, 3.5,
                    "setTrimMultiplier reported success but the configured base did not move");

            // --- the declared limit is inclusive ---
            double limit = SimplebuildingConfig.maxMultiplierLimit;
            run(helper, dispatcher, player, "simplebuilding config setTrimMultiplier " + limit);
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, limit,
                    "the configured upper limit was refused by the command that declares it");

            // --- and both ends of the range are closed ---
            assertRefused(helper, dispatcher, player,
                    "simplebuilding config setTrimMultiplier " + (limit + 0.5), "a value above the limit");
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, limit,
                    "a value above the limit was refused but written anyway");
            assertRefused(helper, dispatcher, player,
                    "simplebuilding config setTrimMultiplier -0.5", "a negative value");
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, limit,
                    "a negative value was refused but written anyway");

            // --- the getter reports and changes nothing ---
            helper.assertValueEqual(run(helper, dispatcher, player, "simplebuilding config getTrimMultiplier"),
                    1, "return code of getTrimMultiplier");
            assertClose(helper, SimplebuildingConfig.trimBenefitBaseMultiplier, limit,
                    "getTrimMultiplier changed the value it was only meant to report");

            helper.succeed();
        } finally {
            if (!wasOperator) {
                players.deop(player.nameAndId());
            }
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // HELPERS: PLAYERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the player
        // list non-empty and the gametest server then stalls on shutdown.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * A {@link ServerPlayer} that is never placed in the level or the player list, built by
     * vanilla's {@code GameTestHelper#makeMockServerPlayer}. It has no {@code connection}, so
     * anything that sends it a packet would throw - and equally it needs no cleanup and cannot
     * collide with another player over a UUID, which is what the save and respawn test needs.
     */
    private static ServerPlayer detachedPlayer(GameTestHelper helper, Vec3 relativePos) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException(
                    "GameTestHelper#makeMockServerPlayer no longer returns a ServerPlayer but a "
                            + raw.getClass().getName());
        }
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static SurvivalTracerAccessor tracker(GameTestHelper helper, ServerPlayer player) {
        if (!(player instanceof SurvivalTracerAccessor accessor)) {
            throw helper.assertionException(
                    "SurvivalTracerMixin is not applied to ServerPlayer, so nothing in this mod is "
                            + "counting distance, time or kills any more");
        }
        return accessor;
    }

    /**
     * Bends the configured base multiplier until {@link TrimMultiplierLogic} reports exactly
     * {@code target} for this player. Same approach as {@code TrimEffectTests}: the three progress
     * factors of a fresh mock player all sit on their floor and cannot usefully be raised from a
     * test, so the configured base is the only handle, and because the multiplier is a plain
     * product, scaling the base scales the result.
     */
    private static void pinProgressMultiplier(GameTestHelper helper, ServerPlayer player, double target) {
        SimplebuildingConfig.trimBenefitBaseMultiplier = 1.0;
        double perUnitOfBase = TrimMultiplierLogic.getMultiplier(player);
        helper.assertTrue(perUnitOfBase > 0.0,
                "the trim multiplier collapsed to " + perUnitOfBase + " at base 1.0, so it can no "
                        + "longer be pinned to a known value");
        SimplebuildingConfig.trimBenefitBaseMultiplier = target / perUnitOfBase;
        double actual = TrimMultiplierLogic.getMultiplier(player);
        helper.assertTrue(Math.abs(actual - target) < 1.0e-6,
                "the configured base no longer scales the multiplier: wanted " + target
                        + " but got " + actual);
    }

    // =====================================================================================
    // HELPERS: STATISTICS AND THE TRACKER
    // =====================================================================================

    private static void setStat(ServerPlayer player, Identifier stat, int value) {
        player.getStats().setValue(player, Stats.CUSTOM.get(stat), value);
    }

    private static int stat(ServerPlayer player, Identifier id) {
        return player.getStats().getValue(Stats.CUSTOM.get(id));
    }

    /** Puts every counter the two progress factors read back to zero, statistics and baselines alike. */
    private static void clearProgress(GameTestHelper helper, ServerPlayer player) {
        setStat(player, Stats.WALK_ONE_CM, 0);
        setStat(player, Stats.SPRINT_ONE_CM, 0);
        setStat(player, Stats.CROUCH_ONE_CM, 0);
        setStat(player, Stats.FLY_ONE_CM, 0);
        setStat(player, Stats.CLIMB_ONE_CM, 0);
        setStat(player, Stats.PLAY_TIME, 0);
        setStat(player, Stats.DAMAGE_TAKEN, 0);
        tracker(helper, player).simplebuilding$setBaseValues(0, 0, 0, 0, 0);
    }

    /**
     * Moves the kill baselines to the current tally plus {@code offset}, which is the only way to
     * take kills back: the tally itself has no setter, {@code awardKillScore} only ever counts up.
     */
    private static void rebaseKills(ServerPlayer player, SurvivalTracerAccessor tracker, int offset) {
        tracker.simplebuilding$setBaseValues(
                stat(player, Stats.WALK_ONE_CM) / 100,
                stat(player, Stats.PLAY_TIME),
                tracker.simplebuilding$getCurrentHostileKills() + offset,
                tracker.simplebuilding$getCurrentPassiveKills() + offset,
                0);
    }

    /** The value the combat curve converges on, needed to invert it. Restores nothing; callers rebase. */
    private static double ceilingOf(GameTestHelper helper, ServerPlayer player, SurvivalTracerAccessor tracker) {
        rebaseKills(player, tracker, 0);
        setStat(player, Stats.DAMAGE_TAKEN, 100_000_000);
        double ceiling = TrimMultiplierLogic.calculateCombatMultiplier(player);
        setStat(player, Stats.DAMAGE_TAKEN, 0);
        helper.assertTrue(ceiling > 0.0 && ceiling <= 1.0 + 1.0e-6,
                "the combat factor saturates at " + ceiling + ", outside the 0..1 band");
        return ceiling;
    }

    /**
     * Gives the respawn test's players a distinctive set of statistics to be rebased onto. Every one
     * of the five distance counters gets a different, non-zero number of metres, so that dropping
     * any single one of them from {@code SurvivalTracerMixin}'s own {@code getStatTotalDistance}
     * changes the sum, and so that removing the {@code /100} multiplies it by a hundred.
     */
    private static void stampProgress(ServerPlayer player) {
        setStat(player, Stats.WALK_ONE_CM, 100);
        setStat(player, Stats.SPRINT_ONE_CM, 200);
        setStat(player, Stats.CROUCH_ONE_CM, 300);
        setStat(player, Stats.FLY_ONE_CM, 400);
        setStat(player, Stats.CLIMB_ONE_CM, 500);
        setStat(player, Stats.PLAY_TIME, 5_678);
        setStat(player, Stats.DAMAGE_TAKEN, 91);
    }

    /**
     * Recovers the scale of {@code floor + (ceiling - floor) * (1 - e^(-x / scale))} from one
     * measurement of it. Only the shape of the curve is assumed; its floor and ceiling are measured
     * by the caller, so a rebalance of either does not turn this into a false failure.
     */
    private static double curveScale(double x, double factor, double floor, double ceiling) {
        double covered = (factor - floor) / (ceiling - floor);
        return -x / Math.log(1.0 - covered);
    }

    private static void assertField(GameTestHelper helper, CompoundTag tag, String key, int expected) {
        helper.assertTrue(tag.contains(key),
                "the saved tracker has no \"" + key + "\" field; a renamed field resets that counter "
                        + "to zero in every world that already exists, without anything looking wrong");
        helper.assertValueEqual(tag.getIntOr(key, Integer.MIN_VALUE), expected,
                "the saved value of \"" + key + "\"");
    }

    private static CompoundTag saveOf(GameTestHelper helper, ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        return output.buildResult();
    }

    // =====================================================================================
    // HELPERS: THE MEASUREMENTS
    // =====================================================================================

    /** Health lost to one hit, with the damage cooldown cleared so repeated hits all land. */
    private static float damageTaken(GameTestHelper helper, ServerPlayer player, DamageSource source) {
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        player.hurtServer(helper.getLevel(), source, HIT_DAMAGE);
        return player.getMaxHealth() - player.getHealth();
    }

    /**
     * {@code FoodData} has no exhaustion getter, so the value is read the way the game stores it.
     * That has the side effect of failing if the field is ever renamed in the save format, which is
     * a change worth noticing anyway.
     */
    private static float exhaustionOf(GameTestHelper helper, ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.getFoodData().addAdditionalSaveData(output);
        CompoundTag tag = output.buildResult();
        helper.assertTrue(tag.contains("foodExhaustionLevel"),
                "FoodData no longer saves a \"foodExhaustionLevel\" field, so the exhaustion "
                        + "assertions in this test cannot read anything");
        return tag.getFloatOr("foodExhaustionLevel", Float.NaN);
    }

    /** Puts the food bar into a known state, exhaustion included, through its own load path. */
    private static void resetFood(GameTestHelper helper, ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("foodLevel", 20);
        tag.putInt("foodTickTimer", 0);
        tag.putFloat("foodSaturationLevel", 5.0F);
        tag.putFloat("foodExhaustionLevel", 0.0F);
        player.getFoodData().readAdditionalSaveData(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        assertClose(helper, exhaustionOf(helper, player), 0.0,
                "the food bar could not be reset, so the next exhaustion reading would include the "
                        + "previous case");
    }

    /**
     * Grants {@code amount} experience and requires the player to end up with the rounded, boosted
     * gain. The multiplier is read immediately before the call because the gain itself changes the
     * player's level, and the level is one of the four factors it is built from.
     */
    private static void assertExperienceGain(GameTestHelper helper, ServerPlayer player, int amount) {
        float multiplier = TrimEffectUtil.getXPMultiplier(player);
        helper.assertTrue(multiplier > 1.0F,
                "test setup broken: the experience multiplier is " + multiplier);
        int expected = Math.round(amount * multiplier);
        helper.assertTrue(expected != amount,
                "test setup broken: " + amount + " experience comes out as " + expected
                        + " with and without the boost, so this case cannot fail");
        int before = player.totalExperience;
        player.giveExperiencePoints(amount);
        helper.assertValueEqual(player.totalExperience - before, expected,
                "a grant of " + amount + " experience at a multiplier of " + multiplier);
    }

    /**
     * Puts a wither the rib trim is strong enough to clear on the wearer, runs the single tick
     * {@code atTick}, and reports whether the trim cleared it. The effect is taken off again either
     * way, so that one tick after another can be driven without them interfering.
     */
    private static boolean ribClearsWitherAt(ServerPlayer player, int atTick) {
        applyWither(player, SHORT_WITHER);
        tickAt(player, atTick);
        boolean cleared = !player.hasEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WITHER);
        return cleared;
    }

    /**
     * What the amethyst material adds to one tick at {@code atTick}, measured against the very same
     * tick worn with an inert material instead. Everything else a tick does to a hurt player - the
     * peaceful auto heal above all - happens in both runs and cancels.
     */
    private static float amethystHealBonus(GameTestHelper helper, ServerPlayer player,
                                           Holder<TrimMaterial> amethyst, Holder<TrimMaterial> inert,
                                           Holder<TrimPattern> blank, int atTick) {
        float withoutIt = healingOverTick(helper, player, inert, blank, atTick);
        float withIt = healingOverTick(helper, player, amethyst, blank, atTick);
        return withIt - withoutIt;
    }

    /** Puts a fresh wither on the wearer, replacing any weaker one that addEffect would refuse. */
    private static void applyWither(ServerPlayer player, int duration) {
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, false, false, false));
    }

    /**
     * Runs exactly one tick of the player while {@code tickCount} reads {@code atTick}.
     *
     * <p>The counter is set to {@code atTick} and not to {@code atTick - 1}, because a tick driven
     * by hand does not advance it. In MC 26.2 {@code tickCount++} sits in
     * {@code ServerLevel#tickNonPassenger}, not in {@code Entity#baseTick} - the level's tick loop
     * raises it, and this test bypasses that loop by calling the connection directly. Subtracting
     * one here made every cadence assertion read a tick that is one short of the period, so the
     * branch under test never fired.
     */
    private static void tickAt(ServerPlayer player, int atTick) {
        player.tickCount = atTick;
        player.connection.tick();
    }

    /**
     * Health gained over one tick on the cadence, for a wearer whose wither is too long to be
     * cleared. Returned as a raw number so the caller can take the difference between two runs and
     * cancel whatever else the world does to a hurt player on a 20 tick boundary.
     */
    private static float witherTickHealing(GameTestHelper helper, ServerPlayer player,
                                           Holder<TrimMaterial> trimMaterial,
                                           Holder<TrimPattern> trimPattern) {
        wear(player, trimMaterial, trimPattern, 4);
        applyWither(player, LONG_WITHER);
        player.setHealth(10.0F);
        tickAt(player, 200);
        helper.assertTrue(player.hasEffect(MobEffects.WITHER),
                "the wither of " + LONG_WITHER + " ticks was cleared instead of healed against, so "
                        + "the two rib branches can no longer be told apart");
        float healed = player.getHealth() - 10.0F;
        player.removeEffect(MobEffects.WITHER);
        return healed;
    }

    /** Health gained over one tick at {@code atTick}, from a wearer starting at half health. */
    private static float healingOverTick(GameTestHelper helper, ServerPlayer player,
                                         Holder<TrimMaterial> trimMaterial,
                                         Holder<TrimPattern> trimPattern, int atTick) {
        wear(player, trimMaterial, trimPattern, 4);
        player.setHealth(10.0F);
        helper.assertTrue(player.getHealth() < player.getMaxHealth(),
                "the wearer is at full health, so a heal could not be measured");
        tickAt(player, atTick);
        return player.getHealth() - 10.0F;
    }

    // =====================================================================================
    // HELPERS: COMMANDS
    // =====================================================================================

    private static int run(GameTestHelper helper, CommandDispatcher<CommandSourceStack> dispatcher,
                           ServerPlayer player, String command) {
        try {
            return dispatcher.execute(command, player.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("/" + command + " was refused: " + e.getMessage()
                    + " - either the command is not registered by this loader at all, or its "
                    + "permission gate or argument bounds have moved");
        }
    }

    private static void assertRefused(GameTestHelper helper,
                                      CommandDispatcher<CommandSourceStack> dispatcher,
                                      ServerPlayer player, String command, String what) {
        try {
            dispatcher.execute(command, player.createCommandSourceStack());
        } catch (CommandSyntaxException expected) {
            return;
        }
        throw helper.assertionException("/" + command + " was accepted from " + what);
    }

    // =====================================================================================
    // HELPERS: SMITHING
    // =====================================================================================

    /**
     * A smithing menu with a real {@link ContainerLevelAccess}, unlike the one in
     * {@code DynamicLightTests}: vanilla's own recipe lookup lives behind that access and does
     * nothing at all with {@code ContainerLevelAccess.NULL}, and vanilla's recipe is exactly what
     * this test is here to run.
     */
    private static SmithingMenu smithingTable(GameTestHelper helper) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException(
                    "GameTestHelper#makeMockServerPlayer no longer returns a ServerPlayer but a "
                            + raw.getClass().getName());
        }
        BlockPos table = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SMITHING_TABLE);
        return new SmithingMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), table));
    }

    /**
     * Trims one piece of mod armour with one mod ingredient at a real smithing table and requires
     * the material that comes out to be the one that ingredient is registered to provide.
     */
    private static void assertTrims(GameTestHelper helper, SmithingMenu table, Item armour,
                                    Item ingredient, ResourceKey<TrimMaterial> expected) {
        ItemStack trimmed = smith(table, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                new ItemStack(armour), new ItemStack(ingredient));
        ArmorTrim trim = trimmed.get(DataComponents.TRIM);
        helper.assertTrue(trim != null,
                "a smithing table refused to trim " + armour + " with " + ingredient + ": either "
                        + armour + " has left " + ItemTags.TRIMMABLE_ARMOR.location() + ", or "
                        + ingredient + " has left " + ItemTags.TRIM_MATERIALS.location() + ", or it "
                        + "has lost the trim material component - without that component vanilla's "
                        + "applyTrim hands back an empty stack and the ingredient can never be "
                        + "applied as a trim again");
        helper.assertTrue(trim.material().is(expected),
                ingredient + " put " + trim.material().getRegisteredName() + " on a piece of armour "
                        + "where " + expected.identifier() + " was expected; the material an item is "
                        + "registered with decides both the colour the player sees and which of "
                        + "TrimEffectUtil's material bonuses the set pays out");
    }

    private static ItemStack smith(SmithingMenu table, Item template, ItemStack base, ItemStack material) {
        Container inputs = table.getSlot(SmithingMenu.TEMPLATE_SLOT).container;
        table.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(template));
        table.getSlot(SmithingMenu.BASE_SLOT).set(base);
        table.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(material);
        table.slotsChanged(inputs);
        return table.getSlot(table.getResultSlot()).getItem();
    }

    // =====================================================================================
    // HELPERS: TRIMS AND REGISTRIES
    // =====================================================================================

    /** Puts {@code pieces} trimmed armour pieces on and empties the remaining armour slots. */
    private static void wear(LivingEntity entity, Holder<TrimMaterial> trimMaterial,
                             Holder<TrimPattern> trimPattern, int pieces) {
        ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
        for (int i = 0; i < ARMOUR_SLOTS.length; i++) {
            entity.setItemSlot(ARMOUR_SLOTS[i],
                    i < pieces ? trimmed(ARMOUR_ITEMS[i], trim) : ItemStack.EMPTY);
        }
    }

    private static void bare(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static ItemStack trimmed(Item item, ArmorTrim trim) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.TRIM, trim);
        return stack;
    }

    private static Holder<TrimMaterial> material(GameTestHelper helper, ResourceKey<TrimMaterial> key) {
        Registry<TrimMaterial> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        Optional<Holder.Reference<TrimMaterial>> found = registry.get(key);
        helper.assertTrue(found.isPresent(), "the trim material " + key.identifier()
                + " is not in the datapack registry; for a mod material that means its generated "
                + "JSON never reached the jar");
        return found.orElseThrow();
    }

    private static Holder<TrimPattern> pattern(GameTestHelper helper, ResourceKey<TrimPattern> key) {
        Registry<TrimPattern> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        Optional<Holder.Reference<TrimPattern>> found = registry.get(key);
        helper.assertTrue(found.isPresent(),
                "the trim pattern " + key.identifier() + " is not in the datapack registry");
        return found.orElseThrow();
    }

    /** The colour the running datapack registry carries for a material, i.e. what datagen wrote out. */
    private static TextColor colourOf(GameTestHelper helper, ResourceKey<TrimMaterial> key, String name) {
        TextColor colour = material(helper, key).value().description().getStyle().getColor();
        helper.assertTrue(colour != null,
                "the trim material " + name + " has no colour on its description, so it renders in "
                        + "plain white and looks like any vanilla trim");
        return colour;
    }

    /**
     * Runs {@code ModTrimMaterials.bootstrap} into a recording context - the exact call datagen
     * makes - and hands back what it registered. That is where the expected colours come from, so
     * that they are the literals datagen would write out and not a second copy of them.
     */
    private static Map<ResourceKey<TrimMaterial>, TrimMaterial> bootstrapped() {
        RecordingBootstrap recorder = new RecordingBootstrap();
        ModTrimMaterials.bootstrap(recorder);
        return recorder.registered;
    }

    /**
     * The colour the running datapack registry carries for a material, required to be the colour
     * {@code bootstrap} declares for it. Returns the registry's reading for the callers that go on
     * to compare the three against each other.
     */
    private static TextColor assertColourIsCurrent(GameTestHelper helper,
                                                   Map<ResourceKey<TrimMaterial>, TrimMaterial> declared,
                                                   ResourceKey<TrimMaterial> key, String name) {
        TrimMaterial inBootstrap = declared.get(key);
        helper.assertTrue(inBootstrap != null,
                "ModTrimMaterials.bootstrap registers no " + name + " at all, so datagen writes no "
                        + "JSON for it and nothing can load it");
        TextColor declaredColour = inBootstrap.description().getStyle().getColor();
        helper.assertTrue(declaredColour != null,
                "ModTrimMaterials.bootstrap gives " + name + " no colour, so datagen writes a "
                        + "description without a style and the material renders in plain white");
        TextColor registered = colourOf(helper, key, name);
        helper.assertTrue(registered.getValue() == declaredColour.getValue(),
                "the colour of " + name + " in the running datapack registry is " + registered
                        + " where ModTrimMaterials.bootstrap declares " + declaredColour
                        + "; the generated JSON on disk is older than the bootstrap that is supposed "
                        + "to produce it, so the game ships a colour nobody edited");
        return registered;
    }

    /**
     * A {@link BootstrapContext} that records what it is handed instead of registering it, so that
     * {@code ModTrimMaterials.bootstrap} can be run from a test the way datagen runs it. Neither
     * unsupported member is reachable from that method today: it ignores the holder {@code register}
     * returns, and it looks nothing up.
     */
    private static final class RecordingBootstrap implements BootstrapContext<TrimMaterial> {

        private final Map<ResourceKey<TrimMaterial>, TrimMaterial> registered = new HashMap<>();

        @Override
        public Holder.Reference<TrimMaterial> register(ResourceKey<TrimMaterial> key,
                                                      TrimMaterial value, Lifecycle lifecycle) {
            registered.put(key, value);
            return null;
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
            throw new UnsupportedOperationException(
                    "ModTrimMaterials.bootstrap looked " + registryKey.identifier() + " up; this "
                            + "recording context cannot serve that, so the colours it collected "
                            + "would be incomplete");
        }
    }

    // =====================================================================================
    // HELPERS: ASSERTIONS
    // =====================================================================================

    /**
     * Several of the values below are {@code float} internally, so a double-tight epsilon would
     * fail on rounding alone; 1.0e-4 is still far below every step this class asserts.
     */
    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-4,
                what + ": expected " + expected + " but got " + actual);
    }

    /** For the recovered curve scales, where an exponential is inverted and a relative bound fits. */
    private static void assertRelative(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < expected * 0.01,
                what + ": measured " + actual + " where " + expected + " was expected");
    }
}
