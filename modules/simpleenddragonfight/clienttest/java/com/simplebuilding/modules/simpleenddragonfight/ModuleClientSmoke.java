package com.simplebuilding.modules.simpleenddragonfight;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Boots the module, shows a Dragonmite next to a Vanilla Endermite, then the growl and spark steps of the entry animation. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleenddragonfight")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleenddragonfight-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var level = (ServerLevel) p.level();
                var base = p.blockPosition().offset(0, 0, 3);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState());
                place(level, DragonFightRegistry.DRAGONMITE.create(level, EntitySpawnReason.COMMAND), base.getX() - 0.2, base.getY(), base.getZ() + 0.5);
                place(level, net.minecraft.world.entity.EntityTypes.ENDERMITE.create(level, EntitySpawnReason.COMMAND), base.getX() + 1.4, base.getY(), base.getZ() + 0.5);
            });
            context.waitTicks(30);
            context.takeScreenshot("simpleenddragonfight-dragonmite");
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(Level.OVERWORLD);
                level.getEntities(DragonFightRegistry.DRAGONMITE, e -> true).forEach(e -> e.discard());
                level.getEntities(net.minecraft.world.entity.EntityTypes.ENDERMITE, e -> true).forEach(e -> e.discard());
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 6, 18);
                EnderDragon d = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(level, EntitySpawnReason.COMMAND);
                if (d == null) throw new AssertionError("dragon did not spawn");
                d.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                d.setNoAi(true);
                level.addFreshEntity(d);
            });
            context.waitTicks(20);
            for (int step = 35; step < 60; step++) {
                int s = step;
                world.getServer().runOnServer(server -> {
                    var level = server.getLevel(Level.OVERWORLD);
                    var p = server.getPlayerList().getPlayers().getFirst();
                    var d = level.getEntities(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, e -> true).getFirst();
                    FightSession.playStep(level, d, p, s, 60, 0);
                });
                context.waitTicks(1);
                if (step == 42) context.takeScreenshot("simpleenddragonfight-growl");
            }
            context.takeScreenshot("simpleenddragonfight-sparks");
        }
    }

    private static void place(ServerLevel level, Mob mob, double x, double y, double z) {
        if (mob == null) throw new AssertionError("mob did not spawn");
        mob.snapTo(x, y, z, 180f, 0f);
        mob.setYBodyRot(180f);
        mob.setYHeadRot(180f);
        mob.setNoAi(true);
        level.addFreshEntity(mob);
    }
}
