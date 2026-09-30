package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Matches explicit {@link PortalRecipe}s against the world. Tries both horizontal
 * orientations and aligns every interior cell of the recipe to the given (ignition or
 * existing-portal) cell, so the structure is recognized regardless of which interior cell
 * the player lit. The first complete match wins.
 */
public final class PortalRecipeMatcher {

    private PortalRecipeMatcher() {
    }

    public static Optional<MatchedPortal> match(PortalWorldView world, List<PortalRecipe> recipes, int x, int y, int z) {
        if (recipes == null) {
            return Optional.empty();
        }
        for (PortalRecipe recipe : recipes) {
            if (recipe == null || !recipe.hasInterior()) {
                continue;
            }
            for (PortalAxis axis : PortalAxis.values()) {
                for (int[] anchor : recipe.interiorCellsRel()) {
                    Optional<MatchedPortal> match = tryPlace(world, recipe, axis, x, y, z, anchor[0], anchor[1]);
                    if (match.isPresent()) {
                        return match;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<MatchedPortal> tryPlace(PortalWorldView world, PortalRecipe recipe, PortalAxis axis,
                                                    int ix, int iy, int iz, int anchorCol, int anchorRow) {
        boolean alongX = axis == PortalAxis.X;
        int originTopY = iy + anchorRow;
        int originAlong = (alongX ? ix : iz) - anchorCol;
        int fixed = alongX ? iz : ix;

        List<BlockPos3i> interior = new ArrayList<>();
        List<MatchedPortal.FrameBlock> frame = new ArrayList<>();

        for (int row = 0; row < recipe.height(); row++) {
            for (int col = 0; col < recipe.width(); col++) {
                PortalRecipe.Cell cell = recipe.cellAt(col, row);
                if (cell == PortalRecipe.Cell.IGNORE) {
                    continue;
                }
                int along = originAlong + col;
                int wy = originTopY - row;
                int wx = alongX ? along : fixed;
                int wz = alongX ? fixed : along;
                if (cell == PortalRecipe.Cell.FRAME) {
                    String blockId = recipe.blockAt(col, row);
                    if (!world.isBlock(wx, wy, wz, blockId)) {
                        return Optional.empty();
                    }
                    frame.add(new MatchedPortal.FrameBlock(new BlockPos3i(wx, wy, wz), blockId));
                } else { // INTERIOR
                    if (!world.isInterior(wx, wy, wz)) {
                        return Optional.empty();
                    }
                    interior.add(new BlockPos3i(wx, wy, wz));
                }
            }
        }
        if (interior.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MatchedPortal(axis, interior, frame));
    }
}
