package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.TrimContracts;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilities;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilityAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.serialization.Lifecycle;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
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
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * MOB27-3 / ZO9 A (docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md): moved from the SimpleBuilding core test class {@code TrimWiringTests (the four mixin-delivery tests)} together with
 * the trim abilities, bodies unchanged except for the inputs (see the helpers at the end); each method comment says what it pins.
 */
final class TrimHookRuleTests {
    private TrimHookRuleTests() {
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

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("trim_hook_the_player_mixin_delivers_speed_hunger_and_experience_behind_its_guards", TrimHookRuleTests::thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards);
        add.accept("trim_hook_every_server_side_hit_runs_through_the_trim_damage_modifier", TrimHookRuleTests::everyServerSideHitRunsThroughTheTrimDamageModifier);
        add.accept("trim_hook_coast_holds_the_air_supply_and_silence_lowers_the_visibility", TrimHookRuleTests::coastHoldsTheAirSupplyAndSilenceLowersTheVisibility);
        add.accept("trim_hook_the_tick_driven_trim_effects_fire_on_their_own_cadence", TrimHookRuleTests::theTickDrivenTrimEffectsFireOnTheirOwnCadence);
    }

    /**
     * The deliveries that ride on the player: walking speed, swimming, luck, block reach and
     * knockback resistance as vanilla attribute modifiers (TrimAttributeHandler), hunger and
     * experience through {@code PlayerEntityMixin} - each together with the guard that decides when
     * it applies.
     *
     * <p>Since 2026-09 the movement bonuses are no longer bent into {@code getSpeed}; they are
     * transient modifiers on {@code movement_speed} (Bolt, Redstone - players only) and on
     * {@code water_movement_efficiency} (Tide - every wearer, so it works without Depth Strider).
     * The test first proves the handler is what delivers them (nothing moves before it has run),
     * then that {@code LivingEntity#tick} runs it on its own within one 10 tick period, that a
     * non-player wearer gets the tide modifier but no walking bonus, and that switching the trim
     * benefits off takes every modifier away again.
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
     * <p>What breaks this: the attribute handler not running from {@code LivingEntity#tick}, a
     * modifier with the wrong attribute, amount or operation, the walking bonus reaching non-players,
     * the benefit switch not clearing the modifiers, the hunger or experience injections leaving the
     * mixin config, the sprint condition on the exhaustion reduction, the rounding of the experience
     * gain, or the guard against non-positive gains.
     */
    static void thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
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

            // --- walking speed: a movement_speed modifier, delivered by TrimAttributeHandler ---
            bare(player);
            TrimAbilityAttributes.update(player);
            float bareSpeed = player.getSpeed();
            helper.assertTrue(bareSpeed > 0.0F,
                    "the mock player's movement speed is " + bareSpeed + ", so multiplying it could "
                            + "not be detected either way");
            wear(player, copper, bolt, 4);
            float landMultiplier = TrimAbilities.getLandSpeedMultiplier(player);
            helper.assertTrue(landMultiplier > 1.0F,
                    "test setup broken: a full bolt set is worth a land speed multiplier of "
                            + landMultiplier);
            assertClose(helper, player.getSpeed(), bareSpeed,
                    "the walking bonus arrived before the attribute handler ran, so something other "
                            + "than the movement_speed modifier is still bending getSpeed");
            // LivingEntity#tick runs the handler on its own, once per 10 tick period.
            for (int tick = 0; tick < TrimAbilityAttributes.INTERVAL; tick++) {
                tickAt(player, 300 + tick);
            }
            assertClose(helper, player.getSpeed(), bareSpeed * landMultiplier,
                    "Player.getSpeed did not pick up the bolt trim bonus within one attribute period; "
                            + "the movement_speed modifier is not being set by LivingEntity#tick");
            AttributeModifier walking = player.getAttribute(Attributes.MOVEMENT_SPEED)
                    .getModifier(TrimAbilityAttributes.WALKING_SPEED_ID);
            helper.assertTrue(walking != null
                            && walking.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                            && Math.abs(walking.amount() - (landMultiplier - 1.0F)) < 1.0e-5,
                    "the walking bonus is not the vanilla modifier it is documented as: " + walking);

