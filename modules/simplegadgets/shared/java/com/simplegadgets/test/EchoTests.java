package com.simplegadgets.test;

import com.google.gson.JsonObject;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.echo.EchoSounderItem;
import com.simplelib.api.GadgetActions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Echo Sounder (OW24-2 G4): recipe, names, link and jump, the charge (empty after a jump, unbreaking, creative), the
 * recharge, the cracked jump, the lock after an attempt, the cooldown, the dimension lock, the switch and the events that
 * SimpleBuilding turns into its advancement and statistic - ported from SimpleBuilding's {@code TweaksTests} and
 * {@code PadOverhaulTests}. The Fabric adapter registers them through {@link GadgetsTests#ALL} as module_game_test_echo_<name>.
 * Standalone and with SimpleBuilding (the recipe has two variants).
 */
public final class EchoTests {
    private static final Identifier COPPER_CORE = Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "copper_core");

    private EchoTests() {
    }

    private static boolean withSimpleBuilding() {
        return BuiltInRegistries.ITEM.containsKey(COPPER_CORE);
    }

    /** Seven nuggets, the recovery compass and a core: "NNN" / "NRN" / "ENN"; alone amethyst shards and a netherite ingot take the place of the enderite. */
    public static void theEchoSounderIsCraftedFromTheRecoveryCompassACoreAndSevenNuggets(GameTestHelper helper) {
        boolean sb = withSimpleBuilding();
        Item n = sb ? BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleores", "enderite_nugget")) : Items.AMETHYST_SHARD;
        Item core = sb ? BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "enderite_core")) : Items.NETHERITE_INGOT;
        helper.assertTrue(n != Items.AIR && core != Items.AIR, "the recipe ingredients are not registered");
        String recipeId = sb ? "simplegadgets:echo_sounder" : "simplegadgets:echo_sounder_without_simplebuilding";
        expectCrafting(helper, grid(n, n, n, n, Items.RECOVERY_COMPASS, n, core, n, n), GadgetsRegistry.ECHO_SOUNDER, recipeId);
        helper.assertTrue(craftingResult(helper, grid(n, n, n, n, Items.RECOVERY_COMPASS, n, n, core, n)).isEmpty(), "the previous bottom-middle core recipe still works");
        helper.assertTrue(craftingResult(helper, grid(n, n, n, n, core, n, n, Items.RECOVERY_COMPASS, n)).isEmpty(), "the swapped compass and core still craft");
        helper.assertTrue(craftingResult(helper, grid(null, n, n, n, Items.RECOVERY_COMPASS, n, core, n, n)).isEmpty(), "the echo sounder crafts with only six nuggets");
        helper.succeed();
    }

    /** Registry id, names and the texts: no ender pearl, no compass. */
    public static void theEchoSounderIsRegisteredAndNamed(GameTestHelper helper) {
        helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(GadgetsRegistry.ECHO_SOUNDER).toString(), "simplegadgets:echo_sounder", "registry id of the echo sounder");
        helper.assertTrue(GadgetsRegistry.ECHO_SOUNDER instanceof EchoSounderItem, "the echo sounder item class");
        ItemStack stack = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        helper.assertValueEqual(stack.getMaxStackSize(), 1, "the echo sounder stacks");
        helper.assertValueEqual(stack.getMaxDamage(), GadgetsConfig.echoMaxCharge, "the echo sounder's repair points");
        helper.assertValueEqual(GadgetsConfig.ECHO_CHARGE_DEFAULT, 1500, "default repair points");
        helper.assertTrue(stack.isValidRepairItem(new ItemStack(Items.ECHO_SHARD)), "echo shards do not repair the echo sounder at the anvil");
        helper.assertTrue(stack.typeHolder().is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE),
                "the echo sounder is missing from enchantable/durability, so unbreaking and mending cannot be put on it");
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        helper.assertValueEqual(en.get("item.simplegadgets.echo_sounder").getAsString(), "Echo Sounder", "english item name");
        helper.assertValueEqual(de.get("item.simplegadgets.echo_sounder").getAsString(), "Echolot", "german item name");
        for (JsonObject lang : List.of(en, de)) {
            for (String key : List.of("item.simplegadgets.echo_sounder.charged", "item.simplegadgets.echo_sounder.cracked",
                    "item.simplegadgets.echo_sounder.cracked_hint", "message.simplegadgets.feature_disabled")) {
                helper.assertTrue(lang.has(key), "lang key " + key + " is missing");
            }
        }
        helper.succeed();
    }

    /** Link with a right click, then jump over the lodestone without any pearl; the echo sounder stays, now empty. */
    public static void theEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        BlockPos abs = helper.absolutePos(lodestone);
        compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false)));
        helper.assertValueEqual(EchoSounderItem.target(compass), GlobalPos.of(helper.getLevel().dimension(), abs), "linked target of the echo sounder");
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "linking did not lock the echo sounder for a moment");
        for (int t = 0; t < EchoSounderItem.ATTEMPT_LOCK_TICKS; t++) {
            player.getCooldowns().tick();
        }
        helper.assertFalse(player.getInventory().hasAnyOf(java.util.Set.of(Items.ENDER_PEARL)), "the mock player starts with an ender pearl, so this proves nothing");
        helper.assertTrue(EchoSounderItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder refused to teleport");
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "the player landed at " + player.position() + " instead of on the lodestone");
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == compass, "the echo sounder was used up");
        helper.assertValueEqual(compass.getDamageValue(), EchoSounderItem.MAX_DAMAGE, "echo sounder damage after one jump");
        helper.assertFalse(EchoSounderItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder ignored its cooldown");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "pearls spent by a jump refused for cooldown");
        helper.succeed();
    }

    /** Even with Unbreaking III a successful jump empties the whole charge (the item stays, repairable). */
    public static void aJumpEmptiesTheWholeChargeEvenWithUnbreaking(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the echo sounder refused to charge");
        tickUse(player, EchoSounderItem.CHARGE_TICKS - 1);
        helper.assertValueEqual(compass.getDamageValue(), 0, "charging must not consume durability per tick");
        tickUse(player, 1);
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(helper.absolutePos(lodestone).above())) < 0.1, "the charged echo sounder did not teleport");
        helper.assertValueEqual(compass.getDamageValue(), compass.getMaxDamage(), "unbreaking must not discount the full charge");
        helper.assertValueEqual(compass.getBarWidth(), 0, "an empty echo sounder must have an empty durability bar");
        helper.assertFalse(compass.isEmpty(), "draining the charge must leave a repairable item");
        helper.assertTrue(EchoSounderItem.isCracked(compass), "the echo sounder is still charged after a jump with unbreaking");
        helper.succeed();
    }

    /** A modified stack capacity counts; creative mode neither empties nor shatters it. */
    public static void theEchoSounderHonorsStackCapacityAndCreativeMode(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        for (boolean creative : new boolean[]{false, true}) {
            ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
            player.getAbilities().instabuild = creative;
            ItemStack compass = linked(helper, lodestone);
            compass.set(DataComponents.MAX_DAMAGE, 2250);
            compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
            player.setItemInHand(InteractionHand.OFF_HAND, compass);
            helper.assertTrue(EchoSounderItem.teleport(player, InteractionHand.OFF_HAND, compass), "the echo sounder refused to jump");
            helper.assertValueEqual(compass.getDamageValue(), creative ? 0 : 2250, "damage with a modified stack capacity");
            helper.assertFalse(compass.isEmpty(), "the first jump destroyed the echo sounder");
            if (creative) {
                player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(compass));
                compass.setDamageValue(2250);
                helper.assertTrue(EchoSounderItem.teleport(player, InteractionHand.OFF_HAND, compass), "creative cracked echo sounder refused to jump");
                helper.assertFalse(compass.isEmpty(), "creative cracked echo sounder shattered");
                helper.assertValueEqual(compass.getDamageValue(), 2250, "creative changed the existing damage");
            }
        }
        helper.succeed();
    }

    /** 3 s of charge; whoever lets go earlier neither jumps nor loses anything, but the attempt locks the item for a moment. */
    public static void theEchoSounderChargesForThreeSecondsAndReleasingEarlyCostsNothing(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        Vec3 start = player.position();
        helper.assertValueEqual(compass.getUseDuration(player), EchoSounderItem.CHARGE_TICKS, "charge ticks of a charged echo sounder");
        helper.assertValueEqual(EchoSounderItem.CHARGE_TICKS, 60, "charge ticks (3 seconds)");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "using the echo sounder did not start a charge");
        helper.assertTrue(player.isUsingItem(), "the echo sounder does not charge");
        tickUse(player, EchoSounderItem.CHARGE_TICKS - 1);
        helper.assertTrue(player.isUsingItem(), "the charge ended before 3 seconds");
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the player jumped before the charge was full");
        player.releaseUsingItem();
        helper.assertFalse(player.isUsingItem(), "releasing did not stop the charge");
        tickUse(player, 5);
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "an early release still jumped");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls left after an early release");
        helper.assertValueEqual(compass.getDamageValue(), 0, "echo sounder damage after an early release");
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "an early release did not lock the echo sounder for a moment");
        helper.succeed();
    }

    /** The full charge jumps; the echo sounder is empty afterwards (no glint, even with Mending) and charges twice as long. */
    public static void aFullChargeJumpsAndLeavesTheEchoSounderEmpty(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        helper.assertTrue(compass.hasFoil(), "a charged echo sounder has no glint");
        helper.assertFalse(compass.isBarVisible(), "a fully charged echo sounder shows a damage bar");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "using the echo sounder did not start a charge");
        tickUse(player, EchoSounderItem.CHARGE_TICKS);
        BlockPos abs = helper.absolutePos(lodestone);
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "a full charge left the player at " + player.position());
        helper.assertFalse(player.isUsingItem(), "the charge did not end with the jump");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == compass && !compass.isEmpty(), "the first jump destroyed the echo sounder");
        helper.assertValueEqual(compass.getDamageValue(), EchoSounderItem.MAX_DAMAGE, "echo sounder damage after the first jump");
        helper.assertValueEqual(compass.getMaxDamage(), GadgetsConfig.echoMaxCharge, "repair points of an empty echo sounder");
        helper.assertTrue(EchoSounderItem.isCracked(compass), "the empty echo sounder is not cracked");
        helper.assertFalse(compass.hasFoil(), "the empty echo sounder still has a glint");
        helper.assertTrue(compass.isBarVisible(), "the empty echo sounder has no durability bar");
        helper.assertValueEqual(compass.getBarWidth(), 0, "empty durability bar width");
        helper.assertValueEqual(compass.getUseDuration(player), EchoSounderItem.CRACKED_CHARGE_TICKS, "charge ticks of an empty echo sounder");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls spent by the jump (none are needed)");
        helper.succeed();
    }

    /** Charged again only after every repair point (Mending: 2 per XP point); echo shards repair at the anvil. */
    public static void theEchoSounderIsOnlyChargedAgainAfterAllItsRepairPoints(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        compass.setDamageValue(EchoSounderItem.MAX_DAMAGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        Vec3 at = player.position();
        int xpToFill = EchoSounderItem.MAX_DAMAGE / 2;
        player.takeXpDelay = 0;
        new ExperienceOrb(helper.getLevel(), at.x, at.y, at.z, xpToFill - 1).playerTouch(player);
        helper.assertValueEqual(compass.getDamageValue(), 2, "damage left after " + (xpToFill - 1) + " XP of mending");
        helper.assertTrue(EchoSounderItem.isCracked(compass), "the echo sounder counts as repaired with 2 points missing");
        helper.assertFalse(compass.hasFoil(), "the not fully repaired echo sounder has a glint");
        helper.assertTrue(compass.isBarVisible(), "partial repair hides the durability bar");
        helper.assertValueEqual(compass.getUseDuration(player), EchoSounderItem.CRACKED_CHARGE_TICKS, "charge ticks with 2 points missing");
        player.takeXpDelay = 0;
        new ExperienceOrb(helper.getLevel(), at.x, at.y, at.z, 1).playerTouch(player);
        helper.assertValueEqual(compass.getDamageValue(), 0, "damage after " + xpToFill + " XP of mending");
        helper.assertFalse(EchoSounderItem.isCracked(compass), "the fully repaired echo sounder is still cracked");
        helper.assertTrue(compass.hasFoil(), "the fully repaired echo sounder has no glint");
        helper.assertFalse(compass.isBarVisible(), "full repair leaves a damage bar");
        helper.assertValueEqual(compass.getUseDuration(player), EchoSounderItem.CHARGE_TICKS, "charge ticks after the full repair");
        helper.succeed();
    }

    /** Not fully repaired: the charge takes twice as long, the jump works, then the echo sounder shatters; both events are told. */
    public static void aCrackedEchoSounderChargesTwiceAsLongAndShattersAfterTheJump(GameTestHelper helper) {
        List<String> told = new ArrayList<>();
        GadgetActions.listen(GadgetActions.ECHO_TELEPORT, p -> told.add("jump"));
        GadgetActions.listen(GadgetActions.ECHO_SHATTER, p -> told.add("shatter"));
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        compass.setDamageValue(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        Vec3 start = player.position();
        helper.assertValueEqual(EchoSounderItem.CRACKED_CHARGE_TICKS, 2 * EchoSounderItem.CHARGE_TICKS, "cracked charge ticks");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the cracked echo sounder does not charge");
        tickUse(player, EchoSounderItem.CHARGE_TICKS);
        helper.assertTrue(player.isUsingItem(), "the cracked echo sounder stopped charging after 3 seconds");
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the cracked echo sounder jumped after 3 seconds");
        tickUse(player, EchoSounderItem.CHARGE_TICKS - 1);
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the cracked echo sounder jumped a tick early");
        helper.assertTrue(told.isEmpty(), "events were told before the jump: " + told);
        tickUse(player, 1);
        BlockPos abs = helper.absolutePos(lodestone);
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "the cracked echo sounder left the player at " + player.position());
        helper.assertTrue(compass.isEmpty(), "the cracked echo sounder survived its jump");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "something is left in the hand after the echo sounder shattered");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls spent by the shattering jump (none are needed)");
        helper.assertTrue(told.contains("jump") && told.contains("shatter"), "the jump and the shattering were not told: " + told);
        helper.succeed();
    }

    /** A jump into or out of a locked dimension (echoBlockedDimensions) is refused and costs nothing; so does the off switch. */
    public static void aBlockedJumpCostsNothing(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has no nether");
        BlockPos lodestone = new BlockPos(0, 100, 0);
        BlockState before = nether.getBlockState(lodestone);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.NETHER, lodestone)), true));
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        String blocked = GadgetsConfig.echoBlockedDimensions;
        boolean enabled = GadgetsConfig.echoEnabled;
        try {
            nether.setBlock(lodestone, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            GadgetsConfig.echoBlockedDimensions = "the_nether";
            helper.assertFalse(EchoSounderItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder reports a jump into a locked nether");
            helper.assertTrue(player.level() == helper.getLevel(), "the player got into the locked nether");
            helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls left after a blocked jump");
            helper.assertValueEqual(compass.getDamageValue(), 0, "echo sounder damage after a blocked jump");
            GadgetsConfig.echoBlockedDimensions = "";
            GadgetsConfig.echoEnabled = false;
            helper.assertFalse(EchoSounderItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the switched-off echo sounder jumped");
            helper.assertTrue(player.level() == helper.getLevel(), "the player got into the nether although the echo sounder is off");
            helper.assertValueEqual(compass.getDamageValue(), 0, "echo sounder damage after the switched-off attempt");
        } finally {
            GadgetsConfig.echoBlockedDimensions = blocked;
            GadgetsConfig.echoEnabled = enabled;
            nether.setBlock(lodestone, before, Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    /** The cooldown after a jump is four times the old 6 s: 480 ticks (24 s). */
    public static void theCooldownAfterAJumpIsTwentyFourSeconds(GameTestHelper helper) {
        helper.assertValueEqual(EchoSounderItem.COOLDOWN_TICKS, 4 * 120, "cooldown of the echo sounder");
        helper.assertValueEqual(GadgetsConfig.echoJumpCooldownTicks, 480, "configured jump cooldown");
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        helper.assertTrue(EchoSounderItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder did not jump");
        for (int t = 0; t < 121; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "the echo sounder is off cooldown after the old 6 s");
        for (int t = 121; t < EchoSounderItem.COOLDOWN_TICKS; t++) {
            player.getCooldowns().tick();
        }
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "the echo sounder is still on cooldown after 24 s");
        helper.succeed();
    }

    /** A right click on the lodestone it is already linked to does nothing; another lodestone links anew. */
    public static void theEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo(GameTestHelper helper) {
        BlockPos first = new BlockPos(6, 1, 6);
        BlockPos second = new BlockPos(2, 1, 6);
        helper.setBlock(first, Blocks.LODESTONE);
        helper.setBlock(second, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemStack compass = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        GlobalPos firstPos = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(first));

        InteractionResult link = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, first)));
        helper.assertTrue(link.consumesAction() && firstPos.equals(EchoSounderItem.target(compass)), "the first click did not link: " + link);
        helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "the first link had no effect");
        player.removeAllEffects();

        InteractionResult again = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, first)));
        helper.assertTrue(again == InteractionResult.FAIL, "clicking the linked lodestone again answered " + again + " instead of FAIL");
        helper.assertFalse(player.hasEffect(MobEffects.BLINDNESS), "clicking the linked lodestone again replayed the link effects");
        helper.assertFalse(player.isUsingItem(), "clicking the linked lodestone again started charging");
        helper.assertValueEqual(EchoSounderItem.target(compass), firstPos, "target after clicking the same lodestone");

        InteractionResult other = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, second)));
        helper.assertTrue(other.consumesAction(), "a different lodestone did not link: " + other);
        helper.assertValueEqual(EchoSounderItem.target(compass), GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(second)),
                "target after clicking another lodestone");
        helper.succeed();
    }

    /**
     * Deliberate use: linking and every attempt that does not jump lock the item for 1 s right next to the lodestone, up to 5 s
     * from 1000 blocks away or in another dimension (shown as the item cooldown). The failure click sounds once per press.
     */
    public static void theEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertValueEqual(EchoSounderItem.ATTEMPT_LOCK_TICKS, 5 * 20, "longest attempt lock of the echo sounder");
        helper.assertValueEqual(GadgetsConfig.echoAttemptLockTicks, 100, "configured attempt lock");
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;

        BlockPos abs = helper.absolutePos(lodestone);
        GlobalPos near = GlobalPos.of(level.dimension(), abs);
        GlobalPos far = GlobalPos.of(level.dimension(), abs.offset(2000, 0, 0));
        GlobalPos halfway = GlobalPos.of(level.dimension(), BlockPos.containing(player.position()).offset(500, 0, 0));
        GlobalPos nether = GlobalPos.of(Level.NETHER, abs);
        int nearLock = EchoSounderItem.attemptLockTicks(player, near);
        helper.assertTrue(nearLock >= 20 && nearLock <= 22, "attempt lock next to the lodestone: " + nearLock);
        helper.assertValueEqual(EchoSounderItem.attemptLockTicks(player, far), 100, "attempt lock 2000 blocks away");
        int half = EchoSounderItem.attemptLockTicks(player, halfway);
        helper.assertTrue(half >= 58 && half <= 62, "attempt lock 500 blocks away: " + half);
        helper.assertValueEqual(EchoSounderItem.attemptLockTicks(player, nether), 100, "attempt lock into another dimension");
        helper.assertValueEqual(EchoSounderItem.attemptLockTicks(player, null), 0, "attempt lock of an unlinked echo sounder");

        ItemStack compass = linked(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        InteractionResult charge = compass.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(charge.consumesAction() && player.isUsingItem(), "the linked echo sounder did not start charging: " + charge);
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "starting to charge already locked the echo sounder");
        player.releaseUsingItem();
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "releasing early did not lock the echo sounder");
        int lock = EchoSounderItem.attemptLockTicks(player, EchoSounderItem.target(compass));
        for (int t = 1; t < lock; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "the attempt lock ended before " + lock + " ticks");
        player.getCooldowns().tick();
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "the attempt lock lasts longer than " + lock + " ticks");

        ItemStack missing = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        missing.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(far), false));
        player.setItemInHand(InteractionHand.MAIN_HAND, missing);
        InteractionResult refused = missing.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(refused == InteractionResult.FAIL && !player.isUsingItem(), "an echo sounder without its lodestone charged: " + refused);
        for (int t = 1; t < EchoSounderItem.ATTEMPT_LOCK_TICKS; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(missing), "the lock after a missing lodestone ended before 5 s");
        player.getCooldowns().tick();
        helper.assertFalse(player.getCooldowns().isOnCooldown(missing), "the lock after a missing lodestone lasts longer than 5 s");

        ItemStack unlinked = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        player.setItemInHand(InteractionHand.MAIN_HAND, unlinked);
        unlinked.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.getCooldowns().isOnCooldown(unlinked), "an unlinked echo sounder locked itself");

        ItemStack fresh = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        player.setItemInHand(InteractionHand.MAIN_HAND, fresh);
        InteractionResult link = fresh.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, lodestone)));
        helper.assertTrue(link.consumesAction() && player.getCooldowns().isOnCooldown(fresh), "linking did not lock the echo sounder: " + link);

        helper.startSequence()
                .thenExecuteAfter(EchoSounderItem.HELD_GAP_TICKS + 2, () -> {
                    helper.assertTrue(EchoSounderItem.failCue(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f),
                            "the first failed click after a pause made no sound");
                    helper.assertFalse(EchoSounderItem.failCue(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f),
                            "a repeated click of the held use key sounded again");
                })
                .thenExecuteAfter(4, () -> helper.assertFalse(EchoSounderItem.failCue(player,
                        net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f), "the held use key repeating after 4 ticks sounded again"))
                .thenExecuteAfter(EchoSounderItem.HELD_GAP_TICKS + 2, () -> helper.assertTrue(EchoSounderItem.failCue(player,
                        net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f), "a new press after letting go made no sound"))
                .thenSucceed();
    }

    // ---- helpers

    private static ItemStack linked(GameTestHelper helper, BlockPos lodestone) {
        ItemStack compass = new ItemStack(GadgetsRegistry.ECHO_SOUNDER);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
        return compass;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
    }

    private static JsonObject langFile(GameTestHelper helper, String code) {
        try (java.io.InputStream in = EchoTests.class.getResourceAsStream("/assets/simplegadgets/lang/" + code + ".json")) {
            helper.assertTrue(in != null, "no " + code + ".json on the classpath");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + code + ".json", e);
        }
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** {@code ticks} use ticks like Vanilla's LivingEntity#updatingUsingItem, by reflection (the mock player is not ticked). */
    private static void tickUse(ServerPlayer player, int ticks) {
        try {
            java.lang.reflect.Method update = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("updateUsingItem", ItemStack.class);
            update.setAccessible(true);
            for (int i = 0; i < ticks && player.isUsingItem(); i++) {
                update.invoke(player, player.getUseItem());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("LivingEntity#updateUsingItem not reachable", e);
        }
    }

    private static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static Optional<ItemStack> craftingResult(GameTestHelper helper, CraftingInput grid) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(holder -> holder.value().assemble(grid));
    }

    private static void expectCrafting(GameTestHelper helper, CraftingInput grid, Item result, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the documented pattern for " + result + " matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched for " + result);
        helper.assertTrue(match.get().value().assemble(grid).is(result), "the recipe for " + result + " made something else");
    }
}
