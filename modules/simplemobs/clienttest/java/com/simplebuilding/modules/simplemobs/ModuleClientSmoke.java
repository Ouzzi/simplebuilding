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
                var at = p.blockPosition().offset(0, 0, 3);
                level.setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var d = MobsRegistry.DECEIVER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (d == null) throw new AssertionError("Deceiver did not spawn");
                d.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                d.setYBodyRot(180f);
                d.setYHeadRot(180f);
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
            // Hollow Howler: idle, jaws open, shriek, emerging
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                level.getEntities(MobsRegistry.DECEIVER, e -> true).forEach(e -> e.discard());
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, 5);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(at.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.SCULK.defaultBlockState());
                var h = MobsRegistry.HOLLOW_HOWLER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (h == null) throw new AssertionError("Hollow Howler did not spawn");
                h.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                h.setYBodyRot(180f);
                h.setYHeadRot(180f);
                h.setNoAi(true);
                level.addFreshEntity(h);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-howler-idle");
            world.getServer().runOnServer(server -> {
                var h = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst();
                h.onHeard(h.blockPosition(), null, null);
            });
            context.waitTicks(3);
            context.takeScreenshot("simplemobs-howler-listen");
            world.getServer().runOnServer(server -> server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst().holdJawsOpen());
            context.waitTicks(10);
            context.takeScreenshot("simplemobs-howler-bite");
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                var h = level.getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst();
                h.startBite();
                h.shriek(level, HowlerLogic.Phase.ONE);
            });
            context.waitTicks(3);
            context.takeScreenshot("simplemobs-howler-shriek");
            world.getServer().runOnServer(server -> server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst().startEmerging());
            context.waitTicks(16);
            context.takeScreenshot("simplemobs-howler-emerge");
        }
    }
}
