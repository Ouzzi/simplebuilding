package com.simplebuilding.modules.simplelights;

import com.simplelights.block.AbstractLightPaneBlock;
import com.simplelights.block.CopperLightPaneBlock;
import com.simplelights.block.CopperRedstoneLightPaneBlock;
import com.simplelights.block.LightPaneBlock;
import com.simplelights.block.RedstoneLightPaneBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PLAN40 simplelights-1: the first client look at the Light Panes. Scenes (each built fresh in front of the player, HUD hidden,
 * spectator camera): a redstone block and a stone block with a pane on all six sides (lit / dark, from above, from below, at night);
 * a normal and a transparent pane on bricks switched by a real lever pull (off, on, on at night); the sixteen colors and the sixteen
 * copper colors lit and dark (day, night); the redstone pane at signal strengths 1..15 and the copper / copper redstone pane flip-flop.
 *
 * <p>Strengths 1..15: a redstone block feeds a line of dust on top of a one-high stone wall; the dust loses one level per block, so the
 * dust above wall block {@code x} has {@code 15 - x} and powers that stone, and the pane hanging on its front reads exactly that
 * strength ({@code getBestNeighborSignal}). Seen from the front the strengths run 1..15 from left to right (start color white: white,
 * orange ... red). Every state is checked on the server; the problems are collected and reported after the last screenshot, so a wrong
 * state still leaves every picture behind. Then four real right-clicks with stone in the hand ({@code ServerPlayerGameMode#useItemOn},
 * like a note block: start color +1 each, no stone placed) and one while sneaking (the stone is placed in front of the pane).
 */
public final class LightsClientSmoke implements FabricClientGameTest {
    private static final List<String> PROBLEMS = new ArrayList<>();
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplelights")) throw new AssertionError("Module did not boot");
        PROBLEMS.clear();
        context.takeScreenshot("simplelights-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                base = server.getPlayerList().getPlayers().getFirst().blockPosition();
                run(server, "gamemode spectator @a");
                run(server, "time set noon");
                run(server, "gamerule advance_time false");
                run(server, "weather clear");
            });
            context.runOnClient(c -> {
                if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
            sides(context, world);
            leverSwitch(context, world);
            colors(context, world, "light_pane", false);
            colors(context, world, "copper_light_pane", true);
            copperToggle(context, world);
            strengths(context, world);
            context.runOnClient(c -> {
                if (c.gui.hud.isHidden()) c.gui.hud.toggle();
            });
        }
        if (!PROBLEMS.isEmpty()) throw new AssertionError(PROBLEMS.size() + " light pane problem(s): " + String.join("; ", PROBLEMS));
    }

    /** A redstone block (all six panes lit) and a stone block (all six dark), each with a pane on every side. */
    private static void sides(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            cube(server, -3, "minecraft:redstone_block");
            cube(server, 3, "minecraft:stone");
        });
        settle(context, world);
        world.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            for (int x : new int[]{-3, 3}) {
                BlockPos c = base.offset(x, 3, 6);
                for (Direction d : Direction.values()) {
                    BlockState s = level.getBlockState(c.relative(d));
                    boolean lit = s.is(com.simplelights.LightsRegistry.LIGHT_PANE) && s.getValue(LightPaneBlock.LIT);
                    if (!s.is(com.simplelights.LightsRegistry.LIGHT_PANE)) PROBLEMS.add("sides: no pane on " + d + " of x " + x + ": " + s);
                    else if (lit != (x < 0)) PROBLEMS.add("sides: pane on " + d + " of x " + x + " lit=" + lit);
                }
            }
        });
        look(context, world, 6.5, 6.5, 0.0, 0.0, 3.0, 6.5);
        context.takeScreenshot("simplelights-sides-above");
        look(context, world, -6.5, 1.3, 0.5, 0.0, 2.9, 6.5);
        context.takeScreenshot("simplelights-sides-below");
        night(context, world, true);
        look(context, world, 6.5, 6.5, 0.0, 0.0, 3.0, 6.5);
        context.takeScreenshot("simplelights-sides-night");
        night(context, world, false);
    }

    /** A block at (x, 3, 6) with a light pane on all six faces. */
    private static void cube(MinecraftServer server, int x, String block) {
        set(server, x, 3, 6, block);
        set(server, x, 4, 6, "simplelights:light_pane[facing=up]");
        set(server, x, 2, 6, "simplelights:light_pane[facing=down]");
        set(server, x, 3, 5, "simplelights:light_pane[facing=north]");
        set(server, x, 3, 7, "simplelights:light_pane[facing=south]");
        set(server, x - 1, 3, 6, "simplelights:light_pane[facing=west]");
        set(server, x + 1, 3, 6, "simplelights:light_pane[facing=east]");
    }

    /** A normal and a transparent pane on a brick wall, each beside a lever; the levers are pulled like a player's right-click. */
    private static void leverSwitch(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -5, 0, 8, 5, 3, 8, "minecraft:bricks");
            set(server, -2, 1, 7, "simplelights:light_pane[facing=north]");
            set(server, -1, 1, 7, "minecraft:lever[face=wall,facing=north,powered=false]");
            set(server, 2, 1, 7, "simplelights:transparent_light_pane[facing=north]");
            set(server, 3, 1, 7, "minecraft:lever[face=wall,facing=north,powered=false]");
            // the same two panes without a lever, for comparison
            set(server, -2, 2, 7, "simplelights:light_pane[facing=north]");
            set(server, 2, 2, 7, "simplelights:transparent_light_pane[facing=north]");
        });
        settle(context, world);
        look(context, world, 0.5, 1.9, 2.0, 0.5, 1.7, 7.5);
        litCheck(world, "switch off", new int[][]{{-2, 1, 7}, {2, 1, 7}}, false);
        context.takeScreenshot("simplelights-switch-off");
        world.getServer().runOnServer(server -> {
            pull(server, -1, 1, 7);
            pull(server, 3, 1, 7);
        });
        settle(context, world);
        litCheck(world, "switch on", new int[][]{{-2, 1, 7}, {2, 1, 7}}, true);
        litCheck(world, "switch, no lever", new int[][]{{-2, 2, 7}, {2, 2, 7}}, false);
        context.takeScreenshot("simplelights-switch-on");
        night(context, world, true);
        context.takeScreenshot("simplelights-switch-on-night");
        night(context, world, false);
    }

    /**
     * The sixteen colors of {@code suffix} ({@code <dye>_light_pane} or {@code <dye>_copper_light_pane}) on a stone wall: the lower row
     * lit by a redstone block under each pane (a copper pane flips on at its first rising edge), the upper row dark. White is on the
     * left as seen from the front.
     */
    private static void colors(ClientGameTestContext context, TestSingleplayerContext world, String suffix, boolean copper) {
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -9, 0, 8, 8, 3, 8, "minecraft:stone");
            DyeColor[] dyes = DyeColor.values();
            for (int i = 0; i < dyes.length; i++) {
                int x = 7 - i;
                set(server, x, 0, 7, "minecraft:redstone_block");
                set(server, x, 1, 7, "simplelights:" + dyes[i].getSerializedName() + "_" + suffix + "[facing=north]");
                set(server, x, 2, 7, "simplelights:" + dyes[i].getSerializedName() + "_" + suffix + "[facing=north]");
            }
        });
        settle(context, world);
        world.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            for (int i = 0; i < 16; i++) {
                for (int y = 1; y <= 2; y++) {
                    BlockState s = level.getBlockState(base.offset(7 - i, y, 7));
                    boolean lit = s.hasProperty(LightPaneBlock.LIT) && s.getValue(LightPaneBlock.LIT);
                    if (!(s.getBlock() instanceof AbstractLightPaneBlock) || lit != (y == 1)) {
                        PROBLEMS.add("colors " + suffix + " #" + i + " y" + y + ": " + s);
                    }
                }
            }
        });
        look(context, world, -0.5, 2.6, -4.0, -0.5, 1.5, 7.5);
        context.takeScreenshot(copper ? "simplelights-copper-colors-day" : "simplelights-colors-day");
        night(context, world, true);
        context.takeScreenshot(copper ? "simplelights-copper-colors-night" : "simplelights-colors-night");
        night(context, world, false);
    }

    /** Copper pane (left) and copper redstone pane (right), each with a lever: off, on, still on after the lever goes off, off again. */
    private static void copperToggle(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -5, 0, 8, 5, 3, 8, "minecraft:stone");
            set(server, 2, 1, 7, "simplelights:copper_light_pane[facing=north]");
            set(server, 3, 1, 7, "minecraft:lever[face=wall,facing=north,powered=false]");
            set(server, -2, 1, 7, "simplelights:copper_redstone_light_pane[facing=north]");
            set(server, -1, 1, 7, "minecraft:lever[face=wall,facing=north,powered=false]");
        });
        settle(context, world);
        look(context, world, 0.5, 1.9, 2.0, 0.5, 1.6, 7.5);
        int[][] panes = {{2, 1, 7}, {-2, 1, 7}};
        litCheck(world, "copper start", panes, false);
        context.takeScreenshot("simplelights-copper-toggle-off");
        boolean[] expected = {true, true, false};
        for (int step = 0; step < expected.length; step++) {
            world.getServer().runOnServer(server -> {
                pull(server, 3, 1, 7);
                pull(server, -1, 1, 7);
            });
            settle(context, world);
            litCheck(world, "copper pull " + (step + 1), panes, expected[step]);
            if (step == 0) {
                world.getServer().runOnServer(server -> {
                    // lever strength 15, start color white: color (0 + 15 - 1) = red
                    BlockState s = server.overworld().getBlockState(base.offset(-2, 1, 7));
                    if (s.getValue(CopperRedstoneLightPaneBlock.COLOR) != DyeColor.RED.getId()) PROBLEMS.add("copper redstone color " + s);
                });
                context.takeScreenshot("simplelights-copper-toggle-on");
            } else if (step == 1) {
                context.takeScreenshot("simplelights-copper-toggle-stays");
            } else {
                context.takeScreenshot("simplelights-copper-toggle-flipped");
            }
        }
    }

    /** Redstone pane at strengths 1..15 (dust line), the colors checked; then the real right-click with stone in the hand. */
    private static void strengths(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -1, 0, 8, 14, 0, 8, "minecraft:stone");
            set(server, -1, 1, 8, "minecraft:redstone_block");
            // one dust at a time from the power source on: each takes its level from the one before (15, 14, ... 1)
            for (int x = 0; x <= 14; x++) set(server, x, 1, 8, "minecraft:redstone_wire");
            for (int x = 0; x <= 14; x++) set(server, x, 0, 7, "simplelights:redstone_light_pane[facing=north]");
        });
        settle(context, world);
        checkStrengths(world, 0);
        look(context, world, 7.5, 1.6, 1.0, 7.5, 0.5, 7.5);
        context.takeScreenshot("simplelights-redstone-strengths");
        night(context, world, true);
        context.takeScreenshot("simplelights-redstone-strengths-night");
        night(context, world, false);
        // Like a note block: right-click with a block in the hand tunes (4 clicks: start color 4), it places nothing.
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = server.overworld();
            player.setGameMode(GameType.CREATIVE);
            player.setShiftKeyDown(false);
            for (int x = 0; x <= 14; x++) {
                for (int click = 0; click < 4; click++) {
                    ItemStack stone = new ItemStack(Items.STONE, 16);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stone);
                    var result = player.gameMode.useItemOn(player, level, stone, InteractionHand.MAIN_HAND, paneHit(x));
                    if (!result.consumesAction()) PROBLEMS.add("right-click on pane " + x + ": " + result);
                }
                if (!level.getBlockState(base.offset(x, 0, 6)).isAir()) PROBLEMS.add("right-click placed a block in front of pane " + x);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setGameMode(GameType.SPECTATOR);
        });
        settle(context, world);
        checkStrengths(world, 4);
        context.takeScreenshot("simplelights-redstone-start-shifted");
        // Sneaking + right-click with the block places it in front of the pane; the start color stays.
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = server.overworld();
            player.setGameMode(GameType.CREATIVE);
            ItemStack stone = new ItemStack(Items.STONE, 16);
            player.setItemInHand(InteractionHand.MAIN_HAND, stone);
            player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player, level, stone, InteractionHand.MAIN_HAND, paneHit(7));
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setGameMode(GameType.SPECTATOR);
            if (!level.getBlockState(base.offset(7, 0, 6)).is(Blocks.STONE)) PROBLEMS.add("sneak right-click did not place the stone");
            if (level.getBlockState(base.offset(7, 0, 7)).getValue(RedstoneLightPaneBlock.START) != 4) PROBLEMS.add("sneak right-click tuned the pane");
        });
        settle(context, world);
        context.takeScreenshot("simplelights-redstone-sneak-placed");
    }

    /** The hit on the front (north) face of the redstone pane at (x, 0, 7): the pane is the last 1/16 block before the wall. */
    private static BlockHitResult paneHit(int x) {
        BlockPos pos = base.offset(x, 0, 7);
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 15.0 / 16.0), Direction.NORTH, pos, false);
    }

    /** Pane x (0..14) reads strength 15 - x and shows dye color (start + strength - 1) mod 16. */
    private static void checkStrengths(TestSingleplayerContext world, int start) {
        world.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            for (int x = 0; x <= 14; x++) {
                BlockPos pos = base.offset(x, 0, 7);
                BlockState s = level.getBlockState(pos);
                int strength = level.getBestNeighborSignal(pos);
                if (!s.is(com.simplelights.LightsRegistry.REDSTONE_LIGHT_PANE)) {
                    PROBLEMS.add("strength pane " + x + " missing: " + s);
                    continue;
                }
                if (strength != 15 - x) PROBLEMS.add("pane " + x + ": strength " + strength + " expected " + (15 - x));
                int color = s.getValue(RedstoneLightPaneBlock.COLOR);
                if (s.getValue(RedstoneLightPaneBlock.START) != start || color != RedstoneLightPaneBlock.colorFor(start, 15 - x)) {
                    PROBLEMS.add("pane " + x + " start " + s.getValue(RedstoneLightPaneBlock.START) + " color " + color);
                }
            }
        });
    }

    private static void litCheck(TestSingleplayerContext world, String what, int[][] cells, boolean lit) {
        world.getServer().runOnServer(server -> {
            for (int[] c : cells) {
                BlockState s = server.overworld().getBlockState(base.offset(c[0], c[1], c[2]));
                boolean on = s.hasProperty(LightPaneBlock.LIT) ? s.getValue(LightPaneBlock.LIT)
                        : s.hasProperty(CopperLightPaneBlock.LIT) && s.getValue(CopperLightPaneBlock.LIT);
                if (on != lit) PROBLEMS.add(what + ": " + s + " lit=" + on);
            }
        });
    }

    /** The lever at base + (x, y, z) pulled the way a player's right-click does ({@code LeverBlock#useWithoutItem}). */
    private static void pull(MinecraftServer server, int x, int y, int z) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = base.offset(x, y, z);
        server.overworld().getBlockState(pos).useWithoutItem(server.overworld(), player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static void night(ClientGameTestContext context, TestSingleplayerContext world, boolean night) {
        world.getServer().runOnServer(server -> run(server, night ? "time set midnight" : "time set noon"));
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
    }

    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        context.waitTicks(10);
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
    }

    /** A smooth stone floor and air above, around the scene. */
    private static void clear(MinecraftServer server) {
        fill(server, -20, -1, -6, 20, -1, 24, "minecraft:smooth_stone");
        fill(server, -20, 0, -6, 20, 10, 24, "minecraft:air");
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
