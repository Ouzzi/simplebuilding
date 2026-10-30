package com.simpleores.test;

import com.simpleores.OresMaterials;
import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import com.simpleores.config.OresConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/**
 * Module GameTests (concept RG30, owner choice ABN30-10). Values (tools, armor), recipes (pressing, blast furnace only, one
 * ingot per pressing), silver smelting, the world generation numbers (roughly: fewer and smaller veins than gold) and the
 * module switch. All on the 16 x 8 x 16 stone arena ({@code simpleores:arena}).
 */
public final class OresTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();

    private static Item item(String path) {
        return OresRegistry.item(path);
    }

    private static ItemStack stack(String path) {
        return new ItemStack(item(path));
    }

    /** Pressing ingredients as the recipes list them: {gold, copper, silver ingots, silver nuggets}. */
    private static CraftingInput grid(int gold, int copper, int silver, int nuggets) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < gold; i++) slots.add(new ItemStack(Items.GOLD_INGOT));
        for (int i = 0; i < copper; i++) slots.add(new ItemStack(Items.COPPER_INGOT));
        for (int i = 0; i < silver; i++) slots.add(stack("silver_ingot"));
        for (int i = 0; i < nuggets; i++) slots.add(stack("silver_nugget"));
        while (slots.size() < 9) slots.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, slots);
    }

    private static ItemStack craft(GameTestHelper h, CraftingInput input) {
        ServerLevel level = h.getLevel();
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    static {
        ALL.put("registered", h -> {
            OresRegistry.BLOCKS.keySet().forEach(p -> check(h, BuiltInRegistries.BLOCK.containsKey(SimpleOres.id(p)), "block " + p));
            OresRegistry.ITEMS.keySet().forEach(p -> check(h, BuiltInRegistries.ITEM.containsKey(SimpleOres.id(p)), "item " + p));
            check(h, BuiltInRegistries.ITEM.containsKey(SimpleOres.id("guide_book")), "guide book");
            check(h, OresRegistry.BLOCKS.size() == 7 && OresRegistry.ITEMS.size() == 7 + 3 + 4 + 18, "block/item count " + OresRegistry.BLOCKS.size() + "/" + OresRegistry.ITEMS.size());
            check(h, OresConfig.enabled && OresConfig.KEYS.equals(List.of("enabled")), "config defaults");
            h.succeed();
        });
        ALL.put("silver_is_no_tool_material", h -> {
            // Owner ABN30-10: silver is an ingredient and a building metal, never a tool or armor material.
            for (String path : OresRegistry.ITEMS.keySet()) {
                boolean silverGear = path.startsWith("silver_") && !path.equals("silver_ingot") && !path.equals("silver_nugget") && !path.equals("silver_block")
                        && !path.equals("silver_ore");
                check(h, !silverGear, "silver gear " + path);
            }
            check(h, !stack("silver_ingot").has(net.minecraft.core.component.DataComponents.TOOL)
                    && !stack("silver_ingot").has(net.minecraft.core.component.DataComponents.EQUIPPABLE), "silver ingot is plain");
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleOres.MOD_ID);
            try {
                check(h, SimpleOres.on() && !OresRegistry.stacks(OresRegistry.NATURAL).isEmpty(), "on: creative stacks");
                com.simplebuilding.framework.api.SubMods.set(SimpleOres.MOD_ID, false);
                check(h, !SimpleOres.on() && OresRegistry.stacks(OresRegistry.NATURAL).isEmpty(), "off: no creative stacks");
                check(h, BuiltInRegistries.BLOCK.containsKey(SimpleOres.id("silver_ore")), "off: blocks and items still exist");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleOres.MOD_ID, before);
                OresConfig.reset();
            }
            h.succeed();
        });
        ALL.put("tool_values", h -> {
            ToolMaterial hard = OresMaterials.ROSE_BRONZE_TOOLS, soft = OresMaterials.BLOSSOM_GOLD_TOOLS;
            check(h, hard.durability() == ToolMaterial.IRON.durability() && hard.durability() == 250, "hard durability = iron");
            check(h, soft.durability() == ToolMaterial.IRON.durability() * 9 / 10, "soft durability = 90 % of iron: " + soft.durability());
            check(h, soft.speed() == ToolMaterial.GOLD.speed() && hard.speed() < soft.speed() && hard.speed() > ToolMaterial.IRON.speed(), "tempo: soft = gold, hard between iron and gold");
            check(h, hard.enchantmentValue() == 20 && soft.enchantmentValue() == ToolMaterial.GOLD.enchantmentValue(), "enchantability");
            check(h, hard.attackDamageBonus() == 0.0F && soft.attackDamageBonus() == 0.0F, "attack bonus like gold");
            check(h, hard.durability() < ToolMaterial.DIAMOND.durability() && soft.durability() < ToolMaterial.DIAMOND.durability(), "no tier beats diamond's durability");
            for (String alloy : new String[] {"rose_bronze", "blossom_gold"}) {
                ToolMaterial m = alloy.equals("rose_bronze") ? hard : soft;
                for (String part : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe"}) {
                    check(h, stack(alloy + "_" + part).getMaxDamage() == m.durability(), alloy + "_" + part + " durability " + stack(alloy + "_" + part).getMaxDamage());
                }
                ItemStack pick = stack(alloy + "_pickaxe");
                check(h, pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()) && pick.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState())
                        && !pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), alloy + " pickaxe mines like iron");
            }
            h.succeed();
        });
        ALL.put("armor_values", h -> {
            var hard = OresMaterials.ROSE_BRONZE_ARMOR;
            var soft = OresMaterials.BLOSSOM_GOLD_ARMOR;
            check(h, hard.durability() == ArmorMaterials.IRON.durability() && soft.durability() == 13 && soft.durability() < hard.durability(), "durability factors");
            check(h, hard.enchantmentValue() == 22 && soft.enchantmentValue() == 25, "enchantability");
            int[] hardWant = {2, 4, 3, 1}, softWant = {2, 5, 3, 1};
            ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
            for (int i = 0; i < 4; i++) {
                check(h, hard.defense().get(types[i]) == hardWant[i] && soft.defense().get(types[i]) == softWant[i], "protection " + types[i]);
                check(h, hard.defense().get(types[i]) <= ArmorMaterials.IRON.defense().get(types[i]) && soft.defense().get(types[i]) <= ArmorMaterials.IRON.defense().get(types[i]),
                        "never above iron: " + types[i]);
                String[] parts = {"helmet", "chestplate", "leggings", "boots"};
                check(h, stack("rose_bronze_" + parts[i]).getMaxDamage() == types[i].getDurability(15), "hard " + parts[i] + " durability");
                check(h, stack("blossom_gold_" + parts[i]).getMaxDamage() == types[i].getDurability(13), "soft " + parts[i] + " durability");
            }
            check(h, hard.toughness() == 0.0F && soft.knockbackResistance() == 0.0F, "no toughness, no knockback resistance");
            h.succeed();
        });
        ALL.put("pressing_recipes", h -> {
            // Hard: 6/5/4 gold + 2 copper + 1 silver ingot. Soft: 7 gold + 1 copper + 1 silver ingot or nugget. Always 1 pressing.
            int[][] hard = {{6, 2, 1, 0}, {5, 2, 1, 0}, {4, 2, 1, 0}};
            for (int[] r : hard) {
                ItemStack out = craft(h, grid(r[0], r[1], r[2], r[3]));
                check(h, out.is(item("rose_bronze_pressling")) && out.getCount() == 1, "hard " + r[0] + "/" + r[1] + "/" + r[2] + " -> " + out);
            }
            int[][] soft = {{7, 1, 1, 0}, {7, 1, 0, 1}};
            for (int[] r : soft) {
                ItemStack out = craft(h, grid(r[0], r[1], r[2], r[3]));
                check(h, out.is(item("blossom_gold_pressling")) && out.getCount() == 1, "soft " + r[0] + "/" + r[1] + "/" + r[2] + "/" + r[3] + " -> " + out);
            }
            check(h, craft(h, grid(6, 3, 0, 0)).isEmpty(), "no silver: no pressing");
            check(h, craft(h, grid(3, 2, 1, 0)).isEmpty(), "too little gold: no pressing");
            check(h, craft(h, grid(8, 0, 1, 0)).isEmpty(), "no copper: no pressing");
            h.succeed();
        });
        ALL.put("pressing_melts_only_in_the_blast_furnace", h -> {
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            for (String alloy : new String[] {"rose_bronze", "blossom_gold"}) {
                SingleRecipeInput in = new SingleRecipeInput(stack(alloy + "_pressling"));
                var blast = recipes.getRecipeFor(RecipeType.BLASTING, in, level);
                check(h, blast.isPresent(), alloy + " pressing: a blast furnace melts it");
                AbstractCookingRecipe recipe = (AbstractCookingRecipe) blast.get().value();
                ItemStack out = recipe.assemble(in);
                // 26.3 stores the furnace time in the recipe and halves it in the blast furnace.
                int ticks = (int) Math.ceil(recipe.cookingTime() / 2.0F);
                check(h, ticks == 600, alloy + " pressing melts in " + ticks + " ticks, expected 600 (30 s)");
                check(h, out.is(item(alloy + "_ingot")) && out.getCount() == 1, alloy + " pressing gives " + out + ", expected exactly 1 ingot");
                check(h, recipes.getRecipeFor(RecipeType.SMELTING, in, level).isEmpty(), alloy + " pressing: furnace refuses");
                check(h, recipes.getRecipeFor(RecipeType.SMOKING, in, level).isEmpty(), alloy + " pressing: smoker refuses");
                check(h, recipes.getRecipeFor(RecipeType.CAMPFIRE_COOKING, in, level).isEmpty(), alloy + " pressing: campfire refuses");
            }
            h.succeed();
        });
        ALL.put("silver_smelting", h -> {
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            for (String source : new String[] {"raw_silver", "silver_ore", "deepslate_silver_ore", "end_silver_ore"}) {
                SingleRecipeInput in = new SingleRecipeInput(stack(source));
                var smelting = recipes.getRecipeFor(RecipeType.SMELTING, in, level);
                check(h, smelting.isPresent() && smelting.get().value().assemble(in).is(item("silver_ingot")), source + " -> silver ingot in a furnace");
                var blasting = recipes.getRecipeFor(RecipeType.BLASTING, in, level);
                check(h, blasting.isPresent() && blasting.get().value().assemble(in).is(item("silver_ingot")), source + " -> silver ingot in a blast furnace");
            }
            check(h, craft(h, CraftingInput.of(1, 1, List.of(stack("silver_ingot")))).is(item("silver_nugget")), "ingot -> nuggets");
            h.succeed();
        });
        ALL.put("ore_drops", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            level.setBlockAndUpdate(pos, OresRegistry.END_SILVER_ORE.defaultBlockState());
            for (int i = 0; i < 12; i++) {
                List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null, pick);
                int nuggets = drops.stream().filter(s -> s.is(item("silver_nugget"))).mapToInt(ItemStack::getCount).sum();
                check(h, nuggets >= 2 && nuggets == drops.stream().mapToInt(ItemStack::getCount).sum(), "end silver ore gives nuggets (2 to 6 plus Fortune): " + drops);
            }
            level.setBlockAndUpdate(pos, OresRegistry.DEEPSLATE_SILVER_ORE.defaultBlockState());
            List<ItemStack> raw = Block.getDrops(level.getBlockState(pos), level, pos, null, null, pick);
            check(h, raw.size() == 1 && raw.getFirst().is(item("raw_silver")), "deepslate silver ore gives raw silver: " + raw);
            check(h, level.getBlockState(pos).requiresCorrectToolForDrops() && !new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(level.getBlockState(pos)) && pick.isCorrectToolForDrops(level.getBlockState(pos)), "the ore needs an iron pickaxe");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.succeed();
        });
        ALL.put("worldgen_frequency", h -> {
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            OreFeature silver = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("silver_ore")));
            OreFeature endSilver = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("end_silver_ore")));
            check(h, silver != null && endSilver != null, "both silver features are registered");
            check(h, silver.size() == 5 && silver.targetStates().size() == 2 && endSilver.size() == 6 && endSilver.targetStates().size() == 1, "vein sizes and targets");
            RandomSource random = RandomSource.create(7L);
            check(h, silver.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random)
                    && silver.targetStates().get(0).state().is(OresRegistry.SILVER_ORE)
                    && silver.targetStates().get(1).target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, random)
                    && silver.targetStates().get(1).state().is(OresRegistry.DEEPSLATE_SILVER_ORE), "stone -> ore, deepslate -> deepslate ore");
            check(h, endSilver.targetStates().get(0).target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random)
                    && !endSilver.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random), "end silver only in end stone");
            double silverPerChunk = perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("silver_ore"))));
            check(h, silverPerChunk >= 1.0 && silverPerChunk <= 2.0, "1 to 2 silver veins per chunk, measured " + silverPerChunk);
            PlacedFeature gold = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_gold")));
            double goldPerChunk = gold == null ? 4.0 : perChunk(level, gold);
            check(h, silverPerChunk < goldPerChunk, "rarer than gold: silver " + silverPerChunk + " veins vs gold " + goldPerChunk);
            Object goldFeature = features.getValue(ResourceKey.create(Registries.FEATURE, Identifier.withDefaultNamespace("ore_gold")));
            int goldSize = goldFeature instanceof OreFeature ore ? ore.size() : 9;
            check(h, silverPerChunk * silver.size() < goldPerChunk * goldSize, "fewer silver ore blocks per chunk than gold");
            check(h, perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_silver_ore")))) >= 1.0, "end silver is placed");
            h.succeed();
        });
        ALL.put("guide_book", com.simpleores.guide.OresGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simpleores.discover.OresDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleores.discover.OresDiscover::behaviourHints);
    }

    /** Average veins one chunk rolls: the count placements of a placed feature, sampled 400 times with a fixed seed. */
    private static double perChunk(ServerLevel level, PlacedFeature placed) {
        if (placed == null) throw new AssertionError("placed feature missing");
        PlacementContext context = new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.of(placed));
        RandomSource random = RandomSource.create(11L);
        long total = 0;
        for (int i = 0; i < 400; i++) {
            int[] n = {0};
            for (PlacementModifier modifier : placed.placement()) {
                if (modifier instanceof CountPlacement) modifier.modify(context, random, BlockPos.ZERO, pos -> n[0]++);
            }
            total += n[0];
        }
        return total / 400.0;
    }

    private OresTests() {}
}
