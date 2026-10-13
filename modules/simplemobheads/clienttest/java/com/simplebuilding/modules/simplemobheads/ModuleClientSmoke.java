package com.simplebuilding.modules.simplemobheads;

import com.simplemobheads.MobHeads;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

/**
 * Client screenshots of every Simple Mobheads look (owner: review all heads): rows of eight standing heads (every
 * variant), every base head on a wall and worn by an armor stand, and every look as an item in the inventory.
 * Screenshots land in integration/run-fabric-263/screenshots as simplemobheads-*.png.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    private static final int PER_ROW = 6;

    /** One look to show: the head and the variant ('' = base look). */
    private record Look(HeadDef head, String variant) {
        ItemStack stack() {
            return MobHeads.stack(head, variant.isEmpty() ? null : variant);
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemobheads")) {
            throw new AssertionError("Module did not boot");
        }
        List<Look> looks = new ArrayList<>();
        List<Look> bases = new ArrayList<>();
        for (HeadDef head : HeadTable.HEADS) {
            looks.add(new Look(head, ""));
            bases.add(new Look(head, ""));
            for (HeadDef.Look look : head.looks().subList(1, head.looks().size())) {
                looks.add(new Look(head, look.variant()));
            }
        }
        context.takeScreenshot("simplemobheads-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
                world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var source = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source, "time set noon");
                server.getCommands().performPrefixedCommand(source, "weather clear");
            });
            for (int row = 0; row * PER_ROW < looks.size(); row++) {
                List<Look> slice = looks.subList(row * PER_ROW, Math.min(looks.size(), (row + 1) * PER_ROW));
                final int r = row;
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var level = (ServerLevel) player.level();
                    BlockPos origin = stage(level, new BlockPos(0, player.blockPosition().getY(), 40 * (r + 1)), false);
                    for (int i = 0; i < slice.size(); i++) {
                        place(level, origin.offset(3 - i, 0, 2), slice.get(i), false);
                    }
                    player.connection.teleport(origin.getX() + 1.0, origin.getY() + 0.2, origin.getZ() - 1.6, 0.0F, 24.0F);
                });
                context.waitTicks(20);
                world.getConnection().waitForClientboundPackets();
                world.getConnection().waitForChunksRender();
                context.takeScreenshot(String.format("simplemobheads-row-%02d", row));
            }
            for (int part = 0; part * PER_ROW < bases.size(); part++) {
                List<Look> slice = bases.subList(part * PER_ROW, Math.min(bases.size(), (part + 1) * PER_ROW));
                final int p = part;
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var level = (ServerLevel) player.level();
                    BlockPos origin = stage(level, new BlockPos(200, player.blockPosition().getY(), 40 * (p + 1)), true);
                    for (int i = 0; i < slice.size(); i++) {
                        place(level, origin.offset(-7 + 2 * i, 1, 4), slice.get(i), true);
                        ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
                        stand.snapTo(origin.getX() - 7 + 2 * i + 0.5, origin.getY(), origin.getZ() + 1.5, 180.0F, 0.0F);
                        stand.setItemSlot(EquipmentSlot.HEAD, slice.get(i).stack());
                        level.addFreshEntity(stand);
                    }
                    player.connection.teleport(origin.getX() + 0.5, origin.getY() + 0.4, origin.getZ() - 3.0, 0.0F, 8.0F);
                });
                context.waitTicks(20);
                world.getConnection().waitForClientboundPackets();
                world.getConnection().waitForChunksRender();
                context.takeScreenshot(String.format("simplemobheads-worn-%02d", part));
            }
            for (int page = 0; page * 36 < looks.size(); page++) {
                List<Look> slice = looks.subList(page * 36, Math.min(looks.size(), (page + 1) * 36));
                context.runOnClient(client -> {
                    var inventory = client.player.getInventory();
                    for (int slot = 0; slot < 36; slot++) {
                        inventory.setItem(slot, slot < slice.size() ? slice.get(slot).stack() : ItemStack.EMPTY);
                    }
                    client.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
                });
                context.waitTicks(5);
                context.takeScreenshot(String.format("simplemobheads-items-%02d", page));
            }
            context.runOnClient(client -> client.gui.setScreen(null));
        }
    }

    /** A clean stone floor (and a wall row behind for wall heads) around {@code origin}. */
    private static BlockPos stage(ServerLevel level, BlockPos origin, boolean wall) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -6; z <= 6; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 4; y++) {
                    boolean back = wall && z == 5 && y == 1;
                    level.setBlockAndUpdate(origin.offset(x, y, z), back ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        return origin;
    }

    /** Places the look as a standing head facing the camera (rotation 0 faces north, towards a player looking south) or as a wall head on the wall row, with its variant. */
    private static void place(ServerLevel level, BlockPos pos, Look look, boolean wall) {
        var block = wall ? MobHeads.wall(look.head().id()) : MobHeads.standing(look.head().id());
        var state = wall ? block.defaultBlockState().setValue(WallSkullBlock.FACING, Direction.NORTH)
                : block.defaultBlockState().setValue(SkullBlock.ROTATION, 0);
        level.setBlockAndUpdate(pos, state);
        if (level.getBlockEntity(pos) instanceof SkullBlockEntity skull) {
            skull.applyComponentsFromItemStack(look.stack());
            skull.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
}
