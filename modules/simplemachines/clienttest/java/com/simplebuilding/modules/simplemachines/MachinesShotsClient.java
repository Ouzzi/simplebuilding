package com.simplebuilding.modules.simplemachines;

import com.simplemachines.MachinesRegistry;
import com.simplemachines.piston.DoublePistons;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * DW04C-6 (b-mach05, docs/ai/PLAN-B-MACH05-2026-11-04.md): pictures of the Double Piston - the item in the inventory next to
 * Vanilla's piston, all six facings at stage 2 next to an extended Vanilla piston, and one piston photographed mid-move with the
 * world frozen ({@code tick freeze} / {@code tick step}). Only calls that existed before the change, so the same scene shows the
 * old and the new state.
 */
public final class MachinesShotsClient implements FabricClientGameTest {
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            // 1: the item next to Vanilla's piston in the survival inventory (owner's picture 7: the plate lay on the side).
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                run(server, "gamemode survival @a");
                p.getInventory().setItem(0, new ItemStack(Items.PISTON));
                p.getInventory().setItem(1, new ItemStack(MachinesRegistry.DOUBLE_PISTON_ITEM));
                p.getInventory().setItem(2, new ItemStack(Items.STICKY_PISTON));
                p.getInventory().setItem(9, new ItemStack(Items.PISTON));
                p.getInventory().setItem(10, new ItemStack(MachinesRegistry.DOUBLE_PISTON_ITEM));
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
            context.waitTicks(5);
            context.takeScreenshot("simplemachines-mach05-inventory");
            context.runOnClient(c -> c.setScreenAndShow(null));
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
            // 2: stage 2 in all six facings, a Vanilla piston out for comparison.
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel l = (ServerLevel) p.level();
                base = p.blockPosition().offset(0, 0, 8);
                run(server, "gamemode spectator @a");
                run(server, "time set noon");
                run(server, "weather clear");
                run(server, "gamerule advance_time false");
                build(l, base);
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            // (literal names: the test runner reads them from the source)
            look(context, world, 13.5, 7.0, 8.0, 13.5, 1.0, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-facings").withSize(1920, 1080));
            look(context, world, 13.5, 7.0, -9.0, 13.5, 1.0, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-facings-back").withSize(1920, 1080));
            // 3: one east-facing piston, the world frozen, stepped tick by tick; a picture in the middle of every move.
            BlockPos a = base.offset(4, 0, 10);
            look(context, world, 5.0, 1.9, 14.0, 5.0, 0.5, 10.5);
            world.getServer().runOnServer(server -> run(server, "tick freeze"));
            world.getServer().runOnServer(server -> ((ServerLevel) server.getPlayerList().getPlayers().getFirst().level())
                    .setBlock(a.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL));
            context.waitTicks(5);
            stepUntil(context, world, c -> moving(c, a.east()));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move1-0to1").withSize(1920, 1080));
            stepUntil(context, world, c -> ext(c, a) == 1 && !moving(c, a.east()));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move2-stage1").withSize(1920, 1080));
            stepUntil(context, world, c -> moving(c, a.east(2)));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move3-1to2").withSize(1920, 1080));
            stepUntil(context, world, c -> ext(c, a) == 2 && !moving(c, a.east(2)));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move4-stage2").withSize(1920, 1080));
            world.getServer().runOnServer(server -> ((ServerLevel) server.getPlayerList().getPlayers().getFirst().level())
                    .setBlock(a.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            context.waitTicks(5);
            stepUntil(context, world, c -> moving(c, a.east()));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move5-2to1").withSize(1920, 1080));
            stepUntil(context, world, c -> ext(c, a) == 1 && !moving(c, a.east()));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move6-stage1").withSize(1920, 1080));
            stepUntil(context, world, c -> moving(c, a) || moving(c, a.east()));
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move7-1to0").withSize(1920, 1080));
            stepUntil(context, world, c -> ext(c, a) == 0);
            context.takeScreenshot(TestScreenshotOptions.of("simplemachines-mach05-move8-stage0").withSize(1920, 1080));
            world.getServer().runOnServer(server -> run(server, "tick unfreeze"));
            context.runOnClient(c -> {
                if (c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
        }
    }

    /** True while the client has a moving block at {@code pos} that is somewhere between start and end. */
    private static boolean moving(Minecraft c, BlockPos pos) {
        return c.level.getBlockEntity(pos) instanceof PistonMovingBlockEntity m && m.getProgress(1.0F) > 0.0F && m.getProgress(1.0F) < 1.0F;
    }

    private static int ext(Minecraft c, BlockPos pos) {
        BlockState s = c.level.getBlockState(pos);
        return s.is(MachinesRegistry.DOUBLE_PISTON) ? s.getValue(DoublePistons.EXT) : -1;
    }

    /** One game tick at a time (frozen world) until the client shows the wanted state; at most 10 ticks (the old code never shows a move). */
    private static void stepUntil(ClientGameTestContext context, TestSingleplayerContext world, Predicate<Minecraft> wanted) {
        for (int i = 0; i < 10; i++) {
            if (context.computeOnClient(wanted::test)) break;
            world.getServer().runOnServer(server -> run(server, "tick step 1"));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(4);
        }
        System.out.println("[mach05] step: " + context.computeOnClient(wanted::test));
        world.getConnection().waitForChunksRender();
        context.waitTicks(2);
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Eye at base + (ex, ey, ez), looking at base + (tx, ty, tz). */
    private static void look(ClientGameTestContext context, TestSingleplayerContext world, double ex, double ey, double ez, double tx, double ty, double tz) {
        world.getServer().runOnServer(server -> run(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                base.getX() + ex, base.getY() + ey - 1.62, base.getZ() + ez, base.getX() + tx, base.getY() + ty, base.getZ() + tz)));
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(15);
    }

    private static void set(ServerLevel l, BlockPos pos, BlockState state) {
        l.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static BlockState piston(Direction f) {
        return MachinesRegistry.DOUBLE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, f);
    }

    private static void build(ServerLevel l, BlockPos b) {
        for (int x = -3; x <= 30; x++) {
            for (int z = -10; z <= 14; z++) {
                for (int y = -3; y <= 7; y++) set(l, b.offset(x, y, z), y < 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        // up, power below (in the floor)
        set(l, b.offset(0, -1, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(0, 0, 0), piston(Direction.UP));
        // down, power above
        set(l, b.offset(4, 4, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(4, 3, 0), piston(Direction.DOWN));
        // north (away from the front camera)
        set(l, b.offset(8, 0, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(8, 0, 0), piston(Direction.NORTH));
        // south (towards the front camera)
        set(l, b.offset(12, 0, -1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(12, 0, 0), piston(Direction.SOUTH));
        // east and west, facing each other with one cell between the heads
        set(l, b.offset(15, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(16, 0, 0), piston(Direction.EAST));
        set(l, b.offset(23, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(22, 0, 0), piston(Direction.WEST));
        // Vanilla piston out, for comparison
        set(l, b.offset(26, 0, 1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        set(l, b.offset(26, 0, 0), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH));
        // the animation piston (east, power placed later at its west side), behind the front camera
        set(l, b.offset(4, 0, 10), piston(Direction.EAST));
    }
}
