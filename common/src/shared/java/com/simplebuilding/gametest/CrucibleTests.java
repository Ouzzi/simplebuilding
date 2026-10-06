package com.simplebuilding.gametest;

import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.fluid.ModBucketItem;
import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.fluid.SoulLava;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crucible P5 in SimpleBuilding (docs/ai/PLAN-CRUCIBLE-P5-2026-10-05.md): the Enderite tiers, the sledgehammer ways,
 * soul lava (flow, unreplaceable, water side, burning, Seelenbrand, heat), the buckets, the reinforced cauldron and the
 * quartz crush. On lines without {@link McVersion#CRUCIBLE} every test passes at once.
 */
public final class CrucibleTests {
    private CrucibleTests() {
    }

    public static void jadeReportsHeatSlotsAndShortestRemainingTime(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        int smokingTicks = helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMOKING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.BEEF)), helper.getLevel()).orElseThrow().value().cookingTime();
        Block[] blocks = {lib("iron_crucible"), lib("reinforced_crucible"), lib("netherite_crucible"), CrucibleCompat.enderiteCrucible()};
        int[] sizes = {6, 9, 18, 27};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(i + 1, 2, 2);
            helper.setBlock(pos.below(), Blocks.CAMPFIRE);
            helper.setBlock(pos, blocks[i]);
            var be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            ((net.minecraft.world.Container) be).setItem(0, new ItemStack(Items.BEEF, 2));
            ((net.minecraft.world.Container) be).setItem(1, new ItemStack(Items.DIRT));
        }
        BlockPos distant = new BlockPos(1, 3, 4);
        helper.setBlock(distant.below(2), Blocks.CAMPFIRE);
        helper.setBlock(distant, blocks[0]);
        ((net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(distant))).setItem(0, new ItemStack(Items.BEEF, 2));
        BlockPos mixed = new BlockPos(5, 2, 4);
        helper.setBlock(mixed.below(), Blocks.CAMPFIRE);
        helper.setBlock(mixed, blocks[0]);
        var mixedInventory = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(mixed));
        mixedInventory.setItem(0, new ItemStack(Items.BEEF, 2));
        mixedInventory.setItem(1, new ItemStack(Items.BREAD));
        BlockPos blocked = new BlockPos(5, 2, 6);
        helper.setBlock(blocked.below(), Blocks.CAMPFIRE);
        helper.setBlock(blocked, blocks[0]);
        var blockedInventory = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(blocked));
        blockedInventory.setItem(0, new ItemStack(Items.BEEF, 2));
        for (int slot = 1; slot < blockedInventory.getContainerSize(); slot++) blockedInventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < blocks.length; i++) {
                var be = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(i + 1, 2, 2)));
                int[] status = CrucibleCompat.status(be);
                helper.assertTrue(status.length == 4 && status[0] == 1 && status[1] == 2 && status[2] == sizes[i], "heat and occupied slots tier " + i);
                helper.assertTrue(status[3] > 0 && status[3] <= smokingTicks * 2, "running duration tier " + i + ": " + status[3]);
                var topic = com.simplebuilding.compat.BlockInfo.Topic.CRUCIBLE;
                helper.assertTrue(com.simplebuilding.compat.BlockInfo.handles(topic, be), "Jade handles tier " + i);
                var lines = com.simplebuilding.compat.BlockInfo.serverLines(topic, be);
                helper.assertTrue(lines.size() == 3 && lines.get(2).argTexts().equals(java.util.List.of(String.valueOf((status[3] + 19) / 20))), "Jade seconds rounded up");
                var tag = new net.minecraft.nbt.CompoundTag();
                com.simplebuilding.compat.BlockInfo.write(tag, topic, lines);
                helper.assertTrue(com.simplebuilding.compat.BlockInfo.read(tag, topic).equals(lines), "Jade server/client round trip");
            }
            int nearTicks = CrucibleCompat.status(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 2))))[3];
            int distantTicks = CrucibleCompat.status(helper.getLevel().getBlockEntity(helper.absolutePos(distant)))[3];
            helper.assertTrue(distantTicks > nearTicks, "two-block heat penalty included in remaining time");
            int mixedTicks = CrucibleCompat.status(helper.getLevel().getBlockEntity(helper.absolutePos(mixed)))[3];
            helper.assertTrue(mixedTicks > 0 && mixedTicks < nearTicks, "shortest job wins over the first slot");
            helper.assertTrue(CrucibleCompat.status(helper.getLevel().getBlockEntity(helper.absolutePos(blocked)))[3] == -1, "blocked jobs have no finite remaining time");
            helper.setBlock(new BlockPos(5, 2, 2), blocks[0]);
            var cold = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5, 2, 2)));
            helper.assertTrue(CrucibleCompat.status(cold)[3] == -1 && com.simplebuilding.compat.BlockInfo.serverLines(com.simplebuilding.compat.BlockInfo.Topic.CRUCIBLE, cold).size() == 2, "cold crucible has no remaining time");
            helper.succeed();
        });
    }

    public static void recipeHeatAndCatalogMatchCookingRules(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        for (var type : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,
                net.minecraft.world.item.crafting.RecipeType.SMOKING, net.minecraft.world.item.crafting.RecipeType.SMELTING,
                net.minecraft.world.item.crafting.RecipeType.BLASTING)) {
            int expected = type == net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING || type == net.minecraft.world.item.crafting.RecipeType.SMOKING ? 1 : 2;
            helper.assertTrue(CrucibleCompat.requiredHeat(new ItemStack(Items.BEEF), type) == expected, "recipe heat " + type);
            helper.assertTrue(CrucibleCompat.requiredHeat(new ItemStack(Items.ANCIENT_DEBRIS), type) == 3, "extreme tag overrides " + type);
            helper.assertTrue(CrucibleCompat.requiredHeat(new ItemStack(ModItems.CRACKED_DIAMOND), type) == 3, "SB extreme tag overrides " + type);
        }
        var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.AbstractCookingRecipe>();
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.BEEF));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMOKING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, helper.getLevel()).ifPresent(holder -> recipes.add(holder.value()));
        var entries = com.simplebuilding.compat.CrucibleRecipeCatalog.build(recipes);
        int fastestMedium = recipes.stream().filter(recipe -> recipe.getType() != net.minecraft.world.item.crafting.RecipeType.SMELTING)
                .mapToInt(net.minecraft.world.item.crafting.AbstractCookingRecipe::cookingTime).min().orElseThrow();
        var beef = entries.stream().filter(entry -> entry.input().is(Items.BEEF)).toList();
        helper.assertTrue(beef.size() == 2, "one beef entry per heat");
        helper.assertTrue(beef.stream().anyMatch(entry -> entry.heat() == 1 && entry.baseTicks() == fastestMedium && entry.result().is(Items.COOKED_BEEF)), "fastest medium recipe wins");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.input().is(Items.BREAD) && entry.warming() && entry.heat() == 1), "bread warming fallback");
        helper.assertTrue(beef.stream().noneMatch(com.simplebuilding.compat.CrucibleRecipeCatalog.Entry::warming), "cooking takes precedence over warming");
        helper.assertTrue(CrucibleCompat.cookingTicks(100, 1) == 200 && CrucibleCompat.cookingTicks(200, 2) == 267, "display time includes heat factor and rounds ticks up");
        helper.succeed();
    }

    /**
     * Jade for the milk cauldron (content, ripeness, what to do) and the reinforced cauldron (content, fill level of
     * water and powder snow), computed from the block state alone and found by registry id. Expected strings are this
     * test's own; the reinforced cauldron is checked with and without a {@code level} property.
     */
    public static void milkAndReinforcedCauldronsShowTheirContents(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block milk = BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplesandwiches:milk_cauldron"));
        // The SimpleBuilding test run does not load SimpleSandwiches: the milk states are only checked where the module is present.
        if (milk != Blocks.AIR) milkCauldron(helper, milk);
        reinforcedCauldron(helper);
        helper.succeed();
    }

    private static void milkCauldron(GameTestHelper helper, Block milk) {
        String content = "jade.simplebuilding.cauldron.content";
        cauldron(helper, milk, new String[] {"content", "milk", "stage", "0"},
                java.util.List.of(content + " [" + content + ".milk]", "jade.simplebuilding.cauldron.ripeness [1, 4]"));
        cauldron(helper, milk, new String[] {"content", "milk", "stage", "2"},
                java.util.List.of(content + " [" + content + ".milk]", "jade.simplebuilding.cauldron.ripeness [3, 4]"));
        cauldron(helper, milk, new String[] {"content", "curdling", "stage", "3"},
                java.util.List.of(content + " [" + content + ".curdling]", "jade.simplebuilding.cauldron.ripeness [4, 4]"));
        cauldron(helper, milk, new String[] {"content", "butter", "stage", "0"},
                java.util.List.of(content + " [" + content + ".butter]", "jade.simplebuilding.cauldron.ready []"));
        cauldron(helper, milk, new String[] {"content", "cheese", "stage", "0"},
                java.util.List.of(content + " [" + content + ".cheese]", "jade.simplebuilding.cauldron.ready []",
                        "jade.simplebuilding.cauldron.cheese_warning []"));
        cauldron(helper, milk, new String[] {"content", "spoiled", "stage", "0"},
                java.util.List.of(content + " [" + content + ".spoiled]", "jade.simplebuilding.cauldron.spoiled []"));
    }

    private static void reinforcedCauldron(GameTestHelper helper) {
        String content = "jade.simplebuilding.cauldron.content";
        Block reinforced = lib("reinforced_cauldron");
        boolean hasLevel = reinforced.getStateDefinition().getProperty("level") != null;
        cauldron(helper, reinforced, new String[] {"content", "empty"}, java.util.List.of(content + " [" + content + ".empty]"));
        cauldron(helper, reinforced, new String[] {"content", "lava"}, java.util.List.of(content + " [" + content + ".lava]"));
        cauldron(helper, reinforced, new String[] {"content", "extreme"}, java.util.List.of(content + " [" + content + ".extreme]"));
        cauldron(helper, reinforced, new String[] {"content", "water"},
                java.util.List.of(content + " [" + content + ".water]", "jade.simplebuilding.cauldron.level [3, 3]"));
        cauldron(helper, reinforced, new String[] {"content", "powder_snow"},
                java.util.List.of(content + " [" + content + ".powder_snow]", "jade.simplebuilding.cauldron.level [3, 3]"));
        if (hasLevel) {
            cauldron(helper, reinforced, new String[] {"content", "water", "level", "2"},
                    java.util.List.of(content + " [" + content + ".water]", "jade.simplebuilding.cauldron.level [2, 3]"));
            cauldron(helper, reinforced, new String[] {"content", "lava", "level", "1"}, java.util.List.of(content + " [" + content + ".lava]"));
        }
        cauldron(helper, Blocks.CAULDRON, new String[0], java.util.List.of());
        cauldron(helper, Blocks.WATER_CAULDRON, new String[0], java.util.List.of());
        helper.assertTrue(com.simplebuilding.compat.BlockInfo.isModCauldron("simplesandwiches:milk_cauldron")
                && com.simplebuilding.compat.BlockInfo.isModCauldron("simplelib:reinforced_cauldron")
                && !com.simplebuilding.compat.BlockInfo.isModCauldron("minecraft:cauldron"), "Jade's fluid line is replaced for exactly the two mod cauldrons");
    }

    private static void cauldron(GameTestHelper helper, Block block, String[] properties, java.util.List<String> expected) {
        BlockState state = block.defaultBlockState();
        for (int i = 0; i < properties.length; i += 2) {
            state = withProperty(state, properties[i], properties[i + 1]);
        }
        java.util.List<String> actual = new java.util.ArrayList<>();
        for (var line : com.simplebuilding.compat.BlockInfo.stateLines(com.simplebuilding.compat.BlockInfo.Topic.CAULDRON, state)) {
            actual.add(line.key() + " " + line.argTexts());
        }
        helper.assertTrue(actual.equals(expected), java.util.Arrays.toString(properties) + " on " + BuiltInRegistries.BLOCK.getKey(block)
                + ": expected " + expected + ", was " + actual);
    }

    private static BlockState withProperty(BlockState state, String name, String value) {
        net.minecraft.world.level.block.state.properties.Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return parsed(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState parsed(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    /**
     * The JEI category "Cauldron and crucible": the crucible build, the barrel, the cauldron upgrade (which the machine
     * upgrade category does not list) and butter and cheese. Numbers are this test's own: 6 strikes, 4 of them walls,
     * 6 attach strikes, 9 barrel fields, 4 cracked diamonds.
     */
    public static void cauldronWorldCatalogMatchesTheRules(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        var entries = new java.util.LinkedHashMap<String, com.simplebuilding.compat.CauldronWorldCatalog.Entry>();
        com.simplebuilding.compat.CauldronWorldCatalog.entries().forEach(entry -> entries.put(entry.id(), entry));
        boolean sandwiches = item("simplesandwiches:butter_block") != Items.AIR && item("simplesandwiches:cheese_block") != Items.AIR;
        helper.assertTrue(entries.keySet().equals(sandwiches
                ? java.util.Set.of("crucible_build", "barrel_attach", "cauldron_reinforce", "milk_butter", "milk_cheese")
                : java.util.Set.of("crucible_build", "barrel_attach", "cauldron_reinforce")), "catalog entries " + entries.keySet());

        var build = entries.get("crucible_build");
        helper.assertTrue(build.inputs().get(0).items().equals(java.util.List.of(Items.IRON_BLOCK)) && build.inputs().get(0).count() == 1, "an iron block");
        helper.assertTrue(build.inputs().get(1).count() == 4 && build.inputs().get(1).items().contains(Items.HEAVY_WEIGHTED_PRESSURE_PLATE), "four walls");
        helper.assertTrue(build.inputs().get(2).count() == 2 && build.inputs().get(2).items().contains(Items.IRON_INGOT)
                && build.inputs().get(2).items().contains(ModItems.IRON_ROD), "two handles: iron ingot or iron rod");
        helper.assertTrue(build.output().items().equals(java.util.List.of(item("simplelib:iron_crucible"))), "the iron crucible");
        helper.assertTrue(build.tools().contains(ModItems.IRON_SLEDGEHAMMER) && build.tools().contains(ModItems.ENDERITE_SLEDGEHAMMER), "every sledgehammer");
        helper.assertTrue(noteArgs(build, 0).equals(java.util.List.of(6)) && noteArgs(build, 1).equals(java.util.List.of(4, 2)), "notes name 6 strikes, 4 walls and 2 handles");

        var barrel = entries.get("barrel_attach");
        helper.assertTrue(barrel.inputs().get(0).items().contains(item("simplelib:copper_barrel"))
                && barrel.inputs().get(0).items().contains(item("simplebuilding:enderite_barrel")), "barrels");
        helper.assertTrue(barrel.inputs().get(1).items().contains(item("simplelib:iron_crucible")), "next to a crucible");
        helper.assertTrue(noteArgs(barrel, 0).equals(java.util.List.of(6)) && noteArgs(barrel, 1).equals(java.util.List.of(9)), "notes name 6 strikes, 9 fields");

        var cauldron = entries.get("cauldron_reinforce");
        helper.assertTrue(cauldron.inputs().get(0).items().equals(java.util.List.of(Items.CAULDRON)), "a cauldron");
        helper.assertTrue(cauldron.inputs().get(1).items().equals(java.util.List.of(ModItems.CRACKED_DIAMOND)) && cauldron.inputs().get(1).count() == 4, "4 cracked diamonds");
        helper.assertTrue(cauldron.output().items().equals(java.util.List.of(item("simplelib:reinforced_cauldron"))), "the reinforced cauldron");
        helper.assertTrue(cauldron.tools().contains(ModItems.STONE_SLEDGEHAMMER), "every hammer works (rank 0)");
        helper.assertTrue(cauldron.durationTicks() == SledgehammerUpgrades.upgradeTicks(SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON)), "duration of the upgrade");
        helper.assertTrue(com.simplebuilding.compat.InWorldRecipeCatalog.build().entries().stream()
                .noneMatch(entry -> entry.id().equals("machine_upgrade/minecraft:cauldron")),
                "the cauldron upgrade is not in the machine upgrade category (else drop it here)");

        if (sandwiches) {
        var butter = entries.get("milk_butter");
        helper.assertTrue(butter.inputs().get(1).items().equals(java.util.List.of(Items.MILK_BUCKET)) && butter.tools().isEmpty()
                && butter.output().items().equals(java.util.List.of(item("simplesandwiches:butter_block"))), "milk bucket in a cauldron gives butter");
        var cheese = entries.get("milk_cheese");
        helper.assertTrue(cheese.inputs().get(2).items().equals(java.util.List.of(Items.FERMENTED_SPIDER_EYE))
                && cheese.output().items().equals(java.util.List.of(item("simplesandwiches:cheese_block"))), "fermented spider eye gives cheese");
        }

        var catalysts = com.simplebuilding.compat.CauldronWorldCatalog.catalysts();
        helper.assertTrue(catalysts.contains(Items.CAULDRON) && catalysts.contains(item("simplelib:reinforced_cauldron"))
                && catalysts.contains(ModItems.DIAMOND_SLEDGEHAMMER) && catalysts.contains(item("simplebuilding:enderite_crucible"))
                && catalysts.contains(item("simplelib:iron_crucible")), "catalysts: both cauldrons, the hammers, the crucibles");
        helper.succeed();
    }

    private static java.util.List<Object> noteArgs(com.simplebuilding.compat.CauldronWorldCatalog.Entry entry, int note) {
        return java.util.List.of(((net.minecraft.network.chat.contents.TranslatableContents) entry.notes().get(note).getContents()).getArgs());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Block lib(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplelib", path));
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack main, ItemStack off) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        return player;
    }

    private static UseOnContext click(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** Enderite crucible: 27 slots and double stacks; Enderite barrel double stacks too (owner F4/F10, 59). */
    public static void enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block crucible = CrucibleCompat.enderiteCrucible();
        helper.assertTrue(crucible != null && CrucibleCompat.crucibleSlots(crucible) == 27, "enderite crucible needs 27 slots");
        helper.assertTrue(CrucibleCompat.stackMultiplier(crucible) == 2, "enderite crucible holds double stacks");
        helper.assertTrue(CrucibleCompat.stackMultiplier(CrucibleCompat.enderiteBarrel()) == 2, "enderite barrel holds double stacks");
        helper.setBlock(new BlockPos(1, 1, 1), crucible);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1))) != null, "enderite crucible has a block entity");
        helper.assertTrue(!CrucibleCompat.axeWaysEnabled(), "SimpleBuilding switches the axe ways off (principle 5a)");
        helper.succeed();
    }

    /** Sledgehammer on an iron block: 4 weighted plates, then 2 iron rods; 2 durability per strike. */
    public static void sledgehammerBuildsTheIronCrucible(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.IRON_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 4));
        helper.assertTrue(player.getOffhandItem().is(TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("simplelib", "crucible_walls"))), "SimpleLib wall materials loaded");
        helper.assertTrue(new ItemStack(ModItems.IRON_ROD).is(TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("simplelib", "crucible_handles"))), "SimpleBuilding handle materials loaded");
        for (int strike = 0; strike < 6; strike++) {
            if (strike == 4) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.IRON_ROD, 2));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
            helper.assertTrue(hammer.getItem().useOn(click(helper, player, pos)).consumesAction(), "strike " + (strike + 1) + " not taken");
            helper.assertTrue(hammer.getDamageValue() == (strike + 1) * CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE,
                    "strike " + (strike + 1) + " must build and damage the hammer");
        }
        helper.assertBlockPresent(lib("iron_crucible"), pos);
        helper.assertTrue(hammer.getDamageValue() == 6 * CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE, "hammer damage " + hammer.getDamageValue());
        helper.assertTrue(player.getOffhandItem().isEmpty(), "both iron rods used");
        helper.succeed();
    }

    /** Crucible and barrel upgrades cost twice a furnace upgrade; the chain ends at Enderite (owner F9-F11, 56, 59). */
    public static void sledgehammerUpgradesCostDouble(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        var iron = SledgehammerUpgrades.upgradeOf(lib("iron_crucible"));
        var netherite = SledgehammerUpgrades.upgradeOf(lib("netherite_crucible"));
        var barrel = SledgehammerUpgrades.upgradeOf(lib("reinforced_barrel"));
        var netheriteBarrel = SledgehammerUpgrades.upgradeOf(lib("netherite_barrel"));
        var cauldron = SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON);
        helper.assertTrue(iron != null && iron.to() == lib("reinforced_crucible") && iron.nugget() == ModItems.CRACKED_DIAMOND
                && iron.materialCost() == 2 && iron.durationFactor() == 2, "iron -> reinforced: 2 cracked diamonds, double strikes");
        helper.assertTrue(netherite != null && netherite.to() == CrucibleCompat.enderiteCrucible() && netherite.nugget() == ModItems.ENDERITE_NUGGET
                && netherite.minHammerRank() == SledgehammerUpgrades.RANK_NETHERITE, "netherite -> enderite with enderite nuggets, netherite hammer");
        helper.assertTrue(barrel != null && barrel.to() == lib("netherite_barrel") && barrel.nugget() == ModItems.NETHERITE_NUGGET
                && barrel.materialCost() == 2, "reinforced -> netherite barrel with netherite nuggets");
        helper.assertTrue(netheriteBarrel != null && netheriteBarrel.to() == CrucibleCompat.enderiteBarrel() && netheriteBarrel.materialCost() == 2,
                "netherite -> enderite barrel");
        helper.assertTrue(cauldron != null && cauldron.to() == CrucibleCompat.reinforcedCauldron() && cauldron.materialCost() == 4,
                "cauldron -> reinforced cauldron with 4 cracked diamonds");
        // The upgrade keeps the contents (owner 11).
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, lib("netherite_crucible"));
        BlockPos abs = helper.absolutePos(pos);
        var be = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        be.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        CrucibleCompat.upgradeInPlace(helper.getLevel(), abs, CrucibleCompat.enderiteCrucible());
        helper.assertBlockPresent(CrucibleCompat.enderiteCrucible(), pos);
        var fresh = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        helper.assertTrue(fresh.getContainerSize() == 27 && fresh.getItem(0).is(Items.RAW_IRON) && fresh.getItem(0).getCount() == 5,
                "upgrade kept the contents");
        helper.succeed();
    }

    /** Overworld reach 2: the third block stays dry, the second gets soul lava (owner 25). */
    public static void soulLavaFlowsTwoBlocksInTheOverworld(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        floor(helper);
        helper.setBlock(new BlockPos(1, 1, 4), ModFluids.SOUL_LAVA_BLOCK);
        helper.runAfterDelay(250, () -> {
            var level = helper.getLevel();
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 4))).getType().isSame(ModFluids.SOUL_LAVA), "first block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(3, 1, 4))).getType().isSame(ModFluids.SOUL_LAVA), "second block wet");
            helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(4, 1, 4))).isEmpty(), "third block dry");
            helper.succeed();
        });
    }

    /** Not replaceable by other fluids or survival placement; only scooping the source removes it (owner 26). */
    public static void soulLavaIsNotReplaceable(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockState state = ModFluids.SOUL_LAVA_BLOCK.defaultBlockState();
        helper.assertTrue(!state.canBeReplaced(), "soul lava must not be replaceable");
        helper.assertTrue(!state.canBeReplaced(Fluids.WATER) && !state.canBeReplaced(Fluids.LAVA), "fluids must not replace soul lava");
        helper.assertTrue(state.getFluidState().isSource() && state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA), "soul lava swims like lava");
        helper.succeed();
    }

    /** Water touching soul lava turns: source -> quartz block, flowing -> blackstone; the soul lava stays (owner 26/54). */
    public static void waterTouchingSoulLavaTurnsToQuartzOrBlackstone(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        floor(helper);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) for (int y = 1; y < 3; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
        BlockPos lava = new BlockPos(3, 1, 3);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(lava, ModFluids.SOUL_LAVA_BLOCK);
        helper.assertBlockPresent(Blocks.QUARTZ_BLOCK, new BlockPos(4, 1, 3));
        helper.assertBlockPresent(Blocks.BLACKSTONE, new BlockPos(2, 1, 3));
        helper.assertBlockPresent(ModFluids.SOUL_LAVA_BLOCK, lava);
        helper.succeed();
    }

    /** Touch: burns twice as long as lava and gives Seelenbrand for a minute (owner 25/28). */
    public static void touchingSoulLavaBurnsLongerAndGivesSoulBurn(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        SoulLava.touch(helper.getLevel(), pig);
        helper.assertTrue(pig.getRemainingFireTicks() >= 20 * 20, "burns longer than lava: " + pig.getRemainingFireTicks());
        MobEffectInstance burn = pig.getEffect(ModEffects.SOUL_BURN);
        helper.assertTrue(burn != null && burn.getDuration() >= SoulLava.soulBurnTicks() - 1, "seelenbrand for a minute");
        helper.succeed();
    }

    /** Fire resistance blocks the Seelenbrand damage, not the effect; without it the damage comes (owner 55). */
    public static void fireResistanceOnlyBlocksSoulBurnDamage(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        Pig safe = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 2));
        Pig hurt = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(5, 1, 5));
        safe.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400));
        for (Pig pig : new Pig[] {safe, hurt}) pig.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, SoulLava.soulBurnTicks()));
        float before = safe.getHealth();
        for (int i = 0; i < 40; i++) {
            McVersion.resetInvulnerableTime(safe);
            McVersion.resetInvulnerableTime(hurt);
            ModEffects.SOUL_BURN.value().applyEffectTick(level, safe, 0);
            ModEffects.SOUL_BURN.value().applyEffectTick(level, hurt, 0);
        }
        helper.assertTrue(safe.getHealth() == before && safe.hasEffect(ModEffects.SOUL_BURN), "fire resistance: no damage, effect stays");
        helper.assertTrue(hurt.getHealth() < hurt.getMaxHealth(), "without fire resistance the soul burn hurts");
        helper.succeed();
    }

    /** Heat: soul lava source extreme, flowing high; the reinforced cauldron with soul lava extreme (owner F30). */
    public static void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), ModFluids.SOUL_LAVA_BLOCK);
        helper.assertTrue("extreme".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(1, 2, 1)))), "source: extreme");
        helper.setBlock(new BlockPos(4, 1, 1), ModFluids.FLOWING_SOUL_LAVA.getFlowing(6, false).createLegacyBlock());
        helper.assertTrue("high".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(4, 2, 1)))), "flowing: high");
        helper.setBlock(new BlockPos(6, 1, 1), CrucibleCompat.reinforcedCauldron("extreme"));
        helper.assertTrue("extreme".equals(CrucibleCompat.heatAt(level, helper.absolutePos(new BlockPos(6, 2, 1)))), "cauldron: extreme");
        TagKey<Item> extreme = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "needs_extreme_heat"));
        helper.assertTrue(new ItemStack(ModItems.LAYERED_RAW_ENDERITE).is(extreme) && new ItemStack(ModItems.CRACKED_DIAMOND).is(extreme),
                "layered raw enderite and cracked diamond need extreme heat");
        helper.succeed();
    }

    /** Copper bucket: no soul lava, oxidizes on pouring, no water source, breaks on lava; axe/honeycomb (owner 32/33). */
    public static void copperBucketRules(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        floor(helper);
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.COPPER, ModFluids.SOUL_LAVA) == null, "copper bucket refuses soul lava");
        ModBucketItem water = (ModBucketItem) ModFluids.COPPER_WATER_BUCKET;
        ItemStack after = water.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(water));
        helper.assertTrue(after.is(ModFluids.COPPER_BUCKET) && ModBucketItem.oxidation(after) == 1, "pouring oxidizes one stage");
        helper.assertTrue(!level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).isSource(), "copper water is never a source");
        ModBucketItem lava = (ModBucketItem) ModFluids.COPPER_LAVA_BUCKET;
        helper.assertTrue(lava.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(lava)).isEmpty(), "copper bucket breaks on lava");
        helper.assertTrue(ModBucketItem.scrape(after) && ModBucketItem.oxidation(after) == 0, "axe scrapes one stage");
        after.set(com.simplebuilding.component.ModDataComponentTypes.WAXED, true);
        ModBucketItem.oxidize(after);
        helper.assertTrue(ModBucketItem.oxidation(after) == 0, "waxed copper does not oxidize");
        helper.succeed();
    }

    /** Iron bucket breaks when pouring soul lava; the Enderite bucket never breaks (owner 34/36). */
    public static void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        floor(helper);
        ModBucketItem iron = (ModBucketItem) ModFluids.SOUL_LAVA_BUCKET;
        helper.assertTrue(iron.pourAt(level, helper.absolutePos(new BlockPos(2, 1, 2)), new ItemStack(iron)).isEmpty(), "iron bucket breaks");
        helper.assertTrue(level.getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).getType() == ModFluids.SOUL_LAVA, "soul lava placed");
        ModBucketItem enderite = (ModBucketItem) ModFluids.ENDERITE_SOUL_LAVA_BUCKET;
        helper.assertTrue(enderite.pourAt(level, helper.absolutePos(new BlockPos(5, 1, 5)), new ItemStack(enderite)).is(ModFluids.ENDERITE_BUCKET),
                "enderite bucket comes back");
        helper.assertTrue(ModBucketItem.filled(ModBucketItem.Kind.ENDERITE, ModFluids.SOUL_LAVA) == ModFluids.ENDERITE_SOUL_LAVA_BUCKET,
                "enderite bucket takes soul lava");
        helper.assertTrue(Items.BUCKET.getDefaultInstance().is(Items.BUCKET)
                && ((LiquidBlock) ModFluids.SOUL_LAVA_BLOCK).pickupBlock(null, level, helper.absolutePos(new BlockPos(2, 1, 2)),
                        level.getBlockState(helper.absolutePos(new BlockPos(2, 1, 2)))).is(ModFluids.SOUL_LAVA_BUCKET),
                "the vanilla bucket scoops soul lava into the soul lava bucket");
        helper.succeed();
    }

    /** The reinforced cauldron takes soul lava from the Enderite bucket and gives it back (owner 30/56). */
    public static void reinforcedCauldronHoldsSoulLava(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, CrucibleCompat.reinforcedCauldron("empty"));
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(ModFluids.ENDERITE_SOUL_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue("extreme".equals(CrucibleCompat.cauldronContent(level.getBlockState(abs))), "cauldron holds soul lava");
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_BUCKET), "enderite bucket comes back empty");
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_SOUL_LAVA_BUCKET), "scooped back");
        helper.assertTrue("empty".equals(CrucibleCompat.cauldronContent(level.getBlockState(abs))), "cauldron empty again");
        helper.succeed();
    }

    /** Sledgehammer on a quartz block: 4 quartz (owner 54). */
    public static void sledgehammerCrushesQuartzBlockIntoFourQuartz(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.QUARTZ_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, ItemStack.EMPTY);
        SledgehammerItem.crushQuartzBlock(helper.getLevel(), helper.absolutePos(pos), player, hammer);
        helper.assertBlockPresent(Blocks.AIR, pos);
        int quartz = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2))
                .stream().filter(e -> e.getItem().is(Items.QUARTZ)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(quartz == SledgehammerItem.QUARTZ_BLOCK_QUARTZ, "quartz dropped: " + quartz);
        helper.succeed();
    }

    /** Vanilla cauldron: the copper bucket pours lava and breaks, the Enderite bucket takes it back (owner 33/36). */
    public static void vanillaCauldronTakesCopperAndEnderiteBuckets(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.CAULDRON);
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer player = player(helper, new ItemStack(ModFluids.COPPER_LAVA_BUCKET), ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.LAVA_CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "copper bucket broke on lava");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModFluids.ENDERITE_BUCKET));
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockPresent(Blocks.CAULDRON, pos);
        helper.assertTrue(player.getMainHandItem().is(ModFluids.ENDERITE_LAVA_BUCKET), "enderite bucket took the lava");
        helper.succeed();
    }
}
