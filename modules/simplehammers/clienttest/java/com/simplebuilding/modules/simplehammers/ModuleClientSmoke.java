package com.simplebuilding.modules.simplehammers;

import com.simplehammers.HammerTiers;
import com.simplehammers.registry.HammerRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Client smoke: the module boots; a diamond sledgehammer in hand in front of a stone wall (area outline with SimpleBuilding). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplehammers")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplehammers-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var at = p.blockPosition().offset(0, 0, -3);
                for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 3; dy++) l.setBlockAndUpdate(at.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HammerRegistry.hammer(HammerTiers.DIAMOND)));
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(at.above()));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simplehammers-in-hand");
        }
    }
}
