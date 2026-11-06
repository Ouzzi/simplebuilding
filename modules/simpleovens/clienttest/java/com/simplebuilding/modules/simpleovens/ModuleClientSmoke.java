package com.simplebuilding.modules.simpleovens;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
/** Client smoke: the module boots; the window of a netherite furnace. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleovens")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleovens-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, -3);
                p.level().setBlockAndUpdate(at, com.simpleovens.OvensRegistry.FURNACES.get(com.simpleovens.OvenTier.NETHERITE).defaultBlockState());
                if (p.level().getBlockEntity(at) instanceof net.minecraft.world.MenuProvider mp) p.openMenu(mp);
                else throw new AssertionError("Netherite furnace has no menu provider");
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(10);
            context.takeScreenshot("simpleovens-furnace-window");
        }
    }
}
