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
/** Client smoke: the module boots; the crucible window with an attached barrel (N37: the barrel box beside the crucible on every tier, inventory centred below). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplelib")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplelib-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            // N39: outline (selection box) of the crucible with a docked barrel, looked at from the side and from above.
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var at = p.blockPosition().offset(0, 0, -3);
                l.setBlockAndUpdate(at, com.simplelib.registry.LibBlocks.NETHERITE_CRUCIBLE.defaultBlockState()
                        .setValue(com.simplelib.crucible.CrucibleBlock.FACING, net.minecraft.core.Direction.EAST));
                l.setBlockAndUpdate(at.west(), com.simplelib.registry.LibBlocks.NETHERITE_BARREL.defaultBlockState()
                        .setValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED, true)
                        .setValue(com.simplelib.crucible.CrucibleBarrelBlock.FACING, net.minecraft.core.Direction.EAST));
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, -0.2, 0));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simplelib-crucible-hitbox");
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
            // N37: every tier with and without a barrel (7 px margins, compact boxes).
            // (literal names: the test runner reads them from the source)
            crucible(context, CrucibleTier.IRON, false);
            context.takeScreenshot("simplelib-crucible-iron-alone");
            crucible(context, CrucibleTier.IRON, true);
            context.takeScreenshot("simplelib-crucible-iron-with-barrel");
            crucible(context, CrucibleTier.REINFORCED, false);
            context.takeScreenshot("simplelib-crucible-reinforced-alone");
            crucible(context, CrucibleTier.REINFORCED, true);
            context.takeScreenshot("simplelib-crucible-reinforced-with-barrel");
            crucible(context, CrucibleTier.NETHERITE, false);
            context.takeScreenshot("simplelib-crucible-netherite-alone");
            crucible(context, CrucibleTier.NETHERITE, true);
            context.takeScreenshot("simplelib-crucible-netherite-with-barrel");
            crucible(context, CrucibleTier.ENDERITE, false);
            context.takeScreenshot("simplelib-crucible-enderite-alone");
            crucible(context, CrucibleTier.ENDERITE, true);
            context.takeScreenshot("simplelib-crucible-enderite-with-barrel");
            context.runOnClient(c -> c.gui.setScreen(null));
        }
    }

    /** A client-side crucible menu of {@code tier} with a barrel attached and a few items, opened and rendered. */
    private static void crucible(ClientGameTestContext context, CrucibleTier tier) {
        crucible(context, tier, true);
    }

    private static void crucible(ClientGameTestContext context, CrucibleTier tier, boolean barrel) {
        context.runOnClient(c -> {
            Inventory inv = c.player.getInventory();
            CrucibleMenu menu = CrucibleMenu.client(tier, 0, inv);
            menu.setData(tier.slots() + 3, barrel ? 1 : 0);
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
