package com.simplevisuals;

import java.util.List;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * IG31-21, owner choice 31.10. (variant A): a cauldron standing on a campfire gets a small stand drawn under it, four 2 x 2 corner
 * posts from the top of the fire to the cauldron and a 2 x 2 frame around the top, in the cauldron's own texture. Pure render
 * add-on (no block, no collision, fire and cauldron unchanged); option {@code visuals.cauldronStand}. This class holds the rule and
 * the boxes; the loader's block-model wrapper draws them (Fabric: {@code CauldronStandModel}).
 */
public final class CauldronStand {
    /** Box in pixels relative to the cauldron block (0..16; y below 0 reaches into the campfire block). */
    public record Box(float x0, float y0, float z0, float x1, float y1, float z1) {}

    /** Top of the Vanilla campfire model (7 px) seen from the cauldron above: -9 px. */
    public static final float FIRE_TOP = -9F;

    public static final List<Box> BOXES = List.of(
            // four corner posts 2 x 2, from the top of the fire to the bottom of the cauldron
            new Box(1, FIRE_TOP, 1, 3, 0, 3), new Box(13, FIRE_TOP, 1, 15, 0, 3),
            new Box(1, FIRE_TOP, 13, 3, 0, 15), new Box(13, FIRE_TOP, 13, 15, 0, 15),
            // frame around the top, 2 x 2, between the posts
            new Box(3, -2, 1, 13, 0, 3), new Box(3, -2, 13, 13, 0, 15),
            new Box(1, -2, 3, 3, 0, 13), new Box(13, -2, 3, 15, 0, 13));

    /** True when {@code cauldron} (any cauldron block, filled or not) stands on {@code below} (any campfire, lit or not). */
    public static boolean applies(BlockState cauldron, BlockState below) {
        return cauldron.getBlock() instanceof AbstractCauldronBlock && below.getBlock() instanceof CampfireBlock;
    }

    /** The option, behind the super-mod switch. */
    public static boolean enabled() {
        return Visuals.CONFIG != null && Visuals.CONFIG.visuals != null && Visuals.active(Visuals.CONFIG.visuals.cauldronStand);
    }

    private CauldronStand() {}
}
