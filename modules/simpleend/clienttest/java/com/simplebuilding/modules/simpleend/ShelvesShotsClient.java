package com.simplebuilding.modules.simpleend;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * DW04C-3 (b-shelves01, docs/ai/PLAN-B-SHELVES01-2026-11-04.md): the owner picture SHELVES01 - per wood (Ashwood, Duskwood from this
 * module, crimson/warped blazewood and Vanilla's oak from SimpleBuilding/Vanilla) a bookshelf, a chiseled one empty, full of Vanilla
 * books and mixed with Blaze and Nihilit Books; the books in the hotbar. Other mods' blocks only by id (rule 3): missing ones stay air.
 */
public final class ShelvesShotsClient implements FabricClientGameTest {
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel l = (ServerLevel) p.level();
                base = p.blockPosition().offset(0, 0, 6);
                run(server, "gamemode creative @a");
                run(server, "time set noon");
                run(server, "weather clear");
                run(server, "gamerule advance_time false");
                build(l);
                String[] hot = {"minecraft:book", "simplebuilding:blaze_book", "simplebuilding:nihilith_book", "simpleend:ashwood_bookshelf",
                        "simpleend:chiseled_ashwood_bookshelf", "simpleend:duskwood_bookshelf", "simpleend:chiseled_duskwood_bookshelf",
                        "simplebuilding:chiseled_crimson_blazewood_bookshelf", "simplebuilding:chiseled_warped_blazewood_bookshelf"};
                for (int i = 0; i < hot.length; i++) p.getInventory().setItem(i, new ItemStack(item(hot[i])));
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(40);
            look(context, world, 5.5, 3.2, -6.5, 5.5, 2.4, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simpleend-shelves01-wall").withSize(1920, 1080));
            look(context, world, 3.0, 2.6, -3.0, 3.0, 2.2, 0.5);
            context.takeScreenshot(TestScreenshotOptions.of("simpleend-shelves01-close").withSize(1920, 1080));
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
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
        context.waitTicks(20);
    }

    /** Rows bottom to top: oak (Vanilla), crimson, warped, Ashwood, Duskwood; columns: bookshelf, chiseled empty, Vanilla books, mixed. */
    private static void build(ServerLevel l) {
        for (int x = -4; x <= 14; x++) {
            for (int z = -10; z <= 4; z++) {
                for (int y = -2; y <= 8; y++) l.setBlock(base.offset(x, y, z), (y < 0 ? Blocks.SMOOTH_STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        String[][] rows = {{"minecraft:bookshelf", "minecraft:chiseled_bookshelf"},
                {"simplebuilding:crimson_blazewood_bookshelf", "simplebuilding:chiseled_crimson_blazewood_bookshelf"},
                {"simplebuilding:warped_blazewood_bookshelf", "simplebuilding:chiseled_warped_blazewood_bookshelf"},
                {"simpleend:ashwood_bookshelf", "simpleend:chiseled_ashwood_bookshelf"},
                {"simpleend:duskwood_bookshelf", "simpleend:chiseled_duskwood_bookshelf"}};
        ItemStack[] vanilla = {new ItemStack(Items.BOOK), new ItemStack(Items.BOOK), new ItemStack(Items.ENCHANTED_BOOK),
                new ItemStack(Items.BOOK), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.BOOK)};
        ItemStack[] mixed = {new ItemStack(item("simplebuilding:blaze_book")), new ItemStack(Items.BOOK), new ItemStack(item("simplebuilding:nihilith_book")),
                new ItemStack(item("simplebuilding:nihilith_book")), new ItemStack(item("simplebuilding:blaze_book")), new ItemStack(Items.ENCHANTED_BOOK)};
        for (int r = 0; r < rows.length; r++) {
            Block shelf = block(rows[r][0]), chiseled = block(rows[r][1]);
            BlockPos row = base.offset(0, r, 0);
            l.setBlock(row.offset(8, 0, 0), shelf.defaultBlockState(), Block.UPDATE_ALL);
            for (int c = 0; c < 3; c++) {
                BlockPos at = row.offset(6 - 2 * c, 0, 0);
                if (chiseled == Blocks.AIR) continue;
                l.setBlock(at, chiseled.defaultBlockState().setValue(ChiseledBookShelfBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
                if (c == 0 || !(l.getBlockEntity(at) instanceof ChiseledBookShelfBlockEntity be)) continue;
                ItemStack[] books = c == 1 ? vanilla : mixed;
                for (int s = 0; s < 6; s++) if (!books[s].isEmpty() && books[s].getItem() != Items.AIR) be.setItem(s, books[s].copy());
            }
        }
    }
}
