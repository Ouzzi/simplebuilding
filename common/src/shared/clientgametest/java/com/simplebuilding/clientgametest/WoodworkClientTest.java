package com.simplebuilding.clientgametest;

import com.simplebuilding.version.McVersion;
import com.simplebuilding.woodwork.CarvedLogBlock;
import com.simplebuilding.woodwork.CrateBlock;
import com.simplebuilding.woodwork.CrateBlockEntity;
import com.simplebuilding.woodwork.SherdMotif;
import com.simplebuilding.woodwork.WoodBlocks;
import com.simplebuilding.woodwork.WoodKind;
import com.simplebuilding.woodwork.WoodenCauldronBlock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Woodwork (docs/ai/PLAN-HOLZWERK-2026-10-09.md, 26.3): the carved-wood motifs are real sprites in the blocks atlas
 * (derived at load from the pottery patterns, not missingno), and one documentary picture of hollow logs, sheets,
 * wooden cauldrons (water, crimson with lava), crates with food and carved logs. Cleans up behind itself.
 */
public final class WoodworkClientTest {
    private static final int Z = 19;
    private static final int CRATE_Z = 16;

    private WoodworkClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.WOODWORK) {
            return;
        }
        script.act("carved wood bakes the derived motif sprites", client -> {
            BlockStateModelSet models = client.getModelManager().getBlockStateModelSet();
            List<String> found = new ArrayList<>();
            for (WoodBlocks.Family family : WoodBlocks.families()) {
                for (SherdMotif motif : new SherdMotif[]{SherdMotif.ANGLER, SherdMotif.SNORT}) {
                    BlockState state = family.carved().defaultBlockState().setValue(CarvedLogBlock.FACING, Direction.NORTH)
                            .setValue(CarvedLogBlock.MOTIF, motif);
                    Set<String> north = spritesOn(models.get(state), Direction.NORTH);
                    String expected = "minecraft:entity/decorated_pot/" + motif.getSerializedName() + "_pottery_pattern_" + family.wood().id();
                    if (!north.contains(expected)) {
                        found.add(state + " shows " + north + ", expected " + expected);
                    }
                }
            }
            if (!found.isEmpty()) {
                throw new AssertionError("Carved wood misses its motif sprites: " + found);
            }
        });

        TestScene.build(script, "minecraft:smooth_stone", "creative");
        onServer(script, "place the woodwork blocks", server -> {
            ServerLevel level = server.overworld();
            WoodBlocks.Family oak = WoodBlocks.family(WoodKind.OAK);
            WoodBlocks.Family cherry = WoodBlocks.family(WoodKind.CHERRY);
            WoodBlocks.Family crimson = WoodBlocks.family(WoodKind.CRIMSON);
            WoodBlocks.Family spruce = WoodBlocks.family(WoodKind.SPRUCE);
            WoodBlocks.Family bamboo = WoodBlocks.family(WoodKind.BAMBOO);
            set(level, 5, 0, oak.hollow().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            set(level, 6, 0, oak.hollowStripped().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            set(level, 5, 1, crimson.hollow().defaultBlockState());
            set(level, 6, 1, bamboo.hollow().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            set(level, 7, 0, oak.cauldron().with(WoodenCauldronBlock.Content.WATER, 3));
            set(level, 8, 0, crimson.cauldron().with(WoodenCauldronBlock.Content.LAVA, 3));
            set(level, 9, 0, oak.crate().defaultBlockState());
            set(level, 10, 0, spruce.crate().defaultBlockState());
            fill(level, 9, List.of(new ItemStack(Items.BREAD, 64), new ItemStack(Items.APPLE, 64), new ItemStack(Items.APPLE, 20)));
            fill(level, 10, List.of(new ItemStack(Items.CARROT, 64), new ItemStack(Items.CARROT, 64), new ItemStack(Items.CARROT, 64),
                    new ItemStack(Items.CARROT, 64), new ItemStack(Items.CARROT, 64), new ItemStack(Items.CARROT, 64), new ItemStack(Items.GOLDEN_CARROT, 40)));
            set(level, 11, 0, carved(oak, SherdMotif.ANGLER));
            set(level, 12, 0, carved(cherry, SherdMotif.HEART));
            set(level, 13, 0, carved(crimson, SherdMotif.SKULL));
            set(level, 14, 0, carved(spruce, SherdMotif.HOWL));
            set(level, 15, 0, carved(bamboo, SherdMotif.PRIZE));
            set(level, 11, 1, carved(WoodBlocks.family(WoodKind.DARK_OAK), SherdMotif.SNORT));
            set(level, 12, 1, carved(WoodBlocks.family(WoodKind.PALE_OAK), SherdMotif.FLOW));
            set(level, 13, 1, carved(WoodBlocks.family(WoodKind.WARPED), SherdMotif.BREWER));
            for (int x = 7; x <= 10; x++) {
                Block sheet = x < 9 ? oak.sheet() : oak.strippedSheet();
                set(level, x, 1, sheet.defaultBlockState());
            }
            for (int x = 7; x <= 10; x++) {
                BlockPos pos = new BlockPos(x, 1, Z);
                level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 3);
            }
        });
        script.awaitPackets();
        script.await("the client has the crate contents", 100,
                client -> client.level.getBlockEntity(new BlockPos(10, 0, Z)) instanceof CrateBlockEntity crate && !crate.isEmpty());
        script.idle("let the blocks render", 20);
        script.shot("woodwork-overview");
        script.command("tp @a 10.5 1.2 17.6 0.0 35.0");
        script.awaitPackets();
        script.idle("let the close-up settle", 20);
        script.shot("woodwork-closeup");
        script.command("fill 4 0 " + Z + " 16 2 " + Z + " minecraft:air", true);
        script.command("kill @e[type=!minecraft:player]", true);
        // Queue N31: crates in every orientation (opening to the camera = north), slats of all twelve woods; three
        // blocks in front of the wall so the camera can also look from behind.
        onServer(script, "place the crates", server -> {
            ServerLevel level = server.overworld();
            Direction[] facings = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.DOWN, Direction.SOUTH, Direction.UP, Direction.UP};
            WoodKind[] woods = WoodKind.values();
            for (int i = 0; i < facings.length; i++) {
                level.setBlock(new BlockPos(5 + i, 0, CRATE_Z), WoodBlocks.family(woods[i % woods.length]).crate().defaultBlockState().setValue(CrateBlock.FACING, facings[i]), 3);
            }
            for (int i = 0; i < woods.length - facings.length; i++) {
                level.setBlock(new BlockPos(5 + i, 1, CRATE_Z), WoodBlocks.family(woods[facings.length + i]).crate().defaultBlockState(), 3);
            }
            fillAt(level, 5, CRATE_Z, List.of(new ItemStack(Items.APPLE, 64), new ItemStack(Items.APPLE, 64), new ItemStack(Items.APPLE, 40)));
            fillAt(level, 6, CRATE_Z, List.of(new ItemStack(Items.CARROT, 64), new ItemStack(Items.CARROT, 20)));
            fillAt(level, 7, CRATE_Z, List.of(new ItemStack(Items.BREAD, 64), new ItemStack(Items.BREAD, 30)));
            fillAt(level, 8, CRATE_Z, List.of(new ItemStack(Items.POTATO, 64), new ItemStack(Items.POTATO, 64), new ItemStack(Items.BAKED_POTATO, 64)));
            fillAt(level, 9, CRATE_Z, List.of(new ItemStack(Items.BEETROOT, 10)));
            fillAt(level, 10, CRATE_Z, List.of(new ItemStack(Items.APPLE, 5)));
            fillAt(level, 11, CRATE_Z, List.of(new ItemStack(Items.COOKIE, 1)));
            List<ItemStack> full = new ArrayList<>();
            for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                full.add(new ItemStack(Items.GOLDEN_CARROT, 64));
            }
            fillAt(level, 12, CRATE_Z, full);
        });
        script.awaitPackets();
        script.await("the client has the crate contents", 100,
                client -> client.level.getBlockEntity(new BlockPos(12, 0, CRATE_Z)) instanceof CrateBlockEntity crate && !crate.isEmpty());
        String[][] views = {
                {"crates-front", "8.5 0.4 13.3 0.0 25.0"},
                {"crates-above", "8.5 2.4 14.6 0.0 65.0"},
                {"crates-left", "3.6 0.6 13.6 -45.0 25.0"},
                {"crates-right", "13.4 0.6 13.6 45.0 25.0"},
                {"crates-back", "8.5 0.6 18.4 180.0 30.0"},
                {"crates-low", "8.5 0.0 13.0 0.0 15.0"},
        };
        for (String[] view : views) {
            script.command("tp @a " + view[1]);
            script.awaitPackets();
            script.idle("let " + view[0] + " settle", 15);
            script.shot(view[0]);
        }
        script.command("fill 4 0 " + CRATE_Z + " 16 2 " + CRATE_Z + " minecraft:air", true);
        script.command("kill @e[type=!minecraft:player]", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
    }

    private static BlockState carved(WoodBlocks.Family family, SherdMotif motif) {
        return family.carved().defaultBlockState().setValue(CarvedLogBlock.FACING, Direction.NORTH).setValue(CarvedLogBlock.MOTIF, motif);
    }

    private static void set(ServerLevel level, int x, int y, BlockState state) {
        level.setBlock(new BlockPos(x, y, Z), state, 3);
    }

    private static void fill(ServerLevel level, int x, List<ItemStack> stacks) {
        fillAt(level, x, Z, stacks);
    }

    private static void fillAt(ServerLevel level, int x, int z, List<ItemStack> stacks) {
        if (level.getBlockEntity(new BlockPos(x, 0, z)) instanceof CrateBlockEntity crate) {
            stacks.forEach(stack -> crate.insert(stack.copy()));
        }
    }

    private static Set<String> spritesOn(BlockStateModel model, Direction side) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        Set<String> sprites = new LinkedHashSet<>();
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(side)) {
                sprites.add(quad.materialInfo().sprite().contents().name().toString());
            }
        }
        return sprites;
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
