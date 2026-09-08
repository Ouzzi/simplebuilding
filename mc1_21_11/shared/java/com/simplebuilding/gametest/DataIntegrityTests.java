package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
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
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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

    /** How often each block loot table is rolled in {@link #everyModBlockLootTableLoads}. */
    private static final int BLOCK_LOOT_ROLLS = 8;

    /** Seed for those rolls, so a failure is reproducible instead of a coin flip. */
    private static final long BLOCK_LOOT_SEED = 20260904L;

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

    // Ingredient#items() is deprecated ("display only") but it is the only way to look at the raw
    // holders, which is exactly what we need: a holder that never got bound is the failure mode.
    @SuppressWarnings("deprecation")
    public static void modRecipesOnlyReferenceRegisteredItems(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RecipeManager recipeManager = server.getRecipeManager();
        ContextMap displayContext = SlotDisplayContext.fromLevel(helper.getLevel());

        List<String> dangling = new ArrayList<>();
        Set<Item> produced = new HashSet<>();
        int modRecipeCount = 0;
        int modRecipesWithoutDisplay = 0;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Identifier recipeId = holder.id().identifier();
            boolean isModRecipe = MOD_ID.equals(recipeId.getNamespace());

            if (isModRecipe) {
                modRecipeCount++;
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
        helper.succeed();
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
                // Not helper.assertItemEntityPresent: that only reports what was missing, and the
                // interesting part of a failure here is what *did* drop instead.
                List<ItemEntity> nearby = helper.getEntities(EntityType.ITEM, dropCase.pos(), 1.5D);
                boolean dropped = nearby.stream().anyMatch(item -> item.getItem().is(dropCase.expectedDrop()));
                helper.assertTrue(dropped, BuiltInRegistries.BLOCK.getKey(dropCase.block())
                        + " should have dropped " + BuiltInRegistries.ITEM.getKey(dropCase.expectedDrop())
                        + " at " + dropCase.pos() + ", but the item entities within 1.5 blocks are "
                        + nearby.stream().map(item -> item.getItem().toString()).toList());
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
     * <p>Steps 3 and 4 pin them on {@link #isVoidProtected}, this file's copy of the tag lookup -
     * and a copy is all it is, so no edit to the mixin can make either of them fail. Step 5 drops
     * four real item entities below the world's floor and ticks them, which is the only place the
     * mixin itself is asked anything: without it an empty {@code if} body, or the mixin missing
     * from {@code simplebuilding.mixins.json} altogether, would leave every assertion above green
     * while every enderite ingot falls into the void. Two of the four carry a custom name, because
     * the regression the tag replaced is a mixin that reads {@code getHoverName} again: a mixin
     * protecting the tag <em>or</em> anything named "Enderite" passes steps 1 to 4 untouched, and
     * only the named dirt in step 5b tells the two apart.
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
}
