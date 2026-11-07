package com.simplecrucibles.test;

import com.google.gson.JsonObject;
import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplecrucibles.compat.CrucibleInfo;
import com.simplecrucibles.compat.CrucibleRecipeCatalog;
import com.simplecrucibles.config.CrucibleConfig;
import com.simplecrucibles.effect.CrucibleEffects;
import com.simplecrucibles.effect.SoulBurnEffect;
import com.simplecrucibles.fluid.CrucibleFluids;
import com.simplecrucibles.fluid.SoulLava;
import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplecrucibles.registry.CrucibleComponents;
import com.simplelib.bucket.Buckets;
import com.simplelib.bucket.ModBucketItem;
import static com.simplelib.test.PartnerChecks.partnerMissing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests of soul lava, the buckets and the JEI/Jade data of Simple Crucibles (split stage 5b: SimpleBuilding's
 * CrucibleTests and WorldPolishTests had them). Registered into {@link CrucibleTests#ALL} under the same ids for every
 * loader ({@code module_game_test_<name>}); the bodies are unchanged apart from the module's own classes.
 */
final class BucketTests {
    private BucketTests() {
    }

    static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("soul_lava_flows_two_blocks_in_the_overworld", BucketTests::soulLavaFlowsTwoBlocksInTheOverworld);
        all.put("soul_lava_is_not_replaceable", BucketTests::soulLavaIsNotReplaceable);
        all.put("water_touching_soul_lava_turns_to_quartz_or_blackstone", BucketTests::waterTouchingSoulLavaTurnsToQuartzOrBlackstone);
        all.put("touching_soul_lava_burns_longer_and_gives_soul_burn", BucketTests::touchingSoulLavaBurnsLongerAndGivesSoulBurn);
        all.put("fire_resistance_only_blocks_soul_burn_damage", BucketTests::fireResistanceOnlyBlocksSoulBurnDamage);
        all.put("soul_burn_bites_twice_as_often_on_cold_ground", BucketTests::soulBurnBitesTwiceAsOftenOnColdGround);
        all.put("copper_bucket_rules", BucketTests::copperBucketRules);
        all.put("copper_bucket_fully_oxidized_scoops_nothing", BucketTests::copperBucketFullyOxidizedScoopsNothing);
        all.put("mod_buckets_stay_in_the_hand_when_pouring_and_scooping", BucketTests::modBucketsStayInTheHandWhenPouringAndScooping);
        all.put("enderite_bucket_two_buckets_in_cauldrons_and_dispensers", BucketTests::enderiteBucketTwoBucketsInCauldronsAndDispensers);
        all.put("copper_bucket_oxidizes_only_in_hotbar_or_offhand", BucketTests::copperBucketOxidizesOnlyInHotbarOrOffhand);
        all.put("copper_bucket_waxed_water_weathers_at_half_pace", BucketTests::copperBucketWaxedWaterWeathersAtHalfPace);
        all.put("copper_bucket_placed_weathers_and_frame_care", BucketTests::copperBucketPlacedWeathersAndFrameCare);
        all.put("iron_bucket_breaks_on_soul_lava_enderite_never", BucketTests::ironBucketBreaksOnSoulLavaEnderiteNever);
        all.put("reinforced_cauldron_holds_soul_lava", BucketTests::reinforcedCauldronHoldsSoulLava);
        all.put("vanilla_cauldron_takes_copper_and_enderite_buckets", BucketTests::vanillaCauldronTakesCopperAndEnderiteBuckets);
        all.put("jade_reports_heat_slots_and_shortest_remaining_time", BucketTests::jadeReportsHeatSlotsAndShortestRemainingTime);
        all.put("recipe_heat_and_catalog_match_cooking_rules", BucketTests::recipeHeatAndCatalogMatchCookingRules);
        all.put("soul_lava_heats_extreme_flowing_high", BucketTests::soulLavaHeatsExtremeFlowingHigh);
        all.put("buckets_draw_no_bar_and_the_enderite_bucket_shows_only_its_name", BucketTests::bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName);
        all.put("registered_soul_lava_and_buckets", BucketTests::registeredSoulLavaAndBuckets);
        all.put("migration_of_buckets_and_soul_lava", BucketTests::migrationOfBucketsAndSoulLava);
        all.put("config_soul_lava_bounds_and_takeover", BucketTests::configSoulLavaBoundsAndTakeover);
        all.put("jade_reinforced_cauldron_lines", BucketTests::jadeReinforcedCauldronLines);
        all.put("jei_info_pages_cover_the_filled_buckets", BucketTests::jeiInfoPagesCoverTheFilledBuckets);
        maxTicks.put("soul_lava_flows_two_blocks_in_the_overworld", 300);
        maxTicks.put("enderite_bucket_two_buckets_in_cauldrons_and_dispensers", 100);
    }

    // ------------------------------------------------------------ helpers

    private static ServerPlayer player(GameTestHelper helper, ItemStack main, ItemStack off) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        return player;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static ItemStack stack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Damage again at once (the invulnerability window would swallow the next hit). */
    private static void resetInvulnerable(Entity entity) {
        entity.setInvulnerableTime(0);
        if (entity instanceof LivingEntity living) living.damageCooldownTime = 0;
    }

    // ------------------------------------------------------------ tests (moved from SimpleBuilding)

    /** Overworld reach 2: the third block stays dry, the second gets soul lava (owner 25). */
    static void soulLavaFlowsTwoBlocksInTheOverworld(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(1, 1, 4), CrucibleFluids.SOUL_LAVA_BLOCK);
        helper.runAfterDelay(250, () -> {
            var level = helper.getLevel();
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 4))).getType().isSame(CrucibleFluids.SOUL_LAVA), "first block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(3, 1, 4))).getType().isSame(CrucibleFluids.SOUL_LAVA), "second block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(4, 1, 4))).isEmpty(), "third block dry");
            helper.succeed();
        });
    }

    /** Not replaceable by other fluids or survival placement; only scooping the source removes it (owner 26). */
    static void soulLavaIsNotReplaceable(GameTestHelper helper) {
        BlockState state = CrucibleFluids.SOUL_LAVA_BLOCK.defaultBlockState();
        helper.assertTrue(!state.canBeReplaced(), "soul lava must not be replaceable");
        helper.assertTrue(!state.canBeReplaced(Fluids.WATER) && !state.canBeReplaced(Fluids.LAVA), "fluids must not replace soul lava");
        helper.assertTrue(state.getFluidState().isSource() && state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA), "soul lava swims like lava");
        helper.succeed();
    }

    /** Water touching soul lava turns: source -> quartz block, flowing -> blackstone; the soul lava stays (owner 26/54). */
    static void waterTouchingSoulLavaTurnsToQuartzOrBlackstone(GameTestHelper helper) {
        floor(helper);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) for (int y = 1; y < 3; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
        BlockPos lava = new BlockPos(3, 1, 3);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(lava, CrucibleFluids.SOUL_LAVA_BLOCK);
        helper.assertBlockPresent(Blocks.QUARTZ_BLOCK, new BlockPos(4, 1, 3));
        helper.assertBlockPresent(Blocks.BLACKSTONE, new BlockPos(2, 1, 3));
        helper.assertBlockPresent(CrucibleFluids.SOUL_LAVA_BLOCK, lava);
        helper.succeed();
    }

    /** Touch: burns twice as long as lava and gives Seelenbrand for two minutes (owner 25/28). */
    static void touchingSoulLavaBurnsLongerAndGivesSoulBurn(GameTestHelper helper) {
        Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        SoulLava.touch(helper.getLevel(), pig);
        helper.assertTrue(pig.getRemainingFireTicks() >= 20 * 20, "burns longer than lava: " + pig.getRemainingFireTicks());
        MobEffectInstance burn = pig.getEffect(CrucibleEffects.SOUL_BURN);
        helper.assertTrue(burn != null && burn.getDuration() >= SoulLava.soulBurnTicks() - 1, "seelenbrand for two minutes");
        helper.succeed();
    }

    /** Fire resistance blocks the Seelenbrand damage, not the effect; without it the damage comes (owner 55). */
    static void fireResistanceOnlyBlocksSoulBurnDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pig safe = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        Pig hurt = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(5, 1, 5));
        safe.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400));
        for (Pig pig : new Pig[] {safe, hurt}) pig.addEffect(new MobEffectInstance(CrucibleEffects.SOUL_BURN, SoulLava.soulBurnTicks()));
        float before = safe.getHealth();
        for (int i = 0; i < 40; i++) {
            resetInvulnerable(safe);
            resetInvulnerable(hurt);
            CrucibleEffects.SOUL_BURN.value().applyEffectTick(level, safe, 0);
            CrucibleEffects.SOUL_BURN.value().applyEffectTick(level, hurt, 0);
        }
        helper.assertTrue(safe.getHealth() == before && safe.hasEffect(CrucibleEffects.SOUL_BURN), "fire resistance: no damage, effect stays");
        helper.assertTrue(hurt.getHealth() < hurt.getMaxHealth(), "without fire resistance the soul burn hurts");
        helper.succeed();
    }

    /**
     * Seelenbrand on cold ground (owner round 11 P1): water, ice or snow at the feet or under them
     * halves the configured interval, so the roll comes twice as often; warm ground keeps it. The
     * gate reads the remaining duration, so 90 ticks left are due at a 30 tick cold interval and
     * not at the 60 tick warm one.
     */
    static void soulBurnBitesTwiceAsOftenOnColdGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        BlockPos ice = new BlockPos(1, 1, 1);
        BlockPos snow = new BlockPos(3, 1, 1);
        BlockPos water = new BlockPos(5, 1, 1);
        BlockPos stone = new BlockPos(7, 1, 1);
        helper.setBlock(ice, Blocks.ICE);
        helper.setBlock(snow, Blocks.SNOW_BLOCK);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(stone, Blocks.STONE);
        helper.assertTrue(level.getFluidState(helper.absolutePos(water)).is(FluidTags.WATER), "the test water sits there");
        helper.assertTrue(SoulBurnEffect.coldGround(level, helper.absolutePos(ice.above())), "ice under the feet is cold");
        helper.assertTrue(SoulBurnEffect.coldGround(level, helper.absolutePos(snow.above())), "snow under the feet is cold");
        helper.assertTrue(SoulBurnEffect.coldGround(level, helper.absolutePos(water)), "water at the feet is cold");
        helper.assertFalse(SoulBurnEffect.coldGround(level, helper.absolutePos(stone.above())), "stone is not cold");
        int base = SoulLava.soulBurnIntervalTicks();
        helper.assertTrue(SoulBurnEffect.intervalTicks(level, helper.absolutePos(ice.above())) == base / 2,
                "cold ground halves the " + base + " tick interval");
        helper.assertTrue(SoulBurnEffect.intervalTicks(level, helper.absolutePos(stone.above())) == base,
                "warm ground keeps the interval");

        double previous = CrucibleConfig.soulLava.soulBurnChance;
        try {
            CrucibleConfig.soulLava.soulBurnChance = 1.0;
            Pig chilled = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 2, 1));
            Pig dry = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(7, 2, 2));
            for (Pig pig : new Pig[] {chilled, dry}) pig.addEffect(new MobEffectInstance(CrucibleEffects.SOUL_BURN, 90));
            float chilledBefore = chilled.getHealth();
            float dryBefore = dry.getHealth();
            resetInvulnerable(chilled);
            resetInvulnerable(dry);
            CrucibleEffects.SOUL_BURN.value().applyEffectTick(level, chilled, 0);
            CrucibleEffects.SOUL_BURN.value().applyEffectTick(level, dry, 0);
            helper.assertTrue(chilled.getHealth() < chilledBefore, "cold ground bites at half the interval");
            helper.assertTrue(dry.getHealth() == dryBefore, "warm ground: 90 ticks left are not due yet");
        } finally {
            CrucibleConfig.soulLava.soulBurnChance = previous;
        }
        helper.succeed();
    }

    /** Copper bucket: no soul lava, pouring does not oxidize (N35), no water source, breaks on lava; axe/honeycomb (owner 32/33). */
    static void copperBucketRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.COPPER, CrucibleFluids.SOUL_LAVA) == null, "copper bucket refuses soul lava");
        ModBucketItem water = (ModBucketItem) CrucibleFluids.COPPER_WATER_BUCKET;
        ItemStack after = water.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(water));
        helper.assertTrue(after.is(CrucibleFluids.COPPER_BUCKET) && ModBucketItem.oxidation(after) == 0, "pouring no longer oxidizes (N35)");
        helper.assertTrue(!level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).isSource(), "copper water is never a source");
        ModBucketItem lava = (ModBucketItem) CrucibleFluids.COPPER_LAVA_BUCKET;
        helper.assertTrue(lava.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(lava)).isEmpty(), "copper bucket breaks on lava");
        after.set(CrucibleComponents.OXIDATION, 1);
        helper.assertTrue(ModBucketItem.scrape(after) && ModBucketItem.oxidation(after) == 0, "axe scrapes one stage");
        after.set(CrucibleComponents.WAXED, true);
        ModBucketItem.oxidize(after);
        helper.assertTrue(ModBucketItem.oxidation(after) == 0, "waxed copper does not oxidize");
        helper.succeed();
    }

    /**
     * Fully oxidized copper bucket (stage 3, Nachtrag 11): scoops nothing - not from a full water cauldron, not as
     * the scoop rule - until the axe scrapes it back to stage 2; a filled one still pours.
     */
    static void copperBucketFullyOxidizedScoopsNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ItemStack old = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        old.set(CrucibleComponents.OXIDATION, 3);
        helper.assertFalse(ModBucketItem.canScoop(old), "a fully oxidized copper bucket may scoop");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, old, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.WATER_CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.COPPER_BUCKET), "the oxidized bucket took water from the cauldron");
        helper.assertTrue(ModBucketItem.scrape(old) && ModBucketItem.canScoop(old), "scraped to stage 2 it scoops again");
        ItemStack full = new ItemStack(CrucibleFluids.COPPER_WATER_BUCKET);
        full.set(CrucibleComponents.OXIDATION, 3);
        ItemStack after = ((ModBucketItem) CrucibleFluids.COPPER_WATER_BUCKET).pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), full);
        helper.assertTrue(after.is(CrucibleFluids.COPPER_BUCKET), "a full stage-3 bucket still pours");
        helper.succeed();
    }



    /**
     * Owner N15 (bug): pouring or scooping with a mod bucket through the real item use kept nothing in the hand,
     * because Vanilla's BucketItem.use consumes the held stack before the wear step. Every kind gives back its own
     * bucket; ceramic one wear step on, creative unchanged.
     */
    static void modBucketsStayInTheHandWhenPouringAndScooping(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        Item[][] kinds = {
                {CrucibleFluids.COPPER_BUCKET, CrucibleFluids.COPPER_WATER_BUCKET},
                {Buckets.ENDERITE_BUCKET, Buckets.ENDERITE_WATER_BUCKET},
                {Buckets.CERAMIC_BUCKET, Buckets.CERAMIC_WATER_BUCKET}};
        int x = 1;
        for (Item[] kind : kinds) {
            if (kind[1] == null) continue; // WH26-7: Enderite and ceramic only with Simple Tools
            ModBucketItem.Kind expected = ((ModBucketItem) kind[1]).kind();
            net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2.0, 3.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
            BlockPos water = helper.absolutePos(new BlockPos(x, 1, 3));
            ItemStack full = new ItemStack(kind[1]);
            int usesBefore = ModBucketItem.ceramicUses(full);
            // Owner N21: a half Enderite bucket pours only on sneak + use (plain use scoops).
            player.setShiftKeyDown(expected == ModBucketItem.Kind.ENDERITE);
            ItemStack poured = useBucket(level, player, full);
            player.setShiftKeyDown(false);
            // Copper pours no source (owner 32), the others do.
            helper.assertTrue(level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER)
                            && level.getFluidState(water).isSource() == (expected != ModBucketItem.Kind.COPPER),
                    expected + ": pouring placed the wrong water; hand " + poured + ", block " + level.getBlockState(water)
                            + ", below " + level.getBlockState(water.below()) + ", eye " + player.getEyePosition() + ", xRot " + player.getXRot());
            helper.assertTrue(poured.getItem() instanceof ModBucketItem empty && empty.kind() == expected && !poured.is(kind[1]) && !poured.is(ModBucketItem.filled(expected, Fluids.WATER)),
                    expected + ": after pouring the hand holds " + poured + " instead of its empty bucket");
            if (expected == ModBucketItem.Kind.CERAMIC) {
                helper.assertTrue(ModBucketItem.ceramicUses(poured) == usesBefore + 1, "ceramic: one wear step per pour, uses " + ModBucketItem.ceramicUses(poured));
            }
            if (expected == ModBucketItem.Kind.COPPER) level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
            ItemStack scooped = useBucket(level, player, poured);
            Item expectedFilled = expected == ModBucketItem.Kind.CERAMIC
                    ? Buckets.CERAMIC_WATER[1] : ModBucketItem.filled(expected, Fluids.WATER);
            helper.assertTrue(scooped.getItem() instanceof ModBucketItem filled && filled.kind() == expected && scooped.is(expectedFilled),
                    expected + ": after scooping the hand holds " + scooped + " instead of its water bucket");
            helper.assertTrue(level.getFluidState(water).isEmpty(), expected + ": scooping left the source");
            x += 3;
        }
        net.minecraft.world.entity.player.Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true; // the mock's game type alone does not grant infinite materials everywhere
        if (Buckets.CERAMIC_WATER_BUCKET == null) {
            helper.succeed();
            return;
        }
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2.0, 6.5));
        creative.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
        ItemStack kept = useBucket(level, creative, new ItemStack(Buckets.CERAMIC_WATER_BUCKET));
        helper.assertTrue(kept.is(Buckets.CERAMIC_WATER_BUCKET) && ModBucketItem.ceramicUses(kept) == 0, "creative: the bucket stays as it was, got " + kept);
        helper.succeed();
    }





    /**
     * Owner N21: cauldrons and dispensers follow the two-bucket rule. A full Enderite bucket fills a Vanilla or
     * reinforced cauldron with one bucket and stays half; a half one takes a full cauldron of its fluid and becomes
     * full. A dispenser pours one bucket of a full one and keeps the half one; a half one pours and comes back empty.
     */
    static void enderiteBucketTwoBucketsInCauldronsAndDispensers(GameTestHelper helper) {
        if (partnerMissing(helper, "simpletools", "the Enderite bucket (Simple Tools, WH26-7) on the reinforced cauldron")) return;
        ServerLevel level = helper.getLevel();
        floor(helper);
        BlockPos cauldron = new BlockPos(2, 1, 2);
        helper.setBlock(cauldron, Blocks.CAULDRON);
        BlockPos abs = helper.absolutePos(cauldron);
        ServerPlayer player = player(helper, new ItemStack(Buckets.FULL_ENDERITE_WATER_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(abs).is(Blocks.WATER_CAULDRON) && player.getMainHandItem().is(Buckets.ENDERITE_WATER_BUCKET),
                "full bucket fills the cauldron and stays half: " + player.getMainHandItem());
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(abs).is(Blocks.CAULDRON) && player.getMainHandItem().is(Buckets.FULL_ENDERITE_WATER_BUCKET),
                "half bucket takes the full cauldron back: " + player.getMainHandItem());
        BlockPos reinforced = new BlockPos(4, 1, 2);
        helper.setBlock(reinforced, SimpleCruciblesApi.reinforcedCauldron("empty"));
        BlockPos rAbs = helper.absolutePos(reinforced);
        BlockHitResult rHit = new BlockHitResult(Vec3.atCenterOf(rAbs), Direction.UP, rAbs, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Buckets.FULL_ENDERITE_SOUL_LAVA_BUCKET));
        level.getBlockState(rAbs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, rHit);
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(rAbs)))
                && player.getMainHandItem().is(Buckets.ENDERITE_SOUL_LAVA_BUCKET), "reinforced cauldron: one bucket of soul lava, half stays");
        level.getBlockState(rAbs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, rHit);
        helper.assertTrue("empty".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(rAbs)))
                && player.getMainHandItem().is(Buckets.FULL_ENDERITE_SOUL_LAVA_BUCKET), "reinforced cauldron: half takes it back to full");
        BlockPos dispenser = new BlockPos(1, 1, 6);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST));
        BlockPos front = helper.absolutePos(dispenser.east());
        var be = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(helper.absolutePos(dispenser));
        be.setItem(0, new ItemStack(Buckets.FULL_ENDERITE_LAVA_BUCKET));
        helper.setBlock(dispenser.above(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(level.getFluidState(front).is(net.minecraft.tags.FluidTags.LAVA) && level.getFluidState(front).isSource(), "dispenser poured lava");
            helper.assertTrue(be.getItem(0).is(Buckets.ENDERITE_LAVA_BUCKET), "dispenser keeps the half bucket: " + be.getItem(0));
            ModBucketItem half = (ModBucketItem) Buckets.ENDERITE_LAVA_BUCKET;
            helper.assertTrue(half.afterPour(new ItemStack(half)).is(Buckets.ENDERITE_BUCKET), "a half one pours its last bucket");
            helper.succeed();
        });
    }



    /** N35: copper buckets oxidize only in the hotbar or off hand, never in the rest of the inventory (simulated ticks). */
    static void copperBucketOxidizesOnlyInHotbarOrOffhand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hotbar = new ItemStack(CrucibleFluids.COPPER_BUCKET), inventory = new ItemStack(CrucibleFluids.COPPER_BUCKET), off = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        player.getInventory().setItem(3, hotbar);
        player.getInventory().setItem(20, inventory);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        helper.assertTrue(ModBucketItem.oxidationInterval(hotbar) == ModBucketItem.OXIDATION_TICKS, "unwaxed pace");
        for (int t = 0; t < 600_000; t++) {
            for (ItemStack stack : new ItemStack[] {hotbar, inventory, off}) stack.getItem().inventoryTick(stack, level, player, null);
        }
        helper.assertTrue(ModBucketItem.oxidation(hotbar) >= 1, "a hotbar bucket must weather, stage " + ModBucketItem.oxidation(hotbar));
        helper.assertTrue(ModBucketItem.oxidation(off) >= 1, "an off-hand bucket must weather, stage " + ModBucketItem.oxidation(off));
        helper.assertTrue(ModBucketItem.oxidation(inventory) == 0, "a bucket in the inventory must not weather");
        helper.succeed();
    }

    /**
     * Nachtrag 60: a waxed copper bucket never weathers; filling it wears the wax (half pace until waxed again),
     * honeycomb renews the wax, an axe scrapes it off.
     */
    static void copperBucketWaxedWaterWeathersAtHalfPace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack empty = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        empty.set(CrucibleComponents.WAXED, true);
        ItemStack plain = new ItemStack(CrucibleFluids.COPPER_WATER_BUCKET);
        helper.assertTrue(ModBucketItem.oxidationInterval(empty) == 0, "waxed empty must not react");
        ItemStack water = ModBucketItem.fill(empty, CrucibleFluids.COPPER_WATER_BUCKET);
        ItemStack lava = ModBucketItem.fill(empty, CrucibleFluids.COPPER_LAVA_BUCKET);
        helper.assertTrue(ModBucketItem.waxed(water) && ModBucketItem.waxWorn(water) && ModBucketItem.waxWorn(lava), "filling wears the wax");
        helper.assertTrue(ModBucketItem.oxidationInterval(water) == 2 * ModBucketItem.oxidationInterval(plain), "worn wax is half pace");
        ItemStack kept = ModBucketItem.fill(new ItemStack(CrucibleFluids.COPPER_BUCKET), CrucibleFluids.COPPER_WATER_BUCKET);
        helper.assertTrue(!ModBucketItem.waxWorn(kept), "an unwaxed bucket has no wax to wear");
        ItemStack emptied = ((ModBucketItem) water.getItem()).afterPour(water);
        helper.assertTrue(ModBucketItem.waxWorn(emptied), "emptying keeps the worn wax");
        player.getInventory().setItem(0, empty);
        player.getInventory().setItem(1, water);
        for (int t = 0; t < 1_500_000; t++) {
            for (ItemStack stack : new ItemStack[] {empty, water}) stack.getItem().inventoryTick(stack, level, player, null);
        }
        helper.assertTrue(ModBucketItem.oxidation(empty) == 0, "waxed empty weathered");
        helper.assertTrue(ModBucketItem.oxidation(water) >= 1 && ModBucketItem.waxed(water), "worn wax must weather slowly and stay waxed");
        // honeycomb renews, then it holds still again
        ServerPlayer actor = player(helper, emptied, new ItemStack(Items.HONEYCOMB));
        helper.assertTrue(ModBucketItem.care(level, actor, emptied, actor.getOffhandItem(), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && !ModBucketItem.waxWorn(emptied) && ModBucketItem.oxidationInterval(emptied) == 0, "honeycomb renews the wax");
        helper.assertTrue(ModBucketItem.care(level, actor, emptied, new ItemStack(Items.HONEYCOMB), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
                == InteractionResult.FAIL, "intact wax takes no second honeycomb");
        helper.succeed();
    }

    /** Nachtrag 60: a copper bucket lying in the world or in an item frame weathers; honeycomb/axe work on the frame; containers do not weather it. */
    static void copperBucketPlacedWeathersAndFrameCare(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level,
                helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)).x, helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)).y, helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)).z,
                new ItemStack(CrucibleFluids.COPPER_BUCKET));
        drop.setNoGravity(true);
        level.addFreshEntity(drop);
        ItemStack waxedStack = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        waxedStack.set(CrucibleComponents.WAXED, true);
        int stage = 0;
        for (int t = 0; t < 600_000 && stage == 0; t++) {
            ItemStack w = ModBucketItem.weatherPlaced(drop.getItem(), level.getRandom());
            if (w != null) { drop.setItem(w); stage = ModBucketItem.oxidation(w); }
        }
        helper.assertTrue(stage == 1 && ModBucketItem.oxidation(drop.getItem()) == 1, "a dropped copper bucket must weather");
        helper.assertTrue(ModBucketItem.weatherPlaced(waxedStack, level.getRandom()) == null, "a waxed placed bucket does not weather");
        drop.discard();
        ItemStack framed = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        ServerPlayer actor = player(helper, ItemStack.EMPTY, new ItemStack(Items.HONEYCOMB));
        helper.assertTrue(ModBucketItem.care(level, actor, framed, actor.getOffhandItem(), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && ModBucketItem.waxed(framed), "honeycomb waxes the bucket");
        helper.assertTrue(ModBucketItem.care(level, actor, framed, new ItemStack(Items.IRON_AXE), net.minecraft.world.entity.EquipmentSlot.MAINHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && !ModBucketItem.waxed(framed), "an axe takes the wax off");
        helper.succeed();
    }





    /** Iron bucket breaks when pouring soul lava; the Enderite bucket never breaks (owner 34/36). */
    static void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ModBucketItem iron = (ModBucketItem) CrucibleFluids.SOUL_LAVA_BUCKET;
        helper.assertTrue(iron.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(iron)).isEmpty(), "iron bucket breaks");
        helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).getType() == CrucibleFluids.SOUL_LAVA, "soul lava placed");
        helper.assertTrue(Items.BUCKET.getDefaultInstance().is(Items.BUCKET)
                && ((LiquidBlock) CrucibleFluids.SOUL_LAVA_BLOCK).pickupBlock(null, level, helper.absolutePos(new BlockPos(2, 1, 2)),
                        level.getBlockState(helper.absolutePos(new BlockPos(2, 1, 2)))).is(CrucibleFluids.SOUL_LAVA_BUCKET),
                "the vanilla bucket scoops soul lava into the soul lava bucket");
        if (partnerMissing(helper, "simpletools", "the Enderite soul lava bucket (Simple Tools, WH26-7)")) return;
        ModBucketItem enderite = (ModBucketItem) Buckets.ENDERITE_SOUL_LAVA_BUCKET;
        helper.assertTrue(enderite.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(enderite)).is(Buckets.ENDERITE_BUCKET),
                "enderite bucket comes back");
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.ENDERITE, CrucibleFluids.SOUL_LAVA) == Buckets.ENDERITE_SOUL_LAVA_BUCKET,
                "enderite bucket takes soul lava");
        helper.succeed();
    }

    /** The reinforced cauldron takes soul lava from the Enderite bucket and gives it back (owner 30/56). */
    static void reinforcedCauldronHoldsSoulLava(GameTestHelper helper) {
        if (partnerMissing(helper, "simpletools", "the Enderite soul lava bucket (Simple Tools, WH26-7) on the reinforced cauldron")) return;
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, SimpleCruciblesApi.reinforcedCauldron("empty"));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(Buckets.ENDERITE_SOUL_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(abs))), "cauldron holds soul lava");
        helper.assertTrue(player.getMainHandItem().is(Buckets.ENDERITE_BUCKET), "enderite bucket comes back empty");
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().is(Buckets.ENDERITE_SOUL_LAVA_BUCKET), "scooped back");
        helper.assertTrue("empty".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(abs))), "cauldron empty again");
        helper.succeed();
    }

    /** Vanilla cauldron: the copper bucket pours lava and breaks, the Enderite bucket takes it back (owner 33/36). */
    static void vanillaCauldronTakesCopperAndEnderiteBuckets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.CAULDRON);
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(CrucibleFluids.COPPER_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.LAVA_CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "copper bucket broke on lava");
        if (partnerMissing(helper, "simpletools", "the Enderite bucket (Simple Tools, WH26-7) on the Vanilla cauldron")) return;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Buckets.ENDERITE_BUCKET));
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(Buckets.ENDERITE_LAVA_BUCKET), "enderite bucket took the lava");
        helper.succeed();
    }

    static void jadeReportsHeatSlotsAndShortestRemainingTime(GameTestHelper helper) {
        int smokingTicks = helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMOKING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.BEEF)), helper.getLevel()).orElseThrow().value().cookingTime();
        Block[] blocks = {CrucibleBlocks.IRON_CRUCIBLE, CrucibleBlocks.REINFORCED_CRUCIBLE, CrucibleBlocks.NETHERITE_CRUCIBLE, CrucibleBlocks.ENDERITE_CRUCIBLE};
        int[] sizes = {6, 12, 18, 30}; // reinforced 12 since N30, enderite 30 since N37
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(i + 1, 2, 2);
            helper.setBlock(pos.below(), Blocks.CAMPFIRE);
            helper.setBlock(pos, blocks[i]);
            var be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            ((net.minecraft.world.Container) be).setItem(0, new ItemStack(Items.BEEF, 2));
            ((net.minecraft.world.Container) be).setItem(1, new ItemStack(Items.DIRT));
        }
        BlockPos distant = new BlockPos(1, 3, 4);
        helper.setBlock(distant.below(2), Blocks.CAMPFIRE);
        helper.setBlock(distant, blocks[0]);
        ((net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(distant))).setItem(0, new ItemStack(Items.BEEF, 2));
        BlockPos mixed = new BlockPos(5, 2, 4);
        helper.setBlock(mixed.below(), Blocks.CAMPFIRE);
        helper.setBlock(mixed, blocks[0]);
        var mixedInventory = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(mixed));
        mixedInventory.setItem(0, new ItemStack(Items.BEEF, 2));
        mixedInventory.setItem(1, new ItemStack(Items.BREAD));
        BlockPos blocked = new BlockPos(5, 2, 6);
        helper.setBlock(blocked.below(), Blocks.CAMPFIRE);
        helper.setBlock(blocked, blocks[0]);
        var blockedInventory = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(blocked));
        blockedInventory.setItem(0, new ItemStack(Items.BEEF, 2));
        for (int slot = 1; slot < blockedInventory.getContainerSize(); slot++) blockedInventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < blocks.length; i++) {
                var be = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(i + 1, 2, 2)));
                int[] status = SimpleCruciblesApi.crucibleStatus(be);
                helper.assertTrue(status.length == 4 && status[0] == 1 && status[1] == 2 && status[2] == sizes[i], "heat and occupied slots tier " + i);
                helper.assertTrue(status[3] > 0 && status[3] <= smokingTicks * 2, "running duration tier " + i + ": " + status[3]);
                helper.assertTrue(CrucibleInfo.isCrucible(be), "Jade handles tier " + i);
                var lines = CrucibleInfo.crucibleLines(CrucibleInfo.crucibleStatus(be));
                helper.assertTrue(lines.size() == 3 && lines.get(2).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents seconds
                        && String.valueOf(seconds.getArgs()[0]).equals(String.valueOf((status[3] + 19) / 20)), "Jade seconds rounded up");
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.putIntArray(CrucibleInfo.CRUCIBLE_DATA, CrucibleInfo.crucibleStatus(be));
                helper.assertTrue(java.util.Arrays.equals(tag.getIntArray(CrucibleInfo.CRUCIBLE_DATA).orElseThrow(), status), "Jade server/client round trip");
            }
            int nearTicks = SimpleCruciblesApi.crucibleStatus(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 2))))[3];
            int distantTicks = SimpleCruciblesApi.crucibleStatus(helper.getLevel().getBlockEntity(helper.absolutePos(distant)))[3];
            helper.assertTrue(distantTicks > nearTicks, "two-block heat penalty included in remaining time");
            int mixedTicks = SimpleCruciblesApi.crucibleStatus(helper.getLevel().getBlockEntity(helper.absolutePos(mixed)))[3];
            helper.assertTrue(mixedTicks > 0 && mixedTicks < nearTicks, "shortest job wins over the first slot");
            helper.assertTrue(SimpleCruciblesApi.crucibleStatus(helper.getLevel().getBlockEntity(helper.absolutePos(blocked)))[3] == -1, "blocked jobs have no finite remaining time");
            helper.setBlock(new BlockPos(5, 2, 2), blocks[0]);
            var cold = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5, 2, 2)));
            helper.assertTrue(SimpleCruciblesApi.crucibleStatus(cold)[3] == -1 && CrucibleInfo.crucibleLines(CrucibleInfo.crucibleStatus(cold)).size() == 2, "cold crucible has no remaining time");
            helper.succeed();
        });
    }

    static void recipeHeatAndCatalogMatchCookingRules(GameTestHelper helper) {
        for (var type : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,
                net.minecraft.world.item.crafting.RecipeType.SMOKING, net.minecraft.world.item.crafting.RecipeType.SMELTING,
                net.minecraft.world.item.crafting.RecipeType.BLASTING)) {
            int expected = type == net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING || type == net.minecraft.world.item.crafting.RecipeType.SMOKING ? 1 : 2;
            helper.assertTrue(SimpleCruciblesApi.requiredHeat(new ItemStack(Items.BEEF), type) == expected, "recipe heat " + type);
            helper.assertTrue(SimpleCruciblesApi.requiredHeat(new ItemStack(Items.ANCIENT_DEBRIS), type) == 3, "extreme tag overrides " + type);
        }
        var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.AbstractCookingRecipe>();
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.BEEF));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMOKING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        var entries = CrucibleRecipeCatalog.build(recipes);
        int fastestMedium = recipes.stream().filter(recipe -> recipe.getType() != net.minecraft.world.item.crafting.RecipeType.SMELTING)
                .mapToInt(net.minecraft.world.item.crafting.AbstractCookingRecipe::cookingTime).min().orElseThrow();
        var beef = entries.stream().filter(entry -> entry.input().is(Items.BEEF)).toList();
        helper.assertTrue(beef.size() == 2, "one beef entry per heat");
        helper.assertTrue(beef.stream().anyMatch(entry -> entry.heat() == 1 && entry.baseTicks() == fastestMedium && entry.result().is(Items.COOKED_BEEF)), "fastest medium recipe wins");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.input().is(Items.BREAD) && entry.warming() && entry.heat() == 1), "bread warming fallback");
        helper.assertTrue(beef.stream().noneMatch(CrucibleRecipeCatalog.Entry::warming), "cooking takes precedence over warming");
        helper.assertTrue(SimpleCruciblesApi.crucibleCookingTicks(100, 1) == 200 && SimpleCruciblesApi.crucibleCookingTicks(200, 2) == 267, "display time includes heat factor and rounds ticks up");
        helper.succeed();
    }

    /** Uses {@code stack} like a right click and returns what the hand holds afterwards. */
    private static ItemStack useBucket(ServerLevel level, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND, success.heldItemTransformedTo());
        }
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** Heat: soul lava source extreme, flowing high; the reinforced cauldron with soul lava extreme (owner F30). */
    static void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), CrucibleFluids.SOUL_LAVA_BLOCK);
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.heatAt(level, helper.absolutePos(new BlockPos(1, 2, 1)))), "source: extreme");
        helper.setBlock(new BlockPos(4, 1, 1), CrucibleFluids.FLOWING_SOUL_LAVA.getFlowing(6, false).createLegacyBlock());
        helper.assertTrue("high".equals(SimpleCruciblesApi.heatAt(level, helper.absolutePos(new BlockPos(4, 2, 1)))), "flowing: high");
        helper.setBlock(new BlockPos(6, 1, 1), SimpleCruciblesApi.reinforcedCauldron("extreme"));
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.heatAt(level, helper.absolutePos(new BlockPos(6, 2, 1)))), "cauldron: extreme");
        helper.succeed();
    }

    /**
     * No bucket draws a durability bar (a worn ceramic one shows its wear in the texture), and every Enderite bucket -
     * empty, half, full, milk - has no tooltip line below its name.
     */
    static void bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        ItemStack worn = stack(Buckets.CERAMIC_WATER_BUCKET);
        if (!worn.isEmpty()) {
            worn.set(Buckets.CERAMIC_USES, 1);
        }
        for (ItemStack stack : List.of(worn, stack(Buckets.CERAMIC_BUCKET), stack(CrucibleFluids.COPPER_WATER_BUCKET),
                stack(Buckets.ENDERITE_WATER_BUCKET), stack(CrucibleFluids.SOUL_LAVA_BUCKET))) {
            if (!stack.isEmpty() && stack.isBarVisible()) {
                problems.add(stack.getItem() + " draws a durability bar");
            }
        }
        for (Item item : new Item[]{Buckets.ENDERITE_BUCKET, Buckets.ENDERITE_WATER_BUCKET, Buckets.FULL_ENDERITE_WATER_BUCKET,
                Buckets.ENDERITE_LAVA_BUCKET, Buckets.FULL_ENDERITE_LAVA_BUCKET, Buckets.ENDERITE_SOUL_LAVA_BUCKET,
                Buckets.FULL_ENDERITE_SOUL_LAVA_BUCKET, Buckets.ENDERITE_MILK_BUCKET, Buckets.FULL_ENDERITE_MILK_BUCKET}) {
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            List<Component> lines = new ArrayList<>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
            if (!lines.isEmpty()) {
                problems.add(item + " has tooltip lines " + lines.stream().map(Component::getString).toList());
            }
            if (stack.isBarVisible()) {
                problems.add(item + " draws a durability bar");
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }


    // ------------------------------------------------------------ split stage 5b (module level)

    /** Every moved bucket item, soul lava (block, two fluids), soul burn and the bucket components are registered. */
    static void registeredSoulLavaAndBuckets(GameTestHelper helper) {
        for (String path : SimpleCrucibles.BUCKETS_FROM_SB) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleCrucibles.id(path)), "no item " + path);
        }
        helper.assertTrue(CrucibleFluids.buckets().size() == SimpleCrucibles.BUCKETS_FROM_SB.size(), "bucket list and moved ids disagree");
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleCrucibles.id("soul_lava")) == CrucibleFluids.SOUL_LAVA_BLOCK, "soul lava block");
        helper.assertTrue(BuiltInRegistries.FLUID.getValue(SimpleCrucibles.id("soul_lava")) == CrucibleFluids.SOUL_LAVA
                && BuiltInRegistries.FLUID.getValue(SimpleCrucibles.id("flowing_soul_lava")) == CrucibleFluids.FLOWING_SOUL_LAVA, "soul lava fluids");
        helper.assertTrue(CrucibleEffects.SOUL_BURN != null && BuiltInRegistries.MOB_EFFECT.getValue(CrucibleEffects.SOUL_BURN_ID) == CrucibleEffects.SOUL_BURN.value(), "soul burn effect");
        for (String path : SimpleCrucibles.COMPONENTS_FROM_SB) {
            helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleCrucibles.id(path)), "no component " + path);
        }
        helper.assertTrue(CrucibleFluids.SOUL_LAVA.defaultFluidState().is(FluidTags.LAVA), "soul lava swims and burns like lava (minecraft:lava tag)");
        helper.succeed();
    }

    /**
     * Old worlds (split stage 5b): SimpleBuilding's bucket items, soul lava block and fluids, soul burn and the bucket
     * components load under the module's ids - a saved stack keeps its oxidation - and the bucket recipes move too.
     */
    static void migrationOfBucketsAndSoulLava(GameTestHelper helper) {
        var problems = new java.util.ArrayList<String>();
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (String path : SimpleCrucibles.BUCKETS_FROM_SB) {
            Identifier from = Identifier.fromNamespaceAndPath("simplebuilding", path);
            Item item = BuiltInRegistries.ITEM.getValue(SimpleCrucibles.id(path));
            if (BuiltInRegistries.ITEM.getValue(from) != item) problems.add("ITEM.getValue(" + from + ")");
            net.minecraft.nbt.CompoundTag saved = (net.minecraft.nbt.CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(item)).getOrThrow();
            saved.putString("id", from.toString());
            ItemStack loaded = ItemStack.CODEC.parse(ops, saved).result().orElse(ItemStack.EMPTY);
            if (loaded.getItem() != item) problems.add("stack " + saved + " loads as " + loaded);
        }
        // A copper bucket saved with SimpleBuilding's component id keeps its oxidation stage.
        ItemStack copper = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        copper.set(CrucibleComponents.OXIDATION, 2);
        String text = ItemStack.CODEC.encodeStart(ops, copper).getOrThrow().toString()
                .replace("simplecrucibles:oxidation", "simplebuilding:oxidation").replace("simplecrucibles:copper_bucket", "simplebuilding:copper_bucket");
        try {
            var tag = net.minecraft.nbt.TagParser.parseCompoundFully(text);
            ItemStack old = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
            Integer stage = old.get(CrucibleComponents.OXIDATION);
            if (!old.is(CrucibleFluids.COPPER_BUCKET) || stage == null || stage != 2) problems.add("old copper bucket " + text + " loads as " + old + " stage " + stage);
        } catch (Exception e) {
            problems.add("old stack text " + text + ": " + e);
        }
        if (BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "soul_lava")) != CrucibleFluids.SOUL_LAVA_BLOCK) problems.add("old soul lava block");
        if (BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "soul_lava")) != CrucibleFluids.SOUL_LAVA) problems.add("old soul lava fluid");
        if (BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "flowing_soul_lava")) != CrucibleFluids.FLOWING_SOUL_LAVA) problems.add("old flowing soul lava");
        if (BuiltInRegistries.MOB_EFFECT.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "soul_burn")) != CrucibleEffects.SOUL_BURN.value()) problems.add("old soul burn");
        for (String path : SimpleCrucibles.COMPONENTS_FROM_SB) {
            if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("simplebuilding", path)) != BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(SimpleCrucibles.id(path)))
                problems.add("old component " + path);
        }
        // Block state palette entry of soul lava in an old chunk.
        net.minecraft.nbt.CompoundTag state = net.minecraft.nbt.NbtUtils.writeBlockState(CrucibleFluids.SOUL_LAVA_BLOCK.defaultBlockState());
        // 26.3 palette entries name the block under "id" (the old probe set "Name" and never read the old id).
        if (!state.contains("id")) problems.add("palette entry without id: " + state);
        state.remove("Name");
        state.putString("id", "simplebuilding:soul_lava");
        if (!net.minecraft.nbt.NbtUtils.readBlockState(helper.getLevel().holderLookup(Registries.BLOCK), state).is(CrucibleFluids.SOUL_LAVA_BLOCK)) problems.add("palette " + state);
        var decoded = net.minecraft.world.level.block.state.BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, state).result().orElse(null);
        if (decoded == null || !decoded.is(CrucibleFluids.SOUL_LAVA_BLOCK)) problems.add("chunk codec loads " + state + " as " + decoded);
        for (String path : SimpleCrucibles.BUCKET_RECIPES_FROM_SB) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.RECIPE, SimpleCrucibles.id(path));
            // IG31-13: Enderite recipes load only with Simple Ores (owner of the material).
            boolean expected = !path.contains("enderite") || com.simplelib.api.SimpleLibApi.materialPresent("enderite");
            if (helper.getLevel().getServer().getRecipeManager().byKey(key).isEmpty() == expected) problems.add((expected ? "no recipe " : "recipe without Simple Ores ") + path);
            if (!SimpleCrucibles.id(path).equals(com.simplelib.api.IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", path)))) problems.add("recipe move " + path);
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** The soulLava group is clamped like SimpleBuilding's was and taken over once from simplebuilding.json (server.soulLava). */
    static void configSoulLavaBoundsAndTakeover(GameTestHelper helper) {
        try {
            JsonObject json = new JsonObject();
            JsonObject soul = new JsonObject();
            soul.addProperty("flowOverworld", 99);
            soul.addProperty("soulBurnChance", 7.0);
            soul.addProperty("fuelMultiplier", 0);
            soul.addProperty("springChance", 1.0);
            json.add("soulLava", soul);
            CrucibleConfig.apply(json);
            helper.assertTrue(SoulLava.flowOverworld() == 4 && SoulLava.soulBurnChance() == 1.0 && SoulLava.fuelMultiplier() == 1
                    && SoulLava.springChance() == 0.05, "soul lava values not clamped");
            helper.assertTrue(com.simplecrucibles.fluid.SoulLavaFluid.reach(helper.getLevel()) == 4, "the fluid reads the configured reach");
            helper.assertTrue(CrucibleFluids.SOUL_LAVA.getTickDelay(helper.getLevel()) == SoulLava.tickDelayOverworld(), "the fluid reads the configured delay");
            CrucibleConfig.reset();
            helper.assertTrue(SoulLava.flowOverworld() == 2 && SoulLava.flowNether() == 5 && SoulLava.soulBurnTicks() == 2400
                    && SoulLava.fuelTicks() == 200_000, "soul lava defaults");
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplecrucibles-soullava");
            String sbText = "{ \"server\": { \"soulLava\": { \"flowNether\": 3, \"burnSeconds\": 40 }, \"tools\": {} } }";
            java.nio.file.Files.writeString(dir.resolve(CrucibleConfig.SB_FILE), sbText);
            CrucibleConfig.load(dir);
            helper.assertTrue(SoulLava.flowNether() == 3 && SoulLava.burnSeconds() == 40, "soul lava options not taken over from SimpleBuilding");
            var written = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(dir.resolve(CrucibleConfig.FILE))).getAsJsonObject();
            helper.assertTrue(written.getAsJsonObject("soulLava").get("flowNether").getAsInt() == 3, "own file lacks the taken soul lava group: " + written);
            helper.assertTrue(java.nio.file.Files.readString(dir.resolve(CrucibleConfig.SB_FILE)).equals(sbText), "SimpleBuilding's file was changed");
            java.nio.file.Files.writeString(dir.resolve(CrucibleConfig.SB_FILE), "{ \"server\": { \"soulLava\": { \"flowNether\": 6 } } }");
            CrucibleConfig.load(dir);
            helper.assertTrue(SoulLava.flowNether() == 3, "the own soul lava group did not win");
        } catch (java.io.IOException | RuntimeException e) {
            helper.fail("soul lava config threw " + e);
            return;
        } finally {
            CrucibleConfig.reset();
        }
        helper.succeed();
    }

    /** Jade on the reinforced cauldron (block state only): content, fill level of water and powder snow; nothing for others. */
    static void jadeReinforcedCauldronLines(GameTestHelper helper) {
        var problems = new java.util.ArrayList<String>();
        for (String content : List.of("empty", "water", "lava", "powder_snow", "extreme")) {
            List<Component> lines = CrucibleInfo.cauldronLines(SimpleCruciblesApi.reinforcedCauldron(content));
            int expected = content.equals("water") || content.equals("powder_snow") ? 2 : 1;
            if (lines.size() != expected || !(lines.get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents first)
                    || !first.getKey().equals("jade.simplecrucibles.cauldron.content")) problems.add(content + ": " + lines);
        }
        if (!CrucibleInfo.cauldronLines(Blocks.WATER_CAULDRON.defaultBlockState()).isEmpty()) problems.add("vanilla cauldron got lines");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** JEI info pages: every bucket without a recipe (filled, worn, full) and the Enderite tier have one; all items exist. */
    static void jeiInfoPagesCoverTheFilledBuckets(GameTestHelper helper) {
        var shown = new java.util.HashSet<Item>();
        CrucibleInfo.infoPages().forEach((page, items) -> items.forEach(item -> {
            helper.assertTrue(item != null && item.asItem() != Items.AIR, "page " + page + " names a missing item");
            shown.add(item.asItem());
        }));
        for (Item bucket : CrucibleFluids.buckets()) {
            helper.assertTrue(shown.contains(bucket), "no JEI info page shows " + BuiltInRegistries.ITEM.getKey(bucket));
        }
        helper.succeed();
    }
}
