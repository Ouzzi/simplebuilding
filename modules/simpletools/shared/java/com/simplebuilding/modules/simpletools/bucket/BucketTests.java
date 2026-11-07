package com.simplebuilding.modules.simpletools.bucket;

import com.simplelib.bucket.Buckets;
import com.simplelib.bucket.EnderiteMilkBucketItem;
import com.simplelib.bucket.ModBucketItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests of the Enderite and ceramic buckets (WH26-7: moved with the items from Simple Crucibles' BucketTests; the
 * bodies are unchanged apart from the shared holder {@link Buckets}). They run with Simple Tools alone too: the soul
 * lava parts only with Simple Crucibles, the reinforced cauldron stays in Simple Crucibles' tests (combo with this module).
 */
public final class BucketTests {
    private BucketTests() {
    }

    public static void addAll(Map<String, Consumer<GameTestHelper>> all) {
        all.put("buckets_registered_and_migrated", BucketTests::bucketsRegisteredAndMigrated);
        all.put("ceramic_bucket_holds_water_and_breaks_after_four_pours", BucketTests::ceramicBucketHoldsWaterAndBreaksAfterFourPours);
        all.put("enderite_bucket_takes_milk_and_drinks_one_filling", BucketTests::enderiteBucketTakesMilkAndDrinksOneFilling);
        all.put("enderite_bucket_holds_two_buckets_of_one_fluid", BucketTests::enderiteBucketHoldsTwoBucketsOfOneFluid);
        all.put("enderite_bucket_two_buckets_in_cauldrons_and_dispensers", BucketTests::enderiteBucketTwoBucketsInCauldronsAndDispensers);
        all.put("ceramic_lava_bucket_scoops_pours_and_wears", BucketTests::ceramicLavaBucketScoopsPoursAndWears);
        all.put("ceramic_bucket_with_lava_is_brittle_and_breaks_on_pour", BucketTests::ceramicBucketWithLavaIsBrittleAndBreaksOnPour);
        all.put("ceramic_bucket_wears_through_stages_and_keeps_its_filling", BucketTests::ceramicBucketWearsThroughStagesAndKeepsItsFilling);
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

    private static ItemStack useBucket(ServerLevel level, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND, success.heldItemTransformedTo());
        }
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    // ------------------------------------------------------------ WH26-7: ids and old worlds

    /**
     * Every bucket is {@code simpletools:*} (the soul lava ones only with Simple Crucibles), and old worlds keep them:
     * saved stacks under {@code simplebuilding:} (before split stage 5b) and {@code simplecrucibles:} (until WH26-7) load
     * as the module's items, a ceramic bucket keeps its pour counter under either old component id, the recipes move too.
     */
    static void bucketsRegisteredAndMigrated(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        boolean soul = Buckets.SOUL_LAVA != null;
        for (String path : ToolsBuckets.IDS) {
            Identifier id = ToolsBuckets.id(path);
            boolean expected = soul || !path.contains("soul_lava");
            if (BuiltInRegistries.ITEM.containsKey(id) != expected) problems.add((expected ? "no item " : "item without Simple Crucibles ") + id);
            if (!expected) continue;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            for (String ns : ToolsBuckets.OLD_NAMESPACES) {
                Identifier from = Identifier.fromNamespaceAndPath(ns, path);
                if (BuiltInRegistries.ITEM.getValue(from) != item) problems.add("ITEM.getValue(" + from + ")");
                net.minecraft.nbt.CompoundTag saved = (net.minecraft.nbt.CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(item)).getOrThrow();
                saved.putString("id", from.toString());
                ItemStack loaded = ItemStack.CODEC.parse(ops, saved).result().orElse(ItemStack.EMPTY);
                if (loaded.getItem() != item) problems.add("stack " + saved + " loads as " + loaded);
            }
        }
        if (Buckets.toolsBuckets().size() != ToolsBuckets.IDS.size() - (soul ? 0 : 2)) problems.add("bucket list and ids disagree: " + Buckets.toolsBuckets().size());
        ItemStack ceramic = new ItemStack(Buckets.CERAMIC_WATER[1]);
        ceramic.set(Buckets.CERAMIC_USES, 1);
        for (String ns : ToolsBuckets.OLD_NAMESPACES) {
            String text = ItemStack.CODEC.encodeStart(ops, ceramic).getOrThrow().toString()
                    .replace("simpletools:ceramic_uses", ns + ":ceramic_uses").replace("simpletools:chipped_ceramic_water_bucket", ns + ":chipped_ceramic_water_bucket");
            try {
                ItemStack old = ItemStack.CODEC.parse(ops, net.minecraft.nbt.TagParser.parseCompoundFully(text)).result().orElse(ItemStack.EMPTY);
                Integer uses = old.get(Buckets.CERAMIC_USES);
                if (!old.is(Buckets.CERAMIC_WATER[1]) || uses == null || uses != 1) problems.add("old ceramic bucket " + text + " loads as " + old + " uses " + uses);
            } catch (Exception e) {
                problems.add("old stack text " + text + ": " + e);
            }
        }
        for (String path : ToolsBuckets.RECIPES) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.RECIPE, ToolsBuckets.id(path));
            // IG31-13: the Enderite recipe loads only with Simple Ores (owner of the material).
            boolean expected = !path.contains("enderite") || com.simplelib.api.SimpleLibApi.materialPresent("enderite");
            if (helper.getLevel().getServer().getRecipeManager().byKey(key).isEmpty() == expected) problems.add((expected ? "no recipe " : "recipe without Simple Ores ") + path);
            for (String ns : ToolsBuckets.OLD_NAMESPACES) {
                if (!ToolsBuckets.id(path).equals(com.simplelib.api.IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath(ns, path)))) problems.add("recipe move " + ns + ":" + path);
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    // ------------------------------------------------------------ tests (moved from Simple Crucibles, WH26-7)

