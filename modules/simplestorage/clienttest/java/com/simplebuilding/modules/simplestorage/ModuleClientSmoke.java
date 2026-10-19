package com.simplebuilding.modules.simplestorage;

import com.simplestorage.client.StorageCraftingScreen;
import com.simplestorage.crate.CrateBlockEntity;
import com.simplestorage.crate.CrateWood;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.table.StorageCraftingMenu;
import com.simplestorage.table.StorageCraftingTableBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client smoke: the module boots; a crate with food and the Storage Crafting Table with its grid lying on top, and the
 * table's screen in the container style (pictures that moved here from SimpleBuilding's client tests in split stage 8a).
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplestorage")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplestorage-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var table = p.blockPosition().offset(0, 0, -2);
                var crate = table.east();
                l.setBlockAndUpdate(table, StorageBlocks.STORAGE_CRAFTING_TABLE.defaultBlockState());
                if (l.getBlockEntity(table) instanceof StorageCraftingTableBlockEntity grid) {
                    grid.grid().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
                    grid.grid().setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
                    grid.grid().setItem(3, new ItemStack(Items.STICK, 2));
                    grid.grid().setItem(4, new ItemStack(Items.DIAMOND));
                    grid.grid().setItem(8, new ItemStack(Items.REDSTONE, 3));
                }
                l.setBlockAndUpdate(crate, StorageBlocks.CRATES.get(CrateWood.OAK).defaultBlockState());
                if (l.getBlockEntity(crate) instanceof CrateBlockEntity food) {
                    food.insert(new ItemStack(Items.BREAD, 64));
                    food.insert(new ItemStack(Items.APPLE, 40));
                }
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(table));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simplestorage-table-and-crate");
            context.runOnClient(c -> {
                var inv = c.player.getInventory();
                StorageCraftingMenu menu = new StorageCraftingMenu(0, inv);
                for (int slot : new int[] {1, 2, 4, 5}) menu.getSlot(slot).set(new ItemStack(Items.OAK_PLANKS, 4));
                menu.getSlot(StorageCraftingMenu.RESULT_SLOT).set(new ItemStack(Items.CRAFTING_TABLE));
                c.gui.setScreen(new StorageCraftingScreen(menu, inv, Component.translatable("container.simplestorage.storage_crafting_table")));
            });
            context.waitTicks(3);
            context.takeScreenshot("simplestorage-table-screen");
            context.runOnClient(c -> c.gui.setScreen(null));
        }
    }
}
