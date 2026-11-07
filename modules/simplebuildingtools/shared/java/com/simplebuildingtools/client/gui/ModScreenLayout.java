package com.simplebuildingtools.client.gui;

/** Where the boxes of the octant and wand screens go (the numbers of SimpleBuilding's ModScreenLayout for these two screens). */
public final class ModScreenLayout {
    public static final int FRAME = 5, FRAME_BOTTOM = 7;
    public static final int SIDE = 8;
    /** Octant panel and the HUD readouts' tone: matt bronze (owner N64 2026-10-15, palette B; light labels read on it). */
    public static final int OCTANT_FILL = 0xFF6F5F4B;
    /** Building wand panel: Vanilla's widths (200 px buttons) plus 8 px on each side, like a container's slot margin. */
    public static final int WAND_W = 216, WAND_H = 94;
    /** Wand rows relative to the panel: title, radius, axis, close (20 high, 24 apart like Vanilla's option lists). */
    public static final int WAND_TITLE_Y = 6, WAND_RADIUS_Y = 18, WAND_AXIS_Y = 42, WAND_CLOSE_Y = 66;
    public static final int WAND_BUTTON_X = SIDE, WAND_BUTTON_W = 200, WAND_CLOSE_W = 100;

    private ModScreenLayout() {
    }

    /** Fill of a building wand's panel from its item id path ({@code copper_building_wand} ...): its material. */
    public static int wandFill(String itemPath) {
        if (itemPath.startsWith("copper")) return 0xFFB4684D;
        if (itemPath.startsWith("iron")) return 0xFF9A9DA2; // SimpleLib IRON
        if (itemPath.startsWith("gold")) return 0xFFD1A13A;
        if (itemPath.startsWith("diamond")) return 0xFF3A9E95;
        if (itemPath.startsWith("netherite")) return 0xFF5F524C; // SimpleLib NETHERITE
        if (itemPath.startsWith("enderite")) return 0xFF8E6CB0; // SimpleLib ENDERITE
        return 0xFF7A5A9E; // creative: the creative tab's purple-ish
    }
}
