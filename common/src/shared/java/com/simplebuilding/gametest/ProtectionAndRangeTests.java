package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

/**
 * The three "protection-ish" features that so far only had data level coverage: Kinetic
 * Protection, Range and the void protection of enderite items.
 *
 * <p>All three used to be checked by asking the JSON whether a component or a tag entry still
 * exists. That proves the datagen ran, not that anything happens in game: a component can be
 * present and never evaluated, a tag can be correct while the code reading it is broken. These
 * tests drive vanilla's own evaluation instead -- the damage protection pipeline, the attribute
 * modifier the enchantment produces, and the {@code ItemEntity} tick the void protection mixin
 * hooks into.
 *
 * <p>None of the three has a survival-only branch, so the hard-wired creative mock player costs
 * this area nothing. The one place creative <em>would</em> have blocked a test - taking real
 * damage - is worked around through {@code abilities.invulnerable}; see
 * {@link #kineticProtectionActuallyReducesTheDamageThePlayerTakes}.
 *
 * <p>What is deliberately <em>not</em> here is written down in the javadoc of each test.
 */
public final class ProtectionAndRangeTests {

    private ProtectionAndRangeTests() {
    }

    /** Tick budget for {@link #voidProtectionLiftsEnderiteBackIntoTheWorldWhileOtherItemsAreLost}. */
    public static final int VOID_PROTECTION_MAX_TICKS = 60;

    /** How long the dropped items are left alone before the world is inspected. */
    private static final int VOID_SETTLE_TICKS = 5;

    /**
     * Blocks below the world floor for the item that must only float. Has to stay above the
     * mixin's rescue threshold ({@code minY - 10}), otherwise the "float, do not teleport" case
     * would silently turn into a second rescue case.
     */
    private static final int VOID_FLOAT_DEPTH = 5;

    /**
     * Blocks below the world floor for the item that must be rescued. Has to be deeper than the
     * 64 blocks after which {@code Entity#checkBelowWorld} deletes an entity, so that surviving
     * really means the mixin acted.
     */
    private static final int VOID_RESCUE_DEPTH = 70;

    /**
     * The depth at which the mixin stops letting the item float and lifts it back into the world
     * ({@code getY() < minY - 10}). Not a number this test chooses - it is read off
     * {@code EnderiteItemMixin} - but it is the number the two probes below sit around.
     */
    private static final int VOID_RESCUE_THRESHOLD_DEPTH = 10;

    /**
     * One block short of the threshold: has to float. Together with the probe below it brackets
     * the threshold, which neither {@link #VOID_FLOAT_DEPTH} nor {@link #VOID_RESCUE_DEPTH} does -
     * both sit far away from it, so the threshold could be moved anywhere between them and this
     * test would not notice, leaving every protected item that lands in that band hanging
     * weightless and out of reach forever.
     */
    private static final int VOID_JUST_ABOVE_THRESHOLD_DEPTH = VOID_RESCUE_THRESHOLD_DEPTH - 1;

    /** One block past the threshold: has to be lifted back, even though vanilla would keep it. */
    private static final int VOID_JUST_BELOW_THRESHOLD_DEPTH = VOID_RESCUE_THRESHOLD_DEPTH + 1;

    /** Where the rescue puts the item: this many blocks above the world floor. */
    private static final int VOID_RESCUE_TARGET_HEIGHT = 5;

    /** Damage dealt in {@link #kineticProtectionActuallyReducesTheDamageThePlayerTakes}. */
    private static final float HIT_DAMAGE = 10.0F;

    /** Kinetic Protection grants this many protection points per level. */
    private static final float KINETIC_POINTS_PER_LEVEL = 2.5F;

    /** Highest level the enchantment definition offers; every test below uses it. */
    private static final int KINETIC_MAX_LEVEL = 4;

    /** Highest level Range offers. */
    private static final int RANGE_MAX_LEVEL = 3;

    /** The four humanoid armour slots, i.e. exactly what {@code slots: ["armor"]} means. */
    private static final List<EquipmentSlot> ARMOUR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    // =====================================================================================
    // (a) KINETIC PROTECTION
    // =====================================================================================

