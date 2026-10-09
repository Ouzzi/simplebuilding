package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.GoatHornHolderBlock;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.StandingRodBlock;
import com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.HammockItem;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Placing, queue N24/N16 (2026-10-09, docs/ai/PLAN-PLATZIEREN-N24-2026-10-09.md): one documentary picture of a 4-ingot
 * stack, a 2-ingot pile, four trims in a pile, goat horns on the floor (torch) and on the wall (stick, soul torch),
 * stacked standing rods (joined) and a hammock tied to a post hanging to the player's hand like a lead.
 *
 * <p>What it looks like is for a human; asserted is only that the client got the blocks with their contents, so a
 * missing renderer registration or block entity sync fails here.
 */
public final class PlaceClientTest {
    private static final int Z = 18;

    private PlaceClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.SMALL_PLACEABLES || ModBlocks.GOAT_HORN_HOLDER == null) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        onServer(script, "build the placing scene", server -> {
            ServerLevel level = server.overworld();
            pile(level, new BlockPos(7, 0, Z), Direction.NORTH, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);
            pile(level, new BlockPos(8, 0, Z), Direction.EAST, Items.GOLD_INGOT, Items.COPPER_INGOT);
            pile(level, new BlockPos(9, 0, Z), Direction.NORTH, ModItems.ENDERITE_INGOT, Items.NETHERITE_INGOT, Items.NETHERITE_INGOT);
            pile(level, new BlockPos(10, 0, Z), Direction.NORTH, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            horn(level, new BlockPos(11, 0, Z), AttachFace.FLOOR, Direction.NORTH, new ItemStack(Items.TORCH));
            horn(level, new BlockPos(12, 0, Z), AttachFace.FLOOR, Direction.EAST, new ItemStack(Items.BLAZE_ROD));
            horn(level, new BlockPos(11, 1, wallZ()), AttachFace.WALL, Direction.NORTH, new ItemStack(Items.SOUL_TORCH));
            horn(level, new BlockPos(12, 1, wallZ()), AttachFace.WALL, Direction.NORTH, new ItemStack(Items.STICK));
            if (ModBlocks.STANDING_ROD != null) {
                level.setBlock(new BlockPos(13, 0, Z), ModBlocks.STANDING_ROD.defaultBlockState()
                        .setValue(StandingRodBlock.ROD, StandingRodBlock.Rod.STICK).setValue(StandingRodBlock.UP, true), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(13, 1, Z), ModBlocks.STANDING_ROD.defaultBlockState()
                        .setValue(StandingRodBlock.ROD, StandingRodBlock.Rod.BONE), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(14, 0, Z), ModBlocks.STANDING_ROD.defaultBlockState()
                        .setValue(StandingRodBlock.ROD, StandingRodBlock.Rod.BLAZE_ROD).setValue(StandingRodBlock.UP, true), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(14, 1, Z), ModBlocks.STANDING_ROD.defaultBlockState()
                        .setValue(StandingRodBlock.ROD, StandingRodBlock.Rod.BLAZE_ROD), Block.UPDATE_CLIENTS);
            }
            // a hammock tied to a fence post, held by the player: hangs like a lead
            BlockPos post = new BlockPos(6, 0, 17);
            level.setBlock(post, Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (ModItems.WHITE_HAMMOCK != null) {
                ItemStack hammock = new ItemStack(ModItems.RED_HAMMOCK != null ? ModItems.RED_HAMMOCK : ModItems.WHITE_HAMMOCK);
                HammockItem.rememberAnchor(hammock, level, post);
                server.getPlayerList().getPlayers().forEach(p -> p.setItemInHand(InteractionHand.MAIN_HAND, hammock.copy()));
            }
        });
        script.command("tp @a 10.5 0.0 14.6 0.0 22.0");
        script.awaitPackets();
        script.await("the client has the piles and the horns", 100, client ->
                        client.level.getBlockEntity(new BlockPos(7, 0, Z)) instanceof PlacedSmallPartsBlockEntity be && be.parts().size() == 4
                                && client.level.getBlockEntity(new BlockPos(11, 0, Z)) instanceof GoatHornHolderBlockEntity horn
                                && horn.held().is(Items.TORCH),
                client -> "pile " + client.level.getBlockEntity(new BlockPos(7, 0, Z)) + ", horn " + client.level.getBlockEntity(new BlockPos(11, 0, Z)));
        script.idle("let the torches flicker and the chunk settle", 40);
        script.shot("placing-n24");
    }

    private static int wallZ() {
        return TestScene.WALL_Z - 1;
    }

    private static void pile(ServerLevel level, BlockPos pos, Direction facing, net.minecraft.world.item.Item... items) {
        level.setBlock(pos, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState().setValue(PlacedSmallPartsBlock.FACING, facing), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity be) {
            be.setParts(java.util.Arrays.stream(items).map(ItemStack::new).toList());
        }
    }

    private static void horn(ServerLevel level, BlockPos pos, AttachFace face, Direction facing, ItemStack held) {
        level.setBlock(pos, ModBlocks.GOAT_HORN_HOLDER.defaultBlockState().setValue(GoatHornHolderBlock.FACE, face)
                .setValue(GoatHornHolderBlock.FACING, facing), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be) {
            be.setHorn(new ItemStack(Items.GOAT_HORN));
            be.setHeld(held);
        }
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
