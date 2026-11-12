package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.block.OctetCellBlock;
import com.simpleoctets.block.OctetCells;
import com.simpleoctets.registry.OctetRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client smoke: the module boots; a row of wood octets, a melon octet and a mixed cell (block entity renderer). PLAN40 simpleoctets-5
 * adds, HUD hidden, spectator camera, each scene built fresh: the material groups in rows (stone with obsidian, bricks, quartz, the
 * "other" group with ice and bone, the metals including Simple Ores' while it is loaded), every cell 7/8 full (the front top corner
 * missing, so the sub-grid shows); mixed cells (halves, checker, eight materials, four ices, a partial one) in the open and under a
 * roof, by day and at night with lanterns at the open row (light on the block entity renderer); the sub-grid shapes in one material
 * (1, 2, 4 octets, stairs, half, pillar, checker both ways, 7/8, 8/8); melon octets next to Vanilla's melon (texture melon_flesh).
 * The aiming, placing and sneak-breaking by hand is the real-mouse probe's part ({@code tools/octets_probe.py}). Sounds (lead octets
 * damping) need an ear: owner check.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    /** Octet index {@code x | y << 1 | z << 2}: index 2 is the top front (north, towards the camera) west corner. */
    private static final int MISSING_FRONT_CORNER = OctetCellBlock.FULL & ~(1 << 2);
    private static BlockPos base;

    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleoctets")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleoctets-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
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
            world.getServer().runOnServer(server -> {
                base = server.getPlayerList().getPlayers().getFirst().blockPosition();
                run(server, "gamemode spectator @a");
                run(server, "time set noon");
                run(server, "gamerule advance_time false");
                run(server, "weather clear");
            });
            hud(context, false);
            materials(context, world);
            mixed(context, world);
            subGrid(context, world);
            melon(context, world);
            hud(context, true);
        }
    }

    /** One row per material group (16 per row at most, longer groups wrap), every cell 7/8 full. */
    private static void materials(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            ServerLevel l = server.overworld();
            int row = 0;
            for (String group : new String[]{"stone", "bricks", "quartz", "other", "metal"}) {
                List<Block> cells = OctetRegistry.materialOctets(group);
                for (int i = 0; i < cells.size(); i++) {
                    if (i > 0 && i % 16 == 0) row++;
                    l.setBlockAndUpdate(base.offset(7 - i % 16, 0, 5 + 2 * row), OctetCellBlock.withMask(cells.get(i).defaultBlockState(), MISSING_FRONT_CORNER));
                }
                System.out.println("[octets] group " + group + ": " + cells.size() + " octet cells");
                row++;
            }
        });
        settle(context, world);
        look(context, world, -0.5, 5.0, -3.0, -0.5, 0.0, 9.0);
        context.takeScreenshot("simpleoctets-materials-day");
        look(context, world, -0.5, 14.0, 4.0, -0.5, 0.0, 10.0);
        context.takeScreenshot("simpleoctets-materials-above");
    }

    /** Five mixed patterns in the open (front) and under a stone roof (back); day, then night with lanterns by the open row. */
    private static void mixed(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            ServerLevel l = server.overworld();
            fill(server, -7, 0, 9, -7, 2, 13, "minecraft:stone");
            fill(server, 7, 0, 9, 7, 2, 13, "minecraft:stone");
            fill(server, -7, 3, 9, 7, 3, 13, "minecraft:stone");
            fill(server, -7, 0, 13, 7, 2, 13, "minecraft:stone");
            List<BlockState[]> patterns = patterns();
            for (int i = 0; i < patterns.size(); i++) {
                OctetCells.set(l, base.offset(4 - 2 * i, 0, 6), patterns.get(i).clone(), false, Block.UPDATE_ALL);
                OctetCells.set(l, base.offset(4 - 2 * i, 0, 11), patterns.get(i).clone(), false, Block.UPDATE_ALL);
            }
            for (int x : new int[]{5, -5}) set(server, x, 0, 5, "minecraft:lantern");
        });
        settle(context, world);
        look(context, world, 0.0, 3.0, 0.0, 0.0, 0.5, 9.0);
        context.takeScreenshot("simpleoctets-mixed-day");
        world.getServer().runOnServer(server -> run(server, "time set midnight"));
        settle(context, world);
        context.takeScreenshot("simpleoctets-mixed-night");
        world.getServer().runOnServer(server -> run(server, "time set noon"));
        settle(context, world);
    }

    /** Halves (stone below, oak above), checker (stone / oak), eight materials, four ices, a partial cell of three materials. */
    private static List<BlockState[]> patterns() {
        BlockState stone = part(Blocks.STONE), oak = part(Blocks.OAK_PLANKS);
        Block[] eight = {Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.BRICKS, Blocks.QUARTZ_BLOCK, Blocks.GOLD_BLOCK, Blocks.OBSIDIAN};
        Block[] ices = {Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.BONE_BLOCK};
        List<BlockState[]> out = new ArrayList<>();
        BlockState[] halves = new BlockState[8], checker = new BlockState[8], many = new BlockState[8], ice = new BlockState[8], partial = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            halves[i] = OctetCellBlock.y(i) == 0 ? stone : oak;
            checker[i] = (OctetCellBlock.x(i) + OctetCellBlock.y(i) + OctetCellBlock.z(i)) % 2 == 0 ? stone : oak;
            many[i] = part(eight[i]);
            ice[i] = part(ices[OctetCellBlock.x(i) + 2 * OctetCellBlock.z(i)]);
            partial[i] = i == 0 || i == 1 || i == 5 ? stone : i == 4 ? oak : i == 6 ? part(Blocks.GOLD_BLOCK) : null;
        }
        out.add(halves);
        out.add(checker);
        out.add(many);
        out.add(ice);
        out.add(partial);
        return out;
    }

    /** The octet part of {@code full}'s material, stone where that block has no octet. */
    private static BlockState part(Block full) {
        Block cell = com.simplelib.api.OctetCells.cellFor(full);
        if (cell == null) cell = com.simplelib.api.OctetCells.cellFor(Blocks.STONE);
        return OctetCells.material(cell.defaultBlockState());
    }

    /** The sub-grid shapes in stone bricks, left to right as seen from the front. */
    private static void subGrid(ClientGameTestContext context, TestSingleplayerContext world) {
        int[] masks = {0x01, 0x03, 0x33, 0xF3, 0x55, 0x05, 0x69, 0x96, 0x7F, OctetCellBlock.FULL};
        world.getServer().runOnServer(server -> {
            clear(server);
            ServerLevel l = server.overworld();
            Block bricks = com.simplelib.api.OctetCells.cellFor(Blocks.STONE_BRICKS);
            if (bricks == null) bricks = com.simplelib.api.OctetCells.cellFor(Blocks.STONE);
            for (int i = 0; i < masks.length; i++) {
                l.setBlockAndUpdate(base.offset(9 - 2 * i, 0, 6), OctetCellBlock.withMask(bricks.defaultBlockState(), masks[i]));
            }
        });
        settle(context, world);
        look(context, world, 0.0, 3.5, -2.5, 0.0, 0.4, 6.5);
        context.takeScreenshot("simpleoctets-subgrid");
        look(context, world, 0.0, 6.0, 2.5, 0.0, 0.0, 6.5);
        context.takeScreenshot("simpleoctets-subgrid-above");
    }

    /** Melon octets (1, 4, checker, 7/8, 8/8) next to a Vanilla melon. */
    private static void melon(ClientGameTestContext context, TestSingleplayerContext world) {
        int[] masks = {0x01, 0x33, 0x69, MISSING_FRONT_CORNER, OctetCellBlock.FULL};
        world.getServer().runOnServer(server -> {
            clear(server);
            ServerLevel l = server.overworld();
            for (int i = 0; i < masks.length; i++) {
                l.setBlockAndUpdate(base.offset(5 - 2 * i, 0, 5), OctetCellBlock.withMask(OctetRegistry.MELON_OCTET.defaultBlockState(), masks[i]));
            }
            set(server, -6, 0, 5, "minecraft:melon");
        });
        settle(context, world);
        look(context, world, -0.5, 2.6, -0.5, -0.5, 0.4, 5.5);
        context.takeScreenshot("simpleoctets-melon");
    }

    private static void hud(ClientGameTestContext context, boolean shown) {
        context.runOnClient(c -> {
            if (c.gui.hud.isHidden() == shown) c.gui.hud.toggle();
        });
    }

    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        context.waitTicks(10);
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
    }

    /** A smooth stone floor and air above, around the scene. */
    private static void clear(MinecraftServer server) {
        fill(server, -16, -1, -6, 16, -1, 24, "minecraft:smooth_stone");
        fill(server, -16, 0, -6, 16, 12, 24, "minecraft:air");
    }

    private static void set(MinecraftServer server, int x, int y, int z, String block) {
        run(server, "setblock " + at(x, y, z) + " " + block);
    }

    private static void fill(MinecraftServer server, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        run(server, "fill " + at(x0, y0, z0) + " " + at(x1, y1, z1) + " " + block);
    }

    private static String at(int x, int y, int z) {
        return (base.getX() + x) + " " + (base.getY() + y) + " " + (base.getZ() + z);
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Eye at base + (ex, ey, ez) looking at base + (tx, ty, tz) ({@code tp ... facing} turns from the feet, so the target is lowered alike). */
    private static void look(ClientGameTestContext context, TestSingleplayerContext world, double ex, double ey, double ez, double tx, double ty, double tz) {
        world.getServer().runOnServer(server -> run(server, String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                base.getX() + ex, base.getY() + ey - 1.62, base.getZ() + ez, base.getX() + tx, base.getY() + ty - 1.62, base.getZ() + tz)));
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(15);
    }
}