            // --- swimming (tide): water_movement_efficiency, the attribute Depth Strider raises ---
            wear(player, copper, tide, 4);
            TrimAbilityAttributes.update(player);
            float swimBonus = TrimAbilities.getSwimSpeedMultiplier(player) - 1.0F;
            helper.assertTrue(swimBonus > 0.0F,
                    "test setup broken: a full tide set is worth a swim bonus of " + swimBonus);
            assertClose(helper, player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY), swimBonus,
                    "a full tide set did not raise water_movement_efficiency, so tide still does "
                            + "nothing without Depth Strider");
            assertClose(helper, player.getSpeed(), bareSpeed,
                    "the walking bonus of the bolt set survived taking the bolt set off");

            // --- luck (host), block reach (shaper), knockback resistance (resin) ---
            wear(player, copper, pattern(helper, TrimPatterns.HOST), 4);
            TrimAbilityAttributes.update(player);
            assertClose(helper, player.getAttributeValue(Attributes.LUCK), TrimAbilities.getLuckBonus(player),
                    "the host luck bonus is not a luck attribute modifier");
            double bareReach = player.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE);
            wear(player, copper, pattern(helper, TrimPatterns.SHAPER), 4);
            TrimAbilityAttributes.update(player);
            assertClose(helper, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), bareReach + 1.0,
                    "a full shaper set at resonance 1.0 is worth 4 x 0.25 blocks of reach");
            wear(player, material(helper, TrimMaterials.RESIN), pattern(helper, TrimPatterns.SHAPER), 4);
            TrimAbilityAttributes.update(player);
            assertClose(helper, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.1,
                    "a full resin set at resonance 1.0 is worth 4 x 0.025 knockback resistance");

            // --- the benefit switch takes every modifier away again ---
            Gate gate = gate(player);
            gate.simplebuilding$setTrimBenefitsEnabled(false);
            TrimAbilityAttributes.update(player);
            assertClose(helper, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0,
                    "the knockback modifier survived the trim benefit switch");
            assertClose(helper, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), bareReach,
                    "the reach modifier survived the trim benefit switch");
            gate.simplebuilding$setTrimBenefitsEnabled(true);

            // --- a non-player wearer: tide yes, walking bonus no (that one is for players) ---
            ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
            wear(stand, copper, tide, 4);
            TrimAbilityAttributes.update(stand);
            assertClose(helper, stand.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY),
                    TrimAbilities.getSwimSpeedMultiplier(stand) - 1.0F,
                    "a tide-trimmed armour stand did not get the water_movement_efficiency modifier");
            wear(stand, copper, bolt, 4);
            TrimAbilityAttributes.update(stand);
            helper.assertTrue(stand.getAttribute(Attributes.MOVEMENT_SPEED)
                            .getModifier(TrimAbilityAttributes.WALKING_SPEED_ID) == null,
                    "a bolt-trimmed armour stand got the walking speed modifier; it is a player bonus");
            bare(stand);

            // --- exhaustion, and the sprint condition on it ---
            wear(player, copper, wayfinder, 4);
            float reduction = TrimAbilities.getExhaustionReduction(player);
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
            helper.assertTrue(TrimAbilities.getXPMultiplier(player) > 1.0F,
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
            restore(configuredBase);
        }
    }

    /**
     * A real hit on a real player, measured on the health bar rather than by calling
     * {@code TrimAbilities.modifyDamage} directly.
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
    static void everyServerSideHitRunsThroughTheTrimDamageModifier(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            player.getAbilities().invulnerable = false;
            player.setPermanentlyInvulnerable(false);
            // Without this ServerPlayer#isInvulnerableTo keeps returning true for 60 ticks.
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            player.setAbsorptionAmount(0.0F);

            Holder<TrimMaterial> enderite = material(helper, SB_ENDERITE);
            Holder<TrimPattern> ward = pattern(helper, TrimPatterns.WARD);
            DamageSource armourPiercing = helper.getLevel().damageSources().flyIntoWall();
            pinProgressMultiplier(helper, player, 1.0);

            // --- the control: an untrimmed set loses the full hit ---
            wear(player, enderite, ward, 4);
            Gate gate = gate(player);
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
            float expected = TrimAbilities.modifyDamage(player, HIT_DAMAGE, armourPiercing);
            helper.assertTrue(expected < HIT_DAMAGE - 0.5F,
                    "test setup broken: a full enderite ward set only takes " + (HIT_DAMAGE - expected)
                            + " points off a " + HIT_DAMAGE + " point hit, which is too little to "
                            + "measure on the health bar");
            float protectedLoss = damageTaken(helper, player, armourPiercing);
            assertClose(helper, protectedLoss, expected,
                    "the player lost " + protectedLoss + " health where TrimAbilities.modifyDamage "
                            + "says " + expected + "; the reduction is being computed but hurtServer "
                            + "is not going through it");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * The two remaining {@code LivingEntityMixin} deliveries that are not about damage: the coast
     * trim holding the air supply under water, and the silence trim lowering how visible the wearer
     * is to the mobs looking for them.
     *
     * <p>Air is the interesting one because the mixin rolls a die for it, and the chance is capped
     * at 75% (Respiration III), so no set makes it deterministic any more. The whole air path is
     * driven for real instead, a hundred times: water is placed around the player's eyes and
     * {@code baseTick} is called, which is where vanilla decides to spend a point of air. The
     * control tick without the trim has to lose its point, otherwise the measurement would be about
     * the player not being submerged. With the full set a hundred ticks have to cost more than
     * nothing (the saving is not unconditional) and far fewer than a hundred points: expected are
     * 25, the bounds 1..60 are each missed with a probability below 1e-12.
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
    static void coastHoldsTheAirSupplyAndSilenceLowersTheVisibility(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            // The drowning branch is skipped entirely for an invulnerable player.
            player.getAbilities().invulnerable = false;
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimMaterial> enderite = material(helper, SB_ENDERITE);
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
            float chance = TrimAbilities.getAirSaveChance(player);
            helper.assertTrue(Math.abs(chance - 0.75F) < 1.0e-6F,
                    "test setup broken: the coast air chance is " + chance + " instead of its 75% cap, "
                            + "so the bounds below no longer fit");
            player.setAirSupply(START_AIR);
            for (int tick = 0; tick < 100; tick++) {
                player.baseTick();
            }
            int spent = START_AIR - player.getAirSupply();
            helper.assertTrue(spent >= 1 && spent <= 60,
                    "a full coast set (75% air saving) spent " + spent + " of 100 air points under "
                            + "water; expected about 25 - either decreaseAirSupply is no longer going "
                            + "through the mixin or the saving became unconditional");

            // --- visibility: same armour in both readings, only the pattern differs ---
            wear(player, copper, blank, 4);
            double plainVisibility = player.getVisibilityPercent((net.minecraft.server.level.ServerLevel) player.level(), null);
            helper.assertTrue(plainVisibility > 0.0,
                    "the wearer is already invisible to everything at " + plainVisibility
                            + ", so multiplying it could not be detected");
            wear(player, copper, silence, 4);
            float stealth = TrimAbilities.getStealthMultiplier(player);
            helper.assertTrue(stealth < 1.0F && stealth > 0.0F,
                    "test setup broken: a full silence set is worth a stealth factor of " + stealth);
            assertClose(helper, player.getVisibilityPercent((net.minecraft.server.level.ServerLevel) player.level(), null), plainVisibility * stealth,
                    "LivingEntity.getVisibilityPercent did not pick the silence trim up; mobs still "
                            + "spot the wearer from just as far away");
            wear(player, copper, blank, 4);
            assertClose(helper, player.getVisibilityPercent((net.minecraft.server.level.ServerLevel) player.level(), null), plainVisibility,
                    "an untrimmed wearer's visibility was modified as well");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
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
    static void theTickDrivenTrimEffectsFireOnTheirOwnCadence(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
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
            int reduction = TrimAbilities.getWitherReductionAmount(player);
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
            float healChance = TrimAbilities.getAmethystHealChance(player);
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
            restore(configuredBase);
        }
    }

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

    /** Health lost to one hit, with the damage cooldown cleared so repeated hits all land. */
    private static float damageTaken(GameTestHelper helper, ServerPlayer player, DamageSource source) {
        player.setHealth(player.getMaxHealth());
        player.setInvulnerableTime(0);
        player.damageCooldownTime = 0;
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
        float multiplier = TrimAbilities.getXPMultiplier(player);
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

    private static Holder<TrimPattern> pattern(GameTestHelper helper, ResourceKey<TrimPattern> key) {
        Registry<TrimPattern> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        Optional<Holder.Reference<TrimPattern>> found = registry.get(key);
        helper.assertTrue(found.isPresent(),
                "the trim pattern " + key.identifier() + " is not in the datapack registry");
        return found.orElseThrow();
    }

    /**
     * Several of the values below are {@code float} internally, so a double-tight epsilon would
     * fail on rounding alone; 1.0e-4 is still far below every step this class asserts.
     */
    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-4,
                what + ": expected " + expected + " but got " + actual);
    }

    // ---------------------------------------------------------------- MOB27-3 / ZO9 A test inputs
    // The bodies above came from the SimpleBuilding core (docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md). There the resonance was
    // pinned through the core config (trimBenefitBaseMultiplier) and the player switch through TrimBenefitUser; here both go through a
    // test host (framework TrimContracts.Host), which works with and without SimpleBuilding loaded and puts the old host back.

    /** Players switched off through {@link #gate}; cleared when a body restores its host. */
    private static final java.util.Set<java.util.UUID> OFF = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The test host: a pinned resonance for players (NaN = ask the previous host), the player switch, the rest from the previous host. */
    private static final class Pinned implements TrimContracts.Host {
        final double target;
        final TrimContracts.Host previous;

        Pinned(double target, TrimContracts.Host previous) {
            this.target = target;
            this.previous = previous;
        }

        @Override
        public double multiplier(Object livingEntity) {
            if (livingEntity instanceof net.minecraft.world.entity.player.Player && !Double.isNaN(target)) return target;
            return previous == null ? Double.NaN : previous.multiplier(livingEntity);
        }

        @Override
        public float strength(String bonusKey) {
            return previous == null ? 1.0f : previous.strength(bonusKey);
        }

        @Override
        public boolean enabled(Object livingEntity) {
            if (livingEntity instanceof net.minecraft.world.entity.player.Player p && OFF.contains(p.getUUID())) return false;
            return previous == null || previous.enabled(livingEntity);
        }
    }

    private static TrimContracts.Host unpinned() {
        TrimContracts.Host current = TrimContracts.host();
        return current instanceof Pinned pinned ? pinned.previous : current;
    }

    /** Puts the host a body started with back (and every player switch on). */
    private static void restore(TrimContracts.Host host) {
        TrimContracts.setHost(host);
        OFF.clear();
    }

    /** Pins the resonance of every player to {@code target} and checks that the abilities read exactly that. */
    private static void pinProgressMultiplier(GameTestHelper helper, ServerPlayer player, double target) {
        TrimContracts.setHost(new Pinned(target, unpinned()));
        double actual = TrimAbilities.resonance(player);
        helper.assertTrue(Math.abs(actual - target) < 1.0e-6,
                "the test host no longer feeds the resonance: wanted " + target + " but got " + actual);
    }

    /** The player's trim switch (formerly the core's TrimBenefitUser on the player), now the host's {@code enabled}. */
    private static Gate gate(ServerPlayer player) {
        return new Gate(player.getUUID());
    }

    private record Gate(java.util.UUID id) {
        boolean simplebuilding$areTrimBenefitsEnabled() {
            return !OFF.contains(id);
        }

        void simplebuilding$setTrimBenefitsEnabled(boolean enabled) {
            TrimContracts.Host current = TrimContracts.host();
            if (!(current instanceof Pinned)) TrimContracts.setHost(new Pinned(Double.NaN, current));
            if (enabled) OFF.remove(id);
            else OFF.add(id);
        }
    }

    // The SimpleBuilding trim materials by key: Simple Mobs reads only the key (getMaterialCount/getTrimCount), so without
    // SimpleBuilding a stand-alone holder with that key stands in (see material()).
    private static final ResourceKey<TrimMaterial> SB_ASTRALIT = sbMaterial("astralit");
    private static final ResourceKey<TrimMaterial> SB_NIHILITH = sbMaterial("nihilith");
    private static final ResourceKey<TrimMaterial> SB_ENDERITE = sbMaterial("enderite");

    private static ResourceKey<TrimMaterial> sbMaterial(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("simplebuilding", name));
    }

    private static Holder<TrimMaterial> material(GameTestHelper helper, ResourceKey<TrimMaterial> key) {
        Registry<TrimMaterial> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        Optional<Holder.Reference<TrimMaterial>> found = registry.get(key);
        if (found.isPresent()) return found.get();
        helper.assertTrue(key.identifier().getNamespace().equals("simplebuilding"), "the trim material " + key.identifier()
                + " is not in the datapack registry");
        // Simple Mobs alone: the SimpleBuilding material is not registered; the abilities read only its key.
        return Holder.Reference.createStandAlone(registry, key);
    }
}
