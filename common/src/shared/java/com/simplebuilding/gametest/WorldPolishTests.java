package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Queue N37, world part (owner 2026-10-11): placeables break part by part, buckets draw no durability bar and the
 * Enderite bucket shows only its name, the sculk jaw needs a full block top.
 */
public final class WorldPolishTests {
    /** The floating boat test waits 40 ticks for the boat to settle. */
    public static final int BOAT_MAX_TICKS = 80;
    private WorldPolishTests() {
    }

    /**
     * Breaking a pile of small parts (the real {@code ServerPlayerGameMode#destroyBlock} of a survival player) takes only
     * the part the player looks at: it drops, the others stay where they lay (the block, its candle count and its
     * facing). The last part breaks the block as before.
     */
    public static void pilesBreakOnlyTheLookedAtPart(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = survivalPlayer(helper);
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState().setValue(PlacedSmallPartsBlock.FACING, Direction.EAST));
        PlacedSmallPartsBlockEntity pile = helper.getBlockEntity(rel, PlacedSmallPartsBlockEntity.class);
        pile.setParts(List.of(new ItemStack(Items.CANDLE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.SEA_PICKLE)));
        BlockPos pos = helper.absolutePos(rel);
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.CANDLES), 1, "candles before");

        breakLookingAt(helper, player, pos, 1);
        helper.assertTrue(helper.getBlockState(rel).is(ModBlocks.PLACED_SMALL_PARTS), "the whole pile broke: " + helper.getBlockState(rel));
        helper.assertValueEqual(items(helper, rel), List.of(Items.CANDLE, Items.SEA_PICKLE), "parts left after breaking the iron ingot");
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.CANDLES), 1, "candles after");
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.FACING), Direction.EAST, "facing after");
        helper.assertValueEqual(dropped(helper, rel, Items.IRON_INGOT), 1, "dropped iron ingots");
        helper.assertValueEqual(dropped(helper, rel, Items.CANDLE) + dropped(helper, rel, Items.SEA_PICKLE), 0, "other parts dropped");

        breakLookingAt(helper, player, pos, 1);
        helper.assertValueEqual(items(helper, rel), List.of(Items.CANDLE), "parts left after breaking the sea pickle");
        helper.assertValueEqual(dropped(helper, rel, Items.SEA_PICKLE), 1, "dropped sea pickles");

        breakLookingAt(helper, player, pos, 0);
        helper.assertTrue(helper.getBlockState(rel).isAir(), "the last part did not break the block: " + helper.getBlockState(rel));
        helper.assertValueEqual(dropped(helper, rel, Items.CANDLE), 1, "dropped candles");
        helper.succeed();
    }

    /** Puts the player's eye one block above part {@code index} looking straight down, then breaks the block. */
    private static void breakLookingAt(GameTestHelper helper, ServerPlayer player, BlockPos pos, int index) {
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(pos);
        List<ItemStack> parts = pile.parts();
        Direction facing = helper.getLevel().getBlockState(pos).getValue(PlacedSmallPartsBlock.FACING);
        AABB part = PlacedSmallParts.partShape(parts.get(index), facing, parts.size(), index,
                PlacedSmallParts.ingotStack(parts)).bounds().move(pos);
        Vec3 top = new Vec3(part.getCenter().x, part.maxY, part.getCenter().z);
        player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
        helper.assertValueEqual(PlacedSmallParts.lookedAtPart(player, parts, facing, pos), index, "looked-at part");
        player.gameMode.destroyBlock(pos);
    }

    private static List<Item> items(GameTestHelper helper, BlockPos rel) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof PlacedSmallPartsBlockEntity be
                ? be.parts().stream().map(ItemStack::getItem).toList() : List.of();
    }

    private static int dropped(GameTestHelper helper, BlockPos rel, Item item) {
        AABB around = new AABB(helper.absolutePos(rel)).inflate(2.0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    /** A real survival player (the mock player is always creative and drops nothing). */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "n37-world"), false);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }


    private static ItemStack stack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * N50: Sculk Jaws generate naturally on Deep Dark floors. Here: the placed feature is registered and has 24 tries per (N55: was 6)
     * chunk, the biome side is a modifier per loader (Fabric BiomeModifications, NeoForge/Forge biome_modifier JSON).
     */
    public static void sculkJawFeatureIsRegistered(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.SCULK_JAW == null) {
            helper.succeed();
            return;
        }
        var placed = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        var jaw = placed.get(com.simplebuilding.util.ModWorldGen.SCULK_JAW_PLACED_KEY);
        helper.assertTrue(jaw.isPresent(), "the placed Sculk Jaw feature is registered");
        helper.assertTrue(jaw.get().value().placement().stream().anyMatch(
                m -> m instanceof net.minecraft.world.level.levelgen.placement.CountPlacement), "it rolls a count per chunk");
        helper.succeed();
    }

    /**
     * N55: a Sculk Jaw spawns only in the middle of sculk - the floor block under it and the four horizontal neighbors of that
     * floor block (a plus of five) are all Sculk. Allowed: the full plus (diagonals do not matter). Forbidden: any one of the
     * five missing or replaced (stone, Sculk Vein, Sculk Catalyst, air), the spot itself occupied.
     */
    public static void sculkJawSpawnsOnlyInTheMiddleOfSculk(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.SCULK_JAW == null) {
            helper.succeed();
            return;
        }
        var rule = com.simplebuilding.util.ModWorldGen.sculkJawSpot();
        BlockPos center = new BlockPos(3, 1, 3);
        BlockPos spot = center.above();
        BlockPos[] plus = {center, center.east(), center.west(), center.north(), center.south()};
        // Reset: a stone slab of 5x5 under the spot, the plus of Sculk on top of it, air above.
        Runnable reset = () -> {
            for (int x = 1; x <= 5; x++) {
                for (int z = 1; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                }
            }
            for (BlockPos pos : plus) {
                helper.setBlock(pos, Blocks.SCULK);
            }
        };
        reset.run();
        helper.assertTrue(rule.test(helper.getLevel(), helper.absolutePos(spot)), "the full plus of sculk is rejected");
        // Diagonal floor blocks are stone: still fine.
        helper.assertTrue(helper.getBlockState(center.east().north()).is(Blocks.STONE), "diagonals are stone in this setup");
        // Each of the five replaced in turn (by stone, a vein-like block, a catalyst, air).
        net.minecraft.world.level.block.state.BlockState[] wrong = {Blocks.STONE.defaultBlockState(),
                Blocks.SCULK_CATALYST.defaultBlockState(), Blocks.SCULK_VEIN.defaultBlockState(), Blocks.AIR.defaultBlockState()};
        for (BlockPos missing : plus) {
            for (var state : wrong) {
                reset.run();
                helper.setBlock(missing, state);
                helper.assertFalse(rule.test(helper.getLevel(), helper.absolutePos(spot)),
                        "spawn allowed with " + state.getBlock() + " at " + missing.subtract(center) + " of the floor plus");
            }
        }
        // Only the floor block is sculk, the neighbors are stone.
        reset.run();
        for (int i = 1; i < plus.length; i++) {
            helper.setBlock(plus[i], Blocks.STONE);
        }
        helper.assertFalse(rule.test(helper.getLevel(), helper.absolutePos(spot)), "spawn allowed on a single sculk block");
        // No sculk at all.
        reset.run();
        for (BlockPos pos : plus) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.assertFalse(rule.test(helper.getLevel(), helper.absolutePos(spot)), "spawn allowed on plain stone");
        // The spot itself must be free.
        reset.run();
        helper.setBlock(spot, Blocks.STONE);
        helper.assertFalse(rule.test(helper.getLevel(), helper.absolutePos(spot)), "spawn allowed inside a block");
        // The placed feature uses a rule filter and the raised number of tries.
        var placed = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                .get(com.simplebuilding.util.ModWorldGen.SCULK_JAW_PLACED_KEY);
        helper.assertTrue(placed.isPresent() && placed.get().value().placement().stream().anyMatch(
                m -> m instanceof net.minecraft.world.level.levelgen.placement.BlockPredicateFilter), "the feature has no block predicate filter");
        helper.succeed();
    }

    /**
     * The sculk jaw stands only on a block with a solid, full top: stone and an upper slab yes; a lower slab, a fence,
     * a glass pane or air no. Taking the block away under a jaw removes it.
     */
    public static void sculkJawNeedsFullBlockTop(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.SCULK_JAW == null) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var jaw = ModBlocks.SCULK_JAW.defaultBlockState();
        Object[][] floors = {
                {Blocks.STONE.defaultBlockState(), true},
                {Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), true},
                {Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), false},
                {Blocks.OAK_FENCE.defaultBlockState(), false},
                {Blocks.GLASS_PANE.defaultBlockState(), false},
                {Blocks.AIR.defaultBlockState(), false},
        };
        for (int i = 0; i < floors.length; i++) {
            BlockPos floor = new BlockPos(1 + i, 1, 1);
            helper.setBlock(floor, (net.minecraft.world.level.block.state.BlockState) floors[i][0]);
            boolean survives = jaw.canSurvive(level, helper.absolutePos(floor.above()));
            helper.assertValueEqual(survives, floors[i][1], "jaw on " + floors[i][0]);
        }
        BlockPos floor = new BlockPos(1, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), jaw);
        helper.setBlock(floor, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(floor.above()).isAir(), "the jaw stayed without its floor: " + helper.getBlockState(floor.above()));
        helper.succeed();
    }

    /**
     * A tier chest boat floating in still water keeps its height on the 1/4096 grid of the move packets
     * ({@code TieredChestBoat#tick}), so the client's deck lies exactly where the server's does and a player standing on it
     * is not set back by the server.
     */
    public static void standersStayOnTheChestBoatDeck(GameTestHelper helper) {
        if (!McVersion.TIERED_VEHICLES || com.simplebuilding.entity.ModEntities.ENDERITE_CHEST_BOAT == null) {
            helper.succeed();
            return;
        }
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }
        ServerLevel level = helper.getLevel();
        var boat = com.simplebuilding.entity.ModEntities.ENDERITE_CHEST_BOAT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.6123, 2.5));
        boat.setPos(at.x, at.y, at.z);
        level.addFreshEntity(boat);
        helper.runAfterDelay(40, () -> {
            double grid = boat.getY() * com.simplebuilding.entity.vehicle.TieredChestBoat.PACKET_GRID;
            helper.assertTrue(boat.isInWater() || boat.getY() > at.y - 1.0, "the boat sank: " + boat.getY());
            helper.assertTrue(grid == Math.rint(grid), "the boat height " + boat.getY() + " is off the packet grid");
            helper.succeed();
        });
    }
}