    /**
     * Kinetic Protection is a pure data enchantment: a {@code damage_protection} effect of 2.5
     * points per level, gated by a {@code damage_source_properties} condition on the damage type
     * tag {@code simplebuilding:kinetic_damage}. This runs vanilla's
     * {@code EnchantmentHelper#getDamageProtection}, which is the exact call
     * {@code LivingEntity#getDamageAfterMagicAbsorb} makes when something hits the wearer -- that
     * helper walks every {@code EquipmentSlot} and keeps only the ones the enchantment's slot
     * list matches, so both the "which slot" and the "how much" half are real here.
     *
     * <p>It breaks if: the effect is dropped from the generated JSON, the per level value or the
     * max level changes, {@code simplebuilding:kinetic_damage} loses
     * {@code minecraft:fly_into_wall} (the enchantment would then protect against nothing at
     * all), the condition is removed (it would protect against everything, i.e. become a second
     * Protection), or the slot list moves -- narrowing {@code armor} to a single piece is caught
     * by the full-set case, widening it to the hand by the last one.
     */
    public static void kineticProtectionScalesWithLevelAndOnlyCoversItsOwnDamageTypes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        DamageSource kinetic = level.damageSources().flyIntoWall();
        DamageSource unrelated = level.damageSources().magic();

        // Setup guard: every case below enchants to KINETIC_MAX_LEVEL. ItemStack#enchant does not
        // clamp, so without this a shrunken max level would leave the numbers passing while the
        // level being measured had become unobtainable in game.
        helper.assertValueEqual(
                enchantment(helper, ModEnchantments.KINETIC_PROTECTION).value().getMaxLevel(),
                KINETIC_MAX_LEVEL, "max level of Kinetic Protection");

        // --- no enchantment: no protection, otherwise every helmet would carry the effect ---
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        assertProtection(helper, player, kinetic, 0.0F, "a plain diamond helmet");

        // --- the enchantment itself, at both ends of its level range ---
        player.setItemSlot(EquipmentSlot.HEAD, kineticArmour(helper, Items.DIAMOND_HELMET, 1));
        assertProtection(helper, player, kinetic, KINETIC_POINTS_PER_LEVEL,
                "Kinetic Protection I on the head");

        player.setItemSlot(EquipmentSlot.HEAD, kineticArmour(helper, Items.DIAMOND_HELMET, KINETIC_MAX_LEVEL));
        assertProtection(helper, player, kinetic, KINETIC_MAX_LEVEL * KINETIC_POINTS_PER_LEVEL,
                "Kinetic Protection IV on the head");

        // --- the damage source condition: magic damage is not in simplebuilding:kinetic_damage ---
        assertProtection(helper, player, unrelated, 0.0F,
                "Kinetic Protection IV against a damage type outside simplebuilding:kinetic_damage");

        // --- slots=[armor] means all four pieces, not just the helmet ---
        player.setItemSlot(EquipmentSlot.CHEST, kineticArmour(helper, Items.DIAMOND_CHESTPLATE, KINETIC_MAX_LEVEL));
        player.setItemSlot(EquipmentSlot.LEGS, kineticArmour(helper, Items.DIAMOND_LEGGINGS, KINETIC_MAX_LEVEL));
        player.setItemSlot(EquipmentSlot.FEET, kineticArmour(helper, Items.DIAMOND_BOOTS, KINETIC_MAX_LEVEL));
        assertProtection(helper, player, kinetic,
                ARMOUR_SLOTS.size() * KINETIC_MAX_LEVEL * KINETIC_POINTS_PER_LEVEL,
                "a full diamond set with Kinetic Protection IV in every piece");

