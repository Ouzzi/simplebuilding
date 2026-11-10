package com.simplemachines.test;

import com.simplemachines.SimpleMachines;
import com.simplemachines.config.MachinesConfig;
import com.simplemachines.smither.AutoSmitherBlock;
import com.simplemachines.smither.AutoSmitherBlockEntity;
import com.simplemachines.smither.AutoSmitherMenu;
import com.simplemachines.smither.Smithers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * MACH32-MOD stage 4 (docs/ai/PLAN-B-MACH32S4-2026-11-10.md): the Auto Smither in Simple Machines. SimpleBuilding's
 * {@code WorkstationTests} rows (once per pulse, eject, hopper sorting, output rejects insertion, capacity and recipe error) plus
 * what the move and the owner's brief add: registration, the old ids and saved data of a world from before 10.11., the delay taken
 * over from SimpleBuilding's config, the automatic netherite upgrade, an armour trim (smithing_trim) and no smithing without a
 * redstone edge. Runs without SimpleBuilding.
 */
public final class AutoSmitherTests {
    private AutoSmitherTests() {}

    /** Block, item, block entity type and menu under simplemachines:auto_smither; the block entity accepts the block. */
    public static void autoSmitherRegistered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Identifier id = SimpleMachines.id(Smithers.ID);
        if (BuiltInRegistries.BLOCK.getValue(id) != Smithers.BLOCK) problems.add("block");
        if (BuiltInRegistries.ITEM.getValue(id) != Smithers.ITEM) problems.add("item");
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id) != Smithers.BLOCK_ENTITY) problems.add("block entity type");
        if (BuiltInRegistries.MENU.getValue(id) != Smithers.MENU) problems.add("menu");
        if (Smithers.BLOCK_ENTITY == null || !Smithers.BLOCK_ENTITY.isValid(Smithers.BLOCK.defaultBlockState())) problems.add("block entity refuses the block");
        if (helper.getLevel().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, id)).isEmpty()) problems.add("recipe");
        helper.assertTrue(problems.isEmpty(), "auto smither registration: " + problems);
        helper.succeed();
    }

    /** simplebuilding:auto_smither (item, block, block entity, recipe) resolves here; a saved Smither loads with its slots. */
    public static void oldAutoSmitherIdsMove(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Identifier old = Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, Smithers.ID);
        if (BuiltInRegistries.BLOCK.getValue(old) != Smithers.BLOCK) problems.add("block " + old);
        if (BuiltInRegistries.ITEM.getValue(old) != Smithers.ITEM) problems.add("item " + old);
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(old) != Smithers.BLOCK_ENTITY) problems.add("block entity type " + old);
        Identifier recipe = com.simplelib.api.IdMoves.target(Registries.RECIPE, old);
        if (recipe == null || !recipe.equals(SimpleMachines.id(Smithers.ID))) problems.add("recipe -> " + recipe);
        CompoundTag saved = new CompoundTag();
        saved.putString("id", old.toString());
        saved.putInt("x", 0);
        saved.putInt("y", 0);
        saved.putInt("z", 0);
        ListTag items = new ListTag();
        CompoundTag sword = new CompoundTag();
        sword.putString("id", "minecraft:diamond_sword");
        sword.putInt("count", 1);
        sword.putByte("Slot", (byte) AutoSmitherBlockEntity.BASE_SLOT);
        items.add(sword);
        saved.put("Items", items);
        saved.putInt("crafting_ticks_remaining", 3);
        BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(new BlockPos(1, 1, 1)), Smithers.BLOCK.defaultBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AutoSmitherBlockEntity smither)) {
            problems.add("the old block entity loaded as " + loaded);
        } else {
            if (!smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.DIAMOND_SWORD)) problems.add("contents " + smither.getItem(AutoSmitherBlockEntity.BASE_SLOT));
            if (smither.saveCustomOnly(helper.getLevel().registryAccess()).getIntOr("crafting_ticks_remaining", -1) != 3) problems.add("crafting ticks");
        }
        helper.assertTrue(problems.isEmpty(), "old auto smither ids and data: " + problems);
        helper.succeed();
    }

    /** SimpleBuilding's server.machines.autoSmitherDelayTicks comes over into autoSmither.delayTicks (read only, the old file stays). */
    public static void smitherDelayComesOverFromSimpleBuilding(GameTestHelper helper) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplemachines-config");
            java.nio.file.Path legacy = dir.resolve(MachinesConfig.LEGACY_FILE);
            helper.assertTrue(MachinesConfig.smitherFromSimpleBuilding(legacy) == null, "a missing file gave a value");
            java.nio.file.Files.writeString(legacy, "{\"server\": {\"machines\": {\"autoSmitherDelayTicks\": 40, \"reinforcedHopperSpeed\": 3}}}");
            com.google.gson.JsonObject taken = MachinesConfig.smitherFromSimpleBuilding(legacy);
            helper.assertTrue(taken != null && taken.size() == 1 && taken.get("delayTicks").getAsInt() == 40, "taken over: " + taken);
            java.nio.file.Files.writeString(legacy, "{\"server\": {\"machines\": {\"reinforcedHopperSpeed\": 3}}}");
            helper.assertTrue(MachinesConfig.smitherFromSimpleBuilding(legacy) == null, "a file without the key gave a value");
            helper.assertTrue(MachinesConfig.KEYS.contains("autoSmither.delayTicks") && MachinesConfig.toJson().has("autoSmither"),
                    "the autoSmither section is not part of the file");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        helper.assertValueEqual(MachinesConfig.autoSmitherDelayTicks, MachinesConfig.MIN_SMITHER_DELAY, "default delay (the Crafter's 4 ticks)");
        helper.succeed();
    }

    /** Owner brief: a diamond chestplate becomes netherite on one pulse, from inputs a hopper put in (template, base, ingot). */
    public static void autoSmitherUpgradesDiamondToNetherite(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = smitherPos.east();
        helper.setBlock(smitherPos, Smithers.BLOCK.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
        helper.setBlock(chestPos, Blocks.CHEST);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        for (ItemStack in : List.of(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.NETHERITE_INGOT))) {
            helper.assertTrue(HopperBlockEntity.addItem(null, smither, in, Direction.UP).isEmpty(), "hopper input " + in);
        }
        helper.setBlock(smitherPos.west(), Blocks.REDSTONE_BLOCK);
        helper.startSequence().thenExecuteAfter(10, () -> {
            helper.assertValueEqual(helper.getBlockEntity(chestPos, ChestBlockEntity.class).countItem(Items.NETHERITE_CHESTPLATE), 1,
                    "netherite chestplate in the chest");
            helper.assertValueEqual(smither.redstoneSignal(), 0, "all inputs used");
        }).thenSucceed();
    }

    /** Owner brief: an armour trim (Vanilla smithing_trim) works the same way; the template is used up like at the table. */
    public static void autoSmitherAppliesArmourTrim(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = smitherPos.east();
        helper.setBlock(smitherPos, Smithers.BLOCK.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
        helper.setBlock(chestPos, Blocks.CHEST);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        smither.setItem(AutoSmitherBlockEntity.TEMPLATE_SLOT, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
        smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.IRON_CHESTPLATE));
        smither.setItem(AutoSmitherBlockEntity.ADDITION_SLOT, new ItemStack(Items.REDSTONE));
        helper.setBlock(smitherPos.west(), Blocks.REDSTONE_BLOCK);
        helper.startSequence().thenExecuteAfter(10, () -> {
            ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
            ItemStack out = ItemStack.EMPTY;
            for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.IRON_CHESTPLATE)) out = chest.getItem(i);
            helper.assertTrue(out.get(DataComponents.TRIM) != null, "trimmed chestplate in the chest: " + out);
            helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.TEMPLATE_SLOT).isEmpty(), "trim template used");
            helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.ADDITION_SLOT).isEmpty(), "redstone used");
        }).thenSucceed();
    }

    /** Owner brief: without a redstone edge (signal off) nothing is smithed; the configured delay holds the pulse back. */
    public static void autoSmitherStopsWithoutRedstone(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        helper.setBlock(smitherPos, Smithers.BLOCK.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        smither.setItem(AutoSmitherBlockEntity.TEMPLATE_SLOT, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.DIAMOND_SWORD));
        smither.setItem(AutoSmitherBlockEntity.ADDITION_SLOT, new ItemStack(Items.NETHERITE_INGOT));
        int before = MachinesConfig.autoSmitherDelayTicks;
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.DIAMOND_SWORD), "smithed without redstone");
                    helper.assertTrue(ejected(helper, smitherPos).isEmpty(), "a result without redstone");
                    MachinesConfig.autoSmitherDelayTicks = 30;
                    try {
                        helper.setBlock(smitherPos.west(), Blocks.REDSTONE_BLOCK);
                    } finally {
                        MachinesConfig.autoSmitherDelayTicks = before;
                    }
                })
                .thenExecuteAfter(10, () -> helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.DIAMOND_SWORD),
                        "smithed before the configured delay"))
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).isEmpty(), "the delayed pulse did not smith");
                    helper.assertTrue(ejected(helper, smitherPos).stream().anyMatch(e -> e.getItem().is(Items.NETHERITE_SWORD)), "no netherite sword");
                })
                .thenSucceed();
    }

    private static List<ItemEntity> ejected(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3));
    }

    /**
     * Wie der Crafter: eine steigende Flanke schmiedet genau einmal, das Ergebnis landet in der Truhe vor der Front,
     * Dauersignal loest nicht erneut aus, eine neue Flanke schon. Ohne passendes Rezept bleibt alles liegen.
     */
    public static void autoSmitherSmithsOncePerPulse(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = smitherPos.east();
        BlockPos powerPos = smitherPos.west();
        helper.setBlock(smitherPos, Smithers.BLOCK.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
        helper.setBlock(chestPos, Blocks.CHEST);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        smither.setItem(AutoSmitherBlockEntity.TEMPLATE_SLOT, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.DIAMOND_SWORD));
        smither.setItem(AutoSmitherBlockEntity.ADDITION_SLOT, new ItemStack(Items.NETHERITE_INGOT, 2));
        helper.assertValueEqual(smither.redstoneSignal(), 15, "comparator with three filled inputs");
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
                    helper.assertValueEqual(chest.countItem(Items.NETHERITE_SWORD), 1, "netherite swords in the chest after one pulse");
                    helper.assertValueEqual(smither.getItem(AutoSmitherBlockEntity.TEMPLATE_SLOT).getCount(), 1, "one template used");
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).isEmpty(), "the sword was used");
                    helper.assertValueEqual(smither.getItem(AutoSmitherBlockEntity.ADDITION_SLOT).getCount(), 1, "one ingot used");
                    helper.assertTrue(helper.getBlockState(smitherPos).getValue(AutoSmitherBlock.TRIGGERED), "the smither is triggered");
                    smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.DIAMOND_SWORD));
                })
                .thenExecuteAfter(10, () -> {
                    helper.assertValueEqual(helper.getBlockEntity(chestPos, ChestBlockEntity.class).countItem(Items.NETHERITE_SWORD), 1,
                            "a steady signal does not smith again");
                    helper.setBlock(powerPos, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(10, () -> {
                    helper.assertValueEqual(helper.getBlockEntity(chestPos, ChestBlockEntity.class).countItem(Items.NETHERITE_SWORD), 2,
                            "a new rising edge smiths again");
                    helper.assertValueEqual(smither.redstoneSignal(), 0, "all inputs used up");
                    smither.setItem(AutoSmitherBlockEntity.TEMPLATE_SLOT, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                    smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.STICK));
                    helper.setBlock(powerPos, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenExecuteAfter(10, () -> {
                    helper.assertValueEqual(helper.getBlockEntity(chestPos, ChestBlockEntity.class).countItem(Items.NETHERITE_SWORD), 2,
                            "without a matching recipe nothing is smithed");
                    helper.assertValueEqual(smither.getItem(AutoSmitherBlockEntity.TEMPLATE_SLOT).getCount(), 1, "the template stays");
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.STICK), "the stick stays");
                })
                .thenSucceed();
    }

    /** Wie der Crafter: ohne Container vor der Front wird das Ergebnis in Blickrichtung ausgeworfen, nichts bleibt im Ausgabeslot. */
    public static void autoSmitherEjectsWithoutContainer(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        BlockPos powerPos = smitherPos.west();
        helper.setBlock(smitherPos, Smithers.BLOCK.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        smither.setItem(AutoSmitherBlockEntity.TEMPLATE_SLOT, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.DIAMOND_SWORD));
        smither.setItem(AutoSmitherBlockEntity.ADDITION_SLOT, new ItemStack(Items.NETHERITE_INGOT));
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).isEmpty(), "the sword was used");
                    helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.RESULT_SLOT).isEmpty(), "nothing stays in the output slot");
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(helper.absolutePos(smitherPos)).inflate(3))
                            .stream().anyMatch(e -> e.getItem().is(Items.NETHERITE_SWORD)), "the netherite sword was ejected as an item");
                })
                .thenSucceed();
    }

    /**
     * Trichter legen nach Vanillas Schmiede-Mengen ein (Vorlage, Basis, Material), Fremdes bleibt draussen, herausziehen
     * geht nur aus dem gespeicherten Ergebnisslot.
     */
    public static void autoSmitherSortsHopperInput(GameTestHelper helper) {
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        helper.setBlock(smitherPos, Smithers.BLOCK);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(smitherPos, AutoSmitherBlockEntity.class);
        helper.assertTrue(HopperBlockEntity.addItem(null, smither, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 3), Direction.UP).isEmpty(),
                "templates go in");
        helper.assertTrue(HopperBlockEntity.addItem(null, smither, new ItemStack(Items.DIAMOND_PICKAXE), Direction.NORTH).isEmpty(), "the pickaxe goes in");
        helper.assertTrue(HopperBlockEntity.addItem(null, smither, new ItemStack(Items.NETHERITE_INGOT, 4), Direction.UP).isEmpty(), "ingots go in");
        helper.assertValueEqual(HopperBlockEntity.addItem(null, smither, new ItemStack(Items.DIRT), Direction.UP).getCount(), 1, "dirt stays out");
        helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.TEMPLATE_SLOT).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                && smither.getItem(AutoSmitherBlockEntity.TEMPLATE_SLOT).getCount() == 3, "templates in the template slot");
        helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.DIAMOND_PICKAXE), "pickaxe in the base slot");
        helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.ADDITION_SLOT).is(Items.NETHERITE_INGOT), "ingots in the addition slot");
        for (Direction side : Direction.values()) {
            for (int slot : smither.getSlotsForFace(side)) {
                helper.assertTrue(smither.canTakeItemThroughFace(slot, smither.getItem(slot), side)
                        == (slot == AutoSmitherBlockEntity.RESULT_SLOT), "only output can be extracted from " + side);
            }
            helper.assertFalse(smither.canPlaceItemThroughFace(AutoSmitherBlockEntity.RESULT_SLOT,
                    new ItemStack(Items.NETHERITE_INGOT), side), "no insertion into output from " + side);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AutoSmitherMenu menu = new AutoSmitherMenu(3, player.getInventory(), smither, new SimpleContainerData(1));
        helper.assertFalse(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).hasItem(), "no free preview result");
        helper.assertTrue(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).mayPickup(player), "finished output can be taken");
        helper.assertFalse(menu.hasRecipeError(), "valid recipe has no error");
        helper.assertTrue(AutoSmitherBlock.smith(helper.getBlockState(smitherPos), helper.getLevel(), helper.absolutePos(smitherPos)),
                "smithing succeeds");
        // Without a container in front the result is ejected (N35); a stored result (older worlds) is what hoppers/players take.
        helper.assertTrue(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).getItem().isEmpty(), "no container: the result is ejected, not stored");
        smither.storeResult(new ItemStack(Items.NETHERITE_PICKAXE));
        helper.assertTrue(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).getItem().is(Items.NETHERITE_PICKAXE), "finished output is stored");
        // Keep every input occupied while the real hopper extracts the finished result.
        smither.setItem(AutoSmitherBlockEntity.BASE_SLOT, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.setBlock(smitherPos.below(), Blocks.HOPPER);
        helper.startSequence().thenExecuteAfter(20, () -> {
            helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.RESULT_SLOT).isEmpty(), "hopper removed output");
            helper.assertValueEqual(helper.getBlockEntity(smitherPos.below(), HopperBlockEntity.class).countItem(Items.NETHERITE_PICKAXE),
                    1, "hopper contains the finished result");
            helper.assertValueEqual(smither.getItem(0).getCount(), 2, "hopper did not take templates");
            helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.BASE_SLOT).is(Items.DIAMOND_PICKAXE), "hopper did not take the base");
            helper.assertValueEqual(smither.getItem(2).getCount(), 3, "hopper did not take additions");
        }).thenSucceed();
    }

    /** Exercise the server click dispatcher, not just the slot predicate. */
    public static void autoSmitherOutputRejectsInsertion(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Smithers.BLOCK);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(pos, AutoSmitherBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AutoSmitherMenu menu = new AutoSmitherMenu(3, player.getInventory(), smither, new SimpleContainerData(1));
        int output = AutoSmitherMenu.RESULT_SLOT;
        helper.assertFalse(menu.getSlot(output).mayPlace(new ItemStack(Items.NETHERITE_INGOT)), "output rejects placement");
        for (int button : new int[]{0, 1}) {
            menu.setCarried(new ItemStack(Items.NETHERITE_INGOT, 8));
            menu.clicked(output, button, ContainerInput.PICKUP, player);
            helper.assertFalse(menu.getSlot(output).hasItem(), "normal click cannot insert");
            helper.assertValueEqual(menu.getCarried().getCount(), 8, "cursor is unchanged");
        }
        menu.setCarried(ItemStack.EMPTY);
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            player.getInventory().setItem(hotbar, new ItemStack(Items.NETHERITE_INGOT));
            menu.clicked(output, hotbar, ContainerInput.SWAP, player);
            helper.assertFalse(menu.getSlot(output).hasItem(), "number key cannot insert");
        }
        player.getInventory().setItem(40, new ItemStack(Items.NETHERITE_INGOT));
        menu.clicked(output, 40, ContainerInput.SWAP, player);
        helper.assertFalse(menu.getSlot(output).hasItem(), "offhand swap cannot insert");
        smither.setItem(2, new ItemStack(Items.NETHERITE_INGOT, 64));
        menu.clicked(31, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertFalse(menu.getSlot(output).hasItem(), "shift click cannot insert when input is full");
        player.setGameMode(GameType.CREATIVE);
        for (int mode : new int[]{0, 1, 2}) {
            menu.setCarried(new ItemStack(Items.NETHERITE_INGOT, 8));
            menu.clicked(-999, mode * 4, ContainerInput.QUICK_CRAFT, player);
            menu.clicked(output, mode * 4 + 1, ContainerInput.QUICK_CRAFT, player);
            menu.clicked(-999, mode * 4 + 2, ContainerInput.QUICK_CRAFT, player);
            helper.assertFalse(menu.getSlot(output).hasItem(), "drag cannot insert");
        }
        player.setGameMode(GameType.SURVIVAL);
        menu.setCarried(new ItemStack(Items.NETHERITE_INGOT));
        menu.clicked(output, 0, ContainerInput.PICKUP_ALL, player);
        helper.assertFalse(menu.getSlot(output).hasItem(), "double click cannot insert");

        player.getInventory().clearContent();
        menu.setCarried(ItemStack.EMPTY);
        smither.setItem(output, new ItemStack(Items.NETHERITE_SWORD));
        menu.clicked(output, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.NETHERITE_SWORD), "normal click takes output");
        menu.setCarried(ItemStack.EMPTY);
        smither.setItem(output, new ItemStack(Items.NETHERITE_SWORD));
        menu.clicked(output, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertFalse(menu.getSlot(output).hasItem(), "shift click takes output");
        helper.assertValueEqual(player.getInventory().countItem(Items.NETHERITE_SWORD), 1, "shift click preserves result");
        smither.setItem(output, new ItemStack(Items.NETHERITE_SWORD));
        player.getInventory().setItem(0, ItemStack.EMPTY);
        menu.clicked(output, 0, ContainerInput.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(0).is(Items.NETHERITE_SWORD), "empty hotbar key takes output");
        smither.setItem(output, new ItemStack(Items.NETHERITE_SWORD));
        player.getInventory().setItem(0, new ItemStack(Items.NETHERITE_INGOT));
        menu.clicked(output, 0, ContainerInput.SWAP, player);
        helper.assertTrue(smither.getItem(output).is(Items.NETHERITE_SWORD), "occupied hotbar cannot replace output");
        helper.assertTrue(player.getInventory().getItem(0).is(Items.NETHERITE_INGOT), "hotbar item stays");
        helper.succeed();
    }

    public static void autoSmitherOutputCapacityAndRecipeError(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Smithers.BLOCK);
        AutoSmitherBlockEntity smither = helper.getBlockEntity(pos, AutoSmitherBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AutoSmitherMenu menu = new AutoSmitherMenu(3, player.getInventory(), smither, new SimpleContainerData(1));
        helper.assertFalse(menu.hasRecipeError(), "empty inputs have no error");
        smither.setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        smither.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        smither.setItem(2, new ItemStack(Items.NETHERITE_INGOT, 2));
        smither.setItem(3, new ItemStack(Items.NETHERITE_SWORD));
        helper.assertFalse(AutoSmitherBlock.smith(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos)), "full output stops crafting");
        helper.assertValueEqual(smither.getItem(0).getCount(), 2, "full output consumes no template");
        helper.assertTrue(smither.getItem(1).is(Items.DIAMOND_SWORD), "full output consumes no base");
        helper.assertValueEqual(smither.getItem(2).getCount(), 2, "full output consumes no addition");
        helper.assertValueEqual(smither.redstoneSignal(), 15, "output does not increase comparator beyond 15");
        menu.broadcastChanges();
        helper.assertFalse(menu.hasRecipeError(), "full output is not an invalid recipe");
        smither.setItem(1, new ItemStack(Items.STICK));
        menu.broadcastChanges();
        helper.assertTrue(menu.hasRecipeError(), "invalid complete inputs show error even with old output");
        smither.setItem(2, ItemStack.EMPTY);
        menu.broadcastChanges();
        helper.assertFalse(menu.hasRecipeError(), "incomplete inputs hide error");
        int[][] positions = {{26, 35}, {44, 35}, {62, 35}, {134, 35}, {8, 84}, {8, 142}};
        int[] indices = {0, 1, 2, 3, 4, 31};
        for (int i = 0; i < indices.length; i++) {
            helper.assertValueEqual(menu.getSlot(indices[i]).x, positions[i][0], "vanilla slot x");
            helper.assertValueEqual(menu.getSlot(indices[i]).y, positions[i][1], "vanilla slot y");
        }
        helper.succeed();
    }

}
