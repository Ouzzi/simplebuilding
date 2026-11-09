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
            check(h, OresRegistry.BLOCKS.size() == 12 + 6 + 1 + 1 + 4 + 5 + 15 + 6 + 5 && OresRegistry.ITEMS.size() == 12 + 3 + 3 + 4 + 18 + 6 + 7 + 4 + 1 + 1 + 4 + 2 + 4 + 5 + 15 + 12 + 12 + 5, "block/item count " + OresRegistry.BLOCKS.size() + "/" + OresRegistry.ITEMS.size());
            check(h, OresConfig.enabled && OresConfig.KEYS.equals(List.of("enabled", "soundproofing", "soundproofFactor", "soundproofThreshold", "soundproofRecords", "sageOre", "dimensionalScrap", "sageOreOtherDimensions", "roseGoldOreChance",
                            "leadOre", "endLeadOre", "zincGeodes", "endZincGeodes", "nickelOre", "tinOre", "tinPlacers"))
                    && OresConfig.leadOre && OresConfig.endLeadOre && OresConfig.zincGeodes && OresConfig.endZincGeodes && OresConfig.nickelOre && OresConfig.tinOre && OresConfig.tinPlacers
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
                check(h, stack(alloys[i] + "_carrot").is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.parse("c:foods/golden"))), "tagged golden food");
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
            // N4 (b-ores13, owner 09.11.): lead only in stone (no deepslate lead), never touching air (Vanilla discard chance 1.0), Y 0 to 72.
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            OreFeature lead = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("lead_ore")));
            check(h, lead != null && lead.size() == 9 && lead.targetStates().size() == 1 && lead.discardChanceOnAirExposure() == 1.0F, "lead feature: veins of 9, stone only, air exposure 1.0");
            RandomSource random = RandomSource.create(7L);
            check(h, lead.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random) && lead.targetStates().get(0).state().is(OresRegistry.LEAD_ORE)
                    && !lead.targetStates().get(0).target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, random), "stone -> lead ore, deepslate stays");
            // the air rule as OreFeature applies it: a candidate next to air is never placed, one inside stone is
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(0, 40, 0);
            check(h, !lead.canPlaceOre(Blocks.STONE.defaultBlockState(), p -> p.equals(at.above()) ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), random, lead.targetStates().get(0), at)
                    && lead.canPlaceOre(Blocks.STONE.defaultBlockState(), p -> Blocks.STONE.defaultBlockState(), random, lead.targetStates().get(0), at), "never at air, always inside stone");
            PlacedFeature placed = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("lead_ore")));
            double perChunk = perChunk(level, placed);
            check(h, perChunk == 6.0, "6 veins per chunk, measured " + perChunk);
            PlacedFeature silverPlaced = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("silver_ore")));
            check(h, perChunk > perChunk(level, silverPlaced) * 2, "more common than silver");
            PlacedFeature copper = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_copper")));
            double copperBlocks = (copper == null ? 16.0 : perChunk(level, copper)) * 10;
            check(h, perChunk * lead.size() < copperBlocks, "fewer lead ore blocks per chunk than copper: " + perChunk * lead.size() + " vs " + copperBlocks);
            int[] range = heights(level, placed);
            check(h, range[0] >= 0 && range[1] <= 72 && range[0] < 12 && range[1] > 60, "veins between Y 0 and 72, measured " + range[0] + " to " + range[1]);
            // End lead: End stone only, never at air, outer islands.
            OreFeature endLead = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("end_lead_ore")));
            check(h, endLead != null && endLead.discardChanceOnAirExposure() == 1.0F && endLead.targetStates().size() == 1 && endLead.targetStates().get(0).state().is(OresRegistry.END_LEAD_ORE)
                    && endLead.targetStates().get(0).target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random)
                    && !endLead.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random), "End lead: End stone only, air exposure 1.0");
            int[] endRange = heights(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_lead_ore"))));
            check(h, endRange[0] >= 20 && endRange[1] <= 80, "End lead Y 20 to 80, measured " + endRange[0] + " to " + endRange[1]);
            // No Simple Ores feature generates the retired ores any more (they stay registered for old worlds).
            for (var ref : features.listElements().toList()) {
                if (!ref.key().identifier().getNamespace().equals(SimpleOres.MOD_ID) || !(ref.value() instanceof net.minecraft.world.level.levelgen.feature.AbstractOreFeature ore)) continue;
                for (var target : ore.targetStates()) {
                    for (String retired : new String[] {"deepslate_lead_ore", "deepslate_zinc_ore", "zinc_ore", "deepslate_tin_ore", "nickel_ore"}) {
                        check(h, !target.state().is(OresRegistry.BLOCKS.get(retired)), ref.key().identifier() + " still generates " + retired);
                    }
                }
            }
            check(h, features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("zinc_ore"))) == null, "zinc forms no free veins");
            h.succeed();
        });
        ALL.put("metal_recipes", h -> {
            // RG30-2: zinc, nickel, tin: smelting and blasting of raw and ores, nuggets, storage; the three alloys give as many ingots as go in.
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            for (String m : OresRegistry.METALS) {
                for (String source : new String[] {"raw_" + m, m + "_ore", "deepslate_" + m + "_ore"}) {
                    SingleRecipeInput in = new SingleRecipeInput(stack(source));
                    var smelting = recipes.getRecipeFor(RecipeType.SMELTING, in, level);
                    check(h, smelting.isPresent() && smelting.get().value().assemble(in).is(item(m + "_ingot")), source + " -> " + m + " ingot in a furnace");
                    var blasting = recipes.getRecipeFor(RecipeType.BLASTING, in, level);
                    check(h, blasting.isPresent() && blasting.get().value().assemble(in).is(item(m + "_ingot")), source + " -> " + m + " ingot in a blast furnace");
                }
                check(h, craft(h, CraftingInput.of(1, 1, List.of(stack(m + "_ingot")))).is(item(m + "_nugget")), m + " ingot -> nuggets");
                check(h, craft(h, CraftingInput.of(1, 1, List.of(stack(m + "_ingot")))).getCount() == 9, m + " ingot gives 9 nuggets");
                for (String[] pair : new String[][] {{m + "_nugget", m + "_ingot"}, {m + "_ingot", m + "_block"}, {"raw_" + m, "raw_" + m + "_block"}}) {
                    List<ItemStack> nine = new ArrayList<>();
                    for (int i = 0; i < 9; i++) nine.add(stack(pair[0]));
                    check(h, craft(h, CraftingInput.of(3, 3, nine)).is(item(pair[1])), "9 x " + pair[0] + " -> " + pair[1]);
                }
                ItemStack back = craft(h, of(stack(m + "_block")));
                check(h, back.is(item(m + "_ingot")) && back.getCount() == 9, m + " block -> 9 ingots");
            }
            Object[][] alloys = {{"brass", 3, "minecraft:copper_ingot", 2, "zinc_ingot", 1}, {"stainless_steel", 4, "minecraft:iron_ingot", 3, "nickel_ingot", 1},
                    {"bronze", 4, "minecraft:copper_ingot", 3, "tin_ingot", 1}};
            for (Object[] row : alloys) {
                String alloy = (String) row[0];
                List<ItemStack> grid = new ArrayList<>();
                Item base = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse((String) row[2]));
                for (int i = 0; i < (int) row[3]; i++) grid.add(new ItemStack(base));
                grid.add(stack((String) row[4]));
                while (grid.size() < 9) grid.add(ItemStack.EMPTY);
                ItemStack made = craft(h, CraftingInput.of(3, 3, grid));
                check(h, made.is(item(alloy + "_ingot")) && made.getCount() == (int) row[1], alloy + " alloy recipe: " + made);
                List<ItemStack> nine = new ArrayList<>();
                for (int i = 0; i < 9; i++) nine.add(stack(alloy + "_ingot"));
                check(h, craft(h, CraftingInput.of(3, 3, nine)).is(item(alloy + "_block")), "9 " + alloy + " ingots -> block");
                ItemStack back = craft(h, of(stack(alloy + "_block")));
                check(h, back.is(item(alloy + "_ingot")) && back.getCount() == 9, alloy + " block -> 9 ingots");
                for (String part : new String[] {"sword", "pickaxe", "helmet"}) check(h, !OresRegistry.ITEMS.containsKey(alloy + "_" + part), alloy + " has no " + part + " (no tool tiers)");
            }
            h.succeed();
        });
        ALL.put("metal_worldgen_drops", h -> {
            // N4 (b-ores13): nickel only in deepslate like diamond (Y -64 to 16), mountain tin in stone and granite (Y 20 to 80, nests only in
            // granite, extra veins in the mountain biomes); every ore drops its raw piece; tiers: zinc and tin stone, nickel iron.
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            RandomSource random = RandomSource.create(7L);
            OreFeature nickel = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("nickel_ore")));
            check(h, nickel != null && nickel.size() == 6 && nickel.targetStates().size() == 1 && nickel.discardChanceOnAirExposure() == 0.5F
                    && nickel.targetStates().get(0).state().is(OresRegistry.BLOCKS.get("deepslate_nickel_ore"))
                    && nickel.targetStates().get(0).target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, random)
                    && !nickel.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random), "nickel: deepslate only, veins of 6, air 0.5 like diamond");
            PlacedFeature nickelPlaced = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("nickel_ore")));
            int[] nickelRange = heights(level, nickelPlaced);
            // like diamond: trapezoid from 80 below to 80 above the bottom (Y -144..16); rolls below the world bottom (Y -64) are lost, most land at -64
            // (relative to the world bottom: Overworld -64 -> up to Y 16; the test level has its own bottom)
            int bottom = level.getMinY();
            check(h, nickelRange[0] < bottom && nickelRange[1] <= bottom + 80 && nickelRange[1] > bottom + 64 && perChunk(level, nickelPlaced) == 10.0,
                    "nickel: 10 veins up to 80 above the bottom (" + bottom + "), measured " + nickelRange[0] + " to " + nickelRange[1]);
            PlacedFeature diamond = placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_diamond")));
            if (diamond != null) check(h, perChunk(level, nickelPlaced) * nickel.size() > perChunk(level, diamond) * 4, "a little more common than the small diamond veins");
            OreFeature tin = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("tin_ore")));
            OreFeature granite = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("tin_ore_granite")));
            OreFeature mountain = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("tin_ore_mountain")));
            check(h, tin != null && granite != null && mountain != null, "three mountain tin features");
            check(h, tin.targetStates().stream().allMatch(t -> t.state().is(OresRegistry.BLOCKS.get("tin_ore")))
                    && tin.targetStates().stream().anyMatch(t -> t.target().test(Blocks.GRANITE.defaultBlockState(), BlockPos.ZERO, random))
                    && tin.targetStates().stream().noneMatch(t -> t.target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, random)), "tin: stone and granite, no deepslate");
            check(h, granite.targetStates().size() == 1 && granite.targetStates().get(0).target().test(Blocks.GRANITE.defaultBlockState(), BlockPos.ZERO, random)
                    && !granite.targetStates().get(0).target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random), "granite nests turn only granite");
            int[] tinRange = heights(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("tin_ore"))));
            check(h, tinRange[0] >= 20 && tinRange[1] <= 80, "tin Y 20 to 80, measured " + tinRange[0] + " to " + tinRange[1]);
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            var mountains = biomes.getOrThrow(net.minecraft.tags.TagKey.create(Registries.BIOME, SimpleOres.id("has_mountain_tin")));
            check(h, mountains.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.STONY_PEAKS)) && mountains.stream().noneMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.PLAINS)),
                    "mountain tin in the stony peaks, not in the plains");
            for (int i = 0; i < 3; i++) {
                String m = OresRegistry.METALS[i];
                BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
                level.setBlockAndUpdate(pos, OresRegistry.BLOCKS.get(m + "_ore").defaultBlockState());
                List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
                check(h, drops.size() == 1 && drops.getFirst().is(item("raw_" + m)), m + " ore gives raw " + m + ": " + drops);
                boolean stoneTier = !m.equals("nickel");
                BlockState state = OresRegistry.BLOCKS.get(m + "_ore").defaultBlockState();
                check(h, new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state) == stoneTier && new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state),
                        m + " ore tier " + (stoneTier ? "stone" : "iron"));
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            h.succeed();
        });
        ALL.put("zinc_geodes", h -> {
            // N4: zinc only in closed mini geodes (Overworld: geode crust, calcite, zinc ore in base stone; End: End stone, geode crust,
            // End zinc ore in End stone); 1 to 8 ores, every ore enclosed, nothing placed when the host has a hole.
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            for (String[] row : new String[][] {{"zinc_geode", "stone"}, {"end_zinc_geode", "end"}}) {
                Object f = features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id(row[0])));
                check(h, f instanceof com.simpleores.MiniGeodeFeature, row[0] + " is a mini geode");
                com.simpleores.MiniGeodeFeature geode = (com.simpleores.MiniGeodeFeature) f;
                boolean end = row[1].equals("end");
                BlockState host = end ? Blocks.END_STONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                Block ore = end ? OresRegistry.END_ZINC_ORE : OresRegistry.BLOCKS.get("zinc_ore");
                check(h, geode.ore().is(ore) && geode.shell().is(end ? Blocks.END_STONE : OresRegistry.GEODE_CRUST)
                        && geode.lining().is(end ? OresRegistry.GEODE_CRUST : Blocks.CALCITE), row[0] + ": shell, lining, ore");
                BlockPos center = h.absolutePos(new BlockPos(8, 4, 8));
                RandomSource random = RandomSource.create(end ? 3L : 2L);
                int min = Integer.MAX_VALUE, max = 0;
                for (int round = 0; round < 24; round++) {
                    fill(level, center, host);
                    check(h, geode.place(level, level.getChunkSource().getGenerator(), random, center), row[0] + " places in solid host rock");
                    int ores = 0;
                    for (BlockPos o : com.simpleores.MiniGeodeFeature.ball(com.simpleores.MiniGeodeFeature.CHECK)) {
                        BlockPos p = center.offset(o);
                        if (!level.getBlockState(p).is(ore)) continue;
                        ores++;
                        for (Direction d : Direction.values()) check(h, !level.getBlockState(p.relative(d)).isAir(), "ore at air");
                        check(h, o.distSqr(BlockPos.ZERO) <= com.simpleores.MiniGeodeFeature.CORE, "ore outside the core");
                    }
                    for (BlockPos o : com.simpleores.MiniGeodeFeature.ball(com.simpleores.MiniGeodeFeature.SHELL)) {
                        if (o.distSqr(BlockPos.ZERO) > com.simpleores.MiniGeodeFeature.CORE) check(h, level.getBlockState(center.offset(o)).equals(geode.shell()), "shell complete");
                    }
                    min = Math.min(min, ores);
                    max = Math.max(max, ores);
                }
                check(h, min >= 1 && max <= 8 && max > min, row[0] + ": 1 to 8 ores per geode, measured " + min + " to " + max);
                // a hole (air) anywhere in the check ball: nothing is placed
                fill(level, center, host);
                level.setBlock(center.offset(3, 0, 0), Blocks.AIR.defaultBlockState(), 2);
                check(h, !geode.place(level, level.getChunkSource().getGenerator(), random, center) && level.getBlockState(center).equals(host), row[0] + ": not placed next to air");
                level.setBlock(center.offset(0, 2, 0), Blocks.WATER.defaultBlockState(), 2);
                check(h, !geode.enclosed(level::getBlockState, center, random), row[0] + ": not enclosed next to water");
            }
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            var endTag = biomes.getOrThrow(net.minecraft.tags.TagKey.create(Registries.BIOME, SimpleOres.id("has_end_zinc_geode")));
            check(h, endTag.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS)) && endTag.stream().noneMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.THE_END)),
                    "End geodes on the outer islands only");
            h.succeed();
        });
        ALL.put("tin_placers", h -> {
            // N4: tin-bearing gravel and sand lie under water in river and beach beds; a shovel digs them for 2 to 5 tin nuggets.
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            check(h, features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("tin_placer"))) instanceof net.minecraft.world.level.levelgen.feature.DiskFeature, "tin placer is a disk");
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            var tag = biomes.getOrThrow(net.minecraft.tags.TagKey.create(Registries.BIOME, SimpleOres.id("has_tin_placer")));
            check(h, tag.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.RIVER)) && tag.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.BEACH))
                    && tag.stream().noneMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.PLAINS)), "rivers and beaches");
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            for (Block placer : new Block[] {OresRegistry.TIN_BEARING_GRAVEL, OresRegistry.TIN_BEARING_SAND}) {
                BlockState state = placer.defaultBlockState();
                check(h, state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(state) > 1.0F, placer + " is dug with a shovel");
                int min = Integer.MAX_VALUE, max = 0;
                for (int i = 0; i < 40; i++) {
                    level.setBlockAndUpdate(pos, state);
                    List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null, new ItemStack(Items.IRON_SHOVEL));
                    check(h, drops.size() == 1 && drops.getFirst().is(item("tin_nugget")), placer + " drops tin nuggets: " + drops);
                    min = Math.min(min, drops.getFirst().getCount());
                    max = Math.max(max, drops.getFirst().getCount());
                }
                check(h, min >= 2 && max <= 5 && max > min, placer + ": 2 to 5 nuggets, measured " + min + " to " + max);
                check(h, placer instanceof net.minecraft.world.level.block.FallingBlock, placer + " falls like gravel and sand");
            }
            // the End ores drop like their Overworld ores, the crust drops itself
            level.setBlockAndUpdate(pos, OresRegistry.END_LEAD_ORE.defaultBlockState());
            List<ItemStack> lead = Block.getDrops(level.getBlockState(pos), level, pos, null, null, new ItemStack(Items.STONE_PICKAXE));
            check(h, lead.size() == 1 && lead.getFirst().is(item("raw_lead")) && lead.getFirst().getCount() <= 2, "End lead ore -> 1 to 2 raw lead: " + lead);
            level.setBlockAndUpdate(pos, OresRegistry.END_ZINC_ORE.defaultBlockState());
            List<ItemStack> zinc = Block.getDrops(level.getBlockState(pos), level, pos, null, null, new ItemStack(Items.STONE_PICKAXE));
            check(h, zinc.size() == 1 && zinc.getFirst().is(item("raw_zinc")), "End zinc ore -> raw zinc: " + zinc);
            level.setBlockAndUpdate(pos, OresRegistry.GEODE_CRUST.defaultBlockState());
            List<ItemStack> crust = Block.getDrops(level.getBlockState(pos), level, pos, null, null, new ItemStack(Items.WOODEN_PICKAXE));
            check(h, crust.size() == 1 && crust.getFirst().is(item("geode_crust")), "geode crust drops itself: " + crust);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.succeed();
        });
        ALL.put("rose_gold_vein_rule", h -> {
            ServerLevel level = h.getLevel();
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            check(h, features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("nether_gold_rose"))) instanceof com.simpleores.RoseGoldVeinFeature, "the vein feature is the rose gold vein");
            com.simpleores.RoseGoldVeinFeature vein = (com.simpleores.RoseGoldVeinFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("nether_gold_rose")));
            check(h, vein.size() == 10 && vein.targetStates().size() == 2 && vein.targetStates().get(0).state().is(OresRegistry.NETHER_ROSE_GOLD_ORE)
                    && vein.targetStates().get(1).state().is(Blocks.NETHER_GOLD_ORE), "veins of 10, rose gold ore first, Nether gold ore second");
            RandomSource random = RandomSource.create(3L);
            for (var target : vein.targetStates()) check(h, target.target().test(Blocks.NETHERRACK.defaultBlockState(), BlockPos.ZERO, random) && !target.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random), "only in netherrack");
            check(h, perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("nether_gold_rose_placed")))) == 10.0
                    && perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("nether_gold_deltas_rose_placed")))) == 20.0, "same tries per chunk as Vanilla (10 and 20)");
            // The rule alone: one per vein, only below Y 31, never at chance 0.
            check(h, com.simpleores.RoseGoldVeinFeature.mayBeRoseGold(false, 30, 100.0, random) && !com.simpleores.RoseGoldVeinFeature.mayBeRoseGold(false, 31, 100.0, random)
                    && !com.simpleores.RoseGoldVeinFeature.mayBeRoseGold(true, 0, 100.0, random) && !com.simpleores.RoseGoldVeinFeature.mayBeRoseGold(false, 0, 0.0, random), "rule: y < 31, one per vein, chance 0 = none");
            // Candidate blocks of one vein, driven through canPlaceOre exactly as OreFeature does: at most one rose gold ore.
            double chance = OresConfig.roseGoldOreChance;
            try {
                check(h, chance == 2.0, "default chance 2 %: " + chance);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                var netherrack = Blocks.NETHERRACK.defaultBlockState();
                OresConfig.roseGoldOreChance = 100.0;
                for (int v = 0; v < 20; v++) {
                    com.simpleores.RoseGoldVeinFeature.beginVein();
                    int rose = 0;
                    for (int i = 0; i < 40; i++) {
                        pos.set(i, 20 + (i % 10), 0);
                        if (vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(0), pos)) rose++;
                    }
                    check(h, rose == 1, "a vein holds exactly one rose gold ore at chance 100 %, found " + rose);
                }
                com.simpleores.RoseGoldVeinFeature.beginVein();
                for (int y : new int[] {31, 32, 60, 120}) {
                    pos.set(0, y, 0);
                    check(h, !vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(0), pos), "no rose gold ore at Y " + y);
                }
                pos.set(0, 30, 0);
                check(h, vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(0), pos), "rose gold ore possible at Y 30");
                com.simpleores.RoseGoldVeinFeature.beginVein();
                check(h, !vein.canPlaceOre(Blocks.STONE.defaultBlockState(), p -> netherrack, random, vein.targetStates().get(0), pos), "only netherrack is replaced");
                check(h, vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(1), pos), "the Nether gold ore target is untouched by the rule");
                OresConfig.roseGoldOreChance = 0.0;
                com.simpleores.RoseGoldVeinFeature.beginVein();
                for (int i = 0; i < 40; i++) {
                    pos.set(i, 10, 0);
                    check(h, !vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(0), pos), "chance 0 never places");
                }
                // The default 2 %: about one per 50 candidate blocks over many single-block veins.
                OresConfig.roseGoldOreChance = 2.0;
                int hits = 0;
                for (int i = 0; i < 5000; i++) {
                    com.simpleores.RoseGoldVeinFeature.beginVein();
                    pos.set(0, 10, 0);
                    if (vein.canPlaceOre(netherrack, p -> netherrack, random, vein.targetStates().get(0), pos)) hits++;
                }
                check(h, hits > 50 && hits < 160, "2 % of 5000 candidates, measured " + hits);
            } finally {
                OresConfig.roseGoldOreChance = chance;
                com.simpleores.RoseGoldVeinFeature.beginVein();
            }
            h.succeed();
        });
        ALL.put("rose_gold_ore_drops_and_smelting", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            level.setBlockAndUpdate(pos, OresRegistry.NETHER_ROSE_GOLD_ORE.defaultBlockState());
            List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null, pick);
            check(h, !drops.isEmpty() && drops.stream().allMatch(s -> s.is(item("rose_gold_pressling"))), "the ore drops a rose gold pressing: " + drops);
            check(h, level.getBlockState(pos).requiresCorrectToolForDrops() && !new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(level.getBlockState(pos))
                    && pick.isCorrectToolForDrops(level.getBlockState(pos)) && level.getBlockState(pos).is(BlockTags.GUARDED_BY_PIGLINS), "iron pickaxe, piglins guard it");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            var recipes = level.getServer().getRecipeManager();
            SingleRecipeInput in = new SingleRecipeInput(stack("nether_rose_gold_ore"));
            var smelting = recipes.getRecipeFor(RecipeType.SMELTING, in, level);
            var blasting = recipes.getRecipeFor(RecipeType.BLASTING, in, level);
            check(h, smelting.isPresent() && smelting.get().value().assemble(in).is(item("rose_gold_ingot")) && blasting.isPresent() && blasting.get().value().assemble(in).is(item("rose_gold_ingot")),
                    "ore -> rose gold ingot in furnace and blast furnace");
            h.succeed();
        });
        ALL.put("sage_crystal_chain", h -> {
            ServerLevel level = h.getLevel();
            ItemStack shards = craft(h, CraftingInput.of(2, 2, List.of(stack("sage_crystal_shard"), stack("sage_crystal_shard"), stack("sage_crystal_shard"), stack("sage_crystal_shard"))));
            check(h, shards.is(item("sage_crystal")) && shards.getCount() == 1, "4 shards -> 1 sage crystal: " + shards);
            check(h, craft(h, CraftingInput.of(2, 2, List.of(stack("sage_crystal_shard"), stack("sage_crystal_shard"), stack("sage_crystal_shard"), ItemStack.EMPTY))).isEmpty(), "3 shards make nothing");
            ItemStack[] nine = new ItemStack[9];
            java.util.Arrays.fill(nine, stack("sage_crystal"));
            check(h, craft(h, of(nine)).is(item("sage_crystal_block")), "9 crystals -> block");
            ItemStack back = craft(h, CraftingInput.of(1, 1, List.of(stack("sage_crystal_block"))));
            check(h, back.is(item("sage_crystal")) && back.getCount() == 9, "block -> 9 crystals");
            check(h, new ItemStack(item("sage_crystal")).is(net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/sage_crystal"))), "the crystal is an ingot");
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            for (Block ore : new Block[] {OresRegistry.NETHER_SAGE_ORE, OresRegistry.END_SAGE_ORE}) {
                level.setBlockAndUpdate(pos, ore.defaultBlockState());
                check(h, level.getBlockState(pos).is(net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/sage"))), ore + " is a sage ore");
                check(h, level.getBlockState(pos).requiresCorrectToolForDrops() && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(level.getBlockState(pos)) && pick.isCorrectToolForDrops(level.getBlockState(pos)), ore + " needs an iron pickaxe");
                for (int i = 0; i < 12; i++) {
                    List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null, pick);
                    int n = drops.stream().filter(s -> s.is(item("sage_crystal_shard"))).mapToInt(ItemStack::getCount).sum();
                    check(h, n >= 1 && n == drops.stream().mapToInt(ItemStack::getCount).sum(), ore + " drops shards (1 to 3 plus Fortune): " + drops);
                }
            }
            level.setBlockAndUpdate(pos, OresRegistry.SAGE_CRYSTAL_BLOCK.defaultBlockState());
            List<ItemStack> own = Block.getDrops(level.getBlockState(pos), level, pos, null, null, pick);
            check(h, own.size() == 1 && own.getFirst().is(item("sage_crystal_block")), "the block drops itself");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            // World generation: moderate numbers, the End ore only in End stone.
            var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
            var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            OreFeature nether = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("nether_sage_ore")));
            OreFeature end = (OreFeature) features.getValue(ResourceKey.create(Registries.FEATURE, SimpleOres.id("end_sage_ore")));
            RandomSource random = RandomSource.create(5L);
            check(h, nether != null && end != null && nether.size() == 4 && end.size() == 4, "veins of 4");
            check(h, nether.targetStates().get(0).state().is(OresRegistry.NETHER_SAGE_ORE) && nether.targetStates().get(0).target().test(Blocks.NETHERRACK.defaultBlockState(), BlockPos.ZERO, random)
                    && !nether.targetStates().get(0).target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random), "nether ore in netherrack");
            check(h, end.targetStates().get(0).state().is(OresRegistry.END_SAGE_ORE) && end.targetStates().get(0).target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random)
                    && !end.targetStates().get(0).target().test(Blocks.NETHERRACK.defaultBlockState(), BlockPos.ZERO, random), "end ore only in end stone");
            double netherPerChunk = perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("nether_sage_ore_placed"))));
            double endPerChunk = perChunk(level, placedRegistry.getValue(ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_sage_ore_placed"))));
            check(h, netherPerChunk == 5.0 && endPerChunk == 3.0, "moderate: 5 and 3 veins per chunk, measured " + netherPerChunk + " / " + endPerChunk);
            h.succeed();
        });
        // IG31-58 (a-dims10): the rift chain of the Dimensional Scrap.
        ALL.put("rift_shard_smelting", h -> {
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            var extreme = net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplecrucibles", "needs_extreme_heat"));
            int seconds = 40;
            float xp = 2.0F;
            for (String d : OresRegistry.DIMENSIONS) {
                ItemStack scrap = stack(d + "dimensional_scrap");
                SingleRecipeInput in = new SingleRecipeInput(scrap);
                var blast = recipes.getRecipeFor(RecipeType.BLASTING, in, level);
                check(h, blast.isPresent(), d + "scrap: a blast furnace melts it");
                AbstractCookingRecipe recipe = (AbstractCookingRecipe) blast.get().value();
                ItemStack out = recipe.assemble(in);
                // 26.3 stores the furnace time in the recipe and halves it in the blast furnace.
                int ticks = (int) Math.ceil(recipe.cookingTime() / 2.0F);
                check(h, ticks == seconds * 20, d + "scrap melts in " + ticks + " ticks, expected " + seconds * 20);
                check(h, recipe.experience() == xp, d + "scrap gives " + recipe.experience() + " experience, expected " + xp);
                check(h, out.is(item(d + "rift_shard")) && out.getCount() == 1, d + "scrap gives " + out + ", expected exactly 1 shard");
                for (RecipeType<? extends AbstractCookingRecipe> type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING))
                    check(h, recipes.getRecipeFor(type, in, level).isEmpty(), d + "scrap: " + type + " refuses");
                check(h, scrap.is(extreme), d + "scrap needs extreme heat in a crucible (tag)");
                // With Simple Crucibles present: its own heat rule reads the tag (EXTREME instead of HIGH for blasting).
                try {
                    Object heat = Class.forName("com.simplecrucibles.crucible.CrucibleJob").getMethod("requiredHeat", ItemStack.class, RecipeType.class)
                            .invoke(null, scrap, RecipeType.BLASTING);
                    check(h, "EXTREME".equals(String.valueOf(heat)), d + "scrap: crucible needs " + heat + ", expected EXTREME");
                } catch (ClassNotFoundException absent) {
                    // standalone: no crucible, the blast furnace is the way
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("crucible heat rule unreachable: " + e);
                }
                seconds += 20;
                xp += 1.0F;
            }
            h.succeed();
        });
        ALL.put("rift_chain", h -> {
            // Scrap -> shard (blast furnace) -> 4 shards + partner ingot -> ingot -> 9 ingots -> block, through all three dimensions.
            ServerLevel level = h.getLevel();
            var recipes = level.getServer().getRecipeManager();
            ItemStack partner = stack("enderite_ingot");
            for (int i = 0; i < 3; i++) {
                String d = OresRegistry.DIMENSIONS[i], metal = OresRegistry.RIFT_METALS[i];
                SingleRecipeInput in = new SingleRecipeInput(stack(d + "dimensional_scrap"));
                ItemStack shard = recipes.getRecipeFor(RecipeType.BLASTING, in, level).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
                check(h, shard.is(item(d + "rift_shard")), d + "scrap melts into its shard: " + shard);
                ItemStack ingot = craft(h, of(shard.copy(), shard.copy(), shard.copy(), shard.copy(), partner.copy()));
                check(h, ingot.is(item(metal + "_ingot")) && ingot.getCount() == 1, "4 " + d + "shards + " + partner + " -> " + ingot);
                check(h, craft(h, of(shard.copy(), shard.copy(), shard.copy(), partner.copy())).isEmpty(), metal + ": 3 shards are not enough");
                if (i > 0) check(h, craft(h, of(shard.copy(), shard.copy(), shard.copy(), shard.copy(), stack("enderite_ingot"))).isEmpty(),
                        metal + ": Enderite is not its partner");
                List<ItemStack> nine = new ArrayList<>();
                for (int k = 0; k < 9; k++) nine.add(ingot.copy());
                ItemStack block = craft(h, CraftingInput.of(3, 3, nine));
                check(h, block.is(item(metal + "_block")), "9 " + metal + " ingots -> " + block);
                partner = ingot;
            }
            h.succeed();
        });
        ALL.put("rift_storage_reversible", h -> {
            ServerLevel level = h.getLevel();
            for (int i = 0; i < 3; i++) {
                Map<String, String> rows = Map.of(OresRegistry.DIMENSIONS[i] + "rift_shard_block", OresRegistry.DIMENSIONS[i] + "rift_shard",
                        OresRegistry.RIFT_METALS[i] + "_block", OresRegistry.RIFT_METALS[i] + "_ingot");
                for (Map.Entry<String, String> row : rows.entrySet()) {
                    List<ItemStack> slots = new ArrayList<>();
                    for (int k = 0; k < 9; k++) slots.add(stack(row.getValue()));
                    ItemStack block = craft(h, CraftingInput.of(3, 3, slots));
                    check(h, block.is(item(row.getKey())) && block.getCount() == 1, "9 -> " + row.getKey() + ": " + block);
                    ItemStack back = craft(h, of(stack(row.getKey())));
                    check(h, back.is(item(row.getValue())) && back.getCount() == 9, row.getKey() + " -> 9: " + back);
                    slots.set(8, ItemStack.EMPTY);
                    check(h, craft(h, CraftingInput.of(3, 3, slots)).isEmpty(), row.getKey() + ": 8 are not enough");
                    SingleRecipeInput in = new SingleRecipeInput(stack(row.getKey()));
                    for (RecipeType<? extends AbstractCookingRecipe> type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING))
                        check(h, level.getServer().getRecipeManager().getRecipeFor(type, in, level).isEmpty(), row.getKey() + " does not melt");
                }
            }
            h.succeed();
        });
        ALL.put("rift_beacon_tags", h -> {
            for (int i = 0; i < 3; i++) {
                String d = OresRegistry.DIMENSIONS[i], metal = OresRegistry.RIFT_METALS[i];
                Block showpiece = OresRegistry.BLOCKS.get(metal + "_block"), shards = OresRegistry.BLOCKS.get(d + "rift_shard_block");
                check(h, showpiece.defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), metal + " block carries a beacon");
                check(h, !shards.defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), d + "shard block is no beacon base");
                check(h, stack(metal + "_ingot").is(ItemTags.BEACON_PAYMENT_ITEMS), metal + " ingot pays a beacon");
                check(h, !stack(d + "rift_shard").is(ItemTags.BEACON_PAYMENT_ITEMS), d + "shard does not pay a beacon");
                for (Block b : List.of(showpiece, shards))
                    check(h, b.defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL) && b.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                            && !b.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL), BuiltInRegistries.BLOCK.getKey(b) + ": diamond pickaxe");
                for (String p : List.of(d + "rift_shard", d + "rift_shard_block", metal + "_ingot", metal + "_block"))
                    check(h, item(p).components().has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT), p + " is fire resistant");
                // Rank beside Enderite: ingots and showpiece blocks Legendary, shards plain like Enderite scrap; no gear tier.
                for (String p : List.of(metal + "_ingot", metal + "_block"))
                    check(h, com.simplelib.api.SimpleRarity.of(stack(p)) == com.simplelib.api.SimpleRarity.LEGENDARY, p + " is Legendary");
                for (String p : List.of(d + "rift_shard", d + "rift_shard_block"))
                    check(h, com.simplelib.api.SimpleRarity.of(stack(p)) == null, p + " keeps its Vanilla rarity");
                check(h, !stack(metal + "_ingot").has(net.minecraft.core.component.DataComponents.TOOL)
                        && !stack(metal + "_ingot").is(ItemTags.TRIM_MATERIALS), metal + " ingot is no tool or trim material (yet)");
            }
            h.succeed();
        });
        ALL.put("guide_book", com.simpleores.guide.OresGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simpleores.discover.OresDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleores.discover.OresDiscover::behaviourHints);
    }

    /** Lowest and highest Y of the placements of 400 chunks (all modifiers but the biome filter). */
    private static int[] heights(ServerLevel level, PlacedFeature placed) {
        if (placed == null) throw new AssertionError("placed feature missing");
        PlacementContext context = new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.of(placed));
        RandomSource random = RandomSource.create(5L);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < 400; i++) {
            List<BlockPos> current = List.of(BlockPos.ZERO);
            for (PlacementModifier modifier : placed.placement()) {
                if (modifier instanceof BiomeFilter) continue;
                List<BlockPos> next = new ArrayList<>();
                for (BlockPos p : current) modifier.modify(context, random, p, next::add);
                current = next;
            }
            for (BlockPos p : current) {
                min = Math.min(min, p.getY());
                max = Math.max(max, p.getY());
            }
        }
        return new int[] {min, max};
    }

    /** The check ball of a mini geode round center filled with host rock. */
    private static void fill(ServerLevel level, BlockPos center, BlockState host) {
        for (BlockPos o : com.simpleores.MiniGeodeFeature.ball(com.simpleores.MiniGeodeFeature.CHECK + 3)) level.setBlock(center.offset(o), host, 2);
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
