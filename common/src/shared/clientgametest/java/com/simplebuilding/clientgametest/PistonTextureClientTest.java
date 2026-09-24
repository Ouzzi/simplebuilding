package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The reinforced sticky piston and the enderite piston render with their own textures.
 *
 * <p>Both blocks are new, and both got their models from datagen helpers that borrow from another
 * piston: the sticky one takes bottom, sides and extended base from the reinforced piston and only
 * its push plate from {@code reinforced_piston_top_sticky}, the enderite one runs through the same
 * {@code registerCustomPiston} as the netherite piston. A helper called with the wrong block, a
 * blockstate that was not regenerated, or a texture missing from the atlas all look fine to every
 * server test - the block still pushes - and only show up as a wrong or pink block on screen.
 *
 * <p><b>The observation is the baked model, not a screenshot.</b> This walks the client's finished
 * block model set, exactly like the furnace check in {@link SmokeClientTest}: for a state it asks
 * which sprites the quads on each side carry. That names the texture outright, is the same on
 * every loader and at every window size, and needs no scene. A texture file that failed to load
 * bakes as {@code minecraft:missingno}, which is not the expected name, so "the file is missing"
 * fails here too. What it does not do is look at the pixels inside a sprite: whether the art in
 * {@code enderite_piston_side.png} differs from the netherite one is a question for a human.
 *
 * <p>Checked states (facing north, so the push plate is the north face of vanilla's
 * {@code template_piston}): retracted, all six faces - north the push plate, south the bottom, the
 * other four the side; extended, the two faces the extended base shares with the retracted block
 * that stay visible, east (side) and south (bottom). The extended base's north face is not
 * checked: the mod's {@code PISTON_BASE_MODEL} template has no {@code inside} slot, so every mod
 * piston's extended base inherits vanilla's {@code piston_inner} there, which is the case for the
 * old reinforced and netherite pistons as well; the head covers that face except while it moves.
 *
 * <p>The plain reinforced piston's push plate is checked as the control: the sticky piston's
 * plate must differ from it, or "the sticky piston looks sticky" was never measured.
 *
 * <p><b>What breaks this test:</b> {@code registerStickyPistonVariant} or
 * {@code registerCustomPiston} in {@code ModModelProvider} called with the wrong block or the
 * wrong suffix, generated blockstates or models not regenerated after a rename, and a texture
 * file ({@code reinforced_piston_top_sticky.png}, {@code enderite_piston_top/side/bottom.png})
 * missing from the resources.
 */
public final class PistonTextureClientTest {

    private PistonTextureClientTest() {
    }

    /** One piston and the three textures its retracted model must show. */
    private record Expected(Block block, String platform, String bottom, String side) {
    }

    public static void inWorld(Script script) {
        script.act("the reinforced sticky and the enderite piston bake their own textures", client -> {
            BlockStateModelSet models = client.getModelManager().getBlockStateModelSet();
            List<String> found = new ArrayList<>();

            List<Expected> pistons = List.of(
                    new Expected(ModBlocks.REINFORCED_STICKY_PISTON, "simplebuilding:block/reinforced_piston_top_sticky",
                            "simplebuilding:block/reinforced_piston_bottom", "simplebuilding:block/reinforced_piston_side"),
                    new Expected(ModBlocks.ENDERITE_PISTON, "simplebuilding:block/enderite_piston_top",
                            "simplebuilding:block/enderite_piston_bottom", "simplebuilding:block/enderite_piston_side"),
                    new Expected(ModBlocks.REINFORCED_PISTON, "simplebuilding:block/reinforced_piston_top",
                            "simplebuilding:block/reinforced_piston_bottom", "simplebuilding:block/reinforced_piston_side"));

            // The retracted push plate of each piston, for the sticky-versus-plain control below.
            List<Set<String>> plates = new ArrayList<>();

            for (Expected piston : pistons) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(piston.block());

                for (boolean extended : new boolean[]{false, true}) {
                    BlockState state = piston.block().defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, Direction.NORTH)
                            .setValue(PistonBaseBlock.EXTENDED, extended);

                    BlockStateModel model = models.get(state);

                    if (model == models.missingModel()) {
                        found.add(state + " has no baked model (the client fell back to the missing model)");
                        if (!extended) {
                            plates.add(Set.of());
                        }
                        continue;
                    }

                    if (!extended) {
                        plates.add(spritesOn(model, Direction.NORTH));
                    }

                    List<Direction> sides = extended
                            ? List.of(Direction.EAST, Direction.SOUTH)
                            : List.of(Direction.values());

                    for (Direction side : sides) {
                        String expected = side == Direction.NORTH ? piston.platform()
                                : side == Direction.SOUTH ? piston.bottom() : piston.side();
                        Set<String> onSide = spritesOn(model, side);

                        if (!onSide.equals(Set.of(expected))) {
                            found.add(blockId + (extended ? " extended" : " retracted") + " shows " + onSide
                                    + " on its " + side + " side, expected exactly [" + expected + "]");
                        }
                    }
                }
            }

            // pistons.get(0) is the sticky one, pistons.get(2) the plain reinforced control.
            Set<String> stickyPlate = plates.get(0);
            Set<String> plainPlate = plates.get(2);

            if (stickyPlate.equals(plainPlate)) {
                found.add("the reinforced sticky piston's push plate " + stickyPlate + " is the plain reinforced "
                        + "piston's, so the two cannot be told apart");
            }

            if (!found.isEmpty()) {
                throw new AssertionError("Piston models show the wrong textures on the client: " + found);
            }
        });
    }

    /** Every sprite the baked model puts on one side of the block, by name. */
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
}
