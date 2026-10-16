package com.simplecrucibles.test;

import com.google.gson.JsonObject;
import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplecrucibles.compat.CrucibleInfo;
import com.simplecrucibles.compat.CrucibleRecipeCatalog;
import com.simplecrucibles.config.CrucibleConfig;
import com.simplecrucibles.effect.CrucibleEffects;
import com.simplecrucibles.effect.SoulBurnEffect;
import com.simplecrucibles.fluid.CrucibleBucketItem;
import com.simplecrucibles.fluid.CrucibleFluids;
import com.simplecrucibles.fluid.EnderiteMilkBucketItem;
import com.simplecrucibles.fluid.SoulLava;
import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplecrucibles.registry.CrucibleComponents;
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
        all.put("ceramic_bucket_holds_water_and_breaks_after_four_pours", BucketTests::ceramicBucketHoldsWaterAndBreaksAfterFourPours);
        all.put("mod_buckets_stay_in_the_hand_when_pouring_and_scooping", BucketTests::modBucketsStayInTheHandWhenPouringAndScooping);
        all.put("enderite_bucket_takes_milk_and_drinks_one_filling", BucketTests::enderiteBucketTakesMilkAndDrinksOneFilling);
        all.put("enderite_bucket_holds_two_buckets_of_one_fluid", BucketTests::enderiteBucketHoldsTwoBucketsOfOneFluid);
        all.put("enderite_bucket_two_buckets_in_cauldrons_and_dispensers", BucketTests::enderiteBucketTwoBucketsInCauldronsAndDispensers);
        all.put("ceramic_lava_bucket_scoops_pours_and_wears", BucketTests::ceramicLavaBucketScoopsPoursAndWears);
        all.put("copper_bucket_oxidizes_only_in_hotbar_or_offhand", BucketTests::copperBucketOxidizesOnlyInHotbarOrOffhand);
        all.put("copper_bucket_waxed_water_weathers_at_half_pace", BucketTests::copperBucketWaxedWaterWeathersAtHalfPace);
        all.put("copper_bucket_placed_weathers_and_frame_care", BucketTests::copperBucketPlacedWeathersAndFrameCare);
        all.put("ceramic_bucket_with_lava_is_brittle_and_breaks_on_pour", BucketTests::ceramicBucketWithLavaIsBrittleAndBreaksOnPour);
        all.put("ceramic_bucket_wears_through_stages_and_keeps_its_filling", BucketTests::ceramicBucketWearsThroughStagesAndKeepsItsFilling);
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
        helper.assertTrue(CrucibleBucketItem.filled(CrucibleBucketItem.Kind.COPPER, CrucibleFluids.SOUL_LAVA) == null, "copper bucket refuses soul lava");
        CrucibleBucketItem water = (CrucibleBucketItem) CrucibleFluids.COPPER_WATER_BUCKET;
        ItemStack after = water.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(water));
        helper.assertTrue(after.is(CrucibleFluids.COPPER_BUCKET) && CrucibleBucketItem.oxidation(after) == 0, "pouring no longer oxidizes (N35)");
        helper.assertTrue(!level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).isSource(), "copper water is never a source");
        CrucibleBucketItem lava = (CrucibleBucketItem) CrucibleFluids.COPPER_LAVA_BUCKET;
        helper.assertTrue(lava.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(lava)).isEmpty(), "copper bucket breaks on lava");
        after.set(CrucibleComponents.OXIDATION, 1);
        helper.assertTrue(CrucibleBucketItem.scrape(after) && CrucibleBucketItem.oxidation(after) == 0, "axe scrapes one stage");
        after.set(CrucibleComponents.WAXED, true);
        CrucibleBucketItem.oxidize(after);
        helper.assertTrue(CrucibleBucketItem.oxidation(after) == 0, "waxed copper does not oxidize");
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
        helper.assertFalse(CrucibleBucketItem.canScoop(old), "a fully oxidized copper bucket may scoop");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, old, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.WATER_CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.COPPER_BUCKET), "the oxidized bucket took water from the cauldron");
        helper.assertTrue(CrucibleBucketItem.scrape(old) && CrucibleBucketItem.canScoop(old), "scraped to stage 2 it scoops again");
        ItemStack full = new ItemStack(CrucibleFluids.COPPER_WATER_BUCKET);
        full.set(CrucibleComponents.OXIDATION, 3);
        ItemStack after = ((CrucibleBucketItem) CrucibleFluids.COPPER_WATER_BUCKET).pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), full);
        helper.assertTrue(after.is(CrucibleFluids.COPPER_BUCKET), "a full stage-3 bucket still pours");
        helper.succeed();
    }

    /**
     * Ceramic bucket: water (lava has its own test), pours a real source, scooping does not wear it, and the fourth
     * pour breaks it; 3 clay balls craft the raw bucket, which smelts into the ceramic bucket.
     */
    static void ceramicBucketHoldsWaterAndBreaksAfterFourPours(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(CrucibleBucketItem.filled(CrucibleBucketItem.Kind.CERAMIC, CrucibleFluids.SOUL_LAVA) == null, "ceramic takes soul lava");
        helper.assertTrue(CrucibleBucketItem.filled(CrucibleBucketItem.Kind.CERAMIC, Fluids.WATER) == CrucibleFluids.CERAMIC_WATER_BUCKET, "ceramic water");
        helper.assertTrue(CrucibleBucketItem.CERAMIC_USES == 4 && CrucibleBucketItem.CERAMIC_USES_PER_STAGE == 1
                        && !new ItemStack(CrucibleFluids.CERAMIC_BUCKET).isDamageableItem(),
                "4 pours, one per stage, no durability bar");
        CrucibleBucketItem water = (CrucibleBucketItem) CrucibleFluids.CERAMIC_WATER_BUCKET;
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack bucket = new ItemStack(CrucibleFluids.CERAMIC_BUCKET);
        for (int cycle = 1; cycle <= 4; cycle++) {
            bucket = CrucibleBucketItem.fill(bucket, CrucibleFluids.CERAMIC_WATER_BUCKET);
            helper.assertTrue(CrucibleBucketItem.ceramicUses(bucket) == cycle - 1
                            && bucket.is(CrucibleFluids.CERAMIC_WATER[cycle - 1]),
                    "scoop " + cycle + " does not wear the bucket: " + bucket);
            water = (CrucibleBucketItem) bucket.getItem();
            bucket = water.pourAt(level, target, bucket);
            if (cycle == 1) helper.assertTrue(level.getFluidState(target).isSource(), "ceramic water makes a source");
            if (cycle < 4) {
                helper.assertTrue(bucket.is(CrucibleFluids.CERAMIC_EMPTY[cycle])
                                && CrucibleBucketItem.ceramicUses(bucket) == cycle,
                        "after pour " + cycle + ": " + bucket + " pours " + CrucibleBucketItem.ceramicUses(bucket));
            }
        }
        helper.assertTrue(bucket.isEmpty(), "the fourth pour did not break the ceramic bucket");
        var recipes = level.getServer().getRecipeManager();
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(clay, ItemStack.EMPTY, clay, ItemStack.EMPTY, clay, ItemStack.EMPTY));
        ItemStack raw = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid);
        helper.assertTrue(raw.is(CrucibleFluids.RAW_CERAMIC_BUCKET) && raw.getCount() == 1, "3 clay -> raw ceramic bucket, got " + raw);
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(raw);
        ItemStack fired = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level).orElseThrow().value().assemble(input);
        helper.assertTrue(fired.is(CrucibleFluids.CERAMIC_BUCKET) && CrucibleBucketItem.ceramicUses(fired) == 0, "raw bucket smelts into the ceramic bucket");
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
                {CrucibleFluids.ENDERITE_BUCKET, CrucibleFluids.ENDERITE_WATER_BUCKET},
                {CrucibleFluids.CERAMIC_BUCKET, CrucibleFluids.CERAMIC_WATER_BUCKET}};
        int x = 1;
        for (Item[] kind : kinds) {
            CrucibleBucketItem.Kind expected = ((CrucibleBucketItem) kind[1]).kind();
            net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2.0, 3.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
            BlockPos water = helper.absolutePos(new BlockPos(x, 1, 3));
            ItemStack full = new ItemStack(kind[1]);
            int usesBefore = CrucibleBucketItem.ceramicUses(full);
            // Owner N21: a half Enderite bucket pours only on sneak + use (plain use scoops).
            player.setShiftKeyDown(expected == CrucibleBucketItem.Kind.ENDERITE);
            ItemStack poured = useBucket(level, player, full);
            player.setShiftKeyDown(false);
            // Copper pours no source (owner 32), the others do.
            helper.assertTrue(level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER)
                            && level.getFluidState(water).isSource() == (expected != CrucibleBucketItem.Kind.COPPER),
                    expected + ": pouring placed the wrong water; hand " + poured + ", block " + level.getBlockState(water)
                            + ", below " + level.getBlockState(water.below()) + ", eye " + player.getEyePosition() + ", xRot " + player.getXRot());
            helper.assertTrue(poured.getItem() instanceof CrucibleBucketItem empty && empty.kind() == expected && !poured.is(kind[1]) && !poured.is(CrucibleBucketItem.filled(expected, Fluids.WATER)),
                    expected + ": after pouring the hand holds " + poured + " instead of its empty bucket");
            if (expected == CrucibleBucketItem.Kind.CERAMIC) {
                helper.assertTrue(CrucibleBucketItem.ceramicUses(poured) == usesBefore + 1, "ceramic: one wear step per pour, uses " + CrucibleBucketItem.ceramicUses(poured));
            }
            if (expected == CrucibleBucketItem.Kind.COPPER) level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
            ItemStack scooped = useBucket(level, player, poured);
            Item expectedFilled = expected == CrucibleBucketItem.Kind.CERAMIC
                    ? CrucibleFluids.CERAMIC_WATER[1] : CrucibleBucketItem.filled(expected, Fluids.WATER);
            helper.assertTrue(scooped.getItem() instanceof CrucibleBucketItem filled && filled.kind() == expected && scooped.is(expectedFilled),
                    expected + ": after scooping the hand holds " + scooped + " instead of its water bucket");
            helper.assertTrue(level.getFluidState(water).isEmpty(), expected + ": scooping left the source");
            x += 3;
        }
        net.minecraft.world.entity.player.Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true; // the mock's game type alone does not grant infinite materials everywhere
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2.0, 6.5));
        creative.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
        ItemStack kept = useBucket(level, creative, new ItemStack(CrucibleFluids.CERAMIC_WATER_BUCKET));
        helper.assertTrue(kept.is(CrucibleFluids.CERAMIC_WATER_BUCKET) && CrucibleBucketItem.ceramicUses(kept) == 0, "creative: the bucket stays as it was, got " + kept);
        helper.succeed();
    }

    /**
     * Queue N32: milking a cow and a goat with the Enderite bucket gives the half milk bucket, milking again the full
     * one (no third), a baby gives nothing. Drinking uses one filling and clears all effects; the crafting remainder of
     * a full one is the half one, of a half one the empty bucket (that is what the milk cauldron reads).
     */
    static void enderiteBucketTakesMilkAndDrinksOneFilling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
        net.minecraft.world.entity.animal.cow.Cow cow = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(2, 1, 2));
        net.minecraft.world.entity.animal.goat.Goat goat = helper.spawn(net.minecraft.world.entity.EntityTypes.GOAT, new BlockPos(3, 1, 2));
        net.minecraft.world.entity.animal.cow.Cow calf = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(4, 1, 2));
        calf.setBaby(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrucibleFluids.ENDERITE_BUCKET));
        player.interactOn(calf, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.ENDERITE_BUCKET), "a calf gives no milk");
        player.interactOn(cow, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.ENDERITE_MILK_BUCKET), "milking a cow: half milk bucket, got " + player.getMainHandItem());
        player.interactOn(goat, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.FULL_ENDERITE_MILK_BUCKET), "milking a goat: full milk bucket, got " + player.getMainHandItem());
        player.interactOn(cow, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.FULL_ENDERITE_MILK_BUCKET), "a full one takes no more");
        helper.assertTrue(((EnderiteMilkBucketItem) CrucibleFluids.FULL_ENDERITE_MILK_BUCKET).amount() == 2
                && ((EnderiteMilkBucketItem) CrucibleFluids.ENDERITE_MILK_BUCKET).amount() == 1, "fill amounts 2/2 and 1/2");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 600));
        ItemStack drunk = player.getMainHandItem().finishUsingItem(level, player);
        helper.assertTrue(drunk.is(CrucibleFluids.ENDERITE_MILK_BUCKET) && player.getActiveEffects().isEmpty(),
                "drinking a full one: half left, effects cleared, got " + drunk);
        drunk = drunk.finishUsingItem(level, player);
        helper.assertTrue(drunk.is(CrucibleFluids.ENDERITE_BUCKET), "drinking the half one leaves the empty bucket, got " + drunk);
        helper.assertTrue(CrucibleFluids.FULL_ENDERITE_MILK_BUCKET.getCraftingRemainder().item().value() == CrucibleFluids.ENDERITE_MILK_BUCKET
                && CrucibleFluids.ENDERITE_MILK_BUCKET.getCraftingRemainder().item().value() == CrucibleFluids.ENDERITE_BUCKET,
                "milk bucket crafting remainders (milk cauldron contract)");
        helper.succeed();
    }

    /**
     * Owner N21/N28: the Enderite bucket holds two buckets of one fluid. Use only scoops while there is room (empty to
     * half to full); a full one pours one bucket on use; sneak + use pours one bucket from a full or half one. A half
     * one never takes a different fluid and never pours on plain use. Soul lava works the same way.
     */
    static void enderiteBucketHoldsTwoBucketsOfOneFluid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2.0, 3.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
        BlockPos water = helper.absolutePos(new BlockPos(1, 1, 3));
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        ItemStack hand = useBucket(level, player, new ItemStack(CrucibleFluids.ENDERITE_BUCKET));
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "first scoop: half, got " + hand);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.FULL_ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "second scoop: full, got " + hand);
        helper.assertTrue(((CrucibleBucketItem) hand.getItem()).amount() == 2 && ((CrucibleBucketItem) CrucibleFluids.ENDERITE_WATER_BUCKET).amount() == 1,
                "fill amounts 2/2 and 1/2");
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isSource(), "use on a full one pours one bucket, got " + hand);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.FULL_ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(),
                "use on a half one only scoops (took the source back), got " + hand);
        player.setShiftKeyDown(true);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isSource(), "sneak + use on a full one pours one bucket, got " + hand);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_BUCKET) && level.getFluidState(water).isSource(), "sneak + use on a half one pours the last bucket, got " + hand);
        player.setShiftKeyDown(false);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        hand = useBucket(level, player, new ItemStack(CrucibleFluids.ENDERITE_WATER_BUCKET));
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "plain use on a half one never pours, got " + hand);
        level.setBlock(water, Blocks.LAVA.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(CrucibleFluids.ENDERITE_WATER_BUCKET) && level.getFluidState(water).is(net.minecraft.tags.FluidTags.LAVA),
                "a half water bucket takes no lava, got " + hand);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(CrucibleFluids.fullEnderite(CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET) == CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET
                && CrucibleFluids.fullEnderite(CrucibleFluids.ENDERITE_LAVA_BUCKET) == CrucibleFluids.FULL_ENDERITE_LAVA_BUCKET, "soul lava and lava fill up to two too");
        CrucibleBucketItem soul = (CrucibleBucketItem) CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET;
        BlockPos soulAt = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(soul.pourAt(level, soulAt, new ItemStack(soul)).is(CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET)
                && level.getFluidState(soulAt).getType() == CrucibleFluids.SOUL_LAVA, "a full soul lava bucket pours one and keeps one");
        helper.assertTrue(!CrucibleFluids.creativeBuckets().contains(CrucibleFluids.FULL_ENDERITE_WATER_BUCKET)
                && CrucibleFluids.buckets().containsAll(CrucibleFluids.fullEnderiteBuckets()), "full buckets: registered, not in the creative tab");
        helper.succeed();
    }

    /**
     * Owner N21: cauldrons and dispensers follow the two-bucket rule. A full Enderite bucket fills a Vanilla or
     * reinforced cauldron with one bucket and stays half; a half one takes a full cauldron of its fluid and becomes
     * full. A dispenser pours one bucket of a full one and keeps the half one; a half one pours and comes back empty.
     */
    static void enderiteBucketTwoBucketsInCauldronsAndDispensers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        BlockPos cauldron = new BlockPos(2, 1, 2);
        helper.setBlock(cauldron, Blocks.CAULDRON);
        BlockPos abs = helper.absolutePos(cauldron);
        ServerPlayer player = player(helper, new ItemStack(CrucibleFluids.FULL_ENDERITE_WATER_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(abs).is(Blocks.WATER_CAULDRON) && player.getMainHandItem().is(CrucibleFluids.ENDERITE_WATER_BUCKET),
                "full bucket fills the cauldron and stays half: " + player.getMainHandItem());
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(abs).is(Blocks.CAULDRON) && player.getMainHandItem().is(CrucibleFluids.FULL_ENDERITE_WATER_BUCKET),
                "half bucket takes the full cauldron back: " + player.getMainHandItem());
        BlockPos reinforced = new BlockPos(4, 1, 2);
        helper.setBlock(reinforced, SimpleCruciblesApi.reinforcedCauldron("empty"));
        BlockPos rAbs = helper.absolutePos(reinforced);
        BlockHitResult rHit = new BlockHitResult(Vec3.atCenterOf(rAbs), Direction.UP, rAbs, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET));
        level.getBlockState(rAbs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, rHit);
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(rAbs)))
                && player.getMainHandItem().is(CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET), "reinforced cauldron: one bucket of soul lava, half stays");
        level.getBlockState(rAbs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, rHit);
        helper.assertTrue("empty".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(rAbs)))
                && player.getMainHandItem().is(CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET), "reinforced cauldron: half takes it back to full");
        BlockPos dispenser = new BlockPos(1, 1, 6);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST));
        BlockPos front = helper.absolutePos(dispenser.east());
        var be = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(helper.absolutePos(dispenser));
        be.setItem(0, new ItemStack(CrucibleFluids.FULL_ENDERITE_LAVA_BUCKET));
        helper.setBlock(dispenser.above(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(level.getFluidState(front).is(net.minecraft.tags.FluidTags.LAVA) && level.getFluidState(front).isSource(), "dispenser poured lava");
            helper.assertTrue(be.getItem(0).is(CrucibleFluids.ENDERITE_LAVA_BUCKET), "dispenser keeps the half bucket: " + be.getItem(0));
            CrucibleBucketItem half = (CrucibleBucketItem) CrucibleFluids.ENDERITE_LAVA_BUCKET;
            helper.assertTrue(half.afterPour(new ItemStack(half)).is(CrucibleFluids.ENDERITE_BUCKET), "a half one pours its last bucket");
            helper.succeed();
        });
    }

    /**
     * Ceramic lava bucket: the empty ceramic bucket scoops lava without wearing, pours a real lava source and
     * comes back worn (no break like copper); soul lava and milk stay refused; it is furnace fuel without a
     * remainder (burns up) and sits in the creative tab and the filled-bucket JEI info.
     */
    static void ceramicLavaBucketScoopsPoursAndWears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(CrucibleFluids.CERAMIC_LAVA_BUCKET != null
                && CrucibleBucketItem.filled(CrucibleBucketItem.Kind.CERAMIC, Fluids.LAVA) == CrucibleFluids.CERAMIC_LAVA_BUCKET, "ceramic lava bucket");
        helper.assertTrue(CrucibleBucketItem.filled(CrucibleBucketItem.Kind.CERAMIC, CrucibleFluids.SOUL_LAVA) == null, "ceramic takes soul lava");
        CrucibleBucketItem lava = (CrucibleBucketItem) CrucibleFluids.CERAMIC_LAVA_BUCKET;
        helper.assertTrue(lava.kind() == CrucibleBucketItem.Kind.CERAMIC && lava.breaksOnPour(), "ceramic lava must break on pour (N35)");
        ItemStack bucket = CrucibleBucketItem.fill(new ItemStack(CrucibleFluids.CERAMIC_BUCKET), CrucibleFluids.CERAMIC_LAVA_BUCKET);
        helper.assertTrue(bucket.is(CrucibleFluids.CERAMIC_LAVA[CrucibleBucketItem.CERAMIC_STAGES - 1]), "scoop makes it brittle: " + bucket);
        lava = (CrucibleBucketItem) bucket.getItem();
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        bucket = lava.pourAt(level, target, bucket);
        helper.assertTrue(level.getFluidState(target).is(net.minecraft.tags.FluidTags.LAVA) && level.getFluidState(target).isSource(),
                "ceramic lava makes a lava source");
        helper.assertTrue(bucket.isEmpty(), "the brittle lava bucket must break when poured: " + bucket);
        ItemStack fuel = new ItemStack(CrucibleFluids.CERAMIC_LAVA_BUCKET);
        boolean burns = fuel.getComponents().keySet().stream().anyMatch(type ->
                "minecraft:cooking_fuel".equals(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))));
        helper.assertTrue(burns, "ceramic lava bucket is no fuel");
        helper.assertTrue(!fuel.getComponents().keySet().stream().anyMatch(type ->
                "minecraft:use_remainder".equals(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)))),
                "ceramic lava bucket leaves a remainder");
        helper.assertTrue(CrucibleFluids.buckets().contains(CrucibleFluids.CERAMIC_LAVA_BUCKET), "not in the bucket list");
        helper.succeed();
    }

    /** N35: copper buckets oxidize only in the hotbar or off hand, never in the rest of the inventory (simulated ticks). */
    static void copperBucketOxidizesOnlyInHotbarOrOffhand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hotbar = new ItemStack(CrucibleFluids.COPPER_BUCKET), inventory = new ItemStack(CrucibleFluids.COPPER_BUCKET), off = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        player.getInventory().setItem(3, hotbar);
        player.getInventory().setItem(20, inventory);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        helper.assertTrue(CrucibleBucketItem.oxidationInterval(hotbar) == CrucibleBucketItem.OXIDATION_TICKS, "unwaxed pace");
        for (int t = 0; t < 600_000; t++) {
            for (ItemStack stack : new ItemStack[] {hotbar, inventory, off}) stack.getItem().inventoryTick(stack, level, player, null);
        }
        helper.assertTrue(CrucibleBucketItem.oxidation(hotbar) >= 1, "a hotbar bucket must weather, stage " + CrucibleBucketItem.oxidation(hotbar));
        helper.assertTrue(CrucibleBucketItem.oxidation(off) >= 1, "an off-hand bucket must weather, stage " + CrucibleBucketItem.oxidation(off));
        helper.assertTrue(CrucibleBucketItem.oxidation(inventory) == 0, "a bucket in the inventory must not weather");
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
        helper.assertTrue(CrucibleBucketItem.oxidationInterval(empty) == 0, "waxed empty must not react");
        ItemStack water = CrucibleBucketItem.fill(empty, CrucibleFluids.COPPER_WATER_BUCKET);
        ItemStack lava = CrucibleBucketItem.fill(empty, CrucibleFluids.COPPER_LAVA_BUCKET);
        helper.assertTrue(CrucibleBucketItem.waxed(water) && CrucibleBucketItem.waxWorn(water) && CrucibleBucketItem.waxWorn(lava), "filling wears the wax");
        helper.assertTrue(CrucibleBucketItem.oxidationInterval(water) == 2 * CrucibleBucketItem.oxidationInterval(plain), "worn wax is half pace");
        ItemStack kept = CrucibleBucketItem.fill(new ItemStack(CrucibleFluids.COPPER_BUCKET), CrucibleFluids.COPPER_WATER_BUCKET);
        helper.assertTrue(!CrucibleBucketItem.waxWorn(kept), "an unwaxed bucket has no wax to wear");
        ItemStack emptied = ((CrucibleBucketItem) water.getItem()).afterPour(water);
        helper.assertTrue(CrucibleBucketItem.waxWorn(emptied), "emptying keeps the worn wax");
        player.getInventory().setItem(0, empty);
        player.getInventory().setItem(1, water);
        for (int t = 0; t < 1_500_000; t++) {
            for (ItemStack stack : new ItemStack[] {empty, water}) stack.getItem().inventoryTick(stack, level, player, null);
        }
        helper.assertTrue(CrucibleBucketItem.oxidation(empty) == 0, "waxed empty weathered");
        helper.assertTrue(CrucibleBucketItem.oxidation(water) >= 1 && CrucibleBucketItem.waxed(water), "worn wax must weather slowly and stay waxed");
        // honeycomb renews, then it holds still again
        ServerPlayer actor = player(helper, emptied, new ItemStack(Items.HONEYCOMB));
        helper.assertTrue(CrucibleBucketItem.care(level, actor, emptied, actor.getOffhandItem(), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && !CrucibleBucketItem.waxWorn(emptied) && CrucibleBucketItem.oxidationInterval(emptied) == 0, "honeycomb renews the wax");
        helper.assertTrue(CrucibleBucketItem.care(level, actor, emptied, new ItemStack(Items.HONEYCOMB), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
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
            ItemStack w = CrucibleBucketItem.weatherPlaced(drop.getItem(), level.getRandom());
            if (w != null) { drop.setItem(w); stage = CrucibleBucketItem.oxidation(w); }
        }
        helper.assertTrue(stage == 1 && CrucibleBucketItem.oxidation(drop.getItem()) == 1, "a dropped copper bucket must weather");
        helper.assertTrue(CrucibleBucketItem.weatherPlaced(waxedStack, level.getRandom()) == null, "a waxed placed bucket does not weather");
        drop.discard();
        ItemStack framed = new ItemStack(CrucibleFluids.COPPER_BUCKET);
        ServerPlayer actor = player(helper, ItemStack.EMPTY, new ItemStack(Items.HONEYCOMB));
        helper.assertTrue(CrucibleBucketItem.care(level, actor, framed, actor.getOffhandItem(), net.minecraft.world.entity.EquipmentSlot.OFFHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && CrucibleBucketItem.waxed(framed), "honeycomb waxes the bucket");
        helper.assertTrue(CrucibleBucketItem.care(level, actor, framed, new ItemStack(Items.IRON_AXE), net.minecraft.world.entity.EquipmentSlot.MAINHAND, actor.blockPosition())
                == InteractionResult.SUCCESS && !CrucibleBucketItem.waxed(framed), "an axe takes the wax off");
        helper.succeed();
    }

    /** N35: lava makes the ceramic bucket brittle at once; pouring it places the lava and the bucket is gone. */
    static void ceramicBucketWithLavaIsBrittleAndBreaksOnPour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ItemStack water = CrucibleBucketItem.fill(new ItemStack(CrucibleFluids.CERAMIC_BUCKET), CrucibleFluids.CERAMIC_WATER_BUCKET);
        helper.assertTrue(water.is(CrucibleFluids.CERAMIC_WATER[0]), "water keeps the stage");
        ItemStack lava = CrucibleBucketItem.fill(new ItemStack(CrucibleFluids.CERAMIC_BUCKET), CrucibleFluids.CERAMIC_LAVA_BUCKET);
        helper.assertTrue(lava.is(CrucibleFluids.CERAMIC_LAVA[CrucibleBucketItem.CERAMIC_STAGES - 1]), "lava makes it brittle: " + lava);
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack left = ((CrucibleBucketItem) lava.getItem()).pourAt(level, target, lava);
        helper.assertTrue(left.isEmpty() && level.getFluidState(target).is(net.minecraft.tags.FluidTags.LAVA), "brittle bucket must break and leave lava");
        helper.succeed();
    }

    /**
     * Ceramic wear stages: as many stages as the copper bucket's oxidation (0..3), one item per stage and filling,
     * one pour each; a stage change keeps the filling, scooping does not wear it, and the brittle bucket breaks on its last
     * pour, the worn stages stay out of the creative tab but in the bucket list (dispensers).
     */
    static void ceramicBucketWearsThroughStagesAndKeepsItsFilling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(CrucibleBucketItem.CERAMIC_STAGES == 4 && CrucibleBucketItem.CERAMIC_USES_PER_STAGE == 1
                && CrucibleBucketItem.CERAMIC_STAGES * CrucibleBucketItem.CERAMIC_USES_PER_STAGE == CrucibleBucketItem.CERAMIC_USES,
                "4 stages of 1 pour (copper: oxidation 0..3)");
        for (int stage = 0; stage < CrucibleBucketItem.CERAMIC_STAGES; stage++) {
            for (Item item : new Item[] {CrucibleFluids.CERAMIC_EMPTY[stage], CrucibleFluids.CERAMIC_WATER[stage], CrucibleFluids.CERAMIC_LAVA[stage]}) {
                helper.assertTrue(item instanceof CrucibleBucketItem bucket && bucket.stage() == stage && CrucibleFluids.buckets().contains(item),
                        "stage item " + item);
            }
        }
        // Scooping keeps the current stage and counter.
        ItemStack empty = new ItemStack(CrucibleFluids.CERAMIC_BUCKET);
        empty.set(CrucibleComponents.CERAMIC_USES, 0);
        ItemStack water = CrucibleBucketItem.fill(empty, CrucibleFluids.CERAMIC_WATER_BUCKET);
        helper.assertTrue(water.is(CrucibleFluids.CERAMIC_WATER[0]) && CrucibleBucketItem.ceramicUses(water) == 0, "scoop does not wear: " + water);
        // Pour advances exactly one stage and keeps the filling counter.
        ItemStack lava = new ItemStack(CrucibleFluids.CERAMIC_WATER[1]); // N35: lava buckets are brittle one-shots, so water here
        lava.set(CrucibleComponents.CERAMIC_USES, 1);
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack after = ((CrucibleBucketItem) lava.getItem()).pourAt(level, target, lava);
        helper.assertTrue(level.getFluidState(target).is(net.minecraft.tags.FluidTags.WATER), "chipped water bucket pours water");
        helper.assertTrue(after.is(CrucibleFluids.CERAMIC_EMPTY[2]) && CrucibleBucketItem.ceramicUses(after) == 2, "pour into stage 2: " + after);
        // The filling survives a stage change too.
        ItemStack held = new ItemStack(CrucibleFluids.CERAMIC_WATER[2]);
        held.set(CrucibleComponents.CERAMIC_USES, 2);
        ItemStack worn = CrucibleBucketItem.wear(held);
        helper.assertTrue(worn.is(CrucibleFluids.CERAMIC_WATER[3]) && CrucibleBucketItem.ceramicUses(worn) == 3, "wear keeps water: " + worn);
        // A fresh worn item without counter starts at its stage.
        helper.assertTrue(CrucibleBucketItem.ceramicUses(new ItemStack(CrucibleFluids.CERAMIC_EMPTY[3])) == 3, "stage 3 starts at 3 pours");
        // The brittle bucket breaks on its fourth pour.
        ItemStack brittle = new ItemStack(CrucibleFluids.CERAMIC_WATER[3]);
        brittle.set(CrucibleComponents.CERAMIC_USES, 3);
        helper.assertTrue(((CrucibleBucketItem) brittle.getItem()).pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), brittle).isEmpty(),
                "the brittle bucket survives its last pour");
        // Worn stages: not in the creative tab rows, but in the JEI info.
        helper.assertTrue(CrucibleFluids.wornCeramicBuckets().size() == 9, "9 worn items");
        helper.succeed();
    }

    /** Iron bucket breaks when pouring soul lava; the Enderite bucket never breaks (owner 34/36). */
    static void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        CrucibleBucketItem iron = (CrucibleBucketItem) CrucibleFluids.SOUL_LAVA_BUCKET;
        helper.assertTrue(iron.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(iron)).isEmpty(), "iron bucket breaks");
        helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).getType() == CrucibleFluids.SOUL_LAVA, "soul lava placed");
        CrucibleBucketItem enderite = (CrucibleBucketItem) CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET;
        helper.assertTrue(enderite.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(enderite)).is(CrucibleFluids.ENDERITE_BUCKET),
                "enderite bucket comes back");
        helper.assertTrue(CrucibleBucketItem.filled(CrucibleBucketItem.Kind.ENDERITE, CrucibleFluids.SOUL_LAVA) == CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET,
                "enderite bucket takes soul lava");
        helper.assertTrue(Items.BUCKET.getDefaultInstance().is(Items.BUCKET)
                && ((LiquidBlock) CrucibleFluids.SOUL_LAVA_BLOCK).pickupBlock(null, level, helper.absolutePos(new BlockPos(2, 1, 2)),
                        level.getBlockState(helper.absolutePos(new BlockPos(2, 1, 2)))).is(CrucibleFluids.SOUL_LAVA_BUCKET),
                "the vanilla bucket scoops soul lava into the soul lava bucket");
        helper.succeed();
    }

    /** The reinforced cauldron takes soul lava from the Enderite bucket and gives it back (owner 30/56). */
    static void reinforcedCauldronHoldsSoulLava(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, SimpleCruciblesApi.reinforcedCauldron("empty"));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue("extreme".equals(SimpleCruciblesApi.cauldronContent(level.getBlockState(abs))), "cauldron holds soul lava");
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.ENDERITE_BUCKET), "enderite bucket comes back empty");
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET), "scooped back");
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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrucibleFluids.ENDERITE_BUCKET));
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(CrucibleFluids.ENDERITE_LAVA_BUCKET), "enderite bucket took the lava");
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
        ItemStack worn = stack(CrucibleFluids.CERAMIC_WATER_BUCKET);
        if (!worn.isEmpty()) {
            worn.set(CrucibleComponents.CERAMIC_USES, 1);
        }
        for (ItemStack stack : List.of(worn, stack(CrucibleFluids.CERAMIC_BUCKET), stack(CrucibleFluids.COPPER_WATER_BUCKET),
                stack(CrucibleFluids.ENDERITE_WATER_BUCKET), stack(CrucibleFluids.SOUL_LAVA_BUCKET))) {
            if (!stack.isEmpty() && stack.isBarVisible()) {
                problems.add(stack.getItem() + " draws a durability bar");
            }
        }
        for (Item item : new Item[]{CrucibleFluids.ENDERITE_BUCKET, CrucibleFluids.ENDERITE_WATER_BUCKET, CrucibleFluids.FULL_ENDERITE_WATER_BUCKET,
                CrucibleFluids.ENDERITE_LAVA_BUCKET, CrucibleFluids.FULL_ENDERITE_LAVA_BUCKET, CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET,
                CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET, CrucibleFluids.ENDERITE_MILK_BUCKET, CrucibleFluids.FULL_ENDERITE_MILK_BUCKET}) {
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
        state.putString("Name", "simplebuilding:soul_lava");
        if (!net.minecraft.nbt.NbtUtils.readBlockState(helper.getLevel().holderLookup(Registries.BLOCK), state).is(CrucibleFluids.SOUL_LAVA_BLOCK)) problems.add("palette " + state);
        for (String path : SimpleCrucibles.BUCKET_RECIPES_FROM_SB) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.RECIPE, SimpleCrucibles.id(path));
            if (helper.getLevel().getServer().getRecipeManager().byKey(key).isEmpty()) problems.add("no recipe " + path);
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
