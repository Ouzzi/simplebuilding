package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.client.gui.AutoSmitherScreen;
import com.simplebuilding.client.gui.BackpackScreen;
import com.simplebuilding.client.gui.FletchingScreen;
import com.simplebuilding.client.gui.ModScreenStyle;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.TieredChestScreen;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackOpenData;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.screen.TieredChestOpenData;
import com.simplebuilding.version.McVersion;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The mod screens in the container style (26.3, {@code ModScreenStyle}; simplecontainers plan W1 G4): each screen
 * is opened on the client with a client-side menu, rendered for a few frames and photographed for the comparison
 * with the previews {@code g4-*.png}. On 26.2 the same screens show their old look (the twin draws nothing).
 *
 * <p>What breaks it: a screen throwing while it draws (missing slot, wrong index), a screen closing by itself, the
 * style switch drifting from the line (26.3 must draw the style). The pictures themselves are checked by eye.
 */
public final class ModUiStyleClientTest {
    private static final BlockPos HOPPER_POS = new BlockPos(10, 1, 22);

    private ModUiStyleClientTest() {}

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");
        script.act("the style is on exactly where SimpleLib is (26.3)", c -> {
            if (ModScreenStyle.ACTIVE != McVersion.CRUCIBLE) {
                throw new AssertionError("ModScreenStyle.ACTIVE is " + ModScreenStyle.ACTIVE + " but SimpleLib's crucible is "
                        + (McVersion.CRUCIBLE ? "there" : "missing") + " on this line");
            }
        });
        // Literal names: the runner reads the expected screenshots from the source.
        chest(script, "modui-chest-reinforced", ChestTier.REINFORCED);
        chest(script, "modui-chest-netherite", ChestTier.NETHERITE);
        chest(script, "modui-chest-enderite", ChestTier.ENDERITE);
        show(script, "modui-chest-reinforced-single", c -> {
            Inventory inv = c.player.getInventory();
            return new TieredChestScreen(new TieredChestMenu(0, inv, new TieredChestOpenData(ChestTier.REINFORCED.ordinal(), false, false)),
                    inv, Component.translatable("container.chest"));
        });
        backpack(script, "modui-backpack-basic", BackpackTier.BASIC);
        backpack(script, "modui-backpack-reinforced", BackpackTier.REINFORCED);
        backpack(script, "modui-backpack-netherite", BackpackTier.NETHERITE);
        backpack(script, "modui-backpack-enderite", BackpackTier.ENDERITE);
        show(script, "modui-backpack-dyed", c -> {
            Inventory inv = c.player.getInventory();
            return new BackpackScreen(new BackpackMenu(0, inv, BackpackOpenData.worn(BackpackTier.REINFORCED, 1, 0x3C44AA)), inv, Component.empty());
        });
        if (McVersion.AUTO_SMITHER) show(script, "modui-auto-smither", c -> {
            Inventory inv = c.player.getInventory();
            AutoSmitherMenu menu = new AutoSmitherMenu(0, inv);
            menu.setData(0, 1);
            return new AutoSmitherScreen(menu, inv, Component.translatable("block.simplebuilding.auto_smither"));
        });
        if (McVersion.FLETCHING) show(script, "modui-fletching", c -> {
            Inventory inv = c.player.getInventory();
            return new FletchingScreen(new FletchingMenu(0, inv), inv, Component.translatable("container.simplebuilding.fletching"));
        });
        if (McVersion.AUTONOMOUS_CRAFTER) show(script, "modui-autonomous-crafter", c -> {
            Inventory inv = c.player.getInventory();
            com.simplebuilding.screen.AutonomousCrafterMenu menu = new com.simplebuilding.screen.AutonomousCrafterMenu(0, inv);
            menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 5));
            menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS, 5));
            menu.setData(4, 1);
            menu.setData(com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity.DATA_FILTER, 1);
            return new com.simplebuilding.client.gui.AutonomousCrafterScreen(menu, inv,
                    Component.translatable("container.simplebuilding.autonomous_crafter"));
        });
        if (McVersion.ASTRAL_ENCHANTING) show(script, "modui-astral-enchanting", c -> {
            Inventory inv = c.player.getInventory();
            com.simplebuilding.screen.AstralEnchantingMenu menu = new com.simplebuilding.screen.AstralEnchantingMenu(0, inv);
            menu.getSlot(com.simplebuilding.screen.AstralEnchantingMenu.ITEM_SLOT).set(new ItemStack(Items.DIAMOND_PICKAXE));
            menu.getSlot(com.simplebuilding.screen.AstralEnchantingMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 20));
            menu.getSlot(com.simplebuilding.screen.AstralEnchantingMenu.BLAZE_SLOT).set(new ItemStack(Items.BLAZE_POWDER, 40));
            var ids = c.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var map = ids.asHolderIdMap();
            var shown = java.util.List.of(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY,
                    net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
            int[] max = {5, 3, 3}, chosen = {4, 3, 0}, cost = {3, 3, 6};
            menu.setData(0, 30);
            for (int row = 0; row < 3; row++) {
                menu.setData(1 + row, map.getId(ids.getOrThrow(shown.get(row))));
                menu.setData(4 + row, max[row]);
                menu.setData(7 + row, chosen[row]);
                menu.setData(10 + row, cost[row]);
            }
            return new com.simplebuilding.client.gui.AstralEnchantingScreen(menu, inv,
                    Component.translatable("container.simplebuilding.astral_enchanting_table"));
        });
        hopper(script, "modui-hopper-reinforced", "reinforced_hopper", 0);
        hopper(script, "modui-hopper-netherite", "netherite_hopper", 1);
        hopper(script, "modui-hopper-enderite", "enderite_hopper", 2);
        script.act("close the last screen", c -> c.gui.setScreen(null));
        script.command("setblock " + HOPPER_POS.getX() + " " + HOPPER_POS.getY() + " " + HOPPER_POS.getZ() + " minecraft:air");
    }

    private static void chest(Script script, String name, ChestTier tier) {
        show(script, name, c -> {
            Inventory inv = c.player.getInventory();
            TieredChestMenu menu = new TieredChestMenu(0, inv, new TieredChestOpenData(tier.ordinal(), true, false));
            menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT, 64));
            menu.getSlot(menu.columns() + 1).set(new ItemStack(Items.COPPER_INGOT, 30));
            return new TieredChestScreen(menu, inv, Component.translatable("container.chestDouble"));
        });
    }

    private static void backpack(Script script, String name, BackpackTier tier) {
        show(script, name, c -> {
            Inventory inv = c.player.getInventory();
            return new BackpackScreen(new BackpackMenu(0, inv, BackpackOpenData.worn(tier, 1)), inv, Component.empty());
        });
    }

    /** A placed hopper of that kind, its screen with filter {@code mode} (0 off, 1 exact, 2 kind) and two filter items. */
    private static void hopper(Script script, String name, String id, int mode) {
        script.command("setblock " + HOPPER_POS.getX() + " " + HOPPER_POS.getY() + " " + HOPPER_POS.getZ() + " simplebuilding:" + id);
        script.awaitPackets();
        script.idle("let the placed hopper reach the client", 5);
        show(script, name, c -> {
            Inventory inv = c.player.getInventory();
            NetheriteHopperScreenHandler menu = new NetheriteHopperScreenHandler(0, inv, HOPPER_POS);
            menu.setData(0, mode);
            // Filter principle: the real items in the slots are the filter.
            menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT, 12));
            menu.getSlot(1).set(new ItemStack(Items.GOLD_INGOT));
            return new NetheriteHopperScreen(menu, inv, Component.translatable("block.simplebuilding." + id));
        });
    }

    private static void show(Script script, String name, Function<Minecraft, AbstractContainerScreen<?>> screen) {
        script.act("open " + name, c -> c.gui.setScreen(screen.apply(c)));
        script.idle("let " + name + " render", 3);
        script.act(name + " is still open", c -> {
            if (!(c.gui.screen() instanceof AbstractContainerScreen<?>)) {
                throw new AssertionError(name + " closed itself: " + c.gui.screen());
            }
        });
        script.shot(name);
    }
}
