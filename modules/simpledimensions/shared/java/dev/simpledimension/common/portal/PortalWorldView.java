package dev.simpledimension.common.portal;

/**
 * Loader-agnostic view of the world used by the portal scanners.
 *
 * <p>The rectangular scanner only needs {@link #isFrame}/{@link #isEmpty}/{@link #isPortal}.
 * The recipe matcher additionally needs {@link #isBlock} to test arbitrary block ids per
 * cell, so a recipe can mix several frame blocks. No light information is ever required —
 * a complete structure is the only activation requirement, like a vanilla nether portal.
 */
public interface PortalWorldView {

    /** True if the block is the frame block configured for the dimension being scanned. */
    boolean isFrame(int x, int y, int z);

    /** True if the block matches the given block id (e.g. "minecraft:obsidian"). */
    boolean isBlock(int x, int y, int z, String blockId);

    /** True if the cell may become a portal block (air or fire). */
    boolean isEmpty(int x, int y, int z);

    /** True if the cell already contains the mod's portal block. */
    boolean isPortal(int x, int y, int z);

    /** A cell that can sit inside an active portal: either empty or an existing portal block. */
    default boolean isInterior(int x, int y, int z) {
        return isEmpty(x, y, z) || isPortal(x, y, z);
    }
}
