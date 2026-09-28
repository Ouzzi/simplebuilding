package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingCoreItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/**
 * The six building cores (owner, 2026-09-28): they do not stack, every recipe that takes a core still
 * crafts with one core per slot, and a right click plays one of three animations rolled 70/20/10 -
 * no gameplay effect, then a short cooldown.
 */
public final class BuildingCoreTests {

    private BuildingCoreTests() {
    }

    private static List<Item> cores() {
        return List.of(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                ModItems.DIAMOND_CORE, ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE);
    }

    private static boolean isCore(Holder<Item> item) {
        return cores().contains(item.value());
    }

    /**
     * Every core has a maximum stack size of 1, so two cores take two slots.
     *
     * <p><strong>What breaks this test:</strong> any core registered with a stack size above 1
     * (they stacked to 16 before 2026-09-28).
     */
    public static void buildingCoresAreNotStackable(GameTestHelper helper) {
        for (Item core : cores()) {
            ItemStack stack = new ItemStack(core);
            helper.assertValueEqual(stack.getMaxStackSize(), 1, BuiltInRegistries.ITEM.getKey(core) + " max stack size");
            helper.assertFalse(stack.isStackable(), BuiltInRegistries.ITEM.getKey(core) + " still stacks");
        }
        helper.succeed();
    }

    /**
     * Every shaped crafting and smithing recipe that takes a core matches with exactly one item per
     * slot - the core included - and every recipe that makes a core makes one. The easter recipes
     * are left out: they only exist on their dates.
     *
     * <p><strong>What breaks this test:</strong> a recipe that would need two cores in one slot (a
     * count-based smithing addition above one), a recipe that yields a stack of cores, or a core
     * recipe that stops matching; and fewer than the seventeen core recipes known on 2026-09-28
     * (then the test would check nothing).
     */
    public static void everyCoreRecipeCraftsWithOneCorePerSlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> checked = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            String id = holder.id().identifier().toString();
            if (id.contains("easter/")) {
                continue;
            }
            Recipe<?> recipe = holder.value();
            if (recipe instanceof ShapedRecipe shaped) {
                List<Optional<Ingredient>> ingredients = shaped.getIngredients();
                if (ingredients.stream().noneMatch(i -> i.isPresent() && i.get().items().anyMatch(BuildingCoreTests::isCore))) {
                    continue;
                }
                List<ItemStack> grid = new ArrayList<>();
                for (Optional<Ingredient> ingredient : ingredients) {
                    grid.add(ingredient.map(BuildingCoreTests::one).orElse(ItemStack.EMPTY));
                }
                CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid);
                helper.assertTrue(shaped.matches(input, level), id + " does not match with one item per slot");
                helper.assertFalse(shaped.assemble(input).isEmpty(), id + " crafts nothing");
                checked.add(id);
            } else if (recipe instanceof SmithingRecipe smithing) {
                boolean baseIsCore = smithing.baseIngredient().items().anyMatch(BuildingCoreTests::isCore);
                boolean additionIsCore = smithing.additionIngredient().map(i -> i.items().anyMatch(BuildingCoreTests::isCore)).orElse(false);
                if (!baseIsCore && !additionIsCore) {
                    continue;
                }
                SmithingRecipeInput input = new SmithingRecipeInput(
                        smithing.templateIngredient().map(BuildingCoreTests::one).orElse(ItemStack.EMPTY),
                        one(smithing.baseIngredient()),
                        smithing.additionIngredient().map(BuildingCoreTests::one).orElse(ItemStack.EMPTY));
                helper.assertTrue(smithing.matches(input, level), id + " does not match with one core");
                ItemStack result = smithing.assemble(input);
                helper.assertFalse(result.isEmpty(), id + " forges nothing");
                helper.assertTrue(result.getCount() <= result.getMaxStackSize(), id + " forges " + result + ", more than a stack");
                checked.add(id);
            }
        }
        helper.assertTrue(checked.size() >= 17, "only " + checked.size() + " core recipes were found: " + checked);
        helper.succeed();
    }

    /**
     * The animation roll: 0-69 glow, 70-89 orbit, 90-99 burst, and over 20000 seeded rolls the
     * shares land within two points of 70/20/10. The same seed gives the same sequence.
     *
     * <p><strong>What breaks this test:</strong> changed weights or boundaries, or a roll that does
     * not come from the random source it is handed.
     */
    public static void coreAnimationRollFollowsTheSeventyTwentyTenWeights(GameTestHelper helper) {
        helper.assertTrue(BuildingCoreItem.fromRoll(0) == BuildingCoreItem.Animation.GLOW, "roll 0");
        helper.assertTrue(BuildingCoreItem.fromRoll(69) == BuildingCoreItem.Animation.GLOW, "roll 69");
        helper.assertTrue(BuildingCoreItem.fromRoll(70) == BuildingCoreItem.Animation.ORBIT, "roll 70");
        helper.assertTrue(BuildingCoreItem.fromRoll(89) == BuildingCoreItem.Animation.ORBIT, "roll 89");
        helper.assertTrue(BuildingCoreItem.fromRoll(90) == BuildingCoreItem.Animation.BURST, "roll 90");
        helper.assertTrue(BuildingCoreItem.fromRoll(99) == BuildingCoreItem.Animation.BURST, "roll 99");

        int[] counts = new int[3];
        RandomSource random = RandomSource.create(20260928L);
        RandomSource twin = RandomSource.create(20260928L);
        int rolls = 20000;
        for (int i = 0; i < rolls; i++) {
            BuildingCoreItem.Animation animation = BuildingCoreItem.roll(random);
            helper.assertTrue(animation == BuildingCoreItem.roll(twin), "the same seed rolled differently at " + i);
            counts[animation.ordinal()]++;
        }
        int[] expected = {70, 20, 10};
        for (int i = 0; i < 3; i++) {
            double share = 100.0 * counts[i] / rolls;
            helper.assertTrue(Math.abs(share - expected[i]) < 2.0,
                    BuildingCoreItem.Animation.values()[i] + " came up " + share + " % instead of about " + expected[i] + " %");
        }
        helper.succeed();
    }

    /**
     * A right click with a core, through the server's use path ({@code ServerPlayerGameMode#useItem}),
     * plays an animation (the click succeeds), keeps the core and starts the cooldown; a second click
     * during the cooldown does nothing.
     *
     * <p><strong>What breaks this test:</strong> a core that passes the click on (plain items do),
     * consumes itself, or has no cooldown.
     */
    public static void rightClickingTheCorePlaysAnAnimationAndStartsTheCooldown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        for (Item core : cores()) {
            ItemStack stack = new ItemStack(core);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult first = player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND);
            helper.assertTrue(first instanceof InteractionResult.Success, BuiltInRegistries.ITEM.getKey(core) + ": the right click was not taken, " + first);
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack), BuiltInRegistries.ITEM.getKey(core) + ": no cooldown after the animation");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, BuiltInRegistries.ITEM.getKey(core) + ": cores in hand after the click");
            InteractionResult second = player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND);
            helper.assertFalse(second instanceof InteractionResult.Success, BuiltInRegistries.ITEM.getKey(core) + ": a click during the cooldown played again");
        }
        helper.succeed();
    }

    private static ItemStack one(Ingredient ingredient) {
        // Prefer the core if the slot takes one, so the core itself is what is tested.
        Optional<Holder<Item>> core = ingredient.items().filter(BuildingCoreTests::isCore).findFirst();
        return new ItemStack(core.orElseGet(() -> ingredient.items().findFirst().orElseThrow()));
    }
}
