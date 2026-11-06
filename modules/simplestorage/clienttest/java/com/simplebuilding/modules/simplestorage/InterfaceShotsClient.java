package com.simplebuilding.modules.simplestorage;

import com.simplestorage.network.CableBlock;
import com.simplestorage.network.InterfaceBlock;
import com.simplestorage.pipe.ItemPipeBlock;
import com.simplestorage.registry.StorageBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * DW03C (b-storage05, docs/ai/PLAN-B-STORAGE05-2026-11-03.md): pictures of the chest interface on chests and barrels, the Simple
 * Crucibles barrel lying and open, and pipes without stubs, 1920x1080 without the HUD. DW04C-1 (b-storage06): the filled corner. Only Vanilla calls and calls that existed
 * before the change, so the same scene photographs the old and the new state (the preview compares both).
 */
public final class InterfaceShotsClient implements FabricClientGameTest {
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
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
            context.waitTicks(20);
            // (literal names: the test runner reads them from the source)
            look(context, world, 3.2, 1.6, 3.2, 0.5, 2.85, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-floating-chest").withSize(1920, 1080));
            look(context, world, 6.5, -0.45, 3.6, 6.5, -0.15, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-chest-pit").withSize(1920, 1080));
            look(context, world, 14.6, 1.4, 3.4, 12.5, 0.3, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-chest-front-side").withSize(1920, 1080));
            look(context, world, 15.0, 1.5, -3.0, 17.5, 0.5, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-chest-back-open").withSize(1920, 1080));
            look(context, world, 25.8, 1.6, 3.6, 22.5, 0.4, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-barrels").withSize(1920, 1080));
            look(context, world, 28.5, 2.4, 3.6, 28.5, 1.2, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw03c-pipes").withSize(1920, 1080));
            look(context, world, 31.3, 0.5, -4.7, 33.0, 2.9, -3.0);
            context.takeScreenshot(TestScreenshotOptions.of("simplestorage-dw04c-corner").withSize(1920, 1080));
            context.runOnClient(c -> {
                if (c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
        }
    }

    private static void run(net.minecraft.server.MinecraftServer server, String command) {
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

    private static void plate(ServerLevel l, BlockPos pos, Direction toChest) {
        set(l, pos, InterfaceBlock.withArms(StorageBlocks.CHEST_INTERFACE.defaultBlockState().setValue(InterfaceBlock.FACING, toChest), l, pos));
    }

    private static void cable(ServerLevel l, BlockPos pos) {
        set(l, pos, CableBlock.withArms(StorageBlocks.COPPER_CABLE.defaultBlockState(), l, pos));
    }

    private static void pipe(ServerLevel l, BlockPos pos, Direction out) {
        set(l, pos, StorageBlocks.PIPES.get(com.simplestorage.pipe.PipeTier.BASIC).defaultBlockState().setValue(ItemPipeBlock.FACING, out));
    }

    private static BlockState chest(Direction front) {
        return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState state, String name, String value) {
        Property property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) return state;
        var parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, (Comparable) parsed.get()) : state;
    }

    private static void build(ServerLevel l, BlockPos b) {
        for (int x = -4; x <= 34; x++) {
            for (int z = -6; z <= 7; z++) {
                for (int y = -3; y <= 6; y++) set(l, b.offset(x, y, z), y < 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        // 1: a floating chest, the interface under it, nothing around the interface (flush with the chest).
        BlockPos c1 = b.offset(0, 3, 0);
        set(l, c1, chest(Direction.SOUTH));
        plate(l, c1.below(), Direction.UP);
        cable(l, c1.below(2));
        // 2: the owner's picture 3: a chest on the floor, its interface in a pit under it (stone on three sides, open to the camera).
        BlockPos c2 = b.offset(6, 0, 0);
        set(l, c2, chest(Direction.SOUTH));
        for (int z = 1; z <= 4; z++) {
            set(l, c2.below().south(z), Blocks.AIR.defaultBlockState());
            set(l, c2.below(2).south(z), Blocks.AIR.defaultBlockState());
        }
        cable(l, c2.below(2));
        plate(l, c2.below(), Direction.UP);
        // 3: a chest on the floor with interfaces in front, at the side and behind.
        BlockPos c3 = b.offset(12, 0, 0);
        set(l, c3, chest(Direction.SOUTH));
        plate(l, c3.south(), Direction.NORTH);
        plate(l, c3.east(), Direction.WEST);
        plate(l, c3.north(), Direction.SOUTH);
        cable(l, c3.east().east());
        // 4: the same, opened, seen from behind (the lid swings over the back interface).
        BlockPos c4 = b.offset(17, 0, 0);
        set(l, c4, chest(Direction.SOUTH));
        plate(l, c4.north(), Direction.SOUTH);
        plate(l, c4.west(), Direction.EAST);
        cable(l, c4.north().above());
        l.blockEvent(c4, Blocks.CHEST, 1, 1);
        // 5: a Vanilla barrel and a Simple Storage copper barrel (lying, open), both with an interface.
        BlockPos f = b.offset(21, 0, 0);
        set(l, f, Blocks.BARREL.defaultBlockState());
        plate(l, f.south(), Direction.NORTH);
        BlockPos g = b.offset(23, 0, 0);
        Identifier copper = Identifier.fromNamespaceAndPath("simplestorage", "copper_barrel");
        if (BuiltInRegistries.BLOCK.containsKey(copper)) {
            BlockState barrel = with(with(BuiltInRegistries.BLOCK.getValue(copper).defaultBlockState(), "top", "south"), "open", "true");
            set(l, g, barrel);
            plate(l, g.east(), Direction.WEST);
            set(l, g.above(), with(BuiltInRegistries.BLOCK.getValue(copper).defaultBlockState(), "top", "east"));
        }
        // 6: pipes. A chest gives into the first pipe, whose output points down into nothing; beside it two pipes that do not feed
        // each other (both down), then one that pushes east into the last, which pushes down into a chest.
        BlockPos p = b.offset(26, 1, 0);
        set(l, p.west(), chest(Direction.SOUTH));
        set(l, p.west().below(), Blocks.SMOOTH_STONE.defaultBlockState());
        pipe(l, p, Direction.DOWN);
        pipe(l, p.east(), Direction.DOWN);
        pipe(l, p.east(2), Direction.EAST);
        pipe(l, p.east(3), Direction.DOWN);
        set(l, p.east(3).below(), chest(Direction.SOUTH));
        for (int i = 0; i <= 3; i++) {
            BlockPos at = p.east(i);
            set(l, at, ItemPipeBlock.withArms(l.getBlockState(at), l, at));
        }
        // 7 (DW04C-1, owner's picture 6): a raised chest, its interface under it between two walls (east and south); both edge
        // strips grow to the walls and meet in the corner, seen from below.
        BlockPos c7 = b.offset(32, 3, -4);
        set(l, c7, chest(Direction.SOUTH));
        set(l, c7.below().east(), Blocks.SMOOTH_STONE.defaultBlockState());
        set(l, c7.below().south(), Blocks.SMOOTH_STONE.defaultBlockState());
        set(l, c7.east(), Blocks.SMOOTH_STONE.defaultBlockState());
        set(l, c7.south(), Blocks.SMOOTH_STONE.defaultBlockState());
        plate(l, c7.below(), Direction.UP);
    }
}
