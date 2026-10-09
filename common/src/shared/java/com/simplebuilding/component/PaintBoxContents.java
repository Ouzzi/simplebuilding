package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Contents of a paint box (owner 2026-10-09, round 3): one count per dye colour in {@code DyeColor} id order (white
 * first, black last) and the colour that is "in front" ({@link #NONE} when none was chosen). Immutable; the item builds
 * a new value for every change.
 */
public record PaintBoxContents(List<Integer> counts, int selected) {
    public static final int COLORS = 16;
    public static final int NONE = -1;
    public static final PaintBoxContents EMPTY = new PaintBoxContents(Collections.nCopies(COLORS, 0), NONE);

    public static final Codec<PaintBoxContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, Integer.MAX_VALUE).listOf().fieldOf("counts").forGetter(PaintBoxContents::counts),
            Codec.intRange(NONE, COLORS - 1).optionalFieldOf("selected", NONE).forGetter(PaintBoxContents::selected)
    ).apply(i, PaintBoxContents::new));

    public static final StreamCodec<ByteBuf, PaintBoxContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(COLORS)), PaintBoxContents::counts,
            ByteBufCodecs.VAR_INT, PaintBoxContents::selected,
            PaintBoxContents::new);

    public PaintBoxContents {
        List<Integer> fixed = new ArrayList<>(Collections.nCopies(COLORS, 0));
        for (int c = 0; c < Math.min(COLORS, counts.size()); c++) fixed.set(c, Math.max(0, counts.get(c)));
        counts = List.copyOf(fixed);
        selected = selected < NONE || selected >= COLORS ? NONE : selected;
    }

    public int count(int color) {
        return this.counts.get(color);
    }

    public int total() {
        int sum = 0;
        for (int count : this.counts) sum += count;
        return sum;
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    /** A copy with {@code color} set to {@code count}. */
    public PaintBoxContents with(int color, int count) {
        List<Integer> copy = new ArrayList<>(this.counts);
        copy.set(color, Math.max(0, count));
        return new PaintBoxContents(copy, this.selected);
    }

    public PaintBoxContents withSelected(int color) {
        return new PaintBoxContents(this.counts, color);
    }
}
