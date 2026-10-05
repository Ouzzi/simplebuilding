package com.simplesandwiches.test;

import com.google.gson.JsonObject;
import com.simplesandwiches.block.CuttingBoardBlock;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import com.simplesandwiches.block.CuttingBoardBlockEntity.Stage;
import com.simplesandwiches.block.MilkCauldronBlock;
import com.simplesandwiches.block.MilkCauldronBlock.Content;
import com.simplesandwiches.block.SliceBlock;
import com.simplesandwiches.config.SandwichConfig;
import com.simplesandwiches.item.BundleEating;
import com.simplesandwiches.item.KnifeItem;
import com.simplesandwiches.registry.ModBlocks;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.ModTags;
import com.simplesandwiches.sandwich.EffectMerger;
import com.simplesandwiches.sandwich.SandwichContents;
import com.simplesandwiches.sandwich.SandwichFormula;
import com.simplesandwiches.sandwich.SandwichItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests of Simple Sandwiches. One body per id; the Fabric adapter ({@code ModuleGameTest}) and the
 * NeoForge/Forge registrations use the same names ({@code module_game_test_<name>}).
 */
public final class SandwichTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    /** Ids that need more than the default 100 ticks. */
    public static final Map<String, Integer> MAX_TICKS = Map.of("cauldron_ripens_in_world", 400);

    static {
        ALL.put("config_bounds", SandwichTests::configBounds);
        ALL.put("guide_book", com.simplesandwiches.guide.SandwichGuide::gameTest);
        ALL.put("ingredient_tag_complete", SandwichTests::ingredientTagComplete);
        ALL.put("hunger_sum_without_cap", SandwichTests::hungerSum);
        ALL.put("butter_bonus", SandwichTests::butterBonus);
        ALL.put("effect_merge", SandwichTests::effectMerge);
        ALL.put("deterministic_stacking", SandwichTests::deterministicStacking);
        ALL.put("board_full_cycle", SandwichTests::boardFullCycle);
        ALL.put("board_butter_only_first", SandwichTests::boardButterOnlyFirst);
        ALL.put("board_break_drops", SandwichTests::boardBreakDrops);
        ALL.put("board_priority_over_eating", SandwichTests::boardPriorityOverEating);
        ALL.put("knife_cake_slices", SandwichTests::knifeCakeSlices);
        ALL.put("knife_melon_and_tools", SandwichTests::knifeMelonAndTools);
        ALL.put("knife_recipe_shape", SandwichTests::knifeRecipeShape);
        ALL.put("slice_block_cutting", SandwichTests::sliceBlockCutting);
        ALL.put("slice_block_keeps_state", SandwichTests::sliceBlockKeepsState);
        ALL.put("cheese_bounces_like_bed", SandwichTests::cheeseBouncesLikeBed);
        ALL.put("cut_sounds_registered", SandwichTests::cutSoundsRegistered);
        ALL.put("cauldron_butter", SandwichTests::cauldronButter);
        ALL.put("cauldron_cheese_spoils", SandwichTests::cauldronCheeseSpoils);
        ALL.put("cauldron_ripens_in_world", SandwichTests::cauldronRipensInWorld);
        ALL.put("bundle_eating", SandwichTests::bundleEating);
        ALL.put("cake_slice", SandwichTests::cakeSlice);
    }

    // --- helpers -----------------------------------------------------------------------------------

    private static ServerPlayer player(GameTestHelper h, BlockPos rel) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = h.absolutePos(rel);
        p.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 1.5);
        p.getFoodData().setFoodLevel(4);
        return p;
    }

    /** Vanilla's right-click order for the main hand: block with item, block without item, item. */
    private static InteractionResult click(GameTestHelper h, ServerPlayer p, BlockPos rel, Direction face) {
        BlockPos abs = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
        ItemStack main = p.getMainHandItem();
        BlockState state = h.getLevel().getBlockState(abs);
        InteractionResult r = state.useItemOn(main, h.getLevel(), p, InteractionHand.MAIN_HAND, hit);
        if (r.consumesAction()) return r;
        if (r instanceof InteractionResult.TryEmptyHandInteraction) {
            InteractionResult r2 = state.useWithoutItem(h.getLevel(), p, hit);
            if (r2.consumesAction()) return r2;
        }
        if (!main.isEmpty()) return main.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        return InteractionResult.PASS;
    }

    private static InteractionResult click(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        return click(h, p, rel, Direction.UP);
    }

    private static SandwichContents contents(boolean buttered, Item... items) {
        List<Holder<Item>> list = new ArrayList<>();
        for (Item i : items) list.add(i.builtInRegistryHolder());
        return new SandwichContents(list, buttered);
    }

    private static MobEffectInstance effect(ItemStack sandwich, Holder<MobEffect> which, float[] probability) {
        Consumable c = sandwich.get(DataComponents.CONSUMABLE);
        for (ConsumeEffect e : c.onConsumeEffects()) {
            if (e instanceof ApplyStatusEffectsConsumeEffect a) {
                for (MobEffectInstance i : a.effects()) {
                    if (i.getEffect().equals(which)) {
                        probability[0] = a.probability();
                        return i;
                    }
                }
            }
        }
        return null;
    }

    private static void near(GameTestHelper h, double actual, double expected, String what) {
        h.assertTrue(Math.abs(actual - expected) < 1.0E-3, what + ": expected " + expected + ", got " + actual);
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    // --- tests -------------------------------------------------------------------------------------

    static void configBounds(GameTestHelper h) {
        // Tests share one server: restore the previous values instead of the defaults.
        JsonObject saved = SandwichConfig.toJson();
        try {
            JsonObject j = new JsonObject();
            j.addProperty("maxIngredients", 99);
            j.addProperty("butterBonus", 5.0);
            j.addProperty("effectDurationCapTicks", 1_000_000);
            j.addProperty("effectDurationMultiplierCap", -3);
            j.addProperty("butterTicks", 1);
            j.addProperty("cheeseTicks", Integer.MAX_VALUE);
            j.addProperty("cheeseHarvestWindowTicks", -5);
            j.addProperty("bundleEating", false);
            SandwichConfig.apply(j);
            h.assertTrue(SandwichConfig.maxIngredients == 5, "maxIngredients clamped to 5");
            near(h, SandwichConfig.butterBonus, 0.25, "butterBonus clamped");
            h.assertTrue(SandwichConfig.effectDurationCapTicks == 12_000, "effect cap clamped to 10 min");
            near(h, SandwichConfig.effectDurationMultiplierCap, 1.0, "multiplier clamped");
            h.assertTrue(SandwichConfig.butterTicks == 200, "butterTicks clamped");
            h.assertTrue(SandwichConfig.cheeseTicks == 72_000, "cheeseTicks clamped");
            h.assertTrue(SandwichConfig.cheeseHarvestWindowTicks == 200, "harvest window clamped");
            h.assertTrue(!SandwichConfig.bundleEating, "bundleEating switch read");
            j = new JsonObject();
            j.addProperty("butterBonus", "NaN");
            SandwichConfig.apply(j);
            near(h, SandwichConfig.butterBonus, SandwichConfig.BUTTER_BONUS_DEFAULT, "NaN falls back to the default");
        } finally {
            SandwichConfig.apply(saved);
        }
        h.succeed();
    }

    static void ingredientTagComplete(GameTestHelper h) {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack s = new ItemStack(item);
            var key = BuiltInRegistries.ITEM.getKey(item);
            boolean vanillaFood = key.getNamespace().equals("minecraft") && s.has(DataComponents.FOOD)
                    && !s.has(DataComponents.USE_REMAINDER) && item != Items.BREAD
                    && !(item instanceof net.minecraft.world.item.BucketItem);
            if (vanillaFood) h.assertTrue(s.is(ModTags.SANDWICH_INGREDIENTS), "Vanilla food missing from the ingredient tag: " + key);
            if (s.is(ModTags.SANDWICH_INGREDIENTS)) h.assertTrue(SandwichFormula.isIngredient(s), "Tag member is no valid ingredient: " + key);
        }
        h.assertTrue(SandwichFormula.isIngredient(new ItemStack(ModItems.CHEESE_SLICE)), "cheese slice is an ingredient");
        h.assertTrue(SandwichFormula.isIngredient(new ItemStack(ModItems.CAKE_SLICE)), "cake slice is an ingredient");
        for (Item no : List.of(Items.MUSHROOM_STEW, Items.HONEY_BOTTLE, Items.MILK_BUCKET, Items.BREAD, Items.CAKE, ModItems.BUTTER_SLICE)) {
            h.assertTrue(!SandwichFormula.isIngredient(new ItemStack(no)), "Not an ingredient: " + no);
        }
        h.succeed();
    }

    static void hungerSum(GameTestHelper h) {
        FoodProperties f = SandwichFormula.create(contents(false, Items.COOKED_BEEF, ModItems.CHEESE_SLICE, Items.CARROT)).get(DataComponents.FOOD);
        h.assertTrue(f.nutrition() == 5 + 8 + 2 + 3, "bread + steak + cheese + carrot = 18, got " + f.nutrition());
        near(h, f.saturation(), 6.0 + 12.8 + 1.2 + 3.6, "saturation sum");
        FoodProperties five = SandwichFormula.create(contents(false, Items.COOKED_BEEF, Items.COOKED_BEEF, Items.COOKED_BEEF,
                Items.COOKED_BEEF, Items.COOKED_BEEF)).get(DataComponents.FOOD);
        h.assertTrue(five.nutrition() == 45, "no cap (owner F4 = C): 5 + 5 x 8 = 45, got " + five.nutrition());
        h.assertTrue(!five.canAlwaysEat(), "steak cannot always be eaten");
        h.assertTrue(SandwichFormula.create(contents(false, Items.GOLDEN_APPLE)).get(DataComponents.FOOD).canAlwaysEat(),
                "golden apple makes it always edible");
        Consumable c = SandwichFormula.create(contents(false, Items.APPLE, Items.APPLE, Items.APPLE)).get(DataComponents.CONSUMABLE);
        near(h, c.consumeSeconds(), 1.6 + 0.6, "eat time 1.6 s + 0.2 s per ingredient");
        h.succeed();
    }

    static void butterBonus(GameTestHelper h) {
        FoodProperties f = SandwichFormula.create(contents(true, Items.COOKED_BEEF)).get(DataComponents.FOOD);
        h.assertTrue(f.nutrition() == Math.round((5 + 8) * 1.1), "butter +10 % nutrition, got " + f.nutrition());
        near(h, f.saturation(), (6.0 + 12.8) * 1.1, "butter +10 % saturation");
        float[] p = new float[1];
        MobEffectInstance hunger = effect(SandwichFormula.create(contents(true, Items.CHICKEN)), MobEffects.HUNGER, p);
        h.assertTrue(hunger != null && hunger.getDuration() == 660, "butter +10 % effect duration (600 -> 660)");
        h.succeed();
    }

    static void effectMerge(GameTestHelper h) {
        float[] p = new float[1];
        MobEffectInstance two = effect(SandwichFormula.create(contents(false, Items.CHICKEN, Items.CHICKEN)), MobEffects.HUNGER, p);
        h.assertTrue(two != null && two.getDuration() == 1200 && two.getAmplifier() == 0, "2x raw chicken: hunger 60 s, level I");
        near(h, p[0], 1 - 0.7 * 0.7, "2x 30 % -> 51 %");
        MobEffectInstance three = effect(SandwichFormula.create(contents(false, Items.CHICKEN, Items.CHICKEN, Items.CHICKEN)), MobEffects.HUNGER, p);
        h.assertTrue(three.getDuration() == 1200, "duration capped at twice the longest single duration");
        effect(SandwichFormula.create(contents(false, Items.ROTTEN_FLESH, Items.CHICKEN)), MobEffects.HUNGER, p);
        near(h, p[0], 1 - 0.2 * 0.7, "80 % and 30 % -> 86 %");
        ItemStack apples = SandwichFormula.create(contents(false, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE));
        MobEffectInstance absorption = effect(apples, MobEffects.ABSORPTION, p);
        h.assertTrue(absorption.getAmplifier() == 3, "strength is the maximum (absorption IV), got " + absorption.getAmplifier());
        h.assertTrue(absorption.getDuration() == 4800, "absorption 2 min + 2 min = 4 min, got " + absorption.getDuration());
        MobEffectInstance regen = effect(apples, MobEffects.REGENERATION, p);
        h.assertTrue(regen.getAmplifier() == 1 && regen.getDuration() == 500, "regeneration II 5 s + 20 s = 25 s, got " + regen.getDuration());
        h.assertTrue(apples.get(DataComponents.RARITY) == Items.ENCHANTED_GOLDEN_APPLE.components().get(DataComponents.RARITY), "highest rarity");
        h.assertTrue(Boolean.TRUE.equals(apples.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "glint from the enchanted apple");
        h.assertTrue(EffectMerger.cappedDuration(30_000, 20_000, 1.0, false) == 12_000, "absolute 10 min cap");
        h.succeed();
    }

    static void deterministicStacking(GameTestHelper h) {
        ItemStack a = SandwichFormula.create(contents(false, Items.COOKED_BEEF, ModItems.CHEESE_SLICE));
        ItemStack b = SandwichFormula.create(contents(false, Items.COOKED_BEEF, ModItems.CHEESE_SLICE));
        h.assertTrue(ItemStack.isSameItemSameComponents(a, b), "equal contents stack");
        h.assertTrue(!ItemStack.isSameItemSameComponents(a, SandwichFormula.create(contents(false, ModItems.CHEESE_SLICE, Items.COOKED_BEEF))), "other order does not stack");
        h.assertTrue(!ItemStack.isSameItemSameComponents(a, SandwichFormula.create(contents(true, Items.COOKED_BEEF, ModItems.CHEESE_SLICE))), "butter does not stack with plain");
        h.assertTrue(a.getMaxStackSize() == ModItems.SANDWICH_STACK, "sandwiches stack to 16");
        h.assertTrue(a.getHoverName().getString().length() > 0, "named");
        h.succeed();
    }

    private static CuttingBoardBlockEntity board(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, ModBlocks.CUTTING_BOARDS.get("oak"));
        return (CuttingBoardBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    static void boardFullCycle(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        CuttingBoardBlockEntity board = board(h, pos);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));
        click(h, p, pos);
        h.assertTrue(board.stage() == Stage.LOAF && p.getMainHandItem().getCount() == 1, "bread laid on the board");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        h.assertTrue(board.stage() == Stage.OPEN, "knife opens the loaf");
        h.assertTrue(p.getMainHandItem().getDamageValue() == 1, "opening costs 1 durability");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.BUTTER_SLICE, 2));
        click(h, p, pos);
        h.assertTrue(board.buttered() && p.getOffhandItem().getCount() == 1, "knife + butter slice butters the bread");
        h.assertTrue(p.isUsingItem() && p.getUseItem().getItem() instanceof KnifeItem
                && p.getUseItem().getUseAnimation() == net.minecraft.world.item.ItemUseAnimation.BRUSH, "spreading plays the wiping (brush) motion");
        p.stopUsingItem();
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        Item[] layers = {Items.COOKED_BEEF, ModItems.CHEESE_SLICE, Items.CARROT, Items.APPLE, Items.COOKED_CHICKEN};
        for (Item layer : layers) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(layer, 2));
            click(h, p, pos);
        }
        h.assertTrue(board.ingredients().size() == 5, "five ingredients");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BAKED_POTATO));
        click(h, p, pos);
        h.assertTrue(board.ingredients().size() == 5 && p.getMainHandItem().getCount() == 1, "sixth ingredient refused");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        p.getInventory().setItem(8, ItemStack.EMPTY);
        click(h, p, pos);
        h.assertTrue(board.ingredients().size() == 4 && count(p, Items.COOKED_CHICKEN) == 1, "knife takes the top ingredient back (LIFO)");
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        click(h, p, pos);
        h.assertTrue(board.stage() == Stage.CLOSED, "empty hand closes");
        click(h, p, pos);
        ItemStack sandwich = p.getMainHandItem();
        h.assertTrue(board.stage() == Stage.EMPTY && sandwich.getItem() instanceof SandwichItem, "empty hand takes the sandwich");
        h.assertTrue(ItemStack.isSameItemSameComponents(sandwich,
                SandwichFormula.create(contents(true, Items.COOKED_BEEF, ModItems.CHEESE_SLICE, Items.CARROT, Items.APPLE))), "board result matches the formula");
        click(h, p, pos);
        h.assertTrue(board.stage() == Stage.CLOSED && p.getMainHandItem().isEmpty(), "a sandwich can be put back");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        h.assertTrue(board.stage() == Stage.OPEN && board.ingredients().size() == 4 && board.buttered(), "knife reopens with contents");
        h.succeed();
    }

    /**
     * Owner bug 2026-10-05: a hungry player ate the bread instead of laying it on the board. Goes
     * through the real server path ({@code ServerPlayerGameMode.useItemOn}, then {@code useItem}
     * only when the block did not consume the click, as the client does).
     */
    static void boardPriorityOverEating(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        CuttingBoardBlockEntity board = board(h, pos);
        ServerPlayer p = player(h, pos);
        h.assertTrue(p.getFoodData().needsFood(), "player is hungry");
        BlockPos abs = h.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(new Vec3(abs.getX() + 0.5, abs.getY() + 2.0 / 16.0, abs.getZ() + 0.5), Direction.UP, abs, false);
        java.util.function.Supplier<InteractionResult> rightClick = () -> {
            InteractionResult r = p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            if (!r.consumesAction() && !p.getMainHandItem().isEmpty()) r = p.gameMode.useItem(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            return r;
        };
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));
        rightClick.get();
        h.assertTrue(board.stage() == Stage.LOAF && p.getMainHandItem().getCount() == 2, "hungry player lays the bread on the board");
        h.assertTrue(!p.isUsingItem(), "no eating started");
        rightClick.get();
        h.assertTrue(board.stage() == Stage.LOAF && p.getMainHandItem().getCount() == 2 && !p.isUsingItem(),
                "second bread on a loaded board: board keeps the click, nothing eaten");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        rightClick.get();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 2));
        rightClick.get();
        h.assertTrue(board.ingredients().size() == 1 && !p.isUsingItem(), "ingredient laid, not eaten");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        rightClick.get();
        rightClick.get();
        ItemStack sandwich = p.getMainHandItem().copy();
        h.assertTrue(sandwich.getItem() instanceof SandwichItem, "sandwich taken");
        rightClick.get();
        h.assertTrue(board.stage() == Stage.CLOSED && !p.isUsingItem(), "sandwich put back, not eaten");
        for (ItemStack s : List.of(new ItemStack(Items.BREAD), sandwich, new ItemStack(Items.APPLE), new ItemStack(ModItems.CHEESE_SLICE),
                new ItemStack(ModItems.BUTTER_SLICE), new ItemStack(ModItems.KNIFE))) {
            h.assertTrue(CuttingBoardBlock.claims(s), "client keeps the click on the board for " + s);
        }
        h.assertTrue(!CuttingBoardBlock.claims(new ItemStack(Items.STONE)) && !CuttingBoardBlock.claims(new ItemStack(Items.MUSHROOM_STEW)),
                "other items fall through");
        h.succeed();
    }

    static void boardButterOnlyFirst(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        CuttingBoardBlockEntity board = board(h, pos);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        click(h, p, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_PORKCHOP).copy());
        p.getMainHandItem().set(DataComponents.CUSTOM_NAME, Component.literal("Golden Pork"));
        click(h, p, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BUTTER_SLICE));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        h.assertTrue(!board.buttered() && p.getMainHandItem().getCount() == 1, "butter refused once ingredients lie on the bread");
        h.assertTrue(CuttingBoardBlock.plan(board, new ItemStack(Items.STONE), ItemStack.EMPTY) == CuttingBoardBlock.Action.NONE, "other items do nothing");
        h.assertTrue(ItemStack.isSameItemSameComponents(board.sandwich(), SandwichFormula.create(contents(false, Items.COOKED_PORKCHOP))),
                "a renamed ingredient changes nothing");
        h.succeed();
    }

    static void boardBreakDrops(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        CuttingBoardBlockEntity board = board(h, pos);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        click(h, p, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        click(h, p, pos);
        h.assertTrue(board.ingredients().size() == 1, "apple on the board");
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        h.assertItemEntityPresent(Items.BREAD, pos, 2.0);
        h.assertItemEntityPresent(Items.APPLE, pos, 2.0);
        h.assertItemEntityPresent(ModItems.CUTTING_BOARDS.get("oak"), pos, 2.0);
        h.succeed();
    }

    static void knifeCakeSlices(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, Blocks.CAKE);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        for (int i = 0; i < CakeBlock.MAX_BITES + 1; i++) click(h, p, pos);
        h.assertTrue(count(p, ModItems.CAKE_SLICE) == 7, "a whole cake gives 7 slices, got " + count(p, ModItems.CAKE_SLICE));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(), "cake gone after the last slice");
        h.assertTrue(p.getFoodData().getFoodLevel() == 4, "cutting does not eat");
        h.setBlock(pos, Blocks.CANDLE_CAKE);
        click(h, p, pos);
        BlockState after = h.getLevel().getBlockState(h.absolutePos(pos));
        h.assertTrue(after.is(Blocks.CAKE) && after.getValue(CakeBlock.BITES) == 1, "candle cake becomes a cut cake");
        h.assertItemEntityPresent(Items.CANDLE, pos, 2.0);
        h.succeed();
    }

    static void knifeRecipeShape(GameTestHelper h) {
        var level = h.getLevel();
        var recipes = level.getServer().getRecipeManager();
        var empty = ItemStack.EMPTY;
        var nugget = new ItemStack(Items.IRON_NUGGET);
        var stick = new ItemStack(Items.STICK);
        for (var slots : List.of(List.of(empty, empty, nugget, empty, nugget, nugget, stick, nugget, empty),
                List.of(nugget, empty, empty, nugget, nugget, empty, empty, nugget, stick))) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, slots);
            var output = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                    .orElseThrow().value().assemble(input);
            h.assertTrue(output.is(ModItems.KNIFE) && output.getCount() == 1, "four nuggets and one stick craft one knife, also mirrored");
        }
        var old = net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                List.of(empty, empty, nugget, empty, nugget, empty, stick, empty, empty));
        h.assertTrue(recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, old, level).isEmpty(),
                "old two-nugget pattern must not craft a knife");
        h.succeed();
    }

    static void knifeMelonAndTools(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, Blocks.MELON);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(), "melon cut up");
        h.assertItemEntityCountIs(Items.MELON_SLICE, pos, 2.0, KnifeItem.MELON_SLICES);
        ItemStack knife = new ItemStack(ModItems.KNIFE);
        Tool tool = knife.get(DataComponents.TOOL);
        near(h, tool.getMiningSpeed(Blocks.COBWEB.defaultBlockState()), 15.0, "cobweb speed like a sword");
        h.assertTrue(tool.isCorrectForDrops(Blocks.COBWEB.defaultBlockState()), "cobweb drops string");
        h.assertTrue(tool.getMiningSpeed(Blocks.BAMBOO.defaultBlockState()) > 1000, "bamboo instantly");
        double damage = 1.0;
        ItemAttributeModifiers mods = knife.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (var e : mods.modifiers()) if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.slot() == EquipmentSlotGroup.MAINHAND) damage += e.modifier().amount();
        near(h, damage, 2.0, "attack damage 2 (a third of the iron sword)");
        h.assertTrue(knife.getMaxDamage() == 250, "iron durability");
        h.assertTrue(knife.isValidRepairItem(new ItemStack(Items.IRON_NUGGET)) && !knife.isValidRepairItem(new ItemStack(Items.IRON_INGOT)),
                "repaired with iron nuggets");
        h.assertTrue(knife.is(ItemTags.MINING_ENCHANTABLE) && knife.is(ItemTags.DURABILITY_ENCHANTABLE) && !knife.is(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                "efficiency, unbreaking, mending only");
        h.succeed();
    }

    static void sliceBlockCutting(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.CHEESE_BLOCK);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        for (int i = 0; i < 3; i++) click(h, p, pos, Direction.UP);
        BlockState s = h.getLevel().getBlockState(h.absolutePos(pos));
        h.assertTrue(s.getValue(SliceBlock.SLICES) == 13 && s.getValue(SliceBlock.CUT) == SliceBlock.Cut.UP, "three slices from the top");
        click(h, p, pos, Direction.NORTH);
        s = h.getLevel().getBlockState(h.absolutePos(pos));
        h.assertTrue(s.getValue(SliceBlock.SLICES) == 13 && s.getValue(SliceBlock.CUT) == SliceBlock.Cut.UP, "side cut refused once cut from the top");
        h.assertTrue(count(p, ModItems.CHEESE_SLICE) == 3, "three cheese slices");
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(h.absolutePos(pos)), h.getLevel(), h.absolutePos(pos), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.CHEESE_BLOCK) && drops.get(0).has(DataComponents.BLOCK_STATE), "partial block drops itself with its state: " + drops);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHEESE_SLICE, 3));
        for (int i = 0; i < 3; i++) click(h, p, pos, Direction.UP);
        s = h.getLevel().getBlockState(h.absolutePos(pos));
        h.assertTrue(s.getValue(SliceBlock.SLICES) == 16, "slices put back");
        drops = net.minecraft.world.level.block.Block.getDrops(s, h.getLevel(), h.absolutePos(pos), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.CHEESE_BLOCK), "whole block drops itself");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNIFE));
        click(h, p, pos, Direction.EAST);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).getValue(SliceBlock.CUT) == SliceBlock.Cut.EAST, "side cut on a whole block");
        for (int i = 0; i < 15; i++) click(h, p, pos, Direction.EAST);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(), "16 slices per block");
        near(h, ModBlocks.BUTTER_BLOCK.getFriction(), 0.9, "butter is slippery (0.9)");
        h.succeed();
    }

    /** Owner 2026-10-05: breaking keeps the cut state in the item; placing restores it (cheese and butter). */
    static void sliceBlockKeepsState(GameTestHelper h) {
        for (SliceBlock block : List.of(ModBlocks.CHEESE_BLOCK, ModBlocks.BUTTER_BLOCK)) {
            BlockPos pos = new BlockPos(2, 2, 2);
            BlockPos abs = h.absolutePos(pos);
            h.setBlock(pos.below(), Blocks.STONE);
            h.setBlock(pos, block.defaultBlockState().setValue(SliceBlock.SLICES, 9).setValue(SliceBlock.CUT, SliceBlock.Cut.NORTH));
            var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, null);
            h.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1, "cut block drops itself: " + drops);
            ItemStack item = drops.get(0);
            var stored = item.get(DataComponents.BLOCK_STATE);
            h.assertTrue(stored != null && "9".equals(stored.properties().get("slices")) && "north".equals(stored.properties().get("cut")),
                    "item keeps slices and cut: " + stored);
            h.setBlock(pos, Blocks.AIR);
            ServerPlayer p = player(h, pos.below());
            p.setItemInHand(InteractionHand.MAIN_HAND, item);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.DOWN, 0.5), Direction.UP, abs.below(), false);
            p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            BlockState placed = h.getLevel().getBlockState(abs);
            h.assertTrue(placed.is(block) && placed.getValue(SliceBlock.SLICES) == 9 && placed.getValue(SliceBlock.CUT) == SliceBlock.Cut.NORTH,
                    "placing restores the cut block: " + placed);
            h.setBlock(pos, block);
            drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, null);
            h.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.get(0), new ItemStack(block)),
                    "whole block drops a plain block item (stacks with new ones)");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            click(h, p, pos, Direction.UP);
            h.assertTrue(h.getLevel().getBlockState(abs).getValue(SliceBlock.SLICES) == 16, "no slice without a knife");
            h.setBlock(pos, Blocks.AIR);
            h.setBlock(pos.below(), Blocks.AIR);
        }
        h.succeed();
    }

    /** Owner 2026-10-05: cheese is springy like a bed (same Vanilla properties as a bed). */
    static void cheeseBouncesLikeBed(GameTestHelper h) {
        near(h, ModBlocks.CHEESE_BLOCK.getBounceRestitution(), Blocks.BED.pick(net.minecraft.world.item.DyeColor.WHITE).getBounceRestitution(), "cheese bounce = bed bounce");
        near(h, ModBlocks.CHEESE_BLOCK.getFallDistanceReduction(), Blocks.BED.pick(net.minecraft.world.item.DyeColor.WHITE).getFallDistanceReduction(), "cheese fall reduction = bed");
        near(h, ModBlocks.BUTTER_BLOCK.getBounceRestitution(), 0.0, "butter does not bounce");
        h.succeed();
    }

    static void cutSoundsRegistered(GameTestHelper h) {
        for (String name : List.of("block.butter_block.cut", "block.cheese_block.cut")) {
            h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(com.simplesandwiches.Sandwiches.id(name)), "sound registered: " + name);
        }
        h.assertTrue(com.simplesandwiches.registry.ModSounds.BUTTER_CUT != com.simplesandwiches.registry.ModSounds.CHEESE_CUT, "two own sounds");
        h.succeed();
    }

    private static BlockPos cauldron(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, Blocks.CAULDRON);
        return pos;
    }

    private static BlockState at(GameTestHelper h, BlockPos rel) {
        return h.getLevel().getBlockState(h.absolutePos(rel));
    }

    private static void step(GameTestHelper h, BlockPos rel) {
        at(h, rel).tick(h.getLevel(), h.absolutePos(rel), h.getLevel().getRandom());
    }

    static void cauldronButter(GameTestHelper h) {
        BlockPos pos = cauldron(h);
        ServerPlayer p = player(h, pos);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        click(h, p, pos);
        h.assertTrue(at(h, pos).is(ModBlocks.MILK_CAULDRON) && at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.MILK, "milk poured in");
        h.assertTrue(p.getMainHandItem().is(Items.BUCKET), "bucket returned");
        h.assertTrue(h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(pos), ModBlocks.MILK_CAULDRON), "ripening scheduled");
        click(h, p, pos);
        h.assertTrue(at(h, pos).is(Blocks.CAULDRON) && p.getMainHandItem().is(Items.MILK_BUCKET), "milk can be taken back");
        click(h, p, pos);
        for (int i = 0; i < MilkCauldronBlock.STAGES - 1; i++) {
            step(h, pos);
            h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.STAGE) == i + 1, "visible ripening stage " + (i + 1));
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(click(h, p, pos) == InteractionResult.PASS, "unripe milk gives nothing");
        step(h, pos);
        h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.BUTTER, "milk ripened into butter by itself");
        step(h, pos);
        h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.BUTTER, "butter never spoils");
        click(h, p, pos);
        h.assertTrue(p.getMainHandItem().is(ModItems.BUTTER_BLOCK) && at(h, pos).is(Blocks.CAULDRON), "butter block taken");
        h.succeed();
    }

    static void cauldronCheeseSpoils(GameTestHelper h) {
        BlockPos pos = cauldron(h);
        ServerPlayer p = player(h, pos);
        for (boolean late : new boolean[]{false, true}) {
            h.setBlock(pos, Blocks.CAULDRON);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
            click(h, p, pos);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FERMENTED_SPIDER_EYE));
            click(h, p, pos);
            h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.CURDLING && p.getMainHandItem().isEmpty(), "rennet starts curdling");
            for (int i = 0; i < MilkCauldronBlock.STAGES; i++) step(h, pos);
            h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.CHEESE, "cheese ripe");
            h.assertTrue(MilkCauldronBlock.interval(Content.CHEESE) == SandwichConfig.cheeseHarvestWindowTicks, "harvest window scheduled");
            if (late) {
                step(h, pos);
                h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.SPOILED, "missed window spoils");
                click(h, p, pos);
                h.assertTrue(at(h, pos).is(Blocks.CAULDRON) && p.getMainHandItem().isEmpty(), "spoiled milk is only emptied");
            } else {
                click(h, p, pos);
                h.assertTrue(p.getMainHandItem().is(ModItems.CHEESE_BLOCK), "cheese block taken in time");
            }
        }
        h.assertTrue(SandwichConfig.cheeseTicks > SandwichConfig.butterTicks, "cheese takes longer than butter");
        h.succeed();
    }

    static void cauldronRipensInWorld(GameTestHelper h) {
        BlockPos pos = cauldron(h);
        ServerPlayer p = player(h, pos);
        JsonObject saved = SandwichConfig.toJson();
        SandwichConfig.butterTicks = SandwichConfig.BUTTER_TICKS_MIN;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        click(h, p, pos);
        h.runAfterDelay(SandwichConfig.BUTTER_TICKS_MIN + 20, () -> {
            try {
                h.assertTrue(at(h, pos).getValue(MilkCauldronBlock.CONTENT) == Content.BUTTER, "scheduled ticks ripen the milk");
            } finally {
                SandwichConfig.apply(saved);
            }
            h.succeed();
        });
    }

    static void bundleEating(GameTestHelper h) {
        ServerPlayer p = player(h, new BlockPos(2, 2, 2));
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable m = new BundleContents.Mutable();
        m.tryInsert(new ItemStack(Items.DIRT, 2));
        m.tryInsert(new ItemStack(Items.COOKED_BEEF, 3));
        bundle.set(DataComponents.BUNDLE_CONTENTS, m.toImmutable());
        p.setItemInHand(InteractionHand.MAIN_HAND, bundle);
        h.assertTrue(BundleEating.top(bundle).is(Items.COOKED_BEEF), "the steak is on top");
        h.assertTrue(bundle.getUseDuration(p) == 32, "eat time of the steak, got " + bundle.getUseDuration(p));
        InteractionResult r = bundle.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(r.consumesAction() && p.isUsingItem(), "starts eating");
        p.stopUsingItem();
        ItemStack result = bundle.finishUsingItem(h.getLevel(), p);
        h.assertTrue(p.getFoodData().getFoodLevel() == 12, "steak eaten from the bundle, food " + p.getFoodData().getFoodLevel());
        h.assertTrue(BundleEating.top(result).is(Items.COOKED_BEEF) && BundleEating.top(result).getCount() == 2, "one steak removed");
        BundleItem.toggleSelectedItem(result, 1);
        h.assertTrue(BundleEating.top(result).is(Items.DIRT) && BundleEating.eating(result) == null, "non-food on top: Vanilla bundle");
        h.assertTrue(result.getUseDuration(p) == 200, "Vanilla drop duration for non-food");
        ItemStack stew = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable sm = new BundleContents.Mutable();
        sm.tryInsert(new ItemStack(Items.MUSHROOM_STEW));
        stew.set(DataComponents.BUNDLE_CONTENTS, sm.toImmutable());
        h.assertTrue(BundleEating.eating(stew) == null, "bowl food stays out");
        p.getFoodData().setFoodLevel(20);
        ItemStack full = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable fm = new BundleContents.Mutable();
        fm.tryInsert(new ItemStack(Items.COOKED_BEEF));
        full.set(DataComponents.BUNDLE_CONTENTS, fm.toImmutable());
        p.setItemInHand(InteractionHand.MAIN_HAND, full);
        h.assertTrue(full.use(h.getLevel(), p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "not hungry: nothing happens");
        boolean before = SandwichConfig.bundleEating;
        try {
            SandwichConfig.bundleEating = false;
            h.assertTrue(BundleEating.eating(full) == null, "config switch off");
        } finally {
            SandwichConfig.bundleEating = before;
        }
        h.succeed();
    }

    static void cakeSlice(GameTestHelper h) {
        ItemStack slice = new ItemStack(ModItems.CAKE_SLICE, 3);
        h.assertTrue(slice.getMaxStackSize() == CakeBlock.MAX_BITES + 1, "stack size = cake bites (7)");
        FoodProperties f = slice.get(DataComponents.FOOD);
        h.assertTrue(f.nutrition() == 2, "2 hunger like a cake bite");
        near(h, f.saturation(), 0.4, "0.4 saturation like a cake bite");
        ServerPlayer p = player(h, new BlockPos(2, 2, 2));
        p.setItemInHand(InteractionHand.MAIN_HAND, slice);
        InteractionResult r = slice.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(r.consumesAction() && !p.isUsingItem(), "eaten in one go");
        h.assertTrue(p.getFoodData().getFoodLevel() == 6 && p.getMainHandItem().getCount() == 2, "slice eaten instantly");
        h.succeed();
    }

    private SandwichTests() {}
}
