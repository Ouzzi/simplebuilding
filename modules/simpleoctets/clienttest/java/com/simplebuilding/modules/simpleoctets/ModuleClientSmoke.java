package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.block.OctetCellBlock;
import com.simpleoctets.block.OctetCells;
import com.simpleoctets.registry.OctetRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Client smoke: the module boots; a row of wood octets, a melon octet and a mixed cell (block entity renderer). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleoctets")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleoctets-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var l = p.level();
                var at = p.blockPosition().offset(-3, 0, -4);
                int[] masks = {0x01, 0x0F, 0x33, 0x7F, 0xFF};
                for (int i = 0; i < Math.min(5, OctetRegistry.WOOD_OCTETS.size()); i++) {
                    l.setBlockAndUpdate(at.east(i), OctetCellBlock.withMask(OctetRegistry.WOOD_OCTETS.get(i).defaultBlockState(), masks[i]));
                }
                l.setBlockAndUpdate(at.east(6), OctetCellBlock.withMask(OctetRegistry.MELON_OCTET.defaultBlockState(), 0x7F));
                BlockState[] parts = new BlockState[8];
                Block oak = com.simplelib.api.OctetCells.cellFor(Blocks.OAK_PLANKS);
                Block stone = com.simplelib.api.OctetCells.cellFor(Blocks.STONE);
                for (int i = 0; i < 8; i++) parts[i] = OctetCells.material((OctetCellBlock.y(i) == 0 ? stone : oak).defaultBlockState());
                OctetCells.set(l, at.east(8), parts, false, Block.UPDATE_ALL);
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(at.east(4)));
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("simpleoctets-octets-row");
        }
    }
}
