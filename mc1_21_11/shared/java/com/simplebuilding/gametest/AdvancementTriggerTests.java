package com.simplebuilding.gametest;

import com.simplebuilding.advancement.AdvancementChecks;
import com.simplebuilding.advancement.ModCounters;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.tweaks.item.EchoCompassItem;
import com.simplebuilding.tweaks.item.LaserBeam;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.SledgehammerUsageEvent;
import com.simplebuilding.util.StripMinerUsageEvent;
import com.simplebuilding.util.VeinMinerUsageEvent;
import com.simplebuilding.util.VersatilityUsageEvent;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The hooks behind the advancements of the 2026-09-28 wave: each test drives the real code path of
 * one feature (the same call the loader hooks, the packet handler or the block make) and checks that
 * the advancement waiting for it is granted then - and not before. {@code AdvancementTreeTests}
 * covers the tree itself (every file loads, every feature has a listener, firing a feature grants
 * its advancements); this class covers that the features fire at the right moment.
 *
 * <p>What breaks these tests: a hook call that was removed or moved behind an early return, a
 * counter that is not saved or not carried over on respawn, a criterion that waits for the wrong
 * count, component or block.
 */
public final class AdvancementTriggerTests {

    private AdvancementTriggerTests() {
    }

    private static final String MOD_ID = "simplebuilding";

