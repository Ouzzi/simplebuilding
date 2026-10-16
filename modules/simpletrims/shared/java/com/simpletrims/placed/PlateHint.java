package com.simpletrims.placed;

import com.simplebuilding.framework.api.TransformHints;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hand hint for placed trim templates (framework {@link TransformHints}): with SimpleBuilding loaded the held tool tilts when
 * a right click would strike the aimed plate (template tool in the main hand, upgrade material in the off hand). Cosmetic and
 * client-asked only; without SimpleBuilding nobody asks. The target highlight is {@link #aimedAtStrikePlate}.
 */
public final class PlateHint {
    public static final String ID = "simpletrims:plate_strike";

    private PlateHint() {}

    public static void register() {
        TransformHints.register(ID, query -> query.player() instanceof Player player && query.hit() instanceof BlockHitResult hit
                && aimedAtStrikePlate(player, hit));
    }

    /** Tool stance (tool + material) while aiming at an upgradable placed plate that the player may change. */
    public static boolean aimedAtStrikePlate(Player player, BlockHitResult hit) {
        return com.simpletrims.SimpleTrims.on() && PlacedTemplates.isToolTarget(player.level(), hit.getBlockPos(), player)
                && PlacedTemplates.hasMaterials(player);
    }
}
