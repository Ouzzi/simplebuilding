package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantmentTags;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.util.context.ContextMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data integrity checks against the running server.
 *
 * <p>These catch broken data packs that survive a boot: a block that silently lost its loot
 * table, an item tag that no longer resolves so an enchantment ends up targeting nothing, a
 * recipe pointing at an item that is not in the registry, or a datapack registry entry that
 * never made it out of the bootstrap.
 *
 * <h2>Known defect</h2>
 *
 * <p>{@code ModRecipeProvider} builds every sledgehammer from one block, two pieces of the tier's
 * material and two sticks - except the stone one: {@code createSledgehammerRecipe(STONE_SLEDGEHAMMER,
 * Items.COBBLESTONE, Items.IRON_INGOT)} passes an iron <em>ingot</em> where the parameter is named
 * {@code block} and where every other tier passes that tier's block (copper block, iron block, gold
 * block, diamond block). The cheapest sledgehammer in the game therefore costs an iron ingot, which
 * gates the whole tier behind iron. {@link #modRecipesOnlyReferenceRegisteredItems} pins the shape
 * of that recipe - the two cobblestones and the two sticks in their documented positions - but
 * deliberately makes no statement about what belongs in the corner slot, so the defect is neither
 * frozen as intended behaviour nor hidden.
 */
public final class DataIntegrityTests {

    /** Tick budget for {@link #brokenModBlocksDropTheirExpectedItem}. */
    public static final int BLOCK_DROP_MAX_TICKS = 60;

    private static final String MOD_ID = Simplebuilding.MOD_ID;

    /** Blocks that intentionally have no item form (mirrors vanilla's piston head). */
    private static final Set<String> BLOCKS_WITHOUT_ITEM = Set.of("netherite_piston_head");

    /** Blocks registered with {@code noLootTable()}. */
    private static final Set<String> BLOCKS_WITHOUT_LOOT_TABLE = Set.of("netherite_piston_head");

    private record DropCase(BlockPos pos, Block block, Item expectedDrop) {
    }

    // Deliberately mixed: a plain drop-self block, a "requires correct tool" block, an ore with
    // an alternatives/silk-touch loot table, and a light block.
    private static final List<DropCase> DROP_CASES = List.of(
            new DropCase(new BlockPos(1, 1, 1), ModBlocks.POLISHED_END_STONE, ModItems.POLISHED_END_STONE),
            new DropCase(new BlockPos(4, 1, 1), ModBlocks.ENDERITE_BLOCK, ModItems.ENDERITE_BLOCK_ITEM),
            new DropCase(new BlockPos(1, 1, 4), ModBlocks.NIHILITH_ORE, ModItems.NIHILITH_SHARD),
            new DropCase(new BlockPos(4, 1, 4), ModBlocks.CONSTRUCTION_LIGHT, ModItems.CONSTRUCTION_LIGHT));

    // =================================================================================
    // 1. Item registry
    // =================================================================================

    public static void everyModItemIsInTheItemRegistry(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Set<Identifier> declared = new HashSet<>();

        for (Item item : declaredModItems()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !MOD_ID.equals(id.getNamespace())) {
                problems.add("item instance " + item + " is not registered under the " + MOD_ID + " namespace (key=" + id + ")");
                continue;
            }
            declared.add(id);
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                problems.add(id + " is missing from BuiltInRegistries.ITEM");
            } else if (BuiltInRegistries.ITEM.getValue(id) != item) {
                problems.add(id + " resolves to a different instance than the one ModItems holds");
            }
        }

        // The colored octants are registered inside a DyeColor loop, so they are easy to lose.
        for (DyeColor color : DyeColor.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "octant_" + color.getName());
            OctantItem expected = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (expected == null) {
                problems.add(id + " is missing from ModItems.COLORED_OCTANT_ITEMS");
                continue;
            }
            declared.add(id);
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                problems.add(id + " is missing from BuiltInRegistries.ITEM");
            } else if (BuiltInRegistries.ITEM.getValue(id) != expected) {
                problems.add(id + " resolves to a different instance than COLORED_OCTANT_ITEMS holds");
            }
        }

        // Reverse direction: nothing may hide in the registry that the mod does not know about.
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (MOD_ID.equals(id.getNamespace()) && !declared.contains(id)) {
                problems.add(id + " is registered but is not reachable from ModItems");
            }
        }

        helper.assertTrue(problems.isEmpty(), "item registry mismatch: " + problems);
        helper.succeed();
    }

    // =================================================================================
    // 2. Block registry + block item pairing
    // =================================================================================

    public static void everyModBlockIsRegisteredAndHasItsBlockItem(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Set<Identifier> declared = new HashSet<>();

        for (Block block : declaredModBlocks()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !MOD_ID.equals(id.getNamespace())) {
                problems.add("block instance " + block + " is not registered under the " + MOD_ID + " namespace (key=" + id + ")");
                continue;
            }
            declared.add(id);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                problems.add(id + " is missing from BuiltInRegistries.BLOCK");
                continue;
            }
            if (BuiltInRegistries.BLOCK.getValue(id) != block) {
                problems.add(id + " resolves to a different instance than the one ModBlocks holds");
                continue;
            }

            boolean itemExpected = !BLOCKS_WITHOUT_ITEM.contains(id.getPath());
            boolean itemPresent = BuiltInRegistries.ITEM.containsKey(id);
            if (!itemExpected) {
                if (itemPresent) {
                    problems.add(id + " is expected to have no item form but one is registered");
                }
                continue;
            }
            if (!itemPresent) {
                problems.add(id + " has no item with a matching identifier");
            } else if (!(BuiltInRegistries.ITEM.getValue(id) instanceof BlockItem blockItem)) {
                problems.add(id + " has an item with a matching identifier that is not a BlockItem");
            } else if (blockItem.getBlock() != block) {
                problems.add(id + " has a BlockItem that places " + BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
            }
        }

        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            if (MOD_ID.equals(id.getNamespace()) && !declared.contains(id)) {
                problems.add(id + " is registered but is not reachable from ModBlocks");
            }
        }

        helper.assertTrue(problems.isEmpty(), "block registry mismatch: " + problems);
        helper.succeed();
    }

    // =================================================================================
    // 3. Recipes
    // =================================================================================

    /**
     * Two different things, because they need the same walk over the recipe manager.
     *
     * <p>The first is the dangling reference check: no mod recipe may point at an item that is not
     * in the registry, and at least one mod recipe has to have loaded at all.
     *
     * <p>The second is the <em>shapes</em>. "The recipes load and their ingredients exist" says
     * nothing about what a player has to put on the bench: the pattern, the amounts, the smithing
     * template and even the existence of one particular recipe were all free to change. The two
     * families the wiki writes a shape down for - the five sledgehammers and the five chisels,
     * plus the two smithing upgrades above them - are therefore really crafted here: the grid is
     * assembled and handed to {@code RecipeManager#getRecipeFor}, which has to answer with exactly
     * that recipe and with exactly that result. A grid one ingredient short has to answer with
     * nothing, which is what separates "the pattern still is what it says" from "the recipe merely
     * still exists".
     *
     * <p>That is twelve of the mod's roughly 130 recipes. Everything else here - wands, octants,
     * the enderite armour and tools, the block and nugget conversions - is still covered only by
     * the dangling reference walk above, which says nothing about their shape. Naming that is
     * more useful than implying the whole recipe tree is pinned.
     *
     * <p>The stone sledgehammer is the one recipe whose corner slot is checked only for being
     * occupied; see the class javadoc's known defect.
     *
     * <p>Items without any resolvable recipe stay a log line rather than a failure: plenty of them
     * are loot or creative only, and {@code simplebuilding:count_based_smithing} exposes no
     * {@code RecipeDisplay} to collect a result from in the first place.
     */
    // Ingredient#items() is deprecated ("display only") but it is the only way to look at the raw
    // holders, which is exactly what we need: a holder that never got bound is the failure mode.
    @SuppressWarnings("deprecation")
    public static void modRecipesOnlyReferenceRegisteredItems(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RecipeManager recipeManager = server.getRecipeManager();
        ContextMap displayContext = SlotDisplayContext.fromLevel(helper.getLevel());

        List<String> dangling = new ArrayList<>();
        Set<Item> produced = new HashSet<>();
        Map<Identifier, RecipeHolder<?>> modRecipes = new HashMap<>();
        int modRecipeCount = 0;
        int modRecipesWithoutDisplay = 0;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Identifier recipeId = holder.id().identifier();
            boolean isModRecipe = MOD_ID.equals(recipeId.getNamespace());

            if (isModRecipe) {
                modRecipeCount++;
                modRecipes.put(recipeId, holder);
                for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
                    if (ingredient.isEmpty()) {
                        dangling.add(recipeId + ": ingredient resolves to no item at all (missing item tag?)");
                        continue;
                    }
                    for (Holder<Item> itemHolder : ingredient.items().toList()) {
                        if (!itemHolder.isBound()) {
                            dangling.add(recipeId + ": unbound ingredient reference " + itemHolder.getRegisteredName());
                        } else if (BuiltInRegistries.ITEM.getKey(itemHolder.value()) == null) {
                            dangling.add(recipeId + ": ingredient item is not in BuiltInRegistries.ITEM");
                        }
                    }
                }
            }

            // Results are collected for every recipe, so a vanilla recipe producing a mod item counts too.
            List<RecipeDisplay> displays = holder.value().display();
            if (isModRecipe && displays.isEmpty()) {
                modRecipesWithoutDisplay++;
            }
            for (RecipeDisplay display : displays) {
                for (ItemStack stack : display.result().resolveForStacks(displayContext)) {
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (BuiltInRegistries.ITEM.getKey(stack.getItem()) == null) {
                        dangling.add(recipeId + ": result item is not in BuiltInRegistries.ITEM");
                    } else {
                        produced.add(stack.getItem());
                    }
                }
            }
        }

        helper.assertTrue(modRecipeCount > 0, "no " + MOD_ID + " recipes were loaded at all");

        // Recipeless items are reported, not failed on: plenty of them are loot/creative only, and
        // recipe types that expose no RecipeDisplay (e.g. simplebuilding:count_based_smithing)
        // cannot contribute a result here either.
        List<String> withoutRecipe = new ArrayList<>();
        for (Item item : allModItems()) {
            if (!produced.contains(item)) {
                withoutRecipe.add(String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
            }
        }
        Collections.sort(withoutRecipe);
        Simplebuilding.LOGGER.info(
                "[gametest] {} {} recipes loaded ({} of them expose no RecipeDisplay); {} mod items have no resolvable recipe result: {}",
                modRecipeCount, MOD_ID, modRecipesWithoutDisplay, withoutRecipe.size(), withoutRecipe);

        helper.assertTrue(dangling.isEmpty(), "recipes referencing items that do not exist: " + dangling);

        // --- the shapes themselves ---
        List<String> shapes = new ArrayList<>();

        // Sledgehammers: one block, two pieces of the tier's material, two sticks.
        String[] hammerPattern = {"BMM", " S ", " S "};
        assertShapedRecipe(helper, modRecipes, "copper_sledgehammer", ModItems.COPPER_SLEDGEHAMMER, hammerPattern,
                Map.of('B', Items.COPPER_BLOCK.weathering().unaffected(), 'M', Items.COPPER_INGOT, 'S', Items.STICK),
                shapes);
        assertShapedRecipe(helper, modRecipes, "iron_sledgehammer", ModItems.IRON_SLEDGEHAMMER, hammerPattern,
                Map.of('B', Items.IRON_BLOCK, 'M', Items.IRON_INGOT, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "gold_sledgehammer", ModItems.GOLD_SLEDGEHAMMER, hammerPattern,
                Map.of('B', Items.GOLD_BLOCK, 'M', Items.GOLD_INGOT, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "diamond_sledgehammer", ModItems.DIAMOND_SLEDGEHAMMER, hammerPattern,
                Map.of('B', Items.DIAMOND_BLOCK, 'M', Items.DIAMOND, 'S', Items.STICK), shapes);

        // The stone tier's corner slot is the known defect, so only the rest of its shape is pinned.
        assertShapeWithFreeCorner(helper, modRecipes, "stone_sledgehammer", hammerPattern,
                Map.of('M', Items.COBBLESTONE, 'S', Items.STICK), shapes);

        // One block short of the pattern: nothing may step in and craft the hammer anyway.
        assertNoCraftingRecipe(helper, new String[]{"BM ", " S ", " S "},
                Map.of('B', Items.IRON_BLOCK, 'M', Items.IRON_INGOT, 'S', Items.STICK),
                "an iron sledgehammer grid with only one iron ingot", shapes);
        assertNoCraftingRecipe(helper, new String[]{"BMM", " S ", "   "},
                Map.of('B', Items.IRON_BLOCK, 'M', Items.IRON_INGOT, 'S', Items.STICK),
                "an iron sledgehammer grid with only one stick", shapes);

        // Chisels: a stick, one piece of the tier's material and two copper nuggets. The shipped
        // json writes this into the lower left corner of a 3x3; both the loader and CraftingInput
        // shrink that to the 2x2 the pattern actually occupies.
        String[] chiselPattern = {"NM", "SN"};
        assertShapedRecipe(helper, modRecipes, "stone_chisel", ModItems.STONE_CHISEL, chiselPattern,
                Map.of('M', Items.COBBLESTONE, 'N', Items.COPPER_NUGGET, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "copper_chisel", ModItems.COPPER_CHISEL, chiselPattern,
                Map.of('M', Items.COPPER_INGOT, 'N', Items.COPPER_NUGGET, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "iron_chisel", ModItems.IRON_CHISEL, chiselPattern,
                Map.of('M', Items.IRON_INGOT, 'N', Items.COPPER_NUGGET, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "gold_chisel", ModItems.GOLD_CHISEL, chiselPattern,
                Map.of('M', Items.GOLD_INGOT, 'N', Items.COPPER_NUGGET, 'S', Items.STICK), shapes);
        assertShapedRecipe(helper, modRecipes, "diamond_chisel", ModItems.DIAMOND_CHISEL, chiselPattern,
                Map.of('M', Items.DIAMOND, 'N', Items.COPPER_NUGGET, 'S', Items.STICK), shapes);

        assertNoCraftingRecipe(helper, new String[]{" M", "SN"},
                Map.of('M', Items.DIAMOND, 'N', Items.COPPER_NUGGET, 'S', Items.STICK),
                "a diamond chisel grid with only one copper nugget", shapes);

        // The two smithing upgrades, template included - that template is the whole gate.
        assertSmithingRecipe(helper, modRecipes, "netherite_chisel_smithing",
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.DIAMOND_CHISEL, Items.NETHERITE_INGOT,
                ModItems.NETHERITE_CHISEL, shapes);
        assertSmithingRecipe(helper, modRecipes, "enderite_chisel_smithing",
                ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_INGOT,
                ModItems.ENDERITE_CHISEL, shapes);

        assertNoSmithingRecipe(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.DIAMOND_CHISEL,
                Items.NETHERITE_INGOT,
                "the netherite chisel upgrade under the enderite template", shapes);
        assertNoSmithingRecipe(helper, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.NETHERITE_CHISEL,
                ModItems.ENDERITE_INGOT,
                "the enderite chisel upgrade under the netherite template", shapes);

        helper.assertTrue(shapes.isEmpty(), "recipe shape problems: " + shapes);
        helper.succeed();
    }

    /**
     * Lays the pattern out on a crafting grid and requires the recipe manager to answer with
     * exactly {@code path} and exactly one {@code result}. Going through the recipe manager rather
     * than reading the recipe's own fields is the point: it is the same lookup a crafting table
     * does, so it fails for a changed pattern, a changed amount, a changed result and a deleted
     * file alike.
     */
    private static void assertShapedRecipe(GameTestHelper helper, Map<Identifier, RecipeHolder<?>> modRecipes,
                                           String path, Item result, String[] pattern,
                                           Map<Character, Item> key, List<String> problems) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
        if (!modRecipes.containsKey(id)) {
            problems.add(id + " is not loaded at all");
            return;
        }

        CraftingInput input = grid(pattern, key);
        Optional<RecipeHolder<CraftingRecipe>> matched = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        if (matched.isEmpty()) {
            problems.add(id + ": its documented pattern " + List.of(pattern) + " crafts nothing at all");
            return;
        }

        Identifier matchedId = matched.get().id().identifier();
        if (!id.equals(matchedId)) {
            problems.add(id + ": its documented pattern crafts " + matchedId + " instead");
            return;
        }

        ItemStack crafted = matched.get().value().assemble(input);
        if (!crafted.is(result) || crafted.getCount() != 1) {
            problems.add(id + ": crafts " + crafted.getCount() + "x" + BuiltInRegistries.ITEM.getKey(crafted.getItem())
                    + " instead of one " + BuiltInRegistries.ITEM.getKey(result));
        }
    }

    /**
     * The same shape check for a recipe whose top left slot is the known defect: the pattern's
     * positions and every other ingredient are pinned, the corner only has to be occupied by
     * something. Read straight off the loaded recipe, because a grid lookup would have to commit
     * to a corner item and would thereby state that the current one is correct.
     */
    private static void assertShapeWithFreeCorner(GameTestHelper helper, Map<Identifier, RecipeHolder<?>> modRecipes,
                                                  String path, String[] pattern, Map<Character, Item> key,
                                                  List<String> problems) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
        RecipeHolder<?> holder = modRecipes.get(id);
        if (holder == null) {
            problems.add(id + " is not loaded at all");
            return;
        }
        if (!(holder.value() instanceof ShapedRecipe shaped)) {
            problems.add(id + " is no longer a shaped crafting recipe but a " + holder.value().getType());
            return;
        }

        List<Optional<Ingredient>> ingredients = shaped.getIngredients();
        int width = pattern[0].length();
        if (ingredients.size() != width * pattern.length) {
            problems.add(id + ": the pattern covers " + ingredients.size() + " slots instead of the "
                    + (width * pattern.length) + " of " + List.of(pattern));
            return;
        }

        for (int row = 0; row < pattern.length; row++) {
            for (int column = 0; column < width; column++) {
                char symbol = pattern[row].charAt(column);
                Optional<Ingredient> slot = ingredients.get(row * width + column);
                String where = id + " at row " + row + ", column " + column;

                if (symbol == ' ') {
                    if (slot.isPresent()) {
                        problems.add(where + ": expected an empty slot but something is required there");
                    }
                    continue;
                }
                if (slot.isEmpty()) {
                    problems.add(where + ": expected '" + symbol + "' but the slot is empty");
                    continue;
                }
                Item expected = key.get(symbol);
                if (expected == null) {
                    continue; // deliberately unasserted slot, see the class javadoc
                }
                if (!slot.get().test(new ItemStack(expected))) {
                    problems.add(where + ": " + BuiltInRegistries.ITEM.getKey(expected) + " does not fit there");
                }
            }
        }
    }

    /** A grid that must not craft anything, so a recipe cannot quietly get cheaper or shapeless. */
    private static void assertNoCraftingRecipe(GameTestHelper helper, String[] pattern, Map<Character, Item> key,
                                               String what, List<String> problems) {
        helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid(pattern, key), helper.getLevel())
                .ifPresent(match -> problems.add(what + " crafts " + match.id().identifier()));
    }

    private static void assertSmithingRecipe(GameTestHelper helper, Map<Identifier, RecipeHolder<?>> modRecipes,
                                             String path, Item template, Item base, Item addition, Item result,
                                             List<String> problems) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
        if (!modRecipes.containsKey(id)) {
            problems.add(id + " is not loaded at all");
            return;
        }

        SmithingRecipeInput input = new SmithingRecipeInput(
                new ItemStack(template), new ItemStack(base), new ItemStack(addition));
        Optional<RecipeHolder<SmithingRecipe>> matched = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
        if (matched.isEmpty()) {
            problems.add(id + ": " + BuiltInRegistries.ITEM.getKey(template) + " + "
                    + BuiltInRegistries.ITEM.getKey(base) + " + " + BuiltInRegistries.ITEM.getKey(addition)
                    + " smiths nothing at all");
            return;
        }

        Identifier matchedId = matched.get().id().identifier();
        if (!id.equals(matchedId)) {
            problems.add(id + ": that smithing table layout produces " + matchedId + " instead");
            return;
        }

        ItemStack smithed = matched.get().value().assemble(input);
        if (!smithed.is(result)) {
            problems.add(id + ": smiths " + BuiltInRegistries.ITEM.getKey(smithed.getItem())
                    + " instead of " + BuiltInRegistries.ITEM.getKey(result));
        }
    }

    /** A smithing table layout that must not produce anything - the upgrade template is a gate. */
    private static void assertNoSmithingRecipe(GameTestHelper helper, Item template, Item base, Item addition,
                                               String what, List<String> problems) {
        SmithingRecipeInput input = new SmithingRecipeInput(
                new ItemStack(template), new ItemStack(base), new ItemStack(addition));
        helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
                .ifPresent(match -> problems.add(what + " smiths " + match.id().identifier()));
    }

    /** Turns a pattern plus its key into the crafting grid a player would lay out. */
    private static CraftingInput grid(String[] pattern, Map<Character, Item> key) {
        List<ItemStack> items = new ArrayList<>();
        for (String row : pattern) {
            for (int column = 0; column < row.length(); column++) {
                char symbol = row.charAt(column);
                items.add(symbol == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(symbol)));
            }
        }
        return CraftingInput.of(pattern[0].length(), pattern.length, items);
    }

    // =================================================================================
    // 4. Loot tables
    // =================================================================================

    public static void everyModBlockLootTableLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ReloadableServerRegistries.Holder lootRegistries = server.reloadableRegistries();
        List<String> problems = new ArrayList<>();

        for (Block block : declaredModBlocks()) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId == null) {
                continue; // already reported by everyModBlockIsRegisteredAndHasItsBlockItem
            }
            Optional<ResourceKey<LootTable>> lootKey = block.getLootTable();

            if (BLOCKS_WITHOUT_LOOT_TABLE.contains(blockId.getPath())) {
                if (lootKey.isPresent()) {
                    problems.add(blockId + " should drop nothing but points at " + lootKey.get().identifier());
                }
                continue;
            }

            if (lootKey.isEmpty()) {
                problems.add(blockId + " has no loot table at all");
                continue;
            }

            // Guards against Properties.ofFullCopy(...) accidentally inheriting a foreign loot table.
            Identifier expected = blockId.withPrefix("blocks/");
            Identifier actual = lootKey.get().identifier();
            if (!expected.equals(actual)) {
                problems.add(blockId + " points at the foreign loot table " + actual + " (expected " + expected + ")");
                continue;
            }

            if (lootRegistries.getLootTable(lootKey.get()) == LootTable.EMPTY) {
                problems.add(actual + " did not load (server resolved it to LootTable.EMPTY)");
            }
        }

        helper.assertTrue(problems.isEmpty(), "block loot table problems: " + problems);
        helper.succeed();
    }

    // =================================================================================
    // 5. Actual block drops
    // =================================================================================

    public static void brokenModBlocksDropTheirExpectedItem(GameTestHelper helper) {
        for (DropCase dropCase : DROP_CASES) {
            helper.setBlock(dropCase.pos(), dropCase.block());
            helper.assertBlockPresent(dropCase.block(), dropCase.pos());
        }

        for (DropCase dropCase : DROP_CASES) {
            // GameTestHelper#destroyBlock deliberately drops nothing, so go through the level.
            boolean destroyed = helper.getLevel().destroyBlock(helper.absolutePos(dropCase.pos()), true);
            helper.assertTrue(destroyed,
                    "could not break " + BuiltInRegistries.BLOCK.getKey(dropCase.block()) + " at " + dropCase.pos());
        }

        helper.runAfterDelay(3, () -> {
            for (DropCase dropCase : DROP_CASES) {
                helper.assertBlockNotPresent(dropCase.block(), dropCase.pos());
                helper.assertItemEntityPresent(dropCase.expectedDrop(), dropCase.pos(), 1.5D);
            }
            // Negative control: without silk touch the ore must yield its shard, never the ore block.
            helper.assertItemEntityNotPresent(ModItems.NIHILITH_ORE_ITEM);
            helper.succeed();
        });
    }

    // =================================================================================
    // 6. Enchantments (a datapack registry)
    // =================================================================================

    public static void modEnchantmentsArePresentInTheDatapackRegistry(GameTestHelper helper) {
        Registry<Enchantment> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<ResourceKey<Enchantment>> declaredKeys = declaredModEnchantmentKeys();

        helper.assertTrue(!declaredKeys.isEmpty(), "no enchantment keys found on ModEnchantments");

        List<String> problems = new ArrayList<>();
        Set<Identifier> declaredIds = new HashSet<>();

        for (ResourceKey<Enchantment> key : declaredKeys) {
            declaredIds.add(key.identifier());
            if (!registry.containsKey(key)) {
                problems.add(key.identifier() + " is missing from the enchantment registry");
                continue;
            }
            Enchantment enchantment = registry.getValueOrThrow(key);
            HolderSet<Item> supported = enchantment.getSupportedItems();
            if (supported.size() == 0) {
                problems.add(key.identifier() + " supports no item at all (its item tag did not resolve)");
                continue;
            }
            for (Holder<Item> itemHolder : supported) {
                if (!itemHolder.isBound()) {
                    problems.add(key.identifier() + " supports the unbound item " + itemHolder.getRegisteredName());
                }
            }
        }

        for (Identifier id : registry.keySet()) {
            if (MOD_ID.equals(id.getNamespace()) && !declaredIds.contains(id)) {
                problems.add(id + " is loaded as data but has no ResourceKey in ModEnchantments");
            }
        }

        helper.assertTrue(problems.isEmpty(), "enchantment registry problems: " + problems);
        helper.succeed();
    }

    // =================================================================================
    // 7. Enchantment tags
    // =================================================================================

    public static void modEnchantmentTagsResolveToTheExpectedEntries(GameTestHelper helper) {
        Registry<Enchantment> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> problems = new ArrayList<>();

        // The mod merges into the vanilla tag; both sides have to survive.
        Set<Identifier> enchantingTable = tagContents(registry, EnchantmentTags.IN_ENCHANTING_TABLE);
        if (!enchantingTable.contains(ModEnchantments.FAST_CHISELING.identifier())) {
            problems.add("minecraft:in_enchanting_table does not contain " + ModEnchantments.FAST_CHISELING.identifier());
        }
        if (!enchantingTable.contains(Enchantments.EFFICIENCY.identifier())) {
            problems.add("minecraft:in_enchanting_table lost its vanilla entries (replace:true regression?)");
        }

        expectTagContents(registry, ModEnchantmentTags.MINING_EXCLUSIVE_SET,
                Set.of(ModEnchantments.STRIP_MINER, ModEnchantments.VEIN_MINER), problems);
        expectTagContents(registry, ModEnchantmentTags.BUILDER_EXCLUSIVE_SET,
                Set.of(ModEnchantments.MASTER_BUILDER, ModEnchantments.COLOR_PALETTE, ModEnchantments.DRAWER), problems);
        expectTagContents(registry, ModEnchantmentTags.COVER_EXCLUSIVE_SET,
                Set.of(ModEnchantments.BRIDGE, ModEnchantments.LINEAR), problems);
        expectTagContents(registry, ModEnchantmentTags.WAND_MODIFIER_EXCLUSIVE_SET,
                Set.of(ModEnchantments.COVER), problems);

        // The tags only matter if the loaded enchantments actually picked them up as exclusive sets.
        expectCompatibility(registry, ModEnchantments.DRAWER, ModEnchantments.MASTER_BUILDER, false, problems);
        expectCompatibility(registry, ModEnchantments.COVER, ModEnchantments.BRIDGE, false, problems);
        expectCompatibility(registry, ModEnchantments.VEIN_MINER, ModEnchantments.STRIP_MINER, false, problems);
        // Bridge and Linear are meant to stack; only Cover excludes them.
        expectCompatibility(registry, ModEnchantments.BRIDGE, ModEnchantments.LINEAR, true, problems);
        expectCompatibility(registry, ModEnchantments.DRAWER, ModEnchantments.FUNNEL, true, problems);

        helper.assertTrue(problems.isEmpty(), "enchantment tag problems: " + problems);
        helper.succeed();
    }

    // =================================================================================
    // 8. Void protection
    // =================================================================================

    /**
     * {@code EnderiteItemMixin} keeps enderite gear from being lost in the void. It used to
     * recognise that gear by display name ({@code getHoverName().getString().contains("Enderite")}),
     * which failed in both directions: in any non-English locale nothing was protected at all, and
     * any foreign item renamed to "Enderite" in an anvil was. The mixin now reads the
     * {@code simplebuilding:void_protected} item tag, and this test pins both directions.
     *
     * <p>Two things are checked that a tag test alone cannot say anything about.
     *
     * <p>First, the <em>protection</em>, not only the tag: the last step drops a real item entity
     * below the world's floor and ticks it. Everything above it only ever evaluates the tag, which
     * would keep passing with an empty {@code if} body in the mixin or with the mixin missing from
     * {@code simplebuilding.mixins.json} altogether - and every enderite ingot would fall into the
     * void all the same.
     *
     * <p>Second, the tag's <em>upper</em> bound. Step 1 compares the shipped tag against
     * {@code ModTags.Items.isVoidProtectedByRule}, which is the very method the datagen fills it
     * with, so widening that rule and re-running datagen would move both sides together. The
     * spelled out list of items that must <em>not</em> be protected is what makes that visible.
     */
    public static void voidProtectedTagIsLanguageIndependent(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();

        // 1. The loaded tag must hold exactly what the datagen rule selects from the registry.
        //    A missing/stale tag JSON, or a new enderite item added after the last datagen run,
        //    shows up here.
        Set<Identifier> actual = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.Items.VOID_PROTECTED)) {
            holder.unwrapKey().ifPresent(key -> actual.add(key.identifier()));
        }
        Set<Identifier> expected = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (ModTags.Items.isVoidProtectedByRule(id)) {
                expected.add(id);
            }
        }
        helper.assertTrue(!expected.isEmpty(), "the void protection rule selects no item at all");
        if (!actual.equals(expected)) {
            Set<Identifier> missing = new TreeSet<>(Comparator.comparing(Identifier::toString));
            missing.addAll(expected);
            missing.removeAll(actual);
            Set<Identifier> unexpected = new TreeSet<>(Comparator.comparing(Identifier::toString));
            unexpected.addAll(actual);
            unexpected.removeAll(expected);
            problems.add(ModTags.Items.VOID_PROTECTED.location() + " is missing " + missing
                    + " and additionally contains " + unexpected + " (datagen not re-run?)");
        }

        // 2. Hard anchors, spelled out instead of derived, so a rule that quietly stops matching
        //    anything cannot make step 1 pass trivially.
        for (Item item : List.of(
                ModItems.ENDERITE_INGOT,
                ModItems.ENDERITE_SCRAP,
                ModItems.RAW_ENDERITE,
                ModItems.ENDERITE_NUGGET,
                ModItems.ENDERITE_PICKAXE,
                ModItems.ENDERITE_SWORD,
                ModItems.ENDERITE_HELMET,
                ModItems.ENDERITE_BLOCK_ITEM)) {
            if (!isVoidProtected(new ItemStack(item))) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is not covered by "
                        + ModTags.Items.VOID_PROTECTED.location());
            }
        }

        // 2b. The other end of the rule: everything that must stay unprotected. Without this the
        //     rule could be widened (say to every "_sledgehammer") and step 1 would still pass,
        //     because it derives its expectation from that same rule.
        for (Item item : List.of(
                ModItems.DIAMOND_SLEDGEHAMMER,
                ModItems.NETHERITE_SLEDGEHAMMER,
                ModItems.STONE_CHISEL,
                ModItems.DIAMOND_CORE,
                Items.DIAMOND,
                Items.NETHERITE_INGOT)) {
            if (isVoidProtected(new ItemStack(item))) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is covered by "
                        + ModTags.Items.VOID_PROTECTED.location() + ", which is meant to hold enderite only");
            }
        }

        // 3. The regression itself: a protected item keeps its protection under a display name
        //    that contains no "Enderite" at all -- this is what every non-English locale looks like.
        ItemStack localized = new ItemStack(ModItems.ENDERITE_INGOT);
        localized.set(DataComponents.CUSTOM_NAME, Component.literal("Enderit-Barren"));
        if (localized.getHoverName().getString().contains("Enderite")) {
            problems.add("test setup broken: the localized name still contains \"Enderite\"");
        }
        if (!isVoidProtected(localized)) {
            problems.add("an enderite ingot loses its void protection under a non-English name");
        }

        // 4. The other direction: renaming a foreign item in an anvil must not buy protection.
        ItemStack impostor = new ItemStack(Items.DIRT);
        impostor.set(DataComponents.CUSTOM_NAME, Component.literal("Enderite Ingot"));
        if (!impostor.getHoverName().getString().contains("Enderite")) {
            problems.add("test setup broken: the impostor name does not contain \"Enderite\"");
        }
        if (isVoidProtected(impostor)) {
            problems.add("minecraft:dirt renamed to \"Enderite Ingot\" is treated as void protected");
        }

        // 5. The mixin, not the tag: a dropped stack below the world's floor has to be caught, and
        //    one that is not on the tag has to be left to fall.
        ServerLevel level = helper.getLevel();
        int minY = level.getMinY();
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 1, 3));

        ItemEntity caught = dropBelowTheWorld(helper, new ItemStack(ModItems.ENDERITE_INGOT), anchor, minY - 5.0);
        caught.tick();
        if (!caught.isNoGravity()) {
            problems.add("an enderite ingot below Y=" + minY + " keeps falling; EnderiteItemMixin did not "
                    + "take it out of gravity, so the tag above protects nothing");
        }

        caught.setPos(anchor.getX() + 0.5, minY - 20.0, anchor.getZ() + 0.5);
        caught.tick();
        if (caught.getY() < minY) {
            problems.add("an enderite ingot at Y=" + (minY - 20) + " was not lifted back into the world, "
                    + "it is at Y=" + caught.getY() + "; vanilla deletes it at Y=" + (minY - 64));
        }

        ItemEntity lost = dropBelowTheWorld(helper, new ItemStack(Items.DIRT), anchor, minY - 20.0);
        lost.tick();
        if (lost.isNoGravity() || lost.getY() >= minY) {
            problems.add("minecraft:dirt is being rescued from the void as well, at Y=" + lost.getY()
                    + "; the mixin no longer looks at the tag at all");
        }

        helper.assertTrue(problems.isEmpty(), "void protection problems: " + problems);
        helper.succeed();
    }

    /** Exactly the test {@code EnderiteItemMixin} performs on the dropped stack. */
    private static boolean isVoidProtected(ItemStack stack) {
        return stack.typeHolder().is(ModTags.Items.VOID_PROTECTED);
    }

    /**
     * Puts a real {@link ItemEntity} into the world below its floor, at the test structure's own
     * X and Z so no foreign chunk is touched, and hands it back for ticking. It is discarded again
     * when the test ends - it lives outside {@code helper.getBounds()}, so nothing else would ever
     * clean it up.
     */
    private static ItemEntity dropBelowTheWorld(GameTestHelper helper, ItemStack stack, BlockPos anchor, double y) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), anchor.getX() + 0.5, y, anchor.getZ() + 0.5, stack);
        entity.setDeltaMovement(0.0, 0.0, 0.0);
        helper.getLevel().addFreshEntity(entity);
        helper.runBeforeTestEnd(entity::discard);
        return entity;
    }

    // =================================================================================
    // Helpers
    // =================================================================================

    private static void expectTagContents(Registry<Enchantment> registry,
                                          TagKey<Enchantment> tag,
                                          Set<ResourceKey<Enchantment>> expectedKeys,
                                          List<String> problems) {
        Set<Identifier> actual = tagContents(registry, tag);
        Set<Identifier> expected = new LinkedHashSet<>();
        for (ResourceKey<Enchantment> key : expectedKeys) {
            expected.add(key.identifier());
        }
        if (!actual.equals(expected)) {
            problems.add(tag.location() + " contains " + actual + " but " + expected + " was expected");
        }
    }

    private static void expectCompatibility(Registry<Enchantment> registry,
                                            ResourceKey<Enchantment> first,
                                            ResourceKey<Enchantment> second,
                                            boolean compatible,
                                            List<String> problems) {
        Optional<Holder.Reference<Enchantment>> a = registry.get(first);
        Optional<Holder.Reference<Enchantment>> b = registry.get(second);
        if (a.isEmpty() || b.isEmpty()) {
            problems.add("cannot compare " + first.identifier() + " with " + second.identifier() + ": one of them is missing");
            return;
        }
        boolean actual = Enchantment.areCompatible(a.get(), b.get());
        if (actual != compatible) {
            problems.add(first.identifier() + " and " + second.identifier()
                    + " are " + (actual ? "compatible" : "exclusive") + " but should be "
                    + (compatible ? "compatible" : "exclusive"));
        }
    }

    private static Set<Identifier> tagContents(Registry<Enchantment> registry, TagKey<Enchantment> tag) {
        Set<Identifier> contents = new LinkedHashSet<>();
        for (Holder<Enchantment> holder : registry.getTagOrEmpty(tag)) {
            holder.unwrapKey().ifPresent(key -> contents.add(key.identifier()));
        }
        return contents;
    }

    /** Items reachable as static fields of {@link ModItems}. */
    private static List<Item> declaredModItems() {
        List<Item> items = new ArrayList<>();
        for (Field field : ModItems.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Item.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                if (field.get(null) instanceof Item item) {
                    items.add(item);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot read ModItems." + field.getName(), e);
            }
        }
        return items;
    }

    /** Static fields plus the dye-colored octants. */
    private static List<Item> allModItems() {
        List<Item> items = declaredModItems();
        items.addAll(ModItems.COLORED_OCTANT_ITEMS.values());
        return items;
    }

    private static List<Block> declaredModBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (Field field : ModBlocks.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Block.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                if (field.get(null) instanceof Block block) {
                    blocks.add(block);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot read ModBlocks." + field.getName(), e);
            }
        }
        return blocks;
    }

    @SuppressWarnings("unchecked")
    private static List<ResourceKey<Enchantment>> declaredModEnchantmentKeys() {
        List<ResourceKey<Enchantment>> keys = new ArrayList<>();
        for (Field field : ModEnchantments.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !ResourceKey.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                if (field.get(null) instanceof ResourceKey<?> key && key.isFor(Registries.ENCHANTMENT)) {
                    keys.add((ResourceKey<Enchantment>) key);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot read ModEnchantments." + field.getName(), e);
            }
        }
        return keys;
    }
}
