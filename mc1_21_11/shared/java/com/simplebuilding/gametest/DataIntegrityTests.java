package com.simplebuilding.gametest;

import java.util.Enumeration;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.InputStream;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantmentTags;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.RegistryOps;

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
 * <p>The two enchanted apples, once the only mod items no creative tab offered, sit in the
 * materials tab since the creative inventory was split into four tabs (2026-09).
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

    /**
     * Fewest enchantments {@link #generatedEnchantmentFilesStillMatchTheirSource} has to record
     * before its comparison says anything. A bootstrap that registers nothing - because the call
     * was removed, or an early return crept in - would otherwise make the test pass by comparing
     * an empty set against itself.
     */
    private static final int MIN_SOURCE_ENCHANTMENTS = 10;


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
            Map.entry(ModEnchantments.DEEP_POCKETS, ModTags.Items.DEEP_POCKETS_ENCHANTABLE),
            Map.entry(ModEnchantments.FUNNEL, ModTags.Items.FUNNEL_ENCHANTABLE),
            Map.entry(ModEnchantments.DRAWER, ModTags.Items.BUNDLE_ENCHANTABLE),
            Map.entry(ModEnchantments.MASTER_BUILDER, ModTags.Items.MASTER_BUILDER_ENCHANTABLE),
            Map.entry(ModEnchantments.COLOR_PALETTE, ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
            Map.entry(ModEnchantments.BREAK_THROUGH, ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
            Map.entry(ModEnchantments.RADIUS, ModTags.Items.RADIUS_ENCHANTABLE),
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
     * <p>Every other registered mod item has to be offered by {@code ModItemGroupsContent} - in
     * exactly one of its four tabs, see {@link #everyModItemIsInExactlyOneCreativeTab}. The list is
     * checked in both directions, so an item added to a tab cannot stay listed here.
     */
    private static final Set<String> ITEMS_NOT_IN_THE_CREATIVE_TAB = Set.of(
            "stone_spatula",
            "copper_spatula",
            "iron_spatula",
            "gold_spatula",
            "diamond_spatula",
            "netherite_spatula");

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
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
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
     * <p>The same treatment is given to the families whose <em>output count</em> is part of the
     * deal - a player crafts five reinforced hoppers and three of every reinforced furnace at a
     * time - and to the quiver, whose smithing chain nothing else touched. A count is the easiest
     * thing in a recipe file to change by accident and the hardest to notice, so every one of them
     * is asserted, not just the result item.
     *
     * <p>The netherite and enderite machines have no crafting recipe at all since 2026-09: they are
     * made in the world with a sledgehammer and a nugget ({@code SledgehammerUpgrades}). The old
     * netherite grids - the hopper column and the 2x2 of one nugget and three reinforced machines -
     * must therefore craft nothing, and no loaded crafting recipe may produce any of the ten
     * netherite or enderite machine items.
     *
     * <p>That is twenty of the mod's roughly 130 recipes. Everything else here - wands,
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
                Map.of('B', Items.COPPER_BLOCK, 'M', Items.COPPER_INGOT, 'S', Items.STICK),
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

        // Quiver: a vanilla bundle, two string, two leather and a copper nugget. The netherite step
        // starts from the reinforced quiver, the tier in between (crafted from the quiver), just as
        // the netherite bundle starts from the reinforced bundle; enderite follows as for the
        // bundles. Nothing else in the suite looks at these three files.
        assertShapedRecipe(helper, modRecipes, "quiver", ModItems.QUIVER, 1,
                new String[]{" SL", "SLN", "B  "},
                Map.of('S', Items.STRING, 'L', Items.LEATHER, 'N', Items.COPPER_NUGGET, 'B', Items.BUNDLE),
                shapes);
        assertSmithingRecipe(helper, modRecipes, "netherite_quiver_smithing",
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.REINFORCED_QUIVER, Items.NETHERITE_INGOT,
                ModItems.NETHERITE_QUIVER, shapes);
        assertSmithingRecipe(helper, modRecipes, "enderite_quiver_smithing",
                ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_INGOT,
                ModItems.ENDERITE_QUIVER, shapes);
        assertNoSmithingRecipe(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.QUIVER,
                ModItems.ENDERITE_INGOT,
                "the enderite quiver upgrade straight from the plain quiver", shapes);

        // Hoppers: five at a time out of five vanilla hoppers, a name tag and three cracked
        // diamonds. The old netherite column (two of those around a nugget) crafts nothing now.
        assertShapedRecipe(helper, modRecipes, "reinforced_hopper_from_crafting", ModItems.REINFORCED_HOPPER, 5,
                new String[]{"HNH", "DDD", "HHH"},
                Map.of('H', Items.HOPPER, 'N', Items.NAME_TAG, 'D', ModItems.CRACKED_DIAMOND), shapes);
        assertNoCraftingRecipe(helper, new String[]{"H", "N", "H"},
                Map.of('H', ModItems.REINFORCED_HOPPER, 'N', ModItems.NETHERITE_NUGGET),
                "the old netherite hopper column", shapes);

        // The name tag is what makes the hopper recipe cost something; a stick may not stand in.
        assertNoCraftingRecipe(helper, new String[]{"HNH", "DDD", "HHH"},
                Map.of('H', Items.HOPPER, 'N', Items.STICK, 'D', ModItems.CRACKED_DIAMOND),
                "the reinforced hopper grid with a stick where the name tag belongs", shapes);

        // The three reinforced furnace blocks: three at a time from three vanilla appliances and
        // six cracked diamonds.
        String[] appliancePattern = {"DDD", "AAA", "DDD"};
        assertShapedRecipe(helper, modRecipes, "reinforced_furnace", ModItems.REINFORCED_FURNACE, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.FURNACE), shapes);
        assertShapedRecipe(helper, modRecipes, "reinforced_smoker", ModItems.REINFORCED_SMOKER, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.SMOKER), shapes);
        assertShapedRecipe(helper, modRecipes, "reinforced_blast_furnace", ModItems.REINFORCED_BLAST_FURNACE, 3,
                appliancePattern, Map.of('D', ModItems.CRACKED_DIAMOND, 'A', Items.BLAST_FURNACE), shapes);

        // The old netherite 2x2 - one nugget, three reinforced machines - crafts nothing for any of
        // the four families it used to exist for: the netherite tier is hammered in the world now.
        String[] bulkPattern = {"NR", "RR"};
        for (Item reinforced : List.of(ModItems.REINFORCED_FURNACE, ModItems.REINFORCED_SMOKER,
                ModItems.REINFORCED_BLAST_FURNACE, ModItems.REINFORCED_PISTON)) {
            assertNoCraftingRecipe(helper, bulkPattern, Map.of('N', ModItems.NETHERITE_NUGGET, 'R', reinforced),
                    "the old netherite 2x2 with " + BuiltInRegistries.ITEM.getKey(reinforced), shapes);
        }

        // And no crafting recipe anywhere produces a netherite or enderite machine.
        Set<Item> worldOnly = Set.of(
                ModItems.NETHERITE_HOPPER, ModItems.NETHERITE_FURNACE, ModItems.NETHERITE_SMOKER,
                ModItems.NETHERITE_BLAST_FURNACE, ModItems.NETHERITE_PISTON,
                ModItems.ENDERITE_HOPPER, ModItems.ENDERITE_FURNACE, ModItems.ENDERITE_SMOKER,
                ModItems.ENDERITE_BLAST_FURNACE, ModItems.ENDERITE_PISTON);
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value().getType() != RecipeType.CRAFTING) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                for (ItemStack stack : display.result().resolveForStacks(displayContext)) {
                    if (worldOnly.contains(stack.getItem())) {
                        shapes.add(holder.id().identifier() + " crafts " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                                + ", which is meant to be made with the sledgehammer only");
                    }
                }
            }
        }

        helper.assertTrue(shapes.isEmpty(), "recipe shape problems: " + shapes);
        TestCleanup.succeed(helper);
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

        // 1.21.11: assemble still takes the registry access as a second argument.
        ItemStack crafted = matched.get().value().assemble(input, helper.getLevel().registryAccess());
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

        ItemStack smithed = matched.get().value().assemble(input, helper.getLevel().registryAccess());
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
        TestCleanup.succeed(helper);
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
            TestCleanup.succeed(helper);
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
     * {@code simplebuilding:deep_pockets_enchantable} to {@code minecraft:pickaxes} leaves a non-empty
     * tag with nothing but bound holders behind. {@link #SUPPORTED_ITEM_TAGS} therefore names the
     * tag each enchantment hangs on, and the check reads it back off the loaded enchantment.
     *
     * <p>The tag key, not its contents: what is <em>in</em> those tags is
     * {@code ModItemTagProvider}'s business and is pinned where the behaviour is - see
     * {@link BundleWiringTests#containerEnchantmentsAcceptTheBundlesTheyAreMeantFor} for the
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
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
    }

    /** Exactly the test {@code EnderiteItemMixin} performs on the dropped stack. */
    private static boolean isVoidProtected(ItemStack stack) {
        return stack.is(ModTags.Items.VOID_PROTECTED);
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
        TestCleanup.before(helper, entity::discard);
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

    /**
     * Holds the datagen source against the files it produced.
     *
     * <p><strong>Why this exists.</strong> {@code ModEnchantments#bootstrap} is only ever called
     * from {@code SimplebuildingDataGenerator}. Nothing at runtime reads that Java: the server
     * loads {@code src/main/generated/data/simplebuilding/enchantment/*.json}, and no Gradle
     * dependency ties {@code runDatagen} to {@code runGametest}. So editing a weight, an anvil
     * fee or a cost curve in the source and forgetting to re-run datagen changes nothing in the
     * game, and nothing says so - the build is green, the tests are green, and the number the
     * player meets is the old one. That is the exact shape of a silent regression, and it was
     * found by the adversarial pass of the 2026-09-08 audit rather than by any test.
     *
     * <p><strong>How.</strong> {@code BootstrapContext} is an interface with two methods, so the
     * test can play the datagen's part: it records every registration and answers {@code lookup}
     * from the running server's registries. Both sides are then encoded through the same
     * {@code Enchantment.DIRECT_CODEC} and compared as json, which makes any difference - a
     * number, a tag, an effect, a slot - show up as a readable diff instead of a boolean.
     *
     * <p><strong>Not covered:</strong> everything else datagen writes. Recipes, loot tables, tags
     * and models have the same exposure, and this only closes the enchantments. It is the file
     * set where a silent change hurts most, because the numbers in it are balance the player
     * meets at every enchanting table, but the gap is real and named here on purpose.
     *
     * <p><strong>What breaks this test:</strong> a change to {@code ModEnchantments} without a
     * datagen run; a hand-edited file under {@code generated/}; an enchantment added to the source
     * but never generated, or generated once and later dropped from the source.
     */
    public static void generatedEnchantmentFilesStillMatchTheirSource(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Registry<Enchantment> loaded = registries.lookupOrThrow(Registries.ENCHANTMENT);
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        Map<ResourceKey<Enchantment>, Enchantment> fromSource = new LinkedHashMap<>();
        ModEnchantments.bootstrap(new BootstrapContext<Enchantment>() {
            @Override
            public Holder.Reference<Enchantment> register(ResourceKey<Enchantment> key,
                                                          Enchantment value, Lifecycle lifecycle) {
                fromSource.put(key, value);
                // ModEnchantments never reads this back - see its private register(...) helper -
                // and a Holder.Reference cannot be built from outside net.minecraft.core, so null
                // is the honest answer rather than a fabricated holder that would lie if used.
                return null;
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
                return registries.lookupOrThrow(key);
            }
        });

        helper.assertTrue(fromSource.size() >= MIN_SOURCE_ENCHANTMENTS,
                "the datagen source registered only " + fromSource.size() + " enchantments, fewer "
                        + "than the " + MIN_SOURCE_ENCHANTMENTS + " this test expects to walk - "
                        + "with an empty recording the comparison below would pass while checking "
                        + "nothing");

        List<String> problems = new ArrayList<>();

        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : fromSource.entrySet()) {
            ResourceKey<Enchantment> key = entry.getKey();
            if (!loaded.containsKey(key)) {
                problems.add(key.identifier() + " is built by the datagen source but no generated "
                        + "file for it reached the server");
                continue;
            }
            JsonElement source = encodeEnchantment(helper, ops, entry.getValue(), key, "the source");
            JsonElement file = encodeEnchantment(helper, ops, loaded.getValueOrThrow(key), key, "the generated file");
            if (!source.equals(file)) {
                problems.add(key.identifier() + ": the generated file says " + file
                        + " but ModEnchantments builds " + source
                        + " - run the datagen, or revert the source change");
            }
        }

        // The other direction: a file that outlived its source entry keeps working in game and
        // would never be noticed by the loop above.
        for (ResourceKey<Enchantment> key : loaded.registryKeySet()) {
            if (key.identifier().getNamespace().equals(MOD_ID) && !fromSource.containsKey(key)) {
                problems.add(key.identifier() + " is loaded from a generated file, but the datagen "
                        + "source no longer builds it - the file is an orphan");
            }
        }

        helper.assertTrue(problems.isEmpty(),
                "datagen source and generated enchantment files have drifted apart: " + problems);

        TestCleanup.succeed(helper);
    }

    /** Encodes one enchantment for the comparison, naming which side failed if the codec balks. */
    private static JsonElement encodeEnchantment(GameTestHelper helper, RegistryOps<JsonElement> ops,
                                                 Enchantment enchantment, ResourceKey<Enchantment> key,
                                                 String side) {
        return Enchantment.DIRECT_CODEC.encodeStart(ops, enchantment)
                .getOrThrow(message -> helper.assertionException(
                        "could not encode " + key.identifier() + " from " + side + ": " + message));
    }

    /**
     * All seven quartz checkers - purpur, lapis, blackstone, resin, nihilith, astralit and ender
     * quartz - are mined with a pickaxe and drop themselves. They copy blocks that need the right
     * tool, so without {@code minecraft:mineable/pickaxe} breaking one gave nothing. The two new
     * ones are crafted like the others, two of the material diagonal to two quartz blocks, four at
     * a time: nihilith shards, astralit dust and ender quartz stand in for the coloured block.
     *
     * <p>What breaks this test: a checker missing from the pickaxe tag or its loot table, and a
     * missing or changed recipe for the new checkers.
     */
    public static void quartzCheckersAreMinedByPickaxeAndCraftedFromTheirMaterial(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        StringBuilder actual = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (Block checker : List.of(ModBlocks.PURPUR_QUARTZ_CHECKER, ModBlocks.LAPIS_QUARTZ_CHECKER,
                ModBlocks.BLACKSTONE_QUARTZ_CHECKER, ModBlocks.RESIN_QUARTZ_CHECKER,
                ModBlocks.NIHILITH_QUARTZ_CHECKER, ModBlocks.ASTRALIT_QUARTZ_CHECKER, ModBlocks.ENDER_QUARTZ_CHECKER)) {
            BlockState state = checker.defaultBlockState();
            Identifier id = BuiltInRegistries.BLOCK.getKey(checker);
            StringBuilder drops = new StringBuilder();
            for (ItemStack drop : Block.getDrops(state, level, helper.absolutePos(new BlockPos(1, 1, 1)), null, null, pickaxe)) {
                drops.append(drop.getCount()).append(' ').append(BuiltInRegistries.ITEM.getKey(drop.getItem()));
            }
            actual.append(id).append(": ").append(pickaxe.isCorrectToolForDrops(state) ? "pickaxe" : "no tool")
                    .append(", ").append(drops).append("; ");
            expected.append(id).append(": pickaxe, 1 ").append(id).append("; ");
        }
        Assertions.valueEqual(helper, actual.toString(), expected.toString(), "how the quartz checkers are mined");

        ItemStack quartz = new ItemStack(Items.QUARTZ_BLOCK);
        Map<Item, String> checkerOf = new LinkedHashMap<>();
        checkerOf.put(ModItems.NIHILITH_SHARD, "simplebuilding:nihilith_quartz_checker");
        checkerOf.put(ModItems.ASTRALIT_DUST, "simplebuilding:astralit_quartz_checker");
        checkerOf.put(ModItems.ENDER_QUARTZ, "simplebuilding:ender_quartz_checker");
        for (Item material : checkerOf.keySet()) {
            ItemStack m = new ItemStack(material);
            CraftingInput grid = CraftingInput.of(2, 2, List.of(m, quartz, quartz, m));
            Optional<RecipeHolder<CraftingRecipe>> match =
                    level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(match.isPresent(), BuiltInRegistries.ITEM.getKey(material) + " and quartz blocks craft nothing");
            ItemStack result = match.get().value().assemble(grid, level.registryAccess());
            Assertions.valueEqual(helper, result.getCount() + " " + BuiltInRegistries.ITEM.getKey(result.getItem()),
                    "4 " + checkerOf.get(material),
                    "what " + BuiltInRegistries.ITEM.getKey(material) + " diagonal to two quartz blocks crafts");
        }
        TestCleanup.succeed(helper);
    }

    /**
     * The three end palettes - astralit, nihilith and ender quartz - are crafted, cut, mined and
     * tagged like vanilla's end stone and purpur families. Each palette has eleven blocks: the
     * base block, bricks with stairs, slab and wall, the polished block with stairs, slab and wall,
     * a pillar and chiseled bricks. Ender quartz has thirteen: like the quartz block with
     * {@code quartz_stairs} and {@code quartz_slab}, its base block has stairs and a slab of its own
     * (six in a stair shape make four, three in a row six, the stonecutter one and two).
     *
     * <p><b>Crafting</b>, every recipe by its documented pattern and count, through the real
     * recipe manager (so a pattern that crafts something else shows up too): four of the material
     * in a square make one base block (like quartz and amethyst), four base blocks four polished,
     * four polished four bricks (the deepslate chain), two polished on top of each other two
     * pillars, six bricks or polished four stairs, three six slabs, six six walls, and two brick
     * slabs one chiseled block. <b>Stonecutting</b>, read from the loaded stonecutter recipes'
     * displays so a missing, doubled or miscounted cut shows up: the base block cuts into all ten
     * others, the polished block into its stairs, slab and wall, the bricks family, the pillar and
     * the chiseled bricks, the bricks into their stairs, slab, wall and the chiseled bricks - slabs
     * always two at a time. The older coated end stone still cuts into the brick set and into the
     * base block, the coated purpur block into the polished block.
     *
     * <p><b>Mining</b>: each block needs the right tool, an iron pickaxe is that tool (the
     * {@code minecraft:mineable/pickaxe} tag), and what it breaks drops the block itself - a double
     * slab two slabs. The five older end stone family blocks are asserted with them: they copy
     * polished end stone or purpur, so they need a pickaxe too, and until 2026-09 they were missing
     * from the pickaxe tag and dropped nothing at all.
     *
     * <p><b>Tags and light</b>: both stairs, both slabs and both walls of each palette sit in the
     * vanilla block and item tags of their shape (a wall outside {@code minecraft:walls} does not
     * connect to its neighbours), and every astralit block glows at 10 like the coated astralit
     * blocks while nihilith and ender quartz stay dark.
     *
     * <p>What breaks this: a removed or changed recipe, a stonecutter cut that went missing,
     * appeared or changed its count, a block dropped from the pickaxe tag or its shape tag, a slab
     * that drops one item as a double slab, a light level that no longer follows the material.
     */
    public static void endBrickSetsAreCraftedCutMinedAndTaggedLikeVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipeManager = level.getServer().getRecipeManager();
        Map<Identifier, RecipeHolder<?>> modRecipes = modRecipes(recipeManager);
        List<String> problems = new ArrayList<>();

        // Stonecutter cuts as "input -> count result", from the loaded recipes' displays.
        ContextMap displayContext = SlotDisplayContext.fromLevel(level);
        Set<String> cuts = new TreeSet<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe) || !MOD_ID.equals(holder.id().identifier().getNamespace())) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                if (!(display instanceof StonecutterRecipeDisplay cut)) {
                    continue;
                }
                for (ItemStack in : cut.input().resolveForStacks(displayContext)) {
                    for (ItemStack out : cut.result().resolveForStacks(displayContext)) {
                        cuts.add(BuiltInRegistries.ITEM.getKey(in.getItem()).getPath() + " -> " + out.getCount() + " "
                                + BuiltInRegistries.ITEM.getKey(out.getItem()).getPath());
                    }
                }
            }
        }

        Set<String> expectedCuts = new TreeSet<>();
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        StringBuilder mined = new StringBuilder();
        StringBuilder minedExpected = new StringBuilder();
        TagKey<Item> stairsItems = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("stairs"));
        TagKey<Item> slabItems = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("slabs"));
        TagKey<Item> wallItems = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("walls"));

        for (EndPaletteMaterial material : END_PALETTE_MATERIALS) {
            ModBlocks.EndPalette p = material.palette();
            String m = p.material();
            Item block = p.block().asItem();
            Item polished = p.polished().asItem();
            Item bricks = p.bricks().asItem();
            String[] square = {"##", "##"};
            assertShapedRecipe(helper, modRecipes, m + "_block", block, 1, square, Map.of('#', material.item()), problems);
            assertShapedRecipe(helper, modRecipes, "polished_" + m, polished, 4, square, Map.of('#', block), problems);
            assertShapedRecipe(helper, modRecipes, m + "_bricks", bricks, 4, square, Map.of('#', polished), problems);
            assertShapedRecipe(helper, modRecipes, m + "_pillar", p.pillar().asItem(), 2, new String[]{"#", "#"},
                    Map.of('#', polished), problems);
            for (Block[] family : new Block[][]{
                    {p.bricks(), p.brickStairs(), p.brickSlab(), p.brickWall()},
                    {p.polished(), p.polishedStairs(), p.polishedSlab(), p.polishedWall()}}) {
                Item full = family[0].asItem();
                assertShapedRecipe(helper, modRecipes, path(family[1]), family[1].asItem(), 4,
                        new String[]{"#  ", "## ", "###"}, Map.of('#', full), problems);
                assertShapedRecipe(helper, modRecipes, path(family[2]), family[2].asItem(), 6, new String[]{"###"},
                        Map.of('#', full), problems);
                assertShapedRecipe(helper, modRecipes, path(family[3]), family[3].asItem(), 6,
                        new String[]{"###", "###"}, Map.of('#', full), problems);
            }
            assertShapedRecipe(helper, modRecipes, "chiseled_" + m + "_bricks", p.chiseled().asItem(), 1,
                    new String[]{"#", "#"}, Map.of('#', p.brickSlab().asItem()), problems);
            helper.assertTrue(p.blocks().size() == (p == ModBlocks.ENDER_QUARTZ_PALETTE ? 13 : 11)
                            && (p.blockStairs() == null) == (p != ModBlocks.ENDER_QUARTZ_PALETTE),
                    m + ": ender quartz alone has stairs and a slab at the base block (13 blocks), the others 11 - but "
                            + p.blocks().size());
            if (p.blockStairs() != null) {
                // like quartz_stairs and quartz_slab from the quartz block
                assertShapedRecipe(helper, modRecipes, path(p.blockStairs()), p.blockStairs().asItem(), 4,
                        new String[]{"#  ", "## ", "###"}, Map.of('#', block), problems);
                assertShapedRecipe(helper, modRecipes, path(p.blockSlab()), p.blockSlab().asItem(), 6, new String[]{"###"},
                        Map.of('#', block), problems);
            }

            // block -> everything, polished -> its family + bricks family + pillar + chiseled,
            // bricks -> their family + chiseled
            List<Block> fromPolished = List.of(p.polishedStairs(), p.polishedSlab(), p.polishedWall(), p.bricks(),
                    p.brickStairs(), p.brickSlab(), p.brickWall(), p.pillar(), p.chiseled());
            for (Block cut : p.blocks()) {
                if (cut != p.block()) {
                    expectCut(expectedCuts, p.block(), cut, p);
                }
            }
            fromPolished.forEach(cut -> expectCut(expectedCuts, p.polished(), cut, p));
            for (Block cut : List.of(p.brickStairs(), p.brickSlab(), p.brickWall(), p.chiseled())) {
                expectCut(expectedCuts, p.bricks(), cut, p);
            }

            for (Block each : p.blocks()) {
                describeMining(helper, level, at, pickaxe, each.defaultBlockState(), mined);
                expectMining(each, material.light(), 1, minedExpected);
            }
            for (Block slab : p.slabs()) {
                BlockState doubleSlab = slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
                describeMining(helper, level, at, pickaxe, doubleSlab, mined);
                expectMining(slab, material.light(), 2, minedExpected);
            }

            StringBuilder shapes = new StringBuilder();
            for (Block stairs : p.stairs()) {
                shapes.append(stairs.defaultBlockState().is(BlockTags.STAIRS) ? "stairs " : "")
                        .append(stairs.asItem().builtInRegistryHolder().is(stairsItems) ? "stairs-item " : "");
            }
            for (Block slab : p.slabs()) {
                shapes.append(slab.defaultBlockState().is(BlockTags.SLABS) ? "slabs " : "")
                        .append(slab.asItem().builtInRegistryHolder().is(slabItems) ? "slabs-item " : "");
            }
            for (Block wall : p.walls()) {
                shapes.append(wall.defaultBlockState().is(BlockTags.WALLS) ? "walls " : "")
                        .append(wall.asItem().builtInRegistryHolder().is(wallItems) ? "walls-item " : "");
            }
            Assertions.valueEqual(helper, shapes.toString().trim(),
                    ("stairs stairs-item ".repeat(p.stairs().size()) + "slabs slabs-item ".repeat(p.slabs().size())
                            + "walls walls-item ".repeat(p.walls().size())).trim(),
                    "the vanilla shape tags the " + m + " stairs, slab and wall belong to");
        }

        // The coated end stone opened the astralit and nihilith brick sets before the palettes had a
        // base block; its cuts stay, and it cuts into the base block too. The coated purpur block
        // cuts into the polished block.
        for (Block[] coated : new Block[][]{
                {ModBlocks.ASTRAL_END_STONE, ModBlocks.ASTRAL_PURPUR_BLOCK},
                {ModBlocks.NIHIL_END_STONE, ModBlocks.NIHIL_PURPUR_BLOCK}}) {
            ModBlocks.EndPalette p = coated[0] == ModBlocks.ASTRAL_END_STONE ? ModBlocks.ASTRALIT_PALETTE : ModBlocks.NIHILITH_PALETTE;
            for (Block cut : List.of(p.block(), p.bricks(), p.brickStairs(), p.brickSlab(), p.brickWall(), p.pillar(), p.chiseled())) {
                expectCut(expectedCuts, coated[0], cut, p);
            }
            expectCut(expectedCuts, coated[1], p.polished(), p);
        }

        for (Block older : List.of(ModBlocks.POLISHED_END_STONE, ModBlocks.ASTRAL_END_STONE, ModBlocks.NIHIL_END_STONE,
                ModBlocks.ASTRAL_PURPUR_BLOCK, ModBlocks.NIHIL_PURPUR_BLOCK)) {
            describeMining(helper, level, at, pickaxe, older.defaultBlockState(), mined);
            expectMining(older, older.defaultBlockState().getLightEmission(), 1, minedExpected);
        }

        Set<String> missingCuts = new TreeSet<>(expectedCuts);
        missingCuts.removeAll(cuts);
        Set<String> strayCuts = new TreeSet<>();
        for (String cut : cuts) {
            if ((cut.contains("astral") || cut.contains("nihil") || cut.contains("ender_quartz")) && !expectedCuts.contains(cut)) {
                strayCuts.add(cut);
            }
        }
        if (!missingCuts.isEmpty()) {
            problems.add("stonecutter cuts missing: " + missingCuts);
        }
        if (!strayCuts.isEmpty()) {
            problems.add("unexpected end palette stonecutter cuts: " + strayCuts);
        }

        Assertions.valueEqual(helper, mined.toString(), minedExpected.toString(), "how the end stone family is mined");
        helper.assertTrue(problems.isEmpty(), "end brick set recipes: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * Every block of the three end palettes that has a vanilla end stone or purpur counterpart is
     * recoloured at the crafting table the way dye recolours glass or terracotta: eight of the
     * vanilla block around one of the material make eight of the palette block. End stone becomes
     * the base block, end stone bricks, their stairs, slab and wall the palette's bricks, stairs,
     * slab and wall, purpur stairs and slab the polished stairs and slab, the purpur pillar the
     * pillar.
     *
     * <p>The purpur block itself is recoloured into polished ender quartz only. For astralit and
     * nihilith the same eight purpur blocks around the dust or shard have been the coating recipe
     * of the astral and nihil purpur block for a long time; a second recipe on the same grid would
     * make one of the two unreachable, so the test pins that the coating recipe still wins there.
     * (The coated purpur block then cuts into the polished block, see
     * {@link #endBrickSetsAreCraftedCutMinedAndTaggedLikeVanilla}.)
     *
     * <p>Each grid goes through the real recipe manager, which also proves that no two recipes
     * compete for it.
     *
     * <p>What breaks this: a recolour recipe that is missing, yields another block or another
     * count than eight, takes another material, or a new recipe that takes over the purpur coating
     * grid.
     */
    public static void endPalettesAreRecolouredFromEndStoneAndPurpurLikeDye(GameTestHelper helper) {
        Map<Identifier, RecipeHolder<?>> modRecipes = modRecipes(helper.getLevel().getServer().getRecipeManager());
        List<String> problems = new ArrayList<>();
        String[] ring = {"###", "#M#", "###"};
        int recipes = 0;
        for (EndPaletteMaterial material : END_PALETTE_MATERIALS) {
            ModBlocks.EndPalette p = material.palette();
            Map<Item, Block> pairs = new LinkedHashMap<>();
            pairs.put(Items.END_STONE, p.block());
            pairs.put(Items.END_STONE_BRICKS, p.bricks());
            pairs.put(Items.END_STONE_BRICK_STAIRS, p.brickStairs());
            pairs.put(Items.END_STONE_BRICK_SLAB, p.brickSlab());
            pairs.put(Items.END_STONE_BRICK_WALL, p.brickWall());
            if (p == ModBlocks.ENDER_QUARTZ_PALETTE) {
                pairs.put(Items.PURPUR_BLOCK, p.polished());
            }
            pairs.put(Items.PURPUR_STAIRS, p.polishedStairs());
            pairs.put(Items.PURPUR_SLAB, p.polishedSlab());
            pairs.put(Items.PURPUR_PILLAR, p.pillar());
            for (Map.Entry<Item, Block> pair : pairs.entrySet()) {
                assertShapedRecipe(helper, modRecipes,
                        path(pair.getValue()) + "_from_" + BuiltInRegistries.ITEM.getKey(pair.getKey()).getPath(),
                        pair.getValue().asItem(), 8, ring, Map.of('#', pair.getKey(), 'M', material.item()), problems);
                recipes++;
            }
        }
        assertShapedRecipe(helper, modRecipes, "astral_purpur_block", ModItems.ASTRAL_PURPUR_BLOCK, 8, ring,
                Map.of('#', Items.PURPUR_BLOCK, 'M', ModItems.ASTRALIT_DUST), problems);
        assertShapedRecipe(helper, modRecipes, "nihil_purpur_block", ModItems.NIHIL_PURPUR_BLOCK, 8, ring,
                Map.of('#', Items.PURPUR_BLOCK, 'M', ModItems.NIHILITH_SHARD), problems);

        Assertions.valueEqual(helper, recipes, 25, "recolour recipes checked (8 + 8 + 9)");
        helper.assertTrue(problems.isEmpty(), "end palette recolouring: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The ender quartz palette is also recoloured from vanilla quartz, the same way dye recolours
     * glass: eight of a quartz block around one ender quartz make eight of the matching palette
     * block. The quartz block becomes the base block, quartz bricks the bricks, the quartz pillar
     * the pillar, the chiseled quartz block the chiseled bricks and smooth quartz the polished
     * block. Smooth quartz stairs and slab become the polished stairs and slab, quartz stairs and
     * slab the ender quartz stairs and slab of the base block - the same pairing vanilla has.
     *
     * <p>Astralit and nihilith are not made from quartz: the same ring around their dust or shard
     * must craft nothing, or quartz would turn into a palette that has nothing to do with it.
     *
     * <p>Each grid goes through the real recipe manager, which also proves that no other recipe
     * takes the same grid.
     *
     * <p>What breaks this: a recolour recipe that is missing, yields another block or another
     * count than eight, takes another material, or competes with another recipe on its grid.
     */
    public static void enderQuartzPaletteIsRecolouredFromQuartzLikeDye(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipeManager = level.getServer().getRecipeManager();
        Map<Identifier, RecipeHolder<?>> modRecipes = modRecipes(recipeManager);
        List<String> problems = new ArrayList<>();
        String[] ring = {"###", "#M#", "###"};
        ModBlocks.EndPalette p = ModBlocks.ENDER_QUARTZ_PALETTE;
        Map<Item, Block> pairs = new LinkedHashMap<>();
        pairs.put(Items.QUARTZ_BLOCK, p.block());
        pairs.put(Items.QUARTZ_BRICKS, p.bricks());
        pairs.put(Items.QUARTZ_PILLAR, p.pillar());
        pairs.put(Items.CHISELED_QUARTZ_BLOCK, p.chiseled());
        pairs.put(Items.SMOOTH_QUARTZ, p.polished());
        pairs.put(Items.SMOOTH_QUARTZ_STAIRS, p.polishedStairs());
        pairs.put(Items.SMOOTH_QUARTZ_SLAB, p.polishedSlab());
        pairs.put(Items.QUARTZ_STAIRS, p.blockStairs());
        pairs.put(Items.QUARTZ_SLAB, p.blockSlab());
        for (Map.Entry<Item, Block> pair : pairs.entrySet()) {
            assertShapedRecipe(helper, modRecipes,
                    path(pair.getValue()) + "_from_" + BuiltInRegistries.ITEM.getKey(pair.getKey()).getPath(),
                    pair.getValue().asItem(), 8, ring, Map.of('#', pair.getKey(), 'M', ModItems.ENDER_QUARTZ), problems);
            for (Item other : List.of(ModItems.ASTRALIT_DUST, ModItems.NIHILITH_SHARD)) {
                CraftingInput grid = grid(ring, Map.of('#', pair.getKey(), 'M', other));
                Optional<RecipeHolder<CraftingRecipe>> match = recipeManager.getRecipeFor(RecipeType.CRAFTING, grid, level);
                if (match.isPresent()) {
                    problems.add("eight " + BuiltInRegistries.ITEM.getKey(pair.getKey()) + " around "
                            + BuiltInRegistries.ITEM.getKey(other) + " craft " + match.get().id().identifier());
                }
            }
        }

        Assertions.valueEqual(helper, pairs.size(), 9, "quartz recolour recipes checked");
        helper.assertTrue(problems.isEmpty(), "ender quartz from quartz: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The Basic Upgrade costs twice the material the crafting table asks for the target
     * tool (the owner's decision): a pickaxe or an axe takes 6, a sword or a hoe 4, a shovel 2 -
     * the vanilla recipes use 3, 2 and 1; the copper tools go to iron at the same prices. The mod's
     * tools follow the same rule: the chisel takes 2 (one ingot or diamond in its recipe) and the
     * sledgehammer 22 (one block plus two ingots, eleven ingots' worth). The building wand is the
     * exception (owner's decision): a wand is made from a core, a late-game item, so its upgrade
     * costs exactly one core of the target tier instead of ingots.
     *
     * <p>Every rung is walked through the real recipe manager: the priced stack forges the next
     * tier with this very recipe, one item fewer forges nothing. Then every count based smithing
     * recipe the mod loads must be in this table, so an upgrade added later cannot slip in at a
     * price nobody decided.
     *
     * <p>What breaks this: a changed count, a missing rung (hoes, the copper axe, shovel, sword and
     * hoe and the building wands were missing until 2026-09-25), an upgrade that takes another
     * material or yields another tool, or a new count based recipe that is not priced here.
     */
    public static void basicUpgradeTemplateCostsTwiceTheCraftingMaterial(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipeManager = level.getServer().getRecipeManager();
        Map<Identifier, RecipeHolder<?>> modRecipes = modRecipes(recipeManager);
        List<String> problems = new ArrayList<>();
        Map<Item[], Integer> ladder = new LinkedHashMap<>();
        for (Item[] tiers : new Item[][]{
                {Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE},
                {Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE}}) {
            addLadder(ladder, tiers, true, 6);
        }
        ladder.put(new Item[]{Items.COPPER_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT}, 6);
        ladder.put(new Item[]{Items.COPPER_AXE, Items.IRON_AXE, Items.IRON_INGOT}, 6);
        ladder.put(new Item[]{Items.COPPER_SWORD, Items.IRON_SWORD, Items.IRON_INGOT}, 4);
        ladder.put(new Item[]{Items.COPPER_HOE, Items.IRON_HOE, Items.IRON_INGOT}, 4);
        ladder.put(new Item[]{Items.COPPER_SHOVEL, Items.IRON_SHOVEL, Items.IRON_INGOT}, 2);
        for (Item[] tiers : new Item[][]{
                {Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD},
                {Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE}}) {
            addLadder(ladder, tiers, true, 4);
        }
        addLadder(ladder, new Item[]{Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL,
                Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL}, true, 2);
        addLadder(ladder, new Item[]{ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                ModItems.DIAMOND_CHISEL}, false, 2);
        addLadder(ladder, new Item[]{ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER}, false, 22);
        ladder.put(new Item[]{ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.IRON_CORE}, 1);
        ladder.put(new Item[]{ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND, ModItems.GOLD_CORE}, 1);
        ladder.put(new Item[]{ModItems.GOLD_BUILDING_WAND, ModItems.DIAMOND_BUILDING_WAND, ModItems.DIAMOND_CORE}, 1);

        Set<Identifier> priced = new HashSet<>();
        for (Map.Entry<Item[], Integer> rung : ladder.entrySet()) {
            Item base = rung.getKey()[0];
            Item result = rung.getKey()[1];
            Item material = rung.getKey()[2];
            int count = rung.getValue();
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "upgrade_"
                    + BuiltInRegistries.ITEM.getKey(base).getPath() + "_to_" + BuiltInRegistries.ITEM.getKey(result).getPath());
            priced.add(id);
            RecipeHolder<?> holder = modRecipes.get(id);
            if (holder == null || !(holder.value() instanceof CountBasedSmithingRecipe recipe)) {
                problems.add(id + " is " + (holder == null ? "not loaded" : "not a count based smithing recipe"));
                continue;
            }
            if (recipe.getAdditionCount() != count) {
                problems.add(id + " takes " + recipe.getAdditionCount() + " instead of " + count);
            }
            if (!recipe.getResultStack().is(result)) {
                problems.add(id + " yields " + recipe.getResultStack() + " instead of " + result);
            }
            SmithingRecipeInput enough = new SmithingRecipeInput(new ItemStack(ModItems.BASIC_UPGRADE_TEMPLATE),
                    new ItemStack(base), new ItemStack(material, count));
            Optional<RecipeHolder<SmithingRecipe>> match = recipeManager.getRecipeFor(RecipeType.SMITHING, enough, level);
            if (match.isEmpty() || !match.get().id().identifier().equals(id)) {
                problems.add(count + " " + material + " on " + base + " forge "
                        + (match.isEmpty() ? "nothing" : match.get().id().identifier().toString()) + " instead of " + id);
            }
            SmithingRecipeInput oneShort = new SmithingRecipeInput(new ItemStack(ModItems.BASIC_UPGRADE_TEMPLATE),
                    new ItemStack(base), new ItemStack(material, count - 1));
            if (recipeManager.getRecipeFor(RecipeType.SMITHING, oneShort, level).isPresent()) {
                problems.add((count - 1) + " " + material + " on " + base + " already forge something");
            }
        }
        for (RecipeHolder<?> holder : modRecipes.values()) {
            if (holder.value() instanceof CountBasedSmithingRecipe && !priced.contains(holder.id().identifier())) {
                problems.add(holder.id().identifier() + " is a count based upgrade nobody priced here");
            }
        }

        Assertions.valueEqual(helper, priced.size(), 34, "upgrade rungs checked (pickaxe 5, axe 5, sword 5, hoe 5, shovel 5, chisel 3, sledgehammer 3, wand 3)");
        helper.assertTrue(problems.isEmpty(), "basic upgrade template costs: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * Adds the rungs of one tool ladder: each tier upgrades into the next with the material of
     * the next. {@code vanilla} picks cobblestone, iron, gold, diamond (wood - stone - iron - gold -
     * diamond); otherwise iron, gold, diamond (copper - iron - gold - diamond).
     */
    private static void addLadder(Map<Item[], Integer> ladder, Item[] tiers, boolean vanilla, int count) {
        Item[] materials = vanilla
                ? new Item[]{Items.COBBLESTONE, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND}
                : new Item[]{Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND};
        for (int i = 0; i + 1 < tiers.length; i++) {
            ladder.put(new Item[]{tiers[i], tiers[i + 1], materials[i]}, count);
        }
    }

    /**
     * Ender quartz, the material of the purple palette: one astralit dust, one nihilith shard and
     * one quartz, anywhere in the grid (shapeless), make two. Four of them in a square make the
     * palette's base block.
     *
     * <p>Two different arrangements go through the real recipe manager, so the recipe is shown to
     * be shapeless rather than merely matching one layout; the loaded recipe must also be a
     * shapeless one. Each of the three ingredients is then left out once: two of three must craft
     * nothing, or the recipe would hand out ender quartz for less than it costs.
     *
     * <p>What breaks this: a changed count, a changed or dropped ingredient, a recipe that became
     * shaped, or the item missing from the materials tab's registry.
     */
    public static void enderQuartzIsCraftedFromAstralitDustNihilithShardAndQuartz(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipeManager = level.getServer().getRecipeManager();
        Map<Identifier, RecipeHolder<?>> modRecipes = modRecipes(recipeManager);
        List<String> problems = new ArrayList<>();
        Map<Character, Item> key = Map.of('A', ModItems.ASTRALIT_DUST, 'N', ModItems.NIHILITH_SHARD, 'Q', Items.QUARTZ);

        assertShapedRecipe(helper, modRecipes, "ender_quartz", ModItems.ENDER_QUARTZ, 2, new String[]{"ANQ"}, key, problems);
        assertShapedRecipe(helper, modRecipes, "ender_quartz", ModItems.ENDER_QUARTZ, 2,
                new String[]{"Q  ", "   ", " NA"}, key, problems);
        RecipeHolder<?> holder = modRecipes.get(Identifier.fromNamespaceAndPath(MOD_ID, "ender_quartz"));
        helper.assertTrue(holder != null && holder.value() instanceof ShapelessRecipe,
                "simplebuilding:ender_quartz should be a shapeless crafting recipe, but it is "
                        + (holder == null ? "missing" : holder.value().getClass().getSimpleName()));

        for (String twoOfThree : List.of("NQ", "AQ", "AN")) {
            CraftingInput grid = grid(new String[]{twoOfThree}, key);
            Optional<RecipeHolder<CraftingRecipe>> match = recipeManager.getRecipeFor(RecipeType.CRAFTING, grid, level);
            if (match.isPresent()) {
                problems.add("only " + twoOfThree + " (A dust, N shard, Q quartz) already crafts " + match.get().id().identifier());
            }
        }

        assertShapedRecipe(helper, modRecipes, "ender_quartz_block", ModItems.ENDER_QUARTZ_BLOCK, 1,
                new String[]{"##", "##"}, Map.of('#', ModItems.ENDER_QUARTZ), problems);
        helper.assertTrue(problems.isEmpty(), "ender quartz recipes: " + problems);
        TestCleanup.succeed(helper);
    }

    /** The mod's own loaded recipes by id. */
    private static Map<Identifier, RecipeHolder<?>> modRecipes(RecipeManager recipeManager) {
        Map<Identifier, RecipeHolder<?>> modRecipes = new HashMap<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (MOD_ID.equals(holder.id().identifier().getNamespace())) {
                modRecipes.put(holder.id().identifier(), holder);
            }
        }
        return modRecipes;
    }

    /** A palette with the item it is made from and the light its blocks give off. */
    private record EndPaletteMaterial(ModBlocks.EndPalette palette, Item item, int light) {
    }

    private static final List<EndPaletteMaterial> END_PALETTE_MATERIALS = List.of(
            new EndPaletteMaterial(ModBlocks.ASTRALIT_PALETTE, ModItems.ASTRALIT_DUST, 10),
            new EndPaletteMaterial(ModBlocks.NIHILITH_PALETTE, ModItems.NIHILITH_SHARD, 0),
            new EndPaletteMaterial(ModBlocks.ENDER_QUARTZ_PALETTE, ModItems.ENDER_QUARTZ, 0));

    private static String path(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    /** One stonecutter cut "base -> count result"; slabs come two at a time. */
    private static void expectCut(Set<String> expected, Block base, Block result, ModBlocks.EndPalette p) {
        int count = p.slabs().contains(result) ? 2 : 1;
        expected.add(path(base) + " -> " + count + " " + path(result));
    }

    private static void describeMining(GameTestHelper helper, ServerLevel level, BlockPos at, ItemStack pickaxe,
                                       BlockState state, StringBuilder out) {
        StringBuilder drops = new StringBuilder();
        for (ItemStack drop : Block.getDrops(state, level, at, null, null, pickaxe)) {
            drops.append(drop.getCount()).append(' ').append(BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath());
        }
        out.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath())
                .append(state.requiresCorrectToolForDrops() ? " needs a tool" : " by hand")
                .append(pickaxe.isCorrectToolForDrops(state) ? ", pickaxe" : ", no pickaxe")
                .append(", light ").append(state.getLightEmission())
                .append(", drops ").append(drops).append("; ");
    }

    private static void expectMining(Block block, int light, int count, StringBuilder out) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        out.append(path).append(" needs a tool, pickaxe, light ").append(light)
                .append(", drops ").append(count).append(' ').append(path).append("; ");
    }

    /**
     * The mod's items are split over four creative tabs - tools and enchanting, building blocks,
     * materials, machines and storage - and every one of them sits in exactly one.
     *
     * <p><b>Registration</b>: all four tabs of {@code ModItemGroupsContent.Tab} are in the creative
     * tab registry under {@code simplebuilding:<id>}, titled with their translation key, and no
     * other {@code simplebuilding} tab exists - in particular not the single
     * {@code building_items} tab that held everything before. The registered tab is what the loader
     * built; {@code populate(tab, ...)} is what each loader hands it as content, so the
     * membership below is driven through that.
     *
     * <p><b>Exactly one tab</b>: every registered mod item except the six legacy spatulas
     * ({@link #ITEMS_NOT_IN_THE_CREATIVE_TAB}) comes out of one tab and only once. Two tabs or two
     * entries would put the same item twice into the search tab; none takes it out of creative.
     *
     * <p><b>A sensible tab</b>: a sample of each tab's kind is pinned - the twelve new astralit and
     * nihilith blocks among the building blocks, the ore detector and the enderite armour among the
     * tools, ingots, ores, templates and the two enchanted apples among the materials, hoppers,
     * pistons and every container among machines and storage - and all enchanted books are in the
     * tools tab, none elsewhere.
     *
     * <p>What breaks this: a tab that is not registered, registered twice or under an old id; an
     * item moved into a second tab or listed twice; an item dropped from every tab; books spread
     * into another tab.
     */
    public static void everyModItemIsInExactlyOneCreativeTab(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();

        Set<Identifier> tabIds = new HashSet<>();
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, tab.id);
            tabIds.add(id);
            CreativeModeTab registered = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id);
            if (registered == null) {
                problems.add(id + " is not a registered creative tab");
            } else if (!(registered.getDisplayName().getContents() instanceof TranslatableContents title)
                    || !tab.translationKey().equals(title.getKey())) {
                problems.add(id + " is titled " + registered.getDisplayName() + " instead of " + tab.translationKey());
            }
        }
        // Der Entwickler-Tab ist kein Tab-Enum-Eintrag (nur in Entwicklungsumgebungen oder per Konfig
        // gefuellt, Duplikate erlaubt); sein Inhalt steht in devEnchantedTabOffersEveryExclusiveChoice...
        tabIds.add(Identifier.fromNamespaceAndPath(MOD_ID, com.simplebuilding.items.DevEnchantedTab.ID));
        for (Identifier id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            if (MOD_ID.equals(id.getNamespace()) && !tabIds.contains(id)) {
                problems.add(id + " is a creative tab ModItemGroupsContent.Tab does not know");
            }
        }

        Map<Item, List<ModItemGroupsContent.Tab>> where = new HashMap<>();
        Map<ModItemGroupsContent.Tab, Integer> books = new LinkedHashMap<>();
        Map<ModItemGroupsContent.Tab, Integer> spacers = new LinkedHashMap<>();
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            books.put(tab, 0);
            ModItemGroupsContent.populate(tab, (CreativeModeTab.Output) (stack, visibility) -> {
                if (stack.is(ModItems.CREATIVE_SPACER)) {
                    // Layout-Fueller, kein Angebot: darf mehrfach stehen, aber nie im Suchtab.
                    spacers.merge(tab, 1, Integer::sum);
                    if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
                        problems.add("a creative_spacer in " + tab + " is visible as " + visibility
                                + ", so it shows up in the search tab");
                    }
                } else if (stack.is(Items.ENCHANTED_BOOK)) {
                    books.merge(tab, 1, Integer::sum);
                } else {
                    where.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(tab);
                }
            }, helper.getLevel().registryAccess());
        }

        Set<Identifier> modItems = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (MOD_ID.equals(id.getNamespace())) {
                modItems.add(id);
            }
        }
        // Das Zeilen-Layout nutzen SimpleTools und SimpleMachines.
        if (!spacers.keySet().equals(Set.of(ModItemGroupsContent.Tab.TOOLS, ModItemGroupsContent.Tab.FUNCTIONAL))) {
            problems.add("creative_spacer fills " + spacers.keySet() + " instead of TOOLS and FUNCTIONAL");
        }
        // Bewusst doppelt (Besitzer, 2026-09-25): der Oktant und alle Baustaebe stehen in SimpleTools und
        // in der Zeile Bauplanung von SimpleMachines - genau dort, je einmal. Sonst kein Mod-Item doppelt.
        Set<Item> allowedTwice = Set.of(ModItems.OCTANT, ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND,
                ModItems.GOLD_BUILDING_WAND, ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND,
                ModItems.ENDERITE_BUILDING_WAND);
        for (Identifier id : modItems) {
            if (id.equals(BuiltInRegistries.ITEM.getKey(ModItems.CREATIVE_SPACER))) {
                continue;
            }
            List<ModItemGroupsContent.Tab> tabs = where.getOrDefault(BuiltInRegistries.ITEM.getValue(id), List.of());
            if (allowedTwice.contains(BuiltInRegistries.ITEM.getValue(id))) {
                if (!tabs.equals(List.of(ModItemGroupsContent.Tab.TOOLS, ModItemGroupsContent.Tab.FUNCTIONAL))) {
                    problems.add(id + " belongs once in TOOLS and once in FUNCTIONAL but is in " + tabs);
                }
                continue;
            }
            int expected = ITEMS_NOT_IN_THE_CREATIVE_TAB.contains(id.getPath()) ? 0 : 1;
            if (tabs.size() != expected) {
                problems.add(id + " is offered " + tabs.size() + "x " + tabs + " instead of " + expected + "x");
            }
        }
        // Der Besitzer will neben den Mod-Maschinen auch ihre Vanilla-Vorbilder im Tab Maschinen & Lager
        // sehen - genau diese, genau dort, genau einmal. Jedes andere Vanilla-Item in einem Mod-Tab ist falsch.
        // Dazu der Kartografentisch in der Zeile Bauplanung und, in SimpleTools, die Vanilla-Werkzeuge,
        // -Waffen und -Ruestungen aller Stufen.
        Map<Item, ModItemGroupsContent.Tab> vanillaHome = new HashMap<>();
        for (Item counterpart : List.of(Items.HOPPER, Items.PISTON, Items.STICKY_PISTON,
                Items.FURNACE, Items.SMOKER, Items.BLAST_FURNACE, Items.BUNDLE, Items.CARTOGRAPHY_TABLE)) {
            vanillaHome.put(counterpart, ModItemGroupsContent.Tab.FUNCTIONAL);
        }
        for (String kind : List.of("pickaxe", "shovel", "hoe", "axe", "sword", "spear")) {
            for (String tier : List.of("wooden", "stone", "copper", "iron", "golden", "diamond", "netherite")) {
                vanillaHome.put(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(tier + "_" + kind)), ModItemGroupsContent.Tab.TOOLS);
            }
        }
        for (String kind : List.of("helmet", "chestplate", "leggings", "boots")) {
            for (String tier : List.of("leather", "chainmail", "copper", "iron", "golden", "diamond", "netherite")) {
                vanillaHome.put(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(tier + "_" + kind)), ModItemGroupsContent.Tab.TOOLS);
            }
        }
        if (vanillaHome.size() != 8 + 42 + 28 || vanillaHome.containsKey(Items.AIR)) {
            problems.add("the vanilla tool and armour list names an item that does not exist: " + vanillaHome.size() + " entries");
        }
        Set<Item> vanillaCounterparts = vanillaHome.keySet();
        vanillaHome.forEach((counterpart, home) -> {
            List<ModItemGroupsContent.Tab> tabs = where.getOrDefault(counterpart, List.of());
            if (!tabs.equals(List.of(home))) {
                problems.add(BuiltInRegistries.ITEM.getKey(counterpart) + " belongs once in " + home + " but is in " + tabs);
            }
        });
        for (Item item : where.keySet()) {
            if (!MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()) && !vanillaCounterparts.contains(item)) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is not a mod item but sits in a mod tab");
            }
        }

        Map<ModItemGroupsContent.Tab, List<Item>> pinned = new LinkedHashMap<>();
        pinned.put(ModItemGroupsContent.Tab.BUILDING_BLOCKS, List.of(
                ModItems.ASTRALIT_BRICKS, ModItems.ASTRALIT_BRICK_STAIRS, ModItems.ASTRALIT_BRICK_SLAB,
                ModItems.ASTRALIT_BRICK_WALL, ModItems.ASTRALIT_PILLAR, ModItems.CHISELED_ASTRALIT_BRICKS,
                ModItems.NIHILITH_BRICKS, ModItems.NIHILITH_BRICK_STAIRS, ModItems.NIHILITH_BRICK_SLAB,
                ModItems.NIHILITH_BRICK_WALL, ModItems.NIHILITH_PILLAR, ModItems.CHISELED_NIHILITH_BRICKS,
                ModItems.ASTRALIT_BLOCK, ModItems.POLISHED_NIHILITH_WALL, ModItems.ENDER_QUARTZ_BLOCK,
                ModItems.POLISHED_ENDER_QUARTZ_SLAB, ModItems.CHISELED_ENDER_QUARTZ_BRICKS,
                ModItems.ASTRAL_END_STONE, ModItems.LAPIS_QUARTZ_CHECKER, ModItems.ENDER_QUARTZ_CHECKER, ModItems.LEVITATING_SAND));
        pinned.put(ModItemGroupsContent.Tab.TOOLS, List.of(
                ModItems.ORE_DETECTOR, ModItems.IRON_CHISEL, ModItems.ENDERITE_SLEDGEHAMMER,
                ModItems.ENDERITE_PICKAXE, ModItems.ENDERITE_HELMET, ModItems.ROTATOR));
        pinned.put(ModItemGroupsContent.Tab.MATERIALS, List.of(
                ModItems.ENDERITE_INGOT, ModItems.ASTRALIT_DUST, ModItems.ENDER_QUARTZ, ModItems.NIHILITH_ORE_ITEM, ModItems.IRON_CORE,
                ModItems.BASIC_UPGRADE_TEMPLATE, ModItems.GLOWING_TRIM_TEMPLATE,
                ModItems.ENCHANTED_NETHERITE_APPLE, ModItems.ENCHANTED_ENDERITE_APPLE));
        pinned.put(ModItemGroupsContent.Tab.FUNCTIONAL, List.of(
                ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_PISTON, ModItems.REINFORCED_FURNACE,
                ModItems.ENDERITE_BUNDLE, ModItems.QUIVER, ModItems.BACKPACK, ModItems.ENDERITE_BACKPACK, ModItems.BLUEPRINT));
        pinned.forEach((tab, items) -> {
            for (Item item : items) {
                List<ModItemGroupsContent.Tab> tabs = where.getOrDefault(item, List.of());
                if (!tabs.equals(List.of(tab))) {
                    problems.add(BuiltInRegistries.ITEM.getKey(item) + " belongs in " + tab + " but is in " + tabs);
                }
            }
        });

        int modEnchantments = (int) helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements().filter(h -> MOD_ID.equals(h.key().identifier().getNamespace())).count();
        Map<ModItemGroupsContent.Tab, Integer> expectedBooks = new LinkedHashMap<>();
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            expectedBooks.put(tab, tab == ModItemGroupsContent.Tab.TOOLS ? modEnchantments : 0);
        }
        Assertions.valueEqual(helper, books.toString(), expectedBooks.toString(), "enchanted books per creative tab");
        helper.assertTrue(problems.isEmpty(), "creative tabs: " + problems);
        TestCleanup.succeed(helper);
    }

    // =================================================================================
    // Creative tabs: layout spacer and the development tab
    // =================================================================================

    /**
     * SimpleMachines is laid out in rows of nine, one category per row: hoppers, pistons, furnaces,
     * smokers, blast furnaces, bundles, quivers, backpacks - each vanilla first and then the tiers -
     * and the building planning row: blueprint, cartography table, an octant and every building
     * wand. The rest of a row is filled with {@code simplebuilding:creative_spacer}.
     *
     * <p>The categories are read back from what the tab really emits (see {@link #rowLayout}): each
     * has to start in the first column, its first entries have to be the expected ones in the
     * expected order (entries appended at the end of a row - dyed variants, say - are allowed), every
     * spacer is only visible in its own tab, no row is spacers only, and the tab does not end on
     * spacers.
     *
     * <p>What breaks this: a missing or extra spacer (the next category no longer starts in column
     * one), a category moved or reordered, a spacer that is visible in the search tab, or trailing
     * filler after the last row.
     */
    public static void machinesAndStorageTabIsLaidOutInRowsOfNine(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        List<List<Item>> expected = List.of(
                List.of(Items.HOPPER, ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_HOPPER),
                List.of(Items.PISTON, Items.STICKY_PISTON, ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON,
                        ModItems.NETHERITE_PISTON, ModItems.ENDERITE_PISTON),
                List.of(Items.FURNACE, ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, ModItems.ENDERITE_FURNACE),
                List.of(Items.SMOKER, ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, ModItems.ENDERITE_SMOKER),
                List.of(Items.BLAST_FURNACE, ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE,
                        ModItems.ENDERITE_BLAST_FURNACE),
                List.of(Items.BUNDLE, ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE),
                List.of(ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_QUIVER),
                List.of(ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK),
                List.of(ModItems.BLUEPRINT, Items.CARTOGRAPHY_TABLE, ModItems.OCTANT, ModItems.COPPER_BUILDING_WAND,
                        ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND, ModItems.DIAMOND_BUILDING_WAND,
                        ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND));
        List<List<Item>> categories = rowLayout(helper, ModItemGroupsContent.Tab.FUNCTIONAL, problems);
        expectRows(categories, expected, problems);
        if (categories.size() != expected.size()) {
            problems.add("SimpleMachines has " + categories.size() + " rows instead of " + expected.size() + ": " + categories);
        }
        helper.assertTrue(problems.isEmpty(), "machines and storage layout: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * SimpleTools is laid out in rows of nine, one family per row from the lowest tier up to
     * enderite, the vanilla tools, weapons and armour of every tier included: chisel, building wand,
     * sledgehammer, pickaxe, shovel, hoe, axe, then sword and spear, then helmet, chestplate,
     * leggings and boots, then the gadgets (octant, velocity gauge, ore detector, magnet, rotator),
     * the sixteen coloured octants (one category over two rows) and last the enchanted books, one
     * per mod enchantment.
     *
     * <p>Same reading as {@link #machinesAndStorageTabIsLaidOutInRowsOfNine}: every category starts
     * in the first column and holds exactly the expected items in order.
     *
     * <p>What breaks this: a family moved, reordered or missing a tier, a vanilla tier dropped, a
     * missing spacer, books spread into another row or a book missing.
     */
    public static void toolsTabIsLaidOutInRowsOfNine(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        List<String> vanillaTools = List.of("wooden", "stone", "copper", "iron", "golden", "diamond", "netherite");
        List<String> vanillaArmour = List.of("leather", "chainmail", "copper", "iron", "golden", "diamond", "netherite");
        List<List<Item>> expected = new ArrayList<>();
        expected.add(List.of(ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL));
        expected.add(List.of(ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND));
        expected.add(List.of(ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                ModItems.ENDERITE_SLEDGEHAMMER));
        Map<String, Item> enderite = new LinkedHashMap<>();
        enderite.put("pickaxe", ModItems.ENDERITE_PICKAXE);
        enderite.put("shovel", ModItems.ENDERITE_SHOVEL);
        enderite.put("hoe", ModItems.ENDERITE_HOE);
        enderite.put("axe", ModItems.ENDERITE_AXE);
        enderite.put("sword", ModItems.ENDERITE_SWORD);
        enderite.put("spear", ModItems.ENDERITE_SPEAR);
        enderite.put("helmet", ModItems.ENDERITE_HELMET);
        enderite.put("chestplate", ModItems.ENDERITE_CHESTPLATE);
        enderite.put("leggings", ModItems.ENDERITE_LEGGINGS);
        enderite.put("boots", ModItems.ENDERITE_BOOTS);
        enderite.forEach((kind, top) -> {
            List<Item> family = new ArrayList<>();
            boolean armour = List.of("helmet", "chestplate", "leggings", "boots").contains(kind);
            for (String tier : armour ? vanillaArmour : vanillaTools) {
                family.add(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(tier + "_" + kind)));
            }
            family.add(top);
            expected.add(family);
        });
        expected.add(List.of(ModItems.OCTANT, ModItems.VELOCITY_GAUGE, ModItems.ORE_DETECTOR, ModItems.MAGNET, ModItems.ROTATOR));
        List<Item> colored = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            colored.add(ModItems.COLORED_OCTANT_ITEMS.get(color));
        }
        expected.add(colored);
        int modEnchantments = (int) helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements().filter(h -> MOD_ID.equals(h.key().identifier().getNamespace())).count();
        expected.add(Collections.nCopies(modEnchantments, Items.ENCHANTED_BOOK));

        List<List<Item>> categories = rowLayout(helper, ModItemGroupsContent.Tab.TOOLS, problems);
        if (!categories.equals(expected)) {
            problems.add("SimpleTools rows are " + categories + " instead of " + expected);
        }
        helper.assertTrue(problems.isEmpty(), "tools layout: " + problems);
        TestCleanup.succeed(helper);
    }

    /** Every category has to start with its expected items in order (more may follow at its end). */
    private static void expectRows(List<List<Item>> categories, List<List<Item>> expected, List<String> problems) {
        for (int c = 0; c < Math.min(categories.size(), expected.size()); c++) {
            List<Item> actual = categories.get(c);
            List<Item> head = actual.subList(0, Math.min(actual.size(), expected.get(c).size()));
            if (!head.equals(expected.get(c))) {
                problems.add("row " + (c + 1) + " is " + actual + " but has to start with " + expected.get(c));
            }
        }
    }

    /**
     * Reads a row laid out tab back into its categories: a run of real items, then its spacer padding.
     * Reports a category that does not start in the first column, a spacer visible in the search tab,
     * a row of spacers only and trailing filler.
     */
    private static List<List<Item>> rowLayout(GameTestHelper helper, ModItemGroupsContent.Tab tab, List<String> problems) {
        List<ItemStack> slots = new ArrayList<>();
        ModItemGroupsContent.populate(tab, (CreativeModeTab.Output) (stack, visibility) -> {
            slots.add(stack);
            if (stack.is(ModItems.CREATIVE_SPACER) && visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
                problems.add(tab + ": spacer at slot " + (slots.size() - 1) + " is visible as " + visibility
                        + ", so it would show up in the search tab");
            }
        }, helper.getLevel().registryAccess());

        List<List<Item>> categories = new ArrayList<>();
        int i = 0;
        while (i < slots.size()) {
            if (slots.get(i).is(ModItems.CREATIVE_SPACER)) {
                problems.add(tab + ": slot " + i + " starts with a spacer instead of an item");
                i++;
                continue;
            }
            int start = i;
            List<Item> category = new ArrayList<>();
            while (i < slots.size() && !slots.get(i).is(ModItems.CREATIVE_SPACER)) {
                category.add(slots.get(i).getItem());
                i++;
            }
            categories.add(category);
            if (start % 9 != 0) {
                problems.add(tab + ": category " + category + " starts in column " + (start % 9 + 1)
                        + " instead of the first column");
            }
            int padding = 0;
            while (i < slots.size() && slots.get(i).is(ModItems.CREATIVE_SPACER)) {
                padding++;
                i++;
            }
            if (padding >= 9) {
                problems.add(tab + ": after " + category + " come " + padding + " spacers, a whole empty row");
            }
            if (i == slots.size() && padding > 0) {
                problems.add(tab + ": the tab ends on " + padding + " spacers after its last row");
            }
        }
        return categories;
    }

    /**
     * The layout spacer is filler that cannot be taken or kept.
     *
     * <p>A slot that holds it reports itself inactive ({@code SlotMixin}), which is what the creative
     * screen asks before it draws a slot, highlights it, shows its tooltip or lets it be clicked; the
     * same slot with a hopper in it stays active, so the mixin cannot pass by switching every slot
     * off. A spacer that lands in an inventory anyway (a {@code /give}, say) is gone after one
     * inventory tick. It is hidden from recipe viewers through {@code c:hidden_from_recipe_viewers},
     * locked in creative slots and hides its tooltip, and item command suggestions leave it out
     * ({@code ItemParserStateMixin}).
     *
     * <p>What breaks this: the mixin not applied or checking the wrong item, the self deletion dropped,
     * the tag or the default components lost, the suggestion filter gone.
     */
    public static void creativeSpacerCannotBeTakenOrKept(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();

        net.minecraft.world.inventory.Slot slot = new net.minecraft.world.inventory.Slot(
                new net.minecraft.world.SimpleContainer(1), 0, 0, 0);
        slot.set(new ItemStack(Items.HOPPER));
        if (!slot.isActive()) {
            problems.add("a slot holding a hopper is inactive; the spacer check switches off every slot");
        }
        slot.set(new ItemStack(ModItems.CREATIVE_SPACER));
        if (slot.isActive()) {
            problems.add("a slot holding the spacer is still active, so the creative screen draws it, "
                    + "shows its tooltip and lets it be picked up");
        }

        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModItems.CREATIVE_SPACER));
        player.getInventory().tick();
        if (!player.getInventory().getItem(0).isEmpty()) {
            problems.add("a spacer in a player's inventory survived an inventory tick");
        }

        ItemStack spacer = new ItemStack(ModItems.CREATIVE_SPACER);
        TagKey<Item> hidden = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "hidden_from_recipe_viewers"));
        if (!spacer.is(hidden)) {
            problems.add("the spacer is not in c:hidden_from_recipe_viewers, so JEI/REI/EMI list it");
        }
        if (!spacer.has(DataComponents.CREATIVE_SLOT_LOCK)) {
            problems.add("the spacer has no creative_slot_lock");
        }
        if (!spacer.getOrDefault(DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT).hideTooltip()) {
            problems.add("the spacer does not hide its tooltip");
        }


        // Command suggestions (ItemParserStateMixin): /give, /clear and /item offer every mod item but
        // the spacer. The same ItemParser the ItemArgument asks on the client, here on the server's
        // registries; the chiseled astralit bricks next to it are the control that suggestions work.
        List<String> offered = new net.minecraft.commands.arguments.item.ItemParser(helper.getLevel().registryAccess())
                .fillSuggestions(new com.mojang.brigadier.suggestion.SuggestionsBuilder("simplebuilding:c", 0))
                .join().getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText).toList();
        if (!offered.contains("simplebuilding:chiseled_astralit_bricks")) {
            problems.add("item suggestions for simplebuilding:c do not offer the chiseled astralit bricks either, "
                    + "so the spacer check below proves nothing: " + offered);
        }
        if (offered.contains("simplebuilding:creative_spacer")) {
            problems.add("/give suggests simplebuilding:creative_spacer");
        }
        helper.assertTrue(problems.isEmpty(), "creative spacer: " + problems);
        TestCleanup.succeed(helper);
    }

    /** Every non-curse enchantment whose supported items hold the stack, straight from the registry. */
    private static List<Holder<Enchantment>> supportedEnchantments(GameTestHelper helper, ItemStack stack) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(h -> !h.is(EnchantmentTags.CURSE) && h.value().isSupportedItem(stack))
                .forEach(out::add);
        return out;
    }

    /**
     * The development tab "SimpleEnchants (Dev)" holds the best tier of every enchantable
     * item family, pre-enchanted at max level, with one variant per choice among mutually exclusive
     * enchantments.
     *
     * <p><b>Registered</b> as {@code simplebuilding:enchanted_dev} with its title key; its entries are
     * {@code PARENT_TAB_ONLY}, so none of the duplicates reaches the search tab.
     *
     * <p><b>Top tiers only</b>: the enderite chisel, wand, sledgehammer, bundle, quiver, backpack,
     * tools and armour, the tierless gadgets and the two vanilla items Constructor's Touch gives a
     * function (stick, shulker box) are there; every lower tier, the colored octants, vanilla
     * netherite gear (an enderite piece carries all of its enchantments) and unenchantable items are
     * not.
     *
     * <p><b>Enchantments</b>, checked against the registry for every item in the tab: each variant only
     * holds enchantments the item supports, each at its max level, no curse, no two incompatible ones,
     * and it is maximal - every supported enchantment it lacks conflicts with one it has. Over all
     * variants of an item every supported enchantment appears, so each member of an exclusive set is
     * offered. Anchors so this cannot pass on an empty tab: all five armour protections on the
     * enderite boots, silk touch and fortune as well as vein and strip miner on the enderite pickaxe,
     * cover and bridge on the enderite wand. No item has more than twelve variants.
     */
    public static void devEnchantedTabOffersEveryExclusiveChoiceAtMaxLevelOnTopTiers(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();

        Identifier devId = Identifier.fromNamespaceAndPath(MOD_ID, "enchanted_dev");
        CreativeModeTab registered = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(devId);
        if (registered == null) {
            problems.add(devId + " is not a registered creative tab");
        } else if (!(registered.getDisplayName().getContents() instanceof TranslatableContents title)
                || !"itemgroup.simplebuilding.enchanted_dev".equals(title.getKey())) {
            problems.add(devId + " is titled " + registered.getDisplayName());
        }

        Map<Item, List<ItemStack>> byItem = new LinkedHashMap<>();
        com.simplebuilding.items.DevEnchantedTab.populate((CreativeModeTab.Output) (stack, visibility) -> {
            byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(stack);
            if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
                problems.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " is offered as " + visibility
                        + ", so the dev duplicates reach the search tab");
            }
        }, helper.getLevel().registryAccess());
        helper.assertTrue(!byItem.isEmpty(), "the dev tab offered nothing, so none of the checks below could fail");

        List<Item> present = List.of(ModItems.ENDERITE_CHISEL, ModItems.ENDERITE_BUILDING_WAND, ModItems.ENDERITE_SLEDGEHAMMER,
                ModItems.ENDERITE_BUNDLE, ModItems.ENDERITE_QUIVER, ModItems.ENDERITE_BACKPACK, ModItems.ENDERITE_PICKAXE,
                ModItems.ENDERITE_AXE, ModItems.ENDERITE_SWORD, ModItems.ENDERITE_SPEAR, ModItems.ENDERITE_HELMET, ModItems.ENDERITE_BOOTS,
                ModItems.OCTANT, ModItems.ORE_DETECTOR, ModItems.MAGNET, ModItems.ROTATOR, ModItems.VELOCITY_GAUGE,
                Items.STICK, Items.SHULKER_BOX);
        for (Item item : present) {
            if (!byItem.containsKey(item)) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is missing from the dev tab");
            }
        }
        List<Item> absent = new ArrayList<>(List.of(
                ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL,
                ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND,
                ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE,
                ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER,
                ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK,
                Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_BOOTS, Items.LEATHER_BOOTS,
                Items.NETHERITE_SPEAR, Items.SHEARS, ModItems.ENDERITE_INGOT, ModItems.CREATIVE_SPACER, Items.ENCHANTED_BOOK));
        absent.addAll(ModItems.COLORED_OCTANT_ITEMS.values());
        for (Item item : absent) {
            if (byItem.containsKey(item)) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " is in the dev tab, but only the best tier of a family belongs there");
            }
        }

        Map<Item, Set<String>> offeredPerItem = new HashMap<>();
        byItem.forEach((item, stacks) -> {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            List<Holder<Enchantment>> supported = supportedEnchantments(helper, new ItemStack(item));
            if (supported.isEmpty()) {
                problems.add(id + " is in the dev tab but supports no enchantment");
            }
            if (stacks.size() > 12) {
                problems.add(id + " has " + stacks.size() + " variants; exclusive sets must not explode");
            }
            Set<String> offered = new TreeSet<>();
            Set<String> variants = new HashSet<>();
            for (ItemStack stack : stacks) {
                var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                        net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                List<Holder<Enchantment>> held = new ArrayList<>(enchantments.keySet());
                if (!variants.add(held.stream().map(h -> h.getRegisteredName()).sorted().toList().toString())) {
                    problems.add(id + " offers the same enchantment set twice: " + held);
                }
                for (Holder<Enchantment> h : held) {
                    offered.add(h.getRegisteredName());
                    if (!supported.contains(h)) {
                        problems.add(id + " carries " + h.getRegisteredName() + ", which it does not support (or a curse)");
                    }
                    if (enchantments.getLevel(h) != h.value().getMaxLevel()) {
                        problems.add(id + " carries " + h.getRegisteredName() + " " + enchantments.getLevel(h)
                                + " instead of its max level " + h.value().getMaxLevel());
                    }
                    for (Holder<Enchantment> other : held) {
                        if (!h.equals(other) && !Enchantment.areCompatible(h, other)) {
                            problems.add(id + " carries the incompatible " + h.getRegisteredName() + " and " + other.getRegisteredName());
                        }
                    }
                }
                for (Holder<Enchantment> missing : supported) {
                    if (!held.contains(missing) && held.stream().allMatch(h -> Enchantment.areCompatible(h, missing))) {
                        problems.add(id + " variant " + held.stream().map(Holder::getRegisteredName).toList()
                                + " could also carry " + missing.getRegisteredName() + " but does not");
                    }
                }
            }
            for (Holder<Enchantment> h : supported) {
                if (!offered.contains(h.getRegisteredName())) {
                    problems.add(id + " supports " + h.getRegisteredName() + " but no variant carries it");
                }
            }
            offeredPerItem.put(item, offered);
        });

        Map<Item, List<String>> anchors = new LinkedHashMap<>();
        anchors.put(ModItems.ENDERITE_BOOTS, List.of("minecraft:protection", "minecraft:fire_protection",
                "minecraft:blast_protection", "minecraft:projectile_protection", "simplebuilding:kinetic_protection",
                "simplebuilding:double_jump"));
        anchors.put(ModItems.ENDERITE_PICKAXE, List.of("minecraft:silk_touch", "minecraft:fortune",
                "simplebuilding:vein_miner", "simplebuilding:strip_miner"));
        anchors.put(ModItems.ENDERITE_BUILDING_WAND, List.of("simplebuilding:cover", "simplebuilding:bridge"));
        anchors.forEach((item, names) -> {
            Set<String> offered = offeredPerItem.getOrDefault(item, Set.of());
            for (String name : names) {
                if (!offered.contains(name)) {
                    problems.add(BuiltInRegistries.ITEM.getKey(item) + " has no variant with " + name);
                }
            }
        });

        helper.assertTrue(problems.isEmpty(), "dev enchanted tab: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The dev tab is only filled in a development environment or with {@code showDevEnchantedTab};
     * otherwise it stays empty, and vanilla does not show an empty category tab at all.
     *
     * <p>Both switches are flipped here and restored in {@code finally}; the body does not yield, so
     * no other test runs while they are flipped.
     *
     * <p>What breaks this: the gate dropped (tab always filled), or one of the two switches ignored.
     */
    public static void devEnchantedTabIsOnlyFilledInDevelopmentOrWhenConfigured(GameTestHelper helper) {
        com.simplebuilding.config.SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "no config loaded, so the option cannot be tested");
        boolean development = com.simplebuilding.platform.ModEnvironment.isDevelopmentEnvironment();
        boolean option = config.showDevEnchantedTab;
        List<String> problems = new ArrayList<>();
        try {
            boolean[][] cases = {{false, false}, {false, true}, {true, false}};
            for (boolean[] c : cases) {
                com.simplebuilding.platform.ModEnvironment.setDevelopmentEnvironment(c[0]);
                config.showDevEnchantedTab = c[1];
                int[] count = {0};
                com.simplebuilding.items.DevEnchantedTab.populateIfShown(
                        (CreativeModeTab.Output) (stack, visibility) -> count[0]++, helper.getLevel().registryAccess());
                boolean shouldFill = c[0] || c[1];
                if ((count[0] > 0) != shouldFill) {
                    problems.add("development=" + c[0] + ", showDevEnchantedTab=" + c[1] + " gave " + count[0]
                            + " entries, expected " + (shouldFill ? "some" : "none"));
                }
            }
        } finally {
            com.simplebuilding.platform.ModEnvironment.setDevelopmentEnvironment(development);
            config.showDevEnchantedTab = option;
        }
        helper.assertTrue(problems.isEmpty(), "dev tab gate: " + problems);
        TestCleanup.succeed(helper);
    }

    /** The 18 trim patterns vanilla ships, spelled out so an empty registry cannot pass the icon test. */
    private static final List<String> VANILLA_TRIM_PATTERNS = List.of(
            "bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib", "sentry",
            "shaper", "silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild");

    /**
     * Visible armour trims: every trimmable armour piece shows the PATTERN of its trim on the item
     * icon, in the colours of the trim material - for every pattern and every material the server
     * knows, vanilla and mod alike.
     *
     * <p>The item model is client data, so this reads the shipped JSON and PNG files off the
     * classpath, the same files the resource manager loads. For each item in
     * {@code #minecraft:trimmable_armor} (vanilla armour, the turtle shell, Enderite armour) it
     * finds the item definition the mod ships - the one whose root is a {@code minecraft:composite};
     * vanilla's own copy in the client jar is a plain {@code minecraft:select} - and checks:
     * <ul>
     *   <li>layer 1 is a {@code minecraft:component} select on {@code minecraft:trim} with ONE case
     *       listing every pattern x material, whose model is the untrimmed piece (no vanilla colour
     *       blob under the pattern), and a fallback (vanilla look without a trim or with a trim the
     *       mod has no picture for);</li>
     *   <li>layer 2 is the same select with a case per pattern x material pointing at
     *       {@code simplebuilding:item/trim_overlay/<slot>_<pattern>_<colour>}, where the colour is
     *       what the material's {@code MaterialAssetGroup} names for this armour (so iron on iron is
     *       {@code iron_darker}, exactly as on the worn armour), and {@code minecraft:empty} as
     *       fallback;</li>
     *   <li>that overlay model exists and uses the sprite {@code simplebuilding:trims/items/...} of
     *       the same name, whose grey source PNG exists, and the items atlas the mod ships lists
     *       that PNG in a {@code paletted_permutations} source together with the colour.</li>
     * </ul>
     *
     * <p>What breaks this: a new armour or trim material without re-running datagen; a pattern
     * without a drawn overlay (tools/textures/generate_trim_overlays.py); a palette missing from
     * the atlas (the icon would show the missing-texture checkerboard); the darker variant
     * computed from the base colour only; or the datagen dropping the fallback.
     */
    public static void everyTrimmableArmourShowsEveryTrimPatternOnItsIcon(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        var access = helper.getLevel().registryAccess();
        List<Holder.Reference<TrimPattern>> patterns = access.lookupOrThrow(Registries.TRIM_PATTERN).listElements().toList();
        List<Holder.Reference<TrimMaterial>> materials = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList();
        Set<String> patternIds = new TreeSet<>();
        patterns.forEach(p -> patternIds.add(p.key().identifier().toString()));
        for (String vanilla : VANILLA_TRIM_PATTERNS) {
            if (!patternIds.contains("minecraft:" + vanilla)) {
                problems.add("trim pattern minecraft:" + vanilla + " is not in the registry");
            }
        }
        helper.assertTrue(materials.size() >= 14, "expected the 11 vanilla and 3 mod trim materials, found " + materials.size());

        JsonObject atlas = shippedJson("assets/minecraft/atlases/items.json",
                json -> json.toString().contains("simplebuilding:trims/items/"));
        Map<String, Set<String>> atlasPalettes = new HashMap<>();
        if (atlas == null) {
            problems.add("no assets/minecraft/atlases/items.json on the classpath lists simplebuilding:trims/items/*");
        } else {
            for (JsonElement source : atlas.getAsJsonArray("sources")) {
                JsonObject s = source.getAsJsonObject();
                if (!"minecraft:paletted_permutations".equals(s.get("type").getAsString())) continue;
                Set<String> palettes = s.getAsJsonObject("permutations").keySet();
                for (JsonElement texture : s.getAsJsonArray("textures")) {
                    atlasPalettes.computeIfAbsent(texture.getAsString(), k -> new HashSet<>()).addAll(palettes);
                }
            }
        }

        int armourPieces = 0;
        int cells = 0;
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.TRIMMABLE_ARMOR)) {
            Item item = holder.value();
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            String slot = holder.is(ItemTags.HEAD_ARMOR) ? "helmet" : holder.is(ItemTags.CHEST_ARMOR) ? "chestplate"
                    : holder.is(ItemTags.LEG_ARMOR) ? "leggings" : holder.is(ItemTags.FOOT_ARMOR) ? "boots" : null;
            var equippable = new ItemStack(item).get(DataComponents.EQUIPPABLE);
            if (slot == null || equippable == null || equippable.assetId().isEmpty()) {
                problems.add(id + " is trimmable armour without an armour slot tag or an equipment asset");
                continue;
            }
            armourPieces++;
            String definitionPath = "assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json";
            JsonObject definition = shippedJson(definitionPath,
                    json -> "minecraft:composite".equals(json.getAsJsonObject("model").get("type").getAsString()));
            if (definition == null) {
                problems.add(id + ": the mod ships no trim-pattern item definition (" + definitionPath + ")");
                continue;
            }
            JsonArray layers = definition.getAsJsonObject("model").getAsJsonArray("models");
            JsonObject base = layers.get(0).getAsJsonObject();
            JsonObject overlay = layers.get(1).getAsJsonObject();
            for (JsonObject select : List.of(base, overlay)) {
                if (!"minecraft:component".equals(select.get("property").getAsString())
                        || !"minecraft:trim".equals(select.get("component").getAsString())
                        || !select.has("fallback")) {
                    problems.add(id + ": a layer does not select on the whole minecraft:trim component with a fallback");
                }
            }
            if (!"minecraft:empty".equals(overlay.getAsJsonObject("fallback").get("type").getAsString())) {
                problems.add(id + ": the pattern layer draws something when the trim is unknown or missing");
            }
            Set<String> untrimmed = new HashSet<>();
            JsonArray baseCases = base.getAsJsonArray("cases");
            JsonObject baseModel = baseCases.get(0).getAsJsonObject().getAsJsonObject("model");
            if (baseCases.size() != 1 || !baseModel.get("model").getAsString().equals(id.withPrefix("item/").toString())) {
                problems.add(id + ": a known trim does not show the untrimmed piece under the pattern (" + baseModel + ")");
            }
            for (JsonElement when : baseCases.get(0).getAsJsonObject().getAsJsonArray("when")) {
                untrimmed.add(trimKey(when.getAsJsonObject()));
            }
            Map<String, String> overlays = new HashMap<>();
            for (JsonElement c : overlay.getAsJsonArray("cases")) {
                JsonObject entry = c.getAsJsonObject();
                overlays.put(trimKey(entry.getAsJsonObject("when")), entry.getAsJsonObject("model").get("model").getAsString());
            }

            for (Holder.Reference<TrimPattern> pattern : patterns) {
                for (Holder.Reference<TrimMaterial> material : materials) {
                    String key = material.key().identifier() + "|" + pattern.key().identifier();
                    String colour = material.value().assets().assetId(equippable.assetId().get()).suffix();
                    String sprite = slot + "_" + pattern.value().assetId().getPath() + "_" + colour;
                    String expected = MOD_ID + ":item/trim_overlay/" + sprite;
                    cells++;
                    if (!untrimmed.contains(key)) {
                        problems.add(id + " " + key + ": not drawn untrimmed under the pattern");
                    }
                    if (!expected.equals(overlays.get(key))) {
                        problems.add(id + " " + key + ": pattern layer " + overlays.get(key) + ", expected " + expected);
                        continue;
                    }
                    JsonObject model = shippedJson("assets/" + MOD_ID + "/models/item/trim_overlay/" + sprite + ".json", json -> true);
                    String texture = model == null ? null : model.getAsJsonObject("textures").get("layer0").getAsString();
                    if (!(MOD_ID + ":trims/items/" + sprite).equals(texture)) {
                        problems.add(expected + ": model missing or layer0 is " + texture);
                    }
                    String grey = MOD_ID + ":trims/items/" + slot + "_" + pattern.value().assetId().getPath();
                    if (DataIntegrityTests.class.getClassLoader().getResource("assets/" + MOD_ID + "/textures/trims/items/"
                            + slot + "_" + pattern.value().assetId().getPath() + ".png") == null) {
                        problems.add(grey + ".png is missing (tools/textures/generate_trim_overlays.py)");
                    }
                    if (!atlasPalettes.getOrDefault(grey, Set.of()).contains(colour)) {
                        problems.add("the items atlas does not colour " + grey + " as " + colour);
                    }
                }
            }
        }
        helper.assertTrue(armourPieces >= 33, "expected 33 trimmable armour pieces (7 vanilla sets, turtle shell, Enderite), found " + armourPieces);
        if (problems.size() > 20) {
            int more = problems.size() - 20;
            problems = new ArrayList<>(problems.subList(0, 20));
            problems.add("... and " + more + " more");
        }
        helper.assertTrue(problems.isEmpty(), "visible armour trims (" + cells + " icon cells): " + problems);
        helper.succeed();
    }

    private static String trimKey(JsonObject when) {
        return when.get("material").getAsString() + "|" + when.get("pattern").getAsString();
    }

    /**
     * The copy of a client resource that the mod ships: the classpath holds the vanilla client jar
     * too, so every copy is read and the one {@code isOurs} accepts is returned (null if none is).
     */
    private static JsonObject shippedJson(String path, java.util.function.Predicate<JsonObject> isOurs) {
        try {
            Enumeration<URL> urls = DataIntegrityTests.class.getClassLoader().getResources(path);
            while (urls.hasMoreElements()) {
                try (InputStream in = urls.nextElement().openStream()) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (isOurs.test(json)) {
                        return json;
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalStateException("cannot read " + path + ": " + e, e);
        }
        return null;
    }

    /**
     * Every vanilla enchantment has its own enchanted book: an entry in
     * {@code VanillaBookTextures.VANILLA}, a case {@code minecraft_<id>} in
     * {@code assets/minecraft/items/enchanted_book.json} that points at
     * {@code simplebuilding:item/enchanted_book_vanilla_<id>}, and that model and its texture in the
     * mod's resources. The list holds nothing that is not a vanilla enchantment any more.
     *
     * <p>What breaks this: a vanilla enchantment added by a Minecraft update without a book, a typo
     * in a case or model name, a texture the generator no longer writes, or a stale list entry.
     */
    public static void everyVanillaEnchantmentHasItsOwnBookModel(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> vanilla = enchantments.listElementIds()
                .map(key -> key.identifier())
                .filter(id -> "minecraft".equals(id.getNamespace()))
                .map(Identifier::getPath)
                .sorted()
                .toList();
        List<String> problems = new ArrayList<>();
        String itemModel = vanillaBookResource("/assets/minecraft/items/enchanted_book.json", problems);
        for (String path : vanilla) {
            if (!com.simplebuilding.enchantment.VanillaBookTextures.VANILLA.contains(path)) {
                problems.add(path + " is missing from VanillaBookTextures.VANILLA");
            }
            String model = com.simplebuilding.enchantment.VanillaBookTextures.modelPath(path);
            String caseEntry = "\"when\": \"" + com.simplebuilding.enchantment.VanillaBookTextures.caseKey(path)
                    + "\", \"model\": { \"type\": \"minecraft:model\", \"model\": \"simplebuilding:" + model + "\" }";
            if (itemModel != null && !itemModel.contains(caseEntry)) {
                problems.add("enchanted_book.json has no case for minecraft:" + path);
            }
            vanillaBookResource("/assets/simplebuilding/models/" + model + ".json", problems);
            vanillaBookResource("/assets/simplebuilding/textures/" + model + ".png", problems);
        }
        for (String listed : com.simplebuilding.enchantment.VanillaBookTextures.VANILLA) {
            if (!vanilla.contains(listed)) {
                problems.add(listed + " is listed but is no vanilla enchantment");
            }
        }
        helper.assertTrue(vanilla.size() >= 40, "only " + vanilla.size() + " vanilla enchantments in the registry");
        helper.assertTrue(problems.isEmpty(), "vanilla enchanted books: " + problems);
        helper.succeed();
    }

    /** Reads a resource of the mod as text, or notes it as missing. */
    private static String vanillaBookResource(String path, List<String> problems) {
        try (java.io.InputStream in = com.simplebuilding.enchantment.VanillaBookTextures.class.getResourceAsStream(path)) {
            if (in == null) {
                problems.add("missing resource " + path);
                return null;
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            problems.add("unreadable resource " + path + ": " + e);
            return null;
        }
    }

    /**
     * The book select value follows the client option {@code vanillaEnchantedBookTextures}: on, a
     * book with Sharpness selects {@code minecraft_sharpness}; off, it selects {@code none} and falls
     * back to the vanilla model. A book with only a mod enchantment never selects a vanilla book (the
     * property picks the mod book before it asks for a vanilla one). The option itself flips
     * {@code VanillaBookTextures.enabled()} and is restored in {@code finally}.
     *
     * <p>What breaks this: the option ignored, the fallback value renamed, or a mod enchantment
     * mistaken for a vanilla one.
     */
    public static void vanillaBookTextureFollowsTheClientOption(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable sharp =
                new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        sharp.set(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable mod =
                new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        mod.set(enchantments.getOrThrow(ModEnchantments.FUNNEL), 1);
        helper.assertValueEqual(com.simplebuilding.enchantment.VanillaBookTextures.key(sharp.toImmutable(), true),
                "minecraft_sharpness", "select value of a Sharpness book with the option on");
        helper.assertValueEqual(com.simplebuilding.enchantment.VanillaBookTextures.key(sharp.toImmutable(), false),
                com.simplebuilding.enchantment.VanillaBookTextures.NONE, "select value of a Sharpness book with the option off");
        helper.assertValueEqual(com.simplebuilding.enchantment.VanillaBookTextures.key(mod.toImmutable(), true),
                com.simplebuilding.enchantment.VanillaBookTextures.NONE, "select value of a book with only a mod enchantment");

        com.simplebuilding.config.SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "no config loaded, so the option cannot be tested");
        boolean option = config.vanillaEnchantedBookTextures;
        try {
            config.vanillaEnchantedBookTextures = false;
            helper.assertTrue(!com.simplebuilding.enchantment.VanillaBookTextures.enabled(), "option off, but enabled() is true");
            config.vanillaEnchantedBookTextures = true;
            helper.assertTrue(com.simplebuilding.enchantment.VanillaBookTextures.enabled(), "option on, but enabled() is false");
        } finally {
            config.vanillaEnchantedBookTextures = option;
        }
        helper.succeed();
    }
}