    /**
     * The counter criterion ({@code simplebuilding:counter}): "Sculptor" waits for 1,000 chisel
     * steps - 999 are not enough, the thousandth grants it. The count is part of the player's save
     * data (it comes back after {@code load}) and follows the player through a respawn after death
     * and through the end portal ({@code restoreFrom} with either flag). Non-positive amounts count
     * nothing.
     */
    public static void theCounterGrantsAtItsThresholdAndSurvivesSaveAndRespawn(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder sculptor = require(helper, manager, "chisel/sculptor");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));

        ModCounters.add(player, ModCounters.CHISEL_STEPS, 999);
        ModCounters.add(player, ModCounters.CHISEL_STEPS, 0);
        ModCounters.add(player, ModCounters.CHISEL_STEPS, -5);
        helper.assertValueEqual(ModCounters.get(player, ModCounters.CHISEL_STEPS), 999L, "chisel steps after 999, 0 and -5");
        helper.assertTrue(!done(player, sculptor), "Sculptor was granted at 999 of 1,000 chisel steps");
        ModCounters.add(player, ModCounters.CHISEL_STEPS, 1);
        helper.assertTrue(done(player, sculptor), "the 1,000th chisel step did not grant Sculptor");
        ModCounters.add(player, ModCounters.WAND_BLOCKS, 42);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        helper.assertTrue(saved.getCompound(ModCounters.NBT_KEY).isPresent(), "the player save carries no " + ModCounters.NBT_KEY);
        ServerPlayer loaded = mockPlayer(helper, new Vec3(2.5, 2.0, 1.5));
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(ModCounters.get(loaded, ModCounters.CHISEL_STEPS), 1000L, "chisel steps after loading the save");
        helper.assertValueEqual(ModCounters.get(loaded, ModCounters.WAND_BLOCKS), 42L, "wand blocks after loading the save");

        ServerPlayer respawned = mockPlayer(helper, new Vec3(3.5, 2.0, 1.5));
        respawned.restoreFrom(player, false);
        helper.assertValueEqual(ModCounters.get(respawned, ModCounters.WAND_BLOCKS), 42L, "wand blocks after a respawn after death");
        ServerPlayer travelled = mockPlayer(helper, new Vec3(4.5, 2.0, 1.5));
        travelled.restoreFrom(player, true);
        helper.assertValueEqual(ModCounters.get(travelled, ModCounters.CHISEL_STEPS), 1000L, "chisel steps after the end portal");
        TestCleanup.succeed(helper);
    }

    /**
     * The real counting path of the sledgehammer: a swing on the middle of a flat 3x3 stone face
     * breaks the eight blocks around it, and exactly those eight count towards "Demolition Crew".
     */
    public static void aSledgehammerSwingCountsTheBlocksItTookAlong(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.setXRot(90.0F);
        player.getAbilities().instabuild = false;
        BlockPos centre = new BlockPos(3, 1, 3);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        BlockPos origin = helper.absolutePos(centre);
        SledgehammerUsageEvent.handleBeforeBlockBreak(helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
        helper.assertValueEqual(ModCounters.get(player, ModCounters.HAMMER_BLOCKS), 8L, "blocks counted for one 3x3 swing");
        TestCleanup.succeed(helper);
    }

    /**
     * Vein Miner, Strip Miner and Versatility each earn their advancement the first time they do
     * something - and not when the player does not sneak (then they do nothing).
     */
    public static void theMiningEnchantmentsEarnTheirAdvancementsOnFirstUse(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder vein = require(helper, manager, "mining/all_in_vein");
        AdvancementHolder strip = require(helper, manager, "mining/tunnel_vision");
        AdvancementHolder versatility = require(helper, manager, "mining/right_tool_for_the_job");
        ServerPlayer player = mockPlayer(helper, new Vec3(6.5, 6.0, 6.5));
        player.setXRot(90.0F);
        player.getAbilities().instabuild = false;

        // --- Vein Miner: two connected iron ores ---
        BlockPos ore = new BlockPos(1, 1, 1);
        helper.setBlock(ore, Blocks.IRON_ORE);
        helper.setBlock(ore.east(), Blocks.IRON_ORE);
        ItemStack veinPickaxe = enchanted(helper, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.VEIN_MINER, 1);
        player.setShiftKeyDown(false);
        breakHook(helper, player, veinPickaxe, ore, true);
        helper.assertTrue(!done(player, vein), "Vein Miner earned its advancement without sneaking");
        player.setShiftKeyDown(true);
        breakHook(helper, player, veinPickaxe, ore, true);
        helper.assertTrue(helper.getBlockState(ore.east()).isAir(), "test setup broken: Vein Miner did not take the second ore");
        helper.assertTrue(done(player, vein), "breaking a vein with Vein Miner did not earn All in Vein");

        // --- Strip Miner: looking down onto a column of stone ---
        BlockPos top = new BlockPos(4, 3, 4);
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(4, y, 4), Blocks.STONE);
        }
        ItemStack stripPickaxe = enchanted(helper, new ItemStack(Items.IRON_PICKAXE), ModEnchantments.STRIP_MINER, 1);
        player.setShiftKeyDown(false);
        breakHook(helper, player, stripPickaxe, top, false);
        helper.assertTrue(!done(player, strip), "Strip Miner earned its advancement without sneaking");
        player.setShiftKeyDown(true);
        breakHook(helper, player, stripPickaxe, top, false);
        helper.assertTrue(helper.getBlockState(top.below()).isAir(), "test setup broken: Strip Miner did not dig below the block");
        helper.assertTrue(done(player, strip), "digging with Strip Miner did not earn Tunnel Vision");

        // --- Versatility: a shovel on stone reaches for the pickaxe in the hotbar ---
        BlockPos stone = new BlockPos(6, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, enchanted(helper, new ItemStack(Items.DIAMOND_SHOVEL), ModEnchantments.VERSATILITY, 1));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_PICKAXE));
        player.getInventory().setSelectedSlot(0);
        player.setShiftKeyDown(false);
        VersatilityUsageEvent.handleAttackBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND, helper.absolutePos(stone), Direction.UP);
        helper.assertTrue(!done(player, versatility), "Versatility earned its advancement without sneaking");
        player.setShiftKeyDown(true);
        VersatilityUsageEvent.handleAttackBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND, helper.absolutePos(stone), Direction.UP);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE), "test setup broken: Versatility did not swap");
        helper.assertTrue(done(player, versatility), "the Versatility swap did not earn The Right Tool for the Job");
        player.setShiftKeyDown(false);
        TestCleanup.succeed(helper);
    }

    /**
     * Air Jump: the server accepts the jump packet only in the air and with the enchantment on the
     * boots; the first accepted one earns "Leap of Faith". Kinetic Protection: fly_into_wall damage
     * earns "Crumple Zone" only while an armor piece carries the enchantment - not unenchanted,
     * and not for damage of another kind.
     */
    public static void airJumpAndKineticProtectionEarnTheirAdvancementsOnFirstUse(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder leap = require(helper, manager, "tweaks/leap_of_faith");
        AdvancementHolder crumple = require(helper, manager, "tweaks/crumple_zone");
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 3.0, 2.5));

        player.setOnGround(false);
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(!done(player, leap), "an air jump without Air Jump boots earned Leap of Faith");
        player.setItemSlot(EquipmentSlot.FEET, enchanted(helper, new ItemStack(Items.IRON_BOOTS), ModEnchantments.DOUBLE_JUMP, 1));
        player.setOnGround(true);
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(!done(player, leap), "a jump packet on the ground earned Leap of Faith");
        player.setOnGround(false);
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), player);
        helper.assertTrue(done(player, leap), "an accepted air jump did not earn Leap of Faith");

        player.getAbilities().invulnerable = false;
        player.setInvulnerable(false);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        hurt(helper, player, helper.getLevel().damageSources().flyIntoWall());
        helper.assertTrue(!done(player, crumple), "flying into a wall without Kinetic Protection earned Crumple Zone");
        player.setItemSlot(EquipmentSlot.CHEST, enchanted(helper, new ItemStack(Items.IRON_CHESTPLATE), ModEnchantments.KINETIC_PROTECTION, 1));
        hurt(helper, player, helper.getLevel().damageSources().generic());
        helper.assertTrue(!done(player, crumple), "generic damage earned Crumple Zone");
        hurt(helper, player, helper.getLevel().damageSources().flyIntoWall());
        helper.assertTrue(done(player, crumple), "flying into a wall with Kinetic Protection did not earn Crumple Zone");
        TestCleanup.succeed(helper);
    }

    /**
     * The component advancements follow the inventory: "Full Radiance" wants an armor piece at
     * Radiance 5 (four upgrades are not enough), "A Splash of Color" a dyed backpack, bundle or
     * quiver (an undyed one is not enough), "Full Spectrum" all 16 colored Octants (15 are not).
     */
    public static void radianceDyedStorageAndAllOctantColorsFollowTheInventory(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder radiance = require(helper, manager, "hammer/full_radiance");
        AdvancementHolder dyed = require(helper, manager, "storage/splash_of_color");
        AdvancementHolder spectrum = require(helper, manager, "octant/full_spectrum");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        player.getInventory().clearContent();

        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        for (int i = 0; i < 4; i++) {
            GlowingTrimUtils.incrementEmissionLevel(helmet);
        }
        give(player, helmet.copy(), 0);
        helper.assertTrue(!done(player, radiance), "Radiance 4 earned Full Radiance");
        GlowingTrimUtils.incrementEmissionLevel(helmet);
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(helmet), 5, "Radiance after five upgrades");
        give(player, helmet, 1);
        helper.assertTrue(done(player, radiance), "an armor piece at Radiance 5 did not earn Full Radiance");

        give(player, new ItemStack(ModItems.QUIVER), 2);
        helper.assertTrue(!done(player, dyed), "an undyed quiver earned A Splash of Color");
        ItemStack backpack = new ItemStack(ModItems.REINFORCED_BACKPACK);
        backpack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26));
        give(player, backpack, 3);
        helper.assertTrue(done(player, dyed), "a dyed backpack did not earn A Splash of Color");

        List<DyeColor> colours = List.of(DyeColor.values());
        for (int i = 0; i < colours.size() - 1; i++) {
            give(player, new ItemStack(ModItems.COLORED_OCTANT_ITEMS.get(colours.get(i))), 4 + i);
        }
        helper.assertTrue(!done(player, spectrum), "15 of 16 Octant colors earned Full Spectrum");
        player.getInventory().setItem(8, new ItemStack(ModItems.COLORED_OCTANT_ITEMS.get(colours.get(colours.size() - 1))));
        player.inventoryMenu.broadcastChanges();
        helper.assertTrue(done(player, spectrum), "all 16 Octant colors did not earn Full Spectrum");
        TestCleanup.succeed(helper);
    }

    /**
     * "Fashion Statement": the player tick notices four armor pieces with the same bonus trim
     * pattern. Three coast pieces and a ward piece are not a set, and with trim benefits switched
     * off a full set does not count either.
     */
    public static void aFullBonusTrimSetIsNoticedByThePlayerTick(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder fashion = require(helper, manager, "trims/fashion_statement");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        Holder<TrimMaterial> iron = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.IRON);
        Holder<TrimPattern> coast = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST);
        Holder<TrimPattern> ward = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.WARD);

        player.setItemSlot(EquipmentSlot.HEAD, trimmed(Items.IRON_HELMET, new ArmorTrim(iron, coast)));
        player.setItemSlot(EquipmentSlot.CHEST, trimmed(Items.IRON_CHESTPLATE, new ArmorTrim(iron, coast)));
        player.setItemSlot(EquipmentSlot.LEGS, trimmed(Items.IRON_LEGGINGS, new ArmorTrim(iron, coast)));
        player.setItemSlot(EquipmentSlot.FEET, trimmed(Items.IRON_BOOTS, new ArmorTrim(iron, ward)));
        helper.assertTrue(!AdvancementChecks.wearsFullBonusTrimSet(player), "three coast pieces and a ward piece count as a full set");
        tickAt(player, AdvancementChecks.INTERVAL * 3);
        helper.assertTrue(!done(player, fashion), "a mixed set earned Fashion Statement");

        player.setItemSlot(EquipmentSlot.FEET, trimmed(Items.IRON_BOOTS, new ArmorTrim(iron, coast)));
        com.simplebuilding.util.TrimBenefitUser user = (com.simplebuilding.util.TrimBenefitUser) player;
        user.simplebuilding$setTrimBenefitsEnabled(false);
        helper.assertTrue(!AdvancementChecks.wearsFullBonusTrimSet(player), "a full set counts with trim benefits switched off");
        user.simplebuilding$setTrimBenefitsEnabled(true);
        tickAt(player, AdvancementChecks.INTERVAL * 3 + 1);
        helper.assertTrue(!done(player, fashion), "the check ran on a tick between two intervals");
        tickAt(player, AdvancementChecks.INTERVAL * 4);
        helper.assertTrue(done(player, fashion), "four coast pieces did not earn Fashion Statement on the next check");
        TestCleanup.succeed(helper);
    }

    /** Sneak-using a backpack on a block sets it down ({@code BlockItem#place}) and earns "Pitching Camp". */
    public static void settingDownTheBackpackEarnsPitchingCamp(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder camp = require(helper, manager, "storage/pitching_camp");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 3.5));
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
        BlockPos absolute = helper.absolutePos(floor);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);

        player.setShiftKeyDown(false);
        backpack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(!done(player, camp), "using a backpack without sneaking earned Pitching Camp");
        player.setShiftKeyDown(true);
        backpack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        player.setShiftKeyDown(false);
        helper.assertTrue(helper.getBlockState(floor.above()).is(ModBlocks.BACKPACK), "test setup broken: the backpack was not set down");
        helper.assertTrue(done(player, camp), "setting the backpack down did not earn Pitching Camp");
        TestCleanup.succeed(helper);
    }

    /** A netherite nugget on a worn Netherite Piston repairs it and earns "Good as New"; on an intact one it does neither. */
    public static void repairingTheBreakerPistonEarnsGoodAsNew(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder repaired = require(helper, manager, "machines/good_as_new");
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.getAbilities().instabuild = false;
        BlockPos piston = new BlockPos(2, 1, 2);
        net.minecraft.world.level.block.state.BlockState upright = ModBlocks.NETHERITE_PISTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, Direction.UP);
        ItemStack nugget = new ItemStack(ModItems.NETHERITE_NUGGET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, nugget);
        BlockPos absolute = helper.absolutePos(piston);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false);

        helper.setBlock(piston, upright);
        helper.getLevel().getBlockState(absolute).useItemOn(nugget, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(!done(player, repaired), "a nugget on an intact piston earned Good as New");
        helper.setBlock(piston, NetheriteBreakerPistonBlock.withDamage(upright, 150));
        helper.getLevel().getBlockState(absolute).useItemOn(nugget, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertValueEqual(NetheriteBreakerPistonBlock.damageOf(helper.getBlockState(piston)), 0, "damage after the repair");
        helper.assertTrue(done(player, repaired), "repairing the Netherite Piston did not earn Good as New");
        TestCleanup.succeed(helper);
    }

    /**
     * "Not Today, Void": a void-protected item below the world is caught on its first tick there,
     * and its thrower earns the advancement. A stone block thrown the same way and a protected item
     * without a thrower earn nothing.
     */
    public static void theVoidCatchesAThrownEnderiteItemForItsThrower(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder saved = require(helper, manager, "enderite/not_today_void");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        Vec3 column = helper.absoluteVec(new Vec3(2.5, 0.0, 2.5));
        double below = helper.getLevel().getMinY() - 2.0;

        ItemEntity stone = new ItemEntity(helper.getLevel(), column.x, below, column.z, new ItemStack(Items.STONE));
        stone.setThrower(player);
        ItemEntity orphan = new ItemEntity(helper.getLevel(), column.x, below, column.z, new ItemStack(ModItems.RAW_ENDERITE));
        ItemEntity thrown = new ItemEntity(helper.getLevel(), column.x, below, column.z, new ItemStack(ModItems.RAW_ENDERITE));
        thrown.setThrower(player);
        try {
            stone.tick();
            orphan.tick();
            helper.assertTrue(orphan.isNoGravity(), "test setup broken: the raw enderite was not caught below the world");
            helper.assertTrue(!done(player, saved), "a stone block or an item nobody threw earned Not Today, Void");
            thrown.tick();
            helper.assertTrue(done(player, saved), "the caught Raw Enderite did not earn its thrower Not Today, Void");
        } finally {
            stone.discard();
            orphan.discard();
            thrown.discard();
        }
        TestCleanup.succeed(helper);
    }

    /**
     * An Echo Sounder that is not fully repaired still jumps, then shatters - and that earns
     * "Broken Record". Before the jump the advancement is open.
     */
    public static void theCrackedEchoSounderShattersAndEarnsBrokenRecord(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder broken = require(helper, manager, "enderite/broken_record");
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
        compass.setDamageValue(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        helper.assertTrue(!done(player, broken), "Broken Record was done before the jump");
        helper.assertTrue(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the cracked echo sounder did not jump");
        helper.assertTrue(compass.isEmpty(), "test setup broken: the cracked echo sounder did not shatter");
        helper.assertTrue(done(player, broken), "the shattered echo sounder did not earn Broken Record");
        TestCleanup.succeed(helper);
    }

    /**
     * The lens beam on TNT primes it after the dwell time and earns "Remote Detonation"; melting
     * ice earns only "Burning Focus".
     */
    public static void theLensBeamPrimingTntEarnsRemoteDetonation(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder focus = require(helper, manager, "gadgets/burning_focus");
        AdvancementHolder detonation = require(helper, manager, "gadgets/remote_detonation");
        ServerPlayer player = mockPlayer(helper, new Vec3(5.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        var rules = helper.getLevel().getGameRules();
        boolean tntExplodes = rules.get(GameRules.TNT_EXPLODES);
        try {
            rules.set(GameRules.TNT_EXPLODES, true, helper.getLevel().getServer());
            BlockPos ice = new BlockPos(2, 2, 4);
            helper.setBlock(ice.below(), Blocks.STONE);
            helper.setBlock(ice, Blocks.ICE);
            beam(helper, player, lens, ice, LaserBeam.MELT_TICKS);
            helper.assertTrue(done(player, focus), "test setup broken: melting ice did not earn Burning Focus");
            helper.assertTrue(!done(player, detonation), "melting ice earned Remote Detonation");

            BlockPos tnt = new BlockPos(3, 2, 2);
            helper.setBlock(tnt.below(), Blocks.STONE);
            helper.setBlock(tnt, Blocks.TNT);
            beam(helper, player, lens, tnt, LaserBeam.IGNITE_TICKS);
            helper.assertTrue(helper.getBlockState(tnt).isAir(), "test setup broken: the beam did not prime the TNT");
            helper.assertTrue(done(player, detonation), "priming TNT with the lens did not earn Remote Detonation");
            helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(tnt)).inflate(1.0)).forEach(PrimedTnt::discard);
        } finally {
            rules.set(GameRules.TNT_EXPLODES, tntExplodes, helper.getLevel().getServer());
        }
        TestCleanup.succeed(helper);
    }

    // -------------------------------------------------------------------------------------

    private static void beam(GameTestHelper helper, ServerPlayer player, ItemStack lens, BlockPos relative, int ticks) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false);
        for (int i = 0; i < ticks; i++) {
            LaserBeam.beamAt(player, lens, hit);
        }
    }

    /** One player tick at the given tick count ({@code tickCount} is counted by the level, not by {@code tick}). */
    private static void tickAt(ServerPlayer player, int tickCount) {
        player.tickCount = tickCount;
        player.tick();
    }

    private static void hurt(GameTestHelper helper, ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
        player.invulnerableTime = 0;
        player.setHealth(player.getMaxHealth());
        player.hurtServer(helper.getLevel(), source, 2.0F);
    }

    /** The loader break hook of Vein Miner ({@code vein}) or Strip Miner, with the arguments the loaders pass. */
    private static void breakHook(GameTestHelper helper, ServerPlayer player, ItemStack tool, BlockPos relative, boolean vein) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos origin = helper.absolutePos(relative);
        if (vein) {
            VeinMinerUsageEvent.handleBeforeBlockBreak(helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
        } else {
            StripMinerUsageEvent.handleBeforeBlockBreak(helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
        }
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);
        return stack;
    }

    private static ItemStack trimmed(Item item, ArmorTrim trim) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.TRIM, trim);
        return stack;
    }

    private static AdvancementHolder require(GameTestHelper helper, ServerAdvancementManager manager, String path) {
        AdvancementHolder holder = manager.get(Identifier.fromNamespaceAndPath(MOD_ID, path));
        helper.assertTrue(holder != null, MOD_ID + ":" + path + " is not loaded");
        return holder;
    }

    private static boolean done(ServerPlayer player, AdvancementHolder holder) {
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void give(ServerPlayer player, ItemStack stack, int slot) {
        player.getInventory().setItem(9 + slot, stack);
        player.inventoryMenu.broadcastChanges();
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