        // --- the slot list: the enchantment declares slots=[armor], not the hand ---
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, kineticArmour(helper, Items.DIAMOND_HELMET, KINETIC_MAX_LEVEL));
        assertProtection(helper, player, kinetic, 0.0F,
                "Kinetic Protection IV on a helmet that is only being carried in the hand");

        helper.succeed();
    }

    /**
     * The same enchantment, but measured on the health bar instead of on the helper: a real hit
     * with {@code minecraft:fly_into_wall} against a player wearing the enchanted helmet has to
     * take away less health than the same hit against a plain one.
     *
     * <p>Making the mock player damageable takes three separate switches, and the game mode is
     * none of them -- neither {@code Player#hurtServer} nor {@code Player#actuallyHurt} looks at
     * it. Two are the obvious flags: {@code abilities.invulnerable}, which {@code Player#hurtServer}
     * checks directly, and the entity flag behind {@code Entity#setInvulnerable}, which
     * {@code Entity#isInvulnerableTo} checks.
     *
     * <p>The third is the one that is easy to miss. {@code ServerPlayer#isInvulnerableTo} also
     * returns {@code true} while {@code !this.connection.hasClientLoaded()}, and
     * {@code hasClientLoaded()} is {@code clientLoadedTimeoutTimer <= 0} -- a counter
     * {@code PlayerList#placeNewPlayer} sets to 60 for every player that logs in, including the
     * one {@code GameTestHelper#makeMockServerPlayerInLevel} builds. A real client clears it by
     * sending {@code ServerboundPlayerLoadedPacket}; nothing sends that for a mock player, so
     * without the line below the player is simply invulnerable for the first 60 ticks and every
     * hit in this test is swallowed.
     *
     * <p>{@code minecraft:fly_into_wall} is in {@code minecraft:bypasses_armor} and in none of
     * {@code bypasses_effects} / {@code bypasses_enchantments} / {@code bypasses_invulnerability},
     * so the armour absorption step is skipped for both hits while the enchantment step still
     * runs, and the numbers are exact: 10 raw damage stays 10 without the enchantment, and the 10
     * protection points of level IV leave {@code 1 - 10/25 = 60 %} of it.
     *
     * <p>It breaks if the enchantment stops reaching the damage pipeline for any of the reasons
     * listed on the previous test, and it fails loudly instead of passing silently if the mock
     * player ever becomes undamageable again -- that is what the {@code plain > 0} guard is for.
     */
    public static void kineticProtectionActuallyReducesTheDamageThePlayerTakes(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        // See the javadoc: these are the switches the damage path reads, not the game mode.
        player.getAbilities().invulnerable = false;
        player.setInvulnerable(false);
        // The same packet a real client sends once it has finished loading; without it
        // ServerPlayer#isInvulnerableTo keeps returning true for the first 60 ticks.
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        // Absorption would be eaten before the health bar and make the measurement lie.
        player.setAbsorptionAmount(0.0F);
        DamageSource kinetic = helper.getLevel().damageSources().flyIntoWall();

        float plain = damageTaken(helper, player, new ItemStack(Items.DIAMOND_HELMET), kinetic);
        helper.assertTrue(plain > 0.0F,
                "the mock player took no damage at all, so this test would prove nothing about the "
                        + "enchantment - the creative mock player is invulnerable again and the "
                        + "damage path is out of reach");
        helper.assertTrue(Math.abs(plain - HIT_DAMAGE) < 0.01F,
                "fly_into_wall bypasses armour, so an unenchanted helmet has to let all "
                        + HIT_DAMAGE + " points through; the player lost " + plain);

        float protectedLoss = damageTaken(helper, player,
                kineticArmour(helper, Items.DIAMOND_HELMET, KINETIC_MAX_LEVEL), kinetic);
        helper.assertTrue(protectedLoss < plain,
                "Kinetic Protection IV did not reduce the damage at all: " + protectedLoss
                        + " lost with the enchantment, " + plain + " without it");
        helper.assertTrue(Math.abs(protectedLoss - plain * 0.6F) < 0.01F,
                "Kinetic Protection IV is worth 10 protection points, which leaves 60 % of the "
                        + "damage; expected " + (plain * 0.6F) + " but the player lost " + protectedLoss);

        helper.succeed();
    }

    // =====================================================================================
    // (b) RANGE
    // =====================================================================================

    /**
     * Range is the mod's only attribute enchantment: {@code +2} block interaction range at level
     * I and {@code +4} for every level above that, in the main hand only.
     *
     * <p>Two steps, because they fail for different reasons. First the modifier vanilla builds
     * out of the enchantment is inspected per level through
     * {@code EnchantmentHelper#forEachModifier}, which filters by
     * {@code Enchantment#matchingSlot}; that catches a changed attribute, a changed amount, a
     * changed operation, a changed modifier id and a changed slot list. Then the whole chain is
     * run for real on a player: hold the tool, tick the player, read
     * {@code Attributes.BLOCK_INTERACTION_RANGE} off the player. The absolute value depends on
     * the game mode, so only the difference between the plain and the enchanted tool is asserted.
     *
     * <p>The player half needs {@code player.connection.tick()} for the same reason the magnet
     * test does: the gametest server ticks {@code ServerPlayer#tick()}, but the equipment change
     * detection sits in {@code LivingEntity#tick()} ({@code detectEquipmentUpdates()} -&gt;
     * {@code collectEquipmentChanges()} -&gt; {@code EnchantmentHelper#runLocationChangedEffects}
     * -&gt; {@code EnchantmentAttributeEffect#onChangedBlock} -&gt;
     * {@code addTransientAttributeModifiers}), which is only reached through
     * {@code ServerPlayer#doTick()} -- and nobody pumps the mock player's connection.
     *
     * <p>Note on the modifier id: vanilla appends {@code "/" + slot} to the id declared in the
     * enchantment, so the id seen here is {@code simplebuilding:enchantment.range/mainhand}. Only
     * the half this mod owns is asserted.
     *
     * <p>The third step is about <em>which items</em> may carry all of that.
     * {@code ItemStack#enchant} never asks, so every measurement above works on a chisel that
     * might long have fallen out of the enchantment's item tag. Range hangs on
     * {@code #simplebuilding:chisel_and_mining_tools}, which is not a hand written item list but
     * four nested tags: the chisels, vanilla's {@code #minecraft:enchantable/mining}, the
     * sledgehammers and {@code #simplebuilding:octants_enchantable}. Each nesting is a single
     * line in the tag provider, and every number measured above is measured on a chisel, so
     * deleting any one of the other three takes a whole item family out of the enchantment
     * without moving a single figure in this test. {@code Enchantment#canEnchant} is what the
     * anvil asks, so that is what is asked here: for the chisel, for the four vanilla mining
     * tools, for the sledgehammer, and for the plain octant plus all sixteen dyed ones.
     */
    public static void rangeAddsBlockInteractionReachInTheMainHandOnly(GameTestHelper helper) {
        // Setup guard: the loop below goes up to RANGE_MAX_LEVEL and ItemStack#enchant does not
        // clamp, so a shrunken max level has to be visible here rather than in the numbers.
        helper.assertValueEqual(enchantment(helper, ModEnchantments.RANGE).value().getMaxLevel(),
                RANGE_MAX_LEVEL, "max level of Range");

        ItemStack plainTool = new ItemStack(ModItems.DIAMOND_CHISEL);
        helper.assertTrue(reachModifiers(plainTool, EquipmentSlot.MAINHAND).isEmpty(),
                "an unenchanted chisel already carries a block interaction range modifier, so the "
                        + "per level numbers below would not be the enchantment's doing");

        // Level I = 2.0, then +4.0 per level above the first.
        double[] expectedAmount = {2.0, 6.0, 10.0};
        for (int enchantLevel = 1; enchantLevel <= RANGE_MAX_LEVEL; enchantLevel++) {
            ItemStack tool = rangeTool(helper, enchantLevel);

            List<AttributeModifier> inHand = reachModifiers(tool, EquipmentSlot.MAINHAND);
            helper.assertValueEqual(inHand.size(), 1,
                    "block interaction range modifiers of Range " + enchantLevel);

            AttributeModifier modifier = inHand.get(0);
            helper.assertTrue(Math.abs(modifier.amount() - expectedAmount[enchantLevel - 1]) < 1.0E-6,
                    "Range " + enchantLevel + " should add " + expectedAmount[enchantLevel - 1]
                            + " blocks of reach, it adds " + modifier.amount());
            helper.assertTrue(modifier.operation() == AttributeModifier.Operation.ADD_VALUE,
                    "Range " + enchantLevel + " no longer adds a flat value but uses "
                            + modifier.operation() + ", which changes the reach for every game mode");
            helper.assertTrue(SimpleBuildingGameTests.MOD_ID.equals(modifier.id().getNamespace())
                            && modifier.id().getPath().startsWith("enchantment.range"),
                    "the Range modifier id moved to " + modifier.id() + "; a modifier id is saved "
                            + "with the item, so existing enchanted tools would keep the old one");

            // slots=[mainhand]: neither armour nor the off hand may see the modifier.
            helper.assertTrue(reachModifiers(tool, EquipmentSlot.HEAD).isEmpty(),
                    "Range " + enchantLevel + " grants reach from the head slot as well");
            helper.assertTrue(reachModifiers(tool, EquipmentSlot.OFFHAND).isEmpty(),
                    "Range " + enchantLevel + " grants reach from the off hand as well");
        }

        // --- who is allowed to carry it: the anvil's own question, item by item ---
        Enchantment range = enchantment(helper, ModEnchantments.RANGE).value();
        helper.assertTrue(range.canEnchant(plainTool),
                "Range cannot be put on the diamond chisel any more, so every number measured "
                        + "above was measured on an item the player can no longer enchant");
        // Vanilla's own mining tools are in only because #simplebuilding:chisel_and_mining_tools
        // nests #minecraft:enchantable/mining; nothing else in the suite asks canEnchant for
        // them, so without this loop that nesting can be dropped unnoticed.
        for (Item miningTool : List.<Item>of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE)) {
            helper.assertTrue(range.canEnchant(new ItemStack(miningTool)),
                    "Range cannot be put on " + miningTool + " any more; the vanilla mining tools "
                            + "reach #simplebuilding:chisel_and_mining_tools only through the "
                            + "nested #minecraft:enchantable/mining, and that link is gone");
        }
        // The sledgehammers hang in the tag twice - directly through
        // #simplebuilding:sledgehammer_tools and again through #minecraft:enchantable/mining, which
        // the provider fills with the same tag - so this only goes red once both are gone.
        helper.assertTrue(range.canEnchant(new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER)),
                "Range cannot be put on the diamond sledgehammer any more, so the sledgehammers "
                        + "dropped out of #simplebuilding:chisel_and_mining_tools on both of the "
                        + "routes that put them there");
        helper.assertTrue(range.canEnchant(new ItemStack(ModItems.OCTANT)),
                "Range cannot be put on the octant; it reaches "
                        + "#simplebuilding:chisel_and_mining_tools only through the nested "
                        + "#simplebuilding:octants_enchantable, and that link is gone");
        helper.assertValueEqual(ModItems.COLORED_OCTANT_ITEMS.size(), DyeColor.values().length,
                "test setup broken: number of dyed octants registered - with none of them the loop "
                        + "below would check nothing");
        for (Item coloredOctant : ModItems.COLORED_OCTANT_ITEMS.values()) {
            helper.assertTrue(range.canEnchant(new ItemStack(coloredOctant)),
                    "Range cannot be put on " + coloredOctant + ", so the dyed octants dropped out "
                            + "of #simplebuilding:octants_enchantable while the plain one stayed");
        }
        helper.assertTrue(!range.canEnchant(new ItemStack(Items.DIAMOND_HELMET)),
                "Range can be put on a diamond helmet, so its item tag accepts everything and the "
                        + "answers above say nothing about the chisel or the octant");

        // --- and now the whole way through to the player's attribute map ---
        ServerPlayer player = mockPlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, plainTool);
        player.connection.tick();
        double without = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        player.setItemSlot(EquipmentSlot.MAINHAND, rangeTool(helper, RANGE_MAX_LEVEL));
        player.connection.tick();
        double with = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        helper.assertTrue(Math.abs((with - without) - expectedAmount[RANGE_MAX_LEVEL - 1]) < 1.0E-4,
                "holding a Range III chisel has to raise the player's block interaction range by "
                        + expectedAmount[RANGE_MAX_LEVEL - 1] + " blocks; it went from " + without
                        + " to " + with);

        helper.succeed();
    }

    // =====================================================================================
    // (c) VOID PROTECTION PHYSICS
    // =====================================================================================

    /**
     * The behaviour behind {@code simplebuilding:void_protected}: an enderite item that falls out
     * of the world stops falling and, once it gets deep enough for vanilla to delete it, is put
     * back inside the world. Implemented in {@code mixin/EnderiteItemMixin} at the head of
     * {@code ItemEntity#tick}, i.e. before {@code Entity#checkBelowWorld} gets its turn - and
     * until now only the <em>contents</em> of the tag were ever tested.
     *
     * <p>Six items are dropped at the same time, and each one pins down a different part of the
     * mixin. Everything is expressed relative to {@code level.getMinY()} on purpose: the bug that
     * once lived here was a set of pre-1.18 constants ({@code y < 0} to start floating,
     * {@code y < -10} to rescue, rescue target {@code y = 5}). With the world floor at -64 that
     * code froze items in mid air at legitimate mining depths and then teleported them up to
     * y = 5, while items between -64 and 0 that had really fallen out of the world were never
     * caught.
     *
     * <ul>
     *   <li>protected, 5 blocks below the floor: has to stop dead where it is (float),
     *       <em>not</em> get teleported -- the old {@code y < -10} rule would have moved it;</li>
     *   <li>protected, 70 blocks below the floor, i.e. past the 64 blocks after which vanilla
     *       deletes entities: has to survive and come back to {@code minY + 5}; the old code
     *       would have parked it at y = 5;</li>
     *   <li>not in the tag, same depth: has to be gone, otherwise the mixin protects everything;</li>
     *   <li>protected but still inside the world at a negative y: has to keep its gravity and
     *       stay where it was dropped. This is the case the old {@code y < 0} constant got
     *       wrong.</li>
     *   <li>protected, one block above and one block below the rescue threshold: the first has to
     *       float where it is, the second has to be lifted back. Those two are what pin the
     *       threshold itself. The four cases above straddle it by 5 and by 70 blocks, so the
     *       threshold could be moved anywhere in between and all of them would still behave
     *       exactly as asserted -- while every protected item that lands in that band would hang
     *       weightless below the world for good, unreachable for the player.</li>
     * </ul>
     *
     * <p>The rescue height is asserted twice: once loosely (inside the world), once against
     * {@code minY + 5} with a block of slack. The loose bound alone leaves {@code minY + 15}
     * green. The slack is there because the item is measured a few ticks after the rescue: today
     * it stays exactly where the mixin put it -- the rescue leaves {@code noGravity} switched on,
     * so nothing moves it -- but a version that switches gravity back on would let it fall about
     * 0.6 blocks in {@link #VOID_SETTLE_TICKS} ticks, and that is not a moved rescue target.
     */
    public static void voidProtectionLiftsEnderiteBackIntoTheWorldWhileOtherItemsAreLost(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int minY = level.getMinY();

        // Setup guards. The whole test is meaningless if the tag does not say what we think, and
        // the two depths have to sit on the right sides of vanilla's 64 block delete margin -
        // otherwise "it floated" and "it was rescued" would be the same case.
        helper.assertTrue(new ItemStack(ModItems.ENDERITE_INGOT).typeHolder().is(ModTags.Items.VOID_PROTECTED),
                "test setup broken: enderite_ingot is not in " + ModTags.Items.VOID_PROTECTED.location());
        helper.assertTrue(!new ItemStack(Items.DIAMOND).typeHolder().is(ModTags.Items.VOID_PROTECTED),
                "test setup broken: minecraft:diamond is in " + ModTags.Items.VOID_PROTECTED.location());
        helper.assertTrue(VOID_FLOAT_DEPTH < 64 && VOID_RESCUE_DEPTH > 64,
                "test setup broken: the float depth has to stay inside vanilla's 64 block margin "
                        + "and the rescue depth has to be outside it");
        helper.assertTrue(VOID_JUST_ABOVE_THRESHOLD_DEPTH < VOID_RESCUE_THRESHOLD_DEPTH
                        && VOID_JUST_BELOW_THRESHOLD_DEPTH > VOID_RESCUE_THRESHOLD_DEPTH
                        && VOID_JUST_BELOW_THRESHOLD_DEPTH < 64,
                "test setup broken: the two threshold probes have to sit on either side of the "
                        + "rescue threshold and both inside vanilla's 64 block margin, otherwise "
                        + "one of them is measuring vanilla's deletion instead of the mixin");

        // Six separate columns inside the room's footprint, at least two blocks apart, so that
        // nothing can merge with anything - ItemEntity#mergeWithNeighbours works on a box half a
        // block wide.
        double floatingY = minY - VOID_FLOAT_DEPTH;
        double deepY = minY - VOID_RESCUE_DEPTH;
        // 20 blocks above the world floor: negative in the overworld (-44), a good 8 blocks of
        // open air above the gametest room, so the item free falls roughly 0.6 blocks in
        // VOID_SETTLE_TICKS ticks and nothing else can move it.
        double insideY = minY + 20;
        helper.assertTrue(insideY < 0.0,
                "test setup broken: the untouched control item is meant to sit at a NEGATIVE y "
                        + "inside the world (that is the case the old y < 0 constant got wrong), "
                        + "but the world floor is " + minY + " so it would sit at " + insideY);

        // The two probes that bracket the threshold, one block on either side of it.
        double justAboveY = minY - VOID_JUST_ABOVE_THRESHOLD_DEPTH;
        double justBelowY = minY - VOID_JUST_BELOW_THRESHOLD_DEPTH;
        double rescueTargetY = minY + VOID_RESCUE_TARGET_HEIGHT;

        ItemEntity floating = dropAt(helper, ModItems.ENDERITE_INGOT, 1.5, floatingY, 1.5);
        ItemEntity rescued = dropAt(helper, ModItems.ENDERITE_INGOT, 3.5, deepY, 1.5);
        ItemEntity doomed = dropAt(helper, Items.DIAMOND, 5.5, deepY, 1.5);
        ItemEntity insideWorld = dropAt(helper, ModItems.ENDERITE_INGOT, 3.5, insideY, 5.5);
        ItemEntity justAbove = dropAt(helper, ModItems.ENDERITE_INGOT, 1.5, justAboveY, 3.5);
        ItemEntity justBelow = dropAt(helper, ModItems.ENDERITE_INGOT, 5.5, justBelowY, 3.5);

        // The rescue height is read in the first tick after the drop, not after the settle: the
        // target y (world floor + 5) is the gametest room's own floor level, so the lifted item
        // lands INSIDE that floor block and vanilla's ItemEntity pushes it out again - upwards,
        // by about a block, on a tick chosen by (tickCount + id) % 4. That push is vanilla's and
        // depends on the entity id, i.e. on every entity any test before this one spawned; after
        // five ticks it had happened on one run and not on the next (1.07 blocks off on both
        // 1.21.11 loaders at once, never before). The mod's claim is the height it lifts to,
        // and that is exact one tick after the drop; where vanilla nudges it afterwards is
        // checked loosely below.
        double[] liftedTo = {Double.NaN};
        double[] justBelowLiftedTo = {Double.NaN};
        helper.runAfterDelay(1, () -> {
            liftedTo[0] = rescued.getY();
            justBelowLiftedTo[0] = justBelow.getY();
        });

        helper.startSequence()
                .thenExecuteAfter(VOID_SETTLE_TICKS, () -> {
                    // --- 1. just below the floor: frozen in place ---
                    helper.assertTrue(floating.isAlive(),
                            "an enderite ingot just below the world floor was removed instead of caught");
                    helper.assertTrue(floating.isNoGravity(),
                            "an enderite ingot below the world floor keeps falling; the mixin no longer "
                                    + "switches gravity off and the item is on its way to being deleted");
                    helper.assertTrue(Math.abs(floating.getY() - floatingY) < 0.1,
                            "an enderite ingot at y=" + floatingY + " (world floor " + minY + ") moved to y="
                                    + floating.getY() + "; it is only " + VOID_FLOAT_DEPTH + " blocks below "
                                    + "the floor, so it must float, not be teleported");

                    // --- 2. deep enough for vanilla to delete it: lifted back into the world ---
                    helper.assertTrue(rescued.isAlive(),
                            "an enderite ingot at y=" + deepY + " fell into the void; vanilla deletes "
                                    + "entities below " + (minY - 64) + " and the rescue did not happen");
                    helper.assertTrue(rescued.getY() >= minY,
                            "the rescued enderite ingot is still outside the world at y=" + rescued.getY()
                                    + " (world floor " + minY + ")");
                    helper.assertTrue(rescued.getY() <= minY + 16,
                            "the rescued enderite ingot should come back to y=" + (minY + 5)
                                    + ", five blocks above the world floor, but it is at y=" + rescued.getY()
                                    + "; a fixed rescue height instead of one derived from getMinY() is "
                                    + "exactly the pre-1.18 bug this test exists for");
                    helper.assertTrue(Math.abs(liftedTo[0] - rescueTargetY) < 0.5,
                            "the rescue put the enderite ingot at y=" + liftedTo[0] + " instead of y="
                                    + rescueTargetY + " (" + VOID_RESCUE_TARGET_HEIGHT + " blocks above the "
                                    + "world floor " + minY + ") one tick after the drop; the target height "
                                    + "moved, and everything between the floor and the first solid block is "
                                    + "still 'inside the world' as far as the bounds above are concerned");
                    helper.assertTrue(Math.abs(rescued.getY() - rescueTargetY) < 2.0,
                            "the rescued enderite ingot drifted from y=" + liftedTo[0] + " to y="
                                    + rescued.getY() + " within " + VOID_SETTLE_TICKS + " ticks; vanilla's "
                                    + "push out of the room floor accounts for about one block, not more");

                    // --- 3. an item outside the tag: the void keeps it ---
                    helper.assertTrue(doomed.isRemoved(),
                            "a plain diamond at y=" + deepY + " survived the void; the void protection now "
                                    + "applies to items that are not in " + ModTags.Items.VOID_PROTECTED.location());

                    // --- 4. inside the world: untouched, gravity included ---
                    helper.assertTrue(insideWorld.isAlive(),
                            "an enderite ingot inside the world was removed");
                    helper.assertTrue(!insideWorld.isNoGravity(),
                            "an enderite ingot at y=" + insideY + " is inside the world (floor " + minY
                                    + ") but the mixin switched its gravity off; dropped enderite would hang "
                                    + "in mid air in any deepslate level mine");
                    helper.assertTrue(insideWorld.getY() < 0.0 && insideWorld.getY() > minY,
                            "an enderite ingot inside the world at y=" + insideY + " was moved to y="
                                    + insideWorld.getY() + "; a rescue with a fixed target height would put "
                                    + "it at y=5, the void rescue must not touch it at all");
                    helper.assertTrue(Math.abs(insideWorld.getY() - insideY) < 3.0,
                            "an enderite ingot inside the world at y=" + insideY + " drifted to y="
                                    + insideWorld.getY() + "; " + VOID_SETTLE_TICKS + " ticks of free fall "
                                    + "are worth well under one block");

                    // --- 5. one block above the rescue threshold: float, do not lift ---
                    helper.assertTrue(justAbove.isAlive() && justAbove.isNoGravity(),
                            "an enderite ingot at y=" + justAboveY + " is below the world floor and has to "
                                    + "be caught; it is alive=" + justAbove.isAlive() + ", weightless="
                                    + justAbove.isNoGravity());
                    helper.assertTrue(Math.abs(justAbove.getY() - justAboveY) < 0.1,
                            "an enderite ingot " + VOID_JUST_ABOVE_THRESHOLD_DEPTH + " blocks below the "
                                    + "world floor was lifted to y=" + justAbove.getY() + "; the rescue is "
                                    + "supposed to start one block deeper, at " + VOID_RESCUE_THRESHOLD_DEPTH
                                    + " blocks, so the threshold has moved up");

                    // --- 6. one block below it: lift, do not leave it hanging ---
                    helper.assertTrue(justBelow.isAlive(),
                            "an enderite ingot at y=" + justBelowY + " was removed instead of rescued");
                    // Read one tick after the drop, for the same reason as the rescued probe above.
                    helper.assertTrue(Math.abs(justBelowLiftedTo[0] - rescueTargetY) < 0.5,
                            "an enderite ingot " + VOID_JUST_BELOW_THRESHOLD_DEPTH + " blocks below the "
                                    + "world floor stayed at y=" + justBelowLiftedTo[0] + " instead of coming "
                                    + "back to y=" + rescueTargetY + "; the rescue threshold has moved "
                                    + "deeper, and everything between " + VOID_RESCUE_THRESHOLD_DEPTH
                                    + " blocks and the new one floats out of reach forever");
                    helper.assertTrue(Math.abs(justBelow.getY() - rescueTargetY) < 2.0,
                            "the enderite ingot rescued from " + VOID_JUST_BELOW_THRESHOLD_DEPTH
                                    + " blocks below the floor drifted from y=" + justBelowLiftedTo[0]
                                    + " to y=" + justBelow.getY() + " within " + VOID_SETTLE_TICKS
                                    + " ticks; vanilla's push out of the room floor accounts for about "
                                    + "one block, not more");
                })
                .thenSucceed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Creates a fully connected mock server player, moves it into the test room and makes sure it
     * leaves the server again once the test is over -- a leaked mock player keeps the player list
     * non-empty and stalls the gametest server on shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 4.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** What vanilla's damage pipeline would subtract for the given source, in protection points. */
    private static void assertProtection(GameTestHelper helper, ServerPlayer player,
                                         DamageSource source, float expected, String what) {
        float actual = EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source);
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4F,
                what + " should be worth " + expected + " protection points, vanilla computed " + actual);
    }

    /**
     * Puts {@code helmet} on the player, refills the health bar and lands one hit, returning the
     * health that was lost. Resetting {@code invulnerableTime} is load bearing: without it the
     * second hit of a test would arrive inside the damage cooldown and be swallowed, which would
     * look exactly like perfect protection.
     */
    private static float damageTaken(GameTestHelper helper, ServerPlayer player, ItemStack helmet,
                                     DamageSource source) {
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        player.hurtServer(helper.getLevel(), source, HIT_DAMAGE);
        return player.getMaxHealth() - player.getHealth();
    }

    /** Every block interaction range modifier the enchantments on {@code stack} produce for a slot. */
    private static List<AttributeModifier> reachModifiers(ItemStack stack, EquipmentSlot slot) {
        List<AttributeModifier> found = new ArrayList<>();
        EnchantmentHelper.forEachModifier(stack, slot, (attribute, modifier) -> {
            if (attribute.value() == Attributes.BLOCK_INTERACTION_RANGE.value()) {
                found.add(modifier);
            }
        });
        return found;
    }

    /**
     * Drops one item at the given room relative x/z but at an <em>absolute</em> y, so the caller
     * can place it outside the build height -- {@code GameTestHelper#spawnItem} runs every
     * coordinate through {@code absoluteVec} and cannot reach below the world. The random launch
     * velocity the {@code ItemEntity} constructor hands out is cleared, and the entity is removed
     * again at the end of the test: the ones below the world floor are outside the structure the
     * gametest framework cleans up on its own.
     */
    private static ItemEntity dropAt(GameTestHelper helper, Item item, double relativeX, double absoluteY,
                                     double relativeZ) {
        Vec3 column = helper.absoluteVec(new Vec3(relativeX, 0.0, relativeZ));
        ItemEntity entity = new ItemEntity(helper.getLevel(), column.x, absoluteY, column.z,
                new ItemStack(item, 1));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNeverPickUp();
        helper.getLevel().addFreshEntity(entity);
        helper.runBeforeTestEnd(entity::discard);
        return entity;
    }

    private static ItemStack kineticArmour(GameTestHelper helper, Item piece, int level) {
        ItemStack stack = new ItemStack(piece);
        stack.enchant(enchantment(helper, ModEnchantments.KINETIC_PROTECTION), level);
        return stack;
    }

    private static ItemStack rangeTool(GameTestHelper helper, int level) {
        ItemStack stack = new ItemStack(ModItems.DIAMOND_CHISEL);
        stack.enchant(enchantment(helper, ModEnchantments.RANGE), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
