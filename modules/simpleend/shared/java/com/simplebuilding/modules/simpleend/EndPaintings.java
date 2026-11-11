package com.simplebuilding.modules.simpleend;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.Level;

/**
 * W11 End paintings: the variants live in data ({@code painting_variant/*.json}, tag {@link #TAG}), the End Painting item
 * places only these, and a placed one is an ordinary Vanilla {@link Painting} (no own entity): it drops the End Painting
 * again ({@code PaintingMixin}). Client animation: {@code client.EndPaintingAnim}, three animations of four frame textures each ({@code <id>_1..12}).
 */
public final class EndPaintings {
    public static final TagKey<PaintingVariant> TAG = TagKey.create(Registries.PAINTING_VARIANT, SimpleEnd.id("end_paintings"));
    /** Frames per animation; keep in step with tools/gen_paintings.py (FRAMES). */
    public static final int ANIM_FRAMES = 4;
    /**
     * Animations per motif, all starting from the idle picture (b-paint12); animation {@code a} (0-based) uses the frame
     * textures {@code <id>_<a*ANIM_FRAMES+1> .. <id>_<(a+1)*ANIM_FRAMES>}. Keep in step with tools/gen_paintings.py (ANIMS).
     */
    public static final int ANIM_COUNT = 3;
    /** Frame order of one animation (1-based frame numbers within the animation) and ticks per step: 16 ticks. */
    public static final int[] ANIM_SEQUENCE = {1, 2, 3, 4, 4, 3, 2, 1};
    public static final int ANIM_STEP_TICKS = 2;
    /** Random gap between two animations of one painting: 42 to 84 s (mean 63 s; b-paint30: was 60 to 120 s, mean 90 s). */
    public static final int GAP_MIN_TICKS = 840;
    public static final int GAP_SPAN_TICKS = 840;

    private EndPaintings() {}

    /** Texture number ({@code <id>_<n>}) of the 1-based frame {@code frame} of animation {@code anim} (0-based). */
    public static int frameTexture(int anim, int frame) {
        return anim * ANIM_FRAMES + frame;
    }

    public static boolean isEnd(Holder<PaintingVariant> variant) {
        return isEnd(variant.value());
    }

    public static boolean isEnd(PaintingVariant variant) {
        return SimpleEnd.ID.equals(variant.assetId().getNamespace());
    }

    /** Like {@code Painting.create}, but chooses among the End variants only (largest that fits, random among equals). */
    public static Optional<Painting> create(Level level, BlockPos pos, Direction direction) {
        List<Painting> fits = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(TAG).forEach(variant -> {
            Painting candidate = new Painting(level, pos, direction, variant);
            if (candidate.survives()) fits.add(candidate);
        });
        int largest = fits.stream().mapToInt(p -> p.getVariant().value().area()).max().orElse(0);
        fits.removeIf(p -> p.getVariant().value().area() < largest);
        return Util.getRandomSafe(fits, level.getRandom());
    }
}
