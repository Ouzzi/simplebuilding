package com.simplebuilding.modules.simplemobs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Fear mobs on screen: the Dread figure (iron T, anvil, skeleton skull), the helmet put on and the Dread rising, idle, swinging and stomping; the Hollow Watcher idle, reaching, screaming, frozen; the items. */
public final class FearClientSmoke implements FabricClientGameTest {
    private static ServerLevel level(MinecraftServer server) { return server.getLevel(Level.OVERWORLD); }

    private static BlockPos front(MinecraftServer server, int dz) {
        return server.getPlayerList().getPlayers().getFirst().blockPosition().offset(0, 0, dz);
    }

    private static void clear(MinecraftServer server) {
        level(server).getEntities(FearMobs.DREAD, e -> true).forEach(e -> e.discard());
        level(server).getEntities(FearMobs.WATCHER, e -> true).forEach(e -> e.discard());
        level(server).getEntities(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, e -> true).forEach(e -> e.discard());
    }

    private static <T extends Mob> T place(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, BlockPos at) {
        T m = type.create(level, EntitySpawnReason.COMMAND);
        if (m == null) throw new AssertionError(type + " did not spawn");
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
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
            // the summoning figure, then the skull placed through the Wither's check: the Dread rises
            world.getServer().runOnServer(server -> {
                server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
                var l = level(server);
                var at = front(server, 6);
                for (int dx = -2; dx <= 2; dx++) l.setBlockAndUpdate(at.offset(dx, -1, 0), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                l.setBlockAndUpdate(at, Blocks.IRON_BLOCK.defaultBlockState());
                l.setBlockAndUpdate(at.above(), Blocks.ANVIL.defaultBlockState());
                l.setBlockAndUpdate(at.offset(-1, 1, 0), Blocks.IRON_BLOCK.defaultBlockState());
                l.setBlockAndUpdate(at.offset(1, 1, 0), Blocks.IRON_BLOCK.defaultBlockState());
                l.setBlockAndUpdate(at.above(2), Blocks.SKELETON_SKULL.defaultBlockState());
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-fear-dread-figure");
            world.getServer().runOnServer(server -> {
                var l = level(server);
                var head = front(server, 6).above(2);
                var p = server.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(head), net.minecraft.core.Direction.SOUTH, head, false);
                net.minecraft.world.item.Items.IRON_HELMET.useOn(new net.minecraft.world.item.context.UseOnContext(p, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                if (l.getEntities(FearMobs.DREAD, e -> true).isEmpty()) throw new AssertionError("the figure did not summon a Dread");
            });
            context.waitTicks(25);
            context.takeScreenshot("simplemobs-fear-dread-rising");
            world.getServer().runOnServer(server -> {
                clear(server);
                place(level(server), FearMobs.DREAD, front(server, 5));
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-fear-dread-idle");
            world.getServer().runOnServer(server -> level(server).getEntities(FearMobs.DREAD, e -> true).getFirst().playAction(DreadEntity.ACT_SWING));
            context.waitTicks(14);
            context.takeScreenshot("simplemobs-fear-dread-swing");
            world.getServer().runOnServer(server -> level(server).getEntities(FearMobs.DREAD, e -> true).getFirst().playAction(DreadEntity.ACT_STOMP));
            context.waitTicks(22);
            context.takeScreenshot("simplemobs-fear-dread-stomp");
            // Hollow Watcher: idle, reaching (grip), screaming, frozen by a spyglass
            world.getServer().runOnServer(server -> {
                clear(server);
                var l = level(server);
                var at = front(server, 7);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) l.setBlockAndUpdate(at.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState());
                place(l, FearMobs.WATCHER, at);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-fear-watcher-idle");
            for (var pose : new Object[][] {{"simplemobs-fear-watcher-grip", HollowWatcherEntity.ACT_GRIP_WINDUP}, {"simplemobs-fear-watcher-scream", HollowWatcherEntity.ACT_SCREAM}}) {
                world.getServer().runOnServer(server -> level(server).getEntities(FearMobs.WATCHER, e -> true).getFirst().playAction((Integer) pose[1], 400));
                context.waitTicks(6);
                context.takeScreenshot((String) pose[0]);
            }
            world.getServer().runOnServer(server -> {
                var w = level(server).getEntities(FearMobs.WATCHER, e -> true).getFirst();
                w.playAction(HollowWatcherEntity.ACT_NONE, 0);
                w.freeze();
            });
            context.waitTicks(10);
            context.takeScreenshot("simplemobs-fear-watcher-frozen");
            // items on armor stands' hands: link, amulet, eye
            world.getServer().runOnServer(server -> {
                clear(server);
                var l = level(server);
                var at = front(server, 3);
                ItemStack[] items = {new ItemStack(FearMobs.LINK), new ItemStack(FearMobs.AMULET), new ItemStack(FearMobs.EYE)};
                for (int i = 0; i < items.length; i++) {
                    ArmorStand stand = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(l, EntitySpawnReason.COMMAND);
                    stand.snapTo(at.getX() + 0.5 + (i - 1) * 1.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
                    stand.setItemSlot(EquipmentSlot.MAINHAND, items[i]);
                    stand.setShowArms(true);
                    l.addFreshEntity(stand);
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-fear-items");
        }
    }
}
