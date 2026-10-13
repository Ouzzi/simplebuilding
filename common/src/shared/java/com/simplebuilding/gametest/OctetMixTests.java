package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.MaterialOctetBlock;
import com.simplebuilding.blocks.custom.MixedOctetBlock;
import com.simplebuilding.blocks.custom.OctetCellBlock;
import com.simplebuilding.blocks.custom.OctetCells;
import com.simplebuilding.blocks.entity.custom.MixedOctetBlockEntity;
import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.chess.ChessItems;
import com.simplebuilding.util.MaterialOctets;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Queue N48 (docs/ai/PLAN-OCTETS-MIX-2026-10-13.md): octets of different materials share a cell. The cell turns into
 * the mixed octet cell (materials in its block entity) and back into a pure, baked cell when one material is left;
 * a player breaks only the looked-at octet; without a player every octet drops; old pure cells stay as they are.
 */
public final class OctetMixTests {
    private static final BlockPos CELL = new BlockPos(2, 2, 2);

    private OctetMixTests() {
    }

    private static Block cell(Block full) {
        return MaterialOctets.cellFor(full);
    }

    private static Item piece(Block full) {
        return ((MaterialOctetBlock) cell(full)).piece();
    }

    private static BlockState material(Block full) {
        return OctetCells.material(cell(full).defaultBlockState());
    }

    private static BlockState lapis() {
        return OctetCells.material(ModBlocks.CHECKER_OCTET.defaultBlockState().setValue(com.simplebuilding.blocks.custom.CheckerOctetBlock.COLOR, ChessColor.LAPIS));
    }

    private static int bit(int x, int y, int z) {
        return 1 << OctetCellBlock.index(x, y, z);
    }