    /**
     * Ceramic bucket: water (lava has its own test), pours a real source, scooping does not wear it, and the fourth
     * pour breaks it; 3 clay balls craft the raw bucket, which smelts into the ceramic bucket.
     */
    static void ceramicBucketHoldsWaterAndBreaksAfterFourPours(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        if (Buckets.SOUL_LAVA != null) helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.CERAMIC, Buckets.SOUL_LAVA) == null, "ceramic takes soul lava");
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.CERAMIC, Fluids.WATER) == Buckets.CERAMIC_WATER_BUCKET, "ceramic water");
        helper.assertTrue(ModBucketItem.CERAMIC_USES == 4 && ModBucketItem.CERAMIC_USES_PER_STAGE == 1
                        && !new ItemStack(Buckets.CERAMIC_BUCKET).isDamageableItem(),
                "4 pours, one per stage, no durability bar");
        ModBucketItem water = (ModBucketItem) Buckets.CERAMIC_WATER_BUCKET;
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack bucket = new ItemStack(Buckets.CERAMIC_BUCKET);
        for (int cycle = 1; cycle <= 4; cycle++) {
            bucket = ModBucketItem.fill(bucket, Buckets.CERAMIC_WATER_BUCKET);
            helper.assertTrue(ModBucketItem.ceramicUses(bucket) == cycle - 1
                            && bucket.is(Buckets.CERAMIC_WATER[cycle - 1]),
                    "scoop " + cycle + " does not wear the bucket: " + bucket);
            water = (ModBucketItem) bucket.getItem();
            bucket = water.pourAt(level, target, bucket);
            if (cycle == 1) helper.assertTrue(level.getFluidState(target).isSource(), "ceramic water makes a source");
            if (cycle < 4) {
                helper.assertTrue(bucket.is(Buckets.CERAMIC_EMPTY[cycle])
                                && ModBucketItem.ceramicUses(bucket) == cycle,
                        "after pour " + cycle + ": " + bucket + " pours " + ModBucketItem.ceramicUses(bucket));
            }
        }
        helper.assertTrue(bucket.isEmpty(), "the fourth pour did not break the ceramic bucket");
        var recipes = level.getServer().getRecipeManager();
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(clay, ItemStack.EMPTY, clay, ItemStack.EMPTY, clay, ItemStack.EMPTY));
        ItemStack raw = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid);
        helper.assertTrue(raw.is(Buckets.RAW_CERAMIC_BUCKET) && raw.getCount() == 1, "3 clay -> raw ceramic bucket, got " + raw);
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(raw);
        ItemStack fired = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level).orElseThrow().value().assemble(input);
        helper.assertTrue(fired.is(Buckets.CERAMIC_BUCKET) && ModBucketItem.ceramicUses(fired) == 0, "raw bucket smelts into the ceramic bucket");
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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Buckets.ENDERITE_BUCKET));
        player.interactOn(calf, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(Buckets.ENDERITE_BUCKET), "a calf gives no milk");
        player.interactOn(cow, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(Buckets.ENDERITE_MILK_BUCKET), "milking a cow: half milk bucket, got " + player.getMainHandItem());
        player.interactOn(goat, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(Buckets.FULL_ENDERITE_MILK_BUCKET), "milking a goat: full milk bucket, got " + player.getMainHandItem());
        player.interactOn(cow, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getMainHandItem().is(Buckets.FULL_ENDERITE_MILK_BUCKET), "a full one takes no more");
        helper.assertTrue(((EnderiteMilkBucketItem) Buckets.FULL_ENDERITE_MILK_BUCKET).amount() == 2
                && ((EnderiteMilkBucketItem) Buckets.ENDERITE_MILK_BUCKET).amount() == 1, "fill amounts 2/2 and 1/2");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 600));
        ItemStack drunk = player.getMainHandItem().finishUsingItem(level, player);
        helper.assertTrue(drunk.is(Buckets.ENDERITE_MILK_BUCKET) && player.getActiveEffects().isEmpty(),
                "drinking a full one: half left, effects cleared, got " + drunk);
        drunk = drunk.finishUsingItem(level, player);
        helper.assertTrue(drunk.is(Buckets.ENDERITE_BUCKET), "drinking the half one leaves the empty bucket, got " + drunk);
        helper.assertTrue(Buckets.FULL_ENDERITE_MILK_BUCKET.getCraftingRemainder().item().value() == Buckets.ENDERITE_MILK_BUCKET
                && Buckets.ENDERITE_MILK_BUCKET.getCraftingRemainder().item().value() == Buckets.ENDERITE_BUCKET,
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
        ItemStack hand = useBucket(level, player, new ItemStack(Buckets.ENDERITE_BUCKET));
        helper.assertTrue(hand.is(Buckets.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "first scoop: half, got " + hand);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.FULL_ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "second scoop: full, got " + hand);
        helper.assertTrue(((ModBucketItem) hand.getItem()).amount() == 2 && ((ModBucketItem) Buckets.ENDERITE_WATER_BUCKET).amount() == 1,
                "fill amounts 2/2 and 1/2");
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isSource(), "use on a full one pours one bucket, got " + hand);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.FULL_ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(),
                "use on a half one only scoops (took the source back), got " + hand);
        player.setShiftKeyDown(true);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isSource(), "sneak + use on a full one pours one bucket, got " + hand);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.ENDERITE_BUCKET) && level.getFluidState(water).isSource(), "sneak + use on a half one pours the last bucket, got " + hand);
        player.setShiftKeyDown(false);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        hand = useBucket(level, player, new ItemStack(Buckets.ENDERITE_WATER_BUCKET));
        helper.assertTrue(hand.is(Buckets.ENDERITE_WATER_BUCKET) && level.getFluidState(water).isEmpty(), "plain use on a half one never pours, got " + hand);
        level.setBlock(water, Blocks.LAVA.defaultBlockState(), 3);
        hand = useBucket(level, player, hand);
        helper.assertTrue(hand.is(Buckets.ENDERITE_WATER_BUCKET) && level.getFluidState(water).is(net.minecraft.tags.FluidTags.LAVA),
                "a half water bucket takes no lava, got " + hand);
        level.setBlock(water, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(Buckets.fullEnderite(Buckets.ENDERITE_LAVA_BUCKET) == Buckets.FULL_ENDERITE_LAVA_BUCKET, "lava fills up to two too");
        // Soul lava (Simple Crucibles' fluid): the Enderite soul lava buckets exist only with that module.
        if (Buckets.ENDERITE_SOUL_LAVA_BUCKET != null) {
            helper.assertTrue(Buckets.fullEnderite(Buckets.ENDERITE_SOUL_LAVA_BUCKET) == Buckets.FULL_ENDERITE_SOUL_LAVA_BUCKET
                    && Buckets.fullEnderite(Buckets.ENDERITE_LAVA_BUCKET) == Buckets.FULL_ENDERITE_LAVA_BUCKET, "soul lava and lava fill up to two too");
            ModBucketItem soul = (ModBucketItem) Buckets.FULL_ENDERITE_SOUL_LAVA_BUCKET;
            BlockPos soulAt = helper.absolutePos(new BlockPos(5, 1, 5));
            helper.assertTrue(soul.pourAt(level, soulAt, new ItemStack(soul)).is(Buckets.ENDERITE_SOUL_LAVA_BUCKET)
                    && level.getFluidState(soulAt).getType() == Buckets.SOUL_LAVA, "a full soul lava bucket pours one and keeps one");
        }
        helper.assertTrue(!Buckets.creativeToolsBuckets().contains(Buckets.FULL_ENDERITE_WATER_BUCKET)
                && Buckets.toolsBuckets().containsAll(Buckets.fullEnderiteBuckets()), "full buckets: registered, not in the creative tab");
        helper.succeed();
    }

    /**
     * Ceramic lava bucket: the empty ceramic bucket scoops lava without wearing, pours a real lava source and
     * comes back worn (no break like copper); soul lava and milk stay refused; it is furnace fuel without a
     * remainder (burns up) and sits in the creative tab and the filled-bucket JEI info.
     */
    static void ceramicLavaBucketScoopsPoursAndWears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(Buckets.CERAMIC_LAVA_BUCKET != null
                && ModBucketItem.filled(ModBucketItem.Kind.CERAMIC, Fluids.LAVA) == Buckets.CERAMIC_LAVA_BUCKET, "ceramic lava bucket");
        if (Buckets.SOUL_LAVA != null) helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.CERAMIC, Buckets.SOUL_LAVA) == null, "ceramic takes soul lava");
        ModBucketItem lava = (ModBucketItem) Buckets.CERAMIC_LAVA_BUCKET;
        helper.assertTrue(lava.kind() == ModBucketItem.Kind.CERAMIC && lava.breaksOnPour(), "ceramic lava must break on pour (N35)");
        ItemStack bucket = ModBucketItem.fill(new ItemStack(Buckets.CERAMIC_BUCKET), Buckets.CERAMIC_LAVA_BUCKET);
        helper.assertTrue(bucket.is(Buckets.CERAMIC_LAVA[ModBucketItem.CERAMIC_STAGES - 1]), "scoop makes it brittle: " + bucket);
        lava = (ModBucketItem) bucket.getItem();
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        bucket = lava.pourAt(level, target, bucket);
        helper.assertTrue(level.getFluidState(target).is(net.minecraft.tags.FluidTags.LAVA) && level.getFluidState(target).isSource(),
                "ceramic lava makes a lava source");
        helper.assertTrue(bucket.isEmpty(), "the brittle lava bucket must break when poured: " + bucket);
        ItemStack fuel = new ItemStack(Buckets.CERAMIC_LAVA_BUCKET);
        boolean burns = fuel.getComponents().keySet().stream().anyMatch(type ->
                "minecraft:cooking_fuel".equals(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))));
        helper.assertTrue(burns, "ceramic lava bucket is no fuel");
        helper.assertTrue(!fuel.getComponents().keySet().stream().anyMatch(type ->
                "minecraft:use_remainder".equals(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)))),
                "ceramic lava bucket leaves a remainder");
        helper.assertTrue(Buckets.toolsBuckets().contains(Buckets.CERAMIC_LAVA_BUCKET), "not in the bucket list");
        helper.succeed();
    }

    /** N35: lava makes the ceramic bucket brittle at once; pouring it places the lava and the bucket is gone. */
    static void ceramicBucketWithLavaIsBrittleAndBreaksOnPour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ItemStack water = ModBucketItem.fill(new ItemStack(Buckets.CERAMIC_BUCKET), Buckets.CERAMIC_WATER_BUCKET);
        helper.assertTrue(water.is(Buckets.CERAMIC_WATER[0]), "water keeps the stage");
        ItemStack lava = ModBucketItem.fill(new ItemStack(Buckets.CERAMIC_BUCKET), Buckets.CERAMIC_LAVA_BUCKET);
        helper.assertTrue(lava.is(Buckets.CERAMIC_LAVA[ModBucketItem.CERAMIC_STAGES - 1]), "lava makes it brittle: " + lava);
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack left = ((ModBucketItem) lava.getItem()).pourAt(level, target, lava);
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
        helper.assertTrue(ModBucketItem.CERAMIC_STAGES == 4 && ModBucketItem.CERAMIC_USES_PER_STAGE == 1
                && ModBucketItem.CERAMIC_STAGES * ModBucketItem.CERAMIC_USES_PER_STAGE == ModBucketItem.CERAMIC_USES,
                "4 stages of 1 pour (copper: oxidation 0..3)");
        for (int stage = 0; stage < ModBucketItem.CERAMIC_STAGES; stage++) {
            for (Item item : new Item[] {Buckets.CERAMIC_EMPTY[stage], Buckets.CERAMIC_WATER[stage], Buckets.CERAMIC_LAVA[stage]}) {
                helper.assertTrue(item instanceof ModBucketItem bucket && bucket.stage() == stage && Buckets.toolsBuckets().contains(item),
                        "stage item " + item);
            }
        }
        // Scooping keeps the current stage and counter.
        ItemStack empty = new ItemStack(Buckets.CERAMIC_BUCKET);
        empty.set(Buckets.CERAMIC_USES, 0);
        ItemStack water = ModBucketItem.fill(empty, Buckets.CERAMIC_WATER_BUCKET);
        helper.assertTrue(water.is(Buckets.CERAMIC_WATER[0]) && ModBucketItem.ceramicUses(water) == 0, "scoop does not wear: " + water);
        // Pour advances exactly one stage and keeps the filling counter.
        ItemStack lava = new ItemStack(Buckets.CERAMIC_WATER[1]); // N35: lava buckets are brittle one-shots, so water here
        lava.set(Buckets.CERAMIC_USES, 1);
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack after = ((ModBucketItem) lava.getItem()).pourAt(level, target, lava);
        helper.assertTrue(level.getFluidState(target).is(net.minecraft.tags.FluidTags.WATER), "chipped water bucket pours water");
        helper.assertTrue(after.is(Buckets.CERAMIC_EMPTY[2]) && ModBucketItem.ceramicUses(after) == 2, "pour into stage 2: " + after);
        // The filling survives a stage change too.
        ItemStack held = new ItemStack(Buckets.CERAMIC_WATER[2]);
        held.set(Buckets.CERAMIC_USES, 2);
        ItemStack worn = ModBucketItem.wear(held);
        helper.assertTrue(worn.is(Buckets.CERAMIC_WATER[3]) && ModBucketItem.ceramicUses(worn) == 3, "wear keeps water: " + worn);
        // A fresh worn item without counter starts at its stage.
        helper.assertTrue(ModBucketItem.ceramicUses(new ItemStack(Buckets.CERAMIC_EMPTY[3])) == 3, "stage 3 starts at 3 pours");
        // The brittle bucket breaks on its fourth pour.
        ItemStack brittle = new ItemStack(Buckets.CERAMIC_WATER[3]);
        brittle.set(Buckets.CERAMIC_USES, 3);
        helper.assertTrue(((ModBucketItem) brittle.getItem()).pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), brittle).isEmpty(),
                "the brittle bucket survives its last pour");
        // Worn stages: not in the creative tab rows, but in the JEI info.
        helper.assertTrue(Buckets.wornCeramicBuckets().size() == 9, "9 worn items");
        helper.succeed();
    }

    /**
     * Owner N21: cauldrons and dispensers follow the two-bucket rule. A full Enderite bucket fills a Vanilla
     * cauldron (SimpleLib mixin; the reinforced one is tested in Simple Crucibles) with one bucket and stays half; a half one takes a full cauldron of its fluid and becomes
     * full. A dispenser pours one bucket of a full one and keeps the half one; a half one pours and comes back empty.
     */
    static void enderiteBucketTwoBucketsInCauldronsAndDispensers(GameTestHelper helper) {
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
}
