package com.simplebuilding.modules.simplemobs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerLevel;

/** Boots the module, then shows the Deceiver in idle and in every action pose (screenshots). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemobs")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplemobs-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var level = (ServerLevel) p.level();
                var at = p.blockPosition().offset(0, 0, 2);
                level.setBlockAndUpdate(at.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var d = MobsRegistry.DECEIVER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (d == null) throw new AssertionError("Deceiver did not spawn");
                d.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                p.setXRot(22f);
                d.setNoAi(true);
                level.addFreshEntity(d);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-deceiver-idle");
            for (var pose : new Object[][] {{"simplemobs-deceiver-summon", DeceiverEntity.ACT_SUMMON, 22}, {"simplemobs-deceiver-teleport", DeceiverEntity.ACT_TELEPORT, 8},
                    {"simplemobs-deceiver-drink", DeceiverEntity.ACT_DRINK, 16}, {"simplemobs-deceiver-unmask", DeceiverEntity.ACT_UNMASK, 4}}) {
                world.getServer().runOnServer(server -> {
                    var d = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.DECEIVER, e -> true).getFirst();
                    d.playAction((Integer) pose[1], 200);
                });
                context.waitTicks((Integer) pose[2]);
                context.takeScreenshot((String) pose[0]);
            }
            world.getServer().runOnServer(server -> {
                var d = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.DECEIVER, e -> true).getFirst();
                d.playAction(DeceiverEntity.ACT_NONE, 0);
                d.setArmored(true);
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-deceiver-armored");
        }
    }
}
