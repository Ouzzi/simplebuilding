package com.simplebuilding.modules.simpleinterfaces;

import com.simplelib.api.client.ui.UiStyleToggle;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
/** Client smoke: the storage screens (W0 + W1 G1 crafter and mounts), the G2 and G3 work screens open styled in a world; screenshots for the owner's review. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleinterfaces")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            show(context, "simpleinterfaces-chest", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.chest")));
            showVanillaComparison(context, "simpleinterfaces-chest-vanilla", c -> new ContainerScreen(
                    ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.chest")));
            show(context, "simpleinterfaces-double-chest", c -> new ContainerScreen(ChestMenu.sixRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.chestDouble")));
            show(context, "simpleinterfaces-barrel", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.barrel")));
            show(context, "simpleinterfaces-ender-chest", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.enderchest")));
            show(context, "simpleinterfaces-shulker", c -> new ShulkerBoxScreen(new ShulkerBoxMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.shulkerBox")));
            show(context, "simpleinterfaces-hopper", c -> new HopperScreen(new HopperMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.hopper")));
            show(context, "simpleinterfaces-dispenser", c -> new DispenserScreen(new DispenserMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.dispenser")));
            show(context, "simpleinterfaces-crafter", c -> {
                CrafterMenu menu = new CrafterMenu(0, c.player.getInventory());
                for (int i : new int[] {0, 1, 2, 6, 7, 8}) menu.getSlot(i).set(new ItemStack(Items.IRON_INGOT, 2));
                menu.getSlot(9).set(new ItemStack(Items.BUCKET));
                menu.setData(4, 1); // slot 4 disabled
                menu.setData(9, 1); // powered
                return new CrafterScreen(menu, c.player.getInventory(), Component.translatable("container.crafter"));
            });
            show(context, "simpleinterfaces-donkey", c -> mount(c, EntityTypes.DONKEY));
            show(context, "simpleinterfaces-horse", c -> mount(c, EntityTypes.HORSE));
            // G3 (work II): survival (else the inventory opens the creative screen), a few items for the screenshots
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("give @a minecraft:iron_pickaxe");
            world.getServer().runCommand("give @a minecraft:oak_log 32");
            world.getServer().runCommand("give @a minecraft:bread 12");
            context.waitTicks(5);
            show(context, "simpleinterfaces-anvil", c -> {
                var menu = new AnvilMenu(0, c.player.getInventory());
                var worn = new ItemStack(Items.IRON_PICKAXE);
                worn.setDamageValue(120);
                menu.getSlot(0).set(worn);
                menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
                return new AnvilScreen(menu, c.player.getInventory(), Component.translatable("container.repair"));
            });
            show(context, "simpleinterfaces-grindstone", c -> new GrindstoneScreen(new GrindstoneMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.grindstone_title")));
            show(context, "simpleinterfaces-stonecutter", c -> {
                var menu = new StonecutterMenu(0, c.player.getInventory());
                menu.getSlot(0).set(new ItemStack(Items.STONE, 32));
                return new StonecutterScreen(menu, c.player.getInventory(), Component.translatable("container.stonecutter"));
            });
            show(context, "simpleinterfaces-loom", c -> {
                var menu = new LoomMenu(0, c.player.getInventory());
                var screen = new LoomScreen(menu, c.player.getInventory(), Component.translatable("container.loom"));
                menu.getSlot(0).set(new ItemStack(Items.BANNER.white())); // after the screen: it listens for the pattern list
                menu.getSlot(1).set(new ItemStack(Items.DYE.red(), 4));
                return screen;
            });
            show(context, "simpleinterfaces-cartography", c -> {
                var menu = new CartographyTableMenu(0, c.player.getInventory());
                menu.getSlot(1).set(new ItemStack(Items.PAPER, 8));
                return new CartographyTableScreen(menu, c.player.getInventory(), Component.translatable("container.cartography_table"));
            });
            show(context, "simpleinterfaces-smithing", c -> {
                var menu = new SmithingMenu(0, c.player.getInventory());
                menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
                menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
                return new SmithingScreen(menu, c.player.getInventory(), Component.translatable("container.upgrade"));
            });
            show(context, "simpleinterfaces-merchant", c -> {
                var menu = new MerchantMenu(0, c.player.getInventory());
                var offers = new MerchantOffers();
                offers.add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 12, 2, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.IRON_INGOT, 4), new ItemStack(Items.EMERALD), 12, 2, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), Optional.of(new ItemCost(Items.BOOK)), new ItemStack(Items.ENCHANTED_BOOK), 12, 2, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 6), 12, 2, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 15), new ItemStack(Items.BELL), 12, 2, 0.05F));
                menu.setOffers(offers);
                menu.setShowProgressBar(true);
                menu.setMerchantLevel(2);
                menu.setXp(30);
                return new MerchantScreen(menu, c.player.getInventory(), Component.translatable("entity.minecraft.villager.weaponsmith"));
            });
            show(context, "simpleinterfaces-inventory", c -> new InventoryScreen(c.player));
            showWork(context);
            context.runOnClient(c -> c.setScreenAndShow(null));
        }
    }

    /** A tamed mount next to the player (a donkey with a chest: 5 columns); its inventory screen as the server would open it. */
    private static Screen mount(Minecraft c, EntityType<? extends AbstractHorse> type) {
        AbstractHorse horse = type.create(c.level, EntitySpawnReason.LOAD);
        horse.setId(-4200 - type.hashCode() % 100); // the inventory preview renders it, which needs an id
        horse.setPos(c.player.position());
        horse.setTamed(true);
        int columns = 0;
        if (horse instanceof AbstractChestedHorse chested) {
            chested.setChest(true);
            columns = chested.getInventoryColumns();
        }
        SimpleContainer container = new SimpleContainer(Math.max(1, columns * 3));
        if (columns > 0) {
            container.setItem(0, new ItemStack(Items.HAY_BLOCK, 12));
            container.setItem(1, new ItemStack(Items.GOLDEN_CARROT, 6));
        }
        HorseInventoryMenu menu = new HorseInventoryMenu(0, c.player.getInventory(), container, horse, columns);
        return new HorseInventoryScreen(menu, c.player.getInventory(), horse, columns);
    }

    private static void show(ClientGameTestContext context, String name, Function<Minecraft, Screen> screen) {
        UiStyleToggle.setEnabled(true);
        context.runOnClient(c -> c.setScreenAndShow(screen.apply(c)));
        context.waitTicks(3);
        context.runOnClient(c -> {
            try {
                Object style = Class.forName("com.simplebuilding.modules.simpleinterfaces.client.StyledScreens")
                        .getMethod("style", net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class).invoke(null, c.gui.screen());
                if (style == null) throw new AssertionError(name + ": screen is not styled");
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        context.takeScreenshot(name);
    }

    private static void showVanillaComparison(ClientGameTestContext context, String name, Function<Minecraft, Screen> screen) {
        UiStyleToggle.setEnabled(false);
        context.runOnClient(c -> c.setScreenAndShow(screen.apply(c)));
        context.waitTicks(3);
        context.runOnClient(c -> {
            try {
                Object style = Class.forName("com.simplebuilding.modules.simpleinterfaces.client.StyledScreens")
                        .getMethod("style", net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class).invoke(null, c.gui.screen());
                if (style != null) throw new AssertionError(name + ": style remained active after toggle");
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        context.takeScreenshot(name);
        UiStyleToggle.setEnabled(true);
    }

    /** W1 G2: crafting table, the three furnaces (lit, half cooked), brewing stand (brewing), beacon, enchanting table. */
    private static void showWork(ClientGameTestContext context) {
        show(context, "simpleinterfaces-crafting", c -> {
            var menu = new net.minecraft.world.inventory.CraftingMenu(0, c.player.getInventory());
            for (int i : new int[] {1, 2, 3}) menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS));
            for (int i : new int[] {5, 8}) menu.getSlot(i).set(new ItemStack(Items.STICK));
            menu.getSlot(0).set(new ItemStack(Items.WOODEN_PICKAXE));
            return new net.minecraft.client.gui.screens.inventory.CraftingScreen(menu, c.player.getInventory(), Component.translatable("container.crafting"));
        });
        show(context, "simpleinterfaces-furnace", c -> {
            var menu = new net.minecraft.world.inventory.FurnaceMenu(0, c.player.getInventory());
            furnace(menu, Items.RAW_IRON, 12, null, Items.IRON_INGOT, 4, 130, 110);
            return new net.minecraft.client.gui.screens.inventory.FurnaceScreen(menu, c.player.getInventory(), Component.translatable("container.furnace"));
        });
        show(context, "simpleinterfaces-blast-furnace", c -> {
            var menu = new net.minecraft.world.inventory.BlastFurnaceMenu(0, c.player.getInventory());
            furnace(menu, Items.RAW_GOLD, 20, null, Items.GOLD_INGOT, 7, 180, 60);
            return new net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen(menu, c.player.getInventory(), Component.translatable("container.blast_furnace"));
        });
        show(context, "simpleinterfaces-smoker", c -> {
            var menu = new net.minecraft.world.inventory.SmokerMenu(0, c.player.getInventory());
            furnace(menu, Items.BEEF, 8, Items.OAK_LOG, Items.COOKED_BEEF, 3, 80, 160);
            return new net.minecraft.client.gui.screens.inventory.SmokerScreen(menu, c.player.getInventory(), Component.translatable("container.smoker"));
        });
        show(context, "simpleinterfaces-brewing-stand", c -> {
            var menu = new net.minecraft.world.inventory.BrewingStandMenu(0, c.player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.POTION));
            menu.getSlot(2).set(new ItemStack(Items.POTION));
            menu.getSlot(3).set(new ItemStack(Items.NETHER_WART, 5));
            menu.setData(0, 220);
            menu.setData(1, 14);
            menu.setData(2, 400);
            menu.setData(3, 20);
            return new net.minecraft.client.gui.screens.inventory.BrewingStandScreen(menu, c.player.getInventory(), Component.translatable("container.brewing"));
        });
        show(context, "simpleinterfaces-beacon", c -> {
            var menu = new net.minecraft.world.inventory.BeaconMenu(0, c.player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
            menu.setData(0, 4);
            return new net.minecraft.client.gui.screens.inventory.BeaconScreen(menu, c.player.getInventory(), Component.translatable("container.beacon"));
        });
        show(context, "simpleinterfaces-enchanting", c -> {
            var menu = new net.minecraft.world.inventory.EnchantmentMenu(0, c.player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 9));
            int[] costs = {5, 17, 30};
            for (int i = 0; i < 3; i++) {
                menu.setData(i, costs[i]);
                menu.setData(4 + i, 0);
                menu.setData(7 + i, 1);
            }
            return new net.minecraft.client.gui.screens.inventory.EnchantmentScreen(menu, c.player.getInventory(), Component.translatable("container.enchant"));
        });
    }

    /** Fills a furnace menu: input, fuel (or none), result and data - lit 0..200, cooking 0..200. */
    private static void furnace(net.minecraft.world.inventory.AbstractFurnaceMenu menu, net.minecraft.world.item.Item input, int count,
            net.minecraft.world.item.Item fuel, net.minecraft.world.item.Item result, int results, int lit, int cooked) {
        menu.getSlot(0).set(new ItemStack(input, count));
        if (fuel != null) menu.getSlot(1).set(new ItemStack(fuel, 14));
        menu.getSlot(2).set(new ItemStack(result, results));
        menu.setData(0, lit);
        menu.setData(1, 200);
        menu.setData(2, cooked);
        menu.setData(3, 200);
    }
}
