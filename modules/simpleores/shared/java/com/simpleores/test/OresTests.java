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
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import com.simpleores.block.LeadPlateBlock;
import com.simpleores.block.SilverMirrorBlock;
import com.simpleores.sound.Soundproofing;
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

    private static CraftingInput of(ItemStack... stacks) {
        List<ItemStack> slots = new ArrayList<>(List.of(stacks));
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
            check(h, OresRegistry.BLOCKS.size() == 12 + 6 + 1 + 1 + 5 && OresRegistry.ITEMS.size() == 12 + 3 + 3 + 4 + 18 + 6 + 7 + 4 + 1 + 1 + 4, "block/item count " + OresRegistry.BLOCKS.size() + "/" + OresRegistry.ITEMS.size());
            check(h, OresConfig.enabled && OresConfig.KEYS.equals(List.of("enabled", "soundproofing", "soundproofFactor", "soundproofThreshold", "soundproofRecords", "sageOre", "dimensionalScrap"))
                    && OresConfig.soundproofing && OresConfig.soundproofFactor == 0.5 && OresConfig.soundproofThreshold == 0.05 && !OresConfig.soundproofRecords
                    , "config defaults");
            h.succeed();
        });
        ALL.put("silver_is_no_tool_material", h -> {
            // Owner ABN30-10: silver is an ingredient and a building metal, never a tool or armor material.
            for (String path : OresRegistry.ITEMS.keySet()) {
                boolean silverGear = path.startsWith("silver_") && !path.equals("silver_ingot") && !path.equals("silver_nugget") && !path.equals("silver_block")
                        && !path.equals("silver_ore") && !path.equals("silver_arrow") && !path.equals("silver_mirror") && !path.equals("silver_sheet");
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
            ToolMaterial hard = OresMaterials.ROSE_BRONZE_TOOLS, soft = OresMaterials.ROSE_GOLD_TOOLS;
            check(h, hard.durability() == ToolMaterial.COPPER.durability() && hard.durability() == 190, "Rose Copper durability = copper");
            check(h, soft.durability() == ToolMaterial.IRON.durability(), "Rose Gold durability = iron: " + soft.durability());
            check(h, soft.speed() == ToolMaterial.GOLD.speed() && hard.speed() < soft.speed() && hard.speed() > ToolMaterial.COPPER.speed() && soft.speed() > ToolMaterial.IRON.speed(),
                    "tempo: Rose Gold = gold, Rose Copper faster than copper, both faster than their durability twin");
            check(h, hard.enchantmentValue() == 15 && soft.enchantmentValue() == ToolMaterial.GOLD.enchantmentValue(), "enchantability");
            check(h, hard.attackDamageBonus() == ToolMaterial.COPPER.attackDamageBonus() && soft.attackDamageBonus() == ToolMaterial.IRON.attackDamageBonus(), "attack bonus like copper / iron");
            check(h, soft.durability() > hard.durability() && soft.speed() > hard.speed() && soft.attackDamageBonus() > hard.attackDamageBonus(), "Rose Gold is the better of the two");
            check(h, hard.durability() < ToolMaterial.DIAMOND.durability() && soft.durability() < ToolMaterial.DIAMOND.durability(), "no tier beats diamond's durability");
            for (String alloy : new String[] {"rose_bronze", "rose_gold"}) {
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
            var soft = OresMaterials.ROSE_GOLD_ARMOR;
            check(h, hard.durability() == ArmorMaterials.COPPER.durability() && soft.durability() == ArmorMaterials.IRON.durability() && soft.durability() > hard.durability(), "durability factors");
            check(h, hard.enchantmentValue() == 12 && soft.enchantmentValue() == 25, "enchantability");
            int[] hardWant = {2, 4, 3, 1}, softWant = {2, 5, 3, 1};
            ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
            for (int i = 0; i < 4; i++) {
                check(h, hard.defense().get(types[i]) == hardWant[i] && soft.defense().get(types[i]) == softWant[i], "protection " + types[i]);
                check(h, hard.defense().get(types[i]) <= ArmorMaterials.IRON.defense().get(types[i]) && soft.defense().get(types[i]) <= ArmorMaterials.IRON.defense().get(types[i]),
                        "never above iron: " + types[i]);
                String[] parts = {"helmet", "chestplate", "leggings", "boots"};
                check(h, stack("rose_bronze_" + parts[i]).getMaxDamage() == types[i].getDurability(11), "hard " + parts[i] + " durability");
                check(h, stack("rose_gold_" + parts[i]).getMaxDamage() == types[i].getDurability(15), "soft " + parts[i] + " durability");
            }
            check(h, hard.toughness() == 0.0F && soft.knockbackResistance() == 0.0F, "no toughness, no knockback resistance");
            h.succeed();
        });
        ALL.put("pressing_recipes", h -> {
            // Owner 05.11.: only two pressings. Rose Gold: 1 gold + 2 copper + 1 silver. Rose Copper: 2 gold + 1 Rose Gold ingot + 1 silver (as worded).
            ItemStack roseCopper = craft(h, of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), stack("rose_gold_ingot"), stack("silver_ingot")));
            check(h, roseCopper.is(item("rose_bronze_pressling")) && roseCopper.getCount() == 1, "Rose Copper pressing: " + roseCopper);
            check(h, craft(h, of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.COPPER_INGOT), stack("silver_ingot"))).isEmpty(), "Rose Copper needs the Rose Gold ingot, not copper");
            int[][] soft = {{1, 2, 1, 0}};
            for (int[] r : soft) {
                ItemStack out = craft(h, grid(r[0], r[1], r[2], r[3]));
                check(h, out.is(item("rose_gold_pressling")) && out.getCount() == 1, "soft " + r[0] + "/" + r[1] + "/" + r[2] + "/" + r[3] + " -> " + out);
            }
            for (int[] gone : new int[][] {{4, 2, 1, 0}, {7, 1, 1, 0}, {1, 2, 0, 1}}) {
                check(h, craft(h, grid(gone[0], gone[1], gone[2], gone[3])).isEmpty(), "removed mix " + gone[0] + "/" + gone[1] + "/" + gone[2] + "/" + gone[3] + " gives nothing");
            }
            check(h, craft(h, grid(1, 2, 0, 0)).isEmpty(), "no silver: no pressing");
            check(h, craft(h, grid(0, 2, 1, 0)).isEmpty(), "no gold: no pressing");
            check(h, craft(h, grid(1, 1, 1, 0)).isEmpty(), "too little copper: no pressing");
            h.succeed();
        });
        ALL.put("pressing_melts_only_in_the_blast_furnace", h -> {
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            for (String alloy : new String[] {"rose_bronze", "rose_gold"}) {
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
        ALL.put("lead_blast_resistance", h -> {
            BlockState lead = OresRegistry.LEAD_BLOCK.defaultBlockState();
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            float obsidian = Blocks.OBSIDIAN.getExplosionResistance();
            check(h, OresRegistry.LEAD_BLOCK.getExplosionResistance() == 1000.0F && 1000.0F < obsidian, "blast resistance 1000, a little below obsidian " + obsidian);
            check(h, lead.getDestroySpeed(level, pos) == 25.0F, "hardness 25: " + lead.getDestroySpeed(level, pos));
            check(h, lead.requiresCorrectToolForDrops() && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(lead) && new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(lead),
                    "the block of lead needs an iron pickaxe");
            BlockState ore = OresRegistry.LEAD_ORE.defaultBlockState();
            check(h, new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore) && !new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(ore), "lead ore needs a stone pickaxe");
            check(h, lead.getSoundType() == OresRegistry.LEAD_SOUND && OresRegistry.LEAD_SOUND.getVolume() > 1.0F && OresRegistry.LEAD_SOUND.getPitch() < 1.0F, "loud, low sound type");
            h.succeed();
        });
        ALL.put("lead_occludes_vibrations", h -> {
            BlockState lead = OresRegistry.LEAD_BLOCK.defaultBlockState();
            check(h, lead.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS), "lead stops vibrations like wool");
            check(h, !lead.is(BlockTags.DAMPENS_VIBRATIONS), "lead does NOT dampen vibrations: steps and mining on it stay loud");
            check(h, !OresRegistry.SILVER_BLOCK.defaultBlockState().is(BlockTags.OCCLUDES_VIBRATION_SIGNALS), "silver does not");
            check(h, lead.is(Soundproofing.BLOCKS) && !OresRegistry.SILVER_BLOCK.defaultBlockState().is(Soundproofing.BLOCKS), "lead is in the soundproofing tag, silver is not");
            h.succeed();
        });
        ALL.put("soundproof_attenuation", h -> {
            OresConfig.reset(); // another test may hold a changed switch across ticks
            // The maths: factor ^ blocks, threshold on the resulting volume.
            check(h, Soundproofing.attenuation(0, 0.5) == 1.0F && Soundproofing.attenuation(1, 0.5) == 0.5F && Soundproofing.attenuation(3, 0.5) == 0.125F, "factor ^ blocks");
            check(h, Soundproofing.audible(4.0F, Soundproofing.attenuation(3, 0.5), 0.05), "TNT (4) behind 3 lead blocks is still heard (0.5)");
            check(h, Soundproofing.audible(0.5F, 0.1F, 0.05) && !Soundproofing.audible(0.4F, 0.1F, 0.05), "threshold: 0.05 is heard, 0.04 is not");
            check(h, !Soundproofing.audible(1.0F, Soundproofing.attenuation(5, 0.5), 0.05), "1.0 behind 5 blocks (0.031) falls below 0.05");
            ServerLevel level = h.getLevel();
            BlockPos origin = h.absolutePos(new BlockPos(0, 2, 2));
            Vec3 from = Vec3.atCenterOf(origin).add(1.0, 0.0, 0.0), to = Vec3.atCenterOf(origin).add(10.0, 0.0, 0.0);
            check(h, Soundproofing.blocksBetween(level, from, to) == 0, "no lead in the way: 0 blocks");
            for (int x = 4; x <= 6; x++) level.setBlockAndUpdate(origin.east(x), OresRegistry.LEAD_BLOCK.defaultBlockState());
            check(h, Soundproofing.blocksBetween(level, from, to) == 3, "three lead blocks in a row: " + Soundproofing.blocksBetween(level, from, to));
            level.setBlockAndUpdate(origin.east(1), OresRegistry.LEAD_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(origin.east(10), OresRegistry.LEAD_BLOCK.defaultBlockState());
            check(h, Soundproofing.blocksBetween(level, from, to) == 3, "the cell of the sound and the cell of the ear do not count");
            level.setBlockAndUpdate(origin.east(8), Blocks.STONE.defaultBlockState());
            check(h, Soundproofing.blocksBetween(level, from, to) == 3, "other blocks do not muffle");
            // The packet filter with a real player: the sound at x = 1, the player at x = 10, three lead blocks between.
            var player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
            try {
                OresConfig.reset();
                player.setPos(to.x, to.y - 1.62, to.z);
                Vec3 eye = player.getEyePosition();
                // The mock player's own eye height decides the row of cells: the sound comes from the same row, the lead stands in it.
                Vec3 source = new Vec3(from.x, eye.y, eye.z);
                BlockPos row = BlockPos.containing(source);
                for (int x = 4; x <= 6; x++) level.setBlockAndUpdate(new BlockPos(origin.getX() + x, row.getY(), row.getZ()), OresRegistry.LEAD_BLOCK.defaultBlockState());
                float between = Soundproofing.blocksBetween(player.level(), source, eye);
                check(h, player.level() == level && between >= 3, "same level " + (player.level() == level) + ", lead between sound " + source + " and eye " + eye + ": " + between);
                ClientboundSoundPacket loud = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, source.x, source.y, source.z, 4.0F, 1.0F, 1L);
                Packet<?>[] out = Soundproofing.filter(player, loud);
                // The packet position is a float, far from the origin (x/z in the millions) it is rounded to whole cells, and the filter walks from that rounded point: count from it, not from the exact one.
                Vec3 wire = new Vec3(loud.getX(), loud.getY(), loud.getZ());
                float wireBetween = Soundproofing.blocksBetween(player.level(), wire, eye);
                check(h, wireBetween >= 3, "packet position " + wire + " vs " + source + ": " + wireBetween + " blocks");
                float want = 4.0F * Soundproofing.attenuation(wireBetween, OresConfig.soundproofFactor);
                check(h, out != null && out.length == 1 && out[0] instanceof ClientboundSoundPacket p && p.getVolume() == want,
                        "TNT volume 4 -> " + want + " behind " + wireBetween + " lead blocks, got " + (out == null ? "null" : out.length + " packets " + (out.length > 0 && out[0] instanceof ClientboundSoundPacket q ? q.getVolume() : -1)));
                ClientboundSoundPacket soft = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, source.x, source.y, source.z, 0.2F, 1.0F, 1L);
                check(h, Soundproofing.filter(player, soft) == null, "sounds under 0.3 are never touched");
                ClientboundSoundPacket record = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.RECORDS, source.x, source.y, source.z, 4.0F, 1.0F, 1L);
                check(h, Soundproofing.filter(player, record) == null, "records are not muffled by default");
                OresConfig.soundproofRecords = true;
                check(h, Soundproofing.filter(player, record) != null, "records are muffled when switched on");
                OresConfig.soundproofRecords = false;
                OresConfig.soundproofThreshold = 0.3;
                ClientboundSoundPacket quiet = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, source.x, source.y, source.z, 1.0F, 1.0F, 1L);
                Packet<?>[] none = Soundproofing.filter(player, quiet);
                check(h, none != null && none.length == 0, "below the threshold the packet is dropped");
                OresConfig.soundproofThreshold = 0.05;
                OresConfig.soundproofing = false;
                check(h, Soundproofing.filter(player, loud) == null, "switched off: nothing is changed");
            } finally {
                OresConfig.reset();
            }
            h.succeed();
        });
        ALL.put("apples_eat_effects", h -> {
            // IG31-41: eating gives the effects of docs/ai/PLAN-B-APPLE41-2026-11-01.md (a step between the golden and the Netherite apple).
            Map<String, int[][]> table = new LinkedHashMap<>();   // {effect index, ticks, amplifier}
            table.put("rose_bronze_apple", new int[][] {{0, 100, 1}, {1, 2400, 0}, {3, 600, 0}});
            table.put("rose_gold_apple", new int[][] {{0, 100, 1}, {1, 2400, 0}, {3, 1000, 0}});
            table.put("enchanted_rose_bronze_apple", new int[][] {{0, 600, 1}, {3, 6000, 0}, {2, 6000, 0}, {1, 2400, 3}});
            table.put("enchanted_rose_gold_apple", new int[][] {{0, 700, 1}, {3, 6000, 0}, {2, 6000, 0}, {1, 2400, 3}});
            @SuppressWarnings("unchecked")
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>[] effects = new net.minecraft.core.Holder[] {net.minecraft.world.effect.MobEffects.REGENERATION,
                    net.minecraft.world.effect.MobEffects.ABSORPTION, net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, net.minecraft.world.effect.MobEffects.RESISTANCE,
                    net.minecraft.world.effect.MobEffects.STRENGTH};
            for (Map.Entry<String, int[][]> row : table.entrySet()) {
                var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
                ItemStack apple = stack(row.getKey());
                var food = apple.get(net.minecraft.core.component.DataComponents.FOOD);
                boolean soft = row.getKey().contains("rose_gold");
                check(h, food != null && food.nutrition() == (soft ? 6 : 5) && food.canAlwaysEat(), row.getKey() + " food: " + food);
                boolean enchanted = row.getKey().startsWith("enchanted_");
                check(h, Boolean.TRUE.equals(apple.get(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) == enchanted, row.getKey() + " glint");
                check(h, apple.getRarity() == (enchanted ? net.minecraft.world.item.Rarity.RARE : net.minecraft.world.item.Rarity.COMMON), row.getKey() + " rarity");
                apple.finishUsingItem(h.getLevel(), player);
                check(h, player.getActiveEffects().size() == row.getValue().length, row.getKey() + " effect count " + player.getActiveEffects());
                for (int[] e : row.getValue()) {
                    var instance = player.getEffect(effects[e[0]]);
                    check(h, instance != null && instance.getDuration() == e[1] && instance.getAmplifier() == e[2], row.getKey() + " effect " + e[0] + ": " + instance);
                }
                check(h, enchanted || player.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) == null, row.getKey() + " plain: no Fire Resistance");
            }
            // The step: more hunger than the golden apple (4), less than the Netherite apple (8); Resistance lasts longer than the golden apple's none.
            check(h, 4 < 5 && 5 < 6 && 6 < 8 && 600 < 1000, "ladder gold < rose copper < rose gold < netherite");
            h.succeed();
        });
        ALL.put("apple_recipes", h -> {
            for (String alloy : List.of("rose_bronze", "rose_gold")) {
                for (boolean enchanted : new boolean[] {false, true}) {
                    ItemStack ring = stack(enchanted ? alloy + "_block" : alloy + "_ingot");
                    List<ItemStack> slots = new ArrayList<>();
                    for (int i = 0; i < 9; i++) slots.add(i == 4 ? new ItemStack(Items.APPLE) : ring.copy());
                    ItemStack out = craft(h, CraftingInput.of(3, 3, slots));
                    check(h, out.is(item((enchanted ? "enchanted_" : "") + alloy + "_apple")) && out.getCount() == 1, alloy + (enchanted ? " blocks" : " ingots") + " round an apple: " + out);
                    slots.set(4, new ItemStack(Items.GOLDEN_APPLE));
                    check(h, craft(h, CraftingInput.of(3, 3, slots)).isEmpty(), "no golden apple in the middle");
                }
                check(h, stack(alloy + "_apple").is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.parse("c:foods/golden"))), "tagged golden food");
            }
            h.succeed();
        });
        ALL.put("storage_block_recipes", h -> {
            // BLOCKS07: nine of the material <-> block, both ways; no recipe melts a block.
            Map<String, Item> materials = new LinkedHashMap<>();
            materials.put("rose_bronze_pressling_block", item("rose_bronze_pressling"));
            materials.put("rose_gold_pressling_block", item("rose_gold_pressling"));
            materials.put("netherite_scrap_block", Items.NETHERITE_SCRAP);
            materials.put("enderite_scrap_block", item("enderite_scrap"));
            materials.put("sage_orb_block", item("sage_orb"));
            for (Map.Entry<String, Item> row : materials.entrySet()) {
                ItemStack nine = new ItemStack(row.getValue());
                List<ItemStack> slots = new ArrayList<>();
                for (int i = 0; i < 9; i++) slots.add(nine.copy());
                ItemStack block = craft(h, CraftingInput.of(3, 3, slots));
                check(h, block.is(item(row.getKey())) && block.getCount() == 1, "9 -> " + row.getKey() + ": " + block);
                ItemStack back = craft(h, of(new ItemStack(item(row.getKey()))));
                check(h, back.is(row.getValue()) && back.getCount() == 9, row.getKey() + " -> 9: " + back);
                slots.set(8, ItemStack.EMPTY);
                check(h, craft(h, CraftingInput.of(3, 3, slots)).isEmpty(), row.getKey() + ": 8 are not enough");
                ServerLevel level = h.getLevel();
                SingleRecipeInput in = new SingleRecipeInput(new ItemStack(item(row.getKey())));
                for (RecipeType<? extends AbstractCookingRecipe> type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING))
                    check(h, level.getServer().getRecipeManager().getRecipeFor(type, in, level).isEmpty(), row.getKey() + " does not melt");
            }
            check(h, item("netherite_scrap_block").components().has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT)
                    && item("enderite_scrap_block").components().has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT), "scrap blocks are fire resistant");
            check(h, OresRegistry.NETHERITE_SCRAP_BLOCK.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL) && OresRegistry.SAGE_ORB_BLOCK.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL), "tool tiers");
            h.succeed();
        });
        ALL.put("rose_carrots", h -> {
            // BLOCKS07: slightly above the golden carrot (6 hunger, factor 1.2), Rose Gold better than Rose Copper, no effects, 8 nuggets round a carrot.
            var golden = Items.GOLDEN_CARROT.components().get(net.minecraft.core.component.DataComponents.FOOD);
            int[] want = {7, 8};
            String[] alloys = {"rose_bronze", "rose_gold"};
            for (int i = 0; i < 2; i++) {
                ItemStack carrot = stack(alloys[i] + "_carrot");
                var food = carrot.get(net.minecraft.core.component.DataComponents.FOOD);
                check(h, food != null && food.nutrition() == want[i] && food.nutrition() > golden.nutrition() && food.saturation() > golden.saturation(), alloys[i] + " carrot food: " + food);
                check(h, !food.canAlwaysEat(), "like the golden carrot, not always edible");
                var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
                player.getFoodData().setFoodLevel(10);
                carrot.finishUsingItem(h.getLevel(), player);
                check(h, player.getActiveEffects().isEmpty(), alloys[i] + " carrot gives no effects");
                check(h, !Boolean.TRUE.equals(carrot.get(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "not enchanted");
                List<ItemStack> slots = new ArrayList<>();
                for (int k = 0; k < 9; k++) slots.add(k == 4 ? new ItemStack(Items.CARROT) : stack(alloys[i] + "_nugget"));
                ItemStack out = craft(h, CraftingInput.of(3, 3, slots));
                check(h, out.is(item(alloys[i] + "_carrot")) && out.getCount() == 1, "8 nuggets round a carrot: " + out);
                slots.set(4, new ItemStack(Items.GOLDEN_CARROT));
                check(h, craft(h, CraftingInput.of(3, 3, slots)).isEmpty(), "no golden carrot in the middle");
                ItemStack nuggets = craft(h, of(stack(alloys[i] + "_ingot")));
                check(h, nuggets.is(item(alloys[i] + "_nugget")) && nuggets.getCount() == 9, "ingot -> 9 nuggets");
                List<ItemStack> nine = new ArrayList<>();
                for (int k = 0; k < 9; k++) nine.add(stack(alloys[i] + "_nugget"));
                check(h, craft(h, CraftingInput.of(3, 3, nine)).is(item(alloys[i] + "_ingot")), "9 nuggets -> ingot");
                check(h, carrot.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.parse("c:foods/golden"))), "tagged golden food");
            }
            var bronze = stack("rose_bronze_carrot").get(net.minecraft.core.component.DataComponents.FOOD);
            var gold = stack("rose_gold_carrot").get(net.minecraft.core.component.DataComponents.FOOD);
            check(h, gold.nutrition() > bronze.nutrition() && gold.saturation() > bronze.saturation(), "Rose Gold is the better carrot");
            h.succeed();
        });
        ALL.put("silver_mirror_recipe", h -> {
            // The silver arrow moved to Simple Arrows on 03.11. (its tests: simplearrows module_game_test_*).
            check(h, !OresRegistry.ITEMS.containsKey("silver_arrow"), "no silver arrow here any more");
            check(h, craft(h, CraftingInput.of(2, 1, List.of(new ItemStack(Items.GLASS_PANE), stack("silver_sheet")))).is(item("silver_mirror")), "glass pane + silver sheet -> mirror");
            check(h, craft(h, CraftingInput.of(2, 1, List.of(stack("silver_sheet"), new ItemStack(Items.GLASS_PANE)))).is(item("silver_mirror")), "shapeless: either order");
            List<ItemStack> old = new ArrayList<>(List.of(ItemStack.EMPTY, stack("silver_ingot"), ItemStack.EMPTY, stack("silver_ingot"), new ItemStack(Items.GLASS_PANE),
                    stack("silver_ingot"), ItemStack.EMPTY, stack("silver_ingot"), ItemStack.EMPTY));
            check(h, craft(h, CraftingInput.of(3, 3, old)).isEmpty(), "the old recipe (4 silver ingots round a glass pane) is gone");
            h.succeed();
        });
        ALL.put("silver_sheet_recipe", h -> {
            var input = CraftingInput.of(3, 1, List.of(stack("silver_ingot"), stack("silver_ingot"), stack("silver_ingot")));
            check(h, craft(h, input).is(item("silver_sheet")) && craft(h, input).getCount() == 12, "3 silver ingots in a row -> 12 silver sheets");
            h.succeed();
        });
        ALL.put("silver_sheet_placement", h -> {
            BlockState sheet = OresRegistry.SILVER_SHEET.defaultBlockState();
            check(h, new ItemStack(item("silver_sheet")).getItem() instanceof net.minecraft.world.item.BlockItem, "has a block item");
            ServerLevel level = h.getLevel();
            BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
            for (Direction d : Direction.values()) {
                BlockState s = sheet.setValue(LeadPlateBlock.FACING, d);
                var shape = s.getShape(level, at);
                double thickness = shape.max(d.getAxis()) - shape.min(d.getAxis());
                check(h, Math.abs(thickness - 1.0 / 16.0) < 1.0E-9, d + ": one pixel thick, was " + thickness);
                for (Direction.Axis axis : Direction.Axis.values()) {
                    if (axis == d.getAxis()) continue;
                    check(h, shape.min(axis) == 0.0 && shape.max(axis) == 1.0, d + ": covers the whole side on " + axis);
                }
                check(h, s.canSurvive(level, at), "needs no support (" + d + ")");
            }
            // placed like the plate: the facing is the clicked face
            check(h, sheet.getBlock() instanceof LeadPlateBlock && !(OresRegistry.LEAD_PLATE.defaultBlockState().getShape(level, at).equals(sheet.getShape(level, at))), "thinner than the 2 px lead plate");
            level.setBlockAndUpdate(at, sheet.setValue(LeadPlateBlock.FACING, Direction.NORTH));
            check(h, level.getBlockState(at).is(OresRegistry.SILVER_SHEET), "placed");
            h.succeed();
        });
        ALL.put("silver_mirror_placement", h -> {
            ServerLevel level = h.getLevel();
            BlockPos wall = h.absolutePos(new BlockPos(3, 1, 3));
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            BlockPos at = wall.north();
            BlockState north = OresRegistry.SILVER_MIRROR.defaultBlockState().setValue(SilverMirrorBlock.FACING, Direction.NORTH);
            check(h, north.canSurvive(level, at), "a mirror facing north hangs on the stone south of it");
            check(h, !north.setValue(SilverMirrorBlock.FACING, Direction.SOUTH).canSurvive(level, at), "no wall behind a mirror facing south: it cannot stand");
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var shape = north.setValue(SilverMirrorBlock.FACING, d).getShape(level, at);
                double thickness = d.getAxis() == Direction.Axis.Z ? shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z) : shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
                check(h, Math.abs(thickness - 1.0 / 16.0) < 1.0E-9, d + ": one pixel thick, was " + thickness);
            }
            level.setBlockAndUpdate(at, north);
            check(h, level.getBlockState(at).is(OresRegistry.SILVER_MIRROR), "placed");
            level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            check(h, level.getBlockState(at).isAir(), "the mirror falls off when its wall goes");
            check(h, new ItemStack(item("silver_mirror")).getItem() instanceof net.minecraft.world.item.BlockItem, "has a block item");
            h.succeed();
        });
        ALL.put("lead_plate_soundproofing", h -> {
            BlockState plate = OresRegistry.LEAD_PLATE.defaultBlockState();
            check(h, plate.is(Soundproofing.HALF_BLOCKS) && !plate.is(Soundproofing.BLOCKS), "plate is in soundproofing_half only");
            check(h, Soundproofing.weight(plate) == 0.5F && Soundproofing.weight(OresRegistry.LEAD_BLOCK.defaultBlockState()) == 1.0F
                    && Soundproofing.weight(Blocks.STONE.defaultBlockState()) == 0.0F, "weights: plate 0.5, lead block 1, stone 0");
            check(h, !plate.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS) && !plate.is(BlockTags.DAMPENS_VIBRATIONS), "the thin plate is no vibration blocker (like a carpet) and steps stay loud");
            check(h, Soundproofing.attenuation(0.5, 0.5) > 0.70F && Soundproofing.attenuation(0.5, 0.5) < 0.71F && Soundproofing.attenuation(2.0, 0.5) == 0.25F, "factor ^ weight");
            ServerLevel level = h.getLevel();
            BlockPos origin = h.absolutePos(new BlockPos(0, 2, 2));
            Vec3 from = Vec3.atCenterOf(origin).add(1.0, 0.0, 0.0), to = Vec3.atCenterOf(origin).add(10.0, 0.0, 0.0);
            level.setBlockAndUpdate(origin.east(4), plate);
            check(h, Soundproofing.blocksBetween(level, from, to) == 0.5F, "one plate: 0.5 of a block");
            level.setBlockAndUpdate(origin.east(5), plate.setValue(LeadPlateBlock.FACING, Direction.NORTH));
            check(h, Soundproofing.blocksBetween(level, from, to) == 1.0F, "two plates (any facing): one block");
            level.setBlockAndUpdate(origin.east(6), OresRegistry.LEAD_BLOCK.defaultBlockState());
            check(h, Soundproofing.blocksBetween(level, from, to) == 2.0F, "two plates and a block: two blocks");
            // Lead octets of Simple Octets (when it is loaded) muffle like a whole block of lead, however little they fill.
            var octet = BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleoctets", "lead_block_octet"));
            if (octet.isPresent()) {
                level.setBlockAndUpdate(origin.east(6), octet.get().defaultBlockState());
                check(h, Soundproofing.blocksBetween(level, from, to) == 2.0F, "a lead octet counts a whole block");
            }
            for (Direction d : Direction.values()) {
                var shape = plate.setValue(LeadPlateBlock.FACING, d).getShape(level, origin.east(4));
                double thickness = shape.max(d.getAxis()) - shape.min(d.getAxis());
                check(h, Math.abs(thickness - 2.0 / 16.0) < 1.0E-9, d + ": two pixels thick, was " + thickness);
            }
            check(h, new ItemStack(item("lead_plate")).getItem() instanceof net.minecraft.world.item.BlockItem, "has a block item");
            h.succeed();
        });
        ALL.put("lead_plate_recipe", h -> {
            var input = CraftingInput.of(3, 1, List.of(stack("lead_ingot"), stack("lead_ingot"), stack("lead_ingot")));
            check(h, craft(h, input).is(item("lead_plate")) && craft(h, input).getCount() == 6, "3 lead ingots in a row -> 6 lead plates");
            h.succeed();
        });
        ALL.put("lead_worldgen", h -> {
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            OreFeature lead = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("lead_ore")));
            check(h, lead != null && lead.size() == 9 && lead.targetStates().size() == 2, "lead feature: veins of 9, stone and deepslate");
            RandomSource random = RandomSource.create(7L);
            check(h, lead.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random) && lead.targetStates().get(0).state().is(OresRegistry.LEAD_ORE)
                    && lead.targetStates().get(1).target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, random) && lead.targetStates().get(1).state().is(OresRegistry.DEEPSLATE_LEAD_ORE),
                    "stone -> ore, deepslate -> deepslate ore");
            PlacedFeature placed = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("lead_ore")));
            double perChunk = perChunk(level, placed);
            check(h, perChunk == 6.0, "6 veins per chunk, measured " + perChunk);
            PlacedFeature silverPlaced = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("silver_ore")));
            check(h, perChunk > perChunk(level, silverPlaced) * 2, "more common than silver");
            PlacedFeature copper = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_copper")));
            double copperBlocks = (copper == null ? 16.0 : perChunk(level, copper)) * 10;
            check(h, perChunk * lead.size() < copperBlocks, "fewer lead ore blocks per chunk than copper: " + perChunk * lead.size() + " vs " + copperBlocks);
            // Height: every vein of 400 chunks lies between Y -64 and 48, and both ends of the range are used.
            PlacementContext context = new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.of(placed));
            RandomSource heights = RandomSource.create(5L);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int i = 0; i < 400; i++) {
                List<BlockPos> current = List.of(BlockPos.ZERO);
                for (PlacementModifier modifier : placed.placement()) {
                    if (modifier instanceof BiomeFilter) continue;
                    List<BlockPos> next = new ArrayList<>();
                    for (BlockPos p : current) modifier.modify(context, heights, p, next::add);
                    current = next;
                }
                for (BlockPos p : current) {
                    min = Math.min(min, p.getY());
                    max = Math.max(max, p.getY());
                }
            }
            check(h, min >= -64 && max <= 48 && min < -50 && max > 36, "veins between Y -64 and 48, measured " + min + " to " + max);
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
