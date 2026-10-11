package com.simplebuilding.modules.simplelib;
import com.simplelib.client.CrucibleScreen;
import com.simplelib.crucible.CrucibleMenu;
import com.simplelib.crucible.CrucibleTier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
/** Client smoke: the module boots; the crucible window with an attached barrel (N14: from enderite the barrel box sits below). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplelib")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplelib-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            crucible(context, CrucibleTier.NETHERITE);
            context.takeScreenshot("simplelib-crucible-netherite-barrel");
            crucible(context, CrucibleTier.ENDERITE);
            context.takeScreenshot("simplelib-crucible-enderite-barrel");
            // Layout comparison: 854x480 (GUI scale 2) and 1920x1080 (auto scale) with the barrel docked.
            // (a real window resize, so the GUI re-lays out at the auto scale)
            context.runOnClient(c -> c.getWindow().setWindowed(1920, 1080));
            context.waitTicks(3);
            context.takeScreenshot("simplelib-crucible-enderite-barrel-1080");
            context.runOnClient(c -> c.getWindow().setWindowed(854, 480));
            context.waitTicks(3);
            // N34 layout mockups a/b/c at 854x480 and 1920x1080 (-Dsimplelib.crucibleLayout limits them, default all).
            String only = System.getProperty("simplelib.crucibleLayout", "abc");
            for (char v : new char[] {'a', 'b', 'c'}) {
                if (only.indexOf(v) < 0) continue;
                context.runOnClient(c -> c.setScreenAndShow(new CrucibleLayoutPreview(v)));
                context.waitTicks(3);
                context.takeScreenshot("simplelib-tiegel-" + v + "-854");
                context.runOnClient(c -> c.getWindow().setWindowed(1920, 1080));
                context.waitTicks(3);
                context.takeScreenshot("simplelib-tiegel-" + v + "-1080");
                context.runOnClient(c -> c.getWindow().setWindowed(854, 480));
                context.waitTicks(3);
            }
            context.runOnClient(c -> c.gui.setScreen(null));
        }
    }

    /** A client-side crucible menu of {@code tier} with a barrel attached and a few items, opened and rendered. */
    private static void crucible(ClientGameTestContext context, CrucibleTier tier) {
        context.runOnClient(c -> {
            Inventory inv = c.player.getInventory();
            CrucibleMenu menu = CrucibleMenu.client(tier, 0, inv);
            menu.setData(tier.slots() + 3, 1);
            menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 12));
            menu.getSlot(menu.wideCrucibleStart()).set(new ItemStack(Items.RAW_IRON, 12));
            menu.getSlot(menu.barrelStart()).set(new ItemStack(Items.IRON_INGOT, 5));
            c.setScreenAndShow(new CrucibleScreen(menu, inv, Component.translatable("container.simplelib." + tier.id() + "_crucible")));
        });
        context.waitTicks(3);
        context.runOnClient(c -> {
            if (!(c.gui.screen() instanceof CrucibleScreen)) throw new AssertionError(tier + " crucible screen closed itself: " + c.gui.screen());
        });
    }
}
