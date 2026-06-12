package com.simplebuilding.component;

public final class ModDataComponentTypes {
    private ModDataComponentTypes() {
    }

    public static final String NBT_OFFSET = "sb_offset";
    public static final String NBT_GLOW_LEVEL = "sb_glow_level";
    public static final String NBT_VISUAL_GLOW = "sb_visual_glow";
    public static final String NBT_LIGHT_SOURCE = "sb_light_source";
    public static final String NBT_COORD_X = "sb_coord_x";
    public static final String NBT_COORD_Y = "sb_coord_y";
    public static final String NBT_COORD_Z = "sb_coord_z";

    public static void registerDataComponentTypes() {
        // 1.19.2 backport: no-op, values are stored in ItemStack NBT.
    }
}