package com.simplebuilding.gametest;

import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.trim.ModTrimMaterials;
import com.simplebuilding.util.TrimBenefitUser;
import com.simplebuilding.util.TrimEffectUtil;
import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.phys.Vec3;

/**
 * What an armour trim actually <em>does</em> to the wearer.
 *
 * <p>{@link HopperAndTrimTests} already pins the multiplier curve - how strong a benefit gets.
 * It says nothing about whether a benefit exists: every number in {@code TrimEffectUtil} could
 * be deleted and that test would still pass. These tests put trimmed armour on a mock player
 * and read the effect back out, and one of them reads it back out through the mixins, which is
 * the only way to notice that {@code PlayerEntityMixin} has fallen out of the mixin config.
 *
 * <p>Four things are pinned deliberately rather than incidentally:
 * <ul>
 *   <li>{@code TrimEffectUtil} matches patterns with {@link String#contains} against the
 *       pattern's <em>asset path</em>, and materials the same way against the material's
 *       registry-key path. The namespace is dropped before the comparison, so a foreign
 *       {@code someothermod:rib_cage} counts as a rib trim. That is a known weakness; the test
 *       pins the current behaviour so that changing it is a deliberate act with a failing test
 *       attached, not a silent one.</li>
 *   <li>Every bonus is neutral (1.0x, or 0) on a player wearing nothing. A regression that made
 *       these non-neutral would buff every player in the world, trim or no trim.</li>
 *   <li>The damage multiplier has a 0.1 floor. Without it a well-levelled player in a full
 *       trimmed set becomes immortal, which is a balance bug no crash report would ever show.</li>
 *   <li>A non-player {@link LivingEntity} gets the flat 0.2 fallback progress factor rather than
 *       the player curve. Trims work on mobs too - {@code LivingEntityMixin} routes every
 *       {@code hurtServer} through {@code modifyDamage} - so that constant is live gameplay.</li>
 * </ul>
 *
 * <p>Test bodies here mutate the static {@code trimBenefitBaseMultiplier} to pin the progress
 * factor to an exact value, and restore it in a {@code finally}. That is safe only because each
 * body runs to completion inside a single server tick - no test may be made to span ticks
 * without moving that calibration somewhere else. It is also why the counting test below does
 * not touch the config at all: counting does not consult the multiplier, so calibrating it there
 * would only add a way for the test to lie.
 */
public final class TrimEffectTests {

    private TrimEffectTests() {
    }

    /** The four slots {@code TrimEffectUtil} walks, and a carrier item for each. */
    private static final EquipmentSlot[] ARMOUR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Chainmail on purpose: the mod reads the trim <em>material</em>, never the armour item, and
     * "chainmail" collides with none of the material names {@code TrimEffectUtil} looks for. Iron
     * or diamond armour would make a reader wonder which of the two the counts came from.
     */
    private static final Item[] ARMOUR_ITEMS = {
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
            Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
    };

    /**
     * The inert carrier pattern. {@code shaper} is the one vanilla pattern whose asset path is
     * not a substring query anywhere in {@code TrimEffectUtil} - not in {@code modifyDamage},
     * not in any getter. Wearing it means whatever a test then measures came from the trim
     * <em>material</em> alone. If a future pattern bonus is keyed on "shaper", the tests that
     * use it as a blank will start failing, which is the correct outcome.
     */
    private static ResourceKey<TrimPattern> inertPattern() {
        return TrimPatterns.SHAPER;
    }

    // =====================================================================================
    // COUNTING: WHICH TRIMS COUNT, AND FOR HOW MUCH
    // =====================================================================================

