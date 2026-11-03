package com.simplebuilding.gametest;

import com.simplebuilding.version.McVersion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.serialization.Lifecycle;
import com.simplebuilding.command.SimplebuildingCommand;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.trim.ModTrimMaterials;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.simplebuilding.util.TrimMultiplierLogic;
import com.simplebuilding.util.TrimStatsLayout;
import java.util.HashMap;
import java.util.List;
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
 * <p>{@code TrimEffectRuleTests} (Simple Mobs, since ZO9 A) pins what the trim rules <em>computes</em> - every count,
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
 *   <li><b>The Tide swim bonus is capped by vanilla, not by the mod.</b> It is delivered now - the
 *       branch sits in {@code PlayerEntityMixin} next to the land one, because
 *       {@code Player#getSpeed} overrides {@code LivingEntity#getSpeed} without calling
 *       {@code super} and the injection in {@code LivingEntityMixin} therefore only ever reaches
 *       non-players. What it buys in the water is still small, and for a vanilla reason spelled
 *       out on
 *       {@link #thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards}: without Depth
 *       Strider, {@code travelInWater} does not read {@code getSpeed()} at all.</li>
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

    /**
     * A wither the rib trim is strong enough to clear outright - under the 100 tick cap on the rib
     * cleansing. Not a multiple of 40, see the test.
     */
    private static final int SHORT_WITHER = 61;

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
     *   <li><b>The ceiling, and with it the amplitude.</b> Cross-checked the same way, against the
     *       experience curve at level 100: all three curves are meant to span one band, so the
     *       saturated survival factor has to be the top of that band and not merely somewhere
     *       above the floor. It needs saying out loud because every scale below is recovered
     *       <em>using</em> this reading - shrink the amplitude of the exponential and the floor,
     *       all three scales and the whole rest of this test come out unchanged, while every trim
     *       benefit an endgame player earns silently loses most of its value.</li>
     *   <li><b>The distance unit.</b> The five {@code *_ONE_CM} counters are summed after each is
     *       divided by 100, so 500 cm of walking and 100 cm in each of the five counters have to
     *       come out the same, and 99 cm has to come out as nothing at all.</li>
     *   <li><b>The two scales.</b> Recovered by inverting the curve at a single measurement, using
     *       the floor and the ceiling this same test measures. The pair is what makes an hour of
     *       play worth as much as four kilometres of walking; if either scale moves, one half of
     *       the mechanic quietly stops mattering.</li>
     *   <li><b>A sum (owner N49).</b> Distance and time are two halves of at most 0.5 each; both count.</li>
     * </ol>
     *
     * <p>The last block is the baseline arithmetic: with the base set to the current reading the
     * factor drops back to the floor, and with the base set <em>above</em> the current reading -
     * which is what a rolled-back statistics file looks like - it stays at the floor instead of
     * going negative and dragging the whole multiplier below zero.
     *
     * <p>What breaks this: a changed scale, a changed floor or a changed amplitude in one curve
     * only, the {@code /100} disappearing, one of the five distance counters being dropped,
     * the sum turning back into {@code max}, or the {@code Math.max(0, ...)} clamps going away.
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
        player.totalExperience = 0;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), floor,
                "the experience curve starts from a different floor than the survival curve");

        // --- the ceiling, needed below to invert the curve ---
        setStat(player, Stats.WALK_ONE_CM, 100_000_000);
        tracker.simplebuilding$setActiveTime(100_000_000);
        double ceiling = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        helper.assertTrue(ceiling > floor && ceiling <= 1.0 + 1.0e-6,
                "the survival factor saturates at " + ceiling + "; it has to converge on a value "
                        + "above its floor and never above 1.0, otherwise the configured base is no "
                        + "longer the only thing that can push the multiplier past 1");
        // The band above is only a band, and every scale below is recovered from this very reading,
        // so the amplitude of the exponential cancels out of all three of them. Pinning the top end
        // against the experience curve is what stops a shrunken amplitude - a fully progressed
        // player quietly getting a fraction of the trim benefits they earned - from passing here.
        player.totalExperience = TrimMultiplierLogic.XP_POINTS_FOR_FULL_FACTOR;
        assertClose(helper, TrimMultiplierLogic.calculateXPMultiplier(player), ceiling,
                "the survival curve saturates at " + ceiling + " where the experience curve reaches "
                        + TrimMultiplierLogic.calculateXPMultiplier(player) + "; the three factors "
                        + "no longer share one 0.1..1.0 band, so a fully progressed player gets a "
                        + "fraction of the multiplier the design promises");
        player.totalExperience = 0;
        // N49: S = distance half + time half, each half covers half of the band above the floor.
        double halfCeiling = floor + (ceiling - floor) / 2.0;

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
        assertRelative(helper, curveScale(4000.0, atFourThousandMetres, floor, halfCeiling), 4000.0,
                "the distance scale of the survival curve");

        // --- the play time scale ---
        clearProgress(helper, player);
        tracker.simplebuilding$setActiveTime(72_000);
        double atOneHour = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        assertRelative(helper, curveScale(72_000.0, atOneHour, floor, halfCeiling), 72_000.0,
                "the play time scale of the survival curve");

        // --- the sum (owner N49): distance and time both count, each half on its own ---
        clearProgress(helper, player);
        setStat(player, Stats.WALK_ONE_CM, 40_000);
        double atFourHundredMetres = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        tracker.simplebuilding$setActiveTime(72_000);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), atFourHundredMetres + atOneHour - floor,
                "an hour of play plus 400 m walked is not the sum of both halves");

        clearProgress(helper, player);
        tracker.simplebuilding$setActiveTime(7_200);
        double atSixMinutes = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        setStat(player, Stats.WALK_ONE_CM, 400_000);
        assertClose(helper, TrimMultiplierLogic.calculateSurvivalMultiplier(player), atFourThousandMetres + atSixMinutes - floor,
                "4000 m walked plus six minutes of play is not the sum of both halves");
        helper.assertTrue(TrimMultiplierLogic.distancePart(100_000_000) <= TrimMultiplierLogic.SURVIVAL_PART_MAX + 1.0e-9
                        && TrimMultiplierLogic.timePart(Integer.MAX_VALUE) <= TrimMultiplierLogic.SURVIVAL_PART_MAX + 1.0e-9,
                "a survival half goes above its maximum of 0.5");

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
     * with a baseline above the current tally, the state a statistics rollback leaves behind, once
     * for the two kill counters and once for the damage counter. The damage counter needs its own
     * case because it has its own clamp and because the rest of this test only ever raises it: a
     * baseline that outruns it can only come from the world's statistics file being lost or rolled
     * back while the player data keeps the baseline, and that is exactly when losing the clamp
     * turns the multiplier negative and {@code modifyDamage} into damage amplification.
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
                        + " instead of the floor; the kill clamps are gone");

        // The same rollback on the damage counter, which has a clamp of its own. Every other case
        // in this test leaves the damage baseline at zero, so nothing above would notice it going
        // away; the counter is left at 20 here so that the two readings really differ in sign.
        setStat(player, Stats.DAMAGE_TAKEN, 20);
        tracker.simplebuilding$setBaseValues(0, 0,
                tracker.simplebuilding$getCurrentHostileKills(),
                tracker.simplebuilding$getCurrentPassiveKills(),
                999_999);
        assertClose(helper, TrimMultiplierLogic.calculateCombatMultiplier(player), floor,
                "a damage baseline above the damage actually taken produced "
                        + TrimMultiplierLogic.calculateCombatMultiplier(player)
                        + " instead of the floor; the clamp on the damage counter is gone, so a "
                        + "lost statistics file drives the combat factor far below zero and "
                        + "modifyDamage turns every trim's damage reduction into amplification");

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
        saverTracker.simplebuilding$setBaseXp(66);
        saverTracker.simplebuilding$setActiveTime(77);
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
        assertField(helper, data, "BaseXp", 66);
        assertField(helper, data, "ActiveTicks", 77);

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
        replacement.putInt("BaseXp", 108);
        replacement.putInt("ActiveTicks", 109);
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
        helper.assertValueEqual(loaderTracker.simplebuilding$getBaseXp(), 108, "BaseXp after loading");
        helper.assertValueEqual(loaderTracker.simplebuilding$getCurrentTime(), 109, "ActiveTicks after loading");
        // A save from before the active clock existed: its BaseTime was a play time statistic and
        // does not fit the new counter, so both start from zero.
        ServerPlayer legacy = detachedPlayer(helper, new Vec3(6.5, 2.0, 2.5));
        CompoundTag legacyTag = saved.copy();
        CompoundTag legacyData = replacement.copy();
        legacyData.remove("ActiveTicks");
        legacyTag.put("SimpleBuildingData", legacyData);
        legacy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacyTag));
        helper.assertValueEqual(tracker(helper, legacy).simplebuilding$getBaseTime(), 0,
                "a save without ActiveTicks kept its play-time BaseTime, which would hold the time factor at its floor for hours");

        // ---------- the respawn ----------
        ServerPlayer dead = detachedPlayer(helper, new Vec3(2.5, 2.0, 4.5));
        SurvivalTracerAccessor deadTracker = tracker(helper, dead);
        deadTracker.simplebuilding$setBaseValues(11, 22, 33, 44, 55);
        deadTracker.simplebuilding$setBaseXp(66);
        deadTracker.simplebuilding$setActiveTime(5678);
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
        helper.assertValueEqual(travelledTracker.simplebuilding$getBaseXp(), 66,
                "the experience baseline was rebased by a respawn that was not a death");
        helper.assertValueEqual(travelledTracker.simplebuilding$getCurrentTime(), 5678,
                "the active time did not follow the player through the portal");
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
                "after a death the time baseline has to be the active time carried over from the old life");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getCurrentTime(), 5678,
                "the active time counter was not carried over into the new life");
        helper.assertValueEqual(respawnedTracker.simplebuilding$getBaseXp(), respawned.totalExperience,
                "after a death the experience baseline has to be the experience the new life starts with");
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

    // (c) WHAT THE MIXINS ACTUALLY DELIVER: moved to Simple Mobs with the trim abilities (ZO9 A / MOB27-3, TrimHookRuleTests).

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
     * The time half of the survival factor counts only active play: every 20 ticks the tracker adds
     * 20 ticks if the player's travelled distance changed within the last minute (1200 ticks).
     * Standing AFK stops the clock after that grace minute; moving again restarts it. Driven through
     * the real {@code ServerPlayer#tick} (the injection sits at its tail), one call per look.
     *
     * <p>What breaks this: the tick injection leaving the mixin, counting plain play time again, the
     * grace window growing or disappearing, or the counter not feeding the survival curve.
     */
    public static void survivalTimeCountsOnlyWhileThePlayerMoves(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        SurvivalTracerAccessor tracker = tracker(helper, player);
        clearProgress(helper, player);

        // First look at the distance: nothing counts yet, there is no earlier reading to compare.
        serverTickAt(player, 20);
        int start = tracker.simplebuilding$getCurrentTime();

        // Moving: the distance changes between two looks, so the next looks count.
        setStat(player, Stats.WALK_ONE_CM, 500);
        serverTickAt(player, 40);
        serverTickAt(player, 60);
        int moving = tracker.simplebuilding$getCurrentTime() - start;
        helper.assertTrue(moving == 40,
                "two looks right after moving added " + moving + " ticks of active time instead of 40");

        // Standing still: the grace minute keeps counting, after it nothing does.
        int beforeIdle = tracker.simplebuilding$getCurrentTime();
        serverTickAt(player, 1240);
        int insideGrace = tracker.simplebuilding$getCurrentTime() - beforeIdle;
        helper.assertTrue(insideGrace == 20,
                "a look inside the grace minute after the last step added " + insideGrace + " ticks instead of 20");
        int afterGrace = tracker.simplebuilding$getCurrentTime();
        serverTickAt(player, 2000);
        serverTickAt(player, 4000);
        helper.assertTrue(tracker.simplebuilding$getCurrentTime() == afterGrace,
                "an AFK player kept earning active time: " + (tracker.simplebuilding$getCurrentTime() - afterGrace)
                        + " ticks after the grace minute");

        // And it feeds the curve: an hour of active time is the same as the old hour of play time.
        tracker.simplebuilding$setActiveTime(72_000);
        tracker.simplebuilding$setBaseValues(0, 0, 0, 0, 0);
        // N49: time is one half of S (distance + time); an hour fills it to about 0.33 of its 0.5.
        helper.assertTrue(TrimMultiplierLogic.calculateSurvivalMultiplier(player) > 0.3,
                "an hour of active time did not move the survival factor");
        clearProgress(helper, player);
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
     * <p>The tree is built once per Minecraft line in {@code SimplebuildingCommand}, and each
     * loader's {@code ModCommands} only hooks it in (until 2026-09 each of the five loader trees
     * built its own copy, so a run only ever covered the one the current loader installed). The
     * first check is therefore that the live {@code /simplebuilding} node is the shared one: its
     * requirement is {@code SimplebuildingCommand.OPERATOR_ONLY} itself, and its usage is exactly
     * what {@code SimplebuildingCommand.register} builds into an empty dispatcher. If a loader stops
     * registering it at all, that check fails first, and the first command that is meant to succeed
     * would fail too rather than silently passing as "correctly refused" - which is why the
     * permitted cases come after the refused one.
     *
     * <p>What breaks this: the {@code requires} gate disappearing or inverting, the argument bounds
     * widening, the executor no longer writing the config, the whole registration being dropped
     * on one loader, or a loader building its own tree again instead of the shared one.
     */
    public static void theTrimMultiplierCommandGuardsItsRangeAndItsPermission(GameTestHelper helper) {
        double configuredBase = com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier;
        ServerPlayer player = mockPlayer(helper);
        PlayerList players = helper.getLevel().getServer().getPlayerList();
        CommandDispatcher<CommandSourceStack> dispatcher =
                helper.getLevel().getServer().getCommands().getDispatcher();
        boolean wasOperator = players.isOp(player.nameAndId());
        Runnable installedSaver = com.simplebuilding.config.ConfigSaving.saver();
        // The command saves the config; a game test must not write the file (other tests hold options
        // flipped while it runs). Saving itself is theTrimMultiplierCommandSavesAndSyncsItsValue's job.
        com.simplebuilding.config.ConfigSaving.setSaver(() -> { });
        try {
            helper.assertFalse(wasOperator,
                    "the mock player is already an operator, so the permission check below would pass "
                            + "with the requires() gate deleted");

            // --- the live node is the shared tree, on every loader ---
            CommandNode<CommandSourceStack> live = dispatcher.getRoot().getChild("simplebuilding");
            helper.assertTrue(live != null, "/simplebuilding is not registered by this loader at all");
            helper.assertTrue(live.getRequirement() == SimplebuildingCommand.OPERATOR_ONLY,
                    "/simplebuilding on this loader carries a requirement of its own instead of "
                            + "SimplebuildingCommand.OPERATOR_ONLY, so the loader builds its own tree again");
            CommandDispatcher<CommandSourceStack> reference = new CommandDispatcher<>();
            SimplebuildingCommand.register(reference);
            // /simplebuilding tweaks ... haengt der Simple-Tweaks-Teil an denselben Knoten.
            com.simplebuilding.tweaks.command.TweaksCommands.register(reference);
            CommandSourceStack source = player.createCommandSourceStack();
            // Sorted: the loaders register the tweaks and config branches in different orders.
            helper.assertValueEqual(java.util.Arrays.stream(dispatcher.getAllUsage(live, source, false)).sorted().toList(),
                    java.util.Arrays.stream(reference.getAllUsage(reference.getRoot().getChild("simplebuilding"), source, false))
                            .sorted().toList(),
                    "usage of the live /simplebuilding subtree against the one SimplebuildingCommand builds");

            // --- without operator rights the subtree is not even reachable ---
            com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier = 1.0;
            assertRefused(helper, dispatcher, player, "simplebuilding config setTrimMultiplier 3.5",
                    "a player who is not an operator");
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, 1.0,
                    "a player who is not an operator changed the trim multiplier anyway");

            players.op(player.nameAndId());
            helper.assertTrue(players.isOp(player.nameAndId()),
                    "the mock player could not be made an operator, so the rest of this test would "
                            + "only be re-checking the refusal above");

            // --- an accepted value reaches the config ---
            run(helper, dispatcher, player, "simplebuilding config setTrimMultiplier 3.5");
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, 3.5,
                    "setTrimMultiplier reported success but the configured base did not move");

            // --- the declared limit is inclusive ---
            double limit = SimplebuildingConfig.maxMultiplierLimit;
            run(helper, dispatcher, player, "simplebuilding config setTrimMultiplier " + limit);
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, limit,
                    "the configured upper limit was refused by the command that declares it");

            // --- and both ends of the range are closed ---
            assertRefused(helper, dispatcher, player,
                    "simplebuilding config setTrimMultiplier " + (limit + 0.5), "a value above the limit");
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, limit,
                    "a value above the limit was refused but written anyway");
            assertRefused(helper, dispatcher, player,
                    "simplebuilding config setTrimMultiplier -0.5", "a negative value");
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, limit,
                    "a negative value was refused but written anyway");

            // --- the getter reports and changes nothing ---
            helper.assertValueEqual(run(helper, dispatcher, player, "simplebuilding config getTrimMultiplier"),
                    1, "return code of getTrimMultiplier");
            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, limit,
                    "getTrimMultiplier changed the value it was only meant to report");

            helper.succeed();
        } finally {
            if (!wasOperator) {
                players.deop(player.nameAndId());
            }
            com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier = configuredBase;
            com.simplebuilding.config.ConfigSaving.setSaver(installedSaver);
        }
    }

    /**
     * {@code setTrimMultiplier} is persistent and reaches the clients (audit 2026-09-26 #19): the
     * value is an instance field of the config - so Gson writes it into {@code simplebuilding.json},
     * which it never did for the old static field - the command saves the config right away, and
     * every online player gets a {@code TrimDataPayload} carrying the new base, which is what the
     * client's {@code TrimStatsPanel} shows ({@code TrimMultiplierLogic.baseMultiplier(true)}).
     *
     * <p>The saver and the packet sender are swapped for recorders, so the test neither writes the
     * real file nor depends on the mock player's connection.
     */
    public static void theTrimMultiplierCommandSavesAndSyncsItsValue(GameTestHelper helper) {
        double configuredBase = com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier;
        ServerPlayer player = mockPlayer(helper);
        PlayerList players = helper.getLevel().getServer().getPlayerList();
        CommandDispatcher<CommandSourceStack> dispatcher =
                helper.getLevel().getServer().getCommands().getDispatcher();
        boolean wasOperator = players.isOp(player.nameAndId());

        List<com.google.gson.JsonElement> saved = new java.util.ArrayList<>();
        Runnable installedSaver = com.simplebuilding.config.ConfigSaving.saver();
        com.simplebuilding.config.ConfigSaving.setSaver(() -> saved.add(
                new com.google.gson.Gson().toJsonTree(com.simplebuilding.Simplebuilding.getConfig())
                        .getAsJsonObject().get("trimBenefitBaseMultiplier")));

        List<Double> sentBases = new java.util.ArrayList<>();
        com.simplebuilding.platform.PlayerPacketSender installedSender = com.simplebuilding.platform.PlatformServices.playerPacketSender();
        com.simplebuilding.platform.PlatformServices.setPlayerPacketSender(new com.simplebuilding.platform.PlayerPacketSender() {
            @Override
            public boolean canSend(ServerPlayer target, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
                return target == player || installedSender.canSend(target, type);
            }

            @Override
            public void send(ServerPlayer target, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
                if (target == player) {
                    if (payload instanceof com.simplebuilding.networking.TrimDataPayload data) {
                        sentBases.add(data.baseMultiplier());
                    }
                    return;
                }
                installedSender.send(target, payload);
            }
        });
        try {
            players.op(player.nameAndId());
            run(helper, dispatcher, player, "simplebuilding config setTrimMultiplier 4.5");

            assertClose(helper, com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier, 4.5,
                    "setTrimMultiplier did not write the config instance");
            assertClose(helper, com.simplebuilding.util.TrimMultiplierLogic.baseMultiplier(false), 4.5,
                    "the server side resonance base does not follow the config");
            helper.assertTrue(saved.size() == 1,
                    "setTrimMultiplier saved the config " + saved.size() + " times instead of once, so the "
                            + "value is gone after a restart");
            helper.assertTrue(saved.get(0) != null && Math.abs(saved.get(0).getAsDouble() - 4.5) < 1e-9,
                    "the saved config carries trimBenefitBaseMultiplier=" + saved.get(0)
                            + " instead of 4.5 - the field is not part of simplebuilding.json");
            helper.assertTrue(sentBases.contains(4.5),
                    "no TrimDataPayload with the new base 4.5 reached the online player (sent: " + sentBases
                            + "), so the client keeps showing its own value");
            helper.succeed();
        } finally {
            if (!wasOperator) {
                players.deop(player.nameAndId());
            }
            com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier = configuredBase;
            com.simplebuilding.config.ConfigSaving.setSaver(installedSaver);
            com.simplebuilding.platform.PlatformServices.setPlayerPacketSender(installedSender);
        }
    }

    /** Resonance button (owner N49): slot-sized, between the extra head slot and the off-hand slot, heart only. */
    public static void resonancePanelLayoutSitsBesideRecipeBook(GameTestHelper helper) {
        helper.assertValueEqual(TrimStatsLayout.SIZE, 19, "the resonance button is 19x19 so the 9x9 heart is exactly centred");
        helper.assertValueEqual(TrimStatsLayout.ICON_INSET * 2 + TrimStatsLayout.ICON_WIDTH, TrimStatsLayout.SIZE, "the heart sits exactly in the middle");
        helper.assertValueEqual(TrimStatsLayout.HEAD_SLOT_X, 77, "the head slot is in the off-hand column");
        helper.assertValueEqual(TrimStatsLayout.HEAD_SLOT_Y, 8, "the head slot is level with the helmet slot");
        // between the head slot frame (7..25) and the off-hand slot frame (61..79), centred in the 36 px gap
        helper.assertTrue(Math.abs((TrimStatsLayout.INVENTORY_Y - (TrimStatsLayout.HEAD_SLOT_Y + 17))
                - (61 - (TrimStatsLayout.INVENTORY_Y + TrimStatsLayout.SIZE))) <= 1, "the button is centred between the two slots (odd gap: 1 px tolerance)");
        helper.assertTrue(TrimStatsLayout.hovered(76, 34, 76, 34) && TrimStatsLayout.hovered(76, 34, 94.9, 52.9)
                && !TrimStatsLayout.hovered(76, 34, 95, 40), "hover area is the 19x19 button");
        helper.assertValueEqual(TrimStatsLayout.ICON_WIDTH, 9, "the heart is 9 wide again (owner N29)");
        helper.assertValueEqual(TrimStatsLayout.ICON_HEIGHT, 9, "the heart is 9 high");
        helper.succeed();
    }

    /** Owner N29/N49: tier hearts, at maximum resonance (all factors 1.0, value = base) the enderite heart. */
    public static void maxResonanceShowsTheDiamondHeart(GameTestHelper helper) {
        helper.assertTrue(TrimStatsLayout.isMaxResonance(3.0, 3.0), "value equal to the base is the maximum");
        helper.assertTrue(TrimStatsLayout.isMaxResonance(2.996, 3.0), "a value shown as the base (3.00x) is the maximum");
        helper.assertFalse(TrimStatsLayout.isMaxResonance(2.99, 3.0), "2.99x below a base of 3.00 is not the maximum");
        helper.assertValueEqual(TrimStatsLayout.heartSprite(1.5, 1.5), "resonance_heart_enderite", "enderite heart at the maximum");
        helper.assertValueEqual(TrimStatsLayout.heartSprite(0.15, 1.5), "resonance_heart_cobblestone", "cobblestone heart at the floor");
        // N49: eight tiers in equal steps over the band 0.1..1.0 of the base
        String[] expected = TrimStatsLayout.TIERS;
        for (int i = 0; i < expected.length; i++) {
            double fraction = 0.1 + 0.9 * (i + 0.5) / expected.length;
            helper.assertValueEqual(TrimStatsLayout.heartSprite(2.0 * fraction, 2.0), "resonance_heart_" + expected[i],
                    "heart tier in the middle of band " + i);
        }
        ServerPlayer player = mockPlayer(helper);
        double base = TrimMultiplierLogic.baseMultiplier(false);
        double total = TrimMultiplierLogic.getMultiplier(player);
        helper.assertFalse(TrimStatsLayout.isMaxResonance(total, base),
                "a fresh player (" + total + " of " + base + ") has no maximum resonance yet");
        helper.assertValueEqual(TrimStatsLayout.heartSprite(base * (1.0 + 1.0 + 1.0) / 3.0, base), "resonance_heart_enderite",
                "all three factors at 1.0 give the enderite heart");
        helper.succeed();
    }

    /**
     * ZO9 A / MOB27-3 (docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md): the trim abilities act in Simple Mobs, the core only SHOWS their
     * rates (tooltip, reference screen through {@link com.simplebuilding.util.TrimBonusCatalog}). The core cannot import Simple Mobs, so
     * both sides pin the same table from docs/TRIM-BALANCE.md: this test the displayed {@link com.simplebuilding.util.TrimRates}, Simple
     * Mobs' {@code trim_ability_table} its acting constants. A rate changed on one side only fails one of the two. Also the exact
     * matching of the catalog (moved here from the old TrimEffectTests): foreign or look-alike patterns/materials show nothing.
     */
    public static void theDisplayedRatesMatchTheBalanceTable(GameTestHelper helper) {
        float[][] table = {
                {com.simplebuilding.util.TrimRates.DAMAGE_FLOOR, 0.2f}, {com.simplebuilding.util.TrimRates.GENERIC_REDUCTION_CAP, 0.25f},
                {com.simplebuilding.util.TrimRates.SENTRY_PROJECTILE, 0.05f}, {com.simplebuilding.util.TrimRates.VEX_MAGIC, 0.06f},
                {com.simplebuilding.util.TrimRates.WILD_THORNS, 0.10f}, {com.simplebuilding.util.TrimRates.DUNE_BLAST, 0.08f},
                {com.simplebuilding.util.TrimRates.COAST_DROWN, 0.10f}, {com.simplebuilding.util.TrimRates.COAST_AIR_SAVE, 0.10f},
                {com.simplebuilding.util.TrimRates.WARD_ALL, 0.03f}, {com.simplebuilding.util.TrimRates.SILENCE_SONIC, 0.20f},
                {com.simplebuilding.util.TrimRates.SILENCE_STEALTH, 0.08f}, {com.simplebuilding.util.TrimRates.SNOUT_FIRE, 0.05f},
                {com.simplebuilding.util.TrimRates.RIB_WITHER, 0.10f}, {com.simplebuilding.util.TrimRates.RIB_WITHER_TICKS, 40f},
                {com.simplebuilding.util.TrimRates.EYE_DRAGON_BREATH, 0.10f}, {com.simplebuilding.util.TrimRates.SPIRE_FALL, 0.08f},
                {com.simplebuilding.util.TrimRates.FLOW_WIND_CHARGE, 0.10f}, {com.simplebuilding.util.TrimRates.BOLT_LIGHTNING, 0.25f},
                {com.simplebuilding.util.TrimRates.BOLT_SPEED, 0.05f}, {com.simplebuilding.util.TrimRates.TIDE_SWIM, 0.10f},
                {com.simplebuilding.util.TrimRates.WAYFINDER_SPRINT_HUNGER, 0.10f}, {com.simplebuilding.util.TrimRates.RAISER_XP, 0.10f},
                {com.simplebuilding.util.TrimRates.HOST_LUCK, 0.5f}, {com.simplebuilding.util.TrimRates.SHAPER_REACH, 0.25f},
                {com.simplebuilding.util.TrimRates.DIAMOND_PHYSICAL, 0.03f}, {com.simplebuilding.util.TrimRates.GOLD_MAGIC, 0.06f},
                {com.simplebuilding.util.TrimRates.LAPIS_MAGIC, 0.04f}, {com.simplebuilding.util.TrimRates.LAPIS_XP, 0.05f},
                {com.simplebuilding.util.TrimRates.IRON_PROJECTILE, 0.05f}, {com.simplebuilding.util.TrimRates.EMERALD_ILLAGER, 0.08f},
                {com.simplebuilding.util.TrimRates.EMERALD_LUCK, 0.5f}, {com.simplebuilding.util.TrimRates.NETHERITE_WITHER_PIERCING, 0.05f},
                {com.simplebuilding.util.TrimRates.QUARTZ_FIRE, 0.05f}, {com.simplebuilding.util.TrimRates.QUARTZ_XP, 0.05f},
                {com.simplebuilding.util.TrimRates.ENDERITE_ALL, 0.05f}, {com.simplebuilding.util.TrimRates.ASTRALIT_PHYSICAL, 0.02f},
                {com.simplebuilding.util.TrimRates.NIHILITH_PHYSICAL, 0.02f}, {com.simplebuilding.util.TrimRates.REDSTONE_SPEED, 0.03f},
                {com.simplebuilding.util.TrimRates.AMETHYST_HEAL_CHANCE, 0.25f}, {com.simplebuilding.util.TrimRates.COPPER_LIGHTNING, 0.10f},
                {com.simplebuilding.util.TrimRates.RESIN_KNOCKBACK_RESISTANCE, 0.025f},
                {com.simplebuilding.util.TrimRates.PATTERN_WEIGHT_NETHERITE, 1.75f}, {com.simplebuilding.util.TrimRates.PATTERN_WEIGHT_ENDERITE, 2.0f},
                {com.simplebuilding.util.TrimRates.NON_PLAYER_MULTIPLIER, 0.2f},
        };
        for (int i = 0; i < table.length; i++) {
            assertClose(helper, table[i][0], table[i][1], "displayed trim rate #" + i + " differs from docs/TRIM-BALANCE.md");
        }
        helper.assertTrue(com.simplebuilding.util.TrimBonusCatalog.forPattern(Identifier.fromNamespaceAndPath("someothermod", "rib_cage")).isEmpty()
                        && com.simplebuilding.util.TrimBonusCatalog.forPattern(Identifier.fromNamespaceAndPath("someothermod", "rib")).isEmpty()
                        && com.simplebuilding.util.TrimBonusCatalog.forMaterial(Identifier.fromNamespaceAndPath("minecraft", "astral")).isEmpty(),
                "the tooltip catalog matched a foreign or look-alike pattern/material");
        helper.assertTrue(com.simplebuilding.util.TrimBonusCatalog.forPattern(Identifier.fromNamespaceAndPath("minecraft", "rib")).size() == 2
                        && com.simplebuilding.util.TrimBonusCatalog.forMaterial(Identifier.fromNamespaceAndPath("simplebuilding", "astralit")).size() == 2,
                "the tooltip catalog lost the exact vanilla/own matches");
        helper.succeed();
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
        setStat(player, Stats.DAMAGE_TAKEN, 0);
        tracker(helper, player).simplebuilding$setActiveTime(0);
        tracker(helper, player).simplebuilding$setBaseValues(0, 0, 0, 0, 0);
        tracker(helper, player).simplebuilding$setBaseXp(0);
        player.totalExperience = 0;
    }

    /**
     * Moves the kill baselines to the current tally plus {@code offset}, which is the only way to
     * take kills back: the tally itself has no setter, {@code awardKillScore} only ever counts up.
     */
    private static void rebaseKills(ServerPlayer player, SurvivalTracerAccessor tracker, int offset) {
        tracker.simplebuilding$setBaseValues(
                stat(player, Stats.WALK_ONE_CM) / 100,
                tracker.simplebuilding$getCurrentTime(),
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
    /** One {@code ServerPlayer#tick} (not the movement tick {@link #tickAt} drives) at a given tick count. */
    private static void serverTickAt(ServerPlayer player, int atTick) {
        player.tickCount = atTick;
        player.tick();
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

    private static Holder<TrimMaterial> material(GameTestHelper helper, ResourceKey<TrimMaterial> key) {
        Registry<TrimMaterial> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        Optional<Holder.Reference<TrimMaterial>> found = registry.get(key);
        helper.assertTrue(found.isPresent(), "the trim material " + key.identifier()
                + " is not in the datapack registry; for a mod material that means its generated "
                + "JSON never reached the jar");
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
     * Enderite trim on Enderite armour is drawn in the darker Enderite palette, the way vanilla draws
     * iron trim on iron armour - on the worn armour and on the item icon alike, because both read
     * the same palette suffix ({@code McVersion#trimColourSuffix}; the icons through
     * {@code ArmorTrimModelProvider}). On every other armour, and for every other material on
     * Enderite armour, the plain palette stays.
     *
     * <p>26.2 keeps the override in the trim material (asset group, so the running registry - the
     * datagen JSON - is checked, and the bootstrap datagen writes it from). 26.3 keeps it in the
     * equipment asset's {@code trim_overrides}, which the client reads from
     * {@code assets/simplebuilding/equipment/enderite.json}; there the shipped JSON has to name the
     * darker palette for the Enderite material, and that palette texture has to exist. The line
     * is told apart by the 26.3 palette folder, which only the 26.3 build ships.
     *
     * <p>What breaks this: dropping the override from {@code ModTrimMaterials} (26.2) or from the
     * overlay equipment JSON (26.3), a stale datagen run, the override keyed to the wrong armour,
     * or the 26.3 suffix rule and the equipment JSON drifting apart.
     */
    public static void enderiteTrimTurnsDarkerOnEnderiteArmour(GameTestHelper helper) {
        var materials = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        TrimMaterial enderite = materials.getOrThrow(ModTrimMaterials.ENDERITE).value();
        TrimMaterial astralit = materials.getOrThrow(ModTrimMaterials.ASTRALIT).value();
        TrimMaterial iron = materials.getOrThrow(TrimMaterials.IRON).value();
        var enderiteArmour = com.simplebuilding.items.ModArmorMaterials.ENDERITE_ASSET_KEY;
        var ironArmour = net.minecraft.world.item.equipment.EquipmentAssets.IRON;

        String actual = "enderite on enderite " + McVersion.trimColourSuffix(enderite, enderiteArmour)
                + ", enderite on iron " + McVersion.trimColourSuffix(enderite, ironArmour)
                + ", astralit on enderite " + McVersion.trimColourSuffix(astralit, enderiteArmour)
                + ", iron on enderite " + McVersion.trimColourSuffix(iron, enderiteArmour)
                + ", declared enderite on enderite "
                + McVersion.trimColourSuffix(bootstrapped().get(ModTrimMaterials.ENDERITE), enderiteArmour);
        helper.assertValueEqual(actual, "enderite on enderite enderite_darker, enderite on iron enderite, "
                        + "astralit on enderite astralit, iron on enderite iron, declared enderite on enderite enderite_darker",
                "the trim palette of each material on each armour");

        ClassLoader loader = TrimWiringTests.class.getClassLoader();
        if (loader.getResource("assets/simplebuilding/textures/palettes/trim/enderite.png") != null) {
            helper.assertTrue(loader.getResource("assets/simplebuilding/textures/palettes/trim/enderite_darker.png") != null,
                    "26.3 ships no simplebuilding:trim/enderite_darker palette texture");
            String overrides = "none";
            try (java.io.InputStream in = loader.getResourceAsStream("assets/simplebuilding/equipment/enderite.json")) {
                if (in != null) {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(
                            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    overrides = json.has("trim_overrides") ? json.get("trim_overrides").toString() : "none";
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            helper.assertValueEqual(overrides,
                    "[{\"palette\":\"simplebuilding:trim/enderite_darker\",\"when\":{\"material\":\"simplebuilding:enderite\"}}]",
                    "the trim_overrides of the Enderite equipment asset on 26.3");
        }
        helper.succeed();
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

        // register(k, v, Lifecycle) is abstract on 26.2, register(k, v) and listContextElements on
        // 26.3 - all three are implemented, none with @Override, so one source fits both.
        public Holder.Reference<TrimMaterial> register(ResourceKey<TrimMaterial> key, TrimMaterial value) {
            return register(key, value, Lifecycle.stable());
        }

        public <S> java.util.stream.Stream<Holder.Reference<S>> listContextElements(
                ResourceKey<? extends Registry<? extends S>> key) {
            throw new UnsupportedOperationException("not used by ModTrimMaterials.bootstrap");
        }

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
