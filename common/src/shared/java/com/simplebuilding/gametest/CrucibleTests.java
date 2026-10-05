package com.simplebuilding.gametest;

import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.fluid.ModBucketItem;
import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.fluid.SoulLava;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crucible P5 in SimpleBuilding (docs/ai/PLAN-CRUCIBLE-P5-2026-10-05.md): the Enderite tiers, the sledgehammer ways,
 * soul lava (flow, unreplaceable, water side, burning, Seelenbrand, heat), the buckets, the reinforced cauldron and the
 * quartz crush. On lines without {@link McVersion#CRUCIBLE} every test passes at once.
 */
public final class CrucibleTests {
    private CrucibleTests() {
    }

    private static Block lib(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplelib", path));
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack main, ItemStack off) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        return player;
    }

    private static UseOnContext click(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** Enderite crucible: 27 slots and double stacks; Enderite barrel double stacks too (owner F4/F10, 59). */
    public static void enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block crucible = CrucibleCompat.enderiteCrucible();
        helper.assertTrue(crucible != null && CrucibleCompat.crucibleSlots(crucible) == 27, "enderite crucible needs 27 slots");
        helper.assertTrue(CrucibleCompat.stackMultiplier(crucible) == 2, "enderite crucible holds double stacks");
        helper.assertTrue(CrucibleCompat.stackMultiplier(CrucibleCompat.enderiteBarrel()) == 2, "enderite barrel holds double stacks");
        helper.setBlock(new BlockPos(1, 1, 1), crucible);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1))) != null, "enderite crucible has a block entity");
        helper.assertTrue(!CrucibleCompat.axeWaysEnabled(), "SimpleBuilding switches the axe ways off (principle 5a)");
        helper.succeed();
    }

    /** Sledgehammer on an iron block: 4 weighted plates, then 2 iron rods; 2 durability per strike. */
    public static void sledgehammerBuildsTheIronCrucible(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.IRON_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 4));
        for (int strike = 0; strike < 6; strike++) {
            if (strike == 4) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.IRON_ROD, 2));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
            helper.assertTrue(CrucibleCompat.hammerUse(click(helper, player, pos)) != null, "strike " + (strike + 1) + " not taken");
        }
        helper.assertBlockPresent(lib("iron_crucible"), pos);
        helper.assertTrue(hammer.getDamageValue() == 6 * CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE, "hammer damage " + hammer.getDamageValue());
        helper.assertTrue(player.getOffhandItem().isEmpty(), "both iron rods used");
        helper.succeed();
    }

    /** Crucible and barrel upgrades cost twice a furnace upgrade; the chain ends at Enderite (owner F9-F11, 56, 59). */
    public static void sledgehammerUpgradesCostDouble(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        var iron = SledgehammerUpgrades.upgradeOf(lib("iron_crucible"));
        var netherite = SledgehammerUpgrades.upgradeOf(lib("netherite_crucible"));
        var barrel = SledgehammerUpgrades.upgradeOf(lib("reinforced_barrel"));
        var cauldron = SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON);
        helper.assertTrue(iron != null && iron.to() == lib("reinforced_crucible") && iron.nugget() == ModItems.CRACKED_DIAMOND
                && iron.materialCost() == 2 && iron.durationFactor() == 2, "iron -> reinforced: 2 cracked diamonds, double strikes");
        helper.assertTrue(netherite != null && netherite.to() == CrucibleCompat.enderiteCrucible() && netherite.nugget() == ModItems.ENDERITE_NUGGET
                && netherite.minHammerRank() == SledgehammerUpgrades.RANK_NETHERITE, "netherite -> enderite with enderite nuggets, netherite hammer");
        helper.assertTrue(barrel != null && barrel.to() == CrucibleCompat.enderiteBarrel() && barrel.materialCost() == 2, "reinforced -> enderite barrel");
        helper.assertTrue(cauldron != null && cauldron.to() == CrucibleCompat.reinforcedCauldron() && cauldron.materialCost() == 4,
                "cauldron -> reinforced cauldron with 4 cracked diamonds");
        // The upgrade keeps the contents (owner 11).
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, lib("netherite_crucible"));
        BlockPos abs = helper.absolutePos(pos);
        var be = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        be.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        CrucibleCompat.upgradeInPlace(helper.getLevel(), abs, CrucibleCompat.enderiteCrucible());
        helper.assertBlockPresent(CrucibleCompat.enderiteCrucible(), pos);
        var fresh = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        helper.assertTrue(fresh.getContainerSize() == 27 && fresh.getItem(0).is(Items.RAW_IRON) && fresh.getItem(0).getCount() == 5,
                "upgrade kept the contents");
        helper.succeed();
    }

    /** Overworld reach 2: the third block stays dry, the second gets soul lava (owner 25). */
    public static void soulLavaFlowsTwoBlocksInTheOverworld(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        floor(helper);
        helper.setBlock(new BlockPos(1, 1, 4), ModFluids.SOUL_LAVA_BLOCK);
        helper.runAfterDelay(250, () -> {
            var level = helper.getLevel();
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 4))).getType().isSame(ModFluids.SOUL_LAVA), "first block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(3, 1, 4))).getType().isSame(ModFluids.SOUL_LAVA), "second block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(4, 1, 4))).isEmpty(), "third block dry");
            helper.succeed();
        });
    }

    /** Not replaceable by other fluids or survival placement; only scooping the source removes it (owner 26). */
    public static void soulLavaIsNotReplaceable(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockState state = ModFluids.SOUL_LAVA_BLOCK.defaultBlockState();
        helper.assertTrue(!state.canBeReplaced(), "soul lava must not be replaceable");
        helper.assertTrue(!state.canBeReplaced(Fluids.WATER) && !state.canBeReplaced(Fluids.LAVA), "fluids must not replace soul lava");
        helper.assertTrue(state.getFluidState().isSource() && state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA), "soul lava swims like lava");
        helper.succeed();
    }

    /** Water touching soul lava turns: source -> quartz block, flowing -> blackstone; the soul lava stays (owner 26/54). */
    public static void waterTouchingSoulLavaTurnsToQuartzOrBlackstone(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        floor(helper);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) for (int y = 1; y < 3; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
        BlockPos lava = new BlockPos(3, 1, 3);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(lava, ModFluids.SOUL_LAVA_BLOCK);
        helper.assertBlockPresent(Blocks.QUARTZ_BLOCK, new BlockPos(4, 1, 3));
        helper.assertBlockPresent(Blocks.BLACKSTONE, new BlockPos(2, 1, 3));
        helper.assertBlockPresent(ModFluids.SOUL_LAVA_BLOCK, lava);
        helper.succeed();
    }

    /** Touch: burns twice as long as lava and gives Seelenbrand for a minute (owner 25/28). */
    public static void touchingSoulLavaBurnsLongerAndGivesSoulBurn(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        SoulLava.touch(helper.getLevel(), pig);
        helper.assertTrue(pig.getRemainingFireTicks() >= 20 * 20, "burns longer than lava: " + pig.getRemainingFireTicks());
        MobEffectInstance burn = pig.getEffect(ModEffects.SOUL_BURN);
        helper.assertTrue(burn != null && burn.getDuration() >= SoulLava.SOUL_BURN_TICKS - 1, "seelenbrand for a minute");
        helper.succeed();
    }

    /** Fire resistance blocks the Seelenbrand damage, not the effect; without it the damage comes (owner 55). */
    public static void fireResistanceOnlyBlocksSoulBurnDamage(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        Pig safe = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        Pig hurt = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(5, 1, 5));
        safe.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400));
        for (Pig pig : new Pig[] {safe, hurt}) pig.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, SoulLava.SOUL_BURN_TICKS));
        float before = safe.getHealth();
        for (int i = 0; i < 40; i++) {
            McVersion.resetInvulnerableTime(safe);
            McVersion.resetInvulnerableTime(hurt);
            ModEffects.SOUL_BURN.value().applyEffectTick(level, safe, 0);
            ModEffects.SOUL_BURN.value().applyEffectTick(level, hurt, 0);
        }
        helper.assertTrue(safe.getHealth() == before && safe.hasEffect(ModEffects.SOUL_BURN), "fire resistance: no damage, effect stays");
        helper.assertTrue(hurt.getHealth() < hurt.getMaxHealth(), "without fire resistance the soul burn hurts");
        helper.succeed();
    }

    /** Heat: soul lava source extreme, flowing high; the reinforced cauldron with soul lava extreme (owner F30). */
    public static void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), ModFluids.SOUL_LAVA_BLOCK);
        helper.assertTrue("extreme".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(1, 2, 1)))), "source: extreme");
        helper.setBlock(new BlockPos(4, 1, 1), ModFluids.FLOWING_SOUL_LAVA.getFlowing(6, false).createLegacyBlock());
        helper.assertTrue("high".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(4, 2, 1)))), "flowing: high");
        helper.setBlock(new BlockPos(6, 1, 1), CrucibleCompat.reinforcedCauldron("extreme"));
        helper.assertTrue("extreme".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(6, 2, 1)))), "cauldron: extreme");
        TagKey<Item> extreme = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "needs_extreme_heat"));
        helper.assertTrue(new ItemStack(ModItems.LAYERED_RAW_ENDERITE).is(extreme) && new ItemStack(ModItems.CRACKED_DIAMOND).is(extreme),
                "layered raw enderite and cracked diamond need extreme heat");
        helper.succeed();
    }

    /** Copper bucket: no soul lava, oxidizes on pouring, no water source, breaks on lava; axe/honeycomb (owner 32/33). */
    public static void copperBucketRules(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.COPPER, ModFluids.SOUL_LAVA) == null, "copper bucket refuses soul lava");
        ModBucketItem water = (ModBucketItem) ModFluids.COPPER_WATER_BUCKET;
        ItemStack after = water.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(water));
        helper.assertTrue(after.is(ModFluids.COPPER_BUCKET) && ModBucketItem.oxidation(after) == 1, "pouring oxidizes one stage");
        helper.assertTrue(!level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).isSource(), "copper water is never a source");
        ModBucketItem lava = (ModBucketItem) ModFluids.COPPER_LAVA_BUCKET;
        helper.assertTrue(lava.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(lava)).isEmpty(), "copper bucket breaks on lava");
        helper.assertTrue(ModBucketItem.scrape(after) && ModBucketItem.oxidation(after) == 0, "axe scrapes one stage");
        after.set(com.simplebuilding.component.ModDataComponentTypes.WAXED, true);
        ModBucketItem.oxidize(after);
        helper.assertTrue(ModBucketItem.oxidation(after) == 0, "waxed copper does not oxidize");
        helper.succeed();
    }

    /** Iron bucket breaks when pouring soul lava; the Enderite bucket never breaks (owner 34/36). */
    public static void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        floor(helper);
        ModBucketItem iron = (ModBucketItem) ModFluids.SOUL_LAVA_BUCKET;
        helper.assertTrue(iron.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(iron)).isEmpty(), "iron bucket breaks");
        helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).getType() == ModFluids.SOUL_LAVA, "soul lava placed");
        ModBucketItem enderite = (ModBucketItem) ModFluids.ENDERITE_SOUL_LAVA_BUCKET;
        helper.assertTrue(enderite.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(enderite)).is(ModFluids.ENDERITE_BUCKET),
                "enderite bucket comes back");
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.ENDERITE, ModFluids.SOUL_LAVA) == ModFluids.ENDERITE_SOUL_LAVA_BUCKET,
                "enderite bucket takes soul lava");
        helper.assertTrue(Items.BUCKET.getDefaultInstance().is(Items.BUCKET)
                && ((LiquidBlock) ModFluids.SOUL_LAVA_BLOCK).pickupBlock(null, level, helper.absolutePos(new BlockPos(2, 1, 2)),
                        level.getBlockState(helper.absolutePos(new BlockPos(2, 1, 2)))).is(ModFluids.SOUL_LAVA_BUCKET),
                "the vanilla bucket scoops soul lava into the soul lava bucket");
        helper.succeed();
    }

    /** The reinforced cauldron takes soul lava from the Enderite bucket and gives it back (owner 30/56). */
    public static void reinforcedCauldronHoldsSoulLava(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, CrucibleCompat.reinforcedCauldron("empty"));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(ModFluids.ENDERITE_SOUL_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue("extreme".equals(CrucibleCompat.cauldronContent(level.getBlockState(abs))), "cauldron holds soul lava");
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_BUCKET), "enderite bucket comes back empty");
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_SOUL_LAVA_BUCKET), "scooped back");
        helper.assertTrue("empty".equals(CrucibleCompat.cauldronContent(level.getBlockState(abs))), "cauldron empty again");
        helper.succeed();
    }

    /** Sledgehammer on a quartz block: 4 quartz (owner 54). */
    public static void sledgehammerCrushesQuartzBlockIntoFourQuartz(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.QUARTZ_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, ItemStack.EMPTY);
        SledgehammerItem.crushQuartzBlock(helper.getLevel(), helper.absolutePos(pos), player, hammer);
        helper.assertBlockPresent(Blocks.AIR, pos);
        int quartz = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2))
                .stream().filter(e -> e.getItem().is(Items.QUARTZ)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(quartz == SledgehammerItem.QUARTZ_BLOCK_QUARTZ, "quartz dropped: " + quartz);
        helper.succeed();
    }

    /** Vanilla cauldron: the copper bucket pours lava and breaks, the Enderite bucket takes it back (owner 33/36). */
    public static void vanillaCauldronTakesCopperAndEnderiteBuckets(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.CAULDRON);
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(ModFluids.COPPER_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.LAVA_CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "copper bucket broke on lava");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModFluids.ENDERITE_BUCKET));
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_LAVA_BUCKET), "enderite bucket took the lava");
        helper.succeed();
    }
}
