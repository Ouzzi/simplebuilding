package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.client.gui.AutoSmitherScreen;
import com.simplebuilding.client.gui.BackpackScreen;
import com.simplebuilding.client.gui.ModScreenStyle;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.TieredChestScreen;
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
 * The mod screens in the container style (26.3, {@code ModScreenStyle}; simpleinterfaces plan W1 G4): each screen
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
        if (ModScreenStyle.ACTIVE) {
            // N43 (PLAN-UI-KONZEPTE-2026-10-12): the slotless screens, first with the comparison toggle off (old look),
            // then in the container style. Literal names again.
            script.act("style off for the old look", c -> ModScreenStyle.setPanels(false));
            panel(script, "modui-octant-vanilla", c -> new com.simplebuilding.client.gui.OctantScreen(new ItemStack(com.simplebuilding.items.ModItems.OCTANT)));
            panel(script, "modui-building-wand-vanilla", c -> new com.simplebuilding.client.gui.BuildingWandScreen(new ItemStack(com.simplebuilding.items.ModItems.NETHERITE_BUILDING_WAND)));
            panel(script, "modui-trim-reference-vanilla", c -> new com.simplebuilding.client.gui.TrimReferenceScreen(null));
            script.act("style on again", c -> ModScreenStyle.setPanels(true));
            panel(script, "modui-octant", c -> new com.simplebuilding.client.gui.OctantScreen(new ItemStack(com.simplebuilding.items.ModItems.OCTANT)));
            panel(script, "modui-building-wand", c -> new com.simplebuilding.client.gui.BuildingWandScreen(new ItemStack(com.simplebuilding.items.ModItems.NETHERITE_BUILDING_WAND)));
            panel(script, "modui-building-wand-gold", c -> new com.simplebuilding.client.gui.BuildingWandScreen(new ItemStack(com.simplebuilding.items.ModItems.GOLD_BUILDING_WAND)));
            panel(script, "modui-trim-reference", c -> new com.simplebuilding.client.gui.TrimReferenceScreen(null));
        }
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
        // The fletching table's screen (and its book) is Simple Arrows' since 04.11. (FletchingScreen; real-mouse probe there).
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
        // ENCH02: the enchanter screen is Simple Enchanting's (its own client smoke, open).
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

    /** A slotless screen (N43): opened, rendered, still open, shot. */
    private static void panel(Script script, String name, Function<Minecraft, net.minecraft.client.gui.screens.Screen> screen) {
        script.act("open " + name, c -> c.gui.setScreen(screen.apply(c)));
        script.idle("let " + name + " render", 3);
        script.act(name + " is still open", c -> {
            if (c.gui.screen() == null) throw new AssertionError(name + " closed itself");
        });
        script.shot(name);
    }
}
