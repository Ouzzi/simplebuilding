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
     * geht nicht; das Menue zeigt die Vorschau des Ergebnisses.
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
        for (int slot = 0; slot < AutoSmitherBlockEntity.SIZE; slot++) {
            helper.assertFalse(smither.canTakeItemThroughFace(slot, smither.getItem(slot), Direction.DOWN), "hoppers cannot pull slot " + slot);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AutoSmitherMenu menu = new AutoSmitherMenu(3, player.getInventory(), smither, new SimpleContainerData(1));
        helper.assertTrue(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).getItem().is(Items.NETHERITE_PICKAXE),
                "the menu previews the result: " + menu.getSlot(AutoSmitherMenu.RESULT_SLOT).getItem());
        helper.assertFalse(menu.getSlot(AutoSmitherMenu.RESULT_SLOT).mayPickup(player), "the preview cannot be taken");
        helper.succeed();
    }
}
