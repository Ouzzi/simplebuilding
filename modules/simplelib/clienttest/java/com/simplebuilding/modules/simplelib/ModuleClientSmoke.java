package com.simplebuilding.modules.simplelib;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
/** Client smoke: the module boots (the crucible screenshots moved to Simple Crucibles with the crucibles, split stage 5). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplelib")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplelib-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            // N71: the four growth stages of the warped wart on soul sand, in a row, seen from the front.
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var row = p.blockPosition().offset(-1, 0, 3);
                for (int age = 0; age < 4; age++) {
                    l.setBlockAndUpdate(row.east(age).below(), net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState());
                    l.setBlockAndUpdate(row.east(age), com.simplelib.registry.LibBlocks.WARPED_WART.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.NetherWartBlock.AGE, age));
                }
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(row.east(1)).add(1.0, -0.3, 0));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simplelib-warped-wart-stages");
            context.takeScreenshot("simplelib-world");
        }
    }
}