    /**
     * Every benefit in the mod is a count multiplied by a rate, so the count is the one number
     * that can break all of them at once.
     *
     * <p>Pinned here: an armour piece scores 1.0, a netherite-trimmed one 1.75 and an
     * enderite-trimmed one 3.5; only worn humanoid armour is looked at, not what is in the hand;
     * and the matching is a substring test on the pattern's asset path, so a foreign namespace
     * and a longer path both still match. The near-miss pair netherite/enderite is checked in
     * both directions because the material scoring tests for "enderite" first, and a scoring
     * branch that started matching loosely would double every netherite trim in the game.
     *
     * <p>This body also fails if {@code simplebuilding:astralit} or {@code :enderite} are missing
     * from the running datapack registry, which is what a dropped datagen output looks like from
     * the inside.
     */
    public static void trimCountsFollowThePatternAndMaterialMatching(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
        Holder<TrimMaterial> netherite = material(helper, TrimMaterials.NETHERITE);
        Holder<TrimMaterial> enderite = material(helper, ModTrimMaterials.ENDERITE);
        Holder<TrimMaterial> astralit = material(helper, ModTrimMaterials.ASTRALIT);
        Holder<TrimPattern> rib = pattern(helper, TrimPatterns.RIB);
        Holder<TrimPattern> ward = pattern(helper, TrimPatterns.WARD);

        // --- nothing worn: nothing counted ---
        bare(player);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 0.0,
                "a naked player scored a rib trim");
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "copper") == 0,
                "a naked player scored a copper trim material");

        // --- plain material: one point per piece ---
        wear(player, copper, rib, 1);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 1.0, "one copper rib piece");
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "copper") == 1,
                "one copper piece was not counted as one material");

        wear(player, copper, rib, 4);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 4.0, "a full copper rib set");
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "copper") == 4,
                "a full copper set was not counted as four materials");

        // --- the material weighting, which is what makes the good materials worth crafting ---
        wear(player, netherite, rib, 2);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 3.5,
                "two netherite rib pieces (1.75 each)");
        wear(player, enderite, rib, 2);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 7.0,
                "two enderite rib pieces (3.5 each)");

        // The scoring checks for "enderite" before "netherite". Neither name contains the other,
        // and if that ever stops being true every netherite trim silently doubles in strength.
        wear(player, netherite, rib, 4);
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "enderite") == 0,
                "a netherite trim was counted as enderite");
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "netherite") == 4,
                "a netherite trim was not counted as netherite");
        wear(player, enderite, rib, 4);
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "netherite") == 0,
                "an enderite trim was counted as netherite");
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 14.0,
                "a full enderite rib set");

        // --- mixed pieces add up independently ---
        ArmorTrim copperRib = new ArmorTrim(copper, rib);
        ArmorTrim netheriteRib = new ArmorTrim(netherite, rib);
        bare(player);
        player.setItemSlot(EquipmentSlot.HEAD, trimmed(ARMOUR_ITEMS[0], copperRib));
        player.setItemSlot(EquipmentSlot.CHEST, trimmed(ARMOUR_ITEMS[1], netheriteRib));
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 2.75,
                "a copper piece plus a netherite piece");

        // --- only worn armour counts; a trimmed chestplate in the hand must not ---
        bare(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, trimmed(ARMOUR_ITEMS[1], copperRib));
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 0.0,
                "a trimmed chestplate carried in the hand was counted as if worn");
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "copper") == 0,
                "a trimmed chestplate carried in the hand was counted as a worn material");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // --- a pattern that does not contain the query scores nothing ---
        wear(player, copper, ward, 4);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 0.0,
                "a ward set was counted as rib");
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "ward"), 4.0, "a full ward set");

        // --- the known weakness, pinned on purpose ---
        // Matching is String.contains against the pattern's asset PATH, with the namespace
        // dropped first. So another mod's "someothermod:rib_cage" counts as a rib trim, and
        // "astral" matches the material "astralit". If matching ever becomes exact, these two
        // assertions are what tells you - and they tell you in the message, not in a stack trace.
        Holder<TrimPattern> foreignRibCage = Holder.direct(new TrimPattern(
                Identifier.fromNamespaceAndPath("someothermod", "rib_cage"),
                Component.literal("Rib Cage"), false));
        wear(player, copper, foreignRibCage, 1);
        assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 1.0,
                "someothermod:rib_cage stopped counting as a rib trim - pattern matching is a "
                        + "substring test on the asset path with the namespace dropped; if that "
                        + "changed on purpose, update this test");
        wear(player, astralit, ward, 4);
        helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "astral") == 4,
                "the material match stopped being a substring test; \"astral\" no longer finds "
                        + "\"astralit\". If that changed on purpose, update this test");

        bare(player);
        helper.succeed();
    }

    // =====================================================================================
    // DAMAGE REDUCTION
    // =====================================================================================

    /**
     * The reductions in {@code modifyDamage}, how a pattern bonus and a material bonus stack,
     * and the two guards at either end of the calculation.
     *
     * <p>Rib only applies to wither damage and ward applies to everything - that split is the
     * whole design of the pattern bonuses, and losing it turns every trim into a flat global
     * armour bonus. Enderite is the material counterpart: 5% per piece off <em>every</em> damage
     * type, checked against two unrelated sources so that a stray damage-type guard around it
     * would show up.
     *
     * <p>The floor is checked with an absurd progress factor: whatever the player's level, at
     * least 10% of every hit has to land, otherwise a trimmed set is immortality. The guard at
     * the other end is checked with a negative amount - without {@code amount <= 0} a reduction
     * would scale incoming healing into more healing.
     *
     * <p>Copper carries the pattern measurements because {@code modifyDamage} has no copper
     * branch, so what is measured there is the pattern alone; {@code shaper} carries the
     * material measurements for the mirror-image reason.
     */
    public static void damageReductionFollowsThePatternAndKeepsItsFloor(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimMaterial> enderite = material(helper, ModTrimMaterials.ENDERITE);
            Holder<TrimPattern> rib = pattern(helper, TrimPatterns.RIB);
            Holder<TrimPattern> ward = pattern(helper, TrimPatterns.WARD);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());

            DamageSource wither = helper.getLevel().damageSources().wither();
            DamageSource fire = helper.getLevel().damageSources().onFire();
            DamageSource generic = helper.getLevel().damageSources().generic();

            pinProgressMultiplier(helper, player, 1.0);

            // --- no trim: the hit is passed through untouched ---
            bare(player);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 10.0,
                    "an untrimmed player took reduced wither damage");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, generic), 10.0,
                    "an untrimmed player took reduced generic damage");

            // --- rib: 10% per piece, but only against wither ---
            wear(player, copper, rib, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 6.0,
                    "a full rib set against wither damage");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, fire), 10.0,
                    "a rib set reduced fire damage, which is the snout pattern's job");

            // --- ward: 3% per piece against everything, no damage type check at all ---
            wear(player, copper, ward, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, generic), 8.8,
                    "a full ward set against a generic hit");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, fire), 8.8,
                    "a full ward set against fire - ward is the unconditional one");

            // --- enderite as a MATERIAL: 5% per piece, also unconditional ---
            // The shaper pattern contributes nothing, so this is the material branch alone.
            wear(player, enderite, blank, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, generic), 8.0,
                    "a full enderite set against a generic hit (4 x 5%)");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 8.0,
                    "the enderite material reduction picked up a damage type condition; it is "
                            + "meant to apply to everything");

            // --- pattern and material stack, and the material also weights the pattern count ---
            // 4 enderite ward pieces: the ward count is 4 x 3.5 = 14, so 14 x 3% = 42% from the
            // pattern, plus 4 x 5% = 20% from the material. Both halves have to be there.
            wear(player, enderite, ward, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, generic), 3.8,
                    "an enderite ward set: 42% from the pattern (a count of 14, because enderite "
                            + "pieces score 3.5 each) plus 20% from the material");

            // --- the floor: reductions may never take more than 90% of the hit ---
            wear(player, copper, rib, 4);
            pinProgressMultiplier(helper, player, 10.0);
            // 4 pieces x 10% x 10 = 400% off, which would heal the player without the clamp.
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 1.0,
                    "the 10% damage floor is gone - a trimmed player can now be made immortal");

            // --- and the guard at the other end ---
            // 0.0F would come back as 0.0F with or without the guard, so it proves nothing; a
            // negative amount is the case the guard is actually there for.
            helper.assertTrue(TrimEffectUtil.modifyDamage(player, -4.0F, wither) == 0.0F,
                    "a negative damage amount was scaled instead of clamped to 0 - without the "
                            + "amount <= 0 guard, a reduction turns incoming healing into more "
                            + "healing, and it got "
                            + TrimEffectUtil.modifyDamage(player, -4.0F, wither));

            // --- a mob is not a player: it gets the flat 0.2 fallback, not the player curve ---
            // The configured base is still cranked up to pin the player at 10.0 here, so if the
            // fallback ever started consulting the config this assertion moves a long way.
            ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
            assertClose(helper, TrimEffectUtil.getGlobalMultiplier(stand), 0.2,
                    "a non-player LivingEntity no longer gets the flat 0.2 progress factor");
            wear(stand, copper, rib, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(stand, 10.0F, wither), 9.2,
                    "a trimmed mob took the player-sized reduction (4 x 10% x 0.2 = 8% expected)");
            bare(stand);

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // THE UTILITY BONUSES
    // =====================================================================================

    /**
     * The nine getters the mixins read for movement, breath, hunger, experience, luck, stealth,
     * wither cleansing and healing. Each is checked twice: neutral on a bare player, and at its
     * documented rate on a full four-piece set at a progress factor of exactly 1.0.
     *
     * <p>The neutral half is the important one. These getters run on every player every tick;
     * if one of them ever returned a bonus for armour that is not there, every player in the
     * world would silently get it.
     *
     * <p>Three of the getters add a pattern contribution and a material contribution together,
     * and those are checked with both halves present at once, because a set that only ever wore
     * one of the two would pass with the other half deleted.
     *
     * <p>The two clamps are checked separately: exhaustion reduction may not exceed 100% (a
     * player who never gets hungry) and stealth may not go below 0 (which would flip into
     * <em>more</em> visible, since the mixin multiplies with it).
     */
    public static void utilityBonusesAreNeutralUntilTheMatchingTrimIsWorn(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());

            pinProgressMultiplier(helper, player, 1.0);

            // --- bare: every single one of them neutral ---
            bare(player);
            assertClose(helper, TrimEffectUtil.getSwimSpeedMultiplier(player), 1.0,
                    "swim speed was not neutral without a tide trim");
            assertClose(helper, TrimEffectUtil.getLandSpeedMultiplier(player), 1.0,
                    "land speed was not neutral without a bolt trim");
            assertClose(helper, TrimEffectUtil.getExhaustionReduction(player), 0.0,
                    "exhaustion was reduced without a wayfinder trim");
            assertClose(helper, TrimEffectUtil.getXPMultiplier(player), 1.0,
                    "experience was multiplied without a raiser trim");
            assertClose(helper, TrimEffectUtil.getLuckBonus(player), 0.0,
                    "luck was granted without a host trim");
            assertClose(helper, TrimEffectUtil.getStealthMultiplier(player), 1.0,
                    "stealth was granted without a silence trim");
            assertClose(helper, TrimEffectUtil.getAirSaveChance(player), 0.0,
                    "air was saved without a coast trim");
            helper.assertTrue(TrimEffectUtil.getWitherReductionAmount(player) == 0,
                    "wither was shortened without a rib trim");
            assertClose(helper, TrimEffectUtil.getAmethystHealChance(player), 0.0,
                    "healing was granted without an amethyst trim");

            // --- one full set per pattern, at progress 1.0 ---
            wear(player, copper, pattern(helper, TrimPatterns.TIDE), 4);
            assertClose(helper, TrimEffectUtil.getSwimSpeedMultiplier(player), 1.4,
                    "tide swim speed (4 x 10%)");

            wear(player, copper, pattern(helper, TrimPatterns.WAYFINDER), 4);
            assertClose(helper, TrimEffectUtil.getExhaustionReduction(player), 0.4,
                    "wayfinder exhaustion reduction (4 x 10%)");

            wear(player, copper, pattern(helper, TrimPatterns.RAISER), 4);
            assertClose(helper, TrimEffectUtil.getXPMultiplier(player), 1.4,
                    "raiser experience bonus (4 x 10%)");

            wear(player, copper, pattern(helper, TrimPatterns.HOST), 4);
            assertClose(helper, TrimEffectUtil.getLuckBonus(player), 4.0,
                    "host luck bonus (4 x 1.0)");

            wear(player, copper, pattern(helper, TrimPatterns.SILENCE), 4);
            assertClose(helper, TrimEffectUtil.getStealthMultiplier(player), 0.4,
                    "silence stealth factor (1.0 - 4 x 15%)");

            wear(player, copper, pattern(helper, TrimPatterns.COAST), 4);
            assertClose(helper, TrimEffectUtil.getAirSaveChance(player), 0.8,
                    "coast air save chance (4 x 20%)");

            wear(player, copper, pattern(helper, TrimPatterns.RIB), 4);
            helper.assertTrue(TrimEffectUtil.getWitherReductionAmount(player) == 160,
                    "rib wither reduction should be 4 x 40 ticks, it is "
                            + TrimEffectUtil.getWitherReductionAmount(player));

            wear(player, copper, pattern(helper, TrimPatterns.BOLT), 4);
            assertClose(helper, TrimEffectUtil.getLandSpeedMultiplier(player), 1.2,
                    "bolt land speed (4 x 5%)");

            // --- and the material-driven ones, on a pattern that contributes nothing ---
            wear(player, material(helper, TrimMaterials.REDSTONE), blank, 4);
            assertClose(helper, TrimEffectUtil.getLandSpeedMultiplier(player), 1.12,
                    "redstone land speed (4 x 3%)");

            wear(player, material(helper, TrimMaterials.AMETHYST), blank, 4);
            assertClose(helper, TrimEffectUtil.getAmethystHealChance(player), 1.0,
                    "amethyst heal chance (4 x 25%)");

            wear(player, material(helper, TrimMaterials.EMERALD), blank, 4);
            assertClose(helper, TrimEffectUtil.getLuckBonus(player), 2.0,
                    "emerald luck bonus (4 x 0.5)");

            wear(player, material(helper, TrimMaterials.LAPIS), blank, 4);
            assertClose(helper, TrimEffectUtil.getXPMultiplier(player), 1.2,
                    "lapis experience bonus (4 x 5%)");

            // --- the three getters that add a pattern and a material together ---
            // Wearing only one half at a time would let the other half be deleted unnoticed.
            wear(player, material(helper, TrimMaterials.REDSTONE), pattern(helper, TrimPatterns.BOLT), 4);
            assertClose(helper, TrimEffectUtil.getLandSpeedMultiplier(player), 1.32,
                    "bolt and redstone together (4 x 5% + 4 x 3%) - one of the two halves is gone");

            wear(player, material(helper, TrimMaterials.LAPIS), pattern(helper, TrimPatterns.RAISER), 4);
            assertClose(helper, TrimEffectUtil.getXPMultiplier(player), 1.6,
                    "raiser and lapis together (4 x 10% + 4 x 5%) - one of the two halves is gone");

            wear(player, material(helper, TrimMaterials.EMERALD), pattern(helper, TrimPatterns.HOST), 4);
            assertClose(helper, TrimEffectUtil.getLuckBonus(player), 6.0,
                    "host and emerald together (4 x 1.0 + 4 x 0.5) - one of the two halves is gone");

            // --- the clamps ---
            pinProgressMultiplier(helper, player, 5.0);
            wear(player, copper, pattern(helper, TrimPatterns.WAYFINDER), 4);
            assertClose(helper, TrimEffectUtil.getExhaustionReduction(player), 1.0,
                    "exhaustion reduction broke through 100% - the player would never get hungry");
            wear(player, copper, pattern(helper, TrimPatterns.SILENCE), 4);
            assertClose(helper, TrimEffectUtil.getStealthMultiplier(player), 0.0,
                    "the stealth factor went below zero, which would make the player more "
                            + "visible instead of less");

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // THE PLAYER SIDE BENEFIT GATE
    // =====================================================================================

    /**
     * The client config {@code enableArmorTrimBenefits} is not read on the server at all; the
     * client sends its value once and the server keeps it per player behind
     * {@link TrimBenefitUser}. {@link NetworkHandlerTests} proves the flag arrives. This proves
     * it is obeyed: with the flag off, counting returns zero and every benefit built on top of
     * it collapses back to neutral.
     *
     * <p>A player who switched the benefits off and still gets damage reduction is a straight
     * bug report, and nothing else in the suite would catch it - the flag would still be stored
     * correctly, it would just be ignored.
     */
    public static void benefitGateSwitchesEveryTrimEffectOff(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            helper.assertTrue(player instanceof TrimBenefitUser,
                    "the trim benefit mixin is not applied to ServerPlayer on this loader");
            TrimBenefitUser gate = (TrimBenefitUser) player;

            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimPattern> rib = pattern(helper, TrimPatterns.RIB);
            DamageSource wither = helper.getLevel().damageSources().wither();

            pinProgressMultiplier(helper, player, 1.0);
            wear(player, copper, rib, 4);

            // --- the benefits are on by default, so the "off" case below means something ---
            helper.assertTrue(gate.simplebuilding$areTrimBenefitsEnabled(),
                    "a fresh player did not start with the trim benefits enabled");
            assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 4.0,
                    "the baseline rib count before the gate is touched");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 6.0,
                    "the baseline wither damage before the gate is touched");

            // --- switched off: counting stops, and everything downstream goes neutral ---
            gate.simplebuilding$setTrimBenefitsEnabled(false);
            assertClose(helper, TrimEffectUtil.getTrimCount(player, "rib"), 0.0,
                    "patterns were still counted with the trim benefits switched off");
            helper.assertTrue(TrimEffectUtil.getMaterialCount(player, "copper") == 0,
                    "materials were still counted with the trim benefits switched off");
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 10.0,
                    "damage was still reduced with the trim benefits switched off");
            helper.assertTrue(TrimEffectUtil.getWitherReductionAmount(player) == 0,
                    "wither was still shortened with the trim benefits switched off");

            wear(player, copper, pattern(helper, TrimPatterns.TIDE), 4);
            assertClose(helper, TrimEffectUtil.getSwimSpeedMultiplier(player), 1.0,
                    "swim speed was still boosted with the trim benefits switched off");

            // --- and back on again: the same armour works again, nothing was consumed ---
            gate.simplebuilding$setTrimBenefitsEnabled(true);
            wear(player, copper, rib, 4);
            assertClose(helper, TrimEffectUtil.modifyDamage(player, 10.0F, wither), 6.0,
                    "the benefits did not come back after re-enabling them");

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // TICK EFFECTS
    // =====================================================================================

    /**
     * {@code TrimEffectUtil.tick} is the only place the mod hands out a status effect for a
     * trim, and the astralit jump boost is the half of it that can be reached from a test.
     *
     * <p>The band edges are what is pinned: below a score of 2.0 there is no effect at all, from
     * 2.0 Jump Boost I, from 8.0 Jump Boost II. Score is pieces times the progress factor, so a
     * player early in a world gets nothing - if the thresholds slip, a brand new player either
     * gets a free permanent Jump Boost II or a fully levelled one gets nothing.
     *
     * <p>The gate is checked here too, because {@code tick} reaches the material count through a
     * different path than the damage code does.
     *
     * <p>The other half of {@code tick}, the Enderscape stasis resistance, cannot be reached in
     * the positive: {@code countTrimById} matches the full id {@code enderscape:stasis} and
     * Enderscape is not a dependency, so no holder in the test registry carries that key and the
     * three amplifier bands of {@code applyStasisEffect} are untestable here. Two negatives are
     * asserted instead, and it is worth being precise about what each one buys:
     * <ul>
     *   <li>A registered vanilla pattern proves the id comparison really runs and really says
     *       no - {@code Holder.Reference.is(Identifier)} compares keys for real.</li>
     *   <li>A locally built {@code simplebuilding:stasis} pattern proves the check has not
     *       decayed into a name or asset-path test, which is the regression this code would
     *       actually invite. It does <em>not</em> prove much beyond that:
     *       {@code Holder.Direct.is(Identifier)} returns false unconditionally, so a rewrite
     *       that still compared registry keys would pass this half either way.</li>
     * </ul>
     */
    public static void astralitJumpBoostCrossesItsThresholdsOnTick(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> astralit = material(helper, ModTrimMaterials.ASTRALIT);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());

            // One piece, so the score is exactly the progress factor and the bands are readable.
            wear(player, astralit, blank, 1);

            // The scores below stay 0.001 clear of each threshold, because the pinned multiplier
            // is a double and landing exactly on a boundary would decide the case by rounding.

            // --- below 2.0: nothing, however tempting ---
            pinProgressMultiplier(helper, player, 1.999);
            tickWithoutJumpBoost(helper, player);
            helper.assertTrue(!player.hasEffect(MobEffects.JUMP_BOOST),
                    "an astralit piece below the 2.0 threshold still granted Jump Boost");

            // --- 2.0 up to 8.0: Jump Boost I ---
            pinProgressMultiplier(helper, player, 2.001);
            tickWithoutJumpBoost(helper, player);
            assertJumpBoost(helper, player, 0, "just above the first threshold (score 2.001)");

            pinProgressMultiplier(helper, player, 7.999);
            tickWithoutJumpBoost(helper, player);
            assertJumpBoost(helper, player, 0, "just below the second threshold (score 7.999)");

            // --- from 8.0: Jump Boost II ---
            pinProgressMultiplier(helper, player, 8.001);
            tickWithoutJumpBoost(helper, player);
            assertJumpBoost(helper, player, 1, "just above the second threshold (score 8.001)");

            // --- four pieces reach the same band on a quarter of the progress ---
            wear(player, astralit, blank, 4);
            pinProgressMultiplier(helper, player, 2.001);
            tickWithoutJumpBoost(helper, player);
            assertJumpBoost(helper, player, 1, "four pieces at progress 2.001 (score 8.004)");

            // --- a material that is not astralit buys nothing, however high the progress ---
            wear(player, material(helper, TrimMaterials.COPPER), blank, 4);
            pinProgressMultiplier(helper, player, 8.001);
            tickWithoutJumpBoost(helper, player);
            helper.assertTrue(!player.hasEffect(MobEffects.JUMP_BOOST),
                    "a copper trim was treated as astralit and granted Jump Boost");

            // --- the benefit gate reaches the tick effects as well ---
            wear(player, astralit, blank, 4);
            ((TrimBenefitUser) player).simplebuilding$setTrimBenefitsEnabled(false);
            tickWithoutJumpBoost(helper, player);
            helper.assertTrue(!player.hasEffect(MobEffects.JUMP_BOOST),
                    "the astralit jump boost was granted with the trim benefits switched off");
            ((TrimBenefitUser) player).simplebuilding$setTrimBenefitsEnabled(true);

            // --- the stasis id gate, in the negative (see the javadoc for what this proves) ---
            pinProgressMultiplier(helper, player, 8.0);

            wear(player, material(helper, TrimMaterials.COPPER), pattern(helper, TrimPatterns.WARD), 4);
            player.removeEffect(MobEffects.RESISTANCE);
            TrimEffectUtil.tick(player);
            helper.assertTrue(!player.hasEffect(MobEffects.RESISTANCE),
                    "an ordinary ward set granted the Enderscape stasis resistance - the id "
                            + "comparison in countTrimById is matching things it should not");

            Holder<TrimPattern> lookAlikeStasis = Holder.direct(new TrimPattern(
                    Identifier.fromNamespaceAndPath("simplebuilding", "stasis"),
                    Component.literal("Stasis"), false));
            wear(player, material(helper, TrimMaterials.COPPER), lookAlikeStasis, 4);
            player.removeEffect(MobEffects.RESISTANCE);
            TrimEffectUtil.tick(player);
            helper.assertTrue(!player.hasEffect(MobEffects.RESISTANCE),
                    "a pattern whose asset path is merely \"stasis\" granted the Enderscape "
                            + "stasis resistance - the check is supposed to match the full id "
                            + "enderscape:stasis, not a name");

            player.removeEffect(MobEffects.JUMP_BOOST);
            player.removeEffect(MobEffects.RESISTANCE);
            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // MOVEMENT
    // =====================================================================================

    /**
     * Nihilith pulls a sneaking, airborne player down. It is the one benefit in the mod that is
     * <em>not</em> scaled by the progress multiplier - a flat 0.08 per piece - so a change that
     * "harmonises" it with the rest would quietly make it useless for a new player and violent
     * for a levelled one. The test pins the flat rate at two very different multipliers.
     *
     * <p>Its four conditions are checked one at a time, because each one is a way for the
     * player to switch the pull off: standing on the ground, not sneaking, flying, or already
     * falling faster than the terminal speed the code allows. The last one is checked on both
     * sides of the boundary - a guard that only ever sees -2.5 could be deleted and replaced by
     * "never push a fast faller" without the test noticing.
     */
    public static void nihilithPullsDownTheSneakingAirbornePlayer(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> nihilith = material(helper, ModTrimMaterials.NIHILITH);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());

            pinProgressMultiplier(helper, player, 1.0);
            wear(player, nihilith, blank, 4);
            player.getAbilities().flying = false;

            // --- sneaking and airborne: 0.08 per piece downwards ---
            airborneSneak(player, true, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, -0.32,
                    "a full nihilith set did not pull a sneaking, falling player down");

            // --- one piece pulls a quarter as hard, so the rate is per piece ---
            wear(player, nihilith, blank, 1);
            airborneSneak(player, true, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, -0.08,
                    "one nihilith piece did not pull with a quarter of the force of four");
            wear(player, nihilith, blank, 4);

            // --- the rate is flat: a huge progress multiplier must not change it ---
            pinProgressMultiplier(helper, player, 10.0);
            airborneSneak(player, true, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, -0.32,
                    "the nihilith pull started scaling with the progress multiplier; it is meant "
                            + "to be a flat 0.08 per piece");
            pinProgressMultiplier(helper, player, 1.0);

            // --- upright: no pull ---
            airborneSneak(player, false, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, 0.0,
                    "an upright player was pulled down - sneaking is the only way to ask for it");

            // --- standing on the ground: no pull ---
            airborneSneak(player, true, false);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, 0.0,
                    "a player standing on the ground was pulled down");

            // --- flying: no pull, otherwise creative flight would fight the trim ---
            airborneSneak(player, true, true);
            player.getAbilities().flying = true;
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, 0.0,
                    "a flying player was pulled down");
            player.getAbilities().flying = false;

            // --- the terminal speed boundary, from both sides ---
            airborneSneak(player, true, true);
            player.setDeltaMovement(0.0, -1.9, 0.0);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, -2.22,
                    "a player still slower than the -2.0 terminal speed was not pulled further");

            airborneSneak(player, true, true);
            player.setDeltaMovement(0.0, -2.5, 0.0);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, -2.5,
                    "the terminal speed guard is gone - a sneaking player now accelerates without "
                            + "bound");

            // --- a material that is not nihilith does nothing ---
            wear(player, material(helper, TrimMaterials.COPPER), blank, 4);
            airborneSneak(player, true, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, 0.0,
                    "a copper trim was treated as nihilith");

            // --- and the benefit gate switches it off as well ---
            wear(player, nihilith, blank, 4);
            ((TrimBenefitUser) player).simplebuilding$setTrimBenefitsEnabled(false);
            airborneSneak(player, true, true);
            TrimEffectUtil.handleNihilithGravity(player);
            assertClose(helper, player.getDeltaMovement().y, 0.0,
                    "the nihilith pull ignored the trim benefit switch");
            ((TrimBenefitUser) player).simplebuilding$setTrimBenefitsEnabled(true);

            player.setShiftKeyDown(false);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // THE WIRING
    // =====================================================================================

    /**
     * Everything above reads {@code TrimEffectUtil} directly, which means every one of those
     * tests would still pass with {@code PlayerEntityMixin} deleted from
     * {@code simplebuilding.mixins.json} - the numbers would all be right and no player would
     * ever see them. This test asks the player instead of the utility class.
     *
     * <p>Two of the mixin's injections return a value that can simply be read back on a standing
     * mock player: {@code getLuck} adds the host/emerald bonus to the luck attribute, and
     * {@code getSpeed} multiplies the movement speed attribute by the bolt/redstone factor. Both
     * are measured against the same player's own untrimmed reading rather than against a
     * hard-coded 0.1, so a vanilla attribute change cannot turn this into a false failure.
     *
     * <p>The gate is checked through the same two calls, because both injections guard on
     * "only if the bonus is actually positive" and that guard is where an off switch gets lost.
     */
    public static void trimBonusesReachThePlayerThroughTheMixins(GameTestHelper helper) {
        double configuredBase = SimplebuildingConfig.trimBenefitBaseMultiplier;
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimPattern> host = pattern(helper, TrimPatterns.HOST);
            Holder<TrimPattern> bolt = pattern(helper, TrimPatterns.BOLT);

            pinProgressMultiplier(helper, player, 1.0);
            bare(player);

            float bareLuck = player.getLuck();
            float bareSpeed = player.getSpeed();
            helper.assertTrue(bareSpeed > 0.0F,
                    "the mock player's movement speed is " + bareSpeed + ", so multiplying it by "
                            + "the bolt bonus could not be detected either way");

            // --- luck: getLuck has to come back four higher with a full host set ---
            wear(player, copper, host, 4);
            assertClose(helper, player.getLuck(), bareLuck + 4.0,
                    "Player.getLuck did not pick up the host trim bonus - TrimEffectUtil computes "
                            + "it, but PlayerEntityMixin is not delivering it");

            // --- speed: getSpeed has to come back 20% higher with a full bolt set ---
            wear(player, copper, bolt, 4);
            assertClose(helper, player.getSpeed(), bareSpeed * 1.2,
                    "Player.getSpeed did not pick up the bolt trim bonus - TrimEffectUtil computes "
                            + "it, but PlayerEntityMixin is not delivering it");

            // --- and the gate reaches both injections ---
            TrimBenefitUser gate = (TrimBenefitUser) player;
            gate.simplebuilding$setTrimBenefitsEnabled(false);
            assertClose(helper, player.getSpeed(), bareSpeed,
                    "the walk speed bonus survived the trim benefit switch");
            wear(player, copper, host, 4);
            assertClose(helper, player.getLuck(), bareLuck,
                    "the luck bonus survived the trim benefit switch");
            gate.simplebuilding$setTrimBenefitsEnabled(true);

            bare(player);
            helper.succeed();
        } finally {
            SimplebuildingConfig.trimBenefitBaseMultiplier = configuredBase;
        }
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown - a failing
        // test would cost minutes of wall clock instead of seconds.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Bends the configured base multiplier until {@link TrimMultiplierLogic} reports exactly
     * {@code target} for this player, and checks that it does.
     *
     * <p>A fresh mock player has no distance walked, no play time and no kills, so its survival
     * and combat factors both sit at the 0.1 floor, its experience factor sits at the 0.1 floor
     * too, and the whole multiplier lands near 0.001 per unit of configured base - far below
     * every threshold in the mod. Those three factors cannot usefully be raised from a test, so
     * the configured base is the only handle. Because the multiplier is a plain product, scaling
     * the base scales the result, and this method's own check is therefore also a test that the
     * base still feeds through at all.
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

    /** Resets the movement state the nihilith pull reads, so each case starts from a standstill. */
    private static void airborneSneak(ServerPlayer player, boolean sneaking, boolean airborne) {
        player.setShiftKeyDown(sneaking);
        player.setOnGround(!airborne);
        player.setDeltaMovement(0.0, 0.0, 0.0);
    }

    /** Clears the effect first, so a weaker new one is not silently rejected by addEffect. */
    private static void tickWithoutJumpBoost(GameTestHelper helper, ServerPlayer player) {
        player.removeEffect(MobEffects.JUMP_BOOST);
        helper.assertTrue(!player.hasEffect(MobEffects.JUMP_BOOST),
                "the previous Jump Boost could not be cleared, so the next case would prove nothing");
        TrimEffectUtil.tick(player);
    }

    private static void assertJumpBoost(GameTestHelper helper, ServerPlayer player, int amplifier, String what) {
        MobEffectInstance effect = player.getEffect(MobEffects.JUMP_BOOST);
        helper.assertTrue(effect != null, "no Jump Boost was granted " + what);
        helper.assertTrue(effect != null && effect.getAmplifier() == amplifier,
                "wrong Jump Boost level " + what + ": expected amplifier " + amplifier
                        + " but got " + (effect == null ? "none" : effect.getAmplifier()));
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
        entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
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

    /**
     * Everything measured here is a {@code float} internally, so a double-tight epsilon would
     * fail on rounding alone; 1.0e-4 is still far below every step this class asserts.
     */
    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-4,
                what + ": expected " + expected + " but got " + actual);
    }
}