    /** Click the top face of the floor below the cell at the given quarter (places into the lower octet there). */
    private static void placeOn(GameTestHelper helper, ServerPlayer player, Item item, int x, int z) {
        BlockPos floor = helper.absolutePos(CELL.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 4));
        BlockHitResult top = new BlockHitResult(new Vec3(floor.getX() + x * 0.5 + 0.25, floor.getY() + 1.0, floor.getZ() + z * 0.5 + 0.25),
                Direction.UP, floor, false);
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top));
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 away = helper.absoluteVec(new Vec3(0.5, 8.0, 0.5));
        player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
        return player;
    }

    /** Octet items of three materials (oak, stone, a chess colour) fill one cell; taking them out turns it pure again. */
    public static void octetsMixMaterialsInOneCell(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        helper.setBlock(CELL.below(), Blocks.STONE);
        ServerPlayer player = mockPlayer(helper);
        BlockPos pos = helper.absolutePos(CELL);
        placeOn(helper, player, piece(Blocks.OAK_PLANKS), 0, 0);
        helper.assertTrue(helper.getBlockState(CELL).is(cell(Blocks.OAK_PLANKS)), "first octet makes a pure oak cell: " + helper.getBlockState(CELL));
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "a pure cell has no block entity");
        placeOn(helper, player, piece(Blocks.STONE), 1, 0);
        placeOn(helper, player, ChessItems.octet(ChessColor.LAPIS), 0, 1);
        BlockState mixed = helper.getBlockState(CELL);
        helper.assertTrue(mixed.is(ModBlocks.MIXED_OCTET), "three materials make a mixed cell: " + mixed);
        helper.assertValueEqual(OctetCellBlock.mask(mixed), bit(0, 0, 0) | bit(1, 0, 0) | bit(0, 0, 1), "mask of the mixed cell");
        BlockState[] parts = OctetCells.parts(helper.getLevel(), pos, mixed);
        helper.assertTrue(parts[OctetCellBlock.index(0, 0, 0)] == material(Blocks.OAK_PLANKS)
                && parts[OctetCellBlock.index(1, 0, 0)] == material(Blocks.STONE)
                && parts[OctetCellBlock.index(0, 0, 1)] == lapis(), "materials per octet: " + java.util.Arrays.toString(parts));
        helper.assertTrue(mixed.getCollisionShape(helper.getLevel(), pos).bounds().maxY <= 0.5, "collision follows the octets");
        // an occupied octet refuses, whatever the material
        helper.assertTrue(com.simplebuilding.items.custom.MaterialOctetItem.placed(helper.getLevel(), material(Blocks.STONE), pos,
                OctetCellBlock.index(0, 0, 0), null) == null, "an occupied octet took a second one");
        // sneak + empty hand takes the clicked octet out (its own material)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        takeOut(helper, player, 1, 0);
        helper.assertTrue(player.getInventory().contains(new ItemStack(piece(Blocks.STONE))), "the stone octet came back");
        helper.assertTrue(helper.getBlockState(CELL).is(ModBlocks.MIXED_OCTET), "oak + lapis stay mixed");
        takeOut(helper, player, 0, 1);
        BlockState pure = helper.getBlockState(CELL);
        helper.assertTrue(pure.is(cell(Blocks.OAK_PLANKS)) && OctetCellBlock.mask(pure) == bit(0, 0, 0), "one material left is a pure cell again: " + pure);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "the pure cell dropped its block entity");
        helper.succeed();
    }

    private static void takeOut(GameTestHelper helper, ServerPlayer player, int x, int z) {
        BlockPos pos = helper.absolutePos(CELL);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + x * 0.5 + 0.25, pos.getY() + 0.5, pos.getZ() + z * 0.5 + 0.25),
                Direction.UP, pos, false);
        helper.getBlockState(CELL).useWithoutItem(helper.getLevel(), player, hit);
    }

    /** A survival player (real game mode) breaks only the octet it looks at; drops are exactly that octet. */
    public static void octetsBreakOnlyTheLookedAtOctet(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            parts[i] = OctetCellBlock.y(i) == 0 ? material(Blocks.STONE) : material(Blocks.OAK_PLANKS);
        }
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        helper.assertTrue(helper.getBlockState(CELL).is(ModBlocks.MIXED_OCTET), "stone below, oak above is mixed");
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        // the four oak octets, each looked at from above
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                breakFromAbove(helper, player, x, 1, z);
            }
        }
        BlockState rest = helper.getBlockState(CELL);
        helper.assertTrue(rest.is(cell(Blocks.STONE)) && OctetCellBlock.mask(rest) == OctetCellBlock.BOTTOM_HALF,
                "four stone octets are left as a pure cell: " + rest);
        helper.assertValueEqual(dropped(helper, piece(Blocks.OAK_PLANKS)), 4, "one oak octet per break");
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 0, "no stone octet dropped yet");
        // pure cells break per octet too
        breakFromAbove(helper, player, 1, 0, 1);
        BlockState three = helper.getBlockState(CELL);
        helper.assertTrue(three.is(cell(Blocks.STONE)) && OctetCellBlock.mask(three) == (OctetCellBlock.BOTTOM_HALF & ~bit(1, 0, 1)),
                "the looked-at stone octet is gone: " + three);
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 1, "one stone octet dropped");
        helper.succeed();
    }

    /** Owner 2026-10-13: sneaking breaks the whole cell - every octet of every material drops (needs the right tool). */
    public static void octetsSneakingBreaksTheWholeCell(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        BlockPos pos = helper.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            parts[i] = OctetCellBlock.y(i) == 0 ? material(Blocks.STONE) : material(Blocks.OAK_PLANKS);
        }
        OctetCells.set(helper.getLevel(), pos, parts, false, Block.UPDATE_ALL);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        Vec3 top = new Vec3(pos.getX() + 0.25, pos.getY() + 1.0, pos.getZ() + 0.25);
        player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
        player.setShiftKeyDown(true);
        player.gameMode.destroyBlock(pos);
        helper.assertBlockPresent(Blocks.AIR, CELL);
        helper.assertValueEqual(dropped(helper, piece(Blocks.OAK_PLANKS)), 4, "every oak octet");
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 4, "every stone octet");
        // a pure cell too
        helper.setBlock(CELL, OctetCellBlock.withMask(cell(Blocks.OAK_PLANKS).defaultBlockState(), 0x0F));
        player.gameMode.destroyBlock(pos);
        helper.assertBlockPresent(Blocks.AIR, CELL);
        helper.assertValueEqual(dropped(helper, piece(Blocks.OAK_PLANKS)), 8, "the pure cell drops its four octets");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /**
     * The sledgehammer follows the same rule: its area swing (not sneaking) takes one octet from each neighbouring
     * cell, sneaking it mines just the hit block - and that cell whole.
     */
    public static void octetsSledgehammerFollowsTheSneakRule(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        BlockState full = OctetCellBlock.withMask(cell(Blocks.STONE).defaultBlockState(), OctetCellBlock.FULL);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(CELL.offset(dx, 0, dz), full);
            }
        }
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.simplebuilding.items.ModItems.DIAMOND_SLEDGEHAMMER));
        BlockPos origin = helper.absolutePos(CELL);
        player.snapTo(origin.getX() + 0.5, origin.getY() + 2.0, origin.getZ() + 0.5, 0.0F, 90.0F);
        player.setShiftKeyDown(false);
        com.simplebuilding.util.SledgehammerUsageEvent.handleBeforeBlockBreak(helper.getLevel(), player, origin, helper.getBlockState(CELL), null);
        int left = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    left += OctetCellBlock.count(helper.getBlockState(CELL.offset(dx, 0, dz)));
                }
            }
        }
        helper.assertValueEqual(left, 8 * 7, "the area swing takes one octet per neighbouring cell");
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 8, "one stone octet per neighbour");
        player.setShiftKeyDown(true);
        helper.assertValueEqual(com.simplebuilding.items.custom.SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 1,
                "sneaking, the hammer mines one block");
        player.gameMode.destroyBlock(origin);
        helper.assertBlockPresent(Blocks.AIR, CELL);
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 16, "sneaking takes the whole cell");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    private static void breakFromAbove(GameTestHelper helper, ServerPlayer player, int x, int y, int z) {
        BlockPos pos = helper.absolutePos(CELL);
        Vec3 top = new Vec3(pos.getX() + x * 0.5 + 0.25, pos.getY() + y * 0.5 + 0.5, pos.getZ() + z * 0.5 + 0.25);
        player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertValueEqual(OctetCells.lookedAt(player, pos, state), OctetCellBlock.index(x, y, z), "looked-at octet");
        player.gameMode.destroyBlock(pos);
    }

    /** Without a player (explosion, {@code Level#destroyBlock}) a mixed cell drops every octet of every material. */
    public static void octetsMixedCellDropsEveryMaterial(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        BlockPos pos = helper.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        parts[0] = material(Blocks.OAK_PLANKS);
        parts[1] = material(Blocks.OAK_PLANKS);
        parts[2] = material(Blocks.STONE);
        parts[5] = lapis();
        OctetCells.set(helper.getLevel(), pos, parts, false, Block.UPDATE_ALL);
        helper.getLevel().destroyBlock(pos, true);
        helper.assertBlockPresent(Blocks.AIR, CELL);
        helper.assertValueEqual(dropped(helper, piece(Blocks.OAK_PLANKS)), 2, "oak octets");
        helper.assertValueEqual(dropped(helper, piece(Blocks.STONE)), 1, "stone octet");
        helper.assertValueEqual(dropped(helper, ChessItems.octet(ChessColor.LAPIS)), 1, "lapis octet");
        helper.succeed();
    }

    /**
     * World compatibility: an old pure cell (state only, as saved before N48) stays a pure cell without block entity and
     * mixes when a foreign octet joins; the mixed cell survives a save and load of its block entity, and a blueprint
     * scan keeps the pure cell of its most common material.
     */
    public static void octetsOldCellsMigrateAndMixedCellsPersist(GameTestHelper helper) {
        if (!McVersion.CHESS || ModBlocks.MIXED_OCTET == null) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CELL);
        BlockState old = OctetCellBlock.withMask(cell(Blocks.OAK_PLANKS).defaultBlockState(), bit(0, 0, 0) | bit(1, 0, 0));
        helper.setBlock(CELL, old);
        helper.assertTrue(helper.getBlockState(CELL) == old && level.getBlockEntity(pos) == null, "an old pure cell loads unchanged");
        BlockState[] parts = com.simplebuilding.items.custom.MaterialOctetItem.placed(level, lapis(), pos, OctetCellBlock.index(0, 1, 0), null);
        helper.assertTrue(parts != null, "a lapis octet fits into the old oak cell");
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        BlockState mixed = helper.getBlockState(CELL);
        helper.assertTrue(mixed.is(ModBlocks.MIXED_OCTET), "old cell + foreign octet is mixed: " + mixed);
        MixedOctetBlockEntity be = (MixedOctetBlockEntity) level.getBlockEntity(pos);
        CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(pos, mixed, tag, level.registryAccess());
        helper.assertTrue(loaded instanceof MixedOctetBlockEntity, "the mixed cell loads its block entity");
        BlockState[] back = ((MixedOctetBlockEntity) loaded).parts();
        helper.assertTrue(back[OctetCellBlock.index(0, 0, 0)] == material(Blocks.OAK_PLANKS) && back[OctetCellBlock.index(1, 0, 0)] == material(Blocks.OAK_PLANKS)
                && back[OctetCellBlock.index(0, 1, 0)] == lapis(), "materials after save and load: " + java.util.Arrays.toString(back) + " " + tag);
        helper.assertValueEqual(mixed.getValue(MixedOctetBlock.LIGHT), 0, "lapis and oak do not glow");
        BlockState scanned = OctetCells.portable(level, pos, mixed);
        helper.assertTrue(scanned.is(cell(Blocks.OAK_PLANKS)) && OctetCellBlock.mask(scanned) == OctetCellBlock.mask(mixed),
                "a blueprint keeps the most common material with every octet: " + scanned);
        helper.succeed();
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB around = new AABB(helper.absolutePos(CELL)).inflate(3.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, around)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    /** A real survival player (the mock player cannot run the block break with drops). */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "n48-octets"), false);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
