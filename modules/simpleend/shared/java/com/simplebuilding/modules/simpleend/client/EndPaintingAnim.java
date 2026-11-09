package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.EndPaintings;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

/**
 * Client only, no network: every End painting plays one of its {@link EndPaintings#ANIM_COUNT} animations (chosen at
 * random each time; all start from the idle picture) at a random moment, 60 to 120 s after the last one (or after it
 * first came into view); between the animations it shows its idle picture.
 */
public final class EndPaintingAnim {
    private static final Map<Painting, long[]> START = new WeakHashMap<>(); // painting -> {tick the next animation starts, its animation}

    private EndPaintingAnim() {}

    /**
     * The frame texture number to show at {@code now} ({@code <id>_<n>}, see {@link EndPaintings#frameTexture}), or 0
     * for the idle picture; advances the timer and picks the next animation at random.
     */
    public static int frame(Map<Painting, long[]> timers, Painting painting, long now, java.util.function.IntUnaryOperator random) {
        long[] next = timers.computeIfAbsent(painting, p -> new long[] {now + gap(random), random.applyAsInt(EndPaintings.ANIM_COUNT)});
        if (now < next[0]) return 0;
        long step = (now - next[0]) / EndPaintings.ANIM_STEP_TICKS;
        if (step < EndPaintings.ANIM_SEQUENCE.length) return EndPaintings.frameTexture((int) next[1], EndPaintings.ANIM_SEQUENCE[(int) step]);
        next[0] = now + gap(random);
        next[1] = random.applyAsInt(EndPaintings.ANIM_COUNT);
        return 0;
    }

    private static long gap(java.util.function.IntUnaryOperator random) {
        return EndPaintings.GAP_MIN_TICKS + random.applyAsInt(EndPaintings.GAP_SPAN_TICKS + 1);
    }

    /** The variant to render: an End painting's frame variant while it animates, else the variant itself. */
    public static PaintingVariant animated(Painting painting, PaintingVariant variant) {
        if (!EndPaintings.isEnd(variant)) return variant;
        int frame = frame(START, painting, painting.level().getGameTime(), painting.level().getRandom()::nextInt);
        if (frame == 0) return variant;
        Identifier id = variant.assetId().withSuffix("_" + frame);
        return new PaintingVariant(variant.width(), variant.height(), id, variant.title(), variant.author());
    }
}
