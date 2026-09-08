package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantmentTags;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItemGroupsContent;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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
 * <h2>Known defects</h2>
 *
 * <p><b>The two enchanted apples cannot be reached in creative.</b>
 * {@code simplebuilding:enchanted_netherite_apple} and {@code simplebuilding:enchanted_enderite_apple}
 * are registered, are handed out by four loot pools and have no recipe, but
 * {@code ModItemGroupsContent#populate} never offers them - the only two mod items with no path
 * into a creative inventory at all. Vanilla puts its own enchanted golden apple in a tab, so this
 * looks like a forgotten line rather than a decision. {@link #ITEMS_NOT_IN_THE_CREATIVE_TAB} names
 * them, next to the six spatulas that are left out on purpose, so the tab check can be green
 * without stating that today's answer is the right one.
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

    /**
     * The blocks that do <em>not</em> drop themselves, and what they drop instead without Silk
     * Touch. Every other mod block is a {@code dropSelf}, so its expectation is derived from its
     * own block item rather than listed here; see {@link #everyModBlockLootTableLoads}.
     */
    private static final Map<String, Item> ORE_DROPS = Map.of(
            "nihilith_ore", ModItems.NIHILITH_SHARD,
            "astralit_ore", ModItems.ASTRALIT_DUST);

    /**
     * The item tag each mod enchantment's {@code supported_items} has to point at - the single
     * argument in {@code ModEnchantments} that decides what a player may put it on.
     *
     * <p>Listed rather than derived on purpose: reading the answer out of {@code ModEnchantments}
     * would move with any change made there, which is exactly the change this is here to catch.
     * A new enchantment has to be added here, and {@link #modEnchantmentsArePresentInTheDatapackRegistry}
     * fails until it is.
     */
    private static final Map<ResourceKey<Enchantment>, TagKey<Item>> SUPPORTED_ITEM_TAGS = Map.ofEntries(
            Map.entry(ModEnchantments.FAST_CHISELING, ModTags.Items.CHISEL_TOOLS),
            Map.entry(ModEnchantments.CONSTRUCTORS_TOUCH, ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE),
            Map.entry(ModEnchantments.RANGE, ModTags.Items.CHISEL_AND_MINING_TOOLS),
            Map.entry(ModEnchantments.DEEP_POCKETS, ModTags.Items.BUNDLE_ENCHANTABLE),
            Map.entry(ModEnchantments.FUNNEL, ModTags.Items.BUNDLE_ENCHANTABLE),
            Map.entry(ModEnchantments.DRAWER, ModTags.Items.BUNDLE_ENCHANTABLE),
            Map.entry(ModEnchantments.MASTER_BUILDER, ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
            Map.entry(ModEnchantments.COLOR_PALETTE, ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
            Map.entry(ModEnchantments.BREAK_THROUGH, ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
            Map.entry(ModEnchantments.RADIUS, ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
            Map.entry(ModEnchantments.OVERRIDE, ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
            Map.entry(ModEnchantments.COVER, ModTags.Items.BUILDING_WAND_ENCHANTABLE),
            Map.entry(ModEnchantments.BRIDGE, ModTags.Items.BUILDING_WAND_ENCHANTABLE),
            Map.entry(ModEnchantments.LINEAR, ModTags.Items.BUILDING_WAND_ENCHANTABLE),
            Map.entry(ModEnchantments.VEIN_MINER, ModTags.Items.VEINMINE_ENCHANTABLE),
            Map.entry(ModEnchantments.STRIP_MINER, ItemTags.PICKAXES),
            Map.entry(ModEnchantments.DOUBLE_JUMP, ItemTags.FOOT_ARMOR),
            Map.entry(ModEnchantments.KINETIC_PROTECTION, ItemTags.ARMOR_ENCHANTABLE),
            Map.entry(ModEnchantments.VERSATILITY, ItemTags.MINING_ENCHANTABLE));

    /** How often each block loot table is rolled when its drops are read. */
    private static final int BLOCK_LOOT_ROLLS = 8;

    /** Seed for those rolls, so a failure is reproducible instead of a coin flip. */
    private static final long BLOCK_LOOT_SEED = 20260904L;

    /**
     * Items the mod registers but deliberately keeps out of its creative tab.
     *
     * <p>The six spatulas are the legacy half of the chisel rename: they stay in the registry so
     * that {@code simplebuilding:*_spatula} stacks in existing worlds keep resolving, but a player
     * starting today is meant to find only the chisels. That one is on purpose.
     *
     * <p>The two enchanted apples are not - see the class javadoc's known defect. They are listed
     * here so that {@link #everyModItemIsInTheItemRegistry} can be green without either cementing
     * their absence as correct or hiding it: the list is checked in both directions, so the day
     * one of them is added to the tab this constant has to be edited, and the note above it read.
     *
     * <p>Every other registered mod item has to be offered by {@code ModItemGroupsContent}.
     */
    private static final Set<String> ITEMS_NOT_IN_THE_CREATIVE_TAB = Set.of(
            "stone_spatula",
            "copper_spatula",
            "iron_spatula",
            "gold_spatula",
            "diamond_spatula",
            "netherite_spatula",
            "enchanted_netherite_apple",
            "enchanted_enderite_apple");

    /**
     * Registry ids that exist for the sake of worlds that were saved with an older version, spelled
     * out because their whole point is the exact string.
     *
     * <p>{@link #everyModItemIsInTheItemRegistry} derives everything else it checks from
     * {@code ModItems}, in both directions, so a rename moves expectation and reality together and
     * passes. For these six that is precisely the failure: a stack of
     * {@code simplebuilding:stone_spatula} in a saved inventory is matched by id, and renaming the
     * registration deletes it on the next world load without a word.
     */
    private static final Set<String> LEGACY_ITEM_IDS = Set.of(
            "stone_spatula",
            "copper_spatula",
            "iron_spatula",
            "gold_spatula",
            "diamond_spatula",
            "netherite_spatula");

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

    /**
     * Three claims about the mod's items, because they need the same walk over {@code ModItems}.
     *
     * <p><b>Registration</b>, in both directions: every declared item has a registry entry in the
     * mod's namespace and resolves back to the same instance, and no {@code simplebuilding} entry
     * exists that {@code ModItems} cannot reach.
     *
     * <p><b>The ids that migrations depend on.</b> The walk above derives its expectation from
     * {@code ModItems} itself, so a renamed registration moves both sides together and passes -
     * while every saved stack under the old id silently disappears on the next world load.
     * {@link #LEGACY_ITEM_IDS} therefore spells the ids of the six legacy spatulas out.
     *
     * <p><b>Reachability in creative.</b> Registration is not the same thing as being findable:
     * {@code ModItemGroupsContent#populate} is the mod's only creative tab, it is a flat list of
     * {@code entries.accept(...)} calls, and deleting a line there takes an item out of the game
     * for every creative player without failing anything else. The tab is driven here - it is
     * plain server side code - and every registered mod item has to come out of it, except the
     * ones {@link #ITEMS_NOT_IN_THE_CREATIVE_TAB} names and explains. The exception list is
     * checked in both directions too, so an item that is added to the tab later cannot stay listed
     * as deliberately absent.
     *
     * <p>What breaks it: a renamed, removed or double registered item; a colored octant lost from
     * the {@code DyeColor} loop; a legacy spatula id renamed; a dropped {@code entries.accept}
     * line; or a {@code populate} that throws or returns early - the last one shows up as every
     * item at once.
     */
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

        // The ids saved worlds match their stacks against, by the literal string.
        for (String path : new TreeSet<>(LEGACY_ITEM_IDS)) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                problems.add(id + " is no longer registered; it is a legacy id, so every stack of it "
                        + "in an existing world is dropped without a word on the next load");
            }
        }

        // Being registered is not the same as being reachable: the creative tab is the only place
        // a player can take a mod item from without a recipe or a chest.
        List<ItemStack> offered = new ArrayList<>();
        ModItemGroupsContent.populate(
                (CreativeModeTab.Output) (stack, visibility) -> offered.add(stack),
                helper.getLevel().registryAccess());
        helper.assertTrue(!offered.isEmpty(),
                "ModItemGroupsContent.populate offered no entries at all, so the tab checks below "
                        + "could not fail");

        Set<Item> inTheTab = new HashSet<>();
        for (ItemStack stack : offered) {
            inTheTab.add(stack.getItem());
        }
        Set<Identifier> declaredInOrder = new TreeSet<>(Comparator.comparing(Identifier::toString));
        declaredInOrder.addAll(declared);
        for (Identifier id : declaredInOrder) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            boolean expected = !ITEMS_NOT_IN_THE_CREATIVE_TAB.contains(id.getPath());
            if (expected && !inTheTab.contains(item)) {
                problems.add(id + " is registered but ModItemGroupsContent never offers it, so it "
                        + "cannot be taken out of the creative inventory at all");
            } else if (!expected && inTheTab.contains(item)) {
                problems.add(id + " is offered in the creative tab but is listed in "
                        + "ITEMS_NOT_IN_THE_CREATIVE_TAB as deliberately left out; drop it from that list");
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
     * <p>The same treatment is given to the three families whose <em>output count</em> is part of
     * the deal - a player crafts five reinforced hoppers, two netherite hoppers and three of every
     * reinforced or netherite furnace at a time - and to the quiver, whose smithing chain nothing
     * else touched. A count is the easiest thing in a recipe file to change by accident and the
     * hardest to notice, so every one of them is asserted, not just the result item.
     *
     * <p>That is twenty-four of the mod's roughly 130 recipes. Everything else here - wands,
     * octants, the enderite armour and tools, the block and nugget conversions - is still covered
     * only by the dangling reference walk above, which says nothing about their shape; the three
     * bundle recipes have a home of their own in
     * {@link BundleWiringTests#bundleRecipesCraftTheBaseAndUpgradeItTierByTier}. Naming that is
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

        // Quiver: a vanilla bundle, two string, two leather and a copper nugget, then the same two
        // smithing steps the bundles use. Nothing else in the suite looks at these three files.
        assertShapedRecipe(helper, modRecipes, "quiver", ModItems.QUIVER, 1,
                new String[]{" SL", "SLN", "B  "},
                Map.of('S', Items.STRING, 'L', Items.LEATHER, 'N', Items.COPPER_NUGGET, 'B', Items.BUNDLE),
                shapes);
        assertSmithingRecipe(helper, modRecipes, "netherite_quiver_smithing",
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.QUIVER, Items.NETHERITE_INGOT,
                ModItems.NETHERITE_QUIVER, shapes);
        assertSmithingRecipe(helper, modRecipes, "enderite_quiver_smithing",
                ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_INGOT,
                ModItems.ENDERITE_QUIVER, shapes);
        assertNoSmithingRecipe(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.QUIVER,
                ModItems.ENDERITE_INGOT,
                "the enderite quiver upgrade straight from the plain quiver", shapes);

        // Hoppers: five at a time out of five vanilla hoppers, a name tag and three cracked
        // diamonds; two netherite ones out of two of those and a netherite nugget.
        assertShapedRecipe(helper, modRecipes, "reinforced_hopper_from_crafting", ModItems.REINFORCED_HOPPER, 5,
                new String[]{"HNH", "DDD", "HHH"},
                Map.of('H', Items.HOPPER, 'N', Items.NAME_TAG, 'D', ModItems.CRACKED_DIAMOND), shapes);
        assertShapedRecipe(helper, modRecipes, "netherite_hopper_from_crafting", ModItems.NETHERITE_HOPPER, 2,
                new String[]{"H", "N", "H"},
                Map.of('H', ModItems.REINFORCED_HOPPER, 'N', ModItems.NETHERITE_NUGGET), shapes);

        // The name tag is what makes the hopper recipe cost something; a stick may not stand in.
        assertNoCraftingRecipe(helper, new String[]{"HNH", "DDD", "HHH"},
                Map.of('H', Items.HOPPER, 'N', Items.STICK, 'D', ModItems.CRACKED_DIAMOND),
                "the reinforced hopper grid with a stick where the name tag belongs", shapes);

        // The six furnace blocks: three at a time from three vanilla appliances and six cracked
        // diamonds, three netherite ones from three of those and a netherite nugget.
        String[] appliancePattern = {"DDD", "AAA", "DDD"};
        assertShapedRecipe(helper, modRecipes, "reinforced_furnace", ModItems.REINFORCED_FURNACE, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.FURNACE), shapes);
        assertShapedRecipe(helper, modRecipes, "reinforced_smoker", ModItems.REINFORCED_SMOKER, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.SMOKER), shapes);
        assertShapedRecipe(helper, modRecipes, "reinforced_blast_furnace", ModItems.REINFORCED_BLAST_FURNACE, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.BLAST_FURNACE), shapes);

        String[] bulkPattern = {"NR", "RR"};
        assertShapedRecipe(helper, modRecipes, "netherite_furnace_bulk", ModItems.NETHERITE_FURNACE, 3,
                bulkPattern, Map.of('N', ModItems.NETHERITE_NUGGET, 'R', ModItems.REINFORCED_FURNACE), shapes);
        assertShapedRecipe(helper, modRecipes, "netherite_smoker_bulk", ModItems.NETHERITE_SMOKER, 3,
                bulkPattern, Map.of('N', ModItems.NETHERITE_NUGGET, 'R', ModItems.REINFORCED_SMOKER), shapes);
        assertShapedRecipe(helper, modRecipes, "netherite_blast_furnace_bulk", ModItems.NETHERITE_BLAST_FURNACE, 3,
                bulkPattern, Map.of('N', ModItems.NETHERITE_NUGGET, 'R', ModItems.REINFORCED_BLAST_FURNACE), shapes);

        // One reinforced furnace short of the 2x2: the upgrade may not get cheaper by accident.
        assertNoCraftingRecipe(helper, new String[]{"NR", "R "},
                Map.of('N', ModItems.NETHERITE_NUGGET, 'R', ModItems.REINFORCED_FURNACE),
                "a netherite furnace grid with only two reinforced furnaces", shapes);

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
        assertShapedRecipe(helper, modRecipes, path, result, 1, pattern, key, problems);
    }

    /**
     * The same check for a recipe that yields more than one item. The count is asserted rather
     * than ignored: "five reinforced hoppers per craft" is a balance decision a player feels, and
     * a {@code "count"} edited from 5 to 1 touches no ingredient and no pattern.
     */
    private static void assertShapedRecipe(GameTestHelper helper, Map<Identifier, RecipeHolder<?>> modRecipes,
                                           String path, Item result, int count, String[] pattern,
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
        if (!crafted.is(result) || crafted.getCount() != count) {
            problems.add(id + ": crafts " + crafted.getCount() + "x" + BuiltInRegistries.ITEM.getKey(crafted.getItem())
                    + " instead of " + count + "x" + BuiltInRegistries.ITEM.getKey(result));
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

    /**
     * Every mod block's loot table: that it exists, that it is the block's own, that the server
     * loaded it - and what falls out of it.
     *
     * <p>The first three were all this test used to do, and they are blind to the one edit that
     * matters to a player: a table named {@code blocks/netherite_hopper} that yields dirt still
     * exists, is still the block's own and still loads. So each table is <em>rolled</em> here,
     * with an empty tool, and the set of items it produces has to be exactly the one item the
     * block is supposed to give. That expectation is derived, not copied: for all but the two ores
     * it is the block's own {@code BlockItem}, which is the promise {@code dropSelf} makes, and
     * {@link #ORE_DROPS} names the two that trade themselves for a resource.
     *
     * <p>The empty tool is deliberate - it is what makes the ore tables take the branch a player
     * without Silk Touch gets. {@link #brokenModBlocksDropTheirExpectedItem} covers the other end
     * of the same promise for four blocks: that breaking one in the world really goes through this
     * table.
     *
     * <p>The item names alone are not enough, because the set they are collected in is the union
     * over all {@link #BLOCK_LOOT_ROLLS} rolls and says nothing about <em>how much</em> came out of
     * any single one of them. Two edits live in that blind spot: a {@code set_count} of four on the
     * entry (every piston mined would hand over four pistons, and the union is still one item), and
     * a {@code random_chance} condition on the pool (seven empty rolls and one lucky one still fill
     * the union). Each roll is therefore held to exactly one stack of exactly one item - which is
     * what every mod table promises today: a single {@code dropSelf} or ore entry, and
     * {@code apply_bonus}/{@code ore_drops} leaves the count at one for the empty tool used here.
     *
     * <p>What breaks it: a block that loses its table or inherits a foreign one through
     * {@code Properties.ofFullCopy}, a table that stops loading, a drop entry swapped for another
     * item, an ore that starts dropping its own block without Silk Touch, a second entry added
     * next to the intended one, a changed drop count, or a pool that only sometimes drops at all.
     */
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

            LootTable table = lootRegistries.getLootTable(lootKey.get());
            if (table == LootTable.EMPTY) {
                problems.add(actual + " did not load (server resolved it to LootTable.EMPTY)");
                continue;
            }

            // What the table actually hands over. Rolled a few times so a table that only
            // sometimes drops the wrong thing cannot slip through on one lucky draw.
            Item expectedDrop = ORE_DROPS.containsKey(blockId.getPath())
                    ? ORE_DROPS.get(blockId.getPath())
                    : BuiltInRegistries.ITEM.getValue(blockId);
            List<List<ItemStack>> rolls = rollBlockLoot(helper, block, table);
            Set<Identifier> dropped = new TreeSet<>(Comparator.comparing(Identifier::toString));
            for (List<ItemStack> produced : rolls) {
                for (ItemStack stack : produced) {
                    dropped.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
            }
            Set<Identifier> wanted = Set.of(BuiltInRegistries.ITEM.getKey(expectedDrop));
            if (!dropped.equals(wanted)) {
                problems.add(actual + " drops " + dropped + " instead of " + wanted
                        + " when the block is broken with an empty hand");
            }

            // The union above cannot see the size of a single roll: a table that hands over four
            // items, or one that drops nothing on most rolls, produces the very same set. Only the
            // first offending roll is reported - eight copies of one defect are not eight defects.
            for (int roll = 0; roll < rolls.size(); roll++) {
                List<ItemStack> produced = rolls.get(roll);
                if (produced.size() != 1) {
                    problems.add(actual + " hands over " + produced.size() + " stacks on roll " + roll
                            + " instead of exactly one; every mod block is a single entry that always drops");
                    break;
                }
                int count = produced.get(0).getCount();
                if (count != 1) {
                    problems.add(actual + " hands over " + count + "x "
                            + BuiltInRegistries.ITEM.getKey(produced.get(0).getItem()) + " on roll " + roll
                            + " instead of a single item");
                    break;
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), "block loot table problems: " + problems);
        helper.succeed();
    }

    /**
     * Rolls one block loot table with an empty tool and returns what each single roll produced.
     *
     * <p>The seed is fixed so a failure is the same on every machine, and the roll goes through
     * the real {@code LootTable} the server loaded rather than reading the json, so the entry, its
     * conditions and its functions are all covered at once.
     *
     * <p>The rolls are kept apart instead of being poured into one set: the stack count and the
     * number of stacks per roll are the part of a table a union throws away, and they are what a
     * {@code set_count} or a {@code random_chance} on the pool changes.
     */
    private static List<List<ItemStack>> rollBlockLoot(GameTestHelper helper, Block block, LootTable table) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);

        List<List<ItemStack>> rolls = new ArrayList<>();
        for (int roll = 0; roll < BLOCK_LOOT_ROLLS; roll++) {
            List<ItemStack> produced = new ArrayList<>();
            for (ItemStack stack : table.getRandomItems(params, BLOCK_LOOT_SEED + roll)) {
                if (!stack.isEmpty()) {
                    produced.add(stack);
                }
            }
            rolls.add(produced);
        }
        return rolls;
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

    /**
     * Every mod enchantment reached the datapack registry, and every one of them still points at
     * the item tag it is meant to point at.
     *
     * <p>"Its tag resolved to something" was all this used to ask, and that is blind to the whole
     * decision: which items an enchantment may go on is a single {@code items.getOrThrow(...)}
     * argument in {@code ModEnchantments}, and swapping Deep Pockets from
     * {@code simplebuilding:bundle_enchantable} to {@code minecraft:pickaxes} leaves a non-empty
     * tag with nothing but bound holders behind. {@link #SUPPORTED_ITEM_TAGS} therefore names the
     * tag each enchantment hangs on, and the check reads it back off the loaded enchantment.
     *
     * <p>The tag key, not its contents: what is <em>in</em> those tags is
     * {@code ModItemTagProvider}'s business and is pinned where the behaviour is - see
     * {@link BundleWiringTests#containerEnchantmentsAcceptTheBundlesTheyAreMeantFor} for the two
     * container tags. This is the wire between the two, and it is checked for all of them.
     *
     * <p>What breaks it: an enchantment missing from the registry or loaded without a key in
     * {@code ModEnchantments}, an item tag that no longer resolves, an enchantment re-pointed at a
     * different tag, or a new enchantment added without deciding - here, on purpose - what it may
     * be put on.
     */
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

            // Which tag it is, not just that some tag answered.
            TagKey<Item> expectedTag = SUPPORTED_ITEM_TAGS.get(key);
            if (expectedTag == null) {
                problems.add(key.identifier() + " is not listed in SUPPORTED_ITEM_TAGS, so nothing says "
                        + "which items it is meant to be allowed on");
                continue;
            }
            Optional<TagKey<Item>> actualTag = supported.unwrapKey();
            if (actualTag.isEmpty()) {
                problems.add(key.identifier() + " no longer hangs on an item tag at all but on a fixed "
                        + "list of items; " + expectedTag.location() + " was expected");
            } else if (!expectedTag.equals(actualTag.get())) {
                problems.add(key.identifier() + " may be put on " + actualTag.get().location()
                        + " instead of " + expectedTag.location());
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
     * void all the same. Two of the four entities dropped there carry a custom name, because the
     * regression this feature exists to keep out is a mixin that reads {@code getHoverName} again.
     * Steps 3 and 4 cannot see that one: they run this file's {@link #isVoidProtected}, a plain tag
     * lookup that no edit to the mixin can influence, so a mixin protecting the tag <em>or</em>
     * anything named "Enderite" would leave them green while every dirt block renamed in an anvil
     * floats in the void again. The named ingot and the named dirt in step 5b tick the real mixin
     * and pin both directions where they are actually decided.
     *
     * <p>Second, the tag's <em>bounds</em>. Step 1 compares the shipped tag against
     * {@code ModTags.Items.isVoidProtectedByRule}, which is the very method the datagen fills it
     * with, so it can only catch a stale tag json - widening or narrowing that rule and re-running
     * datagen would move both sides together. Step 1b therefore states the rule a second time, in
     * this file, in words: every {@code simplebuilding} item whose registry path begins with
     * {@code enderite_}, plus {@code raw_enderite}, and nothing else. Changing
     * {@code VOID_PROTECTED_PATH_PREFIX} or adding to {@code VOID_PROTECTED_EXTRA_PATHS} now has
     * to be a decision made here as well. The spelled out lists of items that must and must not be
     * protected are the third, coarsest net under both.
     *
     * <p>The list of anchors is not a sample of one family: the enderite bundle and quiver are on
     * it because they are the two protected items whose registry path is the only thing putting
     * them there - rename {@code enderite_bundle} to {@code bundle_enderite} and it drops out of
     * the tag while the rule and the tag still agree with each other.
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

        // 1b. The same comparison against the rule as this file states it, so that changing the
        //     rule in ModTags and re-running datagen cannot move expectation and reality together.
        Set<Identifier> byWrittenRule = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (MOD_ID.equals(id.getNamespace())
                    && (id.getPath().startsWith("enderite_") || "raw_enderite".equals(id.getPath()))) {
                byWrittenRule.add(id);
            }
        }
        if (!actual.equals(byWrittenRule)) {
            problems.add(ModTags.Items.VOID_PROTECTED.location() + " holds " + actual
                    + ", but the rule this test states - every simplebuilding item whose path starts "
                    + "with \"enderite_\", plus raw_enderite - selects " + byWrittenRule
                    + "; the void protection rule was changed, which is a decision that belongs here too");
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
                ModItems.ENDERITE_BLOCK_ITEM,
                ModItems.ENDERITE_BUNDLE,
                ModItems.ENDERITE_QUIVER,
                ModItems.ENDERITE_SLEDGEHAMMER,
                ModItems.ENDERITE_BUILDING_WAND,
                ModItems.ENDERITE_UPGRADE_TEMPLATE)) {
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

        // 5b. Steps 3 and 4 through the mixin instead of through this file's copy of its tag test.
        //     The copy is a tag lookup and can never react to a display name, so on its own it
        //     rules nothing out; only these two entities separate "reads the tag" from "reads the
        //     tag or the name", which is the exact shape of the regression the tag replaced.
        ItemEntity localizedEntity = dropBelowTheWorld(helper, localized.copy(), anchor, minY - 20.0);
        localizedEntity.tick();
        if (!localizedEntity.isNoGravity() || localizedEntity.getY() < minY) {
            problems.add("an enderite ingot renamed to \"Enderit-Barren\" was left in the void at Y="
                    + localizedEntity.getY() + "; EnderiteItemMixin decides by display name, so the "
                    + "protection is gone in every non-English locale");
        }

        ItemEntity impostorEntity = dropBelowTheWorld(helper, impostor.copy(), anchor, minY - 20.0);
        impostorEntity.tick();
        if (impostorEntity.isNoGravity() || impostorEntity.getY() >= minY) {
            problems.add("minecraft:dirt renamed to \"Enderite Ingot\" is rescued from the void, it is at Y="
                    + impostorEntity.getY() + "; EnderiteItemMixin reads the display name again, so an "
                    + "anvil buys void protection for any item");
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
