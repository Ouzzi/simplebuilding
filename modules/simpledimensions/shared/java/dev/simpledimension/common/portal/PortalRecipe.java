package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An explicit portal "recipe": a 2D grid describing the exact shape and which block
 * each cell must be. Parsed from config rows + a legend (char -> block id).
 *
 * <p>Cell meaning:
 * <ul>
 *   <li>a legend char -> {@link Cell#FRAME} that must be the mapped block id,</li>
 *   <li>the interior char (default {@code '.'}) -> {@link Cell#INTERIOR} (becomes a portal block),</li>
 *   <li>the ignore char (default {@code ' '}) or any unknown char -> {@link Cell#IGNORE} (not checked).</li>
 * </ul>
 * Row 0 is the top of the portal; column 0 is the left along the horizontal axis.
 */
public final class PortalRecipe {

    public enum Cell { FRAME, INTERIOR, IGNORE }

    private final int width;
    private final int height;
    private final Cell[][] cells;   // [row][col]
    private final String[][] blocks; // [row][col]; non-null only for FRAME cells

    public PortalRecipe(int width, int height, Cell[][] cells, String[][] blocks) {
        this.width = width;
        this.height = height;
        this.cells = cells;
        this.blocks = blocks;
    }

    public static PortalRecipe parse(List<String> rows, Map<String, String> legend, char interiorChar, char ignoreChar) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("recipe rows must not be empty");
        }
        int height = rows.size();
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row == null ? 0 : row.length());
        }
        if (width == 0) {
            throw new IllegalArgumentException("recipe width must be > 0");
        }

        Cell[][] cells = new Cell[height][width];
        String[][] blocks = new String[height][width];
        for (int row = 0; row < height; row++) {
            String line = rows.get(row) == null ? "" : rows.get(row);
            for (int col = 0; col < width; col++) {
                char c = col < line.length() ? line.charAt(col) : ignoreChar;
                if (c == interiorChar) {
                    cells[row][col] = Cell.INTERIOR;
                } else if (c == ignoreChar) {
                    cells[row][col] = Cell.IGNORE;
                } else {
                    String block = legend == null ? null : legend.get(String.valueOf(c));
                    if (block != null) {
                        cells[row][col] = Cell.FRAME;
                        blocks[row][col] = block;
                    } else {
                        cells[row][col] = Cell.IGNORE;
                    }
                }
            }
        }
        return new PortalRecipe(width, height, cells, blocks);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Cell cellAt(int col, int row) {
        return cells[row][col];
    }

    public String blockAt(int col, int row) {
        return blocks[row][col];
    }

    public boolean hasInterior() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (cells[row][col] == Cell.INTERIOR) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Relative (col,row) positions of every interior cell. */
    public List<int[]> interiorCellsRel() {
        List<int[]> out = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (cells[row][col] == Cell.INTERIOR) {
                    out.add(new int[]{col, row});
                }
            }
        }
        return out;
    }
}
