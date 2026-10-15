package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.client.ShopScreens;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client smoke: a shop with one to five goods in the world (plain and glow case, dimmed while inactive) and the three
 * screens (price row layout, buyer screen with an enabled trade button, goods screen) with screenshots.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    private static final ItemStack[] GOODS = {new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.OAK_PLANKS, 40), new ItemStack(Items.ENDER_PEARL, 8)};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleshops")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("weather clear");
            // A row of shops 4 blocks ahead: 1..5 goods (plain), one glow case with 3 goods, one case without a price (inactive).
            for (int n = 1; n <= 5; n++) {
                int x = (n - 3) * 2;
                server.runCommand("execute at @a run fill ~" + x + " ~-1 ~4 ~" + x + " ~-1 ~4 minecraft:stone");
                server.runCommand("execute at @a run setblock ~" + x + " ~ ~4 simpleshops:sales_block");
                server.runCommand("execute at @a run setblock ~" + x + " ~1 ~4 simpleshops:display_case");
            }
            server.runCommand("execute at @a run fill ~-6 ~-1 ~4 ~-6 ~-1 ~4 minecraft:stone");
            server.runCommand("execute at @a run setblock ~-6 ~ ~4 simpleshops:sales_block");
            server.runCommand("execute at @a run setblock ~-6 ~1 ~4 simpleshops:glow_display_case");
            server.runCommand("execute at @a run fill ~6 ~-1 ~4 ~6 ~-1 ~4 minecraft:stone");
            server.runCommand("execute at @a run setblock ~6 ~ ~4 simpleshops:sales_block");
            server.runCommand("execute at @a run setblock ~6 ~1 ~4 simpleshops:display_case");
            context.waitTicks(5);
            server.runOnServer(s -> {
                var level = s.overworld();
                var player = s.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition().offset(0, 0, 4);
                for (int n = 1; n <= 5; n++) fill(level, base.offset((n - 3) * 2, 0, 0), n, true);
                fill(level, base.offset(-6, 0, 0), 3, true);
                fill(level, base.offset(6, 0, 0), 2, false);
            });
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 15");
            context.waitTicks(60);
            context.takeScreenshot("simpleshops-world");
            server.runCommand("execute as @a at @s run tp @s ~-6 ~ ~ -25 10");
            context.waitTicks(40);
            context.takeScreenshot("simpleshops-glow");
            screens(context);
        }
    }

    private static void fill(net.minecraft.server.level.ServerLevel level, BlockPos sales, int goods, boolean price) {
        var salesEntity = (SalesBlockEntity) level.getBlockEntity(sales);
        var caseEntity = (DisplayCaseBlockEntity) level.getBlockEntity(sales.above());
        for (int i = 0; i < goods; i++) caseEntity.goods().set(i, GOODS[i].copy());
        if (price) salesEntity.prices().set(2, new ItemStack(Items.EMERALD, 4));
        salesEntity.changed();
        caseEntity.changed();
    }

    private static void screens(ClientGameTestContext context) {
        context.runOnClient(c -> {
            var menu = new SalesOwnerMenu(1, c.player.getInventory());
            menu.slots.get(2).container.setItem(2, new ItemStack(Items.EMERALD, 12));
            menu.slots.get(1).container.setItem(1, new ItemStack(Items.DIAMOND, 1));
            menu.slots.get(3).container.setItem(3, new ItemStack(Items.GOLD_INGOT, 64));
            menu.slots.get(5 + 1).container.setItem(1, new ItemStack(Items.COAL, 9));
            c.setScreenAndShow(new ShopScreens.Owner(menu, c.player.getInventory(), Component.translatable("container.simpleshops.sales")));
        });
        context.waitTicks(3);
        context.takeScreenshot("simpleshops-owner-screen");
        context.runOnClient(c -> {
            var menu = new ShopBuyerMenu(2, c.player.getInventory());
            for (int i = 0; i < 4; i++) menu.goodsView().setItem(i, GOODS[i].copy());
            menu.priceView().setItem(2, new ItemStack(Items.EMERALD, 4));
            menu.priceView().setItem(3, new ItemStack(Items.COAL, 2));
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            menu.pay().setItem(3, new ItemStack(Items.COAL, 2));
            c.setScreenAndShow(new ShopScreens.Buyer(menu, c.player.getInventory(), Component.translatable("container.simpleshops.buyer", "Alex")));
        });
        context.waitTicks(3);
        context.runOnClient(c -> {
            if (!(c.gui.screen() instanceof ShopScreens.Buyer screen)) throw new AssertionError("not the buyer screen");
            var buttons = screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractButton).toList();
            if (buttons.size() != 1) throw new AssertionError("one trade button expected, got " + buttons.size());
            var button = (net.minecraft.client.gui.components.AbstractButton) buttons.get(0);
            if (!button.active) throw new AssertionError("trade button should be enabled with the exact payment");
            if (button.getRight() > c.getWindow().getGuiScaledWidth() || button.getBottom() > c.getWindow().getGuiScaledHeight()) throw new AssertionError("button outside the window");
        });
        context.takeScreenshot("simpleshops-buyer-screen");
        context.runOnClient(c -> {
            var menu = new StockMenu(3, c.player.getInventory());
            for (int i = 0; i < 3; i++) menu.slots.get(i).container.setItem(i, GOODS[i].copy());
            c.setScreenAndShow(new ShopScreens.Stock(menu, c.player.getInventory(), Component.translatable("container.simpleshops.stock")));
        });
        context.waitTicks(3);
        context.takeScreenshot("simpleshops-stock-screen");
        context.runOnClient(c -> c.setScreenAndShow(null));
    }
}
