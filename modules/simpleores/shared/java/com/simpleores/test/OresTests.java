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
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import com.simpleores.block.SilverMirrorBlock;
import com.simpleores.entity.SilverArrow;
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
            check(h, OresRegistry.BLOCKS.size() == 12 && OresRegistry.ITEMS.size() == 12 + 3 + 3 + 1 + 4 + 18, "block/item count " + OresRegistry.BLOCKS.size() + "/" + OresRegistry.ITEMS.size());
            check(h, OresConfig.enabled && OresConfig.KEYS.equals(List.of("enabled", "soundproofing", "soundproofFactor", "soundproofThreshold", "soundproofRecords", "silverArrowUndeadBonus"))
                    && OresConfig.soundproofing && OresConfig.soundproofFactor == 0.5 && OresConfig.soundproofThreshold == 0.05 && !OresConfig.soundproofRecords
                    && OresConfig.silverArrowUndeadBonus == 3, "config defaults");
            h.succeed();
        });
        ALL.put("silver_is_no_tool_material", h -> {
            // Owner ABN30-10: silver is an ingredient and a building metal, never a tool or armor material.
            for (String path : OresRegistry.ITEMS.keySet()) {
                boolean silverGear = path.startsWith("silver_") && !path.equals("silver_ingot") && !path.equals("silver_nugget") && !path.equals("silver_block")
                        && !path.equals("silver_ore") && !path.equals("silver_arrow") && !path.equals("silver_mirror");
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
                player.setPos(to.x, to.y - player.getEyeHeight(), to.z);
                ClientboundSoundPacket loud = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, from.x, from.y, from.z, 4.0F, 1.0F, 1L);
                Packet<?>[] out = Soundproofing.filter(player, loud);
                check(h, out != null && out.length == 1 && out[0] instanceof ClientboundSoundPacket p && p.getVolume() == 0.5F, "TNT volume 4 -> 0.5 behind 3 lead blocks");
                ClientboundSoundPacket soft = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, from.x, from.y, from.z, 0.2F, 1.0F, 1L);
                check(h, Soundproofing.filter(player, soft) == null, "sounds under 0.3 are never touched");
                ClientboundSoundPacket record = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.RECORDS, from.x, from.y, from.z, 4.0F, 1.0F, 1L);
                check(h, Soundproofing.filter(player, record) == null, "records are not muffled by default");
                OresConfig.soundproofRecords = true;
                check(h, Soundproofing.filter(player, record) != null, "records are muffled when switched on");
                OresConfig.soundproofRecords = false;
                OresConfig.soundproofThreshold = 0.3;
                ClientboundSoundPacket quiet = new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, from.x, from.y, from.z, 1.0F, 1.0F, 1L);
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
        ALL.put("silver_arrow_recipe", h -> {
            CraftingInput arrow = CraftingInput.of(1, 3, List.of(stack("silver_nugget"), new ItemStack(Items.STICK), new ItemStack(Items.FEATHER)));
            ItemStack out = craft(h, arrow);
            check(h, out.is(item("silver_arrow")) && out.getCount() == 4, "nugget / stick / feather -> 4 silver arrows: " + out);
            check(h, craft(h, CraftingInput.of(1, 3, List.of(new ItemStack(Items.FEATHER), new ItemStack(Items.STICK), stack("silver_nugget")))).isEmpty(), "upside down: nothing");
            check(h, stack("silver_arrow").is(ItemTags.ARROWS), "a bow takes it as an arrow");
            check(h, BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(OresRegistry.SILVER_ARROW_ENTITY).is(net.minecraft.tags.EntityTypeTags.ARROWS), "an arrow entity");
            check(h, BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleOres.id("silver_arrow")), "entity type registered");
            List<ItemStack> mirror = new ArrayList<>(List.of(ItemStack.EMPTY, stack("silver_ingot"), ItemStack.EMPTY, stack("silver_ingot"), new ItemStack(Items.GLASS_PANE),
                    stack("silver_ingot"), ItemStack.EMPTY, stack("silver_ingot"), ItemStack.EMPTY));
            check(h, craft(h, CraftingInput.of(3, 3, mirror)).is(item("silver_mirror")), "4 silver ingots round a glass pane -> mirror");
            h.succeed();
        });
        ALL.put("silver_arrow_undead_bonus", h -> {
            ServerLevel level = h.getLevel();
            try {
                OresConfig.silverArrowUndeadBonus = 3;
                float silverZombie = damage(h, EntityTypes.ZOMBIE, true, 1);
                float vanillaZombie = damage(h, EntityTypes.ZOMBIE, false, 1);
                check(h, silverZombie - vanillaZombie > 2.5F && silverZombie - vanillaZombie < 3.5F, "undead: +3 damage (armor takes a little): silver " + silverZombie + " vs " + vanillaZombie);
                float silverSheep = damage(h, EntityTypes.SHEEP, true, 5);
                float vanillaSheep = damage(h, EntityTypes.SHEEP, false, 5);
                check(h, silverSheep == vanillaSheep && vanillaSheep > 0.0F, "a sheep takes the Vanilla damage: " + silverSheep + " vs " + vanillaSheep);
                OresConfig.silverArrowUndeadBonus = 0;
                float off = damage(h, EntityTypes.ZOMBIE, true, 9);
                check(h, Math.abs(off - vanillaZombie) < 0.01F, "bonus 0 = Vanilla damage: " + off + " vs " + vanillaZombie);
                OresConfig.silverArrowUndeadBonus = 6;
                float max = damage(h, EntityTypes.ZOMBIE, true, 13);
                check(h, max - vanillaZombie > 5.5F && max - vanillaZombie < 6.5F, "bonus 6: " + max + " vs " + vanillaZombie);
            } finally {
                OresConfig.reset();
            }
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

    /**
     * Health a fresh mob of the type loses when a silver or Vanilla arrow flying at speed 3 hits it. Mobs stand 2 cells apart per call
     * ({@code slot}) so the invulnerability frames of an earlier hit never matter.
     */
    private static <E extends net.minecraft.world.entity.Mob> float damage(GameTestHelper h, net.minecraft.world.entity.EntityType<E> type, boolean silver, int slot) {
        E mob = h.spawnWithNoFreeWill(type, slot, 1, 12);
        float before = mob.getHealth();
        net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow = silver
                ? new SilverArrow(h.getLevel(), mob.getX() - 3.0, mob.getY() + 1.0, mob.getZ(), stack("silver_arrow"), null)
                : new HitArrow(h.getLevel(), mob.getX() - 3.0, mob.getY() + 1.0, mob.getZ());
        arrow.setDeltaMovement(3.0, 0.0, 0.0);
        if (arrow instanceof SilverArrow silverArrow) silverArrow.onHitEntity(new EntityHitResult(mob));
        else ((HitArrow) arrow).hit(new EntityHitResult(mob));
        float lost = before - mob.getHealth();
        mob.discard();
        return lost;
    }

    /** A Vanilla arrow that can be made to hit without flying (onHitEntity is protected). */
    private static final class HitArrow extends Arrow {
        HitArrow(net.minecraft.world.level.Level level, double x, double y, double z) {
            super(level, x, y, z, new ItemStack(Items.ARROW), null);
        }

        void hit(EntityHitResult result) {
            onHitEntity(result);
        }
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
