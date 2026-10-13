package com.simplebuilding.modules.simplemobs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Blighted Wither and Biome Brush on screen: the Netherite figure, the Blighted Wither next to a Vanilla Wither, the nova warning, the brush in hand before and after a stroke, the items. */
public final class WitherClientSmoke implements FabricClientGameTest {
    private static ServerLevel level(MinecraftServer server) { return server.getLevel(Level.OVERWORLD); }

    private static BlockPos front(MinecraftServer server, int dz) {
        return server.getPlayerList().getPlayers().getFirst().blockPosition().offset(0, 0, dz);
    }

    private static void clear(MinecraftServer server) {
        level(server).getEntities(WitherMobs.BLIGHTED_WITHER, e -> true).forEach(e -> e.discard());
        level(server).getEntities(EntityTypes.WITHER, e -> true).forEach(e -> e.discard());
        level(server).getEntities(EntityTypes.ARMOR_STAND, e -> true).forEach(e -> e.discard());
    }

    private static <T extends net.minecraft.world.entity.Mob> T place(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z) {
        T m = type.create(level, EntitySpawnReason.COMMAND);
        if (m == null) throw new AssertionError(type + " did not spawn");
        m.snapTo(x, y, z, 180f, 0f);
        m.setYBodyRot(180f);
        m.setYHeadRot(180f);
        m.setNoAi(true);
        level.addFreshEntity(m);
        return m;
    }

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
                var l = level(server);
                var at = front(server, 7);
                l.setBlockAndUpdate(at.below(), Blocks.NETHERITE_BLOCK.defaultBlockState());
                l.setBlockAndUpdate(at, Blocks.SOUL_SAND.defaultBlockState());
                for (int dx = -1; dx <= 1; dx++) {
                    l.setBlockAndUpdate(at.offset(dx, 1, 0), Blocks.SOUL_SOIL.defaultBlockState());
                    l.setBlockAndUpdate(at.offset(dx, 2, 0), Blocks.WITHER_SKELETON_SKULL.defaultBlockState());
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-wither-figure");
            world.getServer().runOnServer(server -> {
                var l = level(server);
                l.setBlockAndUpdate(front(server, 7).above(2), Blocks.AIR.defaultBlockState());
                l.setBlockAndUpdate(front(server, 7).above(2), Blocks.WITHER_SKELETON_SKULL.defaultBlockState());
                net.minecraft.world.level.block.WitherSkullBlock.checkSpawn(l, front(server, 7).above(2));
                if (l.getEntities(WitherMobs.BLIGHTED_WITHER, e -> true).isEmpty()) throw new AssertionError("no Blighted Wither from the Netherite figure");
                l.getEntities(WitherMobs.BLIGHTED_WITHER, e -> true).forEach(w -> w.setNoAi(true));
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-wither-waking");
            world.getServer().runOnServer(server -> {
                clear(server);
                var at = front(server, 9);
                var blighted = place(level(server), WitherMobs.BLIGHTED_WITHER, at.getX() - 1.5, at.getY() + 1, at.getZ() + 0.5);
                blighted.setInvulnerableTicks(0);
                var vanilla = place(level(server), EntityTypes.WITHER, at.getX() + 2.5, at.getY() + 1, at.getZ() + 0.5);
                vanilla.setInvulnerableTicks(0);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-wither-blighted-vs-vanilla");
            world.getServer().runOnServer(server -> {
                clear(server);
                var at = front(server, 12);
                var w = place(level(server), WitherMobs.BLIGHTED_WITHER, at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5);
                w.setInvulnerableTicks(0);
                w.setHealth(60);
                w.announce(level(server), BlightedWither.Attack.NOVA);
                for (int i = 0; i < 12; i++) w.tickAbilities(level(server));
            });
            context.waitTicks(4);
            context.takeScreenshot("simplemobs-wither-nova-warning");
            // Biome Brush: grass field, brush in the main hand, snow in the off hand, before and after a stroke
            world.getServer().runOnServer(server -> {
                clear(server);
                var l = level(server);
                var at = front(server, 5).below();
                for (int dx = -8; dx <= 8; dx++) for (int dz = -6; dz <= 6; dz++) l.setBlockAndUpdate(at.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                var p = server.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WitherMobs.BRUSH));
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SAND, 16));
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-wither-brush-before");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var r = BiomeBrushItem.stroke(level(server), p, front(server, 5).below(), p.getMainHandItem(), p.getOffhandItem());
                if (r != BiomeBrush.Result.PAINTED) throw new AssertionError("stroke: " + r);
            });
            context.waitTicks(40);
            context.takeScreenshot("simplemobs-wither-brush-after");
            world.getServer().runOnServer(server -> {
                var l = level(server);
                var at = front(server, 3);
                ItemStack[] items = {new ItemStack(WitherMobs.HEART), new ItemStack(WitherMobs.BRUSH)};
                for (int i = 0; i < items.length; i++) {
                    ArmorStand stand = EntityTypes.ARMOR_STAND.create(l, EntitySpawnReason.COMMAND);
                    stand.snapTo(at.getX() + 0.5 + (i - 0.5) * 1.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
                    stand.setItemSlot(EquipmentSlot.MAINHAND, items[i]);
                    stand.setShowArms(true);
                    l.addFreshEntity(stand);
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-wither-items");
        }
    }
}
