package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.AutoSmitherBlock;
import com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity;
import com.simplebuilding.recipe.SmithingPlacement;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Rezeptbuch am Schmiedetisch und Auto-Schmied (docs/ai/PLAN-WORKSTATIONS-2026-10-02.md). Loader-neutral; auf Linien
 * ohne die Flags gelingen die Tests sofort.
 */
public final class WorkstationTests {
    private WorkstationTests() {
    }

    private static RecipeHolder<?> vanillaRecipe(GameTestHelper helper, String path) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(path));
        return helper.getLevel().getServer().getRecipeManager().byKey(key)
                .orElseThrow(() -> helper.assertionException("missing vanilla recipe " + path));
    }

    /**
     * Klick im Rezeptbuch des Schmiedetischs (echtes Platzier-Paket, also auch der Server-Mixin): Vorlage, die
     * beschaedigte Diamantruestung und der Barren wandern in die Felder, das Ergebnis steht bereit. Fehlt die Basis,
     * bleiben die Felder leer, alles liegt wieder im Inventar.
     */
    public static void smithingRecipeBookPlacesDamagedGear(GameTestHelper helper) {
        if (!McVersion.SMITHING_RECIPE_BOOK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.setDamageValue(10);
        inventory.setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
        inventory.setItem(1, chestplate);
        inventory.setItem(2, new ItemStack(Items.NETHERITE_INGOT, 1));
        SmithingMenu menu = new SmithingMenu(7, inventory, ContainerLevelAccess.NULL);
        player.containerMenu = menu;

        RecipeHolder<?> upgrade = vanillaRecipe(helper, "netherite_chestplate_smithing");
        player.awardRecipes(List.of(upgrade));
        List<RecipeDisplayEntry> displays = new ArrayList<>();
        helper.getLevel().getServer().getRecipeManager().listDisplaysForRecipe(upgrade.id(), displays::add);
        helper.assertFalse(displays.isEmpty(), "the netherite chestplate upgrade has a recipe book entry");
        player.connection.handlePlaceRecipe(new ServerboundPlaceRecipePacket(menu.containerId, displays.getFirst().id(), false));

        helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), 1, "one template placed");
        helper.assertTrue(menu.getSlot(1).getItem().is(Items.DIAMOND_CHESTPLATE) && menu.getSlot(1).getItem().getDamageValue() == 10,
                "the damaged chestplate is the base: " + menu.getSlot(1).getItem());
        helper.assertTrue(menu.getSlot(2).getItem().is(Items.NETHERITE_INGOT), "the ingot is the addition");
        helper.assertTrue(menu.getSlot(menu.getResultSlot()).getItem().is(Items.NETHERITE_CHESTPLATE), "the result is ready");
        helper.assertValueEqual(inventory.countItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 1, "one template left in the inventory");

        SmithingRecipe sword = (SmithingRecipe) vanillaRecipe(helper, "netherite_sword_smithing").value();
        helper.assertValueEqual(SmithingPlacement.place(menu, sword, inventory, false), RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE,
                "without a diamond sword the book only shows the ghost recipe");
        for (int slot = 0; slot < 3; slot++) {
            helper.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "field " + slot + " was cleared for the ghost recipe");
        }
        helper.assertValueEqual(inventory.countItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 2, "templates back in the inventory");
        helper.assertValueEqual(inventory.countItem(Items.DIAMOND_CHESTPLATE), 1, "chestplate back in the inventory");
        helper.assertValueEqual(inventory.countItem(Items.NETHERITE_INGOT), 1, "ingot back in the inventory");
        helper.succeed();
    }

    /**
     * Wie der Crafter: eine steigende Flanke schmiedet genau einmal, das Ergebnis landet in der Truhe vor der Front,
     * Dauersignal loest nicht erneut aus, eine neue Flanke schon. Ohne passendes Rezept bleibt alles liegen.
     */
    public static void autoSmitherSmithsOncePerPulse(GameTestHelper helper) {
        if (!McVersion.AUTO_SMITHER) {
            helper.succeed();
            return;
        }
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = smitherPos.east();
        BlockPos powerPos = smitherPos.west();
        helper.setBlock(smitherPos, ModBlocks.AUTO_SMITHER.defaultBlockState().setValue(AutoSmitherBlock.FACING, Direction.EAST));
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

    /**
     * Trichter legen nach Vanillas Schmiede-Mengen ein (Vorlage, Basis, Material), Fremdes bleibt draussen, herausziehen
     * geht nur aus dem gespeicherten Ergebnisslot.
     */
    public static void autoSmitherSortsHopperInput(GameTestHelper helper) {
        if (!McVersion.AUTO_SMITHER) {
            helper.succeed();
            return;
        }
        BlockPos smitherPos = new BlockPos(1, 2, 1);
        helper.setBlock(smitherPos, ModBlocks.AUTO_SMITHER);
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
        helper.assertTrue(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).getItem().is(Items.NETHERITE_PICKAXE), "finished output is stored");
        helper.setBlock(smitherPos.below(), Blocks.HOPPER);
        helper.startSequence().thenExecuteAfter(20, () -> {
            helper.assertTrue(smither.getItem(AutoSmitherBlockEntity.RESULT_SLOT).isEmpty(), "hopper removed output");
            helper.assertValueEqual(helper.getBlockEntity(smitherPos.below(), HopperBlockEntity.class).countItem(Items.NETHERITE_PICKAXE),
                    1, "hopper contains the finished result");
            helper.assertValueEqual(smither.getItem(0).getCount(), 2, "hopper did not take templates");
            helper.assertValueEqual(smither.getItem(2).getCount(), 3, "hopper did not take additions");
        }).thenSucceed();
    }

    /** Exercise the server click dispatcher, not just the slot predicate. */
    public static void autoSmitherOutputRejectsInsertion(GameTestHelper helper) {
        if (!McVersion.AUTO_SMITHER) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.AUTO_SMITHER);
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
        if (!McVersion.AUTO_SMITHER) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.AUTO_SMITHER);
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
        int[][] positions = {{8, 48}, {26, 48}, {44, 48}, {98, 48}, {8, 84}, {8, 142}};
        int[] indices = {0, 1, 2, 3, 4, 31};
        for (int i = 0; i < indices.length; i++) {
            helper.assertValueEqual(menu.getSlot(indices[i]).x, positions[i][0], "vanilla slot x");
            helper.assertValueEqual(menu.getSlot(indices[i]).y, positions[i][1], "vanilla slot y");
        }
        helper.succeed();
    }
}
